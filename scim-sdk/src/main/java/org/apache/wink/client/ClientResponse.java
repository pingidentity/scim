/*
 * Copyright 2011-2015 UnboundID Corp.
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

package org.apache.wink.client;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *  Wink compatibility layer class - see Wink docs.
 */
public class ClientResponse
{
  private Response response;

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param response Wink compatibility layer class - see Wink docs.
   */
  ClientResponse(final Response response)
  {
    this.response = response;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param entityClass Wink compatibility layer class - see Wink docs.
   * @param <T> Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public <T> T getEntity(final Class<T> entityClass)
  {
    T entity = response.readEntity(entityClass);

    if(entity instanceof InputStream)
    {
      byte [] buffer = new byte[1024];
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      int readLength;
      try
      {
        do
        {
          readLength = ((InputStream) entity).read(buffer);
          if (readLength > 0)
          {
            bos.write(buffer, 0, readLength);
          }
        } while (readLength != -1);

        return (T) (new ByteArrayInputStream(bos.toByteArray()));
      }
      catch (IOException ex)
      {
        throw new RuntimeException(
            "Caught exception trying to read entity.", ex);
      }
    }

    return entity;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Response.Status getStatusType()
  {
    return Response.Status.fromStatusCode(response.getStatus());
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public int getStatusCode()
  {
    return response.getStatus();
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public String getMessage()
  {
    return response.getStatusInfo().getReasonPhrase();
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @throws IOException Wink compatibility layer class - see Wink docs.
   */
  public void consumeContent() throws IOException
  {
    InputStream inputStream = getEntity(InputStream.class);
    try
    {
      while (inputStream.read(new byte[512]) != -1)
      {

      }
    }
    catch (IOException ex)
    {
      // nothing can really be done about this, so ignore it.
    }
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public MultivaluedMap<String, String> getHeaders()
  {
    return response.getStringHeaders();
  }
}
