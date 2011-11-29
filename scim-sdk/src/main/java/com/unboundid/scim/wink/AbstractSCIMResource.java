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

package com.unboundid.scim.wink;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DeleteResourceRequest;
import com.unboundid.scim.sdk.GetResourceRequest;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.PostResourceRequest;
import com.unboundid.scim.sdk.PutResourceRequest;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMResponse;
import com.unboundid.scim.sdk.SortParameters;
import org.apache.wink.common.AbstractDynamicResource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import static com.unboundid.scim.sdk.SCIMConstants.
    HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.unboundid.scim.sdk.SCIMConstants.
    HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_ATTRIBUTES;



/**
 * This class is an abstract Wink dynamic resource implementation for
 * SCIM operations on a SCIM endpoint. The set of supported resources and their
 * endpoints are not known until run-time hence it must be implemented as a
 * dynamic resource.
 */
public abstract class AbstractSCIMResource extends AbstractDynamicResource
{
  /**
   * The ResourceDescriptor for this resource.
   */
  private final ResourceDescriptor resourceDescriptor;

  /**
   * The ResourceStats used to keep activity statistics.
   */
  private final ResourceStats resourceStats;

  /**
   * The SCIMBackend to use to process requests.
   */
  private final SCIMBackend backend;

  /**
   * Create a new AbstractSCIMResource for CRUD operations.
   *
   * @param path                The path of this resource.
   * @param resourceDescriptor  The resource descriptor to use.
   * @param resourceStats       The ResourceStats instance to use.
   * @param backend             The SCIMBackend to use to process requests.
   */
  public AbstractSCIMResource(final String path,
                              final ResourceDescriptor resourceDescriptor,
                              final ResourceStats resourceStats,
                              final SCIMBackend backend)
  {
    this.resourceDescriptor = resourceDescriptor;
    this.backend = backend;
    this.resourceStats = resourceStats;
    super.setPath(path);
  }



  /**
   * Process a GET operation.
   *
   * @param requestContext The request context.
   * @param mediaType      The media type to be produced.
   * @param userID         The user ID requested.
   *
   * @return  The response to the operation.
   */
  Response getUser(final RequestContext requestContext,
                           final MediaType mediaType,
                           final String userID)
  {
    Response.ResponseBuilder responseBuilder;
    try {
      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Process the request.
      final GetResourceRequest getResourceRequest =
          new GetResourceRequest(requestContext.getUriInfo().getBaseUri(),
              requestContext.getAuthID(),
              resourceDescriptor,
              userID,
              queryAttributes);

      BaseResource resource =
          backend.getResource(getResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, mediaType, resource);
      URI location = resource.getMeta().getLocation();
      if(location != null)
      {
        responseBuilder.location(location);
      }
      resourceStats.incrementStat(ResourceStats.GET_OK);
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, mediaType, e);
      resourceStats.incrementStat("get-" + e.getStatusCode());
    }

    if (requestContext.getOrigin() != null)
    {
      responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN,
          requestContext.getOrigin());
    }
    responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS,
        Boolean.TRUE.toString());

    if(mediaType == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.GET_RESPONSE_JSON);
    }
    else if(mediaType == MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.GET_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a GET operation.
   *
   * @param requestContext   The request context.
   * @param mediaType        The media type to be produced.
   * @param filterString     The filter query parameter, or {@code null}.
   * @param sortBy           The sortBy query parameter, or {@code null}.
   * @param sortOrder        The sortOrder query parameter, or {@code null}.
   * @param pageStartIndex   The startIndex query parameter, or {@code null}.
   * @param pageSize         The count query parameter, or {@code null}.
   *
   * @return  The response to the operation.
   */
  protected Response getUsers(final RequestContext requestContext,
                              final MediaType mediaType,
                              final String filterString,
                              final String sortBy,
                              final String sortOrder,
                              final String pageStartIndex,
                              final String pageSize)
  {
    Response.ResponseBuilder responseBuilder;
    try
    {
      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Parse the filter parameters.
      final SCIMFilter filter;
      if (filterString != null && !filterString.isEmpty())
      {
        filter = SCIMFilter.parse(filterString);
      }
      else
      {
        filter = null;
      }

      // Parse the sort parameters.
      final SortParameters sortParameters;
      if (sortBy != null && !sortBy.isEmpty())
      {
        sortParameters =
            new SortParameters(AttributePath.parse(sortBy), sortOrder);
      }
      else
      {
        sortParameters = null;
      }

      // Parse the pagination parameters.
      long startIndex = -1;
      int count = -1;
      if (pageStartIndex != null && !pageStartIndex.isEmpty())
      {
        try
        {
          startIndex = Long.parseLong(pageStartIndex);
        }
        catch (NumberFormatException e)
        {
          Debug.debugException(e);
          throw SCIMException.createException(
              400, "The pagination startIndex value '" + pageStartIndex +
              "' is not parsable");
        }

        if (startIndex <= 0)
        {
          throw SCIMException.createException(
              400, "The pagination startIndex value '" + pageStartIndex +
              "' is invalid because it is not greater than zero");
        }
      }
      if (pageSize != null && !pageSize.isEmpty())
      {
        try
        {
          count = Integer.parseInt(pageSize);
        }
        catch (NumberFormatException e)
        {
          Debug.debugException(e);
          throw SCIMException.createException(
              400, "The pagination count value '" + pageSize +
              "' is not parsable");
        }

        if (count <= 0)
        {
          throw SCIMException.createException(
              400, "The pagination count value '" + pageSize +
              "' is invalid because it is not greater than zero");
        }
      }

      final PageParameters pageParameters;
      if (startIndex >= 0 && count >= 0)
      {
        pageParameters = new PageParameters(startIndex, count);
      }
      else if (startIndex >= 0)
      {
        pageParameters = new PageParameters(startIndex, 0);
      }
      else if (count >= 0)
      {
        pageParameters = new PageParameters(1, count);
      }
      else
      {
        pageParameters = null;
      }

      // Process the request.
      final GetResourcesRequest getResourcesRequest =
          new GetResourcesRequest(requestContext.getUriInfo().getBaseUri(),
              requestContext.getAuthID(),
              resourceDescriptor,
              filter,
              sortParameters,
              pageParameters,
              queryAttributes);


      final Resources resources = backend.getResources(getResourcesRequest);

      // Build the response.
      responseBuilder =
          Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, mediaType, resources);
      resourceStats.incrementStat(ResourceStats.QUERY_OK);
    }
    catch(SCIMException e)
    {
      responseBuilder =
          Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, mediaType, e);
      resourceStats.incrementStat("query-" + e.getStatusCode());
    }

    if (requestContext.getOrigin() != null)
    {
      responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN,
          requestContext.getOrigin());
    }
    responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS,
        Boolean.TRUE.toString());

    if(mediaType == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.QUERY_RESPONSE_JSON);
    }
    else if(mediaType == MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.QUERY_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a POST operation.
   *
   * @param requestContext    The request context.
   * @param consumeMediaType  The media type to be consumed.
   * @param produceMediaType  The media type to be produced.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response postUser(final RequestContext requestContext,
                            final MediaType consumeMediaType,
                            final MediaType produceMediaType,
                            final InputStream inputStream)
  {
    final com.unboundid.scim.marshal.Context marshalContext =
        com.unboundid.scim.marshal.Context.instance();

    final Unmarshaller unmarshaller;
    if (consumeMediaType.equals(MediaType.APPLICATION_JSON_TYPE))
    {
      unmarshaller = marshalContext.unmarshaller(
          com.unboundid.scim.marshal.Context.Format.Json);
      resourceStats.incrementStat(ResourceStats.POST_CONTENT_JSON);
    }
    else
    {
      unmarshaller = marshalContext.unmarshaller(
          com.unboundid.scim.marshal.Context.Format.Xml);
      resourceStats.incrementStat(ResourceStats.POST_CONTENT_XML);
    }

    Response.ResponseBuilder responseBuilder;
    try
    {
      // Parse the resource.
      final BaseResource postedResource = unmarshaller.unmarshal(
          inputStream, resourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY);

      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Process the request.
      final PostResourceRequest postResourceRequest =
          new PostResourceRequest(requestContext.getUriInfo().getBaseUri(),
              requestContext.getAuthID(),
              resourceDescriptor,
              postedResource.getScimObject(),
              queryAttributes);

      final BaseResource resource =
          backend.postResource(postResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.CREATED);
      setResponseEntity(responseBuilder, produceMediaType,
          resource);
      responseBuilder.location(resource.getMeta().getLocation());
      resourceStats.incrementStat(ResourceStats.POST_OK);
    } catch (SCIMException e) {
      Debug.debugException(e);
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, produceMediaType, e);
      resourceStats.incrementStat("post-" + e.getStatusCode());
    }

    if(produceMediaType == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.POST_RESPONSE_JSON);
    }
    else if(produceMediaType == MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.POST_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a PUT operation.
   *
   * @param requestContext    The request context.
   * @param consumeMediaType  The media type to be consumed.
   * @param produceMediaType  The media type to be produced.
   * @param userID            The target user ID.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response putUser(final RequestContext requestContext,
                           final MediaType consumeMediaType,
                           final MediaType produceMediaType,
                           final String userID,
                           final InputStream inputStream)
  {
    final com.unboundid.scim.marshal.Context marshalContext =
        com.unboundid.scim.marshal.Context.instance();

    final Unmarshaller unmarshaller;
    if (consumeMediaType.equals(MediaType.APPLICATION_JSON_TYPE))
    {
      unmarshaller = marshalContext.unmarshaller(
          com.unboundid.scim.marshal.Context.Format.Json);
      resourceStats.incrementStat(ResourceStats.PUT_CONTENT_JSON);
    }
    else
    {
      unmarshaller = marshalContext.unmarshaller(
          com.unboundid.scim.marshal.Context.Format.Xml);
      resourceStats.incrementStat(ResourceStats.PUT_CONTENT_XML);
    }

    Response.ResponseBuilder responseBuilder;
    try {
      // Parse the resource.
      final BaseResource puttedResource = unmarshaller.unmarshal(
          inputStream, resourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY);

      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Process the request.
      final PutResourceRequest putResourceRequest =
          new PutResourceRequest(requestContext.getUriInfo().getBaseUri(),
              requestContext.getAuthID(),
              resourceDescriptor,
              userID, puttedResource.getScimObject(),
              queryAttributes);


      final BaseResource scimResponse = backend.putResource(putResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, produceMediaType, scimResponse);
      responseBuilder.location(scimResponse.getMeta().getLocation());
      resourceStats.incrementStat(ResourceStats.PUT_OK);
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, produceMediaType, e);
      resourceStats.incrementStat("put-" + e.getStatusCode());
    }

    if(produceMediaType == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.PUT_RESPONSE_JSON);
    }
    else if(produceMediaType == MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.PUT_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a DELETE operation.
   *
   * @param requestContext    The request context.
   * @param mediaType  The media type to be produced.
   * @param userID     The target user ID.
   *
   * @return  The response to the operation.
   */
  Response deleteUser(final RequestContext requestContext,
                              final MediaType mediaType, final String userID)
  {
    // Process the request.
    final DeleteResourceRequest deleteResourceRequest =
        new DeleteResourceRequest(requestContext.getUriInfo().getBaseUri(),
            requestContext.getAuthID(),
            resourceDescriptor, userID);
    Response.ResponseBuilder responseBuilder;
    try {
      backend.deleteResource(deleteResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      resourceStats.incrementStat(ResourceStats.DELETE_OK);
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, mediaType, e);
      resourceStats.incrementStat("delete-" + e.getStatusCode());
    }

    return responseBuilder.build();
  }



  /**
   * Sets the response entity (content) for a SCIM response.
   *
   * @param builder       A JAX-RS response builder.
   * @param mediaType     The media type to be returned.
   * @param scimResponse  The SCIM response to be returned.
   */
  private void setResponseEntity(final Response.ResponseBuilder builder,
                                 final MediaType mediaType,
                                 final SCIMResponse scimResponse)
  {
    final com.unboundid.scim.marshal.Context marshalContext =
        com.unboundid.scim.marshal.Context.instance();
    final Marshaller marshaller;
    builder.type(mediaType);
    if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE))
    {
      marshaller = marshalContext.marshaller(
          com.unboundid.scim.marshal.Context.Format.Json);
    }
    else
    {
      marshaller = marshalContext.marshaller(
          com.unboundid.scim.marshal.Context.Format.Xml);
    }

    final StreamingOutput output = new StreamingOutput()
    {
      public void write(final OutputStream outputStream)
          throws IOException, WebApplicationException
      {
        try
        {
          scimResponse.marshal(marshaller, outputStream);
        }
        catch (Exception e)
        {
          Debug.debugException(e);
          throw new WebApplicationException(
              e, Response.Status.INTERNAL_SERVER_ERROR);
        }
      }
    };
    builder.entity(output);
  }
}
