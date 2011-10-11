/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.client;

import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMConstants;
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
  private final RestClient client;
  private final URI baseURL;

  private MediaType acceptType = MediaType.APPLICATION_JSON_TYPE;
  private MediaType contentType = MediaType.APPLICATION_JSON_TYPE;
  private boolean[] overrides = new boolean[3];

  /**
   * Constructs a SCIM client service.
   * @param username The SCIM Consumer username.
   * @param password The SCIM Consumer password.
   * @param baseUrl The SCIM Service Provider URL.
   */
  public SCIMService(final String username, final String password,
                     final URI baseUrl)
  {
    this.baseURL = baseUrl;

    ClientConfig config = new ClientConfig();
    // setup BASIC Auth
//    BasicAuthSecurityHandler basicAuth = new BasicAuthSecurityHandler();
//    basicAuth.setUserName(username);
//    basicAuth.setPassword(password);
    HttpBasicAuthSecurityHandler basicAuth = new HttpBasicAuthSecurityHandler
      (username,password);
    config.handlers(basicAuth);
    this.client = new RestClient(config);
  }

  /**
   * Returns a SCIMEndpoint with the current settings that can be used to
   * invoke CRUD operations.
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
    ResourceDescriptor userResourceDescriptor =
        SchemaManager.instance().getResourceDescriptor(
            SCIMConstants.RESOURCE_NAME_USER);
    return new SCIMEndpoint<UserResource>(this, client,
        userResourceDescriptor, UserResource.USER_RESOURCE_FACTORY);
  }

  /**
   * Returns a SCIMEndpoint for the Groups endpoint defined in the core schema.
   *
   * @return The SCIMEndpoint for the Groups endpoint defined in the
   *         core schema.
   */
  public SCIMEndpoint<GroupResource> getGroupEndpoint()
  {
    ResourceDescriptor userResourceDescriptor =
        SchemaManager.instance().getResourceDescriptor(
            SCIMConstants.RESOURCE_NAME_GROUP);
    return new SCIMEndpoint<GroupResource>(this, client,
        userResourceDescriptor, GroupResource.GROUP_RESOURCE_FACTORY);
  }

  /**
   * Retrieve all service provider supported resources.
   *
   * @return All service provider supported resources.
   */
  public SchemaManager retrieveResources()
  {
    return null;
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
}
