/*
 * Copyright 2011-2012 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import com.unboundid.directory.tests.externalinstance.ExternalInstanceManager;
import com.unboundid.directory.tests.externalinstance.ProxyInstance;
import com.unboundid.directory.tests.externalinstance.TestCaseUtils;
import com.unboundid.directory.tests.externalinstance.standalone.
    ExternalInstanceIdImpl;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;



/**
 * Test coverage for the SCIM server extension running in the Proxy Server.
 * The test methods are inherited from the SCIMExtensionTestCase.
 */
@Test(sequential = true)
public class ProxyExtensionTestCase extends SCIMExtensionTestCase
{
  /**
   * The Proxy Server external instance.
   */
  private ProxyInstance proxyInstance;



  /**
   * Set up before running the tests.
   *
   * @throws Exception  If an error occurs.
   */
  @BeforeClass(alwaysRun = true)
  @Override
  public void setup() throws Exception
  {
    final ExternalInstanceManager m = ExternalInstanceManager.singleton();

    dsInstance = m.getExternalInstance(
        ExternalInstanceIdImpl.BasicDirectoryServer);
    proxyInstance = m.getExternalInstance(ExternalInstanceIdImpl.ProxyOne);

    proxyInstance.runSetup("--ldapsPort",
                           String.valueOf(TestCaseUtils.getFreePort()),
                           "--generateSelfSignedCertificate",
                           "--doNotStart");

    final File extensionZipFile =
        new File(System.getProperty("extensionZipFile"));
    proxyInstance.installExtension(extensionZipFile);

    dsInstance.startInstance();

    dsInstance.dsconfig(
        "set-plugin-prop",
        "--plugin-name", "Processing Time Histogram",
        "--reset", "invoke-for-internal-operations");

    dsInstance.addBaseEntry();
    dsInstance.addEntry("dn: ou=people," + dsInstance.getPrimaryBaseDN(),
            "objectClass: organizationalUnit",
            "ou: people");
    dsInstance.dsconfig(
        "set-group-implementation-prop",
        "--implementation-name", "Static",
        "--set", "cache-user-to-group-mappings:false"
    );

    dsInstance.dsconfig(
        "set-virtual-attribute-prop",
        "--name", "Virtual Static member",
        "--set", "enabled:true",
        "--set", "allow-retrieving-membership:true"
    );

    dsInstance.dsconfig(
        "set-virtual-attribute-prop",
        "--name", "Virtual Static uniqueMember",
        "--set", "enabled:true",
        "--set", "allow-retrieving-membership:true"
    );

    dsInstance.restartInstance();

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

    groupBaseDN = dsInstance.getPrimaryBaseDN();
    userBaseDN = "ou=people," + dsInstance.getPrimaryBaseDN();

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
            "--set", "load-balancing-algorithm:" + dsInstanceName,
            // Need VLV and SORT control for SCIM.
            "--set", "supported-control-oid:2.16.840.1.113730.3.4.9",
            "--set", "supported-control-oid:1.2.840.113556.1.4.473"
        },
        new String[]
        {
            "create-subtree-view",
            "--view-name", "test-view",
            "--set", "base-dn:" + groupBaseDN,
            "--set", "request-processor:" + dsInstanceName
        },
        new String[]
        {
            "set-client-connection-policy-prop",
            "--policy-name", "default",
            "--add", "subtree-view:test-view"
        }
    );

    scimInstance = proxyInstance;
    setupServices();

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
  @AfterClass(alwaysRun = true)
  @Override
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



}
