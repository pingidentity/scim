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

package com.unboundid.scim.sdk;

import com.unboundid.scim.data.Meta;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.StreamedQueryRequest;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import com.unboundid.scim.schema.ResourceDescriptor;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpException;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.NoHttpResponseException;
import org.apache.http.UnsupportedHttpVersionException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.RedirectException;
import org.apache.wink.client.ClientAuthenticationException;
import org.apache.wink.client.ClientConfigException;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.ClientRuntimeException;
import org.apache.wink.client.ClientWebException;
import org.apache.wink.client.RestClient;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;


/**
 * This class represents a SCIM endpoint (ie. Users, Groups, etc.) and handles
 * all protocol-level interactions with the service provider. It acts as a
 * helper class for invoking CRUD operations of resources and processing their
 * results.
 *
 * @param <R> The type of resource instances handled by this SCIMEndpoint.
 */
public class SCIMEndpoint<R extends BaseResource>
{
  private final SCIMService scimService;
  private final ResourceDescriptor resourceDescriptor;
  private final ResourceFactory<R> resourceFactory;
  private final Unmarshaller unmarshaller;
  private final Marshaller marshaller;
  private final MediaType contentType;
  private final MediaType acceptType;
  private final boolean[] overrides = new boolean[3];
  private final RestClient client;
  private final boolean useUrlSuffix;


  /**
   * Create a SCIMEndpoint with the provided information.
   *
   * @param scimService The SCIMService to use.
   * @param restClient The Wink REST client.
   * @param resourceDescriptor The resource descriptor of this endpoint.
   * @param resourceFactory The ResourceFactory that should be used to create
   *                        resource instances.
   */
  SCIMEndpoint(final SCIMService scimService,
               final RestClient restClient,
               final ResourceDescriptor resourceDescriptor,
               final ResourceFactory<R> resourceFactory)
  {
    this.scimService = scimService;
    this.client = restClient;
    this.resourceDescriptor = resourceDescriptor;
    this.resourceFactory = resourceFactory;
    this.contentType = scimService.getContentType();
    this.acceptType = scimService.getAcceptType();
    this.overrides[0] = scimService.isOverridePut();
    this.overrides[1] = scimService.isOverridePatch();
    this.overrides[2] = scimService.isOverrideDelete();
    this.useUrlSuffix = scimService.isUseUrlSuffix();

    if (scimService.getContentType().equals(MediaType.APPLICATION_JSON_TYPE))
    {
      this.marshaller = new JsonMarshaller();
    }
    else
    {
      this.marshaller = new XmlMarshaller();
    }

    if(scimService.getAcceptType().equals(MediaType.APPLICATION_JSON_TYPE))
    {
      this.unmarshaller = new JsonUnmarshaller();
    }
    else
    {
      this.unmarshaller = new XmlUnmarshaller();
    }
  }



  /**
   * Constructs a new instance of a resource object which is empty. This
   * method does not interact with the SCIM service. It creates a local object
   * that may be provided to the {@link SCIMEndpoint#create} method after the
   * attributes have been specified.
   *
   * @return  A new instance of a resource object.
   */
  public R newResource()
  {
    return resourceFactory.createResource(resourceDescriptor, new SCIMObject());
  }

  /**
   * Retrieves a resource instance given the ID.
   *
   * @param id The ID of the resource to retrieve.
   * @return The retrieved resource.
   * @throws SCIMException If an error occurs.
   */
  public R get(final String id)
      throws SCIMException
  {
    return get(id, null, (String[]) null);
  }

  /**
   * Retrieves a resource instance given the ID, only if the current version
   * has been modified.
   *
   * @param id The ID of the resource to retrieve.
   * @param etag The entity tag that indicates the entry should be returned
   *             only if the entity tag of the current resource is different
   *             from the provided value and a value of "*" will not return
   *             an entry if the resource still exists. A value of
   *             <code>null</code> indicates unconditional return.
   * @param requestedAttributes The attributes of the resource to retrieve.
   * @return The retrieved resource.
   * @throws SCIMException If an error occurs.
   */
  public R get(final String id, final String etag,
               final String... requestedAttributes)
      throws SCIMException
  {
    final UriBuilder uriBuilder = UriBuilder.fromUri(scimService.getBaseURL());
    uriBuilder.path(resourceDescriptor.getEndpoint());

    // The ServiceProviderConfig is a special case where the id is not
    // specified.
    if (id != null)
    {
      uriBuilder.path(id);
    }

    URI uri = uriBuilder.build();
    org.apache.wink.client.Resource clientResource =
        client.resource(completeUri(uri));
    if(!useUrlSuffix)
    {
      clientResource.accept(acceptType);
    }
    clientResource.contentType(contentType);
    addAttributesQuery(clientResource, requestedAttributes);

    if(scimService.getUserAgent() != null)
    {
      clientResource.header(HttpHeaders.USER_AGENT, scimService.getUserAgent());
    }

    if(etag != null && !etag.isEmpty())
    {
      clientResource.header(HttpHeaders.IF_NONE_MATCH, etag);
    }

    InputStream entity = null;
    try
    {
      ClientResponse response = clientResource.get();
      entity = response.getEntity(InputStream.class);

      if(response.getStatusType() == Response.Status.OK)
      {
        R resource = unmarshaller.unmarshal(entity, resourceDescriptor,
            resourceFactory);
        addMissingMetaData(response, resource);
        return resource;
      }
      else
      {
        throw createErrorResponseException(response, entity);
      }
    }
    catch(SCIMException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw SCIMException.createException(getStatusCode(e),
                                          getExceptionMessage(e), e);
    }
    finally
    {
      try {
        if (entity != null) {
          entity.close();
        }
      } catch (IOException e) {
        // Lets just log this and ignore.
        Debug.debugException(e);
      }
    }
  }

  /**
   * Retrieves all resource instances that match the provided filter.
   *
   * @param filter The filter that should be used.
   * @return The resource instances that match the provided filter.
   * @throws SCIMException If an error occurs.
   */
  public Resources<R> query(final String filter)
      throws SCIMException
  {
    return query(filter, null, null);
  }

  /**
   * Retrieves all resource instances that match the provided filter.
   * Matching resources are returned sorted according to the provided
   * SortParameters. PageParameters maybe used to specify the range of
   * resource instances that are returned.
   *
   * @param filter The filter that should be used.
   * @param sortParameters The sort parameters that should be used.
   * @param pageParameters The page parameters that should be used.
   * @param requestedAttributes The attributes of the resource to retrieve.
   * @return The resource instances that match the provided filter.
   * @throws SCIMException If an error occurs.
   */
  public Resources<R> query(final String filter,
                            final SortParameters sortParameters,
                            final PageParameters pageParameters,
                            final String... requestedAttributes)
      throws SCIMException
  {
    return query(filter, sortParameters, pageParameters,
                    null, requestedAttributes);
  }

  /**
   * Retrieves all resource instances that match the provided filter.
   * Matching resources are returned sorted according to the provided
   * SortParameters. PageParameters maybe used to specify the range of
   * resource instances that are returned. Additional query parameters may
   * be specified using a Map of parameter names to their values.
   *
   * @param filter The filter that should be used.
   * @param sortParameters The sort parameters that should be used.
   * @param pageParameters The page parameters that should be used.
   * @param additionalQueryParams A map of additional query parameters that
   *                              should be included.
   * @param requestedAttributes The attributes of the resource to retrieve.
   * @return The resource instances that match the provided filter.
   * @throws SCIMException If an error occurs.
   */
  public Resources<R> query(final String filter,
                            final SortParameters sortParameters,
                            final PageParameters pageParameters,
                            final Map<String,String> additionalQueryParams,
                            final String... requestedAttributes)
      throws SCIMException
  {
    URI uri =
        UriBuilder.fromUri(scimService.getBaseURL()).path(
            resourceDescriptor.getEndpoint()).build();
    org.apache.wink.client.Resource clientResource =
        client.resource(completeUri(uri));
    if(!useUrlSuffix)
    {
      clientResource.accept(acceptType);
    }
    clientResource.contentType(contentType);
    addAttributesQuery(clientResource, requestedAttributes);
    if(scimService.getUserAgent() != null)
    {
      clientResource.header("User-Agent", scimService.getUserAgent());
    }
    if(filter != null)
    {
      clientResource.queryParam("filter", filter);
    }
    if(sortParameters != null)
    {
      clientResource.queryParam("sortBy",
          sortParameters.getSortBy().toString());
      if(!sortParameters.isAscendingOrder())
      {
        clientResource.queryParam("sortOrder", sortParameters.getSortOrder());
      }
    }
    if(pageParameters != null)
    {
      clientResource.queryParam("startIndex",
          String.valueOf(pageParameters.getStartIndex()));
      if (pageParameters.getCount() > 0)
      {
        clientResource.queryParam("count",
                                  String.valueOf(pageParameters.getCount()));
      }
    }
    if(additionalQueryParams != null)
    {
      for (String key : additionalQueryParams.keySet())
      {
        clientResource.queryParam(key, additionalQueryParams.get(key));
      }
    }

    InputStream entity = null;
    try
    {
      ClientResponse response = clientResource.get();
      entity = response.getEntity(InputStream.class);

      if(response.getStatusType() == Response.Status.OK)
      {
        return unmarshaller.unmarshalResources(entity, resourceDescriptor,
            resourceFactory);
      }
      else
      {
        throw createErrorResponseException(response, entity);
      }
    }
    catch(SCIMException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw SCIMException.createException(getStatusCode(e),
                                          getExceptionMessage(e), e);
    }
    finally
    {
      try {
        if (entity != null) {
          entity.close();
        }
      } catch (IOException e) {
        // Lets just log this and ignore.
        Debug.debugException(e);
      }
    }
  }


  /**
   * Retrieve resource instances using the UnboundID SCIM extension for
   * streamed query.
   * <p/>
   * Using this form of query will result in better performance when
   * querying against a large candidate result set.  However this query
   * does not support sorting of results or random access into the result
   * set (i.e. specification of a start index).
   * <p/>
   * The return from this method includes both a page of resource instances
   * and a resume token that can be used to retrieve the next page of results.
   * An empty resume token indicates that there are no more results.
   * The number of resources returned may be less than the requested page size,
   * even when there is a non-empty resume token.
   * <p>
   * Example usage:
   * </p>
   * <pre>
   *   StreamedQueryRequest request = new StreamedQueryRequest(
   *         filter, attributes, pageSize);
   *   ListResponse<BaseResource> response;
   *   do
   *   {
   *     response = endpoint.streamedQuery(request);
   *     for (BaseResource b : response)
   *     {
   *       // do something with the resource
   *     }
   *   }
   *   while (request.hasMoreResults(response));
   *
   * </pre>
   * @param request StreamedQueryRequest object specifying the query parameters.
   * @return A single page of resource instances that match the provided
   *  filter.
   * @throws SCIMException if an error occurs.
   */
  public ListResponse<R> streamedQuery(
      final StreamedQueryRequest request)
      throws SCIMException
  {
    URI uri =
        UriBuilder.fromUri(scimService.getBaseURL()).path(
            resourceDescriptor.getEndpoint()).path("/.search").build();
    org.apache.wink.client.Resource clientResource =
        client.resource(uri);
    if(!useUrlSuffix)
    {
      clientResource.accept(acceptType);
    }
    clientResource.contentType(contentType);

    StreamingOutput requestStream = new StreamingOutput()
    {
      public void write(final OutputStream outputStream)
          throws IOException, WebApplicationException
      {
        try {
          marshaller.marshal(request, outputStream);
        }
        catch (Exception e)
        {
          throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
      }
    };

    InputStream entity = null;
    try
    {
      ClientResponse response = clientResource.post(requestStream);
      entity = response.getEntity(InputStream.class);

      if(response.getStatusType() == Response.Status.OK)
      {
        return unmarshaller.unmarshalListResponse(entity,
            resourceDescriptor, resourceFactory);
      }
      else
      {
        throw createErrorResponseException(response, entity);
      }
    }
    catch(SCIMException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw SCIMException.createException(getStatusCode(e),
          getExceptionMessage(e), e);
    }
    finally
    {
      try {
        if (entity != null)
        {
          entity.close();
        }
      } catch (IOException e)
      {
        // Lets just log this and ignore.
        Debug.debugException(e);
      }
    }
  }



  /**
   * Create the specified resource instance at the service provider and return
   * only the specified attributes from the newly inserted resource.
   *
   * @param resource The resource to create.
   * @param requestedAttributes The attributes of the newly inserted resource
   *                            to retrieve.
   * @return The newly inserted resource returned by the service provider.
   * @throws SCIMException If an error occurs.
   */
  public R create(final R resource,
                  final String... requestedAttributes)
      throws SCIMException
  {

    URI uri =
        UriBuilder.fromUri(scimService.getBaseURL()).path(
            resourceDescriptor.getEndpoint()).build();
    org.apache.wink.client.Resource clientResource =
        client.resource(completeUri(uri));
    if(!useUrlSuffix)
    {
      clientResource.accept(acceptType);
    }
    clientResource.contentType(contentType);
    addAttributesQuery(clientResource, requestedAttributes);
    if(scimService.getUserAgent() != null)
    {
      clientResource.header("User-Agent", scimService.getUserAgent());
    }

    StreamingOutput output = new StreamingOutput() {
      public void write(final OutputStream outputStream)
          throws IOException, WebApplicationException {
        try {
          marshaller.marshal(resource, outputStream);
        } catch (Exception e) {
          throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
      }
    };


    InputStream entity = null;
    try
    {
      ClientResponse response = clientResource.post(output);
      entity = response.getEntity(InputStream.class);

      if(response.getStatusType() == Response.Status.CREATED)
      {
        R postedResource = unmarshaller.unmarshal(entity, resourceDescriptor,
            resourceFactory);
        addMissingMetaData(response, postedResource);
        return postedResource;
      }
      else
      {
        throw createErrorResponseException(response, entity);
      }
    }
    catch(SCIMException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw SCIMException.createException(getStatusCode(e),
                                          getExceptionMessage(e), e);
    }
    finally
    {
      try {
        if (entity != null) {
          entity.close();
        }
      } catch (IOException e) {
        // Lets just log this and ignore.
        Debug.debugException(e);
      }
    }
  }

  /**
   * Update the existing resource with the one provided (using the HTTP PUT
   * method).
   *
   * @param resource The modified resource to be updated.
   * @return The updated resource returned by the service provider.
   * @throws SCIMException If an error occurs.
   */
  public R update(final R resource)
      throws SCIMException
  {
    if(resource.getId() == null)
    {
      throw new InvalidResourceException("Resource must have a valid ID");
    }
    return update(resource.getId(),
                  resource.getMeta() == null ?
                      null : resource.getMeta().getVersion(),
                  resource);
  }

  /**
   * Update the existing resource with the one provided (using the HTTP PUT
   * method). This update is conditional upon the provided entity tag matching
   * the tag from the current resource. If (and only if) they match, the update
   * will be performed.
   *
   * @param resource The modified resource to be updated.
   * @param etag The entity tag value that is the expected value for the target
   *             resource. A value of <code>null</code> will not set an
   *             etag precondition and a value of "*" will perform an
   *             unconditional update.
   * @param requestedAttributes The attributes of updated resource
   *                            to return.
   * @return The updated resource returned by the service provider.
   * @throws SCIMException If an error occurs.
   */
  @Deprecated
  public R update(final R resource, final String etag,
                  final String... requestedAttributes)
      throws SCIMException
  {
    String id = resource.getId();
    if(id == null)
    {
      throw new InvalidResourceException("Resource must have a valid ID");
    }
    return update(id, etag, resource, requestedAttributes);
  }

  /**
   * Update the existing resource with the one provided (using the HTTP PUT
   * method). This update is conditional upon the provided entity tag matching
   * the tag from the current resource. If (and only if) they match, the update
   * will be performed.
   *
   * @param id The ID of the resource to update.
   * @param etag The entity tag value that is the expected value for the target
   *             resource. A value of <code>null</code> will not set an
   *             etag precondition and a value of "*" will perform an
   *             unconditional update.
   * @param resource The modified resource to be updated.
   * @param requestedAttributes The attributes of updated resource
   *                            to return.
   * @return The updated resource returned by the service provider.
   * @throws SCIMException If an error occurs.
   */
  public R update(final String id, final String etag, final R resource,
                  final String... requestedAttributes)
      throws SCIMException
  {
    URI uri =
        UriBuilder.fromUri(scimService.getBaseURL()).path(
            resourceDescriptor.getEndpoint()).path(id).build();
    org.apache.wink.client.Resource clientResource =
        client.resource(completeUri(uri));
    if(!useUrlSuffix)
    {
      clientResource.accept(acceptType);
    }
    clientResource.contentType(contentType);
    addAttributesQuery(clientResource, requestedAttributes);
    if(scimService.getUserAgent() != null)
    {
      clientResource.header(HttpHeaders.USER_AGENT, scimService.getUserAgent());
    }
    if(etag != null && !etag.isEmpty())
    {
      clientResource.header(HttpHeaders.IF_MATCH, etag);
    }

    StreamingOutput output = new StreamingOutput() {
      public void write(final OutputStream outputStream)
          throws IOException, WebApplicationException {
        try {
          marshaller.marshal(resource, outputStream);
        } catch (Exception e) {
          throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
      }
    };

    InputStream entity = null;
    try
    {
      ClientResponse response;
      if(overrides[0])
      {
        clientResource.header("X-HTTP-Method-Override", "PUT");
        response = clientResource.post(output);
      }
      else
      {
        response = clientResource.put(output);
      }

      entity = response.getEntity(InputStream.class);

      if(response.getStatusType() == Response.Status.OK)
      {
        R postedResource = unmarshaller.unmarshal(entity, resourceDescriptor,
            resourceFactory);
        addMissingMetaData(response, postedResource);
        return postedResource;
      }
      else
      {
        throw createErrorResponseException(response, entity);
      }
    }
    catch(SCIMException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw SCIMException.createException(getStatusCode(e),
                                          getExceptionMessage(e), e);
    }
    finally
    {
      try {
        if (entity != null) {
          entity.close();
        }
      } catch (IOException e) {
        // Lets just log this and ignore.
        Debug.debugException(e);
      }
    }
  }

  /**
   * Update the existing resource with the one provided (using the HTTP PATCH
   * method). Note that if the {@code attributesToDelete} parameter is
   * specified, those attributes will be removed from the resource before the
   * {@code attributesToUpdate} are merged into the resource.
   *
   * @param resource The to update.
   * @param attributesToUpdate The list of attributes (and their new values) to
   *                           update on the resource.
   * @param attributesToDelete The list of attributes to delete on the resource.
   * @return The updated resource returned by the service provider, or
   *         an empty resource with returned meta data if the service provider
   *         returned no content.
   * @throws SCIMException If an error occurs.
   */
  public R update(final R resource,
                  final List<SCIMAttribute> attributesToUpdate,
                  final List<String> attributesToDelete)
      throws SCIMException
  {
    if(resource.getId() == null)
    {
      throw new InvalidResourceException("Resource must have a valid ID");
    }

    return update(resource.getId(),
        resource.getMeta() == null ?
            null : resource.getMeta().getVersion(),
        attributesToUpdate,
        attributesToDelete);
  }

  /**
   * Update the existing resource with the one provided (using the HTTP PATCH
   * method). Note that if the {@code attributesToDelete} parameter is
   * specified, those attributes will be removed from the resource before the
   * {@code attributesToUpdate} are merged into the resource.
   *
   * @param id The ID of the resource to update.
   * @param etag The entity tag value that is the expected value for the target
   *             resource. A value of <code>null</code> will not set an
   *             etag precondition and a value of "*" will perform an
   *             unconditional update.
   * @param attributesToUpdate The list of attributes (and their new values) to
   *                           update on the resource. These attributes should
   *                           conform to Section 3.2.2 of the SCIM 1.1
   *                           specification (<i>draft-scim-api-01</i>),
   *                           "Modifying Resources with PATCH".
   * @param attributesToDelete The list of attributes to delete on the resource.
   * @param requestedAttributes The attributes of updated resource to return.
   * @return The updated resource returned by the service provider, or
   *         an empty resource with returned meta data if the
   *         {@code requestedAttributes} parameter was not specified and
   *         the service provider returned no content.
   * @throws SCIMException If an error occurs.
   */
  public R update(final String id, final String etag,
                  final List<SCIMAttribute> attributesToUpdate,
                  final List<String> attributesToDelete,
                  final String... requestedAttributes)
          throws SCIMException
  {
    URI uri =
            UriBuilder.fromUri(scimService.getBaseURL()).path(
                    resourceDescriptor.getEndpoint()).path(id).build();
    org.apache.wink.client.Resource clientResource =
        client.resource(completeUri(uri));
    if(!useUrlSuffix)
    {
      clientResource.accept(acceptType);
    }
    clientResource.contentType(contentType);
    addAttributesQuery(clientResource, requestedAttributes);

    if(scimService.getUserAgent() != null)
    {
      clientResource.header(HttpHeaders.USER_AGENT, scimService.getUserAgent());
    }
    if(etag != null && !etag.isEmpty())
    {
      clientResource.header(HttpHeaders.IF_MATCH, etag);
    }

    Diff<R> diff = new Diff<R>(resourceDescriptor, attributesToDelete,
                               attributesToUpdate);
    final BaseResource resource =
            diff.toPartialResource(resourceFactory, true);

    StreamingOutput output = new StreamingOutput() {
      public void write(final OutputStream outputStream)
              throws IOException, WebApplicationException {
        try {
          marshaller.marshal(resource, outputStream);
        } catch (Exception e) {
          throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
      }
    };

    InputStream entity = null;
    try
    {
      ClientResponse response;
      if(overrides[1])
      {
        clientResource.header("X-HTTP-Method-Override", "PATCH");
        response = clientResource.post(output);
      }
      else
      {
        try
        {
          // WINK client doesn't have an invoke method where it always
          // returns a ClientResponse like the other put, post, and get methods.
          // This throws a ClientWebException if the server returns a non 200
          // code.
          response =
              clientResource.invoke("PATCH", ClientResponse.class, output);
        }
        catch (ClientWebException e)
        {
          response = e.getResponse();
        }
      }

      entity = response.getEntity(InputStream.class);

      if(response.getStatusType() == Response.Status.OK)
      {
        R patchedResource = unmarshaller.unmarshal(entity, resourceDescriptor,
                resourceFactory);
        addMissingMetaData(response, patchedResource);
        return patchedResource;
      }
      else if (response.getStatusType() == Response.Status.NO_CONTENT)
      {
        R emptyResource =
            resourceFactory.createResource(resourceDescriptor,
                new SCIMObject());
        emptyResource.setId(id);
        addMissingMetaData(response, emptyResource);
        return emptyResource;
      }
      else
      {
        throw createErrorResponseException(response, entity);
      }
    }
    catch(SCIMException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw SCIMException.createException(getStatusCode(e),
                                          getExceptionMessage(e), e);
    }
    finally
    {
      try {
        if (entity != null) {
          entity.close();
        }
      } catch (IOException e) {
        Debug.debugException(e);
      }
    }
  }

  /**
   * Update the existing resource with the one provided (using the HTTP PATCH
   * method). Note that if the {@code attributesToDelete} parameter is
   * specified, those attributes will be removed from the resource before the
   * {@code attributesToUpdate} are merged into the resource.
   *
   * @param id The ID of the resource to update.
   * @param attributesToUpdate The list of attributes (and their new values) to
   *                           update on the resource.
   * @param attributesToDelete The list of attributes to delete on the resource.
   * @throws SCIMException If an error occurs.
   */
  @Deprecated
  public void update(final String id,
                     final List<SCIMAttribute> attributesToUpdate,
                     final List<String> attributesToDelete)
           throws SCIMException
  {
    update(id, null, attributesToUpdate, attributesToDelete);
  }

  /**
   * Delete the resource instance specified by the provided ID.
   *
   * @param id The ID of the resource to delete.
   * @throws SCIMException If an error occurs.
   */
  @Deprecated
  public void delete(final String id)
      throws SCIMException
  {
    delete(id, null);
  }

  /**
   * Delete the specified resource instance.
   *
   * @param resource The resource to delete.
   * @throws SCIMException If an error occurs.
   */
  public void delete(final R resource)
      throws SCIMException
  {
    delete(resource.getId(), resource.getMeta() == null ?
        null : resource.getMeta().getVersion());
  }

  /**
   * Delete the resource instance specified by the provided ID. This delete is
   * conditional upon the provided entity tag matching the tag from the
   * current resource. If (and only if) they match, the delete will be
   * performed.
   *
   * @param id The ID of the resource to delete.
   * @param etag The entity tag value that is the expected value for the target
   *             resource. A value of <code>null</code> will not set an
   *             etag precondition and a value of "*" will perform an
   *             unconditional delete.
   * @throws SCIMException If an error occurs.
   */
  public void delete(final String id, final String etag)
      throws SCIMException
  {
    URI uri =
        UriBuilder.fromUri(scimService.getBaseURL()).path(
            resourceDescriptor.getEndpoint()).path(id).build();
    org.apache.wink.client.Resource clientResource =
        client.resource(completeUri(uri));
    if(!useUrlSuffix)
    {
      clientResource.accept(acceptType);
    }
    clientResource.contentType(contentType);
    if(scimService.getUserAgent() != null)
    {
      clientResource.header(HttpHeaders.USER_AGENT, scimService.getUserAgent());
    }
    if(etag != null && !etag.isEmpty())
    {
      clientResource.header(HttpHeaders.IF_MATCH, etag);
    }


    InputStream entity = null;
    try
    {
      ClientResponse response;
      if(overrides[2])
      {
        clientResource.header("X-HTTP-Method-Override", "DELETE");
        response = clientResource.post(null);
      } else
      {
        response = clientResource.delete();
      }
      if(response.getStatusType() != Response.Status.OK)
      {
        entity = response.getEntity(InputStream.class);
        throw createErrorResponseException(response, entity);
      }
      else
      {
        response.consumeContent();
      }
    }
    catch(SCIMException e)
    {
      throw e;
    }
    catch(ClientRuntimeException cre)
    {
      // Treat SocketTimeoutException like HTTP 100
      Throwable rootCause = StaticUtils.getRootCause(cre);
      if (rootCause == null || !(rootCause instanceof SocketTimeoutException))
      {
        throw cre;
      }
    }
    catch(Exception e)
    {
      throw SCIMException.createException(getStatusCode(e),
                                          getExceptionMessage(e), e);
    }
    finally
    {
      try {
        if (entity != null) {
          entity.close();
        }
      } catch (IOException e) {
        // Lets just log this and ignore.
        Debug.debugException(e);
      }
    }
  }

  /**
   * Add the attributes query parameter to the client resource request.
   *
   * @param clientResource The Wink client resource.
   * @param requestedAttributes The SCIM attributes to request.
   */
  private void addAttributesQuery(
      final org.apache.wink.client.Resource clientResource,
      final String... requestedAttributes)
  {
    if(requestedAttributes != null && requestedAttributes.length > 0)
    {
      StringBuilder stringBuilder = new StringBuilder();
      for(int i = 0; i < requestedAttributes.length; i++)
      {
        stringBuilder.append(requestedAttributes[i]);
        if(i < requestedAttributes.length - 1)
        {
          stringBuilder.append(",");
        }
      }
      clientResource.queryParam("attributes", stringBuilder.toString());
    }
  }

  /**
   * Add meta values from the response header to the meta complex attribute
   * if they are missing.
   *
   * @param response The response from the service provider.
   * @param resource The return resource instance.
   */
  private void addMissingMetaData(final ClientResponse response,
                                  final R resource)
  {
    URI headerLocation = null;
    String headerEtag = null;
    List<String> values;
    if(response.getStatusType() == Response.Status.CREATED ||
                    response.getStatusType().getFamily() ==
                        Response.Status.Family.REDIRECTION)
    {
      values = response.getHeaders().get(HttpHeaders.LOCATION);
    }
    else
    {
      values = response.getHeaders().get(HttpHeaders.CONTENT_LOCATION);
      if(values == null || values.isEmpty())
      {
        // Fall back to using Location header to maintain backwards
        // compatibility.
        values = response.getHeaders().get(HttpHeaders.LOCATION);
      }
    }
    if(values != null && !values.isEmpty())
    {
      headerLocation = URI.create(values.get(0));
    }
    values = response.getHeaders().get(HttpHeaders.ETAG);
    if(values != null && !values.isEmpty())
    {
      headerEtag = values.get(0);
    }
    Meta meta = resource.getMeta();
    if(meta == null)
    {
      meta = new Meta(null, null, null, null);
    }
    boolean modified = false;
    if(headerLocation != null && meta.getLocation() == null)
    {
      meta.setLocation(headerLocation);
      modified = true;
    }
    if(headerEtag != null && meta.getVersion() == null)
    {
      meta.setVersion(headerEtag);
      modified = true;
    }
    if(modified)
    {
      resource.setMeta(meta);
    }
  }



  /**
   * Returns a SCIM exception representing the error response.
   *
   * @param response  The client response.
   * @param entity    The response content.
   *
   * @return  The SCIM exception representing the error response.
   */
  private SCIMException createErrorResponseException(
      final ClientResponse response,
      final InputStream entity)
  {
    SCIMException scimException = null;

    if(entity != null)
    {
      try
      {
        scimException = unmarshaller.unmarshalError(entity);
      }
      catch (InvalidResourceException e)
      {
        // The response content could not be parsed as a SCIM error
        // response, which is the case if the response is a more general
        // HTTP error. It is better to just provide the HTTP response
        // details in this case.
        Debug.debugException(e);
      }
    }

    if(scimException == null)
    {
      scimException = SCIMException.createException(
          response.getStatusCode(), response.getMessage());
    }

    if(response.getStatusType() == Response.Status.PRECONDITION_FAILED)
    {
      scimException = new PreconditionFailedException(
          scimException.getMessage(),
          response.getHeaders().getFirst(HttpHeaders.ETAG),
          scimException.getCause());
    }
    else if(response.getStatusType() == Response.Status.NOT_MODIFIED)
    {
      scimException = new NotModifiedException(
          scimException.getMessage(),
          response.getHeaders().getFirst(HttpHeaders.ETAG),
          scimException.getCause());
    }

    return scimException;
  }




  /**
   * Returns the complete resource URI by appending the suffix if necessary.
   *
   * @param uri The URI to complete.
   * @return The completed URI.
   */
  private URI completeUri(final URI uri)
  {
    URI completedUri = uri;
    if(useUrlSuffix)
    {
      try
      {
      if(acceptType == MediaType.APPLICATION_JSON_TYPE)
      {
        completedUri = new URI(uri.toString() + ".json");
      }
      else if(acceptType == MediaType.APPLICATION_XML_TYPE)
      {
        completedUri = new URI(uri.toString() + ".xml");
      }
      }
      catch(URISyntaxException e)
      {
        throw new RuntimeException(e);
      }
    }
    return completedUri;
  }

  /**
   * Tries to deduce the most appropriate HTTP response code from the given
   * exception. This method expects most exceptions to be one of 3 or 4
   * expected runtime exceptions that are common to Wink and the Apache Http
   * Client library.
   * <p>
   * Note this method can return -1 for the special case of a
   * {@link com.unboundid.scim.sdk.ConnectException}, in which the service
   * provider could not be reached at all.
   *
   * @param t the Exception instance to analyze
   * @return the most appropriate HTTP status code
   */
  static int getStatusCode(final Throwable t)
  {
    Throwable rootCause = t;
    if(rootCause instanceof ClientRuntimeException)
    {
      //Pull the underlying cause out of the ClientRuntimeException
      rootCause = StaticUtils.getRootCause(t);
    }

    if(rootCause instanceof HttpResponseException)
    {
      HttpResponseException hre = (HttpResponseException) rootCause;
      return hre.getStatusCode();
    }
    else if(rootCause instanceof HttpException)
    {
      if(rootCause instanceof RedirectException)
      {
        return 300;
      }
      else if(rootCause instanceof AuthenticationException)
      {
        return 401;
      }
      else if(rootCause instanceof MethodNotSupportedException)
      {
        return 501;
      }
      else if(rootCause instanceof UnsupportedHttpVersionException)
      {
        return 505;
      }
    }
    else if(rootCause instanceof IOException)
    {
      if(rootCause instanceof NoHttpResponseException)
      {
        return 503;
      }
      else if(rootCause instanceof ConnectionClosedException)
      {
        return 503;
      }
      else
      {
        return -1;
      }
    }

    if(t instanceof ClientWebException)
    {
      ClientWebException cwe = (ClientWebException) t;
      return cwe.getResponse().getStatusCode();
    }
    else if(t instanceof ClientAuthenticationException)
    {
      return 401;
    }
    else if(t instanceof ClientConfigException)
    {
      return 400;
    }
    else
    {
      return 500;
    }
  }

  /**
   * Extracts the exception message from the root cause of the exception if
   * possible.
   *
   * @param t the original Throwable that was caught. This may be null.
   * @return the exception message from the root cause of the exception, or
   *         null if the specified Throwable is null or the message cannot be
   *         determined.
   */
  static String getExceptionMessage(final Throwable t)
  {
    if(t == null)
    {
      return null;
    }

    Throwable rootCause = StaticUtils.getRootCause(t);
    return rootCause.getMessage();
  }
}
