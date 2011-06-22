/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim.schema.Name;
import com.unboundid.scim.schema.User;
import com.unboundid.scim.sdk.PostUserResponse;
import com.unboundid.scim.sdk.SCIMClient;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;



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

    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));

    // A user ID that does not exist should not return anything.
    assertNull(client.getUser("cn=does-not-exist"));

    // Create a user directly on the test DS and ensure it can be fetched
    // using the SCIM client.
    testDS.add(generateUserEntry("b jensen", "dc=example,dc=com",
                                 "Barbara", "Jensen", "password"));
    final User user1 = client.getUser("uid=b jensen,dc=example,dc=com");
    assertNotNull(user1);
    assertEquals(user1.getId(), "uid=b jensen,dc=example,dc=com");
    assertEquals(user1.getName().getFamilyName(), "Jensen");
    assertEquals(user1.getName().getGivenName(), "Barbara");
    assertEquals(user1.getUserName(), "b jensen");

    // Fetch selected attributes only.
    final User partialUser1 =
        client.getUser("uid=b jensen,dc=example,dc=com", "username",
                       "good night + good luck?");
    assertNotNull(partialUser1);
    assertNull(partialUser1.getId());
    assertNull(partialUser1.getName());
    assertEquals(partialUser1.getUserName(), "b jensen");

    // Tidy up.
    client.stopClient();
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

    // Post the user via SCIM.
    final PostUserResponse response = client.postUser(user, "id");
    final User user1 = response.getUser();
    assertNotNull(user1);
    assertEquals(user1.getId(), "uid=bjensen,dc=example,dc=com");
    assertNull(user1.getName());
    assertNull(user1.getUserName());

    // Verify that the entry was actually created.
    final Entry entry = testDS.getEntry("uid=bjensen,dc=example,dc=com");
    assertNotNull(entry);
    assertTrue(entry.hasAttributeValue("sn", "Jensen"));
    assertTrue(entry.hasAttributeValue("cn", "Ms. Barbara J Jensen III"));
    assertTrue(entry.hasAttributeValue("givenName", "Barbara"));

    // Verify that we can fetch the user using the returned resource URI.
    assertNotNull(client.getUserByURI(response.getResourceURI()));

    // Tidy up.
    client.stopClient();
  }
}
