/*
 * Copyright 2011 UnboundID Corp.
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
import org.testng.annotations.BeforeClass;

import javax.ws.rs.core.MediaType;
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
public class SCIMServerTestCase
    extends SCIMRITestCase
{
  private SCIMService service;

  /**
   * Sets up the SCIMService.
   */
  @BeforeClass
  public void setup()
  {
    // Start a client for the SCIM operations.
    service = new SCIMService(URI.create("http://localhost:"+getSSTestPort()),
        "cn=Manager", "password");
  }



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
    testDS.add(generateUserEntry("b jensen", "dc=example,dc=com",
        "Barbara", "Jensen", "password",
        new Attribute("mail", "user.1@example.com"),
        new Attribute("l", "Austin"),
        new Attribute("postalCode", "78759")));

    // Fetch the user through the SCIM client.
    final UserResource user1 = endpoint.get("uid=b jensen,dc=example,dc=com");
    assertNotNull(user1);
    assertEquals(user1.getId(), "uid=b jensen,dc=example,dc=com");
    assertNotNull(user1.getMeta());
    assertNotNull(user1.getMeta().getCreated());
    assertNotNull(user1.getMeta().getLastModified());
    assertNotNull(user1.getMeta().getLocation());
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
        endpoint.get("uid=b jensen,dc=example,dc=com", null,
                     "USERNAME", "name.FORMATTED",
                     "addresses.postalCode",
                     "UrN:sCiM:ScHeMaS:cOrE:1.0:addresses.streetAddress");
    assertNotNull(partialUser);
    assertTrue(partialUser.getId().equalsIgnoreCase(
        "uid=b jensen,dc=example,dc=com"));
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
    partialUser =
        endpoint.get("uid=b jensen,dc=example,dc=com", null,
                     "meta.location");
    assertNotNull(partialUser);
    assertNotNull(partialUser.getId());
    assertNotNull(partialUser.getMeta());
    assertNull(partialUser.getMeta().getCreated());
    assertNull(partialUser.getMeta().getLastModified());
    assertNotNull(partialUser.getMeta().getLocation());
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

    // Create a static group directly on the test DS.
    testDS.add(generateUserEntry("bjensen", "dc=example,dc=com",
        "Barbara", "Jensen", "password"));
    testDS.add(generateGroupOfUniqueNamesEntry(
        "group1", "dc=example,dc=com",
        "uid=bjensen,dc=example,dc=com"));

    // Fetch the Group through the SCIM client.
    SCIMEndpoint<GroupResource> endpoint = service.getGroupEndpoint();
    final GroupResource group1 = endpoint.get("cn=group1,dc=example,dc=com");
    assertNotNull(group1);
    assertEquals(group1.getId(), "cn=group1,dc=example,dc=com");
    assertEquals(group1.getDisplayName(), "group1");
    assertNotNull(group1.getMembers());
    assertEquals(group1.getMembers().iterator().next().getValue(),
        "uid=bjensen,dc=example,dc=com");
    assertNotNull(group1.getMeta());
    assertNotNull(group1.getMeta().getCreated());
    assertNotNull(group1.getMeta().getLastModified());
    assertNotNull(group1.getMeta().getLocation());

    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    final UserResource user = userEndpoint.get("uid=bjensen,dc=example,dc=com");
    assertNotNull(user.getGroups());
    assertEquals(user.getGroups().size(), 1);
    assertEquals(user.getGroups().iterator().next().getValue(),
                 "cn=group1,dc=example,dc=com");
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

    // Create some users directly on the test DS.
    testDS.add(generateUserEntry("user.1", "dc=example,dc=com",
        "User", "One", "password",
        new Attribute("mail", "user.1@example.com"),
        new Attribute("l", "Austin"),
        new Attribute("postalCode", "78759")));
    testDS.add(generateUserEntry("user.2", "dc=example,dc=com",
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

    resources = endpoint.query("id eq \"uid=user.1,dc=example,dc=com\"");
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

    // Create some users directly on the test DS.
    for (final String sortValue : Arrays.asList("B", "C", "A"))
    {
      testDS.add(generateUserEntry(sortValue, "dc=example,dc=com",
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

    // Create some users directly on the test DS.
    final long NUM_USERS = 4;
    for (int i = 0; i < NUM_USERS; i++)
    {
      final String uid = "user." + i;
      testDS.add(generateUserEntry(uid, "dc=example,dc=com",
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
    assertEquals(user1.getId(), "uid=bjensen,dc=example,dc=com");
    assertNull(user1.getName());
    assertNull(user1.getUserName());
    assertNotNull(user1.getMeta());
    assertNotNull(user1.getMeta().getCreated());
    assertNotNull(user1.getMeta().getLastModified());
    assertNotNull(user1.getMeta().getLocation());

    // Verify that the entry was actually created.
    final Entry entry = testDS.getEntry("uid=bjensen,dc=example,dc=com");
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

    // Create a user directly on the test DS.
    final String userDN = "uid=bjensen,dc=example,dc=com";
    testDS.add(generateUserEntry("bjensen", "dc=example,dc=com",
        "Barbara", "Jensen", "password"));


    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
    // Delete the user through SCIM.
    endpoint.delete(userDN);

    // Attempt to delete the user again.
    try
    {
      endpoint.delete(userDN);
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
    final UserResource user = new UserResource(CoreSchema.USER_DESCRIPTOR);
    final Name name = new Name("Ms. Barbara J Jensen III", "Jensen", "J",
        "Barbara", "Ms", "III");
    user.setUserName("bjensen");
    user.setName(name);

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

    // The ID of the test user.
    final String userDN = "uid=bjensen,dc=example,dc=com";

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
    assertNotNull(user1.getMeta().getLastModified());
    assertNotNull(user1.getMeta().getLocation());

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
    endpoint.update(user3);

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
    testDS.add(generateUserEntry(
        "user.1", "dc=example,dc=com", "Test", "User", "password"));
    testDS.add(generateUserEntry(
        "user.2", "dc=example,dc=com", "Test", "User", "password"));

    final String idGroupA = "cn=group A,dc=example,dc=com";
    final String idGroupB = "cn=group B,dc=example,dc=com";

    // Create the contents for the groups.
    GroupResource groupA = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    groupA.setDisplayName("group A");
    final Collection<com.unboundid.scim.data.Entry<String>> membersA =
        new ArrayList<com.unboundid.scim.data.Entry<String>>(1);
    final com.unboundid.scim.data.Entry<String> member1 =
        new com.unboundid.scim.data.Entry<String>(
            "uid=user.1,dc=example,dc=com", "User", false);
    membersA.add(member1);
    groupA.setMembers(membersA);

    GroupResource groupB = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    groupB.setDisplayName("group B");

    SCIMEndpoint<GroupResource> endpoint = service.getGroupEndpoint();
    // Post the new groups.
    groupA = endpoint.create(groupA);
    assertNotNull(groupA);
    Entry entry = testDS.getEntry(idGroupA);
    assertTrue(entry.hasAttributeValue("uniqueMember",
        "uid=user.1,dc=example,dc=com"));

    groupB = endpoint.create(groupB);
    assertNotNull(groupB);

    // Add a value that should be preserved during SCIM updates.
    testDS.modify(idGroupA,
        new Modification(ModificationType.ADD, "description",
            "This value should be preserved"));
    testDS.modify(idGroupB,
        new Modification(ModificationType.ADD, "description",
            "This value should be preserved"));

    // Add some members to each group.
    final com.unboundid.scim.data.Entry<String> member2 =
        new com.unboundid.scim.data.Entry<String>(
            "uid=user.2,dc=example,dc=com", "User", false);
    Collection<com.unboundid.scim.data.Entry<String>> newMembers =
        groupA.getMembers();
    newMembers.add(member2);
    groupA.setMembers(newMembers);

    final com.unboundid.scim.data.Entry<String> memberA =
        new com.unboundid.scim.data.Entry<String>(
            idGroupA, "Group", false);
    memberA.setType("Group");
    memberA.setValue(idGroupA);
    newMembers = new ArrayList<com.unboundid.scim.data.Entry<String>>(1);
    newMembers.add(memberA);
    groupB.setMembers(newMembers);

    // Put the updated groups.
    groupA = endpoint.update(groupA);
    groupB = endpoint.update(groupB);

    assertEquals(groupA.getMembers().size(), 2);
    Iterator<com.unboundid.scim.data.Entry<String>> i =
        groupA.getMembers().iterator();
    assertEquals(i.next().getValue(), "uid=user.1,dc=example,dc=com");
    assertEquals(i.next().getValue(), "uid=user.2,dc=example,dc=com");

    assertEquals(groupB.getMembers().size(), 1);
    assertEquals(groupB.getMembers().iterator().next().getValue(), idGroupA);

    assertNotNull(groupA.getMeta().getLastModified());
    assertNotNull(groupA.getMeta().getLocation());
    assertNotNull(groupB.getMeta().getLastModified());
    assertNotNull(groupB.getMeta().getLocation());

    // Verify that the LDAP entries were updated correctly.
    entry = testDS.getEntry(idGroupA);
    assertTrue(entry.hasAttributeValue("uniqueMember",
        "uid=user.1,dc=example,dc=com"));
    assertTrue(entry.hasAttributeValue("uniqueMember",
        "uid=user.2,dc=example,dc=com"));
    assertTrue(entry.hasAttribute("description"));

    entry = testDS.getEntry(idGroupB);
    assertTrue(entry.hasAttributeValue("uniqueMember", idGroupA));
    assertTrue(entry.hasAttribute("description"));

    // Delete the groups.
    endpoint.delete(idGroupB);
    endpoint.delete(idGroupA);

    assertNull(testDS.getEntry(idGroupA));
    assertNull(testDS.getEntry(idGroupB));
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

    // The ID of the test user.
    final String userDN = "uid=bjensen,dc=example,dc=com";

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
    endpoint.delete(userDN);
  }



}
