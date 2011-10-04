/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ri.client;

import org.apache.wink.client.ClientAuthenticationException;
import org.apache.wink.client.ClientRequest;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.handlers.ClientHandler;
import org.apache.wink.client.handlers.HandlerContext;
import org.apache.wink.common.http.HttpStatus;

import javax.xml.bind.DatatypeConverter;

/**
 * This class provides HTTP Basic Authentication handling.
 */
public class HttpBasicAuthSecurityHandler implements ClientHandler {
  private volatile String username;
  private volatile String encodedCredentials;

  /**
   * Constructs a fully initialized Security handler.
   * @param username The Consumer username.
   * @param password The Consumer password.
   */
  public HttpBasicAuthSecurityHandler(final String username,
    final String password) {
    this.username = username;
    String encoded = DatatypeConverter.printBase64Binary(
        (username + ":" + password).getBytes());
    this.encodedCredentials = "Basic " + encoded;
  }

  /**
   * Attempts to authenticate a Consumer via Http Basic.
   *
   * @param request  The Client Resource request.
   * @param context The provided handler chain.
   * @return Client Response that may indicate success or failure.
   * @throws Exception Thrown if error handling authentication.
   */
  public ClientResponse handle(final ClientRequest request,
    final HandlerContext context) throws Exception {
    ClientResponse response = context.doChain(request);
    if (response.getStatusCode() == HttpStatus.UNAUTHORIZED.getCode()) {
      request.getHeaders().putSingle("Authorization", this.encodedCredentials);
      response = context.doChain(request);
      if (response.getStatusCode() == HttpStatus.UNAUTHORIZED.getCode()) {
        throw new ClientAuthenticationException(username);
      } else {
        // error presumably unrelated to authentication
        return response;
      }
    } else {
      return response;
    }
  }
}
