/*
 * Copyright 2012 UnboundID Corp.
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

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.protocol.HttpContext;


/**
 * This class can be used to configure the Apache Http Client for preemptive
 * authentication. In this mode, the client will send the basic authentication
 * response even before the server gives an unauthorized response in certain
 * situations. This reduces the overhead of making requests over authenticated
 * connections.
 *
 * This behavior conforms to RFC2617: A client MAY preemptively send the
 * corresponding Authorization header with requests for resources in that space
 * without receipt of another challenge from the server. Similarly, when a
 * client sends a request to a proxy, it may reuse a userid and password in the
 * Proxy-Authorization header field without receiving another challenge from the
 * proxy server.
 *
 * The Apache Http Client does not support preemptive authentication out of the
 * box, because if misused or used incorrectly the preemptive authentication can
 * lead to significant security issues, such as sending user credentials in
 * clear text to an unauthorized third party.
 */
public class PreemptiveAuthInterceptor implements HttpRequestInterceptor
{
  private final AuthScheme authScheme;
  private final Credentials credentials;

  /**
   * Constructs a new PreemptiveAuthInterceptor. It is important that this is
   * added as the <b>first</b> request interceptor in the chain. You can do this
   * by making sure the second parameter is zero when adding the interceptor:
   * <p>
   * <code>
   * httpClient.addRequestInterceptor(
   *   new PreemptiveAuthInterceptor(new BasicScheme(), credentials), 0);
   * </code>
   *
   * @param authScheme The AuthScheme to use. This may not be null.
   * @param credentials The Credentials to use. This may not be null.
   */
  public PreemptiveAuthInterceptor(final AuthScheme authScheme,
                                   final Credentials credentials)
  {
    if(authScheme == null)
    {
      throw new NullPointerException(
              "The 'authScheme' parameter cannot be null");
    }

    if(credentials == null)
    {
      throw new NullPointerException(
              "The 'credentials' parameter cannot be null");
    }

    this.authScheme = authScheme;
    this.credentials = credentials;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(final HttpRequest request, final HttpContext context)
      throws HttpException, IOException
  {
    AuthState authState = (AuthState) context.getAttribute(
                                        ClientContext.TARGET_AUTH_STATE);
    if(authState != null)
    {
      authState.setAuthScheme(authScheme);
      authState.setCredentials(credentials);
    }
  }
}
