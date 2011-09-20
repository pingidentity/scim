/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap.wink;

import com.unboundid.scim.ldap.DeleteResourceRequest;
import com.unboundid.scim.ldap.GetResourceRequest;
import com.unboundid.scim.ldap.PostResourceRequest;
import com.unboundid.scim.ldap.PutResourceRequest;
import com.unboundid.scim.ldap.SCIMBackend;
import com.unboundid.scim.ldap.SCIMResponse;
import com.unboundid.scim.ldap.SCIMServer;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.SCIMObject;
import org.apache.wink.common.AbstractDynamicResource;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class is a Wink dynamic resource implementation for CRUD operations
 * on a SCIM resource. The set of supported resources and their endpoints
 * are not known until run-time hence it must be implemented as a dynamic
 * resource.
 */
public class CRUDResource extends AbstractDynamicResource
{
  /**
   * The name of the resource and the REST endpoint. e.g. User
   */
  private final String resourceName;



  @Override
  public String getPath()
  {
    return resourceName;
  }



  /**
   * Create a new dynamic resource for CRUD operations on a SCIM resource.
   *
   * @param resourceName  The name of the resource and the REST endpoint.
   */
  public CRUDResource(final String resourceName)
  {
    this.resourceName = resourceName;
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
  @Produces(MediaType.APPLICATION_XML)
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
   * @param requestContext The request context.
   * @param mediaType      The media type to be produced.
   * @param userID         The user ID requested.
   *
   * @return  The response to the operation.
   */
  private Response getUser(final RequestContext requestContext,
                           final MediaType mediaType,
                           final String userID)
  {
    // Get the SCIM backend to process the request.
    final SCIMBackend backend = lookupBackend(requestContext);

    // Process the request.
    final GetResourceRequest getResourceRequest =
        new GetResourceRequest(requestContext.getUriInfo().getBaseUri(),
                               requestContext.getAuthID(),
                               resourceName,
                               userID,
                               requestContext.getQueryAttributes());
    final SCIMResponse scimResponse = backend.getResource(getResourceRequest);

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

    final com.unboundid.scim.schema.Resource resource =
        scimResponse.getResponse().getResource();
    if (resource != null)
    {
      final UriBuilder uriBuilder =
          requestContext.getUriInfo().getBaseUriBuilder();
      uriBuilder.path(resourceName);
      uriBuilder.path(resource.getId());
      responseBuilder.location(uriBuilder.build());
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
  private Response postUser(final RequestContext requestContext,
                            final MediaType consumeMediaType,
                            final MediaType produceMediaType,
                            final InputStream inputStream)
  {
    // Get the SCIM backend to process the request.
    final SCIMBackend backend = lookupBackend(requestContext);

    final com.unboundid.scim.marshal.Context marshalContext =
        com.unboundid.scim.marshal.Context.instance();

    // Parse the resource.
    final Unmarshaller unmarshaller;
    if (consumeMediaType.equals(MediaType.APPLICATION_JSON_TYPE))
    {
      unmarshaller = marshalContext.unmarshaller(
          com.unboundid.scim.marshal.Context.Format.Json);
    }
    else
    {
      unmarshaller = marshalContext.unmarshaller(
          com.unboundid.scim.marshal.Context.Format.Xml);
    }

    final SCIMObject requestObject;
    try
    {
      requestObject = unmarshaller.unmarshal(inputStream, resourceName);
    }
    catch (Exception e)
    {
      throw new WebApplicationException(
          e, Response.Status.INTERNAL_SERVER_ERROR);
    }

    // Process the request.
    final PostResourceRequest postResourceRequest =
        new PostResourceRequest(requestContext.getUriInfo().getBaseUri(),
                                requestContext.getAuthID(), resourceName,
                                requestObject,
                                requestContext.getQueryAttributes());
    final SCIMResponse scimResponse =
        backend.postResource(postResourceRequest);

    // Build the response.
    final Response.ResponseBuilder responseBuilder =
        Response.status(scimResponse.getStatusCode());
    setResponseEntity(responseBuilder, produceMediaType, scimResponse);

    final com.unboundid.scim.schema.Resource resource =
        scimResponse.getResponse().getResource();
    if (resource != null)
    {
      final UriBuilder uriBuilder =
          requestContext.getUriInfo().getBaseUriBuilder();
      uriBuilder.path(resourceName);
      uriBuilder.path(resource.getId());
      responseBuilder.location(uriBuilder.build());
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
  private Response putUser(final RequestContext requestContext,
                           final MediaType consumeMediaType,
                           final MediaType produceMediaType,
                           final String userID,
                           final InputStream inputStream)
  {
    // Get the SCIM backend to process the request.
    final SCIMBackend backend = lookupBackend(requestContext);

    final com.unboundid.scim.marshal.Context marshalContext =
        com.unboundid.scim.marshal.Context.instance();

    // Parse the resource.
    final Unmarshaller unmarshaller;
    if (consumeMediaType.equals(MediaType.APPLICATION_JSON_TYPE))
    {
      unmarshaller = marshalContext.unmarshaller(
          com.unboundid.scim.marshal.Context.Format.Json);
    }
    else
    {
      unmarshaller = marshalContext.unmarshaller(
          com.unboundid.scim.marshal.Context.Format.Xml);
    }

    final SCIMObject requestObject;
    try
    {
      requestObject = unmarshaller.unmarshal(inputStream, resourceName);
    }
    catch (Exception e)
    {
      throw new WebApplicationException(
          e, Response.Status.INTERNAL_SERVER_ERROR);
    }

    // Process the request.
    final PutResourceRequest putResourceRequest =
        new PutResourceRequest(requestContext.getUriInfo().getBaseUri(),
                               requestContext.getAuthID(), resourceName,
                               userID, requestObject,
                               requestContext.getQueryAttributes());
    final SCIMResponse scimResponse = backend.putResource(putResourceRequest);

    // Build the response.
    final Response.ResponseBuilder responseBuilder =
        Response.status(scimResponse.getStatusCode());
    setResponseEntity(responseBuilder, produceMediaType, scimResponse);

    final com.unboundid.scim.schema.Resource resource =
        scimResponse.getResponse().getResource();
    if (resource != null)
    {
      final UriBuilder uriBuilder =
          requestContext.getUriInfo().getBaseUriBuilder();
      uriBuilder.path(resourceName);
      uriBuilder.path(resource.getId());
      responseBuilder.location(uriBuilder.build());
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
  private Response deleteUser(final RequestContext requestContext,
                              final MediaType mediaType, final String userID)
  {
    // Get the SCIM backend to process the request.
    final SCIMBackend backend = lookupBackend(requestContext);

    // Process the request.
    final DeleteResourceRequest deleteResourceRequest =
        new DeleteResourceRequest(requestContext.getUriInfo().getBaseUri(),
                                  requestContext.getAuthID(),
                                  resourceName, userID);
    final SCIMResponse scimResponse =
        backend.deleteResource(deleteResourceRequest);

    // Build the response.
    final Response.ResponseBuilder responseBuilder =
        Response.status(scimResponse.getStatusCode());
    setResponseEntity(responseBuilder, mediaType, scimResponse);

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
          throw new WebApplicationException(
              e, Response.Status.INTERNAL_SERVER_ERROR);
        }
      }
    };
    builder.entity(output);
  }
}
