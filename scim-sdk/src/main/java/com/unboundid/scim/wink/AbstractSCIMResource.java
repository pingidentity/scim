/*
 * Copyright 2011-2013 UnboundID Corp.
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
import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DeleteResourceRequest;
import com.unboundid.scim.sdk.GetResourceRequest;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.OAuthToken;
import com.unboundid.scim.sdk.OAuthTokenHandler;
import com.unboundid.scim.sdk.OAuthTokenStatus;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.PatchResourceRequest;
import com.unboundid.scim.sdk.PostResourceRequest;
import com.unboundid.scim.sdk.PutResourceRequest;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMRequest;
import com.unboundid.scim.sdk.SCIMResponse;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.UnauthorizedException;
import org.apache.wink.common.AbstractDynamicResource;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
   * The OAuth 2.0 bearer token handler. This may be null.
   */
  private final OAuthTokenHandler tokenHandler;

  /**
   * Create a new AbstractSCIMResource for CRUD operations.
   *
   * @param path                The path of this resource.
   * @param resourceDescriptor  The resource descriptor to use.
   * @param resourceStats       The ResourceStats instance to use.
   * @param backend             The SCIMBackend to use to process requests.
   * @param tokenHandler        The token handler to use for OAuth
   *                            authentication.
   */
  public AbstractSCIMResource(final String path,
                              final ResourceDescriptor resourceDescriptor,
                              final ResourceStats resourceStats,
                              final SCIMBackend backend,
                              final OAuthTokenHandler tokenHandler)
  {
    this.resourceDescriptor = resourceDescriptor;
    this.backend = backend;
    this.tokenHandler = tokenHandler;
    this.resourceStats = resourceStats;
    super.setPath(path);
  }



  /**
   * Process a GET operation.
   *
   * @param requestContext The request context.
   * @param userID         The user ID requested.
   *
   * @return  The response to the operation.
   */
  Response getUser(final RequestContext requestContext, final String userID)
  {
    Response.ResponseBuilder responseBuilder;
    try {
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null) {
        throw new UnauthorizedException("Invalid credentials");
      }
      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Process the request.
      GetResourceRequest getResourceRequest =
          new GetResourceRequest(requestContext.getUriInfo().getBaseUri(),
              authID, resourceDescriptor, userID, queryAttributes);

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              getResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          resourceStats.incrementStat("get-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          getResourceRequest =
               new GetResourceRequest(requestContext.getUriInfo().getBaseUri(),
                   authID, resourceDescriptor, userID, queryAttributes);
        }
      }

      BaseResource resource =
          backend.getResource(getResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        resource);
      URI location = resource.getMeta().getLocation();
      if(location != null)
      {
        responseBuilder.location(location);
      }
      resourceStats.incrementStat(ResourceStats.GET_OK);
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      resourceStats.incrementStat("get-" + e.getStatusCode());
    }

    if (requestContext.getOrigin() != null)
    {
      responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN,
          requestContext.getOrigin());
    }
    responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS,
        Boolean.TRUE.toString());

    if(requestContext.getProduceMediaType() == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.GET_RESPONSE_JSON);
    }
    else if(requestContext.getProduceMediaType() ==
            MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.GET_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a GET operation.
   *
   * @param requestContext   The request context.
   * @param filterString     The filter query parameter, or {@code null}.
   * @param baseID           The SCIM resource ID of the search base entry,
   *                         or {@code null}.
   * @param searchScope      The LDAP search scope to use, or {@code null}.
   * @param sortBy           The sortBy query parameter, or {@code null}.
   * @param sortOrder        The sortOrder query parameter, or {@code null}.
   * @param pageStartIndex   The startIndex query parameter, or {@code null}.
   * @param pageSize         The count query parameter, or {@code null}.
   *
   * @return  The response to the operation.
   */
  protected Response getUsers(final RequestContext requestContext,
                              final String filterString,
                              final String baseID,
                              final String searchScope,
                              final String sortBy,
                              final String sortOrder,
                              final String pageStartIndex,
                              final String pageSize)
  {
    Response.ResponseBuilder responseBuilder;
    try
    {
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null) {
        throw new UnauthorizedException("Invalid credentials");
      }
      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Parse the filter parameters.
      final SCIMFilter filter;
      if (filterString != null && !filterString.isEmpty())
      {
        filter = SCIMFilter.parse(filterString, resourceDescriptor.getSchema());
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
            new SortParameters(AttributePath.parse(sortBy,
                                resourceDescriptor.getSchema()), sortOrder);
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
      GetResourcesRequest getResourcesRequest =
          new GetResourcesRequest(requestContext.getUriInfo().getBaseUri(),
              authID, resourceDescriptor, filter, baseID, searchScope,
              sortParameters, pageParameters, queryAttributes);

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              getResourcesRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          resourceStats.incrementStat("query-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          getResourcesRequest =
              new GetResourcesRequest(requestContext.getUriInfo().getBaseUri(),
                      authID, resourceDescriptor, filter, baseID, searchScope,
                      sortParameters, pageParameters, queryAttributes);
        }
      }

      final Resources resources = backend.getResources(getResourcesRequest);

      // Build the response.
      responseBuilder =
          Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        resources);
      resourceStats.incrementStat(ResourceStats.QUERY_OK);
    }
    catch(SCIMException e)
    {
      responseBuilder =
          Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      resourceStats.incrementStat("query-" + e.getStatusCode());
    }

    if (requestContext.getOrigin() != null)
    {
      responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN,
          requestContext.getOrigin());
    }
    responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS,
        Boolean.TRUE.toString());

    if(requestContext.getProduceMediaType() == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.QUERY_RESPONSE_JSON);
    }
    else if(requestContext.getProduceMediaType() ==
            MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.QUERY_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a POST operation.
   *
   * @param requestContext    The request context.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response postUser(final RequestContext requestContext,
                            final InputStream inputStream)
  {
    final Unmarshaller unmarshaller;
    if (requestContext.getConsumeMediaType().equals(
        MediaType.APPLICATION_JSON_TYPE))
    {
      unmarshaller = new JsonUnmarshaller();
      resourceStats.incrementStat(ResourceStats.POST_CONTENT_JSON);
    }
    else
    {
      unmarshaller = new XmlUnmarshaller();
      resourceStats.incrementStat(ResourceStats.POST_CONTENT_XML);
    }

    Response.ResponseBuilder responseBuilder;
    try
    {
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null)
      {
        throw new UnauthorizedException("Invalid credentials");
      }

      // Parse the resource.
      final BaseResource postedResource = unmarshaller.unmarshal(
          inputStream, resourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY);

      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Process the request.
      PostResourceRequest postResourceRequest =
          new PostResourceRequest(requestContext.getUriInfo().getBaseUri(),
              authID, resourceDescriptor, postedResource.getScimObject(),
              queryAttributes);

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              postResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          resourceStats.incrementStat("post-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          postResourceRequest =
              new PostResourceRequest(requestContext.getUriInfo().getBaseUri(),
                    authID, resourceDescriptor, postedResource.getScimObject(),
                    queryAttributes);
        }
      }

      final BaseResource resource =
          backend.postResource(postResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.CREATED);
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
          resource);
      responseBuilder.location(resource.getMeta().getLocation());
      resourceStats.incrementStat(ResourceStats.POST_OK);
    } catch (SCIMException e) {
      Debug.debugException(e);
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      resourceStats.incrementStat("post-" + e.getStatusCode());
    }

    if(requestContext.getProduceMediaType() == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.POST_RESPONSE_JSON);
    }
    else if(requestContext.getProduceMediaType() ==
            MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.POST_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a PUT operation.
   *
   * @param requestContext    The request context.
   * @param userID            The target user ID.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response putUser(final RequestContext requestContext,
                   final String userID,
                   final InputStream inputStream)
  {
    final Unmarshaller unmarshaller;
    if (requestContext.getConsumeMediaType().equals(
        MediaType.APPLICATION_JSON_TYPE))
    {
      unmarshaller = new JsonUnmarshaller();
      resourceStats.incrementStat(ResourceStats.PUT_CONTENT_JSON);
    }
    else
    {
      unmarshaller = new XmlUnmarshaller();
      resourceStats.incrementStat(ResourceStats.PUT_CONTENT_XML);
    }

    Response.ResponseBuilder responseBuilder;
    try {
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null)
      {
        throw new UnauthorizedException("Invalid credentials");
      }

      // Parse the resource.
      final BaseResource puttedResource = unmarshaller.unmarshal(
          inputStream, resourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY);

      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Process the request.
      PutResourceRequest putResourceRequest =
          new PutResourceRequest(requestContext.getUriInfo().getBaseUri(),
              authID, resourceDescriptor, userID,
              puttedResource.getScimObject(), queryAttributes);

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              putResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          resourceStats.incrementStat("put-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          putResourceRequest =
             new PutResourceRequest(requestContext.getUriInfo().getBaseUri(),
                  authID, resourceDescriptor, userID,
                  puttedResource.getScimObject(), queryAttributes);
        }
      }

      final BaseResource scimResponse = backend.putResource(putResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        scimResponse);
      responseBuilder.location(scimResponse.getMeta().getLocation());
      resourceStats.incrementStat(ResourceStats.PUT_OK);
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      resourceStats.incrementStat("put-" + e.getStatusCode());
    }

    if(requestContext.getProduceMediaType() == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.PUT_RESPONSE_JSON);
    }
    else if(requestContext.getProduceMediaType() ==
            MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.PUT_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a PATCH operation.
   *
   * @param requestContext    The request context.
   * @param userID            The target user ID.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response patchUser(final RequestContext requestContext,
                     final String userID,
                     final InputStream inputStream)
  {
    final Unmarshaller unmarshaller;
    if (requestContext.getConsumeMediaType().equals(
            MediaType.APPLICATION_JSON_TYPE))
    {
      unmarshaller = new JsonUnmarshaller();
      resourceStats.incrementStat(ResourceStats.PATCH_CONTENT_JSON);
    }
    else
    {
      unmarshaller = new XmlUnmarshaller();
      resourceStats.incrementStat(ResourceStats.PATCH_CONTENT_XML);
    }

    Response.ResponseBuilder responseBuilder;
    try {
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null)
      {
        throw new UnauthorizedException("Invalid credentials");
      }

      // Parse the resource.
      final BaseResource patchedResource = unmarshaller.unmarshal(
           inputStream, resourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY);

      final String attributes =
              requestContext.getUriInfo().getQueryParameters().getFirst(
                      QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
              new SCIMQueryAttributes(resourceDescriptor, attributes);

      // Process the request.
      PatchResourceRequest patchResourceRequest =
              new PatchResourceRequest(requestContext.getUriInfo().getBaseUri(),
                      authID, resourceDescriptor, userID,
                      patchedResource.getScimObject(), queryAttributes);

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              patchResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          resourceStats.incrementStat("patch-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          patchResourceRequest =
             new PatchResourceRequest(requestContext.getUriInfo().getBaseUri(),
                   authID, resourceDescriptor, userID,
                   patchedResource.getScimObject(), queryAttributes);
        }
      }

      final BaseResource scimResponse =
              backend.patchResource(patchResourceRequest);

      // Build the response.
      if (!queryAttributes.allAttributesRequested())
      {
        responseBuilder = Response.status(Response.Status.OK);
        setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                scimResponse);
      }
      else
      {
        responseBuilder = Response.status(Response.Status.NO_CONTENT);
      }

      resourceStats.incrementStat(ResourceStats.PATCH_OK);
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
              e);
      resourceStats.incrementStat("patch-" + e.getStatusCode());
    }

    if(requestContext.getProduceMediaType() == MediaType.APPLICATION_JSON_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.PATCH_RESPONSE_JSON);
    }
    else if(requestContext.getProduceMediaType() ==
            MediaType.APPLICATION_XML_TYPE)
    {
      resourceStats.incrementStat(ResourceStats.PATCH_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Process a DELETE operation.
   *
   * @param requestContext    The request context.
   * @param userID     The target user ID.
   *
   * @return  The response to the operation.
   */
  Response deleteUser(final RequestContext requestContext,
                      final String userID)
  {
    // Process the request.
    Response.ResponseBuilder responseBuilder;
    try {
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null)
      {
        throw new UnauthorizedException("Invalid credentials");
      }
      DeleteResourceRequest deleteResourceRequest =
        new DeleteResourceRequest(requestContext.getUriInfo().getBaseUri(),
            authID, resourceDescriptor, userID);

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              deleteResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          resourceStats.incrementStat("delete-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          deleteResourceRequest =
             new DeleteResourceRequest(requestContext.getUriInfo().getBaseUri(),
                   authID, resourceDescriptor, userID);
        }
      }

      backend.deleteResource(deleteResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      resourceStats.incrementStat(ResourceStats.DELETE_OK);
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
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
    final Marshaller marshaller;
    builder.type(mediaType);
    if (mediaType.equals(MediaType.APPLICATION_JSON_TYPE))
    {
      marshaller = new JsonMarshaller();
    }
    else
    {
      marshaller = new XmlMarshaller();
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


  /**
   * Handles OAuth bearer token validation. This method should only be called if
   * 1) the request was not previously authenticated with HTTP Basic Auth, and
   * 2) we have a OAuthTokenHandler available.
   * <p>
   * It will call into the OAuthTokenHandler implementation to validate the
   * bearer token and then do one of two things:
   * <ul>
   *   <li>If the token is valid, it will set the output parameter 'authIDRef'
   *       to the DN of the authorization entry and return null.</li>
   *   <li>If the token is invalid, it will construct a Http Response with the
   *       appropriate headers and return it.</li>
   * </ul>
   *
   * @param context      The incoming HTTP request context.
   * @param request      The unmarshalled SCIM Request for passing into the
   *                     token handler.
   * @param authIDRef    An output parameter to contain the DN of the
   *                     authorization entry.
   * @param tokenHandlerImpl The OAuthTokenHandler to use.
   * @return             {@code null} if the token was successfully validated,
   *                     otherwise a Response instance containing the error
   *                     information.
   */
  static Response validateOAuthToken(final RequestContext context,
                                     final SCIMRequest request,
                                     final AtomicReference<String> authIDRef,
                                     final OAuthTokenHandler tokenHandlerImpl)
  {
    HttpHeaders headers = context.getHeaders();
    List<String> headerList = headers.getRequestHeader("Authorization");

    if (headerList == null || headerList.isEmpty())
    {
      //If the client lacks any authentication information, just return 401
      Response.ResponseBuilder builder = Response.status(401);
      builder.header("WWW-Authenticate", "Bearer realm=SCIM");
      return builder.build();

    }
    else if (headerList.size() > 1)
    {
      return invalidRequest("The Authorization header has too many values");
    }

    String header = headerList.get(0);
    String[] authorization = header.split(" ");
    if (authorization.length == 2 &&
          authorization[0].equalsIgnoreCase("Bearer") &&
            authorization[1].length() > 0)
    {
      try
      {
        byte[] rawToken = DatatypeConverter.parseBase64Binary(authorization[1]);
        if (rawToken == null)
        {
          throw new IllegalArgumentException();
        }
      }
      catch (IllegalArgumentException e)
      {
        return invalidRequest("The access token cannot be base64-decoded");
      }

      try
      {
        OAuthToken token = tokenHandlerImpl.decodeOAuthToken(authorization[1]);

        if (token == null)
        {
          return invalidRequest("Could not decode the access token");
        }

        if (!tokenHandlerImpl.isTokenAuthentic(token))
        {
          return invalidToken("The access token is not authentic");
        }

        if (!tokenHandlerImpl.isTokenForThisServer(token))
        {
          return invalidToken(
                  "The access token is not intended for this server");
        }

        if (tokenHandlerImpl.isTokenExpired(token))
        {
          return invalidToken("The access token is expired");
        }

        OAuthTokenStatus status =
                tokenHandlerImpl.validateToken(token, request);

        if (status.getErrorCode().equals(
                OAuthTokenStatus.ErrorCode.INVALID_TOKEN))
        {
          String errorDescription = status.getErrorDescription();
          return invalidToken(errorDescription);
        }
        else if (status.getErrorCode().equals(
                OAuthTokenStatus.ErrorCode.INSUFFICIENT_SCOPE))
        {
          String errorDescription = status.getErrorDescription();
          String scope = status.getScope();
          return insufficientScope(scope, errorDescription);
        }

        String authID = tokenHandlerImpl.getAuthzDN(token);
        if (authID == null)
        {
          return invalidToken(
                  "The access token did not contain an authorization DN");
        }
        else
        {
          authIDRef.set(authID);
          return null;
        }
      }
      catch(Throwable t)
      {
        Debug.debugException(t);
        return invalidRequest(t.getMessage());
      }
    }
    else if(authorization.length == 2 &&
              authorization[0].equalsIgnoreCase("Basic") &&
                authorization[1].length() > 0)
    {
      //The client tried to do Basic Authentication, and since we made it here,
      //it failed.
      Response.ResponseBuilder builder = Response.status(401);
      builder.header("WWW-Authenticate", "Basic realm=SCIM");
      return builder.build();
    }
    else
    {
      return invalidRequest("The Authorization header was malformed");
    }
  }

  /**
   * Creates an invalid_request Response with the specified error description.
   *
   * @param errorDescription The description of the validation error.
   * @return a Response instance.
   */
  private static Response invalidRequest(final String errorDescription)
  {
    Response.ResponseBuilder builder = Response.status(400);
    builder.header("WWW-Authenticate", "Bearer realm=SCIM");
    builder.header("WWW-Authenticate", "error=\"invalid_request\"");
    if (errorDescription != null && !errorDescription.isEmpty())
    {
      builder.header("WWW-Authenticate", "error_description=\"" +
              errorDescription + "\"");
    }
    return builder.build();
  }

  /**
   * Creates an invalid_token Response with the specified error description.
   *
   * @param errorDescription The description of the validation error.
   * @return a Response instance.
   */
  private static Response invalidToken(final String errorDescription)
  {
    Response.ResponseBuilder builder = Response.status(401);
    builder.header("WWW-Authenticate", "Bearer realm=SCIM");
    builder.header("WWW-Authenticate", "error=\"invalid_token\"");
    if (errorDescription != null && !errorDescription.isEmpty())
    {
      builder.header("WWW-Authenticate", "error_description=\"" +
              errorDescription + "\"");
    }
    return builder.build();
  }

  /**
   * Creates an insufficient_scope Response with the specified error
   * description and scope.
   *
   * @param errorDescription The description of the validation error.
   * @param scope The OAuth scope required to access the target resource.
   * @return a Response instance.
   */
  private static Response insufficientScope(final String scope,
                                            final String errorDescription)
  {
    Response.ResponseBuilder builder = Response.status(403);
    builder.header("WWW-Authenticate", "Bearer realm=SCIM");
    builder.header("WWW-Authenticate", "error=\"insufficient_scope\"");
    if (errorDescription != null && !errorDescription.isEmpty())
    {
      builder.header("WWW-Authenticate", "error_description=\"" +
              errorDescription + "\"");
    }
    if (scope != null && !scope.isEmpty())
    {
      builder.header("WWW-Authenticate", "scope=\"" + scope + "\"");
    }
    return builder.build();
  }
}
