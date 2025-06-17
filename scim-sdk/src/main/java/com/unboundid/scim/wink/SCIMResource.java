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
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Variant;
import java.io.InputStream;
import java.util.List;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class is a Wink resource implementation for operations
 * on a SCIM resource.
 */
@Path("{endpoint}")
public class SCIMResource extends AbstractSCIMResource
{
  /**
   * Create a new SCIM wink resource for operations on a SCIM endpoint.
   *
   * @param application         The SCIMApplication initializing this reosurce.
   * @param tokenHandler        The token handler to use for OAuth
   *                            authentication.
   */
  public SCIMResource(final SCIMApplication application,
                      final OAuthTokenHandler tokenHandler)
  {
    super(application, tokenHandler);
  }



  /**
   * Implement the GET query operation producing JSON or XML format.
   *
   * @param endpoint         The resource endpoint.
   * @param request          The current request.
   * @param httpRequest      The current HTTP servlet request.
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
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response doJsonGet(@PathParam("endpoint") final String endpoint,
                            @Context final Request request,
                            @Context final HttpServletRequest httpRequest,
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
    // We want to reliably produce JSON when the Accept header doesn't indicate
    // a preference between JSON and XML. We can't get that behavior from Jersey
    // using one method which @Produces JSON and another which @Produces XML.
    final List<Variant> responseVariants = Variant
            .mediaTypes(
                MediaType.valueOf(MediaType.APPLICATION_JSON),
                MediaType.valueOf(MediaType.APPLICATION_XML))
            .add().build();
    final Variant variant = request.selectVariant(responseVariants);
    final MediaType mediaType = (variant != null) ?
                                variant.getMediaType() :
                                MediaType.APPLICATION_JSON_TYPE;

    final RequestContext requestContext =
        new RequestContext(httpRequest, securityContext, headers, uriInfo,
                           mediaType,
                           mediaType);

    return getUsers(requestContext, endpoint, filterString, baseID, searchScope,
                    sortBy, sortOrder, pageStartIndex, pageSize);
  }



  /**
   * Implement the GET operation on a specified user resource producing
   * JSON or XML format.
   *
   * @param endpoint         The resource endpoint.
   * @param userID           The requested user ID.
   * @param request          The current request.
   * @param httpRequest      The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @GET
  @Path("{userID}")
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response doJsonGet(@PathParam("endpoint") final String endpoint,
                            @PathParam("userID") final String userID,
                            @Context final Request request,
                            @Context final HttpServletRequest httpRequest,
                            @Context final SecurityContext securityContext,
                            @Context final HttpHeaders headers,
                            @Context final UriInfo uriInfo)
  {
    // We want to reliably produce JSON when the Accept header doesn't indicate
    // a preference between JSON and XML. We can't get that behavior from Jersey
    // using one method which @Produces JSON and another which @Produces XML.
    final List<Variant> responseVariants = Variant
            .mediaTypes(
                MediaType.valueOf(MediaType.APPLICATION_JSON),
                MediaType.valueOf(MediaType.APPLICATION_XML))
            .add().build();
    final Variant variant = request.selectVariant(responseVariants);
    final MediaType mediaType = (variant != null) ?
                                variant.getMediaType() :
                                MediaType.APPLICATION_JSON_TYPE;

    final RequestContext requestContext =
        new RequestContext(httpRequest, securityContext, headers, uriInfo,
                           mediaType,
                           mediaType);

    return getUser(requestContext, endpoint, userID);
  }



  /**
   * Implement the GET operation on a specified user resource where the URL
   * specifies JSON content type.
   *
   * @param endpoint         The resource endpoint.
   * @param userID           The requested user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @GET
  @Path("{userID}.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response doDotJsonGet(@PathParam("endpoint") final String endpoint,
                               @PathParam("userID") final String userID,
                               @Context final HttpServletRequest request,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return getUser(requestContext, endpoint, userID);
  }



  /**
   * Implement the GET operation on a specified user resource where the URL
   * specifies XML content type.
   *
   * @param endpoint         The resource endpoint.
   * @param userID           The requested user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @GET
  @Path("{userID}.xml")
  @Produces(MediaType.APPLICATION_XML)
  public Response doDotXmlGet(@PathParam("endpoint") final String endpoint,
                              @PathParam("userID") final String userID,
                              @Context final HttpServletRequest request,
                              @Context final SecurityContext securityContext,
                              @Context final HttpHeaders headers,
                              @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return getUser(requestContext, endpoint, userID);
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
  public Response doJsonJsonPost(final InputStream inputStream,
                                 @PathParam("endpoint") final String endpoint,
                                 @Context final HttpServletRequest request,
                                 @Context final SecurityContext securityContext,
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
  public Response doXmlXmlPost(final InputStream inputStream,
                               @PathParam("endpoint") final String endpoint,
                               @Context final HttpServletRequest request,
                               @Context final SecurityContext securityContext,
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
   * Implement the POST operation consuming XML format and producing JSON
   * format.
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
  public Response doXmlJsonPost(final InputStream inputStream,
                                @PathParam("endpoint") final String endpoint,
                                @Context final HttpServletRequest request,
                                @Context final SecurityContext securityContext,
                                @Context final HttpHeaders headers,
                                @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return postUser(requestContext, endpoint, inputStream);
  }



  /**
   * Implement the POST operation consuming JSON format and producing XML
   * format.
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
  public Response doJsonXmlPost(final InputStream inputStream,
                                @PathParam("endpoint") final String endpoint,
                                @Context final HttpServletRequest request,
                                @Context final SecurityContext securityContext,
                                @Context final HttpHeaders headers,
                                @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return postUser(requestContext, endpoint, inputStream);
  }


  /**
   * Implement the PUT operation consuming and producing JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PUT
  @Path("{userID}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonJsonPut(final InputStream inputStream,
                                @PathParam("endpoint") final String endpoint,
                                @PathParam("userID") final String userID,
                                @Context final HttpServletRequest request,
                                @Context final SecurityContext securityContext,
                                @Context final HttpHeaders headers,
                                @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return putUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PUT operation where the URL specifies JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PUT
  @Path("{userID}.json")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonDotJsonPut(final InputStream inputStream,
                                   @PathParam("endpoint") final String endpoint,
                                   @PathParam("userID") final String userID,
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
    return putUser(requestContext,
        endpoint,
        userID,
        inputStream);
  }



  /**
   * Implement the PUT operation consuming XML and where the URL specifies
   * producing JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PUT
  @Path("{userID}.json")
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doXmlDotJsonPut(final InputStream inputStream,
                                  @PathParam("endpoint") final String endpoint,
                                  @PathParam("userID") final String userID,
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
    return putUser(requestContext,
        endpoint,
        userID,
        inputStream);
  }



  /**
   * Implement the PUT operation where the URL specifies XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PUT
  @Path("{userID}.xml")
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlDotXmlPut(final InputStream inputStream,
                                 @PathParam("endpoint") final String endpoint,
                                 @PathParam("userID") final String userID,
                                 @Context final HttpServletRequest request,
                                 @Context final SecurityContext securityContext,
                                 @Context final HttpHeaders headers,
                                 @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return putUser(requestContext,
                   endpoint,
                   userID,
                   inputStream);
  }



  /**
   * Implement the PUT operation where the URL specifies XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PUT
  @Path("{userID}.xml")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_XML)
  public Response doJsonDotXmlPut(final InputStream inputStream,
                                  @PathParam("endpoint") final String endpoint,
                                  @PathParam("userID") final String userID,
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
    return putUser(requestContext,
                   endpoint,
                   userID,
                   inputStream);
  }



  /**
   * Implement the PUT operation consuming and producing XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PUT
  @Path("{userID}")
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlXmlPut(final InputStream inputStream,
                              @PathParam("endpoint") final String endpoint,
                              @PathParam("userID") final String userID,
                              @Context final HttpServletRequest request,
                              @Context final SecurityContext securityContext,
                              @Context final HttpHeaders headers,
                              @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return putUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PUT operation consuming XML format and producing JSON
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PUT
  @Path("{userID}")
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doXmlJsonPut(final InputStream inputStream,
                               @PathParam("endpoint") final String endpoint,
                               @PathParam("userID") final String userID,
                               @Context final HttpServletRequest request,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return putUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PUT operation consuming JSON format and producing XML
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PUT
  @Path("{userID}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_XML)
  public Response doJsonXmlPut(final InputStream inputStream,
                               @PathParam("endpoint") final String endpoint,
                               @PathParam("userID") final String userID,
                               @Context final HttpServletRequest request,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return putUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PATCH operation consuming and producing JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PATCH
  @Path("{userID}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonJsonPatch(final InputStream inputStream,
                                 @PathParam("endpoint") final String endpoint,
                                 @PathParam("userID") final String userID,
                                 @Context final HttpServletRequest request,
                                 @Context final SecurityContext securityContext,
                                 @Context final HttpHeaders headers,
                                 @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
           new RequestContext(request, securityContext, headers, uriInfo,
                    MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
    return patchUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PATCH operation consuming and producing XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PATCH
  @Path("{userID}")
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlXmlPatch(final InputStream inputStream,
                               @PathParam("endpoint") final String endpoint,
                               @PathParam("userID") final String userID,
                               @Context final HttpServletRequest request,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
           new RequestContext(request, securityContext, headers, uriInfo,
                    MediaType.APPLICATION_XML_TYPE,
                    MediaType.APPLICATION_XML_TYPE);
    return patchUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PATCH operation consuming XML format and producing JSON
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PATCH
  @Path("{userID}")
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doXmlJsonPatch(final InputStream inputStream,
                                @PathParam("endpoint") final String endpoint,
                                @PathParam("userID") final String userID,
                                @Context final HttpServletRequest request,
                                @Context final SecurityContext securityContext,
                                @Context final HttpHeaders headers,
                                @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
           new RequestContext(request, securityContext, headers, uriInfo,
                    MediaType.APPLICATION_XML_TYPE,
                    MediaType.APPLICATION_JSON_TYPE);
    return patchUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PATCH operation consuming JSON format and producing XML
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PATCH
  @Path("{userID}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_XML)
  public Response doJsonXmlPatch(final InputStream inputStream,
                                @PathParam("endpoint") final String endpoint,
                                @PathParam("userID") final String userID,
                                @Context final HttpServletRequest request,
                                @Context final SecurityContext securityContext,
                                @Context final HttpHeaders headers,
                                @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
           new RequestContext(request, securityContext, headers, uriInfo,
                    MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_XML_TYPE);
    return patchUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PATCH operation consuming and producing JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PATCH
  @Path("{userID}.json")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonDotJsonPatch(final InputStream inputStream,
                                    @PathParam("endpoint") final String
                                        endpoint,
                                    @PathParam("userID") final String userID,
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
    return patchUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PATCH operation consuming and producing XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PATCH
  @Path("{userID}.xml")
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlDotXmlPatch(final InputStream inputStream,
                                  @PathParam("endpoint") final String endpoint,
                                  @PathParam("userID") final String userID,
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
    return patchUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PATCH operation consuming XML format and producing JSON
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PATCH
  @Path("{userID}.json")
  @Consumes(MediaType.APPLICATION_XML)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doXmlDotJsonPatch(final InputStream inputStream,
                                   @PathParam("endpoint") final String endpoint,
                                   @PathParam("userID") final String userID,
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
    return patchUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the PATCH operation consuming JSON format and producing XML
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @PATCH
  @Path("{userID}.xml")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_XML)
  public Response doJsonDotXmlPatch(final InputStream inputStream,
                                   @PathParam("endpoint") final String endpoint,
                                   @PathParam("userID") final String userID,
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
    return patchUser(requestContext, endpoint, userID, inputStream);
  }



  /**
   * Implement the DELETE operation on a specified user resource producing
   * JSON format.
   *
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @DELETE
  @Path("{userID}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonDelete(
                               @PathParam("endpoint") final String endpoint,
                               @PathParam("userID") final String userID,
                               @Context final HttpServletRequest request,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return deleteUser(requestContext,endpoint, userID);
  }



  /**
   * Implement the DELETE operation on a specified user resource producing
   * XML format.
   *
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @DELETE
  @Path("{userID}")
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlDelete(
                              @PathParam("endpoint") final String endpoint,
                              @PathParam("userID") final String userID,
                              @Context final HttpServletRequest request,
                              @Context final SecurityContext securityContext,
                              @Context final HttpHeaders headers,
                              @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return deleteUser(requestContext, endpoint, userID);
  }



  /**
   * Implement the DELETE operation on a specified user resource where the URL
   * specifies JSON content type.
   *
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @DELETE
  @Path("{userID}.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response doDotJsonDelete(@PathParam("endpoint") final String endpoint,
                                  @PathParam("userID") final String userID,
                                  @Context final HttpServletRequest request,
                                  @Context
                                  final SecurityContext securityContext,
                                  @Context final HttpHeaders headers,
                                  @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return deleteUser(requestContext, endpoint, userID);
  }



  /**
   * Implement the DELETE operation on a specified user resource where the URL
   * specifies XML content type.
   *
   * @param endpoint         The resource endpoint.
   * @param userID           The target user ID.
   * @param request          The current HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @DELETE
  @Path("{userID}.xml")
  @Produces(MediaType.APPLICATION_XML)
  public Response doDotXmlDelete(@PathParam("endpoint") final String endpoint,
                                 @PathParam("userID") final String userID,
                                 @Context final HttpServletRequest request,
                                 @Context final SecurityContext securityContext,
                                 @Context final HttpHeaders headers,
                                 @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_XML_TYPE,
                           MediaType.APPLICATION_XML_TYPE);
    return deleteUser(requestContext, endpoint, userID);
  }
}
