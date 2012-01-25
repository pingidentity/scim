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

import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.data.ServiceProviderConfig;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.BulkResponse;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMResponse;
import com.unboundid.scim.sdk.UnauthorizedException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;



/**
 * This class is the base class for JAX-RS resources implementing the Bulk
 * operation.
 */
public class AbstractBulkResource
{
  /**
   * The SCIM JAX-RS application associated with this resource.
   */
  private final SCIMApplication application;

  /**
   * The resource stats for the bulk operation end-point.
   */
  private final ResourceStats bulkResourceStats;

  /**
   * The SCIMBackend to use to process individual operations within a bulk
   * operation.
   */
  private final SCIMBackend backend;



  /**
   * Create a new instance of the bulk resource.
   *
   * @param application        The SCIM JAX-RS application associated with this
   *                           resource.
   * @param bulkResourceStats  The resource stats for the bulk operation
   *                           end-point.
   * @param backend            The SCIMBackend to use to process individual
   *                           operations within a bulk operation.
   */
  public AbstractBulkResource(final SCIMApplication application,
                              final ResourceStats bulkResourceStats,
                              final SCIMBackend backend)
  {
    this.application       = application;
    this.bulkResourceStats = bulkResourceStats;
    this.backend           = backend;
  }



  /**
   * Process a POST operation.
   *
   * @param requestContext    The request context.
   * @param inputStream       The content to be consumed.
   *
   * @return  The response to the operation.
   */
  Response postBulk(final RequestContext requestContext,
                    final InputStream inputStream)
  {
    final Unmarshaller unmarshaller;
    if (requestContext.getConsumeMediaType().equals(
        MediaType.APPLICATION_JSON_TYPE))
    {
      unmarshaller = new JsonUnmarshaller();
      bulkResourceStats.incrementStat(ResourceStats.POST_CONTENT_JSON);
    }
    else
    {
      unmarshaller = new XmlUnmarshaller();
      bulkResourceStats.incrementStat(ResourceStats.POST_CONTENT_XML);
    }

    Response.ResponseBuilder responseBuilder;
    try
    {
      String authID = requestContext.getAuthID();
      if(authID == null)
      {
        throw new UnauthorizedException("Invalid credentials");
      }

      // Check the Content-Length against the maxPayloadSize.
      final ServiceProviderConfig serviceProviderConfig =
          application.getServiceProviderConfig();
      final BulkConfig bulkConfig = serviceProviderConfig.getBulkConfig();
      if (requestContext.getContentLength() > bulkConfig.getMaxPayloadSize())
      {
        throw SCIMException.createException(
            413, "The content length of the bulk request (" +
                 requestContext.getContentLength() +
                 ") exceeds the maxPayloadSize (" +
                 bulkConfig.getMaxPayloadSize() + ")");
      }

      final BulkContentRequestHandler handler =
          new BulkContentRequestHandler(application.getDescriptors());

      unmarshaller.bulkUnmarshal(inputStream, bulkConfig, handler);

      final List<BulkOperation> responses =
          handler.processOperations(application, requestContext, backend);

      // Build the response.
      responseBuilder = Response.status(Response.Status.OK);
      final SCIMResponse scimResponse = new BulkResponse(responses);
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        scimResponse);
      bulkResourceStats.incrementStat(ResourceStats.POST_OK);
    }
    catch (SCIMException e)
    {
      Debug.debugException(e);
      // Build the response.
      responseBuilder = Response.status(e.getStatusCode());
      setResponseEntity(responseBuilder, requestContext.getProduceMediaType(),
                        e);
      bulkResourceStats.incrementStat("post-" + e.getStatusCode());
    }

    if (requestContext.getProduceMediaType() == MediaType.APPLICATION_JSON_TYPE)
    {
      bulkResourceStats.incrementStat(ResourceStats.POST_RESPONSE_JSON);
    }
    else if (requestContext.getProduceMediaType() ==
             MediaType.APPLICATION_XML_TYPE)
    {
      bulkResourceStats.incrementStat(ResourceStats.POST_RESPONSE_XML);
    }

    return responseBuilder.build();
  }



  /**
   * Sets the response entity (content) for a SCIM bulk response.
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
}
