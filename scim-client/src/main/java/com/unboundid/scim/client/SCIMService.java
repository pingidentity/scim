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

package com.unboundid.scim.client;

import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMException;
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
   * Constructs a SCIM client service.
   * @param baseUrl The SCIM Service Provider URL.
   */
  public SCIMService(final URI baseUrl)
  {
    this.baseURL = baseUrl;

    ClientConfig config = new ClientConfig();
    this.client = new RestClient(config);
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
   * Retrieve all service provider supported resources.
   *
   * @return All service provider supported resources.
   */
  public SCIMEndpoint<ResourceDescriptor> getResourceSchemaEndpoint()
  {
    return new SCIMEndpoint<ResourceDescriptor>(this, client,
        CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY);
  }

  /**
   * Retrieve the resource schema for provided resource endpoint.
   *
   * @param resourceName The name of the resource endpoint.
   * @return The ResourceDescriptor for the endpoint or <code>null</code> if
   *         it is not available.
   * @throws SCIMException if an error occurs.
   */
  public ResourceDescriptor getResourceSchema(final String resourceName)
    throws SCIMException
  {
    if(resourceName.equals(SCIMConstants.RESOURCE_NAME_USER))
    {
      return CoreSchema.USER_DESCRIPTOR;
    }
    else if(resourceName.equals(SCIMConstants.RESOURCE_NAME_GROUP))
    {
      return CoreSchema.GROUP_DESCRIPTOR;
    }
    else
    {
      throw new ResourceNotFoundException("Resource " + resourceName +
          " not defined by the service provider");
    }
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
   * Retrieves the accept media type that should be used when reader data from
   * the SCIM service provider.
   *
   * @return The accept media type that should be used when reader data from
   * the SCIM service provider.
   */
  public MediaType getAcceptType() {
    return acceptType;
  }

  /**
   * Sets the accept media type that should be used when reader data from
   * the SCIM service provider.
   *
   * @param acceptType The accept media type that should be used when reader
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

  /**
   * Sets the basic authentication credentials that should be used.
   *
   * @param username The username;
   * @param password The password.
   */
  public void setUserCredentials(final String username, final String password)
  {
    ClientConfig config = new ClientConfig();
    HttpBasicAuthSecurityHandler basicAuth = new HttpBasicAuthSecurityHandler
      (username,password);
    config.handlers(basicAuth);
    this.client = new RestClient(config);
  }
}
