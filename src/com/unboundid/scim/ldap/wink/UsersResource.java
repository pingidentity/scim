/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap.wink;

import com.unboundid.scim.ldap.GetResourcesRequest;
import com.unboundid.scim.ldap.PageParameters;
import com.unboundid.scim.ldap.SCIMBackend;
import com.unboundid.scim.ldap.SCIMFilter;
import com.unboundid.scim.ldap.SCIMResponse;
import com.unboundid.scim.ldap.SCIMServer;
import com.unboundid.scim.ldap.SortParameters;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMQueryAttributes;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
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
import java.security.Principal;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class is a JAX-RS resource implementation for the SCIM Users end-point.
 */
@Path(RESOURCE_ENDPOINT_USERS + "{format:(\\.[^/]*?)?}")
public class UsersResource
{
  /**
   * The URI information of the current request.
   */
  @Context
  private UriInfo uriInfo;

  /**
   * The HTTP headers of the current request.
   */
  @Context
  private HttpHeaders headers;

  /**
   * The SCIM backend to process the current request.
   */
  private final SCIMBackend backend;

  /**
   * The HTTP authenticated user ID. Not to be confused with a SCIM user ID.
   */
  private final String authID;

  /**
   * The query attributes of the current request.
   */
  private final SCIMQueryAttributes queryAttributes;

  /**
   * The filter parameters, or {@code null} if there are no filter parameters.
   */
  private final SCIMFilter filter;

  /**
   * The sorting parameters, or {@code null} if there are no sorting
   * parameters.
   */
  private final SortParameters sortParameters;

  /**
   * The pagination parameters, or {@code null} if there are no pagination
   * parameters.
   */
  private final PageParameters pageParameters;

  /**
   * The value of the HTTP Origin header.
   */
  @HeaderParam(HEADER_NAME_ORIGIN)
  private String origin;

  /**
   * The response format specified in the URI.
   */
  @PathParam("format")
  private String format;


  /**
   * Creates the resource implementation for a request.
   *
   * @param servletContext   The servlet context of the current request.
   * @param securityContext  The security context of the current request.
   * @param attributes       The query attributes, or {@code null}.
   * @param filterBy         The filterBy query parameter, or {@code null}.
   * @param filterOp         The filterOp query parameter, or {@code null}.
   * @param filterValue      The filterValue query parameter, or {@code null}.
   * @param sortBy           The sortBy query parameter, or {@code null}.
   * @param sortOrder        The sortOrder query parameter, or {@code null}.
   * @param pageStartIndex   The startIndex query parameter, or {@code null}.
   * @param pageSize         The count query parameter, or {@code null}.
   */
  public UsersResource(@Context final ServletContext servletContext,
                       @Context final SecurityContext securityContext,
                       @QueryParam(QUERY_PARAMETER_ATTRIBUTES)
                       final String attributes,
                       @QueryParam(QUERY_PARAMETER_FILTER_BY)
                       final String filterBy,
                       @QueryParam(QUERY_PARAMETER_FILTER_OP)
                       final String filterOp,
                       @QueryParam(QUERY_PARAMETER_FILTER_VALUE)
                       final String filterValue,
                       @QueryParam(QUERY_PARAMETER_SORT_BY)
                       final String sortBy,
                       @QueryParam(QUERY_PARAMETER_SORT_ORDER)
                       final String sortOrder,
                       @QueryParam(QUERY_PARAMETER_PAGE_START_INDEX)
                       final String pageStartIndex,
                       @QueryParam(QUERY_PARAMETER_PAGE_SIZE)
                       final String pageSize)
  {
    // Get the SCIM backend to process the request.
    final SCIMServer scimServer = SCIMServer.getInstance();
    backend = scimServer.getBackend(servletContext.getContextPath());

    // Determine the authenticated ID for the request.
    final Principal userPrincipal = securityContext.getUserPrincipal();
    if (userPrincipal != null)
    {
      authID = userPrincipal.getName();
    }
    else
    {
      authID = null;
    }

    // Parse the query attributes.
    if (attributes != null && !attributes.isEmpty())
    {
      queryAttributes = new SCIMQueryAttributes(attributes.split(","));
    }
    else
    {
      queryAttributes = new SCIMQueryAttributes();
    }

    // Parse the filter parameters.
    if (filterBy != null && !filterBy.isEmpty())
    {
      final String filterBySchemaURI;
      final String attributePath;
      final int lastColonPos =
          filterBy.lastIndexOf(SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE);
      if (lastColonPos == -1)
      {
        filterBySchemaURI = SCHEMA_URI_CORE;
        attributePath = filterBy;
      }
      else
      {
        filterBySchemaURI = filterBy.substring(0, lastColonPos);
        attributePath = filterBy.substring(lastColonPos+1);
      }

      final String[] filterByPath = attributePath.split("\\.");

      filter = new SCIMFilter(
          filterOp, filterValue, filterBySchemaURI, filterByPath);
    }
    else
    {
      filter = null;
    }

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
  }



  /**
   * Implement the GET operation producing XML or JSON format.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
  public Response doGet()
  {
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

    return getUsers(mediaType);
  }



  /**
   * Process a GET operation.
   *
   * @param mediaType  The media type to be produced.
   *
   * @return  The response to the operation.
   */
  protected Response getUsers(final MediaType mediaType)
  {
    // Process the request.
    final GetResourcesRequest getResourcesRequest =
        new GetResourcesRequest(authID,
                                RESOURCE_ENDPOINT_USERS,
                                filter,
                                sortParameters,
                                pageParameters,
                                queryAttributes);
    final SCIMResponse scimResponse = backend.getResources(getResourcesRequest);

    // Build the response.
    final Response.ResponseBuilder responseBuilder =
        Response.status(scimResponse.getStatusCode());
    setResponseEntity(responseBuilder, mediaType, scimResponse);

    if (origin != null)
    {
      responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN, origin);
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
          throw new WebApplicationException(
              e, Response.Status.INTERNAL_SERVER_ERROR);
        }
      }
    };
    builder.entity(output);
  }



}
