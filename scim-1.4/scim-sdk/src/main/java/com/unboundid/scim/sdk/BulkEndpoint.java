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

package com.unboundid.scim.sdk;

import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;



/**
 * This class provides a way for a SCIM client to invoke a bulk request.
 */
public class BulkEndpoint
{
  private final SCIMService service;
  private final RestClient client;
  private final MediaType contentType;
  private final MediaType acceptType;
  private final Unmarshaller unmarshaller;
  private final Marshaller marshaller;



  /**
   * Create a new instance of a bulk request.
   *
   * @param service       The SCIM service.
   * @param client        The REST client.
   */
  BulkEndpoint(final SCIMService service, final RestClient client)
  {
    this.service      = service;
    this.client       = client;

    this.contentType  = service.getContentType();
    this.acceptType   = service.getAcceptType();

    if (contentType.equals(MediaType.APPLICATION_JSON_TYPE))
    {
      this.marshaller = new JsonMarshaller();
    }
    else
    {
      this.marshaller = new XmlMarshaller();
    }

    if (acceptType.equals(MediaType.APPLICATION_JSON_TYPE))
    {
      this.unmarshaller = new JsonUnmarshaller();
    }
    else
    {
      this.unmarshaller = new XmlUnmarshaller();
    }
  }



  /**
   * Retrieves the response to the bulk request. This should only be called
   * after all operations to be performed have been provided.
   *
   * @param operations    The bulk operations to be performed.
   * @param failOnErrors  The number of errors that the service provider will
   *                      accept before the operation is terminated and an
   *                      error response is returned. A value of -1 indicates
   *                      the the service provider will continue to perform
   *                      as many operations as possible without regard to
   *                      failures.
   *
   * @return  The bulk response.
   *
   * @throws SCIMException  If the request fails.
   */
  public BulkResponse processRequest(final List<BulkOperation> operations,
                                     final int failOnErrors)
      throws SCIMException
  {
    final BulkContentResponseHandler handler =
        new BulkContentResponseHandler();
    processRequest(handler, operations, failOnErrors);

    return new BulkResponse(handler.getOperations());
  }



  /**
   * Processes the bulk request. This should only be called
   * after all operations to be performed have been provided.
   *
   * @param handler       The bulk content handler that is to be used to process
   *                      each operation response in the bulk response.
   * @param operations    The bulk operations to be performed.
   * @param failOnErrors  The number of errors that the service provider will
   *                      accept before the operation is terminated and an
   *                      error response is returned. A value of -1 indicates
   *                      the the service provider will continue to perform
   *                      as many operations as possible without regard to
   *                      failures.
   *
   * @throws SCIMException  If the request fails.
   */
  private void processRequest(final BulkContentHandler handler,
                              final List<BulkOperation> operations,
                              final int failOnErrors)
      throws SCIMException
  {
    final URI uri =
        UriBuilder.fromUri(service.getBaseURL()).path("Bulk").build();
    final Resource clientResource = client.resource(uri);
    clientResource.accept(acceptType);
    clientResource.contentType(contentType);

    final StreamingOutput output = new StreamingOutput()
    {
      public void write(final OutputStream outputStream)
          throws IOException, WebApplicationException
      {
        try
        {
          marshaller.bulkMarshal(outputStream, failOnErrors, operations);
        }
        catch (Exception e)
        {
          throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }
      }
    };

    ClientResponse response = null;
    InputStream entity = null;
    try
    {
      response = clientResource.post(output);
      entity = response.getEntity(InputStream.class);

      if(response.getStatusType() == Response.Status.OK)
      {
        final BulkConfig bulkConfig =
            new BulkConfig(true, operations.size(), Long.MAX_VALUE);
        unmarshaller.bulkUnmarshal(entity, bulkConfig, handler);
      }
      else
      {
        throw createErrorResponseException(response, entity);
      }
    }
    catch(SCIMException e)
    {
      throw e;
    }
    catch(Exception e)
    {
      throw SCIMException.createException(SCIMEndpoint.getStatusCode(e),
                               SCIMEndpoint.getExceptionMessage(e), e);
    }
    finally
    {
      try
      {
        if (entity != null)
        {
          entity.close();
        }
      }
      catch (IOException e)
      {
        // Let's just log this and ignore.
        Debug.debugException(e);
      }
    }
  }



  /**
   * Returns a SCIM exception representing the error response.
   *
   * @param response  The client response.
   * @param entity    The response content.
   *
   * @return  The SCIM exception representing the error response.
   */
  private SCIMException createErrorResponseException(
      final ClientResponse response,
      final InputStream entity)
  {
    SCIMException scimException = null;

    if (entity != null)
    {
      try
      {
        scimException = unmarshaller.unmarshalError(entity);
      }
      catch (InvalidResourceException e)
      {
        // The response content could not be parsed as a SCIM error
        // response, which is the case if the response is a more general
        // HTTP error. It is better to just provide the HTTP response
        // details in this case.
        Debug.debugException(e);
      }
    }

    if (scimException == null)
    {
      scimException = SCIMException.createException(
          response.getStatusCode(), response.getMessage());
    }

    return scimException;
  }
}
