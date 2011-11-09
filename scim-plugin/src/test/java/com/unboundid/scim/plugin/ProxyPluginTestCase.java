/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.plugin;

import com.unboundid.directory.tests.standalone.DirectoryInstance;
import com.unboundid.directory.tests.standalone.ExternalInstanceId;
import com.unboundid.directory.tests.standalone.ExternalInstanceManager;
import com.unboundid.directory.tests.standalone.ProxyInstance;
import com.unboundid.directory.tests.standalone.TestCaseUtils;
import com.unboundid.scim.client.SCIMEndpoint;
import com.unboundid.scim.client.SCIMService;
import com.unboundid.scim.data.UserResource;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URI;

import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;



/**
 * Test coverage for the SCIM proxy plugin.
 */
@Test(sequential = true)
public class ProxyPluginTestCase extends ServerExtensionTestCase
{
  /**
   * The Directory Server external instance.
   */
  private DirectoryInstance dsInstance;

  /**
   * The Proxy Server external instance.
   */
  private ProxyInstance proxyInstance;

  /**
   * The SCIM service client.
   */
  private SCIMService service;

  /**
   * Base DN of the Directory Server.
   */
  private String baseDN;



  /**
   * Set up before running the tests.
   *
   * @throws Exception  If an error occurs.
   */
  @BeforeClass
  public void setup() throws Exception
  {
    final ExternalInstanceManager m = ExternalInstanceManager.singleton();

    dsInstance = m.getExternalInstance(ExternalInstanceId.BasicDirectoryServer);
    proxyInstance = m.getExternalInstance(ExternalInstanceId.ProxyOne);

    final File pluginZipFile = new File(System.getProperty("pluginZipFile"));
    installExtension(proxyInstance, pluginZipFile);

    dsInstance.startInstance();
    dsInstance.addBaseEntry();

    proxyInstance.runCommandExpectSuccess(
        "prepare-external-server",
        "--no-prompt",
        "--hostname", dsInstance.getLdapHost(),
        "--port", String.valueOf(dsInstance.getLdapPort()),
        "--bindDN", "cn=Directory Manager",
        "--bindPassword", "password",
        "--proxyBindDN", "cn=Proxy User,cn=Root DNs,cn=config",
        "--proxyBindPassword", "password",
        "--baseDN", "dc=example,dc=com"
    );

    baseDN = dsInstance.getPrimaryBaseDN();

    proxyInstance.startInstance();

    proxyInstance.dsconfig(
        "set-log-publisher-prop",
        "--publisher-name", "File-Based Access Logger",
        "--set", "suppress-internal-operations:false"
    );

    final String dsInstanceName = String.valueOf(dsInstance.getInstanceId());

    proxyInstance.dsconfig(
        new String[]{
            "create-external-server",
            "--server-name", dsInstanceName,
            "--type", "unboundid-ds",
            "--set", "server-host-name:" + dsInstance.getLdapHost(),
            "--set", "server-port:" + dsInstance.getLdapPort(),
            "--set", "bind-dn:cn=Proxy User",
            "--set", "password:password"
        },
        new String[]{
            "create-load-balancing-algorithm",
            "--algorithm-name", dsInstanceName,
            "--type", "single-server",
            "--set", "enabled:true",
            "--set", "backend-server:" + dsInstanceName
        },
        new String[]{
            "create-request-processor",
            "--processor-name", dsInstanceName,
            "--type", "proxying",
            "--set", "enabled:true",
            "--set", "load-balancing-algorithm:" + dsInstanceName
        },
        new String[]
        {
            "create-subtree-view",
            "--view-name", "test-view",
            "--set", "base-dn:" + baseDN,
            "--set", "request-processor:" + dsInstanceName
        },
        new String[]
        {
            "set-client-connection-policy-prop",
            "--policy-name", "default",
            "--add", "subtree-view:test-view"
        }
    );

    int scimPort = TestCaseUtils.getFreePort();
    int scimSecurePort = TestCaseUtils.getFreePort();
    configureExtension(proxyInstance, scimPort, scimSecurePort);

    final URI uri = new URI("http", null, proxyInstance.getLdapHost(), scimPort,
                            null, null, null);
    service = new SCIMService(uri, proxyInstance.getDirmanagerDN(),
                              proxyInstance.getDirmanagerPassword());

    //
    //This can be helpful if you want to make SCIM requests from your browser
    //or some other REST client:
    //
    //JOptionPane.showConfirmDialog(null, "SCIM is listening at: " +
    //        uri.toString(), null, JOptionPane.OK_CANCEL_OPTION);

  }



  /**
   * Tear down after the tests are finished.
   */
  @AfterClass
  public void tearDown()
  {
    //
    //This can be helpful if you want to attach a debugger to the external
    //instance that is running the SCIM server. Just wait for the dialog to pop
    //up, then navigate to the external instance root, stop it, modify the
    //java.properties to include -Xdebug, etc, and then start it back up. You
    //can leave the dialogue up until you are finished debugging, as it will
    //block this thread.
    //
    //JOptionPane.showConfirmDialog(null, "About to shutdown...", null,
    //                                JOptionPane.OK_CANCEL_OPTION);

    final ExternalInstanceManager m = ExternalInstanceManager.singleton();
    proxyInstance.stopInstance();
    dsInstance.stopInstance();
    m.destroyInstance(proxyInstance.getInstanceId());
    m.destroyInstance(dsInstance.getInstanceId());
  }



  /**
   * Tests retrieval of a simple user resource.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testRetrieve() throws Exception
  {
    //Add an entry to the Directory
    proxyInstance.addEntry("dn: uid=testRetrieve," + baseDN,
                           "objectclass: top",
                           "objectclass: person",
                           "objectclass: organizationalPerson",
                           "objectclass: inetOrgPerson",
                           "uid: testRetrieve",
                           "cn: testRetrieve",
                           "givenname: Test",
                           "sn: User",
                           "title: Chief of R&D",
                           "displayName: John Smith",
                           "mail: jsmith@example.com",
                           "employeeType: Engineer");

    //Try to retrieve the user via SCIM
    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    String beforeCount =
        getMonitorAsString(proxyInstance, "user-resource-get-successful");
    UserResource user = userEndpoint.get("uid=testRetrieve," + baseDN);
    String afterCount =
        getMonitorAsString(proxyInstance, "user-resource-get-successful");
    assertNotNull(user);
    assertTrue(user.getId().equalsIgnoreCase("uid=testRetrieve," + baseDN));
    assertEquals(user.getUserName(), "testRetrieve");
    assertEquals(user.getUserType(), "Engineer");
    assertEquals(user.getName().getFormatted(), "testRetrieve");
    assertEquals(user.getName().getGivenName(), "Test");
    assertEquals(user.getName().getFamilyName(), "User");
    assertEquals(user.getTitle(), "Chief of R&D");
    assertEquals(user.getDisplayName(), "John Smith");
    assertEquals(user.getEmails().iterator().next().getValue(),
                 "jsmith@example.com");

    //Make sure the stats were updated properly
    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 1);
  }
}
