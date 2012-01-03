/*
 * Copyright 2011-2012 UnboundID Corp.
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
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.RestClient;

import javax.ws.rs.core.MediaType;
import java.net.URI;

/**
 * The SCIMService class represents a client connection to a SCIM service
 * provider. It handles setting up and configuring the connection which will
 * be used by the SCIMEndpoints that are obtained form this SCIMService.
 */
public class SCIMService
{
  private RestClient client;
  private final URI baseURL;

  private MediaType acceptType = MediaType.APPLICATION_JSON_TYPE;
  private MediaType contentType = MediaType.APPLICATION_JSON_TYPE;
  private boolean[] overrides = new boolean[3];

  /**
   * Constructs a new SCIMService that is configured from the provided
   * <code>org.apache.wink.client.ClientConfig</code> instance.
   *
   * @param baseUrl The SCIM Service Provider URL.
   * @param clientConfig The configuration to use.
   */
  public SCIMService(final URI baseUrl, final ClientConfig clientConfig)
  {
    this.baseURL = baseUrl;
    this.client = new RestClient(clientConfig);
  }

  /**
   * Constructs a new SCIMService that uses
   * <code>java.net.HttpURLConnection</code> for the HTTP layer.
   *
   * @param baseUrl The SCIM Service Provider URL.
   */
  public SCIMService(final URI baseUrl)
  {
    this(baseUrl, new ClientConfig());
  }

  /**
   * Constructs a new SCIMService with basic authentication support
   * using the provided credentials. This SCIMService will use
   * <code>java.net.HttpURLConnection</code> for the HTTP layer.
   *
   * @param baseUrl The SCIM Service Provider URL.
   * @param username The username.
   * @param password The password.
   */
  public SCIMService(final URI baseUrl, final String username,
                     final String password)
  {
    this(baseUrl, new ClientConfig().handlers(new HttpBasicAuthSecurityHandler
      (username,password)));
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
    return resources.iterator().next();
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
}
