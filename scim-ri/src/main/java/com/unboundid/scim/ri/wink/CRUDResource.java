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

package com.unboundid.scim.ri.wink;

import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMBackend;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;



/**
 * This class is a Wink dynamic resource implementation for CRUD operations
 * on a SCIM resource. The set of supported resources and their endpoints
 * are not known until run-time hence it must be implemented as a dynamic
 * resource.
 */
public class CRUDResource extends AbstractSCIMResource
{
  /**
   * Create a new SCIM wink resource for CRUD operations on a SCIM endpoint.
   *
   * @param resourceDescriptor  The resource descriptor to use.
   * @param backend             The SCIMBackend to use to process requests.
   */
  public CRUDResource(final ResourceDescriptor resourceDescriptor,
                      final SCIMBackend backend)
  {
    super(resourceDescriptor.getName(), resourceDescriptor, backend);
  }



  /**
   * Implement the GET operation on a specified user resource producing
   * JSON format.
   *
   * @param userID           The requested user ID.
   * @param servletContext   The servlet context for the request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @GET
  @Path("{userID}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonGet(@PathParam("userID") final String userID,
                            @Context final ServletContext servletContext,
                            @Context final SecurityContext securityContext,
                            @Context final HttpHeaders headers,
                            @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return getUser(requestContext, MediaType.APPLICATION_JSON_TYPE, userID);
  }



  /**
   * Implement the GET operation on a specified user resource producing
   * XML format.
   *
   * @param userID           The requested user ID.
   * @param servletContext   The servlet context for the request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @GET
  @Path("{userID}")
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlGet(@PathParam("userID") final String userID,
                           @Context final ServletContext servletContext,
                           @Context final SecurityContext securityContext,
                           @Context final HttpHeaders headers,
                           @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return getUser(requestContext, MediaType.APPLICATION_XML_TYPE, userID);
  }



  /**
   * Implement the GET operation on a specified user resource where the URL
   * specifies JSON content type.
   *
   * @param userID           The requested user ID.
   * @param servletContext   The servlet context for the request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @GET
  @Path("{userID}.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response doDotJsonGet(@PathParam("userID") final String userID,
                               @Context final ServletContext servletContext,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return getUser(requestContext, MediaType.APPLICATION_JSON_TYPE, userID);
  }



  /**
   * Implement the GET operation on a specified user resource where the URL
   * specifies XML content type.
   *
   * @param userID           The requested user ID.
   * @param servletContext   The servlet context for the request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @GET
  @Path("{userID}.xml")
  @Produces(MediaType.APPLICATION_XML)
  public Response doDotXmlGet(@PathParam("userID") final String userID,
                              @Context final ServletContext servletContext,
                              @Context final SecurityContext securityContext,
                              @Context final HttpHeaders headers,
                              @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return getUser(requestContext, MediaType.APPLICATION_XML_TYPE, userID);
  }



  /**
   * Implement the POST operation consuming and producing JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param servletContext   The servlet context for the request.
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
                                 @Context final ServletContext servletContext,
                                 @Context final SecurityContext securityContext,
                                 @Context final HttpHeaders headers,
                                 @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return postUser(requestContext,
                    MediaType.APPLICATION_JSON_TYPE,
                    MediaType.APPLICATION_JSON_TYPE,
                    inputStream);
  }



  /**
   * Implement the POST operation consuming and producing XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param servletContext   The servlet context for the request.
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
                               @Context final ServletContext servletContext,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return postUser(requestContext,
                    MediaType.APPLICATION_XML_TYPE,
                    MediaType.APPLICATION_XML_TYPE,
                    inputStream);
  }



  /**
   * Implement the POST operation consuming XML format and producing JSON
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param servletContext   The servlet context for the request.
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
                                @Context final ServletContext servletContext,
                                @Context final SecurityContext securityContext,
                                @Context final HttpHeaders headers,
                                @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return postUser(requestContext,
                    MediaType.APPLICATION_XML_TYPE,
                    MediaType.APPLICATION_JSON_TYPE,
                    inputStream);
  }



  /**
   * Implement the POST operation consuming JSON format and producing XML
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param servletContext   The servlet context for the request.
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
                                @Context final ServletContext servletContext,
                                @Context final SecurityContext securityContext,
                                @Context final HttpHeaders headers,
                                @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return postUser(requestContext,
        MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_XML_TYPE,
        inputStream);
  }



  /**
   * Implement the PUT operation consuming and producing JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
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
                                @PathParam("userID") final String userID,
                                @Context final ServletContext servletContext,
                                @Context final SecurityContext securityContext,
                                @Context final HttpHeaders headers,
                                @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return putUser(requestContext,
                   MediaType.APPLICATION_JSON_TYPE,
                   MediaType.APPLICATION_JSON_TYPE,
                   userID,
                   inputStream);
  }



  /**
   * Implement the PUT operation where the URL specifies JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
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
  public Response doDotJsonPut(final InputStream inputStream,
                               @PathParam("userID") final String userID,
                               @Context final ServletContext servletContext,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return putUser(requestContext,
                   MediaType.APPLICATION_JSON_TYPE,
                   MediaType.APPLICATION_JSON_TYPE,
                   userID,
                   inputStream);
  }



  /**
   * Implement the PUT operation where the URL specifies XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
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
  public Response doDotXmlPut(final InputStream inputStream,
                              @PathParam("userID") final String userID,
                              @Context final ServletContext servletContext,
                              @Context final SecurityContext securityContext,
                              @Context final HttpHeaders headers,
                              @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return putUser(requestContext,
                   MediaType.APPLICATION_XML_TYPE,
                   MediaType.APPLICATION_XML_TYPE,
                   userID,
                   inputStream);
  }



  /**
   * Implement the PUT operation consuming and producing XML format.
   *
   * @param inputStream      The content to be consumed.
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
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
                              @PathParam("userID") final String userID,
                              @Context final ServletContext servletContext,
                              @Context final SecurityContext securityContext,
                              @Context final HttpHeaders headers,
                              @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return putUser(requestContext,
                   MediaType.APPLICATION_XML_TYPE,
                   MediaType.APPLICATION_XML_TYPE,
                   userID,
                   inputStream);
  }



  /**
   * Implement the PUT operation consuming XML format and producing JSON
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
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
                               @PathParam("userID") final String userID,
                               @Context final ServletContext servletContext,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return putUser(requestContext,
                   MediaType.APPLICATION_XML_TYPE,
                   MediaType.APPLICATION_JSON_TYPE,
                   userID,
                   inputStream);
  }



  /**
   * Implement the PUT operation consuming JSON format and producing XML
   * format.
   *
   * @param inputStream      The content to be consumed.
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
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
                               @PathParam("userID") final String userID,
                               @Context final ServletContext servletContext,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return putUser(requestContext,
        MediaType.APPLICATION_JSON_TYPE,
        MediaType.APPLICATION_XML_TYPE,
        userID,
        inputStream);
  }



  /**
   * Implement the DELETE operation on a specified user resource producing
   * JSON format.
   *
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @DELETE
  @Path("{userID}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonDelete(@PathParam("userID") final String userID,
                               @Context final ServletContext servletContext,
                               @Context final SecurityContext securityContext,
                               @Context final HttpHeaders headers,
                               @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return deleteUser(requestContext, MediaType.APPLICATION_JSON_TYPE, userID);
  }



  /**
   * Implement the DELETE operation on a specified user resource producing
   * XML format.
   *
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @DELETE
  @Path("{userID}")
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlDelete(@PathParam("userID") final String userID,
                              @Context final ServletContext servletContext,
                              @Context final SecurityContext securityContext,
                              @Context final HttpHeaders headers,
                              @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return deleteUser(requestContext, MediaType.APPLICATION_XML_TYPE, userID);
  }



  /**
   * Implement the DELETE operation on a specified user resource where the URL
   * specifies JSON content type.
   *
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @DELETE
  @Path("{userID}.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response doDotJsonDelete(@PathParam("userID") final String userID,
                                  @Context
                                  final ServletContext servletContext,
                                  @Context
                                  final SecurityContext securityContext,
                                  @Context final HttpHeaders headers,
                                  @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return deleteUser(requestContext, MediaType.APPLICATION_JSON_TYPE, userID);
  }



  /**
   * Implement the DELETE operation on a specified user resource where the URL
   * specifies XML content type.
   *
   * @param userID           The target user ID.
   * @param servletContext   The servlet context for the request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @DELETE
  @Path("{userID}.xml")
  @Produces(MediaType.APPLICATION_XML)
  public Response doDotXmlDelete(@PathParam("userID") final String userID,
                                 @Context final ServletContext servletContext,
                                 @Context final SecurityContext securityContext,
                                 @Context final HttpHeaders headers,
                                 @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(servletContext, securityContext, headers, uriInfo);
    return deleteUser(requestContext, MediaType.APPLICATION_XML_TYPE, userID);
  }
}
