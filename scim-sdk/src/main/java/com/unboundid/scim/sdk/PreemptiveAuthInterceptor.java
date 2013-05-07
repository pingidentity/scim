/*
 * Copyright 2012-2013 UnboundID Corp.
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
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthProtocolState;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.RouteInfo;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
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
  /**
   * {@inheritDoc}
   */
  @Override
  public void process(final HttpRequest request, final HttpContext context)
          throws HttpException, IOException
  {
    final HttpClientContext clientContext = HttpClientContext.adapt(context);
    final RouteInfo route = clientContext.getHttpRoute();
    HttpHost target = clientContext.getTargetHost();
    if(target.getPort() < 0)
    {
      target = new HttpHost(
              target.getHostName(),
              route.getTargetHost().getPort(),
              target.getSchemeName());
    }

    AuthCache authCache = clientContext.getAuthCache();
    if(authCache == null)
    {
      authCache = new BasicAuthCache();
      BasicScheme basicAuth = new BasicScheme();
      authCache.put(target, basicAuth);
      clientContext.setAuthCache(authCache);
      return;
    }

    final CredentialsProvider credsProvider =
            clientContext.getCredentialsProvider();
    if(credsProvider == null)
    {
      return;
    }

    final AuthState targetState = clientContext.getTargetAuthState();
    if(targetState != null &&
            targetState.getState() == AuthProtocolState.UNCHALLENGED)
    {
      final AuthScheme authScheme = authCache.get(target);
      if(authScheme != null)
      {
        doPreemptiveAuth(target, authScheme, targetState, credsProvider);
      }
    }

    final HttpHost proxy = route.getProxyHost();
    final AuthState proxyState = clientContext.getProxyAuthState();
    if(proxy != null && proxyState != null &&
            proxyState.getState() == AuthProtocolState.UNCHALLENGED)
    {
      final AuthScheme authScheme = authCache.get(proxy);
      if(authScheme != null)
      {
        doPreemptiveAuth(proxy, authScheme, proxyState, credsProvider);
      }
    }
  }

  /**
   * Method to update the AuthState in order to preemptively supply the
   * credentials to the server.
   *
   * @param host the HttpHost which we're authenticating to
   * @param authScheme the AuthScheme in use
   * @param authState the AuthState object from the HttpContext
   * @param credsProvider the CredentialsProvider which has the username and
   *                      password
   */
  private void doPreemptiveAuth(
          final HttpHost host,
          final AuthScheme authScheme,
          final AuthState authState,
          final CredentialsProvider credsProvider)
  {
    final String schemeName = authScheme.getSchemeName();

    final AuthScope authScope =
            new AuthScope(host, AuthScope.ANY_REALM, schemeName);
    final Credentials creds = credsProvider.getCredentials(authScope);

    if(creds != null)
    {
      if(AuthSchemes.BASIC.equalsIgnoreCase(schemeName))
      {
        authState.setState(AuthProtocolState.CHALLENGED);
      }
      else
      {
        authState.setState(AuthProtocolState.SUCCESS);
      }
      authState.update(authScheme, creds);
    }
  }
}
