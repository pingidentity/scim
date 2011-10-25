/*
 * Copyright 2011 UnboundID Corp.
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

import com.unboundid.scim.data.AuthenticationScheme;
import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.data.ChangePasswordConfig;
import com.unboundid.scim.data.ETagConfig;
import com.unboundid.scim.data.FilterConfig;
import com.unboundid.scim.data.PatchConfig;
import com.unboundid.scim.data.ServiceProviderConfig;
import com.unboundid.scim.data.SortConfig;
import com.unboundid.scim.ldap.ConfigurableResourceMapper;
import com.unboundid.scim.ri.wink.SCIMApplication;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.ldap.ResourceMapper;
import com.unboundid.scim.sdk.SCIMObject;
import org.apache.wink.server.internal.servlet.RestServlet;
import org.apache.wink.server.utils.RegistrationUtils;
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

import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.unboundid.scim.sdk.SCIMConstants.*;



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
  private volatile Map<ServletContextHandler, SCIMBackend> backends;


  /**
   * The set of resource mappers registered for SCIM resource end-points.
   */
  private volatile Map<ResourceDescriptor, ResourceMapper> resourceMappers;

  /**
   * Monitor data.
   */
  private SCIMMonitorData monitorData;



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
    final Server s = new Server(serverConfig.getListenPort());
    s.setThreadPool(new QueuedThreadPool(serverConfig.getMaxThreads()));

    final ContextHandlerCollection contexts = new ContextHandlerCollection();
    s.setHandler(contexts);

    this.config = serverConfig;
    this.server = s;
    this.backends = new HashMap<ServletContextHandler, SCIMBackend>();
    this.resourceMappers = new HashMap<ResourceDescriptor, ResourceMapper>();
    this.monitorData = new SCIMMonitorData();

    if (serverConfig.getResourcesFile() != null)
    {
      final List<ResourceMapper> mappers =
          ConfigurableResourceMapper.parse(serverConfig.getResourcesFile());
      for (final ResourceMapper resourceMapper : mappers)
      {
        this.registerResourceMapper(resourceMapper);
      }
    }
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
   */
  public void registerBackend(final String baseURI,
                              final SCIMBackend backend)
  {
    // For now, v1 is the only supported API version.
    final String v1BaseURI =
        UriBuilder.fromPath(baseURI).path("v1").build().getPath();

    registerBackendPrivate(baseURI, backend);
    registerBackendPrivate(v1BaseURI, backend);
  }



  /**
   * Register a backend with this server under the specified base URI.
   *
   * @param baseURI The base URI with which the backend is associated.
   * @param backend The backend to be registered. It must not be {@code null}.
   */
  private void registerBackendPrivate(final String baseURI,
                                      final SCIMBackend backend)
  {
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
      final Map<ServletContextHandler, SCIMBackend> newBackends =
          new HashMap<ServletContextHandler, SCIMBackend>(backends);

      final ServletContextHandler contextHandler =
          new ServletContextHandler(
              (ContextHandlerCollection) server.getHandler(),
              normalizedBaseURI);
      newBackends.put(contextHandler, backend);

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

      // JAX-RS implementation using Apache Wink.
      System.setProperty("wink.httpMethodOverrideHeaders",
                         "X-HTTP-Method-Override");
      System.setProperty("wink.response.defaultCharset",
                         "true");
      final ServletHolder winkServletHolder =
          new ServletHolder(RestServlet.class);
      winkServletHolder.setInitOrder(1);
      contextHandler.addServlet(winkServletHolder, "/*");

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
   */
  public void registerResourceMapper(final ResourceMapper resourceMapper)
  {
    synchronized (this)
    {
      if (resourceMappers.get(
          resourceMapper.getResourceDescriptor()) == resourceMapper)
      {
        throw new RuntimeException("The resource mapper was already " +
            "registered for resource " +
            resourceMapper.getResourceDescriptor().getName());
      }

      final Map<ResourceDescriptor, ResourceMapper> newResourceMappers =
          new HashMap<ResourceDescriptor, ResourceMapper>(resourceMappers);

      newResourceMappers.put(resourceMapper.getResourceDescriptor(),
          resourceMapper);
      resourceMappers = newResourceMappers;
    }
  }



  /**
   * Retrieve the set of resource mappers registered for the provided resource
   * end-point.
   *
   * @param resourceDescriptor The ResourceDescriptor for which the registered
   *                           resource mappers are requested.
   *
   * @return The set of resource mappers registered for the provided resource
   *         end-point. This is never {@code null} but it may be empty.
   */
  public ResourceMapper getResourceMapper(
      final ResourceDescriptor resourceDescriptor)
  {
    return resourceMappers.get(resourceDescriptor);
  }



  /**
   * Retrieve the current monitoring data.
   * @return  The current monitoring data.
   */
  public SCIMMonitorData getMonitorData()
  {
    return monitorData;
  }



  /**
   * Retrieve the service provider configuration.
   * @return  The service provider configuration.
   */
  public ServiceProviderConfig getServiceProviderConfig()
  {
    final SCIMObject scimObject = new SCIMObject();
    final ServiceProviderConfig serviceProviderConfig =
        ServiceProviderConfig.SERVICE_PROVIDER_CONFIG_RESOURCE_FACTORY.
            createResource(CoreSchema.SERVICE_PROVIDER_CONFIG_SCHEMA_DESCRIPTOR,
                           scimObject);

    int maxResults = 0;
    for (final SCIMBackend b : backends.values())
    {
      maxResults = Math.max(maxResults, b.getConfig().getMaxResults());
    }

    serviceProviderConfig.setId(SCHEMA_URI_CORE);
    serviceProviderConfig.setPatchConfig(new PatchConfig(false));
    serviceProviderConfig.setBulkConfig(new BulkConfig(false, 0, 0));
    serviceProviderConfig.setFilterConfig(new FilterConfig(true, maxResults));
    serviceProviderConfig.setChangePasswordConfig(
        new ChangePasswordConfig(false));
    serviceProviderConfig.setSortConfig(new SortConfig(false));
    serviceProviderConfig.setETagConfig(new ETagConfig(false));

    final List<AuthenticationScheme> authenticationSchemes =
        new ArrayList<AuthenticationScheme>();
    authenticationSchemes.add(
        new AuthenticationScheme(
            "HttpBasic",
            "The HTTP Basic Access Authentication scheme. This scheme is not " +
            "considered to be a secure method of user authentication (unless " +
            "used in conjunction with some external secure system such as " +
            "SSL), as the user name and password are passed over the network " +
            "as cleartext.",
            "http://www.ietf.org/rfc/rfc2617.txt",
            "http://en.wikipedia.org/wiki/Basic_access_authentication",
            null, false));
    serviceProviderConfig.setAuthenticationSchemes(authenticationSchemes);

    return serviceProviderConfig;
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
    for(Map.Entry<ServletContextHandler, SCIMBackend> entry :
        backends.entrySet())
    {
      RegistrationUtils.registerApplication(
          new SCIMApplication(resourceMappers.keySet(), entry.getValue()),
          entry.getKey().getServletContext());
    }
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
