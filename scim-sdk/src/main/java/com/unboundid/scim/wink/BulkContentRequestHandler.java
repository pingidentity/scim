/*
 * Copyright 2012 UnboundID Corp.
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
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DeleteResourceRequest;
import com.unboundid.scim.sdk.InvalidResourceException;
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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



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
   * A map from bulkId to resourceID.
   */
  private final Map<String,String> resourceIDs;

  /**
   * A set containing the index number of bulk operations that contain bulkId
   * data references.
   */
  private final Set<Integer> bulkIdRefOperations;

  private final List<BulkOperation> operations;



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
   * @param descriptors  The resource descriptors keyed by endpoint.
   */
  public BulkContentRequestHandler(
      final Map<String,ResourceDescriptor> descriptors)
  {
    this.descriptors = descriptors;
    resourceIDs = new HashMap<String, String>();
    bulkIdRefOperations = new HashSet<Integer>();
    operations = new ArrayList<BulkOperation>();
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
      bulkIdRefOperations.add(opIndex);
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
  {
    operations.add(bulkOperation);
  }



  /**
   * Process the bulk operations.
   *
   * @param application     The SCIM application.
   * @param requestContext  The request context for the bulk request.
   * @param backend         The SCIM backend to process the operations.
   *
   * @return  The bulk operation responses.
   */
  public List<BulkOperation> processOperations(
      final SCIMApplication application,
      final RequestContext requestContext,
      final SCIMBackend backend)
  {
    final Set<String> bulkIds = new HashSet<String>(operations.size());
    final List<BulkOperation> responses =
        new ArrayList<BulkOperation>(operations.size());
    int errorCount = 0;

    for (int opIndex = 0; opIndex < operations.size(); opIndex++)
    {
      final BulkOperation response =
          processOperation(application, requestContext, bulkIds, backend,
                           operations.get(opIndex), opIndex);
      responses.add(response);
      if (response.getStatus().getDescription() != null &&
          !response.getStatus().getCode().equals("200") &&
          !response.getStatus().getCode().equals("201"))
      {
        errorCount++;
        if (errorCount == failOnErrors)
        {
          break;
        }
      }
    }

    return responses;
  }



  /**
   * Process an operation from a bulk request.
   *
   * @param application     The SCIM application.
   * @param requestContext  The bulk request context.
   * @param bulkIds         The set of defined bulkIds from all operations.
   * @param backend         The SCIM backend to process the operation.
   * @param operation       The operation to be processed from the bulk request.
   * @param opIndex         The index of the operation.
   *
   * @return  The operation response.
   */
  private BulkOperation processOperation(final SCIMApplication application,
                                         final RequestContext requestContext,
                                         final Set<String> bulkIds,
                                         final SCIMBackend backend,
                                         final BulkOperation operation,
                                         final int opIndex)
  {
    final BulkOperation.Method method = operation.getMethod();
    final String bulkId = operation.getBulkId();
    final String path = operation.getPath();
    BaseResource resource = operation.getData();

    int statusCode = 200;
    String statusMessage = null;
    String location = null;
    String endpoint = null;
    String resourceID = null;

    final ResourceDescriptor descriptor;
    final ResourceStats resourceStats;
    try
    {
      if (method == null)
      {
        throw new InvalidResourceException(
            "The bulk operation does not specify a HTTP method");
      }

      if (method == BulkOperation.Method.PATCH)
      {
        throw SCIMException.createException(501, "PATCH is not supported");
      }

      if (path == null)
      {
        throw new InvalidResourceException(
            "The bulk operation does not specify a path");
      }

      if (path != null)
      {
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

        if (method != BulkOperation.Method.POST)
        {
          if (endPos >= path.length() - 1)
          {
            throw new InvalidResourceException(
                "The bulk operation does not have a resource ID in " +
                "the path");
          }
          resourceID = path.substring(endPos+1);
          if (resourceID.startsWith("bulkId:"))
          {
            final String ref = resourceID.substring(7);
            resourceID = resourceIDs.get(ref);
            if (resourceID == null)
            {
              throw new InvalidResourceException(
                  "The bulk operation path has an undefined or forward " +
                  "bulkId reference '" + ref + "'");
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
      Debug.debugException(e);
      statusCode = e.getStatusCode();
      statusMessage = e.getMessage();

      final Status status =
          new Status(String.valueOf(statusCode), statusMessage);

      return new BulkOperation(method, bulkId, null, null, location,
                               null, status);
    }

    try
    {
      final UriBuilder locationBuilder =
          UriBuilder.fromUri(requestContext.getUriInfo().getBaseUri());
      locationBuilder.path(path);

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

      // Resolve bulkId references in the data.
      if (resource != null && bulkIdRefOperations.contains(opIndex))
      {
        resource = resolveBulkIds(resource);
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
        }
      }

      // Request no attributes because we will not provide the resource in
      // the response.
      final SCIMQueryAttributes queryAttributes =
          new SCIMQueryAttributes(descriptor, "");

      switch (method)
      {
        case POST:
          if (bulkId == null)
          {
            throw new InvalidResourceException(
                "The bulk operation has method POST but does not " +
                "specify a bulkId");
          }

          final BaseResource postedResource = backend.postResource(
              new PostResourceRequest(requestContext.getUriInfo().getBaseUri(),
                                      requestContext.getAuthID(),
                                      descriptor,
                                      resource.getScimObject(),
                                      queryAttributes));

          final String newResourceID = postedResource.getId();
          resourceIDs.put(bulkId, newResourceID);
          locationBuilder.path(newResourceID);
          location = locationBuilder.build().toString();
          statusCode = 201;
          resourceStats.incrementStat(ResourceStats.POST_OK);
          break;

        case PUT:
          backend.putResource(
              new PutResourceRequest(requestContext.getUriInfo().getBaseUri(),
                                     requestContext.getAuthID(),
                                     descriptor,
                                     resourceID,
                                     resource.getScimObject(),
                                     queryAttributes));
          location = locationBuilder.build().toString();
          resourceStats.incrementStat(ResourceStats.PUT_OK);
          break;

        case DELETE:
          backend.deleteResource(new DeleteResourceRequest(
              requestContext.getUriInfo().getBaseUri(),
              requestContext.getAuthID(),
              descriptor,
              resourceID));
          location = locationBuilder.build().toString();
          resourceStats.incrementStat(ResourceStats.DELETE_OK);
          break;
      }
    }
    catch (SCIMException e)
    {
      Debug.debugException(e);
      statusCode = e.getStatusCode();
      statusMessage = e.getMessage();
      resourceStats.incrementStat("post-" + e.getStatusCode());
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
      }
    }

    final Status status =
        new Status(String.valueOf(statusCode), statusMessage);

    return new BulkOperation(method, bulkId, null, null, location,
                             null, status);
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
          throw new InvalidResourceException(
              "Undefined or forward bulkId reference '" + bulkId + "'");
        }
      }
      else
      {
        return v;
      }
    }
  }
}
