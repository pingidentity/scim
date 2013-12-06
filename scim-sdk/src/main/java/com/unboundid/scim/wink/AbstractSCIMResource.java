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
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DeleteResourceRequest;
import com.unboundid.scim.sdk.GetResourceRequest;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.OAuthToken;
import com.unboundid.scim.sdk.OAuthTokenHandler;
import com.unboundid.scim.sdk.OAuthTokenStatus;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.PatchResourceRequest;
import com.unboundid.scim.sdk.PostResourceRequest;
import com.unboundid.scim.sdk.PutResourceRequest;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.ResourceSchemaBackend;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMRequest;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.UnauthorizedException;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.unboundid.scim.sdk.SCIMConstants.
    HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.unboundid.scim.sdk.SCIMConstants.
    HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.unboundid.scim.sdk.SCIMConstants.QUERY_PARAMETER_ATTRIBUTES;
import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_ENDPOINT_SCHEMAS;


/**
 * This class is an abstract Wink resource implementation for
 * SCIM operations on a SCIM endpoint.
 */
public abstract class AbstractSCIMResource extends AbstractStaticResource
{
  private final SCIMApplication application;

  /**
   * The OAuth 2.0 bearer token handler. This may be null.
   */
  private final OAuthTokenHandler tokenHandler;

  private final ResourceSchemaBackend resourceSchemaBackend;

  /**
   * Create a new AbstractSCIMResource for CRUD operations.
   *
   * @param application         The SCIM JAX-RS application associated with this
   *                            resource.
   * @param tokenHandler        The token handler to use for OAuth
   *                            authentication.
   */
  public AbstractSCIMResource(final SCIMApplication application,
                              final OAuthTokenHandler tokenHandler)
  {
    this.application = application;
    this.tokenHandler = tokenHandler;
    this.resourceSchemaBackend = new ResourceSchemaBackend(application);
  }



  /**
   * Process a GET operation.
   *
   * @param requestContext The request context.
   * @param endpoint       The endpoint requested.
   * @param userID         The user ID requested.
   *
   * @return  The response to the operation.
   */
  Response getUser(final RequestContext requestContext,
                   final String endpoint, final String userID)
  {
    SCIMBackend backend;
    ResourceDescriptor resourceDescriptor = null;
    Response.ResponseBuilder responseBuilder;
    try {
      backend = getBackend(endpoint);
      resourceDescriptor = backend.getResourceDescriptor(endpoint);
      if(resourceDescriptor == null)
      {
        throw new ResourceNotFoundException(
                endpoint + " is not a valid resource endpoint");
      }
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null) {
        throw new UnauthorizedException("Invalid credentials");
      }
      final String attributes =
          requestContext.getUriInfo().getQueryParameters().getFirst(
              QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(resourceDescriptor, attributes);

      Collection<EntityTag> versions = null;
      List<String> ifNoneMatchHeaders =
          requestContext.getHeaders().getRequestHeader(
              HttpHeaders.IF_NONE_MATCH);

      final String ifNoneMatchHeader = ifNoneMatchHeaders ==
          null || ifNoneMatchHeaders.isEmpty() ?
          null : ifNoneMatchHeaders.get(0);

      if(ifNoneMatchHeader != null)
      {
        String[] valueTokens = ifNoneMatchHeader.split(",");
        versions = new ArrayList<EntityTag>(valueTokens.length);
        for(String token : valueTokens)
        {
          try
          {
            versions.add(EntityTag.valueOf(token));
          }
          catch(IllegalArgumentException e)
          {
            throw new InvalidResourceException(e.getMessage(), e);
          }
        }
      }

      // Process the request.
      GetResourceRequest getResourceRequest =
          new GetResourceRequest(requestContext.getUriInfo().getBaseUri(),
              authID, resourceDescriptor, userID, queryAttributes,
              requestContext.getRequest());

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              getResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          application.getStatsForResource(resourceDescriptor.getName()).
              incrementStat("get-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          getResourceRequest =
               new GetResourceRequest(requestContext.getUriInfo().getBaseUri(),
                   authID, resourceDescriptor, userID, queryAttributes,
                   requestContext.getRequest());
        }
      }

      BaseResource resource =
          backend.getResource(getResourceRequest);

      // Build the response.
      boolean matchFound = false;
      if(versions != null)
      {
        EntityTag currentVersion =
            EntityTag.valueOf(resource.getMeta().getVersion());
        for(EntityTag version : versions)
        {
          if(version.getValue().equals("*") || currentVersion.equals(version))
          {
            matchFound = true;
            break;
          }
        }
      }

      if(matchFound)
      {
        responseBuilder = Response.status(Response.Status.NOT_MODIFIED);
        application.getStatsForResource(resourceDescriptor.getName()).
          incrementStat(ResourceStats.GET_NOT_MODIFIED);
      }
      else
      {
        responseBuilder = Response.status(Response.Status.OK);
        setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                          resource);
        application.getStatsForResource(resourceDescriptor.getName()).
          incrementStat(ResourceStats.GET_OK);
      }
      responseBuilder.location(resource.getMeta().getLocation());
      responseBuilder.tag(resource.getMeta().getVersion());

      if (requestContext.getOrigin() != null)
      {
        responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN,
            requestContext.getOrigin());
      }
      responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS,
          Boolean.TRUE.toString());

      if(requestContext.getProduceMediaType() ==
          MediaType.APPLICATION_JSON_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.GET_RESPONSE_JSON);
      }
      else if(requestContext.getProduceMediaType() ==
              MediaType.APPLICATION_XML_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.GET_RESPONSE_XML);
      }
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      if(resourceDescriptor != null)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat("get-" + e.getStatusCode());
      }
    }

    return responseBuilder.build();
  }



  /**
   * Process a GET operation.
   *
   * @param requestContext   The request context.
   * @param endpoint         The endpoint requested.
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
                              final String endpoint,
                              final String filterString,
                              final String baseID,
                              final String searchScope,
                              final String sortBy,
                              final String sortOrder,
                              final String pageStartIndex,
                              final String pageSize)
  {
    SCIMBackend backend;
    ResourceDescriptor resourceDescriptor = null;
    Response.ResponseBuilder responseBuilder;
    try
    {
      backend = getBackend(endpoint);
      resourceDescriptor = backend.getResourceDescriptor(endpoint);
      if(resourceDescriptor == null)
      {
        throw new ResourceNotFoundException(
                endpoint + " is not a valid resource endpoint");
      }
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
        if(resourceDescriptor.getSchema().equalsIgnoreCase(
                "urn:unboundid:schemas:scim:ldap:1.0"))
        {
          filter = SCIMFilter.parse(
                  filterString, resourceDescriptor.getSchema());
        }
        else
        {
          filter = SCIMFilter.parse(filterString);
        }
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
      int startIndex = -1;
      int count = -1;
      if (pageStartIndex != null && !pageStartIndex.isEmpty())
      {
        try
        {
          startIndex = Integer.parseInt(pageStartIndex);
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
              sortParameters, pageParameters, queryAttributes,
              requestContext.getRequest());

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              getResourcesRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          application.getStatsForResource(resourceDescriptor.getName()).
              incrementStat("query-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          getResourcesRequest =
              new GetResourcesRequest(requestContext.getUriInfo().getBaseUri(),
                      authID, resourceDescriptor, filter, baseID, searchScope,
                      sortParameters, pageParameters, queryAttributes,
                      requestContext.getRequest());
        }
      }

      final Resources resources = backend.getResources(getResourcesRequest);

      // Build the response.
      responseBuilder =
          Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        resources);

      if (requestContext.getOrigin() != null)
      {
        responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN,
            requestContext.getOrigin());
      }
      responseBuilder.header(HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS,
          Boolean.TRUE.toString());

      application.getStatsForResource(resourceDescriptor.getName()).
          incrementStat(ResourceStats.QUERY_OK);
      if(requestContext.getProduceMediaType() ==
          MediaType.APPLICATION_JSON_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.QUERY_RESPONSE_JSON);
      }
      else if(requestContext.getProduceMediaType() ==
              MediaType.APPLICATION_XML_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.QUERY_RESPONSE_XML);
      }
    }
    catch(SCIMException e)
    {
      responseBuilder =
          Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      if(resourceDescriptor != null)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat("query-" + e.getStatusCode());
      }
    }

    return responseBuilder.build();
  }



  /**
   * Process a POST operation.
   *
   * @param requestContext    The request context.
   * @param endpoint       The endpoint requested.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response postUser(final RequestContext requestContext,
                    final String endpoint,
                    final InputStream inputStream)
  {
    SCIMBackend backend;
    ResourceDescriptor resourceDescriptor = null;
    Response.ResponseBuilder responseBuilder;
    try
    {
      backend = getBackend(endpoint);
      resourceDescriptor = backend.getResourceDescriptor(endpoint);
      if(resourceDescriptor == null)
      {
        throw new ResourceNotFoundException(
                endpoint + " is not a valid resource endpoint");
      }
      final Unmarshaller unmarshaller;
      if (requestContext.getConsumeMediaType().equals(
          MediaType.APPLICATION_JSON_TYPE))
      {
        unmarshaller = new JsonUnmarshaller();
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.POST_CONTENT_JSON);
      }
      else
      {
        unmarshaller = new XmlUnmarshaller();
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.POST_CONTENT_XML);
      }
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
              queryAttributes, requestContext.getRequest());

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              postResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          application.getStatsForResource(resourceDescriptor.getName()).
              incrementStat("post-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          postResourceRequest =
              new PostResourceRequest(requestContext.getUriInfo().getBaseUri(),
                    authID, resourceDescriptor, postedResource.getScimObject(),
                    queryAttributes, requestContext.getRequest());
        }
      }

      final BaseResource resource = backend.postResource(postResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.CREATED);
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
          resource);
      responseBuilder.location(resource.getMeta().getLocation());
      responseBuilder.tag(resource.getMeta().getVersion());
      application.getStatsForResource(resourceDescriptor.getName()).
          incrementStat(ResourceStats.POST_OK);
      if(requestContext.getProduceMediaType() ==
          MediaType.APPLICATION_JSON_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.POST_RESPONSE_JSON);
      }
      else if(requestContext.getProduceMediaType() ==
              MediaType.APPLICATION_XML_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.POST_RESPONSE_XML);
      }
    } catch (SCIMException e) {
      Debug.debugException(e);
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      if(resourceDescriptor != null)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat("post-" + e.getStatusCode());
      }
    }

    return responseBuilder.build();
  }



  /**
   * Process a PUT operation.
   *
   * @param requestContext    The request context.
   * @param endpoint          The endpoint requested.
   * @param userID            The target user ID.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response putUser(final RequestContext requestContext,
                   final String endpoint,
                   final String userID,
                   final InputStream inputStream)
  {
    SCIMBackend backend;
    ResourceDescriptor resourceDescriptor = null;
    Response.ResponseBuilder responseBuilder;
    try {
      backend = getBackend(endpoint);
      resourceDescriptor = backend.getResourceDescriptor(endpoint);
      if(resourceDescriptor == null)
      {
        throw new ResourceNotFoundException(
                endpoint + " is not a valid resource endpoint");
      }
      final Unmarshaller unmarshaller;
      if (requestContext.getConsumeMediaType().equals(
          MediaType.APPLICATION_JSON_TYPE))
      {
        unmarshaller = new JsonUnmarshaller();
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.PUT_CONTENT_JSON);
      }
      else
      {
        unmarshaller = new XmlUnmarshaller();
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.PUT_CONTENT_XML);
      }
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

      Collection<EntityTag> versions = null;
        List<String> ifMatchHeaders =
            requestContext.getHeaders().getRequestHeader(HttpHeaders.IF_MATCH);

      final String ifMatchHeader = ifMatchHeaders ==
          null || ifMatchHeaders.isEmpty() ? null : ifMatchHeaders.get(0);

      if(ifMatchHeader != null)
      {
        String[] valueTokens = ifMatchHeader.split(",");
        versions = new ArrayList<EntityTag>(valueTokens.length);
        for(String token : valueTokens)
        {
          EntityTag tag;
          try
          {
            tag = EntityTag.valueOf(token);
          }
          catch(IllegalArgumentException e)
          {
            throw new InvalidResourceException(e.getMessage(), e);
          }
          if(tag.getValue().equals("*"))
          {
            // Should behave as if the if-match header was not there
            versions = null;
            break;
          }
          else
          {
            versions.add(tag);
          }
        }
      }

      // Process the request.
      PutResourceRequest putResourceRequest =
          new PutResourceRequest(requestContext.getUriInfo().getBaseUri(),
              authID, resourceDescriptor, userID,
              puttedResource.getScimObject(), versions, queryAttributes,
              requestContext.getRequest());

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              putResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          application.getStatsForResource(resourceDescriptor.getName()).
              incrementStat("put-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          putResourceRequest =
             new PutResourceRequest(requestContext.getUriInfo().getBaseUri(),
                  authID, resourceDescriptor, userID,
                  puttedResource.getScimObject(), versions, queryAttributes,
                  requestContext.getRequest());
        }
      }

      final BaseResource scimResponse = backend.putResource(putResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        scimResponse);
      responseBuilder.location(scimResponse.getMeta().getLocation());
      responseBuilder.tag(scimResponse.getMeta().getVersion());
      application.getStatsForResource(resourceDescriptor.getName()).
          incrementStat(ResourceStats.PUT_OK);
      if(requestContext.getProduceMediaType() ==
          MediaType.APPLICATION_JSON_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.PUT_RESPONSE_JSON);
      }
      else if(requestContext.getProduceMediaType() ==
              MediaType.APPLICATION_XML_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.PUT_RESPONSE_XML);
      }
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      if(resourceDescriptor != null)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat("put-" + e.getStatusCode());
      }
    }

    return responseBuilder.build();
  }



  /**
   * Process a PATCH operation.
   *
   * @param requestContext    The request context.
   * @param endpoint          The endpoint requested.
   * @param userID            The target user ID.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response patchUser(final RequestContext requestContext,
                     final String endpoint,
                     final String userID,
                     final InputStream inputStream)
  {
    SCIMBackend backend;
    ResourceDescriptor resourceDescriptor = null;
    Response.ResponseBuilder responseBuilder;
    try {
      backend = getBackend(endpoint);
      resourceDescriptor = backend.getResourceDescriptor(endpoint);
      if(resourceDescriptor == null)
      {
        throw new ResourceNotFoundException(
                endpoint + " is not a valid resource endpoint");
      }
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null)
      {
        throw new UnauthorizedException("Invalid credentials");
      }
      final Unmarshaller unmarshaller;
      if (requestContext.getConsumeMediaType().equals(
              MediaType.APPLICATION_JSON_TYPE))
      {
        unmarshaller = new JsonUnmarshaller();
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.PATCH_CONTENT_JSON);
      }
      else
      {
        unmarshaller = new XmlUnmarshaller();
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.PATCH_CONTENT_XML);
      }
      // Parse the resource.
      final BaseResource patchedResource = unmarshaller.unmarshal(
           inputStream, resourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY);

      final String attributes =
              requestContext.getUriInfo().getQueryParameters().getFirst(
                      QUERY_PARAMETER_ATTRIBUTES);
      final SCIMQueryAttributes queryAttributes =
              new SCIMQueryAttributes(resourceDescriptor, attributes);

      Collection<EntityTag> versions = null;
      List<String> ifMatchHeaders =
          requestContext.getHeaders().getRequestHeader(HttpHeaders.IF_MATCH);

      final String ifMatchHeader = ifMatchHeaders ==
          null || ifMatchHeaders.isEmpty() ? null : ifMatchHeaders.get(0);

      if(ifMatchHeader != null)
      {
        String[] valueTokens = ifMatchHeader.split(",");
        versions = new ArrayList<EntityTag>(valueTokens.length);
        for(String token : valueTokens)
        {
          EntityTag tag;
          try
          {
            tag = EntityTag.valueOf(token);
          }
          catch(IllegalArgumentException e)
          {
            throw new InvalidResourceException(e.getMessage(), e);
          }
          if(tag.getValue().equals("*"))
          {
            // Should behave as if the if-match header was not there
            versions = null;
            break;
          }
          else
          {
            versions.add(tag);
          }
        }
      }

      // Process the request.
      PatchResourceRequest patchResourceRequest =
              new PatchResourceRequest(requestContext.getUriInfo().getBaseUri(),
                      authID, resourceDescriptor, userID,
                      patchedResource.getScimObject(), versions,
                      queryAttributes, requestContext.getRequest());

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              patchResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          application.getStatsForResource(resourceDescriptor.getName()).
              incrementStat("patch-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          patchResourceRequest =
             new PatchResourceRequest(requestContext.getUriInfo().getBaseUri(),
                   authID, resourceDescriptor, userID,
                   patchedResource.getScimObject(), versions, queryAttributes,
                   requestContext.getRequest());
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
      responseBuilder.location(scimResponse.getMeta().getLocation());
      responseBuilder.tag(scimResponse.getMeta().getVersion());

      application.getStatsForResource(resourceDescriptor.getName()).
          incrementStat(ResourceStats.PATCH_OK);
      if(requestContext.getProduceMediaType() ==
          MediaType.APPLICATION_JSON_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.PATCH_RESPONSE_JSON);
      }
      else if(requestContext.getProduceMediaType() ==
              MediaType.APPLICATION_XML_TYPE)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat(ResourceStats.PATCH_RESPONSE_XML);
      }
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
              e);
      if(resourceDescriptor != null)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat("patch-" + e.getStatusCode());
      }
    }

    return responseBuilder.build();
  }



  /**
   * Process a DELETE operation.
   *
   * @param requestContext    The request context.
   * @param endpoint          The endpoint requested.
   * @param userID            The target user ID.
   *
   * @return  The response to the operation.
   */
  Response deleteUser(final RequestContext requestContext,
                      final String endpoint,
                      final String userID)
  {
    SCIMBackend backend;
    ResourceDescriptor resourceDescriptor = null;
    // Process the request.
    Response.ResponseBuilder responseBuilder;
    try {
      backend = getBackend(endpoint);
      resourceDescriptor = backend.getResourceDescriptor(endpoint);
      if(resourceDescriptor == null)
      {
        throw new ResourceNotFoundException(
                endpoint + " is not a valid resource endpoint");
      }
      String authID = requestContext.getAuthID();
      if(authID == null && tokenHandler == null)
      {
        throw new UnauthorizedException("Invalid credentials");
      }

      Collection<EntityTag> versions = null;
      List<String> ifMatchHeaders =
          requestContext.getHeaders().getRequestHeader(HttpHeaders.IF_MATCH);

      final String ifMatchHeader = ifMatchHeaders ==
          null || ifMatchHeaders.isEmpty() ? null : ifMatchHeaders.get(0);


      if(ifMatchHeader != null)
      {
        String[] valueTokens = ifMatchHeader.split(",");
        versions = new ArrayList<EntityTag>(valueTokens.length);
        for(String token : valueTokens)
        {
          EntityTag tag;
          try
          {
            tag = EntityTag.valueOf(token);
          }
          catch(IllegalArgumentException e)
          {
            throw new InvalidResourceException(e.getMessage(), e);
          }
          if(tag.getValue().equals("*"))
          {
            // Should behave as if the if-match header was not there
            versions = null;
            break;
          }
          else
          {
            versions.add(tag);
          }
        }
      }

      DeleteResourceRequest deleteResourceRequest =
        new DeleteResourceRequest(requestContext.getUriInfo().getBaseUri(),
            authID, resourceDescriptor, userID, versions,
            requestContext.getRequest());

      if (authID == null)
      {
        AtomicReference<String> authIDRef = new AtomicReference<String>();
        Response response = validateOAuthToken(requestContext,
                              deleteResourceRequest, authIDRef, tokenHandler);
        if (response != null)
        {
          application.getStatsForResource(resourceDescriptor.getName()).
              incrementStat("delete-" + response.getStatus());
          return response;
        }
        else
        {
          authID = authIDRef.get();
          deleteResourceRequest =
             new DeleteResourceRequest(requestContext.getUriInfo().getBaseUri(),
                   authID, resourceDescriptor, userID, versions,
                   requestContext.getRequest());
        }
      }

      backend.deleteResource(deleteResourceRequest);
      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      application.getStatsForResource(resourceDescriptor.getName()).
          incrementStat(ResourceStats.DELETE_OK);
    } catch (SCIMException e) {
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      if(resourceDescriptor != null)
      {
        application.getStatsForResource(resourceDescriptor.getName()).
            incrementStat("delete-" + e.getStatusCode());
      }
    }

    return responseBuilder.build();
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
      builder.header("WWW-Authenticate", "Bearer realm=\"SCIM\"");
      return builder.build();

    }
    else if (headerList.size() > 1)
    {
      return invalidRequest("The Authorization header has too many values",
              context.getProduceMediaType());
    }

    String header = headerList.get(0);
    String[] authorization = header.split(" ");
    if (authorization.length == 2 &&
          authorization[0].equalsIgnoreCase("Bearer") &&
            authorization[1].length() > 0)
    {
      try
      {
        OAuthToken token = tokenHandlerImpl.decodeOAuthToken(authorization[1]);

        if (token == null)
        {
          return invalidRequest("Could not decode the access token",
                  context.getProduceMediaType());
        }

        if (!tokenHandlerImpl.isTokenAuthentic(token))
        {
          return invalidToken("The access token is not authentic",
                  context.getProduceMediaType());
        }

        if (!tokenHandlerImpl.isTokenForThisServer(token))
        {
          return invalidToken(
                  "The access token is not intended for this server",
                  context.getProduceMediaType());
        }

        if (tokenHandlerImpl.isTokenExpired(token))
        {
          return invalidToken("The access token is expired",
                  context.getProduceMediaType());
        }

        OAuthTokenStatus status =
                tokenHandlerImpl.validateToken(token, request);

        if (status.getErrorCode().equals(
                OAuthTokenStatus.ErrorCode.INVALID_TOKEN))
        {
          String errorDescription = status.getErrorDescription();
          return invalidToken(errorDescription, context.getProduceMediaType());
        }
        else if (status.getErrorCode().equals(
                OAuthTokenStatus.ErrorCode.INSUFFICIENT_SCOPE))
        {
          String errorDescription = status.getErrorDescription();
          String scope = status.getScope();
          return insufficientScope(scope, errorDescription,
                  context.getProduceMediaType());
        }

        String authID = tokenHandlerImpl.getAuthzDN(token);
        if (authID == null)
        {
          return invalidToken(
                  "The access token did not contain an authorization DN",
                  context.getProduceMediaType());
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
        return invalidRequest(t.getMessage(), context.getProduceMediaType());
      }
    }
    else if(authorization.length == 2 &&
              authorization[0].equalsIgnoreCase("Basic") &&
                authorization[1].length() > 0)
    {
      //The client tried to do Basic Authentication, and since we made it here,
      //it failed.
      Response.ResponseBuilder builder = Response.status(401);
      builder.header("WWW-Authenticate", "Basic realm=\"SCIM\"");
      SCIMException exception = new UnauthorizedException(null);
      setResponseEntity(builder, context.getProduceMediaType(), exception);
      return builder.build();
    }
    else
    {
      return invalidRequest("The Authorization header was malformed",
              context.getProduceMediaType());
    }
  }

  /**
   * Creates an invalid_request Response with the specified error description.
   *
   * @param errorDescription The description of the validation error.
   * @param mediaType The accept-type for SCIMRequest.
   * @return a Response instance.
   */
  private static Response invalidRequest(final String errorDescription,
                                         final MediaType mediaType)
  {
    Response.ResponseBuilder builder = Response.status(400);
    String authHeaderValue = "Bearer realm=\"SCIM\", error=\"invalid_request\"";
    if (errorDescription != null && !errorDescription.isEmpty())
    {
      authHeaderValue += ", error_description=\"" + errorDescription + "\"";
    }
    builder.header("WWW-Authenticate", authHeaderValue);

    SCIMException exception =
            SCIMException.createException(400, errorDescription);
    setResponseEntity(builder, mediaType, exception);

    return builder.build();
  }

  /**
   * Creates an invalid_token Response with the specified error description.
   *
   * @param errorDescription The description of the validation error.
   * @param mediaType The accept-type for SCIMRequest.
   * @return a Response instance.
   */
  private static Response invalidToken(final String errorDescription,
                                       final MediaType mediaType)
  {
    Response.ResponseBuilder builder = Response.status(401);
    String authHeaderValue = "Bearer realm=\"SCIM\", error=\"invalid_token\"";
    if (errorDescription != null && !errorDescription.isEmpty())
    {
      authHeaderValue += ", error_description=\"" + errorDescription + "\"";
    }
    builder.header("WWW-Authenticate", authHeaderValue);

    SCIMException exception =
            SCIMException.createException(401, errorDescription);
    setResponseEntity(builder, mediaType, exception);

    return builder.build();
  }

  /**
   * Creates an insufficient_scope Response with the specified error
   * description and scope.
   *
   * @param errorDescription The description of the validation error.
   * @param scope The OAuth scope required to access the target resource.
   * @param mediaType The accept-type for SCIMRequest.
   * @return a Response instance.
   */
  private static Response insufficientScope(final String scope,
                                            final String errorDescription,
                                            final MediaType mediaType)
  {
    Response.ResponseBuilder builder = Response.status(403);
    String authHeaderValue =
            "Bearer realm=\"SCIM\", error=\"insufficient_scope\"";
    if (errorDescription != null && !errorDescription.isEmpty())
    {
      authHeaderValue += ", error_description=\"" + errorDescription + "\"";
    }
    if (scope != null && !scope.isEmpty())
    {
      authHeaderValue += ", scope=\"" + scope + "\"";
    }
    builder.header("WWW-Authenticate", authHeaderValue);

    SCIMException exception =
            SCIMException.createException(403, errorDescription);
    setResponseEntity(builder, mediaType, exception);

    return builder.build();
  }

  /**
   * Retrieves the backend that should service the provided endpoint.
   *
   * @param endpoint The endpoint requested.
   * @return The backend that should service the provided endpoint.
   */
  private SCIMBackend getBackend(final String endpoint)
  {
    if(endpoint.equals(RESOURCE_ENDPOINT_SCHEMAS))
    {
      return resourceSchemaBackend;
    }

    return application.getBackend();
  }
}
