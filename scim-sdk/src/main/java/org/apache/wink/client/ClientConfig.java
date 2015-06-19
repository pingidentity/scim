/*
 * Copyright 2011-2015 UnboundID Corp.
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

package org.apache.wink.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.wink.client.handlers.ClientHandler;
import org.apache.wink.client.handlers.HandlerContext;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Configuration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Wink compatibility layer class - see Wink docs.
 */
public class ClientConfig
{
  private static class WinkHandlerClientRequestFilter
      implements ClientRequestFilter
  {
    private ClientHandler handler;
    private ClientConfig clientConfig;

    /**
     *  Wink compatibility layer class - see Wink docs.
     * @param handler Wink compatibility layer class - see Wink docs.
     * @param clientConfig Wink compatibility layer class - see Wink docs.
     */
    WinkHandlerClientRequestFilter(final ClientHandler handler,
                                   final ClientConfig clientConfig)
    {
      this.handler = handler;
    }

    /**
     *  Wink compatibility layer class - see Wink docs.
     * @param clientRequestContext Wink compatibility layer class
     *                             - see Wink docs.
     * @throws IOException Wink compatibility layer class - see Wink docs.
     */
    public void filter(final ClientRequestContext clientRequestContext)
        throws IOException
    {
      try
      {
        ClientRequest request = new ClientRequest(clientRequestContext);
        HandlerContext context = new HandlerContext();
        handler.handle(request, context);
      }
      catch(Exception ex)
      {
        throw new IOException("Unable to invoke client handler.", ex);
      }
    }
  }

  private boolean bypassHostnameVerification;
  private int maxPooledConnections;
  private List<ClientHandler> handlers = new ArrayList<ClientHandler>();
  private Iterator<ClientHandler> clientHandlerIterator = null;
  private int connectTimeout = 30000;
  private int readTimeout = 60000;

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param bypassHostnameVerification Wink compatibility layer
   *                                   class - see Wink docs.
   */
  public void setBypassHostnameVerification(
      final boolean bypassHostnameVerification)
  {
    this.bypassHostnameVerification = bypassHostnameVerification;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public boolean isBypassHostnameVerification()
  {
    return bypassHostnameVerification;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param handlers Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public ClientConfig handlers(final ClientHandler ... handlers)
  {
    if(handlers != null)
    {
      for(ClientHandler handler : handlers)
      {
        this.handlers.add(handler);
      }
    }
    return this;
  }

  /**
   * Wink compatibility layer class - see Wink docs.
   * @param connectTimeout Wink compatibility layer class - see Wink docs.
   */
  public void connectTimeout(final int connectTimeout)
  {
    this.connectTimeout = connectTimeout;
  }

  /**
   * Wink compatibility layer class - see Wink docs.
   * @param readTimeout Wink compatibility layer class - see Wink docs.
   */
  public void readTimeout(final int readTimeout)
  {
    this.readTimeout = readTimeout;
  }

  /**
   * Wink compatibility layer class - see Wink docs.
   * @param maxPooledConnections Wink compatibility layer class - see Wink docs.
   */
  public void setMaxPooledConnections(final int maxPooledConnections)
  {
    this.maxPooledConnections = maxPooledConnections;
  }

  /**
   * Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public int getMaxPooledConnections()
  {
    return maxPooledConnections;
  }

  /**
   * Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  Configuration getConfiguration()
  {
    org.glassfish.jersey.client.ClientConfig jerseyConfig =
        new org.glassfish.jersey.client.ClientConfig();
    clientHandlerIterator = handlers.iterator();
    while(clientHandlerIterator.hasNext())
    {
      ClientHandler handler = clientHandlerIterator.next();
      jerseyConfig.register(new WinkHandlerClientRequestFilter(handler, this));
    }

    PoolingHttpClientConnectionManager mgr = new
        PoolingHttpClientConnectionManager();

    mgr.setMaxTotal(200);
    mgr.setDefaultMaxPerRoute(20);

    jerseyConfig.property(ApacheClientProperties.CONNECTION_MANAGER,
        mgr);
    RequestConfig reqConfig = RequestConfig.custom()
        .setConnectTimeout(connectTimeout)
        .setSocketTimeout(readTimeout)
        .setConnectionRequestTimeout(200)
        .build();
    jerseyConfig.property(ApacheClientProperties.REQUEST_CONFIG, reqConfig);
    jerseyConfig.connectorProvider(new ApacheConnectorProvider());

    return jerseyConfig;
  }
}
