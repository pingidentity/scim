/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.wink;

import com.unboundid.scim.schema.Name;
import com.unboundid.scim.schema.ObjectFactory;
import com.unboundid.scim.schema.PluralAttribute;
import com.unboundid.scim.schema.User;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.handlers.BasicAuthSecurityHandler;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import java.net.URI;

/**
 * Tests SCIM operations via the Apache Wink Client.
 */
public class SCIMWinkClientTest extends SCIMRITestCase {

  /**
   * Tests creation of a new user via XML.
   *
   * @throws Exception if error creating a new user.
   */
  @Test
  public void testCreateUser() throws Exception {
    ClientConfig config = new ClientConfig();
    BasicAuthSecurityHandler basicAuth = new BasicAuthSecurityHandler();
    basicAuth.setUserName(
      getTestBindDN() != null ? getTestBindDN() : "cn=Directory Manager");
    basicAuth.setPassword(
      getTestBindPassword() != null ? getTestBindPassword() : "password");
    config.handlers(basicAuth);

    RestClient client = new RestClient(config);

    // create new user
    User newUser = new User();
    newUser.setUserName("bjensen");
    newUser.setExternalId("bjsensen");
    newUser.setDisplayName("Ms. Barbara J Jensen III");
    Name name = new Name();
    name.setFamilyName("Jensen");
    name.setGivenName("Barbara");
    name.setFormatted("Ms. Barbara J Jensen III");
    newUser.setName(name);

    User.Emails emails = new User.Emails();
    PluralAttribute email = new PluralAttribute();
    email.setValue("bjensen@example.com");
    emails.getEmail().add(email);
    newUser.setEmails(emails);

    URI postUri =
      new URI("http", null, "localhost", getSSTestPort(), "/User", null, null);

    ObjectFactory o = new ObjectFactory();
    JAXBElement<User> user = o.createUser(newUser);

    ClientResponse post =
      client.resource(postUri).accept(MediaType.APPLICATION_XML)
        .contentType(MediaType.APPLICATION_XML).post(user);
    assertTrue(post.getStatusCode() == 201);
  }
}
