/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri.wink;

import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.ri.SCIMServer;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMResponse;
import com.unboundid.scim.sdk.SortParameters;
import org.apache.wink.common.AbstractDynamicResource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;

import static com.unboundid.scim.sdk.SCIMConstants.
    HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.unboundid.scim.sdk.SCIMConstants.
    HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN;



/**
 * This class is a base class of Wink dynamic resource implementations for
 * query operations on a SCIM resource. The set of supported resources and
 * their endpoints are not known until run-time hence they must be implemented
 * as dynamic resources.
 */
public class QueryResourceBase extends AbstractDynamicResource
{
  /**
   * The ResourceDescriptor associated with this resource.
   */
  private final ResourceDescriptor resourceDescriptor;



  @Override
  public String getPath()
  {
    return resourceDescriptor.getQueryEndpoint();
  }



  /**
   * Create a new dynamic resource for query operations on a SCIM resource.
   *
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             resource.
   */
  public QueryResourceBase(final ResourceDescriptor resourceDescriptor)
  {
    this.resourceDescriptor = resourceDescriptor;
  }



  /**
   * Lookup the backend that should process a given request.
   *
   * @param requestContext  The request context.
   *
   * @return  The backend that should process the request.
   */
  private SCIMBackend lookupBackend(final RequestContext requestContext)
  {
    final SCIMServer scimServer = SCIMServer.getInstance();
    final String baseURI = requestContext.getServletContext().getContextPath();

    final SCIMBackend backend = scimServer.getBackend(baseURI);
    if (backend == null)
    {
      throw new RuntimeException("Base URI is not valid: " + baseURI);
    }

    return backend;
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
    // Get the SCIM backend to process the request.
    final SCIMBackend backend = lookupBackend(requestContext);

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
          new SortParameters(
              SCIMAttributeType.fromQualifiedName(sortBy),
              sortOrder);
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
      startIndex = Long.parseLong(pageStartIndex);
    }
    if (pageSize != null && !pageSize.isEmpty())
    {
      count = Integer.parseInt(pageSize);
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
      pageParameters = new PageParameters(0, count);
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
                                requestContext.getQueryAttributes());
    Response.ResponseBuilder responseBuilder;
    try
    {
      final Resources resources = backend.getResources(getResourcesRequest);

      // Build the response.
      responseBuilder =
          Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, mediaType, resources);
    }
    catch(SCIMException e)
    {
      responseBuilder =
          Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, mediaType, e);
    }

    if (requestContext.getOrigin() != null)
    {
      responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN,
                             requestContext.getOrigin());
    }
    responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS,
                           Boolean.TRUE.toString());

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
