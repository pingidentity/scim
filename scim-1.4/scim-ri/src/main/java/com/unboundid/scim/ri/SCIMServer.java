/*
 * Copyright 2011-2013 UnboundID Corp.
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

package com.unboundid.scim.ri;

import com.unboundid.scim.wink.SCIMApplication;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.ldap.ResourceMapper;
import com.unboundid.util.StaticUtils;
import org.apache.wink.server.internal.DeploymentConfiguration;
import org.apache.wink.server.internal.servlet.RestServlet;
import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.ws.rs.core.Application;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.unboundid.scim.sdk.Debug.debugException;



/**
 * This class implements a stand-alone System for Cross-Domain Identity
 * Management (SCIM) server that uses an LDAP server as its resource storage
 * repository.
 */
public class SCIMServer
{
  /**
   * The singleton instance of the SCIM server.
   */
  private static SCIMServer scimServer;



  static
  {
    scimServer = new SCIMServer();
  }



  /**
   * The configuration of this SCIM server.
   */
  private SCIMServerConfig config;

  /**
   * A Jetty server instance to serve HTTP requests.
   */
  private Server server;

  /**
   * The Jetty ContextHandlerCollection used to handle requests.
   */
  private ContextHandlerCollection contextHandlerCollection;

  /**
   * The set of SCIMApplications registered with the server,
   * keyed by the ServletContextHandler.
   */
  private volatile Map<ServletContextHandler, SCIMApplication> backends;


  /**
   * The set of resource mappers registered for SCIM resource end-points.
   */
  private volatile Map<ResourceDescriptor, ResourceMapper> resourceMappers;




  /**
   * Initialize the SCIM server with the provided configuration.
   * All backend must be registered with the server using
   * {@link #registerBackend(String, SCIMBackend)} before calling
   * {@link #startListening()}.
   *
   * @param serverConfig The desired server configuration.
   *
   * @throws Exception If an error occurred while initializing the server.
   */
  public void initializeServer(final SCIMServerConfig serverConfig)
      throws Exception
  {
    final ArrayList<Connector> connectors = new ArrayList<Connector>();
    if (serverConfig.getSslContext() != null)
    {
      final SslContextFactory f;

      // NOTE:  Even though we're providing a pre-initialized SSL context, the
      // SslContextFactory expects to be configured with a keystore path.  In
      // that case, we'll just create an empty temp file to use for that
      // purpose, since it won't actually be used.
      final File tempFile = File.createTempFile(
          "dummy-scim-server-", ".keystore");
      tempFile.deleteOnExit();
      f = new SslContextFactory(tempFile.getAbsolutePath());
      f.setSslContext(serverConfig.getSslContext());

      final SslSocketConnector c = new SslSocketConnector(f);
      c.setPort(serverConfig.getListenPort());
      connectors.add(c);
    }
    else
    {
      final Connector c = new SelectChannelConnector();
      c.setPort(serverConfig.getListenPort());
      connectors.add(c);
    }

    final Server s = new Server(serverConfig.getListenPort());
    s.setConnectors(connectors.toArray(new Connector[connectors.size()]));
    s.setThreadPool(new QueuedThreadPool(serverConfig.getMaxThreads()));

    final HandlerCollection handlers = new HandlerCollection();
    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    final RequestLogHandler requestLogHandler = new RequestLogHandler();
    handlers.setHandlers(
        new Handler[]{contexts, new DefaultHandler(), requestLogHandler});
    s.setHandler(handlers);

    if(serverConfig.getAccessLogFile() != null)
    {
      NCSARequestLog requestLog =
          new NCSARequestLog(serverConfig.getAccessLogFile());
      requestLog.setRetainDays(90);
      requestLog.setAppend(true);
      requestLog.setExtended(false);
      requestLog.setLogTimeZone("GMT");
      requestLogHandler.setRequestLog(requestLog);
    }

    this.config = serverConfig;
    this.server = s;
    this.contextHandlerCollection = contexts;
    this.backends = new HashMap<ServletContextHandler, SCIMApplication>();
    this.resourceMappers = new HashMap<ResourceDescriptor, ResourceMapper>();

    if (serverConfig.getResourcesFile() != null)
    {
      final List<ResourceMapper> mappers =
          ResourceMapper.parse(serverConfig.getResourcesFile());
      for (final ResourceMapper resourceMapper : mappers)
      {
        resourceMappers.put(resourceMapper.getResourceDescriptor(),
                            resourceMapper);
      }
    }
  }



  /**
   * Indicates whether the provided configuration is valid for the SCIM server.
   *
   * @param  serverConfig         The configuration for the SCIM server.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  public boolean isConfigAcceptable(final SCIMServerConfig serverConfig,
                                    final List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    try
    {
      ResourceMapper.parse(serverConfig.getResourcesFile());
    }
    catch (Exception e)
    {
      debugException(e);
      unacceptableReasons.add(
          "The resources file '" + serverConfig.getResourcesFile() +
          "' cannot be parsed: " + StaticUtils.getExceptionMessage(e));
      acceptable = false;
    }

    return acceptable;
  }



  /**
   * Retrieves the singleton instance of the SCIM server.
   *
   * @return The singleton instance of the SCIM server.
   */
  public static SCIMServer getInstance()
  {
    return scimServer;
  }



  /**
   * Register a backend with this server under the specified base URI. Each
   * supported version of the REST API will be accessible by appending the
   * version to the base URI (e.g. baseURI/v1/). The most recent version of
   * the API will be accessible at the base URI itself.
   *
   * @param baseURI The base URI with which the backend is associated.
   * @param backend The backend to be registered. It must not be {@code null}.
   *
   * @return The SCIMApplication associated with this backend.
   */
  public SCIMApplication registerBackend(final String baseURI,
                                         final SCIMBackend backend)
  {
    final SCIMApplication application;
    synchronized (this)
    {
      final String normalizedBaseURI = normalizeURI(baseURI);
      for(ServletContextHandler contextHandler : backends.keySet())
      {
        if(contextHandler.getContextPath().equals(normalizedBaseURI))
        {
          throw new RuntimeException("There is already a backend registered " +
              "for base URI " + normalizedBaseURI);
        }
      }
      final Map<ServletContextHandler, SCIMApplication> newBackends =
          new HashMap<ServletContextHandler, SCIMApplication>(backends);

      final ServletContextHandler contextHandler =
          new ServletContextHandler(contextHandlerCollection,
              normalizedBaseURI);
      application = new SCIMApplication(
              resourceMappers.keySet(), backend, null);
      application.setBulkMaxConcurrentRequests(
          config.getBulkMaxConcurrentRequests());
      application.setBulkMaxOperations(config.getBulkMaxOperations());
      application.setBulkMaxPayloadSize(config.getBulkMaxPayloadSize());
      newBackends.put(contextHandler, application);

      // Configure authentication.
      final FilterHolder filterHolder =
          new FilterHolder(new BasicAuthenticationFilter(backend));
      contextHandler.addFilter(filterHolder, "/*", null);

      // JAX-RS implementation using Apache Wink.
      final ServletHolder winkServletHolder =
          new ServletHolder(new RestServlet()
          {
            @Override
            protected Application getApplication(
                final DeploymentConfiguration configuration)
                throws ClassNotFoundException, InstantiationException,
                IllegalAccessException
            {
              return application;
            }
          });
      winkServletHolder.setInitOrder(1);
      winkServletHolder.setInitParameter("propertiesLocation",
          "com/unboundid/scim/wink/wink-scim.properties");
      contextHandler.addServlet(winkServletHolder, "/*");
      // For now, v1 is the only supported API version.
      contextHandler.addServlet(winkServletHolder, "/v1/*");

      backends = newBackends;
    }
    return application;
  }



  /**
   * Retrieve the configured resource mappers.
   *
   * @return  The configured resource mappers.
   */
  public Map<ResourceDescriptor, ResourceMapper> getResourceMappers()
  {
    return resourceMappers;
  }



  /**
   * Attempts to start listening for client connections.
   *
   * @throws Exception If an error occurs during startup.
   */
  public void startListening()
      throws Exception
  {
    if (resourceMappers.isEmpty())
    {
      throw new RuntimeException("No resource mappers have been registered " +
          "with the SCIM server");
    }
    server.start();
  }



  /**
   * Retrieves the configured listen port for the server.
   *
   * @return The configured listen port for the server, or -1 if
   *         the server is not started.
   */
  public int getListenPort()
  {
    if (server.isStarted())
    {
      for (Connector con : server.getConnectors())
      {
        int port = con.getLocalPort();
        if (port > 0)
        {
          return port;
        }
      }

      return -1;
    }
    else
    {
      return -1;
    }
  }



  /**
   * Shuts down this SCIM server.
   *
   * @throws Exception If an error occurs during shutdown.
   */
  public void shutdown()
      throws Exception
  {
    if (server != null)
    {
      server.stop();

      try
      {
        server.join();
      }
      catch (InterruptedException e)
      {
        Debug.debugException(e);
        // No action required.
      }

      server.destroy();
      server = null;
    }

    if (resourceMappers != null)
    {
      // Make sure that each resource mapper is finalized exactly once.
      for (final ResourceMapper b : resourceMappers.values())
      {
        b.finalizeMapper();
      }

      resourceMappers = null;
    }
  }



  /**
   * Normalize a URI that has been provided to us. This just ensures that the
   * string starts with a '/'.
   *
   * @param uri The URI to be normalized.
   *
   * @return The normalized URI, always starting with a '/'.
   */
  private String normalizeURI(final String uri)
  {
    if (!uri.startsWith("/"))
    {
      return "/" + uri;
    }
    else
    {
      return uri;
    }
  }
}
