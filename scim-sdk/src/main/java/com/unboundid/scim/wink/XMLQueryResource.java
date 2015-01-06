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

package com.unboundid.scim.wink;

import com.unboundid.scim.sdk.OAuthTokenHandler;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import java.io.InputStream;

import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_BASE_ID;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_FILTER;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_PAGE_SIZE;
import static com.unboundid.scim.sdk.SCIMConstants.
    QUERY_PARAMETER_PAGE_START_INDEX;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_SCOPE;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_SORT_BY;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_SORT_ORDER;



/**
 * This class is a Wink dynamic resource implementation for query operations
 * on a SCIM resource where the client requests XML response format in the URL
 * by appending ".xml" on to the endpoint.
 */
@Path("{endpoint}.xml")
public class XMLQueryResource extends AbstractSCIMResource
{
  /**
   * Create a new SCIM wink resource for XML query operations on a
   * SCIM endpoint.
   *
   * @param application         The SCIM JAX-RS application associated with this
   *                            resource.
   * @param tokenHandler        The token handler to use for OAuth
   *                            authentication.
   */
  public XMLQueryResource(final SCIMApplication application,
                          final OAuthTokenHandler tokenHandler)
  {
    super(application, tokenHandler);
  }



  /**
   * Implement the GET operation producing XML format.
   *
   * @param endpoint         The resource endpoint.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context of the current request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   * @param filterString     The filter query parameter, or {@code null}.
   * @param baseID           The SCIM resource ID of the search base entry,
   *                         or {@code null}.
   * @param searchScope      The LDAP search scope to use, or {@code null}.
   * @param sortBy           The sortBy query parameter, or {@code null}.
   * @param sortOrder        The sortOrder query parameter, or {@code null}.
   * @param pageStartIndex   The startIndex query parameter, or {@code null}.
   * @param pageSize         The count query parameter, or {@code null}.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlGet(@PathParam("endpoint") final String endpoint,
                           @Context final HttpServletRequest request,
                           @Context final SecurityContext securityContext,
                           @Context final HttpHeaders headers,
                           @Context final UriInfo uriInfo,
                           @QueryParam(QUERY_PARAMETER_FILTER)
                           final String filterString,
                           @QueryParam(QUERY_PARAMETER_BASE_ID)
                           final String baseID,
                           @QueryParam(QUERY_PARAMETER_SCOPE)
                           final String searchScope,
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
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_XML_TYPE);

    return getUsers(requestContext, endpoint, filterString, baseID, searchScope,
                    sortBy, sortOrder, pageStartIndex, pageSize);
  }



  /**
   * Implement the POST operation consuming and producing XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @POST
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlDotXmlPost(final InputStream inputStream,
                                  @PathParam("endpoint") final String endpoint,
                                  @Context final HttpServletRequest request,
                                  @Context final SecurityContext
                                      securityContext,
                                  @Context final HttpHeaders headers,
                                  @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return postUser(requestContext, endpoint, inputStream);
  }



  /**
   * Implement the POST operation consuming JSON and producing XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_XML)
  public Response doJsonDotXmlPost(final InputStream inputStream,
                                   @PathParam("endpoint") final String endpoint,
                                   @Context final HttpServletRequest request,
                                   @Context final SecurityContext
                                       securityContext,
                                   @Context final HttpHeaders headers,
                                   @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return postUser(requestContext, endpoint, inputStream);
  }
}
