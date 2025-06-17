/*
 * Copyright 2011-2024 Ping Identity Corporation
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

import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.data.ServiceProviderConfig;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import com.unboundid.scim.facade.org.apache.wink.client.RestClient;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static com.unboundid.scim.schema.CoreSchema
                 .createCustomGroupResourceDescriptor;
import static com.unboundid.scim.schema.CoreSchema
                 .createCustomUserResourceDescriptor;


/**
 * The SCIMService class represents a client connection to a SCIM service
 * provider. It handles setting up and configuring the connection which will
 * be used by the SCIMEndpoints that are obtained form this SCIMService.
 */
public class SCIMService
{
  private final RestClient client;
  private final URI baseURL;

  private MediaType acceptType = MediaType.APPLICATION_JSON_TYPE;
  private MediaType contentType = MediaType.APPLICATION_JSON_TYPE;
  private final boolean[] overrides = new boolean[3];
  private String userAgent;
  private boolean useUrlSuffix;

  /**
   * Constructs a new SCIMService from a url and a jersey client config.
   * @param baseUrl The SCIM Service Provider URL.
  * @param clientConfig The client config object.
   */
  public SCIMService(final URI baseUrl,
    final org.glassfish.jersey.client.ClientConfig clientConfig)
  {
    this.baseURL = baseUrl;
    this.client = new RestClient(clientConfig);
  }

  /**
   * Constructs a new SCIMService.
   *
   * @param baseUrl The SCIM Service Provider URL.
   */
  public SCIMService(final URI baseUrl)
  {
    this(baseUrl, createDefaultClientConfig());
  }

  /**
   * Constructs a new SCIMService with OAuth authentication support
   * using the provided credentials.

   * @param baseUrl The SCIM Service Provider URL.
   * @param oAuthToken The OAuth token.
   */
  public SCIMService(final URI baseUrl, final OAuthToken oAuthToken) {
    this(baseUrl, createDefaultClientConfig().register(
        new ClientRequestFilter()
        {
          public void filter(final ClientRequestContext clientRequestContext)
              throws IOException
          {
            try
            {
              clientRequestContext.getHeaders().add(
                  "Authorization", oAuthToken.getFormattedValue());
            }
            catch (Exception ex)
            {
              throw new RuntimeException(
                  "Unable to add authorization handler", ex);
            }
          }
        }
    ));
  }

  /**
   * Constructs a new SCIMService with basic authentication support
   * using the provided credentials.
   *
   * @param baseUrl The SCIM Service Provider URL.
   * @param username The username.
   * @param password The password.
   */
  public SCIMService(final URI baseUrl, final String username,
                     final String password)
  {
    this(baseUrl, createDefaultClientConfig().
        property(ApacheClientProperties.CREDENTIALS_PROVIDER,
            createBasicCredentialsProvider(username, password)).
        property(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION, true));
  }

  /**
   * Returns a SCIMEndpoint with the current settings that can be used to
   * invoke CRUD operations. Any changes to the SCIMService configuration will
   * not be reflected in the returned SCIMEndpoint.
   *
   * @param resourceDescriptor The ResourceDescriptor of the endpoint.
   * @param resourceFactory The ResourceFactory that should be used to
   *                        create SCIM resource instances.
   * @param <R> The type of SCIM resource instances.
   * @return The SCIMEndpoint that can be used to invoke CRUD operations.
   */
  public <R extends BaseResource> SCIMEndpoint<R> getEndpoint(
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory)
  {
    return new SCIMEndpoint<R>(this, client, resourceDescriptor,
        resourceFactory);
  }


  /**
   * Returns a SCIMEndpoint for the specified endpoint.
   *
   * @param endpointPath SCIM endpoint relative path, e.g. "Users".
   * @return SCIMEndpoint that can be used to invoke CRUD operations.
   * @throws SCIMException for an invalid endpoint path
   */
  public SCIMEndpoint<BaseResource> getEndpoint(
      final String endpointPath) throws SCIMException
  {
    return getEndpoint(endpointPath, BaseResource.BASE_RESOURCE_FACTORY);
  }


  /**
   * Returns a SCIMEndpoint for the specified endpoint.
   *
   * @param endpointPath SCIM endpoint relative path, e.g. "Users".
   * @param resourceFactory The ResourceFactory that should be used to
   *                        create SCIM resource instances.
   * @param <R> the type of SCIM resource instances.
   * @return SCIMEndpoint that can be used to invoke CRUD operations.
   * @throws SCIMException for an invalid endpoint path.
   */
  public <R extends BaseResource> SCIMEndpoint<R> getEndpoint(
      final String endpointPath,
      final ResourceFactory<R> resourceFactory)
      throws SCIMException
  {
    ResourceDescriptor descriptor =
        getResourceDescriptorForEndpoint(endpointPath);
    if (descriptor == null) {
      throw new ResourceNotFoundException(
          "No schema found for endpoint " + endpointPath);
    }
    return getEndpoint(descriptor, resourceFactory);
  }



  /**
   * Returns a SCIMEndpoint for the Users endpoint defined in the core schema.
   *
   * @return The SCIMEndpoint for the Users endpoint defined in the core schema.
   */
  public SCIMEndpoint<UserResource> getUserEndpoint()
  {
    return new SCIMEndpoint<UserResource>(this, client,
        CoreSchema.USER_DESCRIPTOR, UserResource.USER_RESOURCE_FACTORY);
  }



  /**
   * Returns a SCIMEndpoint for the Users endpoint defined in the core schema
   * with a custom user resource name and users endpoint name.
   *
   * @param userResourceName   Provide a custom user resource name.
   * @param usersEndpointName  Provide a custom users endpoint name.
   * @return The SCIMEndpoint for the Users endpoint defined in the core schema.
   */
  public SCIMEndpoint<UserResource> getUserEndpoint(
    final String userResourceName, final String usersEndpointName)
  {
    return new SCIMEndpoint<UserResource>(this, client,
      createCustomUserResourceDescriptor(userResourceName, usersEndpointName),
      UserResource.USER_RESOURCE_FACTORY);
  }



  /**
   * Returns a SCIMEndpoint for the Groups endpoint defined in the core schema.
   *
   * @return The SCIMEndpoint for the Groups endpoint defined in the
   *         core schema.
   */
  public SCIMEndpoint<GroupResource> getGroupEndpoint()
  {
    return new SCIMEndpoint<GroupResource>(this, client,
        CoreSchema.GROUP_DESCRIPTOR, GroupResource.GROUP_RESOURCE_FACTORY);
  }



  /**
   * Returns a SCIMEndpoint for the Groups endpoint defined in the core schema
   * with a custom group resource name and groups endpoint name.
   *
   * @param groupResourceName   Provide a custom group resource name.
   * @param groupsEndpointName  Provide a custom groups endpoint name.
   * @return The SCIMEndpoint for the Groups endpoint defined in the
   *         core schema.
   */
  public SCIMEndpoint<GroupResource> getGroupEndpoint(
    final String groupResourceName, final String groupsEndpointName)
  {
    return new SCIMEndpoint<GroupResource>(this, client,
      createCustomGroupResourceDescriptor(groupResourceName,
      groupsEndpointName), GroupResource.GROUP_RESOURCE_FACTORY);
  }



  /**
   * Returns a SCIMEndpoint for the Schemas endpoint. This endpoint allows for
   * the retrieval of schema for all service provider supported resources.
   *
   * @return The SCIMEndpoint for the Schemas endpoint.
   */
  public SCIMEndpoint<ResourceDescriptor> getResourceSchemaEndpoint()
  {
    return new SCIMEndpoint<ResourceDescriptor>(this, client,
        CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY);
  }

  /**
   * Retrieves the ResourceDescriptor for the specified resource from the
   * SCIM service provider.
   *
   * @param resourceName The name of the resource.
   * @param schema The schema URN of the resource or <code>null</code>
   *        to match only based on the name of the resource.
   * @return The ResourceDescriptor for the specified resource or
   *         <code>null</code> if none are found.
   * @throws SCIMException If the ResourceDescriptor could not be read.
   */
  public ResourceDescriptor getResourceDescriptor(final String resourceName,
                                                  final String schema)
      throws SCIMException
  {
    final SCIMEndpoint<ResourceDescriptor> endpoint =
        getResourceSchemaEndpoint();
    String filter = "name eq \"" + resourceName + "\"";
    if(schema != null)
    {
      filter += " and schema eq \"" + schema + "\"";
    }
    final Resources<ResourceDescriptor> resources = endpoint.query(filter);
    if(resources.getTotalResults() == 0)
    {
      return null;
    }
    if(resources.getTotalResults() > 1)
    {
      throw new InvalidResourceException(
          "The service provider returned multiple resource descriptors " +
              "with resource name '" + resourceName);
    }

    ResourceDescriptor descriptor = resources.iterator().next();
    descriptor.setStrictMode(false);

    return descriptor;
  }

  /**
   * Retrieves the ResourceDescriptor for the specified endpoint from the
   * SCIM service provider.
   *
   * @param endpoint The name of the SCIM endpoint, e.g. "Users".
   * @return The ResourceDescriptor for the specified endpoint or
   *         <code>null</code> if none are found.
   * @throws SCIMException If the ResourceDescriptor could not be read.
   */
  public ResourceDescriptor getResourceDescriptorForEndpoint(
      final String endpoint)
      throws SCIMException
  {
    final SCIMEndpoint<ResourceDescriptor> schemaEndpoint =
        getResourceSchemaEndpoint();
    String filter = "endpoint eq \"" + endpoint + "\"";

    final Resources<ResourceDescriptor> resources =
        schemaEndpoint.query(filter);
    if(resources.getTotalResults() == 0)
    {
      return null;
    }
    if(resources.getTotalResults() > 1)
    {
      throw new InvalidResourceException(
          "The service provider returned multiple resource descriptors " +
              "for endpoint '" + endpoint);
    }

    ResourceDescriptor descriptor = resources.iterator().next();
    descriptor.setStrictMode(false);

    return descriptor;
  }

  /**
   * Retrieves the Service Provider Config from the SCIM service provider.
   *
   * @return  The Service Provider Config.
   *
   * @throws SCIMException  If the Service Provider Config could not be read.
   */
  public ServiceProviderConfig getServiceProviderConfig()
      throws SCIMException
  {
    final SCIMEndpoint<ServiceProviderConfig> endpoint =
        getEndpoint(CoreSchema.SERVICE_PROVIDER_CONFIG_SCHEMA_DESCRIPTOR,
            ServiceProviderConfig.SERVICE_PROVIDER_CONFIG_RESOURCE_FACTORY);

    // The ServiceProviderConfig is a special case where there is only a
    // single resource at the endpoint, so the id is not specified.
    return endpoint.get(null);
  }



  /**
   * Invoke a bulk request. The service provider will perform as
   * many operations as possible without regard to the number of failures.
   *
   * @param operations  The operations to be performed.
   *
   * @return  The bulk response.
   *
   * @throws SCIMException  If the request fails.
   */
  public BulkResponse processBulkRequest(
      final List<BulkOperation> operations)
      throws SCIMException
  {
    return processBulkRequest(operations, -1);
  }



  /**
   * Invoke a bulk request.
   *
   * @param operations    The operations to be performed.
   * @param failOnErrors  The number of errors that the service provider will
   *                      accept before the operation is terminated and an
   *                      error response is returned. A value of -1 indicates
   *                      the the service provider will continue to perform
   *                      as many operations as possible without regard to
   *                      failures.
   *
   * @return  The bulk response.
   *
   * @throws SCIMException  If the request fails.
   */
  public BulkResponse processBulkRequest(
      final List<BulkOperation> operations,
      final int failOnErrors)
      throws SCIMException
  {
    final BulkEndpoint request = new BulkEndpoint(this, client);

    return request.processRequest(operations, failOnErrors);
  }



  /**
   * Retrieves the SCIM Service Provider URL.
   *
   * @return The SCIM Service Provider URL.
   */
  public URI getBaseURL() {
    return baseURL;
  }

  /**
   * Retrieves the content media type that should be used when writing data to
   * the SCIM service provider.
   *
   * @return The content media type that should be used when writing data to
   * the SCIM service provider.
   */
  public MediaType getContentType() {
    return contentType;
  }

  /**
   * Sets the content media type that should be used when writing data to
   * the SCIM service provider.
   *
   * @param contentType he content media type that should be used when writing
   * data to the SCIM service provider.
   */
  public void setContentType(final MediaType contentType) {
    this.contentType = contentType;
  }

  /**
   * Retrieves the accept media type that should be used when reading data from
   * the SCIM service provider.
   *
   * @return The accept media type that should be used when reading data from
   * the SCIM service provider.
   */
  public MediaType getAcceptType() {
    return acceptType;
  }

  /**
   * Sets the accept media type that should be used when reading data from
   * the SCIM service provider.
   *
   * @param acceptType The accept media type that should be used when reading
   * data from the SCIM service provider.
   */
  public void setAcceptType(final MediaType acceptType) {
    this.acceptType = acceptType;
  }

  /**
   * Retrieves the user-agent string that will be used in the HTTP request
   * headers.
   *
   * @return The user-agent string. This may be null, in which case a default
   * user-agent will be used.
   */
  public String getUserAgent() {
    return userAgent;
  }

  /**
   * Sets the user-agent string to use in the request headers.
   *
   * @param userAgent The user-agent string that should be used.
   */
  public void setUserAgent(final String userAgent) {
    this.userAgent = userAgent;
  }

  /**
   * Whether to override DELETE operations with POST.
   *
   * @return <code>true</code> to override DELETE operations with POST or
   * <code>false</code> to use the DELETE method.
   */
  public boolean isOverrideDelete() {
    return overrides[2];
  }

  /**
   * Sets whether to override DELETE operations with POST.
   *
   * @param overrideDelete <code>true</code> to override DELETE operations with
   * POST or <code>false</code> to use the DELETE method.
   */
  public void setOverrideDelete(final boolean overrideDelete) {
    this.overrides[2] = overrideDelete;
  }

  /**
   * Whether to override PATCH operations with POST.
   *
   * @return <code>true</code> to override PATCH operations with POST or
   * <code>false</code> to use the PATCH method.
   */
  public boolean isOverridePatch() {
    return overrides[1];
  }

  /**
   * Sets whether to override PATCH operations with POST.
   *
   * @param overridePatch <code>true</code> to override PATCH operations with
   * POST or <code>false</code> to use the PATCH method.
   */
  public void setOverridePatch(final boolean overridePatch) {
    this.overrides[1] = overridePatch;
  }

  /**
   * Whether to override PUT operations with POST.
   *
   * @return <code>true</code> to override PUT operations with POST or
   * <code>false</code> to use the PUT method.
   */
  public boolean isOverridePut() {
    return overrides[0];
  }

  /**
   * Sets whether to override PUT operations with POST.
   *
   * @param overridePut <code>true</code> to override PUT operations with
   * POST or <code>false</code> to use the PUT method.
   */
  public void setOverridePut(final boolean overridePut) {
    this.overrides[0] = overridePut;
  }

  /**
   * Whether to use URL suffix to specify the desired response data format
   * (ie. .json or .xml) instead of using the HTTP Accept Header.
   *
   * @return {@code true} to use URL suffix to specify the desired response
   *         data format or {@code false} to use the HTTP Accept Header.
   */
  public boolean isUseUrlSuffix()
  {
    return useUrlSuffix;
  }

  /**
   * Sets whether to use URL suffix to specify the desired response data format
   * (ie. .json or .xml) instead of using the HTTP Accept Header.
   *
   * @param useUrlSuffix {@code true} to use URL suffix to specify the desired
   *                     response data format or {@code false} to use the HTTP
   *                     Accept Header.
   */
  public void setUseUrlSuffix(final boolean useUrlSuffix)
  {
    this.useUrlSuffix = useUrlSuffix;
  }

  /**
   * Create a new ClientConfig with the default settings.
   *
   * @return A new ClientConfig with the default settings.
   */
  private static ClientConfig createDefaultClientConfig() {
    final PoolingHttpClientConnectionManager mgr =
        new PoolingHttpClientConnectionManager();
    mgr.setMaxTotal(100);
    mgr.setDefaultMaxPerRoute(100);

    ClientConfig jerseyConfig = new ClientConfig();
    ApacheConnectorProvider connectorProvider = new ApacheConnectorProvider();
    jerseyConfig.connectorProvider(connectorProvider);
    return jerseyConfig;
  }

  /**
   * Create a new BasicCredentialsProvider with the provided credentials.
   *
   * @param username The username.
   * @param password The password.
   * @return A new BasicCredentialsProvider.
   */
  private static BasicCredentialsProvider createBasicCredentialsProvider(
      final String username, final String password)
  {
    BasicCredentialsProvider provider = new BasicCredentialsProvider();
    provider.setCredentials(
        AuthScope.ANY,
        new UsernamePasswordCredentials(username, password)
    );
    return provider;
  }
}
