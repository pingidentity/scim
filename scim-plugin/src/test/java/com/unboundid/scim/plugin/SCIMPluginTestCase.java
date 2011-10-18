/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.plugin;

import com.unboundid.directory.tests.standalone.ExternalInstance;
import com.unboundid.directory.tests.standalone.ExternalInstanceId;
import com.unboundid.directory.tests.standalone.ExternalInstanceManager;
import com.unboundid.directory.tests.standalone.TestCaseUtils;
import com.unboundid.scim.client.SCIMEndpoint;
import com.unboundid.scim.client.SCIMService;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.sdk.Version;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;



/**
 * Test coverage for the SCIM plugin.
 */
public class SCIMPluginTestCase extends ServerExtensionTestCase
{
  /**
   * The Directory Server external instance.
   */
  private ExternalInstance instance = null;

  /**
   * The SCIM service port.
   */
  private int scimPort;

  /**
   * The SCIM service client.
   */
  private SCIMService service;


  /**
   * Set up before each test method.
   * @throws Exception  If an error occurs.
   */
  @BeforeMethod
  public void setup() throws Exception
  {
    final ExternalInstanceManager m = ExternalInstanceManager.singleton();
    try
    {
      instance = m.getExternalInstance(ExternalInstanceId.BasicServer);
    }
    catch(RuntimeException e)
    {
      // This could occur if there are no external instance ZIPs found. In which
      // case, we can just let the tests pass as this was warned before.
      if(e.getMessage().equals("ExternalInstance must be provided with the " +
          "path to a DS zip file. This can be done by setting the dsZipPath " +
          "environment variable to the full path of the zip.  Or by setting " +
          "the JVM property dsZipPath to the full path of the zip."))
      {
        return;
      }
      else
      {
        throw e;
      }
    }

    final File pluginZipFile = new File(System.getProperty("pluginZipFile"));
    installExtension(instance, pluginZipFile);

    instance.startInstance();
    instance.addBaseEntry();

    scimPort = TestCaseUtils.getFreePort();
    configurePlugin(instance, "scim-plugin", scimPort);

    final URI uri = new URI("http", null, instance.getLdapHost(), scimPort,
                            null, null, null);
    service = new SCIMService(uri);
    service.setUserCredentials(instance.getDirmanagerDN(),
                               instance.getDirmanagerPassword());

  }



  /**
   * Tear down after each test method.
   */
  @AfterMethod
  public void tearDown()
  {
    final ExternalInstanceManager m = ExternalInstanceManager.singleton();
    instance.stopInstance();
    m.destroyInstance(instance.getInstanceId());
  }



  /**
   * Tests that the plugin can be installed and enabled.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void enablePlugin()
      throws Exception
  {
    assertEquals(getMonitorAsString(instance, "Version"), Version.VERSION);
  }



  /**
   * Tests Group resources and the groups attribute.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testGroups()
      throws Exception
  {
    final SCIMEndpoint<GroupResource> groupEndpoint =
        service.getGroupEndpoint();
    final SCIMEndpoint<UserResource> userEndpoint =
        service.getUserEndpoint();

    // Create some users.
    UserResource user1 = userEndpoint.newResource();
    user1.setUserName("user.1");
    user1.setName(new Name("User 1", "User", null, "Test", null, null));
    user1 = userEndpoint.insert(user1);

    // Create different kinds of groups.
    GroupResource groupOfUniqueNames = groupEndpoint.newResource();
    groupOfUniqueNames.setDisplayName("groupOfUniqueNames");
    final List<Entry<String>> members = new ArrayList<Entry<String>>(1);
    members.add(new Entry<String>(user1.getId(), null, false));
    groupOfUniqueNames.setMembers(members);
    groupOfUniqueNames = groupEndpoint.insert(groupOfUniqueNames);

    // Verify that the groups attribute is set correctly.
    user1 = userEndpoint.get(user1.getId());
    assertNotNull(user1.getGroups(), "User does not have the groups attribute");
    assertEquals(user1.getGroups().iterator().next().getValue(),
                 groupOfUniqueNames.getId());
  }
}
