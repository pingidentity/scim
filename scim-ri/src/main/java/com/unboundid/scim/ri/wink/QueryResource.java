/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri.wink;

import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMResponse;
import com.unboundid.scim.ri.SCIMServer;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.sdk.SCIMAttributeType;
import org.apache.wink.common.AbstractDynamicResource;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.io.OutputStream;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class is a Wink dynamic resource implementation for query operations
 * on a SCIM resource. The set of supported resources and their endpoints
 * are not known until run-time hence it must be implemented as a dynamic
 * resource.
 */
public class QueryResource extends AbstractDynamicResource
{
  /**
   * The REST endpoint for querying the resource. e.g. Users
   */
  private final String resourceEndpoint;



  @Override
  public String getPath()
  {
    // The path is the resource endpoint followed by an optional media format
    // specifier (.json or .xml).
    return resourceEndpoint + "{format:(\\.[^/]*?)?}";
  }



  /**
   * Create a new dynamic resource for query operations on a SCIM resource.
   *
   * @param resourceEndpoint  The REST endpoint for querying the resource.
   */
  public QueryResource(final String resourceEndpoint)
  {
    this.resourceEndpoint = resourceEndpoint;
  }



  /**
   * Implement the GET operation producing XML or JSON format.
   *
   * @param format           The response format specified in the URI.
   * @param servletContext   The servlet context of the current request.
   * @param securityContext  The security context of the current request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   * @param filterString     The filter query parameter, or {@code null}.
   * @param sortBy           The sortBy query parameter, or {@code null}.
   * @param sortOrder        The sortOrder query parameter, or {@code null}.
   * @param pageStartIndex   The startIndex query parameter, or {@code null}.
   * @param pageSize         The count query parameter, or {@code null}.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response doGet(@PathParam("format") final String format,
                        @Context final ServletContext servletContext,
                        @Context final SecurityContext securityContext,
                        @Context final HttpHeaders headers,
                        @Context final UriInfo uriInfo,
                        @QueryParam(QUERY_PARAMETER_FILTER)
                        final String filterString,
                        @QueryParam(QUERY_PARAMETER_SORT_BY)
                        final String sortBy,
                        @QueryParam(QUERY_PARAMETER_SORT_ORDER)
                        final String sortOrder,
                        @QueryParam(QUERY_PARAMETER_PAGE_START_INDEX)
                        final String pageStartIndex,
                        @QueryParam(QUERY_PARAMETER_PAGE_SIZE)
                        final String pageSize)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);

    final MediaType mediaType;
    if (format == null)
    {
      mediaType = MediaType.APPLICATION_JSON_TYPE;
    }
    else if (format.equalsIgnoreCase(".xml"))
    {
      mediaType = MediaType.APPLICATION_XML_TYPE;
    }
    else
    {
      mediaType = MediaType.APPLICATION_JSON_TYPE;
    }

    return getUsers(requestContext, mediaType, filterString, sortBy,
                    sortOrder, pageStartIndex, pageSize);
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
  private Response getUsers(final RequestContext requestContext,
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
                                resourceEndpoint,
                                filter,
                                sortParameters,
                                pageParameters,
                                requestContext.getQueryAttributes());
    final SCIMResponse scimResponse = backend.getResources(getResourcesRequest);

    // Build the response.
    final Response.ResponseBuilder responseBuilder =
        Response.status(scimResponse.getStatusCode());
    setResponseEntity(responseBuilder, mediaType, scimResponse);

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
          marshaller.marshal(scimResponse.getResponse(), outputStream);
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
