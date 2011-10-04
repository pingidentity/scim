/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri.client;

import com.unboundid.scim.schema.ObjectFactory;
import com.unboundid.scim.schema.User;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.RestClient;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Example SCIM client that exercises the SCIM API.
 */
public class Client {
  /**
   * The SCIM User endpoint.
   */
  public static final String USER_ENDPOINT = "/User";
  private String username;
  private String password;
  private String hostname;
  private Integer port;

  private RestClient client = null;
  private URI userEndpointUri = null;

  /**
   * Constructs a Client.
   * @param username The SCIM Consumer username.
   * @param password The SCIM Consumer password.
   * @param hostname The SCIM Service Provider hostname.
   * @param port     The SCIM Service Provider port.
   *
   * @throws URISyntaxException If error parsing the Service Provider endpoint.
   */
  public Client(final String username, final String password,
    final String hostname, final Integer port) throws URISyntaxException {
    this.username = username;
    this.password = password;
    this.hostname = hostname;
    this.port = port;

    ClientConfig config = new ClientConfig();
    // setup BASIC Auth
//    BasicAuthSecurityHandler basicAuth = new BasicAuthSecurityHandler();
//    basicAuth.setUserName(username);
//    basicAuth.setPassword(password);
    HttpBasicAuthSecurityHandler basicAuth = new HttpBasicAuthSecurityHandler
      (username,password);
    config.handlers(basicAuth);
    this.client = new RestClient(config);
    this.userEndpointUri =
      new URI("http", null, hostname, port, USER_ENDPOINT, null, null);
  }


  /**
   * Creates a new User.
   *
   * @param user The User to create.
   * @return The Service Provider response.
   */
  public ClientResponse createUser(final User user) {
    return client.resource(userEndpointUri).accept(MediaType.APPLICATION_XML)
      .contentType(MediaType.APPLICATION_XML).post(this.getJaxbUser(user));
  }

  /**
   * @param user The edited User.
   * @return The Service Provider response.
   * @throws URISyntaxException If error parsing the Service Provider endpoint.
   */
  public ClientResponse editUser(final User user) throws URISyntaxException {
    return client.resource(getExistantUserEndpoint(user.getId()).toString())
      .accept(MediaType.APPLICATION_XML).contentType(MediaType.APPLICATION_XML)
      .put(this.getJaxbUser(user));
  }


  /**
   * Deletes a specified User from the SCIM Service Provider.
   *
   * @param userId The SCIM id attribute value.
   * @return The Service Provider response.
   * @throws URISyntaxException If error parsing the Service Provider endpoint.
   */
  public ClientResponse deleteUser(final String userId)
    throws URISyntaxException {
    return client.resource(getExistantUserEndpoint(userId).toString())
      .accept(MediaType.APPLICATION_XML).contentType(MediaType.APPLICATION_XML)
      .delete();
  }


  /**
   * Retrieves a SCIM User.
   *
   * @param userId The SCIM id attribute value.
   * @return The Service Provider response.
   * @throws URISyntaxException If error parsing the Service Provider endpoint.
   */
  public ClientResponse getUser(final String userId) throws URISyntaxException {
    return client.resource(getExistantUserEndpoint(userId).toString())
      .accept(MediaType.APPLICATION_XML).contentType(MediaType.APPLICATION_XML)
      .get();
  }

  /**
   * Constructs a URI endpoint for an existing SCIM User.
   *
   * @param id The id of the User.
   * @return A URI endpoint for the specified User.
   * @throws URISyntaxException Thrown if the constructed URI is invalid.
   */
  private URI getExistantUserEndpoint(final String id)
    throws URISyntaxException {
    return new URI(this.userEndpointUri.toString() + "/" + id);
  }

  /**
   * Creates a JAXB Element representation of the SCIM User.
   *
   * @param user The User to convert.
   * @return JAXB wrapped SCIM User.
   */
  private JAXBElement<User> getJaxbUser(final User user) {
    ObjectFactory o = new ObjectFactory();
    return o.createUser(user);
  }
}
