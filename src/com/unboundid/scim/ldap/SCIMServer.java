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
   * An LDAP external server to provide the resource storage repository.
   */
  private LDAPExternalServer ldapExternalServer;


  /**
   * Create a new SCIM server with the provided configuration.
   *
   * @param serverConfig  The desired server configuration.
   */
  public SCIMServer(final SCIMServerConfig serverConfig)
  {
    final Server s = new Server(serverConfig.getListenPort());
    s.setThreadPool(new QueuedThreadPool(serverConfig.getMaxThreads()));

    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    s.setHandler(contexts);

    final ServletContextHandler rootContext =
        new ServletContextHandler(contexts,
                                  normalizeURI(serverConfig.getBaseURI()),
                                  ServletContextHandler.NO_SESSIONS);

    final LDAPExternalServer les = new LDAPExternalServer(serverConfig);
    final HttpServlet servlet =
        new SCIMServlet(serverConfig, les);
    rootContext.addServlet(new ServletHolder(servlet), "/User/*");

    this.config = serverConfig;
    this.server = s;
    this.ldapExternalServer = les;
  }



  /**
   * Attempts to start listening for client connections.
   *
   * @throws Exception If an error occurs during startup.
   */
  public void startListening()
      throws Exception
  {
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

    if (ldapExternalServer != null)
    {
      ldapExternalServer.close();
      ldapExternalServer = null;
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
