/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.scim.schema.User;
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
    final SCIMClient client = new SCIMClient("localhost", getSSTestPort(), "/");
    client.startClient();

    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));

    // A user ID that does not exist should not return anything.
    assertNull(client.getUser("does-not-exist"));

    // Create a user directly on the test DS and ensure it can be fetched
    // using the SCIM client.
    testDS.add(generateUserEntry("bjensen", "dc=example,dc=com",
                                 "Barbara", "Jensen", "password"));
    final String entryUUID1 =
        testDS.getEntry("uid=bjensen,dc=example,dc=com", "entryUUID").
            getAttributeValue("entryUUID");
    final User user1 = client.getUser(entryUUID1);
    assertNotNull(user1);
    assertEquals(user1.getId(), entryUUID1);
    assertEquals(user1.getName().getFamilyName(), "Jensen");
    assertEquals(user1.getName().getGivenName(), "Barbara");
    assertEquals(user1.getUserName(), "bjensen");

    // Fetch selected attributes only.
    final User partialUser1 = client.getUser(entryUUID1, "username");
    assertNotNull(partialUser1);
    assertNull(partialUser1.getId());
    assertNull(partialUser1.getName());
    assertEquals(partialUser1.getUserName(), "bjensen");

    // Tidy up.
    client.stopClient();
  }
}
