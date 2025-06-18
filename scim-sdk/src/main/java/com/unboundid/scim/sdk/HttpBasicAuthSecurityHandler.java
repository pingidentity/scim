/*
 * Copyright 2011-2025 Ping Identity Corporation
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

package com.unboundid.scim.sdk;

import com.unboundid.scim.facade.org.apache.wink.client.ClientRequest;
import com.unboundid.scim.facade.org.apache.wink.client.ClientResponse;
import com.unboundid.scim.facade.org.apache.wink.client.handlers.ClientHandler;
import com.unboundid.scim.facade.org.apache.wink.client.handlers.HandlerContext;

import jakarta.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;


/**
 * This class provides HTTP Basic Authentication handling.
 */
public class HttpBasicAuthSecurityHandler implements ClientHandler
{

  private volatile String encodedCredentials;

  /**
   * Constructs a fully initialized Security handler.
   * @param username The Consumer username.
   * @param password The Consumer password.
   */
  public HttpBasicAuthSecurityHandler(final String username,
                                      final String password)
  {
    String encoded = null;
    try
    {
      encoded = DatatypeConverter.printBase64Binary(
              (username + ":" + password).getBytes("UTF-8"));
    }
    catch(UnsupportedEncodingException e)
    {
      //UTF-8 is pretty standard, so this should not happen.
      throw new IllegalArgumentException(e.getMessage());
    }
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
                               final HandlerContext context) throws Exception
  {
    request.getHeaders().putSingle("Authorization", this.encodedCredentials);
    return null;
  }
}
