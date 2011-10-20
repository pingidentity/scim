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
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.Version;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;



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
    instance.dsconfig(
        "set-log-publisher-prop",
        "--publisher-name", "File-Based Access Logger",
        "--set", "suppress-internal-operations:false"
    );
    instance.addBaseEntry();

    int scimPort = TestCaseUtils.getFreePort();
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
    if (instance == null)
    {
      return;
    }

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
    if (instance == null)
    {
      return;
    }

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
    if (instance == null)
    {
      return;
    }

    final String BASE_DN = instance.getPrimaryBaseDN();
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

    instance.getConnectionPool().add(
        generateGroupOfNamesEntry("groupOfNames", BASE_DN,
                                  user1.getId()));
    GroupResource groupOfNames =
        groupEndpoint.get("cn=groupOfNames," + BASE_DN);

    instance.addEntry(
        "dn: cn=groupOfURLs," + BASE_DN,
        "objectClass: groupOfURLs",
        "cn: groupOfURLs",
        "memberURL: ldap:///" + BASE_DN + "??sub?(sn=User)"
    );
    GroupResource groupOfURLs =
        groupEndpoint.get("cn=groupOfURLs," + BASE_DN);

    // Verify that the groups attribute is set correctly.
    user1 = userEndpoint.get(user1.getId());
    assertNotNull(user1.getGroups(), "User does not have the groups attribute");
    assertEquals(user1.getGroups().size(), 3,
                 "User has the wrong number of groups");
    final ArrayList<String> groups = new ArrayList<String>();
    // TODO cannot access display name in plurals
//    final ArrayList<String> displayNames = new ArrayList<String>();
    for (final Entry<String> groupEntry : user1.getGroups())
    {
      groups.add(groupEntry.getValue());
//      displayNames.add(groupEntry.getDisplay());
    }
    assertTrue(groups.contains(groupOfNames.getId()));
    assertTrue(groups.contains(groupOfUniqueNames.getId()));
    assertTrue(groups.contains(groupOfURLs.getId()));
//    assertTrue(displayNames.contains("groupOfNames"));
//    assertTrue(displayNames.contains("groupOfUniqueNames"));
//    assertTrue(displayNames.contains("groupOfURLs"));

    // Verify that the members attribute is set correctly.
    assertNotNull(groupOfNames.getMembers());
    assertEquals(groupOfNames.getMembers().size(), 1);
    assertEquals(groupOfNames.getMembers().iterator().next().getValue(),
                 user1.getId());

    assertNotNull(groupOfUniqueNames.getMembers());
    assertEquals(groupOfUniqueNames.getMembers().size(), 1);
    assertEquals(groupOfUniqueNames.getMembers().iterator().next().getValue(),
                 user1.getId());

    assertNotNull(groupOfURLs.getMembers());
    assertEquals(groupOfURLs.getMembers().size(), 1);
    assertEquals(groupOfURLs.getMembers().iterator().next().getValue(),
                 user1.getId());
  }



  /**
   * Provides test coverage for pagination.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testPagination()
      throws Exception
  {
    if (instance == null)
    {
      return;
    }

    final String BASE_DN = instance.getPrimaryBaseDN();

    // Create some users.
    final long NUM_USERS = 10;
    for (int i = 0; i < NUM_USERS; i++)
    {
      final String uid = "user." + i;
      instance.getConnectionPool().add(
          generateUserEntry(uid, BASE_DN,
                            "Test", "User", "password"));
    }

    // Fetch the users one page at a time with page size equal to 1.
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    int pageSize = 1;
    final Set<String> userIDs = new HashSet<String>();
    for (long startIndex = 1; startIndex <= NUM_USERS; startIndex += pageSize)
    {
      final Resources<UserResource> resources =
          userEndpoint.query(null, null,
                             new PageParameters(startIndex, pageSize));
      assertEquals(resources.getTotalResults(), NUM_USERS);
      assertEquals(resources.getStartIndex(), startIndex);
      assertEquals(resources.getItemsPerPage(), pageSize);
      for (final UserResource resource : resources)
      {
        userIDs.add(resource.getId());
      }
    }
    assertEquals(userIDs.size(), NUM_USERS);

    // Create some groups.
    final long NUM_GROUPS = 10;
    for (int i = 0; i < NUM_GROUPS; i++)
    {
      final String cn = "group." + i;
      instance.getConnectionPool().add(
          generateGroupOfNamesEntry(cn, BASE_DN, userIDs));
    }

    // Fetch the groups one page at a time with page size equal to 3.
    final SCIMEndpoint<GroupResource> groupEndpoint =
        service.getGroupEndpoint();
    pageSize = 3;
    final Set<String> groupIDs = new HashSet<String>();
    for (long startIndex = 1; startIndex <= NUM_GROUPS; startIndex += pageSize)
    {
      final Resources<GroupResource> resources =
          groupEndpoint.query("displayName sw 'group'", null,
                             new PageParameters(startIndex, pageSize));
      assertEquals(resources.getTotalResults(), NUM_GROUPS);
      assertEquals(resources.getStartIndex(), startIndex);
      int numResources = 0;
      for (final GroupResource resource : resources)
      {
        numResources++;
        groupIDs.add(resource.getId());
      }
      assertEquals(resources.getItemsPerPage(), numResources);
    }
    assertEquals(groupIDs.size(), NUM_GROUPS);

    // Attempt to fetch resources from a non-existent page.
    final long startIndex = NUM_GROUPS + 1;
    final Resources<GroupResource> resources =
        groupEndpoint.query(null, null,
                            new PageParameters(startIndex, pageSize));
    assertEquals(resources.getTotalResults(), NUM_GROUPS);
    assertEquals(resources.getItemsPerPage(), 0);
    assertEquals(resources.getStartIndex(), startIndex);
  }
}
