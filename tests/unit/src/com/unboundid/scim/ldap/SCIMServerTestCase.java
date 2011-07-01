/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.scim.schema.Address;
import com.unboundid.scim.schema.Name;
import com.unboundid.scim.schema.PluralAttribute;
import com.unboundid.scim.schema.Resource;
import com.unboundid.scim.schema.User;
import com.unboundid.scim.sdk.PostUserResponse;
import com.unboundid.scim.sdk.SCIMClient;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;

import java.util.List;



/**
 * This class provides test coverage for the SCIMServer class.
 */
public class SCIMServerTestCase
    extends SCIMRITestCase
{
  /**
   * Provides test coverage for the GET operation on a user resource.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testGetUser()
      throws Exception
  {
    // Start a client for the SCIM operations.
    final SCIMClient client = new SCIMClient("localhost", getSSTestPort(), "");
    client.startClient();

    try
    {
      // Test receiving XML content.
      client.setAcceptXML(true);
      client.setAcceptJSON(false);
      testGetUser(client);

      // Test receiving JSON content.
      client.setAcceptXML(false);
      client.setAcceptJSON(true);
      testGetUser(client);
    }
    finally
    {
      client.stopClient();
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
    // Start a client for the SCIM operations.
    final SCIMClient client = new SCIMClient("localhost", getSSTestPort(), "");
    client.startClient();

    try
    {
      // Test receiving XML content.
      client.setAcceptXML(true);
      client.setAcceptJSON(false);
      testGetUsers(client);

      // Test receiving JSON content.
      client.setAcceptXML(false);
      client.setAcceptJSON(true);
      testGetUsers(client);
    }
    finally
    {
      client.stopClient();
    }
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
    // Start a client for the SCIM operations.
    final SCIMClient client = new SCIMClient("localhost", getSSTestPort(), "");
    client.startClient();

    try
    {
      // Test sending and receiving XML content.
      client.setSendJSON(false);
      client.setAcceptXML(true);
      client.setAcceptJSON(false);
      testPostUser(client);

      // Test sending and receiving JSON content.
      client.setSendJSON(true);
      client.setAcceptXML(false);
      client.setAcceptJSON(true);
      testPostUser(client);

      // Test sending XML and receiving JSON content.
      client.setSendJSON(false);
      client.setAcceptXML(false);
      client.setAcceptJSON(true);
      testPostUser(client);

      // Test sending JSON and receiving XML content.
      client.setSendJSON(true);
      client.setAcceptXML(true);
      client.setAcceptJSON(false);
      testPostUser(client);
    }
    finally
    {
      client.stopClient();
    }
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
    // Start a client for the SCIM operations.
    final SCIMClient client = new SCIMClient("localhost", getSSTestPort(), "");
    client.startClient();

    try
    {
      // Test sending and receiving XML content.
      client.setSendJSON(false);
      client.setAcceptXML(true);
      client.setAcceptJSON(false);
      testPutUser(client);

      // Test sending and receiving JSON content.
      client.setSendJSON(true);
      client.setAcceptXML(false);
      client.setAcceptJSON(true);
      testPutUser(client);

      // Test sending XML and receiving JSON content.
      client.setSendJSON(false);
      client.setAcceptXML(false);
      client.setAcceptJSON(true);
      testPutUser(client);

      // Test sending JSON and receiving XML content.
      client.setSendJSON(true);
      client.setAcceptXML(true);
      client.setAcceptJSON(false);
      testPutUser(client);
    }
    finally
    {
      client.stopClient();
    }
  }



  /**
   * Provides test coverage for the GET operation on a user resource.
   *
   * @param client  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testGetUser(final SCIMClient client)
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));

    // A user ID that does not exist should not return anything.
    assertNull(client.getUser("cn=does-not-exist"));

    // Create a user directly on the test DS.
    testDS.add(generateUserEntry("b jensen", "dc=example,dc=com",
                                 "Barbara", "Jensen", "password"));

    // Fetch the user through the SCIM client.
    final User user1 = client.getUser("uid=b jensen,dc=example,dc=com");
    assertNotNull(user1);
    assertEquals(user1.getId(), "uid=b jensen,dc=example,dc=com");
    assertEquals(user1.getName().getFamilyName(), "Jensen");
    assertEquals(user1.getName().getGivenName(), "Barbara");
    assertEquals(user1.getUserName(), "b jensen");
    assertNotNull(user1.getMeta());
    assertNotNull(user1.getMeta().getCreated());
    assertNotNull(user1.getMeta().getLastModified());

    // Fetch selected attributes only.
    final User partialUser1 =
        client.getUser("uid=b jensen,dc=example,dc=com", "username",
                       "good night + good luck?");
    assertNotNull(partialUser1);
    assertNull(partialUser1.getId());
    assertNull(partialUser1.getName());
    assertEquals(partialUser1.getUserName(), "b jensen");
  }



  /**
   * Provides test coverage for the GET operation to fetch selected users.
   *
   * @param client  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testGetUsers(final SCIMClient client)
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));

    // Create some users directly on the test DS.
    testDS.add(generateUserEntry("user.1", "dc=example,dc=com",
                                 "User", "One", "password",
                                 new Attribute("mail", "user.1@example.com"),
                                 new Attribute("l", "Austin")));
    testDS.add(generateUserEntry("user.2", "dc=example,dc=com",
                                 "User", "Two", "password"));

    // Fetch all the users through the SCIM client.
    List<Resource> resources = client.getResources("Users", null);

    assertEquals(resources.size(), 2);
    for (final Resource r : resources)
    {
      assertTrue(r instanceof User);
      final User u = (User)r;
      assertNotNull(u.getId());
      assertNotNull(u.getMeta());
      assertNotNull(u.getMeta().getCreated());
      assertNotNull(u.getMeta().getLastModified());
      assertNotNull(u.getName());
      assertNotNull(u.getName().getFamilyName());
      assertNotNull(u.getName().getGivenName());
    }

    resources = client.getResources(
        "Users", SCIMFilter.createEqualityFilter("userName", "user.1"));
    assertEquals(resources.size(), 1);

    resources = client.getResources(
        "Users", SCIMFilter.createEqualityFilter("userName", "User.1"));
    assertEquals(resources.size(), 0);

    resources = client.getResources(
        "Users",
        SCIMFilter.createIgnoresCaseEqualityFilter("userName", "User.1"));
    assertEquals(resources.size(), 1);

    resources = client.getResources(
        "Users", SCIMFilter.createStartsWithFilter("userName", "user"));
    assertEquals(resources.size(), 2);

    resources = client.getResources(
        "Users", SCIMFilter.createContainsFilter("userName", "1"));
    assertEquals(resources.size(), 1);

    resources = client.getResources(
        "Users", SCIMFilter.createPresenceFilter("userName"));
    assertEquals(resources.size(), 2);

    resources = client.getResources(
        "Users", SCIMFilter.createEqualityFilter("name.formatted", "User One"));
    assertEquals(resources.size(), 1);

    resources = client.getResources(
        "Users",
        SCIMFilter.createEqualityFilter("emails.value", "user.1@example.com"));
    assertEquals(resources.size(), 1);

    resources = client.getResources(
        "Users",
        SCIMFilter.createEqualityFilter("addresses.locality", "Austin"));
    assertEquals(resources.size(), 1);
  }



  /**
   * Provides test coverage for the POST operation on a user resource.
   *
   * @param client  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testPostUser(final SCIMClient client)
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));

    // Create the contents for a new user.
    final User user = new User();
    final Name name = new Name();
    name.setFamilyName("Jensen");
    name.setFormatted("Ms. Barbara J Jensen III");
    name.setGivenName("Barbara");
    user.setUserName("bjensen");
    user.setName(name);

    // Post the user via SCIM, returning selected attributes.
    final PostUserResponse response = client.postUser(user, "id", "meta");

    // Check the returned user.
    final User user1 = response.getUser();
    assertNotNull(user1);
    assertEquals(user1.getId(), "uid=bjensen,dc=example,dc=com");
    assertNull(user1.getName());
    assertNull(user1.getUserName());
    assertNotNull(user1.getMeta());
    assertNotNull(user1.getMeta().getCreated());
    assertNotNull(user1.getMeta().getLastModified());

    // Verify that the entry was actually created.
    final Entry entry = testDS.getEntry("uid=bjensen,dc=example,dc=com");
    assertNotNull(entry);
    assertTrue(entry.hasAttributeValue("sn", "Jensen"));
    assertTrue(entry.hasAttributeValue("cn", "Ms. Barbara J Jensen III"));
    assertTrue(entry.hasAttributeValue("givenName", "Barbara"));

    // Verify that we can fetch the user using the returned resource URI.
    assertNotNull(client.getUserByURI(response.getResourceURI()));
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
    // Start a client for the SCIM operations.
    final SCIMClient client = new SCIMClient("localhost", getSSTestPort(), "");
    client.startClient();

    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));

    // Create a user directly on the test DS.
    final String userDN = "uid=bjensen,dc=example,dc=com";
    testDS.add(generateUserEntry("bjensen", "dc=example,dc=com",
                                 "Barbara", "Jensen", "password"));


    // Delete the user through SCIM.
    assertTrue(client.deleteUser(userDN));

    // Attempt to delete the user again.
    assertFalse(client.deleteUser(userDN));

    // Verify that the entry was actually deleted.
    final Entry entry = testDS.getEntry(userDN);
    assertNull(entry);

    // Create the contents for a user to be created via SCIM.
    final User user = new User();
    final Name name = new Name();
    name.setFamilyName("Jensen");
    name.setFormatted("Ms. Barbara J Jensen III");
    name.setGivenName("Barbara");
    user.setUserName("bjensen");
    user.setName(name);

    // Create the user via SCIM.
    final PostUserResponse response = client.postUser(user, "id");

    // Delete the user by providing the returned resource URI.
    assertTrue(client.deleteResourceByURI(response.getResourceURI()));

    // Verify that the entry was actually deleted.
    assertNull(testDS.getEntry(userDN));

    // Tidy up.
    client.stopClient();
  }



  /**
   * Provides test coverage for the PUT operation on a user resource.
   *
   * @param client  The SCIM client to use during the test.
   *
   * @throws Exception  If the test failed.
   */
  private void testPutUser(final SCIMClient client)
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));

    // The ID of the test user.
    final String userDN = "uid=bjensen,dc=example,dc=com";

    // Create the contents for a new user.
    final User user = new User();
    final Name name = new Name();
    name.setFormatted("Ms. Barbara J Jensen III");
    name.setFamilyName("Jensen");
    user.setUserName("bjensen");
    user.setName(name);

    // Attempt to replace a user that does not exist.
    assertNull(client.putUser(userDN, user));

    // Post a new user.
    final PostUserResponse response = client.postUser(user);
    final User user1 = response.getUser();
    assertNotNull(user1);

    // Add a value that should be preserved during SCIM updates.
    testDS.modify(userDN, new Modification(ModificationType.ADD, "description",
                                           "This value should be preserved"));

    // Add some values to the user.

    user1.getName().setGivenName("Barbara");

    final User.Emails emails = new User.Emails();
    final PluralAttribute email = new PluralAttribute();
    email.setType("work");
    email.setValue("bjensen@example.com");
    emails.getEmail().add(email);
    user1.setEmails(emails);

    final User.PhoneNumbers phoneNumbers = new User.PhoneNumbers();
    final PluralAttribute workPhoneNumber = new PluralAttribute();
    workPhoneNumber.setType("work");
    workPhoneNumber.setValue("800-864-8377");
    final PluralAttribute homePhoneNumber = new PluralAttribute();
    homePhoneNumber.setType("home");
    homePhoneNumber.setValue("818-123-4567");
    phoneNumbers.getPhoneNumber().add(workPhoneNumber);
    phoneNumbers.getPhoneNumber().add(homePhoneNumber);
    user1.setPhoneNumbers(phoneNumbers);

    final User.Addresses addresses = new User.Addresses();
    final Address workAddress = new Address();
    workAddress.setType("work");
    workAddress.setFormatted("100 Universal City Plaza\n" +
                             "Hollywood, CA 91608 USA");
    workAddress.setStreetAddress("100 Universal City Plaza");
    workAddress.setLocality("Hollywood");
    workAddress.setRegion("CA");
    workAddress.setPostalCode("91608");
    workAddress.setCountry("USA");
    final Address homeAddress = new Address();
    homeAddress.setFormatted("456 Hollywood Blvd\nHollywood, CA 91608 USA");
    homeAddress.setType("home");
    addresses.getAddress().add(workAddress);
    addresses.getAddress().add(homeAddress);
    user1.setAddresses(addresses);

    // Put the updated user.
    final User user2 = client.putUser(user1.getId(), user1);

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

    // Remove some values from the user.

    user2.getName().setGivenName(null);

    final User.PhoneNumbers newPhoneNumbers = new User.PhoneNumbers();
    for (final PluralAttribute a : user2.getPhoneNumbers().getPhoneNumber())
    {
      if (a.getType().equalsIgnoreCase("work"))
      {
        newPhoneNumbers.getPhoneNumber().add(a);
      }
    }
    user2.setPhoneNumbers(newPhoneNumbers);

    final User.Addresses newAddresses = new User.Addresses();
    for (final Address a : user2.getAddresses().getAddress())
    {
      if (a.getType().equalsIgnoreCase("work"))
      {
        newAddresses.getAddress().add(a);
      }
    }
    user2.setAddresses(newAddresses);

    // Put the updated user.
    final User user3 = client.putUser(user2.getId(), user2);

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
    client.putUser(user3.getId(), user3);

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
   * Provides test coverage for PUT and DELETE operations on a user resource
   * invoking the operations using POST with a method override.
   *
   * @throws Exception  If the test failed.
   */
  @Test
  public void testMethodOverride()
      throws Exception
  {
    // Start a client for the SCIM operations.
    final SCIMClient client = new SCIMClient("localhost", getSSTestPort(), "");
    client.startClient();

    // Tell the client to use method override.
    client.setPutUsesMethodOverride(true);
    client.setDeleteUsesMethodOverride(true);

    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));

    // The ID of the test user.
    final String userDN = "uid=bjensen,dc=example,dc=com";

    // Create the contents for a new user.
    final User user = new User();
    final Name name = new Name();
    name.setFormatted("Ms. Barbara J Jensen III");
    name.setFamilyName("Jensen");
    user.setUserName("bjensen");
    user.setName(name);

    // Post a new user.
    final PostUserResponse response = client.postUser(user);
    final User user1 = response.getUser();
    assertNotNull(user1);

    // Add some values to the user.
    user1.getName().setGivenName("Barbara");

    // Put the updated user.
    client.putUser(user1.getId(), user1);

    // Delete the user.
    client.deleteUser(userDN);

    // Tidy up.
    client.stopClient();
  }



}
