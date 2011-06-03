/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.http.HttpServlet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



/**
 * This class implements a stand-alone Simple Cloud Identity Management (SCIM)
 * server that uses an LDAP server as its resource storage repository.
 */
public class SCIMServer
{
  /**
   * The configuration of this SCIM server.
   */
  private SCIMServerConfig config;

  /**
   * A Jetty server instance to serve HTTP requests.
   */
  private Server server;

  /**
   * The set of backends registered with the server, keyed by the base URI.
   */
  private Map<String,SCIMBackend> backends;



  /**
   * Create a new SCIM server with the provided configuration.
   * All backend must be registered with the server using
   * {@link #registerBackend(String, SCIMBackend)} before calling
   * {@link #startListening()}.
   *
   * @param serverConfig  The desired server configuration.
   */
  public SCIMServer(final SCIMServerConfig serverConfig)
  {
    final Server s = new Server(serverConfig.getListenPort());
    s.setThreadPool(new QueuedThreadPool(serverConfig.getMaxThreads()));

    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    s.setHandler(contexts);

    this.config = serverConfig;
    this.server = s;
    this.backends = new HashMap<String, SCIMBackend>();
  }



  /**
   * Register a backend with this server under the specified base URI.
   *
   * @param baseURI  The base URI with which the backend is associated.
   * @param backend  The backend to be registered. It must not be {@code null}.
   */
  public void registerBackend(final String baseURI,
                              final SCIMBackend backend)
  {
    synchronized (this)
    {
      final String normalizedBaseURI = normalizeURI(baseURI);
      if (backends.containsKey(baseURI))
      {
        throw new RuntimeException("There is already a backend registered " +
                                   "for base URI " + normalizedBaseURI);
      }
      final Map<String,SCIMBackend> newBackends =
          new HashMap<String, SCIMBackend>(backends);
      newBackends.put(normalizedBaseURI, backend);

      final ServletContextHandler contextHandler =
          new ServletContextHandler(
              (ContextHandlerCollection)server.getHandler(),
              normalizedBaseURI,
              ServletContextHandler.NO_SESSIONS);

      final HttpServlet servlet = new SCIMServlet(backend);
      contextHandler.addServlet(new ServletHolder(servlet), "/User/*");

      backends = newBackends;
    }
  }



  /**
   * Attempts to start listening for client connections.
   *
   * @throws Exception If an error occurs during startup.
   */
  public void startListening()
      throws Exception
  {
    if (backends.isEmpty())
    {
      throw new RuntimeException("No backends have been registered with the " +
                                 "SCIM server");
    }
    server.start();
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
        // No action required.
      }

      server.destroy();
      server = null;
    }

    if (backends != null)
    {
      // Make sure that each backend is finalized just once.
      // The same backend may be referenced more than once.
      Set<SCIMBackend> allBackends = new HashSet<SCIMBackend>();
      for (final SCIMBackend b : backends.values())
      {
        if (b != null)
        {
          allBackends.add(b);
        }
      }

      for (final SCIMBackend b : allBackends)
      {
        b.finalizeBackend();
      }

      backends = null;
    }
  }



  /**
   * Normalize a URI that has been provided to us. This just ensures that the
   * string starts with a '/'.
   *
   * @param uri  The URI to be normalized.
   *
   * @return  The normalized URI, always starting with a '/'.
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
