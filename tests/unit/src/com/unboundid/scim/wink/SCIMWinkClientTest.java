/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.wink;

import com.unboundid.scim.schema.Name;
import com.unboundid.scim.schema.PluralAttribute;
import com.unboundid.scim.schema.User;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.apache.wink.client.ClientResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.URISyntaxException;
import java.util.Date;

/**
 * Tests SCIM operations via the Apache Wink Client.
 */
public class SCIMWinkClientTest extends SCIMRITestCase {
  private static final String USERNAME = "cn=Directory Manager";
  private static final String PASSWORD = "password";
  private static final String HOST = "localhost";

  /**
   * Performs pre-test processing.
   * @throws Exception if error initializing tests.
   */
  @BeforeClass
  public void beforeClass() throws Exception {
    // Initialize the in-memory test DS with the base entry.
    getTestDS(true, false);

  }

  /**
   * Tests creation of a new user via XML.
   *
   * @throws Exception if error creating a new user.
   */
  @Test
  public void testCreateUser() throws Exception {
    assertTrue(this.createUser().getStatusCode() == 201);
  }

  /**
   * Tests updating a new user via XML.
   *
   * @throws Exception if error editing a new user.
   */
  @Test
  public void testEditUser() throws Exception {
    String changedAttributeValue = "ASSERT_DISPLAY_NAME_CHANGED";
    User user = this.getUserFromResponse(this.createUser());
    user.setDisplayName(changedAttributeValue);
    final ClientResponse response = getClient().editUser(user);
    assertTrue(response.getStatusCode() == 200);
    User userFromResponse = this.getUserFromResponse(response);
    assertEquals(userFromResponse.getDisplayName(), changedAttributeValue);
  }

  /**
   * Tests updating a new user via XML.
   *
   * @throws Exception if error editing a new user.
   */
  @Test
  public void testDeleteUser() throws Exception {
    User user = this.getUserFromResponse(this.createUser());
    String id = user.getId();
    ClientResponse clientResponse = getClient().deleteUser(id);
    assertTrue(clientResponse.getStatusCode() == 200);
  }


  /**
   * Tests user retrieval via XML.
   *
   * @throws Exception if error fetching the User.
   */
  @Test
  public void testGetUser() throws Exception {
    User user = this.getUserFromResponse(this.createUser());
    ClientResponse clientResponse = this.getClient().getUser(user.getId());
    assertTrue(clientResponse.getStatusCode() == 200);
    User userFromResponse = this.getUserFromResponse(clientResponse);
    // todo: do deep equality check
  }

  /**
   * Returns an initialized client.
   *
   * @return A Wink client.
   * @throws URISyntaxException If error parsing the Service Provider endpoint.
   */
  private Client getClient() throws URISyntaxException {
    return new Client(USERNAME, PASSWORD, HOST, getSSTestPort());
  }

  /**
   * Creates a new SCIM User.
   *
   * @return The newly created SCIM User.
   * @throws URISyntaxException If error parsing the Service Provider endpoint.
   */
  private ClientResponse createUser() throws URISyntaxException {
    return getClient().createUser(getTemplateUser());
  }

  /**
   * Pulls the User POJO from the Service Provider response.
   *
   * @param response The Service Provider response.
   * @return The SCIM User POJO.
   */
  private User getUserFromResponse(final ClientResponse response) {
    User user = response.getEntity(User.class);
    assertNotNull(user);
    return user;
  }

  /**
   * Creates a POJO representing the canonical SCIM User 'Babs'.
   *
   * @return The templated User.
   */
  private User getTemplateUser() {
    // create new user
    User user = new User();
    // make the user unique enough
    user.setUserName("bjensen" + new Date().getTime());
    user.setExternalId(user.getUserName());
    user.setDisplayName("Ms. Barbara J Jensen III");
    Name name = new Name();
    name.setFamilyName("Jensen");
    name.setGivenName("Barbara");
    name.setFormatted("Ms. Barbara J Jensen III");
    user.setName(name);

    User.Emails emails = new User.Emails();
    PluralAttribute email = new PluralAttribute();
    email.setValue("bjensen@example.com");
    emails.getEmail().add(email);
    user.setEmails(emails);
    return user;
  }
}
