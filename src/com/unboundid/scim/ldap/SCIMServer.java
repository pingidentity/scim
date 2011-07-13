/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import com.unboundid.scim.config.SchemaManager;
import org.eclipse.jetty.http.security.Constraint;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.http.HttpServlet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_ENDPOINT_USERS;
import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_NAME_USER;



/**
 * This class implements a stand-alone Simple Cloud Identity Management (SCIM)
 * server that uses an LDAP server as its resource storage repository.
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
   * The set of backends registered with the server, keyed by the base URI.
   */
  private volatile Map<String, SCIMBackend> backends;


  /**
   * The set of resource mappers registered for SCIM resource end-points.
   */
  private volatile Map<String, Set<ResourceMapper>> resourceMappers;

  /**
   * The set of resource mappers registered for SCIM resource query end-points.
   */
  private volatile Map<String, ResourceMapper> queryResourceMappers;



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
    SchemaManager.init(serverConfig.getSchemaFiles());

    final Server s = new Server(serverConfig.getListenPort());
    s.setThreadPool(new QueuedThreadPool(serverConfig.getMaxThreads()));

    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    s.setHandler(contexts);

    this.config = serverConfig;
    this.server = s;
    this.backends = new HashMap<String, SCIMBackend>();
    this.resourceMappers = new HashMap<String, Set<ResourceMapper>>();
    this.queryResourceMappers = new HashMap<String, ResourceMapper>();

    this.registerResourceMapper(new UserResourceMapper(),
                                RESOURCE_NAME_USER, RESOURCE_ENDPOINT_USERS);
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
   * Register a backend with this server under the specified base URI.
   *
   * @param baseURI The base URI with which the backend is associated.
   * @param backend The backend to be registered. It must not be {@code null}.
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
      final Map<String, SCIMBackend> newBackends =
          new HashMap<String, SCIMBackend>(backends);
      newBackends.put(normalizedBaseURI, backend);

      final ServletContextHandler contextHandler =
          new ServletContextHandler(
              (ContextHandlerCollection) server.getHandler(),
              normalizedBaseURI);

      // Configure authentication.

      final LoginService loginService = new LDAPLoginService(backend);
      server.addBean(loginService);

      final ConstraintSecurityHandler security =
          new ConstraintSecurityHandler();
      contextHandler.setSecurityHandler(security);
      Constraint constraint = new Constraint();
      constraint.setAuthenticate(true);

      // A user possessing (literally) any role will do
      constraint.setRoles(new String[]{Constraint.ANY_ROLE});

      // * maps to all external endpoints
      final ConstraintMapping mapping = new ConstraintMapping();
      mapping.setPathSpec("/*");
      mapping.setConstraint(constraint);

      // for now force map all roles - that is the assertions is only "is the
      // user authenticated" - not are they authenticated && possess a
      // roles(s)
      final Set<String> knownRoles = new HashSet<String>();
      knownRoles.add(Constraint.ANY_ROLE);
      security.setConstraintMappings(Collections.singletonList(mapping),
                                     knownRoles);

      // use the HTTP Basic authentication mechanism
      security.setAuthenticator(new BasicAuthenticator());
      security.setLoginService(loginService);

      // strictness refers to Jetty's role handling
      security.setStrict(false);
      security.setHandler(contextHandler);
      security.setServer(server);

      final HttpServlet servlet = new SCIMServlet(backend);
      contextHandler.addServlet(new ServletHolder(servlet), "/*");

      backends = newBackends;
    }
  }



  /**
   * Register a resource mapper with this server for the specified SCIM
   * resource end-points. e.g. User and Users. Multiple resource mappers may be
   * registered for a single resource end-point.
   *
   * @param resourceMapper    The resource mapper to be registered. It must not
   *                          be {@code null}.
   * @param resourceEndPoint  The SCIM resource end-point. e.g. User
   * @param queryEndPoint     The SCIM resource query end-point with which the
   *                          mapper is associated. e.g. Users
   */
  public void registerResourceMapper(final ResourceMapper resourceMapper,
                                     final String resourceEndPoint,
                                     final String queryEndPoint)
  {
    synchronized (this)
    {
      if (resourceEndPoint != null)
      {
        final Map<String, Set<ResourceMapper>> newResourceMappers =
            new HashMap<String, Set<ResourceMapper>>();

        Set<ResourceMapper> mappers = resourceMappers.get(resourceEndPoint);
        if (mappers != null && mappers.contains(resourceMapper))
        {
          throw new RuntimeException("The resource mapper was already " +
                                     "registered for resource end-point " +
                                     resourceEndPoint);
        }

        for (Map.Entry<String, Set<ResourceMapper>> e :
            resourceMappers.entrySet())
        {
          newResourceMappers.put(e.getKey(),
                                 new HashSet<ResourceMapper>(e.getValue()));
        }

        mappers = newResourceMappers.get(resourceEndPoint);
        if (mappers == null)
        {
          mappers = new HashSet<ResourceMapper>();
          newResourceMappers.put(resourceEndPoint, mappers);
        }

        mappers.add(resourceMapper);

        resourceMappers = newResourceMappers;
      }
    }

    if (queryEndPoint != null)
    {
      if (queryResourceMappers.get(queryEndPoint) == resourceMapper)
      {
        throw new RuntimeException("The resource mapper was already " +
                                   "registered for resource query end-point " +
                                   resourceEndPoint);
      }

      final Map<String, ResourceMapper> newQueryResourceMappers =
          new HashMap<String, ResourceMapper>(queryResourceMappers);

      newQueryResourceMappers.put(queryEndPoint, resourceMapper);
      queryResourceMappers = newQueryResourceMappers;
    }
  }



  /**
   * Retrieve the set of resource mappers registered for the provided resource
   * end-point.
   *
   * @param resourceEndPoint The resource end-point for which the registered
   *                         resource mappers are requested.
   *
   * @return The set of resource mappers registered for the provided resource
   *         end-point. This is never {@code null} but it may be empty.
   */
  public Set<ResourceMapper> getResourceMappers(final String resourceEndPoint)
  {
    final Set<ResourceMapper> mappers = resourceMappers.get(resourceEndPoint);
    if (mappers == null)
    {
      return Collections.emptySet();
    }
    else
    {
      return Collections.unmodifiableSet(mappers);
    }
  }



  /**
   * Retrieve the resource mapper registered for the provided resource query
   * end-point.
   *
   * @param resourceEndPoint The query resource end-point for which the
   *                         registered resource mapper is requested.
   *
   * @return The resource mapper registered for the provided resource end
   *         point, or {@code null} if there is no such resource mapper.
   */
  public ResourceMapper getQueryResourceMapper(final String resourceEndPoint)
  {
    return queryResourceMappers.get(resourceEndPoint);
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

    if (resourceMappers != null)
    {
      // Make sure that each resource mapper is finalized exactly once.
      Set<ResourceMapper> allMappers = new HashSet<ResourceMapper>();
      for (final Set<ResourceMapper> mappers : resourceMappers.values())
      {
        if (mappers != null)
        {
          allMappers.addAll(mappers);
        }
      }

      for (final ResourceMapper b : allMappers)
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
