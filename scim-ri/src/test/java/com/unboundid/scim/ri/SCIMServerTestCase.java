/*
 * Copyright 2011-2013 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */

package com.unboundid.scim.ri;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.scim.data.Address;
import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.AuthenticationScheme;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Manager;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.ServiceProviderConfig;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.BulkResponse;
import com.unboundid.scim.sdk.Diff;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.PreconditionFailedException;
import com.unboundid.scim.sdk.ResourceConflictException;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMService;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.Status;
import com.unboundid.scim.sdk.UnauthorizedException;
import com.unboundid.scim.wink.ResourceStats;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.unboundid.scim.sdk.SCIMConstants.SCHEMA_URI_CORE;
import static com.unboundid.scim.sdk.SCIMConstants.
    SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE;
import static java.util.Arrays.asList;


/**
 * This class provides test coverage for the SCIMServer class.
 */
public abstract class SCIMServerTestCase extends SCIMRITestCase
{
  /**
   * Provides test coverage for accessing a specific API version.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testAPIVersion()
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create a user directly on the test DS.
    testDS.add(generateUserEntry("b jensen", userBaseDN,
        "Barbara", "Jensen", "password",
        new Attribute("mail", "user.1@example.com"),
        new Attribute("l", "Austin"),
        new Attribute("postalCode", "78759")));

    SCIMService v1Service =
        new SCIMService(URI.create("http://localhost:"+getSSTestPort()+"/v1"),
            "cn=Manager", "password");
    SCIMEndpoint<UserResource> endpoint = v1Service.getUserEndpoint();

    // Determine the user ID.
    final String userID = endpoint.query(null).iterator().next().getId();

    // Fetch the user through the SCIM client.
    final UserResource user1 = endpoint.get(userID);
    assertNotNull(user1);
    assertEquals(user1.getId(), userID);
  }



  /**
   * Provides test coverage for the resource schema endpoint.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testResourceSchema()
      throws Exception
  {
    final SCIMEndpoint<ResourceDescriptor> schemaEndpoint =
        service.getResourceSchemaEndpoint();
    final ResourceDescriptor userDescriptor =
        schemaEndpoint.get(SCHEMA_URI_CORE +
            SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE +
            "User");

    // Make sure the convenience method works and returns the
    // same descriptor.
    assertEquals(service.getResourceDescriptor("User", SCHEMA_URI_CORE),
                 userDescriptor);

    // Make sure that we can use mixed case for the schema URN.
    assertEquals(
        service.getResourceDescriptor("User", "UrN:sCiM:ScHeMaS:cOrE:1.0"),
        userDescriptor);
    assertEquals(schemaEndpoint.get("UrN:sCiM:ScHeMaS:cOrE:1.0:User"),
                 userDescriptor);

    assertEquals(userDescriptor.getName(), "User");
    assertNotNull(userDescriptor.getDescription());
    assertEquals(userDescriptor.getSchema(), SCHEMA_URI_CORE);
    assertEquals(userDescriptor.getEndpoint(), "Users");
    assertTrue(userDescriptor.getAttributes().size() > 0);

    final ResourceDescriptor groupDescriptor =
        schemaEndpoint.get(SCHEMA_URI_CORE +
                           SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE +
                           "Group");
    // Make sure the convenience method works and returns the
    // same descriptor.
    assertEquals(groupDescriptor,
        service.getResourceDescriptor("Group", SCHEMA_URI_CORE));
    assertEquals(groupDescriptor.getName(), "Group");
    assertNotNull(groupDescriptor.getDescription());
    assertEquals(groupDescriptor.getSchema(), SCHEMA_URI_CORE);
    assertEquals(groupDescriptor.getEndpoint(), "Groups");
    assertTrue(groupDescriptor.getAttributes().size() > 0);

    Iterator<ResourceDescriptor> iterator =
        schemaEndpoint.query("name eq \"User\"").iterator();
    assertTrue(iterator.hasNext());
    assertEquals(iterator.next().getName(), "User");
    assertFalse(iterator.hasNext());

    final Resources<ResourceDescriptor> resources =
        schemaEndpoint.query(null, null, new PageParameters(1, 1),
                             "NAME", "attributes.name");
    assertTrue(resources.getTotalResults() >= 2);
    assertEquals(resources.getItemsPerPage(), 1);
    assertEquals(resources.getStartIndex(), 1);
    for (final ResourceDescriptor r : resources)
    {
      assertNotNull(r.getName());
      assertNull(r.getDescription());
//      for (final AttributeDescriptor a : r.getAttributes())
//      {
//        assertNotNull(a.getName());
//        assertNull(a.getDescription());
//      }
    }
  }



  /**
   * Provides test coverage for the GET operation on a user resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testGetUser()
      throws Exception
  {
    // Use custom resource mapping with descriptions for this test
    File f = getFile(
       "scim-ri/src/test/resources/resources-custom-tests.xml");
    assertTrue(f.exists());
    SCIMServerConfig config = new SCIMServerConfig();
    config.setResourcesFile(f);
    reconfigureTestSuite(config);

    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    SCIMEndpoint<UserResource> endpoint = service.getEndpoint(
            service.getResourceDescriptor("User", null),
            UserResource.USER_RESOURCE_FACTORY);

    // A user ID that does not exist should not return anything.
    try
    {
      endpoint.get("cn=does-not-exist");
      assertTrue(false, "Should have thrown ResourceNotFoundException");
    }
    catch(ResourceNotFoundException e)
    {
      // expected
    }

    // Create a user directly on the test DS.
    testDS.add(generateUserEntry("b jensen", userBaseDN,
        "Barbara", "Jensen", "password",
        new Attribute("mail", "user.1@example.com"),
        new Attribute("l", "Austin"),
        new Attribute("postalCode", "78759"),
        new Attribute("description", "first", "second", "third")
    ));

    // Determine the user ID.
    final String userID = endpoint.query(null).iterator().next().getId();

    // Fetch the user through the SCIM client.
    final UserResource user1 = endpoint.get(userID);
    assertNotNull(user1);
    assertEquals(user1.getId(), userID);
    assertNotNull(user1.getMeta());
    assertNotNull(user1.getMeta().getCreated());
    assertNotNull(user1.getMeta().getLastModified());
    assertNotNull(user1.getMeta().getLocation());
    assertNotNull(user1.getMeta().getVersion());
    assertLocation(user1.getMeta().getLocation(), "Users", userID);
    assertNotNull(user1.getUserName());
    assertEquals(user1.getUserName(), "b jensen");
    assertEquals(user1.getName().getFamilyName(), "Jensen");
    assertEquals(user1.getName().getGivenName(), "Barbara");
    assertNotNull(user1.getEmails());
    assertNotNull(user1.getAddresses());

    // Test to ensure that custom multi valued attributes return DS-8226
    Iterator<UserResource> customResults =
            endpoint.query(SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION +
                           ":descriptions.value eq \"second\"").iterator();
    assertTrue(customResults.hasNext());
    final UserResource user2 = customResults.next();
    Collection<com.unboundid.scim.data.Entry<String>> descriptions =
            user2.getAttributeValues(
                    SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION,
                    "descriptions",
                    com.unboundid.scim.data.Entry.STRINGS_RESOLVER);
    List<String> values = new ArrayList<String>(3);
    for (com.unboundid.scim.data.Entry entry : descriptions)
    {
      values.add((String)entry.getValue());
    }
    assertTrue(values.contains("first"));
    assertTrue(values.contains("second"));
    assertTrue(values.contains("third"));

    // Ensure that we can retrieve the user again using meta.location
    /*
    assertNotNull(client.getUserByURI(user1.getMeta().getLocation()));
    */

    // Fetch selected attributes only. (id and meta should always be returned)
    UserResource partialUser =
        endpoint.get(userID, null,
                     "USERNAME", "name.FORMATTED",
                     "addresses.postalCode",
                     "UrN:sCiM:ScHeMaS:cOrE:1.0:addresses.streetAddress");
    assertNotNull(partialUser);
    assertTrue(partialUser.getId().equals(userID));
    assertNotNull(partialUser.getMeta());
    assertNotNull(partialUser.getMeta().getCreated());
    assertNotNull(partialUser.getMeta().getLastModified());
    assertNotNull(partialUser.getMeta().getLocation());
    assertNotNull(partialUser.getMeta().getVersion());
    assertNotNull(partialUser.getUserName());
    assertNotNull(partialUser.getName());
    assertNotNull(partialUser.getName().getFormatted());
    assertNull(partialUser.getName().getFamilyName());
    assertNull(partialUser.getName().getGivenName());
    assertNull(partialUser.getEmails());
    assertNotNull(partialUser.getAddresses());
    for (final Address address : partialUser.getAddresses())
    {
      assertNull(address.getLocality());
      assertNotNull(address.getPostalCode());
    }

    // Fetch selected meta sub-attributes.
    partialUser = endpoint.get(userID, null, "meta.location");
    assertNotNull(partialUser);
    assertNotNull(partialUser.getId());
    assertNotNull(partialUser.getMeta());
    assertNull(partialUser.getMeta().getCreated());
    assertNull(partialUser.getMeta().getLastModified());
    assertNotNull(partialUser.getMeta().getLocation());
    assertLocation(user1.getMeta().getLocation(), "Users", userID);

    //Verify that we cannot obtain user1 through the Groups endpoint
    SCIMEndpoint<GroupResource> grpEndpoint = service.getGroupEndpoint();
    try
    {
      grpEndpoint.get(user1.getId());
      fail("Should not have found " + user1.getId() +
              " through the Groups endpoint");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }

    // Clean Up
    config.setResourcesFile(getFile("resource/resources.xml"));
    reconfigureTestSuite(config);
  }



  /**
   * Provides test coverage for the GET operation on a Group resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testGetGroup()
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create a static group directly on the test DS.
    testDS.add(generateUserEntry("bjensen", userBaseDN,
        "Barbara", "Jensen", "password"));
    testDS.add(generateGroupOfUniqueNamesEntry(
        "group1", "dc=example,dc=com",
        "uid=bjensen," + userBaseDN));

    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    final SCIMEndpoint<GroupResource> endpoint = service.getGroupEndpoint();

    // Determine the resource IDs.
    final String groupID = endpoint.query(null).iterator().next().getId();
    final String userID = userEndpoint.query(null).iterator().next().getId();

    // Fetch the Group through the SCIM client.
    final GroupResource group1 = endpoint.get(groupID);
    assertNotNull(group1);
    assertEquals(group1.getId(), groupID);
    assertEquals(group1.getDisplayName(), "group1");
    assertNotNull(group1.getMembers());
    assertEquals(group1.getMembers().iterator().next().getValue(), userID);
    assertNotNull(group1.getMeta());
    assertNotNull(group1.getMeta().getCreated());
    assertNotNull(group1.getMeta().getLastModified());
    assertNotNull(group1.getMeta().getLocation());
    assertLocation(group1.getMeta().getLocation(), "Groups", groupID);

    final UserResource user = userEndpoint.get(userID);
    assertNotNull(user.getGroups());
    assertEquals(user.getGroups().size(), 1);
    assertEquals(user.getGroups().iterator().next().getValue(),
                 groupID);

    //Verify that we cannot obtain group1 through the Users endpoint
    try
    {
      userEndpoint.get(group1.getId());
      fail("Should not have found " + group1.getId() +
              " through the Users endpoint");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }
  }



  /**
   * Provides test coverage for the GET operation to fetch selected users.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testGetUsers()
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create some users directly on the test DS.
    testDS.add(generateUserEntry("user.1", userBaseDN,
        "User", "One", "password",
        new Attribute("mail", "user.1@example.com"),
        new Attribute("l", "Austin"),
        new Attribute("postalCode", "78759")));
    testDS.add(generateUserEntry("user.2", userBaseDN,
        "User", "Two", "password"));

    // Fetch all the users through the SCIM client.
    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
    Resources<UserResource> resources = endpoint.query(null);

    assertEquals(resources.getTotalResults(), 2);
    for (final UserResource u : resources)
    {
      assertNotNull(u.getId());
      assertNotNull(u.getMeta());
      assertNotNull(u.getMeta().getCreated());
      assertNotNull(u.getMeta().getLastModified());
      assertNotNull(u.getMeta().getLocation());
      assertNotNull(u.getMeta().getVersion());
      assertNotNull(u.getName());
      assertNotNull(u.getName().getFamilyName());
      assertNotNull(u.getName().getGivenName());

      // Ensure that we can retrieve the user again using meta.location
      // assertNotNull(endpoint.get(u.getMeta().getLocation().toString()));
    }

    final String userID = resources.iterator().next().getId();
    resources = endpoint.query("id eq \"" + userID + "\"");
    assertEquals(resources.getTotalResults(), 1);

    resources = endpoint.query("userName eq \"user.1\"");
    assertEquals(resources.getTotalResults(), 1);

    resources = endpoint.query("UrN:sCiM:ScHeMaS:cOrE:1.0:USERNAME eq " +
                               "\"User.1\"");
    assertEquals(resources.getTotalResults(), 1);

    resources = endpoint.query("userName sw \"user\"");
    assertEquals(resources.getTotalResults(), 2);

    resources = endpoint.query("userName co \"1\"");
    assertEquals(resources.getTotalResults(), 1);

    resources = endpoint.query("userName pr");
    assertEquals(resources.getTotalResults(), 2);

    resources = endpoint.query("name.formatted eq \"User One\"");
    assertEquals(resources.getTotalResults(), 1);

    resources = endpoint.query("emails eq \"user.1@example.com\"");
    assertEquals(resources.getTotalResults(), 1);

    resources = endpoint.query("addresses.locality eq \"Austin\"");
    assertEquals(resources.getTotalResults(), 1);

    // Fetch selected attributes.
    resources = endpoint.query("addresses.LOCALITY eq \"Austin\"",
                               null, null,
                               "USERNAME", "name.FORMATTED",
                               "addresses.postalCode",
                               "addresses.streetAddress",
                               "meta.location");
    assertEquals(resources.getTotalResults(), 1);
    for (final UserResource u : resources)
    {
      assertNotNull(u.getId());
      assertNotNull(u.getMeta());
      assertNull(u.getMeta().getCreated());
      assertNull(u.getMeta().getLastModified());
      assertNotNull(u.getMeta().getLocation());
      assertLocation(u.getMeta().getLocation(), "Users", u.getId());
      assertNotNull(u.getUserName());
      assertNotNull(u.getName());
      assertNotNull(u.getName().getFormatted());
      assertNull(u.getName().getFamilyName());
      assertNull(u.getName().getGivenName());
      assertNotNull(u.getAddresses());
      for (final Address address : u.getAddresses())
      {
        assertNull(address.getLocality());
        assertNotNull(address.getPostalCode());
      }
      assertNull(u.getEmails());
    }
  }



  /**
   * Provides test coverage for the GET operation to fetch sorted users.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testGetSortedUsers()
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create some users directly on the test DS.
    for (final String sortValue : Arrays.asList("B", "C", "A"))
    {
      testDS.add(generateUserEntry(sortValue, userBaseDN,
          "User", sortValue, "password"));
    }

    final String sortBy = "UrN:sCiM:ScHeMaS:cOrE:1.0:userName";
    final String sortAscending = "ascending";
    final String sortDescending = "descending";

    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
    Resources<UserResource> resources = endpoint.query(null,
        new SortParameters(sortBy, sortAscending), null);
    List<String> sortValues = new ArrayList<String>();
    for (final UserResource resource : resources)
    {
      sortValues.add(resource.getUserName());
    }
    assertEquals(sortValues, Arrays.asList("A", "B", "C"));

    resources = endpoint.query(null,
        new SortParameters(sortBy, sortDescending), null);
    sortValues = new ArrayList<String>();
    for (final UserResource resource : resources)
    {
      sortValues.add(resource.getUserName());
    }
    assertEquals(sortValues, Arrays.asList("C", "B", "A"));
  }



  /**
   * Provides test coverage for the write operations on a group resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testWriteGroup()
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();

    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final String idUser1 = userEndpoint.create(
        userEndpoint.newResource().setUserName("user.1").setName(
            new Name("Test User", "User", null, "Test", null, null))).getId();
    final String idUser2 = userEndpoint.create(
        userEndpoint.newResource().setUserName("user.2").setName(
            new Name("Test User", "User", null, "Test", null, null))).getId();

    final String dnGroupA = "cn=group A,dc=example,dc=com";
    final String dnGroupB = "cn=group B,dc=example,dc=com";

    // Create the contents for the groups.
    GroupResource groupA = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    groupA.setDisplayName("group A");
    final Collection<com.unboundid.scim.data.Entry<String>> membersA =
        new ArrayList<com.unboundid.scim.data.Entry<String>>(1);
    final com.unboundid.scim.data.Entry<String> member1 =
        new com.unboundid.scim.data.Entry<String>(
            idUser1, "User", false);
    membersA.add(member1);
    groupA.setMembers(membersA);

    GroupResource groupB = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    groupB.setDisplayName("group B");

    SCIMEndpoint<GroupResource> groupEndpoint = service.getGroupEndpoint();
    // Post the new groups.
    groupA = groupEndpoint.create(groupA);
    assertNotNull(groupA);
    Entry entry = testDS.getEntry(dnGroupA);
    assertTrue(entry.hasAttributeValue("uniqueMember",
                                       "uid=user.1," + userBaseDN));

    groupB = groupEndpoint.create(groupB);
    assertNotNull(groupB);

    // Add a value that should be preserved during SCIM updates.
    testDS.modify(dnGroupA,
        new Modification(ModificationType.ADD, "description",
            "This value should be preserved"));
    testDS.modify(dnGroupB,
                  new Modification(ModificationType.ADD, "description",
                                   "This value should be preserved"));

    // Get the latest version
    groupA = groupEndpoint.get(groupA.getId());
    groupB = groupEndpoint.get(groupB.getId());

    // Add some members to each group.
    final com.unboundid.scim.data.Entry<String> member2 =
        new com.unboundid.scim.data.Entry<String>(
            idUser2, "User", false);
    Collection<com.unboundid.scim.data.Entry<String>> newMembers =
        groupA.getMembers();
    newMembers.add(member2);
    groupA.setMembers(newMembers);

    final com.unboundid.scim.data.Entry<String> memberA =
        new com.unboundid.scim.data.Entry<String>(
            groupA.getId(), "Group", false);
    newMembers = new ArrayList<com.unboundid.scim.data.Entry<String>>(1);
    newMembers.add(memberA);
    groupB.setMembers(newMembers);

    // Put the updated groups.
    groupA = groupEndpoint.update(groupA);
    groupB = groupEndpoint.update(groupB);

    assertEquals(groupA.getMembers().size(), 2);
    Iterator<com.unboundid.scim.data.Entry<String>> i =
        groupA.getMembers().iterator();
    assertEquals(i.next().getValue(), idUser1);
    assertEquals(i.next().getValue(), idUser2);

    assertEquals(groupB.getMembers().size(), 1);
    assertTrue(groupB.getMembers().iterator().next().getValue().
        equals(groupA.getId()));

    assertNotNull(groupA.getMeta().getLastModified());
    assertNotNull(groupA.getMeta().getLocation());
    assertNotNull(groupB.getMeta().getLastModified());
    assertNotNull(groupB.getMeta().getLocation());

    // Verify that the LDAP entries were updated correctly.
    entry = testDS.getEntry(dnGroupA);
    assertTrue(entry.hasAttributeValue("uniqueMember",
                                       "uid=user.1," + userBaseDN));
    assertTrue(entry.hasAttributeValue("uniqueMember",
                                       "uid=user.2," + userBaseDN));
    assertTrue(entry.hasAttribute("description"));

    entry = testDS.getEntry(dnGroupB);
    assertTrue(entry.hasAttributeValue("uniqueMember", dnGroupA));
    assertTrue(entry.hasAttribute("description"));

    groupA = groupEndpoint.get(groupA.getId());
    groupB = groupEndpoint.get(groupB.getId());

    // Delete the groups.
    groupEndpoint.delete(groupB);
    groupEndpoint.delete(groupA);

    assertNull(testDS.getEntry(dnGroupA));
    assertNull(testDS.getEntry(dnGroupB));
  }



  /**
   * Provides test coverage for PUT and DELETE operations on a user resource
   * invoking the operations using POST with a method override.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testMethodOverride()
      throws Exception
  {
    // Tell the client to use method override.
    service.setOverridePut(true);
    service.setOverrideDelete(true);

    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create the contents for a new user.
    final UserResource user = new UserResource(CoreSchema.USER_DESCRIPTOR);
    final Name name = new Name("Ms. Barbara J Jensen III", "Jensen", null,
        null, null, null);
    user.setUserName("bjensen");
    user.setName(name);

    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
    // Post a new user.
    UserResource user1 = endpoint.create(user);
    assertNotNull(user1);

    // Add some values to the user.
    Name newName = user1.getName();
    newName.setGivenName("Barbara");
    user1.setName(newName);

    // Put the updated user.
    user1 = endpoint.update(user1);

    // Delete the user.
    endpoint.delete(user1);
  }



  /**
   * Retrieve the user resource that has the provided userName value.
   *
   * @param userName  The userName of the user to be retrieved.
   *
   * @return  The user resource.
   *
   * @throws SCIMException  If the resource could not be retrieved.
   */
  private UserResource getUser(final String userName)
      throws SCIMException
  {
    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();

    final Resources<UserResource> resources =
        endpoint.query("userName eq \"" + userName + "\"");

    return resources.iterator().next();
  }



  /**
   * Tests modifying a user password through SCIM using modify.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testPasswordModify() throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    testDS.add("dn: uid=testPasswordModify," + userBaseDN,
        "objectclass: top",
        "objectclass: person",
        "objectclass: organizationalPerson",
        "objectclass: inetOrgPerson",
        "uid: testPasswordModify",
        "userPassword: oldPassword",
        "cn: testPasswordModify",
        "givenname: Test",
        "sn: User");

    //Update the entry via SCIM
    SCIMService userService = createSCIMService("uid=testPasswordModify," +
        userBaseDN, "oldPassword");
    SCIMEndpoint<UserResource> userEndpoint = userService.getUserEndpoint();
    UserResource user = getUser("testPasswordModify");
    assertNotNull(user);

    //Verify that not including the password attribute in the PUT will not
    //affect the current value
    user.setPassword(null);
    user.setTitle("Engineer");
    user = userEndpoint.update(user);
    assertNotNull(user);
    assertEquals(user.getTitle(), "Engineer");
    assertEquals(testDS.bind("uid=testPasswordModify," + userBaseDN,
        "oldPassword").getResultCode(), ResultCode.SUCCESS);

    //Now change the password
    user.setPassword("anotherPassword");

    UserResource returnedUser = userEndpoint.update(user);

    //Verify what is returned from the SDK
    assertEquals(returnedUser.getId(), user.getId());
    assertNull(returnedUser.getPassword());

    //We shouldn't be able to use this service anymore since it is using
    //the old credentials
    try
    {
      userEndpoint.get(user.getId());
      assertTrue(false, "Expected Unauthorized return code");
    }
    catch(SCIMException e)
    {
      // Expected.
    }

    //Verify the password was changed in the Directory
    assertEquals(testDS.bind("uid=testPasswordModify," + userBaseDN,
        "anotherPassword").getResultCode(), ResultCode.SUCCESS);
  }



  /**
   * Tests basic resource creation through SCIM.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testPostUser() throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    //Create a new user
    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    UserResource manager = userEndpoint.newResource();
    manager.setName(
        new Name("Mr. Manager", "Manager", null, null, "Mr.", null));

    // Try to create the user with a missing required attribute
    try
    {
      userEndpoint.create(manager);
      fail("Expected a 400 response when trying to create user with " +
          "missing required attr");
    }
    catch (InvalidResourceException e)
    {
      // expected (error code is 400)
      // Add missing Attr
      manager.setUserName("myManager");
    }

    // Try to create the user with a read only CORE attribute (groups)
    Collection<com.unboundid.scim.data.Entry<String>> groups =
            new HashSet<com.unboundid.scim.data.Entry<String>>(1);
    groups.add(
            new com.unboundid.scim.data.Entry<String>("managers", "work", true)
    );
    manager.setGroups(groups);
    try
    {
      userEndpoint.create(manager);
      fail("Expected a 400 response when trying to create user with " +
          "read only attr from core schema");
    }
    catch (InvalidResourceException e)
    {
      // Expect failure due to read only
      assertTrue(e.getMessage().contains("read only"));
      // Remove read only attr
      manager.setGroups(null);
    }

    manager = userEndpoint.create(manager);

    UserResource user = userEndpoint.newResource();
    user.setUserName("jdoe");
    user.setName(
        new Name("John C. Doe", "Doe", "Charles", "John", null, "Sr."));
    user.setPassword("newPassword");
    user.setTitle("Vice President");
    user.setUserType("Employee");
    Collection<com.unboundid.scim.data.Entry<String>> emails =
        new HashSet<com.unboundid.scim.data.Entry<String>>(1);
    emails.add(new com.unboundid.scim.data.Entry<String>(
        "j.doe@example.com", "work", true));
    user.setEmails(emails);
    user.setLocale(Locale.US.getDisplayName());
    user.setTimeZone(TimeZone.getDefault().getID());

    // Set a missing required sub-attribute (managerId)
    user.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER,
            new Manager(null, "Mr. Manager"));

    //Verify basic properties of UserResource
    assertEquals(user.getUserName(), "jdoe");
    assertEquals(user.getName().getFormatted(), "John C. Doe");
    assertEquals(user.getPassword(), "newPassword");
    assertEquals(user.getTitle(), "Vice President");
    assertEquals(user.getUserType(), "Employee");
    assertEquals(user.getEmails().iterator().next().getValue(),
                          "j.doe@example.com");
    assertEquals(user.getLocale(), Locale.US.getDisplayName());
    assertEquals(user.getTimeZone(), TimeZone.getDefault().getID());
    assertEquals(user.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER).getDisplayName(), "Mr. Manager");

    // Try to create the user with a missing required sub-attribute
    long beforeCount =
        getStatsForResource("User").getStat(ResourceStats.POST_BAD_REQUEST);
    try
    {
      userEndpoint.create(user);
      fail("Expected a 400 response when trying to create user with " +
          "missing required attr");
    }
    catch (InvalidResourceException e)
    {
      // expected (error code is 400)
    }
    long afterCount =
        getStatsForResource("User").getStat(ResourceStats.POST_BAD_REQUEST);
    assertEquals(beforeCount, afterCount - 1);

    //Try to create the user with an invalid manager id
    user.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER, new Manager("fake-id", "Mr. Manager"));
    beforeCount = getStatsForResource("User").getStat(
            ResourceStats.POST_BAD_REQUEST);
    try
    {
      userEndpoint.create(user);
      fail("Expected a 400 response when trying to create user with " +
              "invalid manager id");
    }
    catch(InvalidResourceException e)
    {
      // expected (error code is 400)
    }
    afterCount = getStatsForResource("User").getStat(
            ResourceStats.POST_BAD_REQUEST);
    assertEquals(beforeCount, afterCount - 1);

    user.setSingularAttributeValue(
        SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
        Manager.MANAGER_RESOLVER,
        new Manager(manager.getId(), "Mr. Manager"));

    //Mark the start and end time with a 500ms buffer on either side, because
    //the Directory Server will record the actual createTimestamp using the
    //TimeThread, which only updates once every 100ms.
    Date startTime = new Date(System.currentTimeMillis() - 500);
    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.POST_OK);
    UserResource returnedUser = userEndpoint.create(user);
    afterCount =
        getStatsForResource("User").getStat(ResourceStats.POST_OK);
    Date createTime = returnedUser.getMeta().getCreated();
    Date endTime = new Date(System.currentTimeMillis() + 500);

    try
    {
      //Try to create the same user again
      userEndpoint.create(user);
      fail("Expected a 409 response when trying to create duplicate user");
    }
    catch (ResourceConflictException e)
    {
      //expected (error code is 409)
    }

    //Verify what is returned from the SDK
    assertNotNull(returnedUser);
    assertTrue(returnedUser.getMeta().getLocation().toString().endsWith(
        returnedUser.getId()));
    assertEquals(returnedUser.getUserName(), user.getUserName());
    assertEquals(returnedUser.getName().getFormatted(),
                   user.getName().getFormatted());
    assertNull(returnedUser.getPassword());
    assertEquals(returnedUser.getTitle(), user.getTitle());
    assertEquals(returnedUser.getUserType(), user.getUserType());
    assertEquals(returnedUser.getEmails().iterator().next().getValue(),
        user.getEmails().iterator().next().getValue());
    assertTrue(createTime.after(startTime));
    assertTrue(createTime.before(endTime));
    assertEquals(beforeCount, afterCount - 1);

    //TODO: no LDAP schema for Locale
    //assertEquals(returnedUser.getLocale(), user.getLocale());

    //TODO: no LDAP schema for TimeZone
    //assertEquals(returnedUser.getTimeZone(), user.getTimeZone());

    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER).getManagerId(),
                 user.getSingularAttributeValue(
                   SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
                   Manager.MANAGER_RESOLVER).getManagerId());

    //Verify what is actually in the Directory
    SearchResultEntry entry = testDS.getEntry(
                                          "uid=jdoe," + userBaseDN, "*", "+");
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("iNetOrgPerson"));
    assertEquals(entry.getAttributeValue("cn"), "John C. Doe");
    assertEquals(entry.getAttributeValue("givenName"), "John");
    assertEquals(entry.getAttributeValue("sn"), "Doe");
    assertEquals(entry.getAttributeValue("title"), "Vice President");
    assertEquals(entry.getAttributeValue("mail"), "j.doe@example.com");
    assertTrue(entry.getAttributeValue("manager").equalsIgnoreCase(
        "uid=myManager," + userBaseDN));
    assertEquals(testDS.bind(entry.getDN(), "newPassword").getResultCode(),
        ResultCode.SUCCESS);

    // Verify that a query returns all attributes including extension
    // attributes.
    returnedUser = userEndpoint.query("userName eq \"jdoe\"").iterator().next();
    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER).getManagerId(),
                 user.getSingularAttributeValue(
                   SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
                   Manager.MANAGER_RESOLVER).getManagerId());

    //Create a new group
    SCIMEndpoint<GroupResource> grpEndpoint = service.getGroupEndpoint();
    GroupResource group = grpEndpoint.newResource();
    group.setDisplayName("Engineering");
    Collection<com.unboundid.scim.data.Entry<String>> members =
        new HashSet<com.unboundid.scim.data.Entry<String>>();
    members.add(new com.unboundid.scim.data.Entry<String>(
        returnedUser.getId(), "User", false));
    group.setMembers(members);

    //Verify the basic properties of GroupResource
    assertEquals(group.getDisplayName(), "Engineering");
    assertEquals(group.getMembers().iterator().next().getValue(),
                          returnedUser.getId());

    //Do the create and verify what is returned from the endpoint
    startTime = new Date(System.currentTimeMillis() - 500);
    beforeCount =
        getStatsForResource("Group").getStat(ResourceStats.POST_OK);
    GroupResource returnedGroup = grpEndpoint.create(group);
    afterCount =
        getStatsForResource("Group").getStat(ResourceStats.POST_OK);
    createTime = returnedGroup.getMeta().getCreated();
    endTime = new Date(System.currentTimeMillis() + 500);

    assertNotNull(returnedGroup);
    assertEquals(returnedGroup.getDisplayName(), group.getDisplayName());
    assertTrue(createTime.after(startTime));
    assertTrue(createTime.before(endTime));
    assertEquals(returnedGroup.getMembers().iterator().next().getValue(),
        group.getMembers().iterator().next().getValue());
    assertEquals(beforeCount, afterCount - 1);

    //Verify what is actually in the Directory
    SearchResultEntry groupEntry = testDS.getEntry(
        "cn=Engineering," + groupBaseDN, "*", "+");
    assertNotNull(groupEntry);
    assertTrue(groupEntry.hasObjectClass("groupOfUniqueNames"));
    assertEquals(groupEntry.getAttributeValue("cn"), "Engineering");
    assertTrue(groupEntry.getAttributeValue("uniqueMember").
        equalsIgnoreCase(entry.getDN()), entry.toLDIFString());
  }



  /**
   * Tests a basic modify operation against a user resource using PUT.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testPutUser() throws Exception
  {
    // Use custom resource mapping with read only department for this test
    File f = getFile(
       "scim-ri/src/test/resources/resources-custom-tests.xml");
    assertTrue(f.exists());
    SCIMServerConfig config = new SCIMServerConfig();
    config.setResourcesFile(f);
    reconfigureTestSuite(config);

     // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    //Add an entry to the Directory
    testDS.add("dn: uid=testModifyWithPut," + userBaseDN,
                      "objectclass: top",
                      "objectclass: person",
                      "objectclass: organizationalPerson",
                      "objectclass: inetOrgPerson",
                      "uid: testModifyWithPut",
                      "userPassword: oldPassword",
                      "cn: testModifyWithPut",
                      "givenname: Test",
                      "sn: User",
                      "departmentNumber: 42");

    //Update the entry via SCIM
    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    UserResource user = getUser("testModifyWithPut");
    assertNotNull(user);
    //This will change the 'cn' to 'Test User'
    user.setName(new Name("Test User", "User", null, "Test", null, null));
    user.setUserType("Employee");
    user.setPassword("anotherPassword");
    user.setTitle("Chief of Operations");
    user.setDisplayName("Test Modify with PUT");
    user.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
              AttributeValueResolver.STRING_RESOLVER, "456");

    //Mark the start and end time with a 500ms buffer on either side, because
    //the Directory Server will record the actual modifyTimestamp using the
    //TimeThread, which only updates once every 100ms.
    Date startTime = new Date(System.currentTimeMillis() - 500);
    UserResource returnedUser = userEndpoint.update(user);
    Date lastModified = returnedUser.getMeta().getLastModified();
    Date endTime = new Date(System.currentTimeMillis() + 500);

    //Verify what is returned from the SDK
    assertEquals(returnedUser.getId(), user.getId());
    assertTrue(user.getMeta().getLocation().toString().endsWith(user.getId()));
    assertEquals(returnedUser.getUserName(), "testModifyWithPut");
    assertEquals(returnedUser.getName().getFormatted(), "Test User");
    assertEquals(returnedUser.getName().getGivenName(), "Test");
    assertEquals(returnedUser.getName().getFamilyName(), "User");
    assertNull(returnedUser.getPassword());
    assertEquals(returnedUser.getTitle(), user.getTitle());
    assertEquals(returnedUser.getDisplayName(), user.getDisplayName());
    assertTrue(lastModified.after(startTime));
    assertTrue(lastModified.before(endTime));
    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
              AttributeValueResolver.STRING_RESOLVER), "456");
    assertEquals(returnedUser.getSingularAttributeValue(
                SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "department",
                  AttributeValueResolver.STRING_RESOLVER), "42");

    //Verify the contents of the entry in the Directory
    SearchResultEntry entry =
      testDS.getEntry(
          "uid=testModifyWithPut," + userBaseDN);
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("inetOrgPerson"));
    assertEquals(entry.getAttributeValue("uid"), "testModifyWithPut");
    assertEquals(entry.getAttributeValue("cn"), "Test User");
    assertEquals(entry.getAttributeValue("givenname"), "Test");
    assertEquals(entry.getAttributeValue("sn"), "User");
    assertEquals(entry.getAttributeValue("title"), "Chief of Operations");
    assertEquals(entry.getAttributeValue("employeeNumber"), "456");
    assertEquals(entry.getAttributeValue("displayName"),
                                                   "Test Modify with PUT");
    assertEquals(testDS.bind(entry.getDN(), "anotherPassword").getResultCode(),
        ResultCode.SUCCESS);

    //Make the exact same update again; this should result in no net change to
    //the Directory entry, but should not fail.
    long beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PUT_OK);
    returnedUser = userEndpoint.update(returnedUser);
    long afterCount =
        getStatsForResource("User").getStat(ResourceStats.PUT_OK);

    assertEquals(beforeCount, afterCount - 1);

    // Try to put the user WITH a read only attribute included
    String origDeptValue = returnedUser.getSingularAttributeValue(
                        SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION,
                        "department", AttributeValueResolver.STRING_RESOLVER);
    assertEquals(origDeptValue, "42");
    returnedUser.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION,
            "department", AttributeValueResolver.STRING_RESOLVER, "69");
    try
    {
      // Try putting with no etag (for backward compatibility)
      userEndpoint.update(returnedUser.getId(), null, returnedUser);
    }
    catch (InvalidResourceException e)
    {
      e.printStackTrace();
      fail("Expected success when doing PUT with read only attribute but " +
                   "got exception: " + e.toString());
    }
    try
    {
      // Try putting with wildcard etag
      userEndpoint.update(returnedUser.getId(), "*, \"123\"", returnedUser);
    }
    catch (InvalidResourceException e)
    {
      e.printStackTrace();
      fail("Expected success when doing PUT with read only attribute but " +
                   "got exception: " + e.toString());
    }
    // Verify value did not change
    user = getUser("testModifyWithPut");
    assertEquals(user.getSingularAttributeValue(
                    SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "department",
                    AttributeValueResolver.STRING_RESOLVER), origDeptValue);

    // Try to put the user WITHOUT a read only attribute included
    returnedUser.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION,
            "department", AttributeValueResolver.STRING_RESOLVER, null);
    try
    {
      // Try putting with multiple etags
      userEndpoint.update(returnedUser.getId(),
          "\"123\"," + returnedUser.getMeta().getVersion(), returnedUser);
    }
    catch (InvalidResourceException e)
    {
      e.printStackTrace();
      fail("Expected success when doing PUT with read only attribute but " +
                   "got exception: " + e.toString());
    }
    // Verify value did not change
    user = getUser("testModifyWithPut");
    assertEquals(user.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "department",
            AttributeValueResolver.STRING_RESOLVER), origDeptValue);
    returnedUser.setSingularAttributeValue(
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION,
          "department", AttributeValueResolver.STRING_RESOLVER, origDeptValue);

    // Try to put user with non-matching etag
    beforeCount =
        getStatsForResource("User").getStat(
            ResourceStats.PUT_PRECONDITION_FAILED);
    try
    {
      userEndpoint.update(user.getId(), "\"123\"", user);
      fail("Expected PreconditionFailedException when putting with " +
          "non-matching etag");
    }
    catch(PreconditionFailedException e)
    {
      //expected
    }
    afterCount =
        getStatsForResource("User").getStat(
            ResourceStats.PUT_PRECONDITION_FAILED);
    assertEquals(beforeCount, afterCount - 1);

    // Try to put the user with a missing required attribute
    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PUT_BAD_REQUEST);
    returnedUser.setUserName(null);
    try
    {
      userEndpoint.update(returnedUser);
      fail("Expected a 400 response when trying to create user with " +
          "missing required attr");
    }
    catch (InvalidResourceException e)
    {
      // expected (error code is 400)
    }
    afterCount =
        getStatsForResource("User").getStat(ResourceStats.PUT_BAD_REQUEST);
    assertEquals(beforeCount, afterCount - 1);

    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PUT_NOT_FOUND);
    try
    {
      //Try to update an entry that doesn't exist
      user.setId("uid=fakeUserName," + userBaseDN);
      userEndpoint.update(user);
      fail("Expected ResourceNotFoundException when updating " +
            "non-existent user");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }
    afterCount =
        getStatsForResource("User").getStat(ResourceStats.PUT_NOT_FOUND);
    assertEquals(beforeCount, afterCount - 1);

    // Clean Up
    config.setResourcesFile(getFile("resource/resources.xml"));
    reconfigureTestSuite(config);
  }



  /**
   * Tests a basic modify operation against a user resource by generating a
   * diff and using a PATCH request.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testPatchUserWithDiff() throws Exception
  {
     // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    //Add an entry to the Directory
    testDS.add("dn: uid=testModifyWithPut," + userBaseDN,
                      "objectclass: top",
                      "objectclass: person",
                      "objectclass: organizationalPerson",
                      "objectclass: inetOrgPerson",
                      "uid: testModifyWithPut",
                      "userPassword: oldPassword",
                      "cn: testModifyWithPut",
                      "givenname: Test",
                      "sn: User");

    //Update the entry via SCIM
    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    UserResource sourceUser = getUser("testModifyWithPut");
    UserResource targetUser = getUser("testModifyWithPut");
    assertNotNull(sourceUser);
    //This will change the 'cn' to 'Test User'
    targetUser.setName(new Name("Test User", "User", null, "Test", null, null));
    targetUser.setUserType("Employee");
    targetUser.setPassword("anotherPassword");
    targetUser.setTitle("Chief of Operations");
    targetUser.setDisplayName("Test Modify with PUT");
    targetUser.setSingularAttributeValue(
        SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
        AttributeValueResolver.STRING_RESOLVER, "456");

    //Generate the diff
    Diff<UserResource> diff = Diff.generate(sourceUser, targetUser);

    //Mark the start and end time with a 500ms buffer on either side, because
    //the Directory Server will record the actual modifyTimestamp using the
    //TimeThread, which only updates once every 100ms.
    Date startTime = new Date(System.currentTimeMillis() - 500);
    userEndpoint.update(sourceUser, diff.getAttributesToUpdate(),
        diff.getAttributesToDelete());
    UserResource returnedUser = getUser("testModifyWithPut");
    Date lastModified = returnedUser.getMeta().getLastModified();
    Date endTime = new Date(System.currentTimeMillis() + 500);

    //Verify what is returned from the SDK
    assertEquals(returnedUser.getId(), targetUser.getId());
    assertTrue(targetUser.getMeta().getLocation().toString().endsWith(
        targetUser.getId()));
    assertEquals(returnedUser.getUserName(), "testModifyWithPut");
    assertEquals(returnedUser.getName().getFormatted(), "Test User");
    assertEquals(returnedUser.getName().getGivenName(), "Test");
    assertEquals(returnedUser.getName().getFamilyName(), "User");
    assertNull(returnedUser.getPassword());
    assertEquals(returnedUser.getTitle(), targetUser.getTitle());
    assertEquals(returnedUser.getDisplayName(), targetUser.getDisplayName());
    assertTrue(lastModified.after(startTime));
    assertTrue(lastModified.before(endTime));
    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
              AttributeValueResolver.STRING_RESOLVER), "456");

    //Verify the contents of the entry in the Directory
    SearchResultEntry entry =
      testDS.getEntry(
          "uid=testModifyWithPut," + userBaseDN);
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("inetOrgPerson"));
    assertEquals(entry.getAttributeValue("uid"), "testModifyWithPut");
    assertEquals(entry.getAttributeValue("cn"), "Test User");
    assertEquals(entry.getAttributeValue("givenname"), "Test");
    assertEquals(entry.getAttributeValue("sn"), "User");
    assertEquals(entry.getAttributeValue("title"), "Chief of Operations");
    assertEquals(entry.getAttributeValue("employeeNumber"), "456");
    assertEquals(entry.getAttributeValue("displayName"),
                                                   "Test Modify with PUT");
    assertEquals(testDS.bind(entry.getDN(), "anotherPassword").getResultCode(),
        ResultCode.SUCCESS);
  }



  /**
   * Tests a basic modify operation against a User using PATCH.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testPatchUser()
      throws Exception
  {
     // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    //Add an entry to the Directory
    testDS.add("dn: uid=testModifyWithPatch," + userBaseDN,
        "objectclass: top",
        "objectclass: person",
        "objectclass: organizationalPerson",
        "objectclass: inetOrgPerson",
        "uid: testModifyWithPatch",
        "userPassword: oldPassword",
        "cn: testModifyWithPatch",
        "givenname: Test",
        "sn: User",
        "telephoneNumber: 512-123-4567",
        "homePhone: 972-987-6543",
        "mail: testEmail.1@example.com",
        "mail: testEmail.2@example.com");

    //Update the entry via SCIM
    UserResource user = getUser("testModifyWithPatch");
    assertNotNull(user);

    //This will change the 'cn' to 'Test User'
    user.setName(new Name("Test User", "User", null, "Test", null, null));

    //Other simple attributes to patch
    user.setUserType("Employee");
    user.setPassword("anotherPassword");
    user.setTitle("Chief of Operations");
    user.setDisplayName("Test Modify with PATCH");

    //Try an attribute in the extended schema
    user.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
            AttributeValueResolver.STRING_RESOLVER, "456");

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    List<SCIMAttribute> attrsToUpdate = new ArrayList<SCIMAttribute>(6);
    final SCIMObject scimObject = user.getScimObject();

    attrsToUpdate.add(
          scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "name"));
    attrsToUpdate.add(
          scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "userType"));
    attrsToUpdate.add(
          scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "password"));
    attrsToUpdate.add(
          scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "title"));
    attrsToUpdate.add(
          scimObject.getAttribute(
              SCIMConstants.SCHEMA_URI_CORE, "displayName"));
    attrsToUpdate.add(
          scimObject.getAttribute(
              SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber"));

    user = userEndpoint.update(user, attrsToUpdate, null);

    //Verify the contents of the entry in the Directory
    SearchResultEntry entry = testDS.getEntry(
                    "uid=testModifyWithPatch," + userBaseDN);
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("inetOrgPerson"));
    assertEquals(entry.getAttributeValue("uid"), "testModifyWithPatch");
    assertEquals(entry.getAttributeValue("cn"), "Test User");
    assertEquals(entry.getAttributeValue("givenname"), "Test");
    assertEquals(entry.getAttributeValue("sn"), "User");
    assertEquals(entry.getAttributeValue("title"), "Chief of Operations");
    assertEquals(entry.getAttributeValue("employeeNumber"), "456");
    assertEquals(entry.getAttributeValue("displayName"),
            "Test Modify with PATCH");
    assertEquals(testDS.bind(entry.getDN(), "anotherPassword").getResultCode(),
        ResultCode.SUCCESS);

    //Verify that existing attributes did not get touched
    assertEquals(entry.getAttributeValue("telephoneNumber"), "512-123-4567");
    assertEquals(entry.getAttributeValue("homePhone"), "972-987-6543");
    assertEquals(entry.getAttributeValues("mail").length, 2);

    String[] attrsToGet = { "userName", "title", "userType",
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION + ":employeeNumber" };
    //Make the exact same update again; this should result in no net change to
    //the Directory entry, but should not fail.

    long beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_OK);

    // Try patching with no etag (for backward compatibility)
    user = userEndpoint.update(user.getId(), null, attrsToUpdate, null);

    long afterCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_OK);

    assertEquals(beforeCount, afterCount - 1);

    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_OK);

    // Try patching with wildcard etag
    user = userEndpoint.update(user.getId(), "*, \"123\"", attrsToUpdate, null);

    afterCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_OK);

    assertEquals(beforeCount, afterCount - 1);

    //Make the exact same update again; this should result in no net change to
    //the Directory entry, but should not fail.

    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_OK);

    // Try patching with multiple etags
    user = userEndpoint.update(user.getId(),
        "\"123\"," + user.getMeta().getVersion(), attrsToUpdate, null,
        attrsToGet);

    afterCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_OK);

    assertEquals(beforeCount, afterCount - 1);

    // Try to put user with non-matching etag
    beforeCount =
        getStatsForResource("User").getStat(
            ResourceStats.PATCH_PRECONDITION_FAILED);
    try
    {
      userEndpoint.update(user.getId(), "\"123\"", attrsToUpdate, null);
      fail("Expected PreconditionFailedException when patching with " +
          "non-matching etag");
    }
    catch(PreconditionFailedException e)
    {
      //expected
    }
    afterCount =
        getStatsForResource("User").getStat(
            ResourceStats.PATCH_PRECONDITION_FAILED);
    assertEquals(beforeCount, afterCount - 1);

    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_NOT_FOUND);
    try
    {
      //Try to update an entry that doesn't exist
      userEndpoint.update(
              "uid=fakeUserName," + userBaseDN, "\"12345\"", attrsToUpdate,
              null);
      fail("Expected ResourceNotFoundException when patching " +
              "non-existent user");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }
    afterCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_NOT_FOUND);
    assertEquals(beforeCount, afterCount - 1);

    //Try a more complex patch, where we simultaneously delete a few attributes
    //and update a few more. Specifically, we are going to delete the phone
    //numbers completely and also remove one of the name attributes and replace
    //it with another.
    attrsToUpdate.clear();

    List<String> attrsToDelete = Collections.unmodifiableList(
        asList("phoneNumbers", "name.familyName"));

    SCIMAttributeValue value =
            SCIMAttributeValue.createStringValue("testEmail.1@example.com");
    SCIMAttribute email1Value = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                 "emails").getSubAttribute("value"), value);

    value = SCIMAttributeValue.createStringValue("work");
    SCIMAttribute emailType = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                 "emails").getSubAttribute("type"), value);

    value = SCIMAttributeValue.createStringValue("delete");
    SCIMAttribute email1Operation = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                 "emails").getSubAttribute("operation"), value);

    SCIMAttributeValue email2 = SCIMAttributeValue.createComplexValue(
                                  email1Value, emailType, email1Operation);

    value = SCIMAttributeValue.createStringValue("testEmail.3@example.com");
    SCIMAttribute email3Value = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                 "emails").getSubAttribute("value"), value);

    SCIMAttributeValue email3 = SCIMAttributeValue.createComplexValue(
                                  email3Value, emailType);

    SCIMAttribute emails = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
           "emails"), email2, email3);

    attrsToUpdate.add(emails);

    value = SCIMAttributeValue.createStringValue("PatchedUser");
    SCIMAttributeValue familyNameValue = SCIMAttributeValue.createComplexValue(
         SCIMAttribute.create(CoreSchema.USER_DESCRIPTOR.getAttribute(
               SCIMConstants.SCHEMA_URI_CORE, "name").getSubAttribute(
                   "familyName"), value));

    SCIMAttribute familyName = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
             "name"), familyNameValue);

    attrsToUpdate.add(familyName);

    //Do the PATCH, this time testing the version of the method that returns
    //the resource.

    //Mark the start and end time with a 500ms buffer on either side, because
    //the Directory Server will record the actual modifyTimestamp using the
    //TimeThread, which only updates once every 100ms.
    Date startTime = new Date(System.currentTimeMillis() - 500);
    UserResource returnedUser = userEndpoint.update(user.getId(),
        user.getMeta().getVersion(), attrsToUpdate, attrsToDelete, attrsToGet);
    Date lastModified = returnedUser.getMeta().getLastModified();
    Date endTime = new Date(System.currentTimeMillis() + 500);

    //Verify the contents of the entry in the Directory
    entry = testDS.getEntry(
        "uid=testModifyWithPatch," + userBaseDN);
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("inetOrgPerson"));
    assertEquals(entry.getAttributeValue("uid"), "testModifyWithPatch");
    assertEquals(entry.getAttributeValue("cn"), "Test User");
    assertEquals(entry.getAttributeValue("givenname"), "Test");
    assertEquals(entry.getAttributeValue("sn"), "PatchedUser");
    assertEquals(entry.getAttributeValue("title"), "Chief of Operations");
    assertEquals(entry.getAttributeValue("employeeNumber"), "456");
    assertEquals(entry.getAttributeValue("displayName"),
            "Test Modify with PATCH");
    assertEquals(testDS.bind(entry.getDN(), "anotherPassword").getResultCode(),
        ResultCode.SUCCESS);

    assertFalse(entry.hasAttribute("telephoneNumber"));
    assertFalse(entry.hasAttribute("homePhone"));
    assertEquals(entry.getAttributeValues("mail").length, 2);
    List<String> emailList = Arrays.asList(entry.getAttributeValues("mail"));
    assertFalse(emailList.contains("testEmail.1@example.com"));
    assertTrue(emailList.contains("testEmail.2@example.com"));
    assertTrue(emailList.contains("testEmail.3@example.com"));

    //Verify what is returned from the SDK
    assertEquals(returnedUser.getId(), user.getId());
    assertTrue(
            returnedUser.getMeta().getLocation().toString()
                    .endsWith(user.getId()));
    assertEquals(returnedUser.getUserName(), "testModifyWithPatch");
    assertEquals(returnedUser.getTitle(), user.getTitle());
    assertEquals(returnedUser.getUserType(), user.getUserType());
    assertNull(returnedUser.getPassword());
    assertTrue(lastModified.after(startTime));
    assertTrue(lastModified.before(endTime));
    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
            AttributeValueResolver.STRING_RESOLVER), "456");

    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_BAD_REQUEST);
    attrsToDelete = Collections.unmodifiableList(
        asList("userName"));
    try
    {
      userEndpoint.update(user.getId(), null, null, attrsToDelete, null);
      fail("Expected a 400 response when trying to patch user by " +
          "deleting required attr");
    }
    catch (InvalidResourceException e)
    {
      // expected (error code is 400)
    }
    afterCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_BAD_REQUEST);
    assertEquals(beforeCount, afterCount - 1);

    // Test PATCH with read only attr
    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_BAD_REQUEST);
    SCIMAttribute groupAttr = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
             "groups"), SCIMAttributeValue.createStringValue("users"));
    attrsToUpdate.clear();
    attrsToUpdate.add(groupAttr);
    try
    {
      userEndpoint.update(user, attrsToUpdate, null);
      fail("Expected a 400 response when trying to patch user with " +
          "read only attr");
    }
    catch (InvalidResourceException e)
    {
      // Expect failure due to read only
      assertTrue(e.getMessage().contains("read only"));
    }
    afterCount =
        getStatsForResource("User").getStat(ResourceStats.PATCH_BAD_REQUEST);
    assertEquals(beforeCount, afterCount - 1);
  }



  /**
   * Tests the basic DELETE functionality via SCIM.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testDeleteUser() throws Exception
  {
     // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    testDS.add(
       generateUserEntry("testDelete", userBaseDN, "Test", "User", "password"));

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final UserResource user = getUser("testDelete");

    long beforeCount =
        getStatsForResource("User").getStat(
            ResourceStats.DELETE_PRECONDITION_FAILED);
    try
    {
      userEndpoint.delete(user.getId(), "\"123\"");
      fail("Expected PreconditionFailedException when deleting with " +
          "non-matching etag");
    }
    catch(PreconditionFailedException e)
    {
      //expected
    }
    long afterCount =
        getStatsForResource("User").getStat(
            ResourceStats.DELETE_PRECONDITION_FAILED);
    assertEquals(beforeCount, afterCount - 1);

    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.DELETE_OK);
    userEndpoint.delete(user);
    afterCount =
        getStatsForResource("User").getStat(ResourceStats.DELETE_OK);

    assertEquals(beforeCount, afterCount - 1);

    beforeCount =
        getStatsForResource("User").getStat(ResourceStats.DELETE_NOT_FOUND);
    try
    {
      //Should throw ResourceNotFoundException
      userEndpoint.delete(user);
      fail("Expected ResourceNotFoundException when deleting " +
              "non-existent user");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }
    afterCount =
        getStatsForResource("User").getStat(ResourceStats.DELETE_NOT_FOUND);
    assertEquals(beforeCount, afterCount - 1);
  }



  /**
   * Tests the basic DELETE functionality via SCIM with wildcard etag.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testDeleteUser2() throws Exception
  {
     // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    testDS.add(
       generateUserEntry("testDelete", userBaseDN, "Test", "User", "password"));

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final UserResource user = getUser("testDelete");
    userEndpoint.delete(user.getId(), "*, \"123\"");
  }



  /**
   * Tests the basic DELETE functionality via SCIM with multiple etags.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testDeleteUser3() throws Exception
  {
     // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    testDS.add(
       generateUserEntry("testDelete", userBaseDN, "Test", "User", "password"));

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final UserResource user = getUser("testDelete");
    userEndpoint.delete(user.getId(), "\"123\"," + user.getMeta().getVersion());
  }




  /**
   * Tests retrieval of a simple user resource.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testGetUser2()
      throws Exception
  {
     // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    final String uid = "testRetrieve";
    final String dn = "uid=" + uid + "," + userBaseDN;

    //Add an entry to the Directory
    testDS.add("dn: " + dn,
                      "objectclass: top",
                      "objectclass: person",
                      "objectclass: organizationalPerson",
                      "objectclass: inetOrgPerson",
                      "uid: " + uid,
                      "userPassword: anotherPassword",
                      "cn: testRetrieve",
                      "givenname: Test",
                      "sn: User",
                      "title: Chief of Research",
                      "displayName: John Smith",
                      "mail: jsmith@example.com",
                      "employeeType: Engineer");

    final String userID = getUser(uid).getId();

    //Try to retrieve the user via SCIM
    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    long beforeCount =
        getStatsForResource("User").getStat(ResourceStats.GET_OK);
    UserResource user = userEndpoint.get(userID);
    long afterCount =
        getStatsForResource("User").getStat(ResourceStats.GET_OK);
    assertNotNull(user);
    assertEquals(user.getId(), userID);
    assertTrue(user.getMeta().getLocation().toString().endsWith(userID),
               "location='" + user.getMeta().getLocation() + "' userID='" +
               userID + "'");
    assertEquals(user.getUserName(), uid);
    assertEquals(user.getUserType(), "Engineer");
    assertEquals(user.getName().getFormatted(), "testRetrieve");
    assertEquals(user.getName().getGivenName(), "Test");
    assertEquals(user.getName().getFamilyName(), "User");
    assertEquals(user.getTitle(), "Chief of Research");
    assertEquals(user.getDisplayName(), "John Smith");
    assertEquals(user.getEmails().iterator().next().getValue(),
                                                "jsmith@example.com");
    assertNull(user.getPassword());

    //Retrieve the user with only the 'userName' and 'password' attribute
    user = userEndpoint.get(userID, null, "userName", "password");
    assertNotNull(user);
    assertEquals(user.getId(), userID);
    assertEquals(user.getUserName(), uid);
    assertNull(user.getUserType());
    assertNull(user.getName());
    assertNull(user.getTitle());
    assertNull(user.getDisplayName());
    assertNull(user.getEmails());
    assertNull(user.getPassword());

    //Make sure the stats were updated properly
    assertEquals(beforeCount, afterCount - 1);

    // Verify that precondition GET works
    beforeCount =
            getStatsForResource("User").getStat(ResourceStats.GET_NOT_MODIFIED);
    assertNull(userEndpoint.get(user.getId(), user.getMeta().getVersion()));
    afterCount =
            getStatsForResource("User").getStat(ResourceStats.GET_NOT_MODIFIED);

    //Make sure the stats were updated properly
    assertEquals(beforeCount, afterCount - 1);

    // Verify that precondition GET works with wildcard etag
    beforeCount =
            getStatsForResource("User").getStat(ResourceStats.GET_NOT_MODIFIED);
    assertNull(userEndpoint.get(user.getId(), "*, \"123\""));
    afterCount =
            getStatsForResource("User").getStat(ResourceStats.GET_NOT_MODIFIED);

    // Verify that precondition GET works with multiple etags
    beforeCount =
            getStatsForResource("User").getStat(ResourceStats.GET_NOT_MODIFIED);
    assertNull(userEndpoint.get(user.getId(), "\"123\"," +
        user.getMeta().getVersion()));
    afterCount =
            getStatsForResource("User").getStat(ResourceStats.GET_NOT_MODIFIED);

    //Make sure the stats were updated properly
    assertEquals(beforeCount, afterCount - 1);
  }



  /**
   * Tests the list/query resources functionality using all the different
   * filter types.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testFiltering() throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

    long halfwayTime = 0;

    // Create some users.
    final long NUM_USERS = 10;
    for (int i = 0; i < NUM_USERS; i++)
    {
      if(i == NUM_USERS / 2)
      {
        //Record the time when half the entries have been created
        Thread.sleep(300);
        halfwayTime = System.currentTimeMillis();
        Thread.sleep(300);
      }

      final String uid = "filterUser." + i;

      com.unboundid.ldap.sdk.Entry e;

      if(i % 2 == 0)
      {
        e = generateUserEntry(uid, userBaseDN, "Test", "User", "password",
                new Attribute("mail", uid + "@example.com",
                                            "evenNumber@example.com"));
      }
      else if(i % 3 == 0)
      {
        e = generateUserEntry(uid, userBaseDN, "Test", "User", "password",
                new Attribute("displayName", uid));
      }
      else
      {
        e = generateUserEntry(uid, userBaseDN, "Test", "User", "password",
                new Attribute("title", "Engineer"));
      }

      testDS.add(e);
    }

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    //
    // All of the filters used below first check if the userName starts with
    // "filterUser" so that we only consider the users created for this test.
    //

    long beforeCount =
        getStatsForResource("User").getStat(ResourceStats.QUERY_OK);

    //Test 'eq' (equals)
    Resources<UserResource> results =
        userEndpoint.query("userName sw \"filterUser\" and " +
                               "emails eq \"filterUser.6@example.com\"");
    assertEquals(results.getTotalResults(), 1);
    Iterator<UserResource> iter = results.iterator();
    UserResource user = iter.next();
    assertEquals(user.getUserName(), "filterUser.6");
    assertFalse(iter.hasNext());

    //Test 'co' (contains)
    results =
      userEndpoint.query("userName sw \"filterUser\" and " +
                             "emails co \"User.4@example\"");
    assertEquals(results.getTotalResults(), 1);
    iter = results.iterator();
    user = iter.next();
    assertEquals(user.getUserName(), "filterUser.4");
    assertFalse(iter.hasNext());

    //Test 'sw' (starts with)
    results =
      userEndpoint.query("userName sw \"filterUser\" and title sw \"Eng\"");
    assertEquals(results.getTotalResults(), 3);
    iter = results.iterator();
    while(iter.hasNext())
    {
      user = iter.next();
      String uid = user.getUserName();
      int idx = Integer.parseInt(uid.substring(uid.indexOf(".") + 1));
      assertTrue(idx % 2 != 0);
      assertTrue(idx % 3 != 0);
    }

    //Test 'pr' (present)
    results = userEndpoint.query("userName sw \"filterUser\" and emails pr");
    assertEquals(results.getTotalResults(), 5);
    iter = results.iterator();
    while(iter.hasNext())
    {
      user = iter.next();
      String uid = user.getUserName();
      int idx = Integer.parseInt(uid.substring(uid.indexOf(".") + 1));
      assertTrue(idx % 2 == 0);
    }

    //Test 'gt' (greater than)
    Date halfwayDate = new Date(halfwayTime);
    String formattedTime = formatter.format(halfwayDate);
    results = userEndpoint.query("userName sw \"filterUser\" and " +
                                 "meta.created gt \"" + formattedTime + "\"");
    assertEquals(results.getTotalResults(), 5);
    iter = results.iterator();
    while(iter.hasNext())
    {
      user = iter.next();
      String uid = user.getUserName();
      int idx = Integer.parseInt(uid.substring(uid.indexOf(".") + 1));
      assertTrue(idx >= 5);
    }

    //Test 'lt' (less than)
    results = userEndpoint.query("userName sw \"filterUser\" and " +
                                 "meta.created lt \"" + formattedTime + "\"");
    assertEquals(results.getTotalResults(), 5);
    iter = results.iterator();
    while(iter.hasNext())
    {
      user = iter.next();
      String uid = user.getUserName();
      int idx = Integer.parseInt(uid.substring(uid.indexOf(".") + 1));
      assertTrue(idx < 5);
    }
    long afterCount =
        getStatsForResource("User").getStat(ResourceStats.QUERY_OK);
    assertEquals(beforeCount, afterCount - 6);
  }



  /**
   * Provides test coverage for pagination.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testPagination() throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create some users.
    final Set<String> userDNs = new HashSet<String>();
    final long NUM_USERS = 10;
    for (int i = 0; i < NUM_USERS; i++)
    {
      final String uid = "paginationUser." + i;
      testDS.add(
          generateUserEntry(uid, userBaseDN,
                            "Test", "User", "password"));
      userDNs.add("uid=" + uid + "," + userBaseDN);
    }

    // Fetch the users one page at a time with page size equal to 1.
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    int pageSize = 1;
    final Set<String> userIDs = new HashSet<String>();
    for (int startIndex = 1; startIndex <= NUM_USERS; startIndex += pageSize)
    {
      final Resources<UserResource> resources =
          userEndpoint.query("userName sw \"paginationUser\"", null,
                             new PageParameters(startIndex, pageSize));
      assertEquals(resources.getTotalResults(), NUM_USERS);
      assertEquals(resources.getStartIndex(), startIndex);
      assertEquals(resources.getItemsPerPage(), pageSize);
      assertTrue(userIDs.add(resources.iterator().next().getId()));
    }
    assertEquals(userIDs.size(), NUM_USERS);

    // Create some groups.
    final int NUM_GROUPS = 10;
    for (int i = 0; i < NUM_GROUPS; i++)
    {
      final String cn = "paginationGroup." + i;
      testDS.add(
          generateGroupOfNamesEntry(cn, groupBaseDN, userDNs));
    }

    // Fetch the groups one page at a time with page size equal to 3.
    final SCIMEndpoint<GroupResource> groupEndpoint =
           service.getGroupEndpoint();
    pageSize = 3;
    final Set<String> groupIDs = new HashSet<String>();
    for (int startIndex = 1; startIndex <= NUM_GROUPS; startIndex += pageSize)
    {
      final Resources<GroupResource> resources =
        groupEndpoint.query("displayName sw \"paginationGroup\"",
          new SortParameters("displayName", "ascending"),
          new PageParameters(startIndex, pageSize));

      assertEquals(resources.getTotalResults(), NUM_GROUPS);
      assertEquals(resources.getStartIndex(), startIndex);
      assertEquals(resources.getItemsPerPage(), startIndex < 10 ? pageSize : 1);

      int numResources = 0;
      for (final GroupResource resource : resources)
      {
        numResources++;
        assertTrue(groupIDs.add(resource.getId()));
      }
      assertEquals(resources.getItemsPerPage(), numResources);
    }
    assertEquals(groupIDs.size(), NUM_GROUPS);

    // Attempt to fetch resources from a non-existent page.
    final int startIndex = NUM_GROUPS + 1;
    final Resources<GroupResource> resources =
        groupEndpoint.query("displayName sw \"paginationGroup\"",
          new SortParameters("displayName", "ascending"),
          new PageParameters(startIndex, pageSize));
    assertEquals(resources.getTotalResults(), NUM_GROUPS);
    assertEquals(resources.getItemsPerPage(), 0);
    assertEquals(resources.getStartIndex(), startIndex);
  }



  /**
   * Tests the maxResults configuration setting.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testMaxResults() throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Lower the maxResults setting.
    final int maxResults = 1;
    SCIMServerConfig config = new SCIMServerConfig();
    config.setResourcesFile(getFile("resource/resources.xml"));
    config.setMaxResults(maxResults);
    reconfigureTestSuite(config);

    // Create some users.
    final long NUM_USERS = 10;
    for (int i = 0; i < NUM_USERS; i++)
    {
      final String uid = "maxResultsUser." + i;
      testDS.add(
          generateUserEntry(uid, userBaseDN, "Test", "User", "password"));
    }

    // Try to fetch more users than can be returned.
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    final Resources<UserResource> resources = userEndpoint.query(null);
    assertEquals(resources.getItemsPerPage(), maxResults);
    assertEquals(resources.getTotalResults(), maxResults);

    //Clean up
    config.setMaxResults(Integer.MAX_VALUE);
    reconfigureTestSuite(config);
  }


  /**
   * Tests the Service Provider Config endpoint.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testServiceProviderConfig() throws Exception
  {
    final ServiceProviderConfig config = service.getServiceProviderConfig();

    // These assertions need to be updated as optional features are implemented.
    assertTrue(config.getPatchConfig().isSupported());
    assertTrue(config.getBulkConfig().isSupported());
    assertTrue(config.getBulkConfig().getMaxOperations() > 0);
    assertTrue(config.getBulkConfig().getMaxPayloadSize() > 0);
    assertTrue(config.getFilterConfig().isSupported());
    assertTrue(config.getFilterConfig().getMaxResults() > 0);
    assertTrue(config.getChangePasswordConfig().isSupported());
    assertTrue(config.getSortConfig().isSupported());
    assertTrue(config.getETagConfig().isSupported());
    assertTrue(config.getAuthenticationSchemes().size() > 0);
    assertTrue(config.getXmlDataFormatConfig().isSupported());

    for (final AuthenticationScheme s : config.getAuthenticationSchemes())
    {
      assertNotNull(s.getName());
      assertNotNull(s.getDescription());
    }
  }



  /**
   * Tests HTTP Basic Auth.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testBasicAuth() throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create a new user.
    final String dn = "uid=basicAuthUser," + userBaseDN;
    testDS.add(
        generateUserEntry(
            "basicAuthUser", userBaseDN, "Basic", "User", "password"));
    final String id = getUser("basicAuthUser").getId();

    // Create a client service that authenticates as the user.
    final SCIMService basicAuthService = createSCIMService(dn, "password");

    // Check that the authenticated user can read its own entry.
    final SCIMEndpoint<UserResource> endpoint =
        basicAuthService.getUserEndpoint();
    final UserResource userResource = endpoint.get(id);
    assertNotNull(userResource);

    System.out.println("basicAuthUser = " + userResource);
    assertNotNull(userResource.getMeta().getCreated());
    assertNotNull(userResource.getMeta().getLastModified());
  }



  /**
   * Tests HTTP Basic Auth With Funky Password.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testBasicAuthWithFunkyPassword() throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    final String FUNKY_PASSWORD = "P@ss:word//{}";

    // Create a new user.
    final String dn = "uid=basicAuthUser," + userBaseDN;
    testDS.add(
        generateUserEntry(
            "basicAuthUser", userBaseDN, "Basic", "User", FUNKY_PASSWORD));
    final String id = getUser("basicAuthUser").getId();

    // Create a client service that authenticates as the user.
    final SCIMService basicAuthService = createSCIMService(dn, FUNKY_PASSWORD);

    // Check that the authenticated user can read its own entry.
    final SCIMEndpoint<UserResource> endpoint =
        basicAuthService.getUserEndpoint();
    final UserResource userResource = endpoint.get(id);
    assertNotNull(userResource);

    System.out.println("basicAuthUser = " + userResource);
    assertNotNull(userResource.getMeta().getCreated());
    assertNotNull(userResource.getMeta().getLastModified());
  }



  /**
   * Tests HTTP Basic Auth with the wrong credentials.
   *
   * @throws Exception If the test fails.
   */
  @Test(expectedExceptions = UnauthorizedException.class)
  public void testBasicAuthInvalidCredentials()
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create a new user.
    final String dn = "uid=invalidCredentials," + userBaseDN;
    testDS.add(
        generateUserEntry(
            "invalidCredentials", userBaseDN, "Basic", "User", "password"));
    final String id = getUser("invalidCredentials").getId();

    // Create a client service that authenticates with the wrong password.
    final SCIMService basicAuthService = createSCIMService(dn, "assword");

    basicAuthService.getUserEndpoint().get(id);
  }



  /**
   * Tests the SCIM Bulk operation.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testBulk() throws Exception
  {
    getTestDS(true, true); //initialize the test DS

    final String mediaSubType = service.getContentType().getSubtype() + "-" +
        service.getAcceptType().getSubtype();
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    final SCIMEndpoint<GroupResource> groupEndpoint =
        service.getGroupEndpoint();

    UserResource userAlice = userEndpoint.newResource();
    userAlice.setName(new Name("Alice Ecila", "Ecila", null,
                               "Alice", null, null));
    userAlice.setUserName("alice-" + mediaSubType);

    UserResource userBob = userEndpoint.newResource();
    userBob.setName(new Name("Bob The Builder", "Builder", "The",
                             "Bob", null, null));
    userBob.setUserName("bob-" + mediaSubType);
    userBob = userEndpoint.create(userBob);
    userBob.setTitle("Construction Worker");
    userBob.setSingularAttributeValue(
        SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
        Manager.MANAGER_RESOLVER,
        new Manager("bulkId:alice", "Miss Manager"));

    UserResource userDave = userEndpoint.newResource();
    userDave.setName(new Name("Dave Allen", "Allen", null,
                              "Dave", null, null));
    userDave.setUserName("dave-" + mediaSubType);
    userDave = userEndpoint.create(userDave);

    GroupResource group = groupEndpoint.newResource();
    group.setDisplayName("group-" + mediaSubType);
    Collection<com.unboundid.scim.data.Entry<String>> members =
        new ArrayList<com.unboundid.scim.data.Entry<String>>();
    members.add(new com.unboundid.scim.data.Entry<String>(
        "bulkId:alice", null));
    group.setMembers(members);

    final long beforeBulkSuccessCount =
        getStatsForResource("Bulk").getStat(ResourceStats.POST_OK);
    final long beforeBulkContentCount =
        getStatsForResource("Bulk").getStat("post-content-" +
            service.getContentType().getSubtype());
    final long beforeBulkResponseCount =
        getStatsForResource("Bulk").getStat("post-response-" +
            service.getAcceptType().getSubtype());

    final long beforeUserPostSuccessCount =
        getStatsForResource("User").getStat(ResourceStats.POST_OK);
    final long beforeUserPutSuccessCount =
        getStatsForResource("User").getStat(ResourceStats.PUT_OK);
    final long beforeUserDeleteSuccessCount =
        getStatsForResource("User").getStat(ResourceStats.DELETE_OK);
    final long beforeGroupPostSuccessCount =
        getStatsForResource("Group").getStat(ResourceStats.POST_OK);

    final List<BulkOperation> operations = new ArrayList<BulkOperation>();

    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.POST, "alice", null,
        "/Users", userAlice));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.PUT, "bob",
        "\"123\"," + userBob.getMeta().getVersion(),
        "/Users/" + userBob.getId(), userBob));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.DELETE, null, "*",
        "/Users/" + userDave.getId(), null));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.POST, "group", null,
        "/Groups", group));

    final BulkResponse response = service.processBulkRequest(operations);
    final List<BulkOperation> responses = new ArrayList<BulkOperation>();
    for (final BulkOperation o : response)
    {
      responses.add(o);
    }

    final long afterBulkSuccessCount =
        getStatsForResource("Bulk").getStat(ResourceStats.POST_OK);
    final long afterBulkContentCount =
        getStatsForResource("Bulk").getStat("post-content-" +
            service.getContentType().getSubtype());
    final long afterBulkResponseCount =
        getStatsForResource("Bulk").getStat("post-response-" +
            service.getAcceptType().getSubtype());

    assertTrue(afterBulkSuccessCount > beforeBulkSuccessCount);
    assertTrue(afterBulkContentCount > beforeBulkContentCount);
    assertTrue(afterBulkResponseCount > beforeBulkResponseCount);

    final long afterUserPostSuccessCount =
        getStatsForResource("User").getStat(ResourceStats.POST_OK);
    final long afterUserPutSuccessCount =
        getStatsForResource("User").getStat(ResourceStats.PUT_OK);
    final long afterUserDeleteSuccessCount =
        getStatsForResource("User").getStat(ResourceStats.DELETE_OK);
    final long afterGroupPostSuccessCount =
        getStatsForResource("Group").getStat(ResourceStats.POST_OK);

    assertEquals(afterUserPostSuccessCount, beforeUserPostSuccessCount+ 1);
    assertEquals(afterUserPutSuccessCount, beforeUserPutSuccessCount + 1);
    assertEquals(afterUserDeleteSuccessCount, beforeUserDeleteSuccessCount + 1);
    assertEquals(afterGroupPostSuccessCount, beforeGroupPostSuccessCount + 1);


    assertEquals(responses.size(), operations.size());

    for (int i = 0; i < operations.size(); i++)
    {
      final BulkOperation o = operations.get(i);
      final BulkOperation r = responses.get(i);

      assertEquals(o.getMethod(), r.getMethod());
      assertEquals(o.getBulkId(), r.getBulkId());
      if (o.getMethod().equals(BulkOperation.Method.POST) ||
              o.getMethod().equals(BulkOperation.Method.PUT))
      {
        assertNotNull(r.getLocation());
        assertNotNull(r.getVersion());
      }

      assertNotNull(r.getStatus());

      if (o.getMethod().equals(BulkOperation.Method.POST))
      {
        assertEquals(r.getStatus().getCode(), "201");
      }
      else
      {
        assertEquals(r.getStatus().getCode(), "200");
      }
    }

    userAlice = userEndpoint.query(
        "userName eq \"alice-" + mediaSubType + "\"").iterator().next();
    assertEquals(responses.get(0).getBulkId(), "alice");
    assertTrue(responses.get(0).getLocation().endsWith(userAlice.getId()));

    try
    {
      userEndpoint.get(userDave.getId());
      fail("User Dave was not deleted");
    }
    catch (SCIMException e)
    {
      // Expected.
      assertEquals(e.getStatusCode(), 404);
    }

    userBob = userEndpoint.get(userBob.getId());
    assertEquals(userBob.getTitle(), "Construction Worker");
    assertEquals(userBob.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER).getManagerId(),
                 userAlice.getId());

    group = groupEndpoint.query(
        "displayName eq \"group-" + mediaSubType + "\"").iterator().next();
    assertEquals(responses.get(3).getBulkId(), "group");
    assertTrue(responses.get(3).getLocation().endsWith(group.getId()));
    assertEquals(group.getMembers().iterator().next().getValue(),
                 userAlice.getId());

    groupEndpoint.delete(group);
  }



  /**
   * Tests that the server returns the correct response for invalid SCIM Bulk
   * operations.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testInvalidBulk()
      throws Exception
  {
    getTestDS(true, true); //initialize the test DS

    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    UserResource testUser = userEndpoint.newResource();
    testUser.setName(new Name("Test Invalid Bulk", "Bulk", null,
                              "Test", null, null));
    testUser.setUserName("test-invalid-bulk");
    testUser = userEndpoint.create(testUser);

    final SCIMEndpoint<GroupResource> groupEndpoint =
        service.getGroupEndpoint();

    final GroupResource testGroup = groupEndpoint.newResource();
    testGroup.setDisplayName("test-invalid-bulk");
    final Collection<com.unboundid.scim.data.Entry<String>> members =
        new ArrayList<com.unboundid.scim.data.Entry<String>>();
    members.add(new com.unboundid.scim.data.Entry<String>(
        "bulkId:undefined", null));
    testGroup.setMembers(members);

    // Missing method.
    testInvalidBulkOperation(
        BulkOperation.createRequest(null, null, null, "/Users", null),
        "400");

    // POST with missing bulkId.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.POST, null, null,
                                    "/Users", testUser),
        "400");

    // Missing path.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.DELETE, null,
                                    "\"123\"", null, null),
        "400");

    // POST with missing data.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.POST, "user", null,
                                    "/Users", null),
        "400");

    // POST specifies path with a resource ID.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.POST, "user", null,
                                    "/Users/1", testUser),
        "400");

    // DELETE specifies a path with no resource ID.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.DELETE, null,
                                    "\"123\"", "/Users", null),
        "400");

    // Undefined bulkId reference in the data.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.POST, "group", null,
                                    "/Groups/", testGroup),
        "409");

    // PATCH a resource that doesn't exist.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.PATCH, null, "\"123\"",
                                    "/Users/1", testUser),
        "404");

    // PUT a resource with non-matching version
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.PUT, null, "\"123\"",
                                    "/Users/" + testUser.getId(), testUser),
        "412");

    // DELETE a resource with non-matching version
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.DELETE, null,
                                    "\"123\"", "/Users/" + testUser.getId(),
                                    testUser),
        "412");
    // PATCH a resource with non-matching version
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.PATCH, null, "\"123\"",
                                    "/Users/" + testUser.getId(), testUser),
        "412");

    userEndpoint.delete(testUser);
  }



  /**
   * Tests that the server returns the correct response for an invalid SCIM Bulk
   * operation.
   *
   * @param o                     An invalid operation.
   * @param expectedResponseCode  The expected response code.
   *
   * @throws Exception If the test fails.
   */
  private void testInvalidBulkOperation(final BulkOperation o,
                                        final String expectedResponseCode)
      throws Exception
  {
    // We allow only the individual operation to fail within the request.
    final BulkResponse response = service.processBulkRequest(Arrays.asList(o));
    final Status status = response.iterator().next().getStatus();
    final String bulkId = response.iterator().next().getBulkId();

    assertNotNull(status);
    assertEquals(status.getCode(), expectedResponseCode);
    assertNotNull(status.getDescription());
    assertEquals(bulkId, o.getBulkId());
  }



  /**
   * Tests that the server returns the correct response to a SCIM Bulk request
   * which contains two operations with the same bulkId.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testDuplicateBulkId()
      throws Exception
  {
    getTestDS(true, true); //initialize the test DS

    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final UserResource testUser1 = userEndpoint.newResource();
    testUser1.setName(new Name("Test Duplicate BulkId", "BulkId", null,
                              "Test", null, null));
    testUser1.setUserName("test-duplicate-bulkid-1");

    final UserResource testUser2 = userEndpoint.newResource();
    testUser2.setName(new Name("Test Duplicate BulkId", "BulkId", null,
                              "Test", null, null));
    testUser2.setUserName("test-duplicate-bulkid-2");

    final List<BulkOperation> operations = new ArrayList<BulkOperation>(2);
    operations.add(
        BulkOperation.createRequest(BulkOperation.Method.POST, "bulkid1", null,
                                    "/Users", testUser1));
    operations.add(
        BulkOperation.createRequest(BulkOperation.Method.POST, "bulkid1", null,
                                    "/Users", testUser2));

    final BulkResponse bulkResponse = service.processBulkRequest(operations);
    final List<BulkOperation> responses = new ArrayList<BulkOperation>();
    for (final BulkOperation o : bulkResponse)
    {
      responses.add(o);
    }

    assertEquals(responses.get(0).getStatus().getCode(), "201");
    assertEquals(responses.get(1).getStatus().getCode(), "400");
  }



  /**
   * Tests the bulkMaxPayloadSize configuration setting.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testBulkMaxPayloadSize() throws Exception
  {
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < 100; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null, "/Users/" + i, null));
    }

    // Lower the bulkMaxPayloadSize setting.
    int bulkMaxPayloadSize = 1000;
    SCIMServerConfig config = new SCIMServerConfig();
    config.setBulkMaxPayloadSize(bulkMaxPayloadSize);
    reconfigureTestSuite(config);

    try
    {
      try
      {
        service.processBulkRequest(operations);
        fail("The bulkMaxPayloadSize was intentionally exceeded but the " +
             "bulk request was not rejected");
      }
      catch (SCIMException e)
      {
        // Expected.
        assertEquals(e.getStatusCode(), 413);
      }
    }
    finally
    {
      //Clean up
      config.setBulkMaxPayloadSize(Long.MAX_VALUE);
      reconfigureTestSuite(config);
    }

    // Increase the bulkMaxPayloadSize setting.
    bulkMaxPayloadSize = 10000;
    config.setBulkMaxPayloadSize(bulkMaxPayloadSize);
    reconfigureTestSuite(config);

    try
    {
      // The same request should now succeed (although all the operations fail)
      service.processBulkRequest(operations);
    }
    finally
    {
      //Clean up
      config.setBulkMaxPayloadSize(Long.MAX_VALUE);
      reconfigureTestSuite(config);
    }
  }



  /**
   * Tests the bulkMaxOperations configuration setting.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testBulkMaxOperations() throws Exception
  {
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < 100; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null, "/Users/" + i, null));
    }

    // Lower the bulkMaxOperations setting.
    int bulkMaxOperations = 99;
    SCIMServerConfig config = new SCIMServerConfig();
    config.setBulkMaxOperations(bulkMaxOperations);
    reconfigureTestSuite(config);

    try
    {
      final long beforeDelete404Count =
          getStatsForResource("User").getStat(ResourceStats.DELETE_NOT_FOUND);
      try
      {
        service.processBulkRequest(operations);
        fail("The bulkMaxOperations was intentionally exceeded but the " +
             "bulk request was not rejected");
      }
      catch (SCIMException e)
      {
        // Expected.
        assertEquals(e.getStatusCode(), 413);
      }

      // Ensure no delete operations were attempted.
      final long afterDelete404Count =
          getStatsForResource("User").getStat(ResourceStats.DELETE_NOT_FOUND);
      assertEquals(afterDelete404Count, beforeDelete404Count,
              getStatsForResource("User").toString());
    }
    finally
    {
      //Clean up
      config.setBulkMaxOperations(Long.MAX_VALUE);
      reconfigureTestSuite(config);
    }

    // Increase the bulkMaxPayloadSize setting.
    bulkMaxOperations = 100;
    config.setBulkMaxOperations(bulkMaxOperations);
    reconfigureTestSuite(config);

    try
    {
      // The same request should now succeed (although all the operations fail)
      service.processBulkRequest(operations);
    }
    finally
    {
      //Clean up
      config.setBulkMaxOperations(Long.MAX_VALUE);
      reconfigureTestSuite(config);
    }
  }



  /**
   * Tests the Bulk request failOnErrors value.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testFailOnErrors() throws Exception
  {
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < 10; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null, "/Users/" + i, null));
    }

    assertEquals(getSize(service.processBulkRequest(operations, 1)), 1);
    assertEquals(getSize(service.processBulkRequest(operations, 9)), 9);
    assertEquals(getSize(service.processBulkRequest(operations, 10)), 10);
    assertEquals(getSize(service.processBulkRequest(operations, -1)), 10);
  }



  /**
   * Determine the number of operations in a bulk response.
   *
   * @param bulkResponse  The bulk response to be sized.
   *
   * @return  The number of operations in the bulk response.
   */
  private int getSize(final BulkResponse bulkResponse)
  {
    int count = 0;
    for (final BulkOperation o : bulkResponse)
    {
      count++;
    }

    return count;
  }



  /**
   * Our implementation of Bulk operations permits a bulkId reference to be
   * used as a resource ID in the path. This method tests that feature.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testBulkIdInPath() throws Exception
  {
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final UserResource testUser = userEndpoint.newResource();
    testUser.setName(new Name("Test BulkId In Path", "Path", null,
                              "Test", null, null));
    testUser.setUserName("test-bulkid-in-path");

    final List<BulkOperation> operations = new ArrayList<BulkOperation>();

    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.POST, "user", null,
        "/Users", testUser));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.DELETE, null, null,
        "/Users/" + "bulkId:user", null));

    final BulkResponse response = service.processBulkRequest(operations);
    final List<BulkOperation> responses = new ArrayList<BulkOperation>();
    for (final BulkOperation o : response)
    {
      responses.add(o);
    }

    assertEquals(responses.size(), operations.size());

    for (int i = 0; i < operations.size(); i++)
    {
      final BulkOperation o = operations.get(i);
      final BulkOperation r = responses.get(i);

      if (o.getMethod().equals(BulkOperation.Method.POST))
      {
        assertEquals(r.getStatus().getCode(), "201");
      }
      else
      {
        assertEquals(r.getStatus().getCode(), "200");
      }
    }

    assertEquals(userEndpoint.query(
        "userName eq \"test-bulkid-in-path\"").getTotalResults(), 0);
  }



  /**
   * Process a bunch of bulk requests in parallel.
   *
   * @param numRequests           The total number of bulk requests to process.
   * @param numThreads            The number of threads to use.
   * @param operations            The bulk operations for the content of each
   *                              request.
   * @param numSuccessfulRequests Returns the number of successful requests.
   * @param numFailedRequests     Returns the number of failed requests.
   *
   * @throws Exception  If the requests could not be processed.
   */
  private void processBulkRequestsInParallel(
      final int numRequests, final int numThreads,
      final List<BulkOperation> operations,
      final AtomicInteger numSuccessfulRequests,
      final AtomicInteger numFailedRequests)
      throws Exception
  {
    numSuccessfulRequests.set(0);
    numFailedRequests.set(0);

    // Create a runnable to execute a single bulk request with the same
    // operations.
    final Runnable runnable = new Runnable()
    {
      public void run()
      {
        try
        {
          service.processBulkRequest(operations);
          numSuccessfulRequests.incrementAndGet();
        }
        catch (Exception e)
        {
          numFailedRequests.incrementAndGet();
          // Ignore.
        }
      }
    };

    // Execute a bunch of requests in parallel.
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

    for(int i = 0; i < numRequests; i++)
    {
      executorService.submit(runnable);
    }

    // Make sure all the requests were completed.
    executorService.shutdown();

    assertTrue(executorService.awaitTermination(5, TimeUnit.MINUTES));
    assertEquals(numSuccessfulRequests.get() + numFailedRequests.get(),
            numRequests, "numSuccessfulRequests=" + numSuccessfulRequests +
               " numFailedRequests=" + numFailedRequests);
  }



  /**
   * Tests the bulkMaxOperations configuration setting.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testBulkMaxConcurrentRequests() throws Exception
  {
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    // Create some bulk operations.
    final int numUsers = 1;
    final List<UserResource> users = new ArrayList<UserResource>();
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < numUsers; i++)
    {
      final UserResource user = userEndpoint.newResource();
      user.setName(new Name("User " + i, "User", null, "Test", null, null));
      user.setUserName("user." + i);
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.POST, "bulkid." + i, null, "/Users", user));
      users.add(user);
    }

    for (int i = 0; i < numUsers; i++)
    {
      final UserResource user = users.get(i);
      user.setTitle("Updated Title");
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.PUT, null, null,
          "/Users/bulkId:bulkid." + i, user));
    }

    for (int i = 0; i < numUsers; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null,
          "/Users/bulkId:bulkid." + i, null));
    }

    final AtomicInteger numSuccessfulRequests = new AtomicInteger(0);
    final AtomicInteger numFailedRequests = new AtomicInteger(0);

    final int numRequests = 20;
    final int numThreads = 5;

    // Set the bulkMaxConcurrentRequests setting to 1.
    int bulkMaxConcurrentRequests = 1;
    SCIMServerConfig config = new SCIMServerConfig();
    config.setBulkMaxConcurrentRequests(bulkMaxConcurrentRequests);
    reconfigureTestSuite(config);

    try
    {
      processBulkRequestsInParallel(numRequests, numThreads, operations,
                                    numSuccessfulRequests,
                                    numFailedRequests);
      assertTrue(numSuccessfulRequests.get() > 0);
      assertTrue(numFailedRequests.get() > 0);

      // Set the bulkMaxConcurrentRequests setting to 5.
      bulkMaxConcurrentRequests = 5;
      config.setBulkMaxConcurrentRequests(bulkMaxConcurrentRequests);
      reconfigureTestSuite(config);

      processBulkRequestsInParallel(numRequests, numThreads, operations,
                                    numSuccessfulRequests,
                                    numFailedRequests);
      assertEquals(numSuccessfulRequests.get(), numRequests);
      assertEquals(numFailedRequests.get(), 0);

      // Set the bulkMaxConcurrentRequests setting back to 1.
      bulkMaxConcurrentRequests = 1;
      config.setBulkMaxConcurrentRequests(bulkMaxConcurrentRequests);
      reconfigureTestSuite(config);

      processBulkRequestsInParallel(numRequests, numThreads, operations,
                                    numSuccessfulRequests,
                                    numFailedRequests);
      assertTrue(numSuccessfulRequests.get() > 0);
      assertTrue(numFailedRequests.get() > 0);
    }
    finally
    {
      //Clean up
      config.setBulkMaxConcurrentRequests(Integer.MAX_VALUE);
      reconfigureTestSuite(config);
    }
  }

}
