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

package com.unboundid.scim.wink;

import com.unboundid.scim.sdk.OAuthTokenHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import java.io.InputStream;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class is a Wink resource implementation for query operations
 * on a SCIM resource where the client requests JSON response format in the URL
 * by appending ".json" on to the endpoint.
 */
@Path("{endpoint}.json")
public class JSONQueryResource extends AbstractSCIMResource
{
  /**
   * Create a new SCIM wink resource for XML query operations on a SCIM
   * endpoint.
   *
   * @param application         The SCIM JAX-RS application associated with this
   *                            resource.
   * @param tokenHandler        The token handler to use for OAuth
   *                            authentication.
   */
  public JSONQueryResource(final SCIMApplication application,
                           final OAuthTokenHandler tokenHandler)
  {
    super(application, tokenHandler);
  }



  /**
   * Implement the GET operation producing JSON format.
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
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonGet(@PathParam("endpoint") final String endpoint,
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
                            @QueryParam(QUERY_PARAMETER_SORT_BY_LC)
                            final String sortBy,
                            @QueryParam(QUERY_PARAMETER_SORT_ORDER_LC)
                            final String sortOrder,
                            @QueryParam(QUERY_PARAMETER_PAGE_START_INDEX_LC)
                            final String pageStartIndex,
                            @QueryParam(QUERY_PARAMETER_PAGE_SIZE)
                            final String pageSize)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);

    return getUsers(requestContext, endpoint, filterString, baseID, searchScope,
                    sortBy, sortOrder, pageStartIndex, pageSize);
  }



  /**
   * Implement the POST operation consuming and producing JSON format.
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
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonDotJsonPost(final InputStream inputStream,
                                    @PathParam("endpoint") final String
                                        endpoint,
                                    @Context final HttpServletRequest request,
                                    @Context final SecurityContext
                                        securityContext,
                                    @Context final HttpHeaders headers,
                                    @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return postUser(requestContext, endpoint, inputStream);
  }



  /**
   * Implement the POST operation consuming XML and producing JSON format.
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
  @Produces(MediaType.APPLICATION_JSON)
  public Response doXmlDotJsonPost(final InputStream inputStream,
                                    @PathParam("endpoint") final String
                                        endpoint,
                                    @Context final HttpServletRequest request,
                                    @Context final SecurityContext
                                        securityContext,
                                    @Context final HttpHeaders headers,
                                    @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return postUser(requestContext, endpoint, inputStream);
  }
}
