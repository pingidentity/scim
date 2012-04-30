/*
 * Copyright 2011-2012 UnboundID Corp.
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
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.scim.data.Address;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMService;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.PageParameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.unboundid.scim.sdk.SCIMConstants.SCHEMA_URI_CORE;
import static com.unboundid.scim.sdk.SCIMConstants.
    SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE;



/**
 * This class provides test coverage for the SCIMServer class.
 */
public class SCIMServerTestCase extends SCIMRITestCase
{

  private final String userBaseDN = "ou=people,dc=example,dc=com";


  /**
   * Provides test coverage for accessing a specific API version.
   *
   * @throws Exception  If the test fails.
   */
  @Test(dependsOnMethods = "testPostUser")
  public void testAPIVersion()
      throws Exception
  {
    SCIMService v1Service =
        new SCIMService(URI.create("http://localhost:"+getSSTestPort()+"/v1"),
            "cn=Manager", "password");

    SCIMEndpoint<UserResource> userEndpoint =
        v1Service.getUserEndpoint();
    userEndpoint.query(null);
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
    // Test receiving XML content.
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testGetUser(service);

    // Test receiving JSON content.
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testGetUser(service);
  }



  /**
   * Provides test coverage for the GET operation on a group resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testGetGroup()
      throws Exception
  {
    // Test receiving XML content.
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testGetGroup(service);

    // Test receiving JSON content.
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testGetGroup(service);
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
    // Test receiving XML content.
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testGetUsers(service);

    // Test receiving JSON content.
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testGetUsers(service);
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
    // Test receiving XML content.
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testGetSortedUsers(service);

    // Test receiving JSON content.
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testGetSortedUsers(service);
  }



  /**
   * Provides test coverage for the GET operation to fetch pages of users.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testGetPaginatedUsers()
      throws Exception
  {
    // Test receiving XML content.
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testGetPaginatedUsers(service);

    // Test receiving JSON content.
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testGetPaginatedUsers(service);
  }



  /**
   * Provides test coverage for the POST operation on a user resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testPostUser()
      throws Exception
  {
    // Test sending and receiving XML content.
    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testPostUser(service);

    // Test sending and receiving JSON content.
    service.setContentType(MediaType.APPLICATION_JSON_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testPostUser(service);

    // Test sending XML and receiving JSON content.
    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testPostUser(service);

    // Test sending JSON and receiving XML content.
    service.setContentType(MediaType.APPLICATION_JSON_TYPE);
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testPostUser(service);
  }



  /**
   * Provides test coverage for the PUT operation on a user resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testPutUser()
      throws Exception
  {
    // Test sending and receiving XML content.
    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testPutUser(service);

    // Test sending and receiving JSON content.
    service.setContentType(MediaType.APPLICATION_JSON_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testPutUser(service);

    // Test sending XML and receiving JSON content.
    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testPutUser(service);

    // Test sending JSON and receiving XML content.
    service.setContentType(MediaType.APPLICATION_JSON_TYPE);
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testPutUser(service);
  }



  /**
   * Provides test coverage for write operations on a group resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testWriteGroup()
      throws Exception
  {
    // Test sending and receiving XML content.
    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testWriteGroup(service);

    // Test sending and receiving JSON content.
    service.setContentType(MediaType.APPLICATION_JSON_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testWriteGroup(service);

    // Test sending XML and receiving JSON content.
    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    testWriteGroup(service);

    // Test sending JSON and receiving XML content.
    service.setContentType(MediaType.APPLICATION_JSON_TYPE);
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    testWriteGroup(service);
  }


  /**
   * Verifies that we support Users and Groups with different base DNs and that
   * we can still search for a Group and get all of its members (even though
   * they're in a different part of the DIT), and that we can search for a user
   * and get all of its groups (even though they're in a different part of the
   * DIT).
   * @throws Exception  If the test failed.
   */
  @Test
  public void testDifferentBaseDNs() throws Exception
  {
    //Switch the RI server to use a different resources.xml
    File f = getFile(
       "scim-ri/src/test/resources/resources-different-base-dns.xml");
    assertTrue(f.exists());
    reconfigureTestSuite(f);

    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));
    testDS.add(generateOrgEntry("example.com", null));

    // Create some users under dc=example,dc=com
    UserResource user1 = new UserResource(CoreSchema.USER_DESCRIPTOR);
    Name name1 = new Name("Ms. Barbara J Jensen III", "Jensen", "J",
        "Barbara", "Ms", "III");
    user1.setUserName("bjensen");
    user1.setName(name1);
    user1.setPassword("4wrj2U81j");

    UserResource user2 = new UserResource(CoreSchema.USER_DESCRIPTOR);
    Name name2 = new Name("Mr. John D. Smith", "Smith", "D",
        "John", "Mr", null);
    user2.setUserName("jsmith");
    user2.setName(name2);
    user2.setPassword("Rls3xj2%4");

    // Post the users via SCIM, returning selected attributes.
    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
    user1 = endpoint.create(user1, "id", "meta");
    user2 = endpoint.create(user2, "id", "meta");

    //Verify the users were created under ou=people,dc=example,dc=com
    assertEquals(
            DN.getParent(user1.getId()), new DN(userBaseDN));
    assertEquals(
            DN.getParent(user2.getId()), new DN(userBaseDN));

    //Create a group containing both users
    SCIMEndpoint<GroupResource> grpEndpoint = service.getGroupEndpoint();
    GroupResource group1 = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    group1.setDisplayName("group1");
    Set<com.unboundid.scim.data.Entry<String>> members1 =
          new HashSet<com.unboundid.scim.data.Entry<String>>();
    members1.add(new com.unboundid.scim.data.Entry<String>(
                              user1.getId(), "User", false));
    members1.add(new com.unboundid.scim.data.Entry<String>(
                              user2.getId(), "User", false));
    group1.setMembers(members1);
    group1 = grpEndpoint.create(group1, "id", "meta", "members");

    //Verify that the group was created under o=example.com and contains
    //both members
    assertEquals(DN.getParent(group1.getId()), new DN("o=example.com"));
    assertEquals(group1.getMembers().size(), 2);

    //Verify that the members have the correct base DN
    for(com.unboundid.scim.data.Entry<String> entry : group1.getMembers())
    {
      DN dn = new DN(entry.getValue());
      assertTrue(dn.isDescendantOf(userBaseDN, false));
    }

    //Create another group under o=example.com
    GroupResource group2 = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    group2.setDisplayName("group2");
    Set<com.unboundid.scim.data.Entry<String>> members2 =
          new HashSet<com.unboundid.scim.data.Entry<String>>();
    members2.add(new com.unboundid.scim.data.Entry<String>(
                                user1.getId(), "User", false));
    members2.add(new com.unboundid.scim.data.Entry<String>(
                                group1.getId(), "Group", false));
    group2.setMembers(members2);
    group2 = grpEndpoint.create(group2, "id", "meta", "members");

    //Verify that the group was created under o=example.com and contains
    //both members
    assertEquals(DN.getParent(group2.getId()), new DN("o=example.com"));
    assertEquals(group2.getMembers().size(), 2);
    for(com.unboundid.scim.data.Entry<String> entry : group2.getMembers())
    {
      DN dn = new DN(entry.getValue());
      if(entry.getType().equalsIgnoreCase("user"))
      {
        assertTrue(dn.isDescendantOf(userBaseDN, false));
      }
      else if(entry.getType().equalsIgnoreCase("group"))
      {
        assertTrue(dn.isDescendantOf("o=example.com", false));
      }
      else
      {
        fail("Unknown base DN for group member: " + dn);
      }
    }

    //Verify that user1 correctly identifies its two groups which should
    //both be direct.
    user1 = endpoint.get(user1.getId(), "id", "meta", "groups");
    assertEquals(user1.getGroups().size(), 2);
    for(com.unboundid.scim.data.Entry<String> entry : user1.getGroups())
    {
      DN dn = new DN(entry.getValue());
      assertTrue(dn.isDescendantOf("o=example.com", false));
      assertEquals(entry.getType(), "direct");
    }

    //Verify that user2 correctly identifies its two groups which should
    //both be direct.
    user2 = endpoint.get(user2.getId(), "id", "meta", "groups");
    assertEquals(user2.getGroups().size(), 2);
    for(com.unboundid.scim.data.Entry<String> entry : user2.getGroups())
    {
      DN dn = new DN(entry.getValue());
      assertTrue(dn.isDescendantOf("o=example.com", false));
      if(entry.getDisplay().equals("group1"))
      {
        assertEquals(entry.getType(), "direct");
      }
      else if(entry.getDisplay().equals("group2"))
      {
        assertEquals(entry.getType(), "indirect");
      }
    }

    //Cleanup
    reconfigureTestSuite(getFile("resource/resources.xml"));
  }



  /**
   * Provides test coverage for the GET operation on a user resource.
   *
   * @param service  The SCIM service to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testGetUser(final SCIMService service)
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
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
        new Attribute("postalCode", "78759")));

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
    assertLocation(user1.getMeta().getLocation(), "Users", userID);
    assertNotNull(user1.getUserName());
    assertEquals(user1.getUserName(), "b jensen");
    assertEquals(user1.getName().getFamilyName(), "Jensen");
    assertEquals(user1.getName().getGivenName(), "Barbara");
    assertNotNull(user1.getEmails());
    assertNotNull(user1.getAddresses());

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
  }



  /**
   * Provides test coverage for the GET operation on a Group resource.
   *
   * @param service  The SCIM service to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testGetGroup(final SCIMService service)
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
   * @param service  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testGetUsers(final SCIMService service)
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
   * @param service  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testGetSortedUsers(final SCIMService service)
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
   * Provides test coverage for the GET operation to fetch pages of users.
   *
   * @param service  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testGetPaginatedUsers(final SCIMService service)
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create some users directly on the test DS.
    final long NUM_USERS = 4;
    for (int i = 0; i < NUM_USERS; i++)
    {
      final String uid = "user." + i;
      testDS.add(generateUserEntry(uid, userBaseDN,
          "Test", "User", "password"));
    }

    // Fetch the users one page at a time with page size equal to 1.
    int pageSize = 1;
    final Set<String> userIDs = new HashSet<String>();
    for (long startIndex = 1; startIndex <= NUM_USERS; startIndex += pageSize)
    {
      SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
      Resources<UserResource> resources = endpoint.query(null, null,
          new PageParameters(startIndex, pageSize));
      assertEquals(resources.getTotalResults(), NUM_USERS);
      assertEquals(resources.getStartIndex(), startIndex);
      assertEquals(resources.getItemsPerPage(), pageSize);
      for (final UserResource resource : resources)
      {
        final String userID = resource.getId();
        userIDs.add(userID);
      }
    }

    assertEquals(userIDs.size(), NUM_USERS);
  }



  /**
   * Provides test coverage for the POST operation on a user resource.
   *
   * @param service  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testPostUser(final SCIMService service)
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create the contents for a new user.
    final UserResource user = new UserResource(CoreSchema.USER_DESCRIPTOR);
    final Name name = new Name("Ms. Barbara J Jensen III", "Jensen", "J",
        "Barbara", "Ms", "III");
    user.setUserName("bjensen");
    user.setName(name);

    // Post the user via SCIM, returning selected attributes.
    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
    final UserResource user1 = endpoint.create(user, "id", "meta");

    // Check the returned user.
    assertNotNull(user1);
    assertNotNull(user1.getId());
    assertNull(user1.getName());
    assertNull(user1.getUserName());
    assertNotNull(user1.getMeta());
    assertNotNull(user1.getMeta().getCreated());
    assertNotNull(user1.getMeta().getLastModified());
    assertNotNull(user1.getMeta().getLocation());
    assertLocation(user1.getMeta().getLocation(), "Users", user1.getId());

    // Check the resource ID.
    assertEquals(endpoint.get(user1.getId()).getId(), user1.getId());

    // Verify that the entry was actually created.
    final Entry entry = testDS.getEntry("uid=bjensen," + userBaseDN);
    assertNotNull(entry);
    assertTrue(entry.hasAttributeValue("sn", "Jensen"));
    assertTrue(entry.hasAttributeValue("cn", "Ms. Barbara J Jensen III"));
    assertTrue(entry.hasAttributeValue("givenName", "Barbara"));

    // Verify that we can fetch the user using the returned resource URI.
    //assertNotNull(client.getUserByURI(response.getResourceURI()));
    //assertEquals(response.getResourceURI(), user1.getMeta().getLocation());
  }



  /**
   * Provides test coverage for the DELETE operation on a user resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testDeleteUser()
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // Create a user directly on the test DS.
    final String userDN = "uid=bjensen," + userBaseDN;
    testDS.add(generateUserEntry("bjensen", userBaseDN,
        "Barbara", "Jensen", "password"));


    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();

    // Determine the user ID.
    final String userID = endpoint.query(null).iterator().next().getId();

    // Delete the user through SCIM.
    endpoint.delete(userID);

    // Attempt to delete the user again.
    try
    {
      endpoint.delete(userID);
      assertTrue(false, "Should throw ResourceNotFoundException");
    }
    catch (ResourceNotFoundException e)
    {
      //expected
    }

    // Verify that the entry was actually deleted.
    final Entry entry = testDS.getEntry(userDN);
    assertNull(entry);

    // Create the contents for a user to be created via SCIM.
//    final UserResource user = new UserResource(CoreSchema.USER_DESCRIPTOR);
//    final Name name = new Name("Ms. Barbara J Jensen III", "Jensen", "J",
//        "Barbara", "Ms", "III");
//    user.setUserName("bjensen");
//    user.setName(name);

    // Create the user via SCIM.
    //final UserResource response = endpoint.create(user);

    // Delete the user by providing the returned resource URI.
    //assertTrue(client.deleteResourceByURI(response.getResourceURI()));

    // Verify that the entry was actually deleted.
    //assertNull(testDS.getEntry(userDN));

    // Tidy up.
    //client.stopClient();
  }



  /**
   * Provides test coverage for the PUT operation on a user resource.
   *
   * @param service  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testPutUser(final SCIMService service)
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    // The ID of the test user.
    final String userDN = "uid=bjensen," + userBaseDN;

    // Create the contents for a new user.
    final UserResource user = new UserResource(CoreSchema.USER_DESCRIPTOR);
    final Name name = new Name("Ms. Barbara J Jensen III", "Jensen", "J",
        "Barbara", "Ms", "III");
    user.setUserName("bjensen");
    user.setName(name);
    user.setId(userDN);

    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
    // Attempt to replace a user that does not exist.
    try
    {
      endpoint.update(user);
      assertTrue(false, "Should've thrown a ResourceNotFoundException");
    }
    catch(ResourceNotFoundException e)
    {
      // expected
    }

    // Post a new user.
    final UserResource user1 = endpoint.create(user);

    // Add a value that should be preserved during SCIM updates.
    testDS.modify(userDN, new Modification(ModificationType.ADD, "description",
        "This value should be preserved"));

    // Add some values to the user.

    user1.getName().setGivenName("Barbara");

    final Collection<com.unboundid.scim.data.Entry<String>> emails =
        new ArrayList<com.unboundid.scim.data.Entry<String>>(1);
    emails.add(new com.unboundid.scim.data.Entry<String>(
        "bjensen@example.com", "work", false));
    user1.setEmails(emails);

    final Collection<com.unboundid.scim.data.Entry<String>> phoneNumbers =
        new ArrayList<com.unboundid.scim.data.Entry<String>>(2);
    phoneNumbers.add(new com.unboundid.scim.data.Entry<String>(
        "800-864-8377", "work", false));
    phoneNumbers.add(new com.unboundid.scim.data.Entry<String>(
        "818-123-4567", "home", false));
    user1.setPhoneNumbers(phoneNumbers);

    final Collection<Address> addresses =
        new ArrayList<Address>(2);
    final Address workAddress = new Address("100 Universal City Plaza\n" +
        "Hollywood, CA 91608 USA", "100 Universal City Plaza", "Hollywood",
        "CA", "91608", "USA", "work", false);
    final Address homeAddress = new Address("456 Hollywood Blvd\nHollywood, " +
        "CA 91608 USA", null, null, null, null, null, "home", false);
    addresses.add(workAddress);
    addresses.add(homeAddress);
    user1.setAddresses(addresses);

    // Put the updated user.
    final UserResource user2 = endpoint.update(user1);

    // Verify that the LDAP entry was updated correctly.
    final Entry entry2 = testDS.getEntry(userDN);
    assertTrue(entry2.hasAttributeValue("givenName", "Barbara"));
    assertTrue(entry2.hasAttributeValue("mail", "bjensen@example.com"));
    assertTrue(entry2.hasAttributeValue("telephoneNumber", "800-864-8377"));
    assertTrue(entry2.hasAttributeValue("homePhone", "818-123-4567"));
    assertTrue(entry2.hasAttributeValue(
        "postalAddress", "100 Universal City Plaza$Hollywood, CA 91608 USA"));
    assertTrue(entry2.hasAttributeValue("street", "100 Universal City Plaza"));
    assertTrue(entry2.hasAttributeValue("l", "Hollywood"));
    assertTrue(entry2.hasAttributeValue("st", "CA"));
    assertTrue(entry2.hasAttributeValue("postalCode", "91608"));
    assertTrue(entry2.hasAttributeValue(
        "homePostalAddress", "456 Hollywood Blvd$Hollywood, CA 91608 USA"));
    assertTrue(entry2.hasAttribute("description"));
    assertNotNull(user2.getMeta().getLastModified());
    assertNotNull(user2.getMeta().getLocation());
    assertLocation(user2.getMeta().getLocation(), "Users", user2.getId());

    // Ensure that we can retrieve the user again using meta.location
    // assertNotNull(client.getUserByURI(user1.getMeta().getLocation()));

    // Remove some values from the user.

    Name newName = user2.getName();
    newName.setGivenName(null);
    user2.setName(newName);

    final Collection<com.unboundid.scim.data.Entry<String>> newPhoneNumbers =
        new ArrayList<com.unboundid.scim.data.Entry<String>>(1);
    for (final com.unboundid.scim.data.Entry<String> a :
        user2.getPhoneNumbers())
    {
      if (a.getType().equalsIgnoreCase("work"))
      {
        newPhoneNumbers.add(a);
      }
    }
    user2.setPhoneNumbers(newPhoneNumbers);

    final Collection<Address> newAddresses =
        new ArrayList<Address>(1);
    for (final Address a : user2.getAddresses())
    {
      if (a.getType().equalsIgnoreCase("work"))
      {
        newAddresses.add(a);
      }
    }
    user2.setAddresses(newAddresses);

    // Put the updated user.
    final UserResource user3 = endpoint.update(user2);

    final Entry entry3 = testDS.getEntry(userDN);
    assertFalse(entry3.hasAttribute("givenName"));
    assertTrue(entry3.hasAttributeValue("mail", "bjensen@example.com"));
    assertTrue(entry3.hasAttributeValue("telephoneNumber", "800-864-8377"));
    assertFalse(entry3.hasAttribute("homePhone"));
    assertTrue(entry3.hasAttributeValue(
        "postalAddress", "100 Universal City Plaza$Hollywood, CA 91608 USA"));
    assertTrue(entry3.hasAttributeValue("street", "100 Universal City Plaza"));
    assertTrue(entry3.hasAttributeValue("l", "Hollywood"));
    assertTrue(entry3.hasAttributeValue("st", "CA"));
    assertTrue(entry3.hasAttributeValue("postalCode", "91608"));
    assertFalse(entry3.hasAttribute("homePostalAddress"));
    assertTrue(entry3.hasAttribute("description"));

    // Remove some more values from the user.
    user3.setEmails(null);
    user3.setAddresses(null);
    user3.setPhoneNumbers(null);

    // Put the updated user.
    final UserResource user4 = endpoint.update(user3);

    final Entry entry4 = testDS.getEntry(userDN);
    assertFalse(entry4.hasAttribute("givenName"));
    assertFalse(entry4.hasAttribute("mail"));
    assertFalse(entry4.hasAttribute("telephoneNumber"));
    assertFalse(entry4.hasAttribute("homePhone"));
    assertFalse(entry4.hasAttribute("postalAddress"));
    assertFalse(entry4.hasAttribute("street"));
    assertFalse(entry4.hasAttribute("l"));
    assertFalse(entry4.hasAttribute("st"));
    assertFalse(entry4.hasAttribute("postalCode"));
    assertFalse(entry4.hasAttribute("homePostalAddress"));
    assertTrue(entry4.hasAttribute("description"));

    // Change and attribute that is mapped to a attribute in the RDN and make
    // sure the DN is changed as well
    user4.setDisplayName("displayName");
    user4.setUserName("updated-bjensen");

    endpoint.update(user4);

    final Entry entry5 = testDS.getEntry("uid=updated-bjensen," + userBaseDN);
    assertTrue(entry5.hasAttributeValue("displayName", "displayName"));
  }



  /**
   * Provides test coverage for the write operations on a group resource.
   *
   * @param service  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testWriteGroup(final SCIMService service)
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

    // Delete the groups.
    groupEndpoint.delete(groupB.getId());
    groupEndpoint.delete(groupA.getId());

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
    final UserResource user1 = endpoint.create(user);
    assertNotNull(user1);

    // Add some values to the user.
    Name newName = user1.getName();
    newName.setGivenName("Barbara");
    user1.setName(newName);

    // Put the updated user.
    endpoint.update(user1);

    // Delete the user.
    endpoint.delete(user1.getId());
  }

}
