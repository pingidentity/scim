/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri.wink;

import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMBackend;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_FILTER;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_PAGE_SIZE;
import static com.unboundid.scim.sdk.SCIMConstants.
    QUERY_PARAMETER_PAGE_START_INDEX;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_SORT_BY;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_SORT_ORDER;



/**
 * This class is a Wink dynamic resource implementation for query operations
 * on a SCIM resource. The set of supported resources and their endpoints
 * are not known until run-time hence it must be implemented as a dynamic
 * resource.
 */
public class QueryResource extends AbstractSCIMResource
{
  /**
   * Create a new SCIM wink resource for query operations on a SCIM endpoint.
   *
   * @param resourceDescriptor  The resource descriptor to use.
   * @param backend             The SCIMBackend to use to process requests.
   */
  public QueryResource(final ResourceDescriptor resourceDescriptor,
                       final SCIMBackend backend)
  {
    super(resourceDescriptor.getQueryEndpoint(), resourceDescriptor, backend);
  }



  /**
   * Implement the GET operation producing JSON format.
   *
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
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonGet(@Context final ServletContext servletContext,
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

    return getUsers(requestContext, MediaType.APPLICATION_JSON_TYPE,
                    filterString, sortBy, sortOrder, pageStartIndex, pageSize);
  }



  /**
   * Implement the GET operation producing XML format.
   *
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
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlGet(@Context final ServletContext servletContext,
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

    return getUsers(requestContext, MediaType.APPLICATION_XML_TYPE,
                    filterString, sortBy, sortOrder, pageStartIndex, pageSize);
  }
}
