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
import com.unboundid.scim.sdk.BulkStreamResponse;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DebugType;
import com.unboundid.scim.sdk.OAuthTokenHandler;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMResponse;
import com.unboundid.scim.sdk.ServerErrorException;
import com.unboundid.scim.sdk.UnauthorizedException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;



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
   * The OAuth 2.0 bearer token handler. This may be null.
   */
  private final OAuthTokenHandler tokenHandler;

  /**
   * Create a new instance of the bulk resource.
   *
   * @param application        The SCIM JAX-RS application associated with this
   *                           resource.
   * @param bulkResourceStats  The resource stats for the bulk operation
   *                           end-point.
   * @param backend            The SCIMBackend to use to process individual
   *                           operations within a bulk operation.
   * @param tokenHandler       The token handler to use for OAuth
   *                           authentication.
   */
  public AbstractBulkResource(final SCIMApplication application,
                              final ResourceStats bulkResourceStats,
                              final SCIMBackend backend,
                              final OAuthTokenHandler tokenHandler)
  {
    this.application       = application;
    this.bulkResourceStats = bulkResourceStats;
    this.backend           = backend;
    this.tokenHandler      = tokenHandler;
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
      if(authID == null && tokenHandler == null)
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

      // Fail the request if the maximum concurrent requests would be exceeded.
      application.acquireBulkRequestPermit();
      try
      {
        // Write the request to a temporary file.
        final File requestFile = File.createTempFile(
            "scim-bulk-request-",
            "." + requestContext.getConsumeMediaType().getSubtype(),
            application.getTmpDataDir());
        try
        {
          requestFile.deleteOnExit();
          final FileOutputStream fileOutputStream =
              new FileOutputStream(requestFile);
          try
          {
            final byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1)
            {
              totalBytes += bytesRead;
              if (totalBytes > bulkConfig.getMaxPayloadSize())
              {
                throw SCIMException.createException(
                    413,
                    "The size of the bulk request exceeds the maxPayloadSize " +
                    "(" + bulkConfig.getMaxPayloadSize() + ")");
              }
              fileOutputStream.write(buffer, 0, bytesRead);
            }
          }
          finally
          {
            fileOutputStream.close();
          }

          // Write the response to a temporary file.
          final BulkStreamResponse bulkStreamResponse =
              new BulkStreamResponse(application, requestContext);
          try
          {
            final BulkContentRequestHandler handler =
                new BulkContentRequestHandler(application, requestContext,
                                              backend, bulkStreamResponse,
                                              tokenHandler);
            unmarshaller.bulkUnmarshal(requestFile, bulkConfig, handler);

            // Build the response.
            responseBuilder = Response.status(Response.Status.OK);
            setResponseEntity(responseBuilder,
                              requestContext.getProduceMediaType(),
                              bulkStreamResponse);
            bulkResourceStats.incrementStat(ResourceStats.POST_OK);
          }
          catch (Exception e)
          {
            Debug.debugException(e);
            bulkStreamResponse.finalizeResponse();
            throw e;
          }
        }
        finally
        {
          if (!requestFile.delete())
          {
            Debug.debug(Level.WARNING, DebugType.OTHER,
                        "Could not delete temporary file " +
                        requestFile.getAbsolutePath());
          }
        }
      }
      catch (SCIMException e)
      {
        throw e;
      }
      catch (Exception e)
      {
        Debug.debugException(e);
        throw new ServerErrorException(
            "Error processing bulk request: " + e.getMessage());
      }
      finally
      {
        application.releaseBulkRequestPermit();
      }
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
