/*
 * Copyright 2012-2013 UnboundID Corp.
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
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.BulkContentHandler;
import com.unboundid.scim.sdk.BulkException;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.BulkStreamResponse;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DeleteResourceRequest;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.OAuthTokenHandler;
import com.unboundid.scim.sdk.PatchResourceRequest;
import com.unboundid.scim.sdk.PostResourceRequest;
import com.unboundid.scim.sdk.PutResourceRequest;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.ServerErrorException;
import com.unboundid.scim.sdk.Status;
import com.unboundid.scim.sdk.UnauthorizedException;

import static com.unboundid.scim.wink.AbstractSCIMResource.validateOAuthToken;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This class implements the bulk operation handler to process bulk operation
 * requests in the SCIM server.
 * The original purpose of the BulkContentHandler interface was to allow
 * each operation to be processed as soon as it had been read so that
 * we do not have to hold the entire content of a bulk request in
 * memory. However, there are two issues making that approach infeasible:
 * <ol>
 * <li>Since JSON objects are unordered, the failOnErrors value could
 * conceivably come after the Operations array, which would be too late.</li>
 * <li>It would not be possible to reject a request that exceeded the
 * maxOperations setting without processing any operations.</li>
 * </ol>
 */
public class BulkContentRequestHandler extends BulkContentHandler
{
  /**
   * The resource descriptors keyed by endpoint.
   */
  private final Map<String,ResourceDescriptor> descriptors;

  /**
   * The SCIM application.
   */
  private final SCIMApplication application;

  /**
   * The request context for the bulk request.
   */
  private final RequestContext requestContext;

  /**
   * The SCIM backend to process the operations.
   */
  private final SCIMBackend backend;

  /**
   * The OAuth 2.0 bearer token handler. This may be null.
   */
  private final OAuthTokenHandler tokenHandler;

  /**
   * The bulk stream response to write the operation responses to.
   */
  private final BulkStreamResponse bulkStreamResponse;

  /**
   * A map from bulkId to resourceID.
   */
  private final Map<String,String> resourceIDs;

  /**
   * A set containing the unresolved bulkId data references for the latest
   * bulk operation.
   */
  private final Set<String> unresolvedBulkIdRefs;


  private int errorCount = 0;

  /**
   * The set of defined bulkIds from all operations.
   */
  private final Set<String> bulkIds;



  /**
   * The number of errors that the Service Provider will accept before the
   * operation is terminated and an error response is returned. The default
   * is to continue performing as many changes as possible without regard to
   * failures.
   */
  private int failOnErrors = Integer.MAX_VALUE;



  /**
   * Create a new instance of this bulk operation handler.
   *
   * @param application         The SCIM application.
   * @param requestContext      The request context for the bulk request.
   * @param backend             The SCIM backend to process the operations.
   * @param bulkStreamResponse  The bulk stream response to write response
   *                            operations to.
   * @param tokenHandler        The OAuth token handler implementation to use.
   */
  public BulkContentRequestHandler(
      final SCIMApplication application,
      final RequestContext requestContext,
      final SCIMBackend backend,
      final BulkStreamResponse bulkStreamResponse,
      final OAuthTokenHandler tokenHandler)
  {
    this.descriptors        = application.getDescriptors();
    this.application        = application;
    this.requestContext     = requestContext;
    this.backend            = backend;
    this.tokenHandler       = tokenHandler;
    this.bulkStreamResponse = bulkStreamResponse;

    resourceIDs = new HashMap<String, String>();
    unresolvedBulkIdRefs = new HashSet<String>();
    bulkIds = new HashSet<String>();
  }



  /**
   * {@inheritDoc}
   */
  public void handleFailOnErrors(final int failOnErrors)
  {
    this.failOnErrors = failOnErrors;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String transformValue(final int opIndex, final String value)
  {
    if (value.startsWith("bulkId:"))
    {
      final String bulkId = value.substring(7);
      final String resourceID = resourceIDs.get(bulkId);
      if (resourceID != null)
      {
        return resourceID;
      }
      else
      {
        unresolvedBulkIdRefs.add(bulkId);
      }
    }

    return value;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ResourceDescriptor getResourceDescriptor(final String endpoint)
  {
    return descriptors.get(endpoint);
  }



  /**
   * {@inheritDoc}
   */
  public void handleOperation(final int opIndex,
                              final BulkOperation bulkOperation)
      throws BulkException, SCIMException
  {
    if (errorCount < failOnErrors)
    {
      final BulkOperation response = processOperation(bulkOperation);
      unresolvedBulkIdRefs.clear();
      bulkStreamResponse.writeBulkOperation(response);
    }
  }



  /**
   * {@inheritDoc}
   */
  public boolean handleException(final int opIndex,
                                 final BulkException bulkException)
      throws SCIMException
  {
    Debug.debugException(bulkException);
    if (errorCount < failOnErrors)
    {
      int statusCode = bulkException.getCause().getStatusCode();
      String statusMessage = bulkException.getCause().getMessage();

      final Status status =
          new Status(String.valueOf(statusCode), statusMessage);

      BulkOperation response = BulkOperation.createResponse(
          bulkException.getMethod(), bulkException.getBulkId(),
          bulkException.getLocation(), status);
      bulkStreamResponse.writeBulkOperation(response);
      errorCount++;
      return errorCount != failOnErrors;
    }
    else
    {
      return false;
    }
  }



  /**
   * Process an operation from a bulk request.
   *
   * @param operation       The operation to be processed from the bulk request.
   *
   * @return  The operation response.
   * @throws  BulkException  If an error occurs while processing the individual
   *                         operation within the bulk operation.
   */
  private BulkOperation processOperation(final BulkOperation operation)
      throws BulkException
  {
    final String httpMethod = operation.getMethod();
    final String bulkId = operation.getBulkId();
    final String path = operation.getPath();
    BaseResource resource = operation.getData();

    int statusCode = 200;
    String location = null;
    String endpoint = null;
    String resourceID = null;

    final ResourceDescriptor descriptor;
    final ResourceStats resourceStats;
    final BulkOperation.Method method;
    try
    {
      if (httpMethod == null || httpMethod.isEmpty())
      {
        throw new InvalidResourceException(
            "The bulk operation does not specify a HTTP method");
      }

      try
      {
        method = BulkOperation.Method.valueOf(httpMethod);
      }
      catch (IllegalArgumentException e)
      {
        throw new BulkException(SCIMException.createException(
            405, "The bulk operation specifies an invalid " +
            "HTTP method '" + httpMethod + "'. Allowed methods are " +
            Arrays.asList(BulkOperation.Method.values())),
            httpMethod, bulkId, location);
      }

      if (path == null)
      {
        throw new InvalidResourceException(
            "The bulk operation does not specify a path");
      }

      if (path != null)
      {
        // Parse the path into an endpoint and optional resource ID.
        int startPos = 0;
        if (path.charAt(startPos) == '/')
        {
          startPos++;
        }

        int endPos = path.indexOf('/', startPos);
        if (endPos == -1)
        {
          endPos = path.length();
        }

        endpoint = path.substring(startPos, endPos);

        if (endPos < path.length() - 1)
        {
          resourceID = path.substring(endPos+1);
        }

        if (method == BulkOperation.Method.POST)
        {
          if (resourceID != null)
          {
            throw new InvalidResourceException(
                "The bulk operation has method POST but the path includes" +
                "a resource ID");
          }
        }
        else
        {
          if (resourceID == null)
          {
            throw new InvalidResourceException(
                "The bulk operation does not have a resource ID in " +
                "the path");
          }

          if (resourceID.startsWith("bulkId:"))
          {
            final String ref = resourceID.substring(7);
            resourceID = resourceIDs.get(ref);
            if (resourceID == null)
            {
              throw SCIMException.createException(
                  409, "Cannot resolve bulkId reference '" + ref + "'");
            }
          }
        }
      }

      descriptor = getResourceDescriptor(endpoint);
      if (descriptor == null)
      {
        throw new InvalidResourceException(
            "The bulk operation specifies an unknown resource " +
            "endpoint '" + endpoint + "'");
      }

      resourceStats = application.getStatsForResource(descriptor.getName());
      if (resourceStats == null)
      {
        throw new ServerErrorException(
            "Cannot find resource stats for resource '" +
            descriptor.getName() + "'");
      }
    }
    catch (SCIMException e)
    {
      throw new BulkException(e, httpMethod, bulkId, location);
    }

    final UriBuilder locationBuilder =
        UriBuilder.fromUri(requestContext.getUriInfo().getBaseUri());
    locationBuilder.path(path);

    try
    {
      if (method == BulkOperation.Method.POST && bulkId == null)
      {
        throw new InvalidResourceException(
            "The bulk operation has method POST but does not " +
            "specify a bulkId");
      }

      if (bulkId != null)
      {
        if (!bulkIds.add(bulkId))
        {
          throw new InvalidResourceException(
              "The bulk operation defines a duplicate bulkId '" +
              bulkId + "'");
        }
      }

      if (method != BulkOperation.Method.DELETE && resource == null)
      {
        throw new InvalidResourceException(
            "The bulk operation does not have any resource data");
      }

      if (!unresolvedBulkIdRefs.isEmpty())
      {
        throw SCIMException.createException(
            409, "Cannot resolve bulkId references "
                 + unresolvedBulkIdRefs);
      }

      if (requestContext.getConsumeMediaType().equals(
          MediaType.APPLICATION_JSON_TYPE))
      {
        switch (method)
        {
          case POST:
            resourceStats.incrementStat(ResourceStats.POST_CONTENT_JSON);
            break;
          case PUT:
            resourceStats.incrementStat(ResourceStats.PUT_CONTENT_JSON);
            break;
          case PATCH:
            resourceStats.incrementStat(ResourceStats.PATCH_CONTENT_JSON);
            break;
        }
      }
      else
      {
        switch (method)
        {
          case POST:
            resourceStats.incrementStat(ResourceStats.POST_CONTENT_XML);
            break;
          case PUT:
            resourceStats.incrementStat(ResourceStats.PUT_CONTENT_XML);
            break;
          case PATCH:
            resourceStats.incrementStat(ResourceStats.PATCH_CONTENT_XML);
        }
      }

      // Request no attributes because we will not provide the resource in
      // the response.
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(descriptor, "");

      switch (method)
      {
        case POST:
          PostResourceRequest postResourceRequest =
               new PostResourceRequest(requestContext.getUriInfo().getBaseUri(),
                                       requestContext.getAuthID(),
                                       descriptor,
                                       resource.getScimObject(),
                                       queryAttributes);

          if (requestContext.getAuthID() == null)
          {
            AtomicReference<String> authIDRef = new AtomicReference<String>();
            Response response = validateOAuthToken(requestContext,
                                  postResourceRequest, authIDRef, tokenHandler);
            if (response != null)
            {
              throw new UnauthorizedException("Invalid credentials");
            }
            else
            {
              String authID = authIDRef.get();
              postResourceRequest = new PostResourceRequest(
                              requestContext.getUriInfo().getBaseUri(),
                              authID, descriptor, resource.getScimObject(),
                              queryAttributes);
            }
          }

          final BaseResource postedResource =
                  backend.postResource(postResourceRequest);

          resourceID = postedResource.getId();
          locationBuilder.path(resourceID);
          statusCode = 201;
          resourceStats.incrementStat(ResourceStats.POST_OK);
          break;

        case PUT:
          PutResourceRequest putResourceRequest =
                new PutResourceRequest(requestContext.getUriInfo().getBaseUri(),
                                       requestContext.getAuthID(),
                                       descriptor,
                                       resourceID,
                                       resource.getScimObject(),
                                       queryAttributes);

          if (requestContext.getAuthID() == null)
          {
            AtomicReference<String> authIDRef = new AtomicReference<String>();
            Response response = validateOAuthToken(requestContext,
                                   putResourceRequest, authIDRef, tokenHandler);
            if (response != null)
            {
              throw new UnauthorizedException("Invalid credentials");
            }
            else
            {
              String authID = authIDRef.get();
              putResourceRequest = new PutResourceRequest(
                      requestContext.getUriInfo().getBaseUri(),
                      authID, descriptor, resourceID, resource.getScimObject(),
                      queryAttributes);
            }
          }

          backend.putResource(putResourceRequest);
          resourceStats.incrementStat(ResourceStats.PUT_OK);
          break;

        case PATCH:
          PatchResourceRequest patchResourceRequest =
              new PatchResourceRequest(requestContext.getUriInfo().getBaseUri(),
                                       requestContext.getAuthID(),
                                       descriptor,
                                       resourceID,
                                       resource.getScimObject(),
                                       queryAttributes);

          if (requestContext.getAuthID() == null)
          {
            AtomicReference<String> authIDRef = new AtomicReference<String>();
            Response response = validateOAuthToken(requestContext,
                                 patchResourceRequest, authIDRef, tokenHandler);
            if (response != null)
            {
              throw new UnauthorizedException("Invalid credentials");
            }
            else
            {
              String authID = authIDRef.get();
              patchResourceRequest = new PatchResourceRequest(
                      requestContext.getUriInfo().getBaseUri(),
                      authID, descriptor, resourceID, resource.getScimObject(),
                      queryAttributes);
            }
          }

          backend.patchResource(patchResourceRequest);
          resourceStats.incrementStat(ResourceStats.PATCH_OK);
          break;

        case DELETE:
          DeleteResourceRequest deleteResourceRequest =
             new DeleteResourceRequest(requestContext.getUriInfo().getBaseUri(),
                                       requestContext.getAuthID(),
                                       descriptor,
                                       resourceID);

          if (requestContext.getAuthID() == null)
          {
            AtomicReference<String> authIDRef = new AtomicReference<String>();
            Response response = validateOAuthToken(requestContext,
                                deleteResourceRequest, authIDRef, tokenHandler);
            if (response != null)
            {
              throw new UnauthorizedException("Invalid credentials");
            }
            else
            {
              String authID = authIDRef.get();
              deleteResourceRequest = new DeleteResourceRequest(
                      requestContext.getUriInfo().getBaseUri(),
                      authID, descriptor, resourceID);
            }
          }

          backend.deleteResource(deleteResourceRequest);
          resourceStats.incrementStat(ResourceStats.DELETE_OK);
          break;
      }

      if (bulkId != null)
      {
        resourceIDs.put(bulkId, resourceID);
      }
    }
    catch (SCIMException e)
    {
      switch (method)
      {
        case POST:
          resourceStats.incrementStat("post-" + e.getStatusCode());
          break;
        case PUT:
          resourceStats.incrementStat("put-" + e.getStatusCode());
          break;
        case PATCH:
          resourceStats.incrementStat("patch-" + e.getStatusCode());
          break;
        case DELETE:
          resourceStats.incrementStat("delete-" + e.getStatusCode());
          break;
      }
      throw new BulkException(e, httpMethod, bulkId, location);
    }

    if (requestContext.getProduceMediaType() ==
        MediaType.APPLICATION_JSON_TYPE)
    {
      switch (method)
      {
        case POST:
          resourceStats.incrementStat(ResourceStats.POST_RESPONSE_JSON);
          break;
        case PUT:
          resourceStats.incrementStat(ResourceStats.PUT_RESPONSE_JSON);
          break;
        case PATCH:
          resourceStats.incrementStat(ResourceStats.PATCH_RESPONSE_JSON);
          break;
      }
    }
    else if (requestContext.getProduceMediaType() ==
             MediaType.APPLICATION_XML_TYPE)
    {
      switch (method)
      {
      case POST:
        resourceStats.incrementStat(ResourceStats.POST_RESPONSE_XML);
        break;
      case PUT:
        resourceStats.incrementStat(ResourceStats.PUT_RESPONSE_XML);
        break;
      case PATCH:
        resourceStats.incrementStat(ResourceStats.PATCH_RESPONSE_XML);
      }
    }

    // Set the location for all operations except an unsuccessful POST.
    if (method != BulkOperation.Method.POST || statusCode == 201)
    {
      location = locationBuilder.build().toString();
    }

    final Status status =
        new Status(String.valueOf(statusCode), null);

    return BulkOperation.createResponse(method.name(), bulkId, location,
                                        status);
  }



  /**
   * Obtain a copy of the provided resource with each bulkId reference
   * resolved to a resource ID.
   *
   * @param resource  The resource that may contain bulkId references.
   *
   * @return  A copy of the resource with bulkId references resolved.
   *
   * @throws SCIMException  If there are any undefined bulkId references.
   */
  private BaseResource resolveBulkIds(final BaseResource resource)
      throws SCIMException
  {
    final SCIMObject src = resource.getScimObject();
    final SCIMObject dst = new SCIMObject();

    for (final String schema : src.getSchemas())
    {
      for (final SCIMAttribute a : src.getAttributes(schema))
      {
        dst.setAttribute(resolveBulkIds(a));
      }
    }

    return new BaseResource(resource.getResourceDescriptor(), dst);
  }



  /**
   * Obtain a copy of the provided SCIM attribute with each bulkId reference
   * resolved to a resource ID.
   *
   * @param a  The attribute that may contain bulkId references.
   *
   * @return  A copy of the attribute with bulkId references resolved.
   *
   * @throws SCIMException  If there are any undefined bulkId references.
   */
  private SCIMAttribute resolveBulkIds(final SCIMAttribute a)
      throws SCIMException
  {
    final SCIMAttributeValue[] srcValues = a.getValues();
    final SCIMAttributeValue[] dstValues =
        new SCIMAttributeValue[a.getValues().length];
    for (int i = 0; i < srcValues.length; i++)
    {
      dstValues[i] = resolveBulkIds(srcValues[i]);
    }

    return SCIMAttribute.create(a.getAttributeDescriptor(), dstValues);
  }



  /**
   * Obtain a copy of the provided SCIM attribute value with each bulkId
   * reference resolved to a resource ID.
   *
   * @param v  The attribute value that may contain bulkId references.
   *
   * @return  A copy of the attribute value with bulkId references resolved.
   *
   * @throws SCIMException  If there are any undefined bulkId references.
   */
  private SCIMAttributeValue resolveBulkIds(final SCIMAttributeValue v)
      throws SCIMException
  {
    if (v.isComplex())
    {
      final Collection<SCIMAttribute> srcAttrs = v.getAttributes().values();
      final ArrayList<SCIMAttribute> dstAttrs =
          new ArrayList<SCIMAttribute>(srcAttrs.size());
      for (final SCIMAttribute a : srcAttrs)
      {
        dstAttrs.add(resolveBulkIds(a));
      }

      return SCIMAttributeValue.createComplexValue(dstAttrs);
    }
    else
    {
      final String s = v.getStringValue();
      if (s.startsWith("bulkId:"))
      {
        final String bulkId = s.substring(7);
        final String resourceID = resourceIDs.get(bulkId);
        if (resourceID != null)
        {
          return SCIMAttributeValue.createStringValue(resourceID);
        }
        else
        {
          throw SCIMException.createException(
              409, "Cannot resolve bulkId reference '" + bulkId + "'");
        }
      }
      else
      {
        return v;
      }
    }
  }
}
