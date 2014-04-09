/*
 * Copyright 2012-2014 UnboundID Corp.
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

import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.StreamMarshaller;
import com.unboundid.scim.marshal.json.JsonStreamMarshaller;
import com.unboundid.scim.marshal.xml.XmlStreamMarshaller;
import com.unboundid.scim.wink.RequestContext;
import com.unboundid.scim.wink.SCIMApplication;

import javax.ws.rs.core.MediaType;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;



/**
 * Implements a SCIMResponse to handle bulk responses without keeping the
 * entire response in memory.
 */
public class BulkStreamResponse implements SCIMResponse
{
  private final File file;
  private final StreamMarshaller streamMarshaller;


  /**
   * Create a new bulk stream response.
   *
   * @param application     The SCIM JAX-RS application.
   * @param requestContext  The bulk request context.
   *
   * @throws SCIMException  If the bulk stream response could not be created.
   */
  public BulkStreamResponse(final SCIMApplication application,
                            final RequestContext requestContext)
      throws SCIMException
  {
    try
    {
      file = File.createTempFile(
          "scim-bulk-response-",
          "." + requestContext.getProduceMediaType().getSubtype(),
          application.getTmpDataDir());
      file.deleteOnExit();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot create a temporary file for the bulk response" +
          e.getMessage());
    }

    final OutputStream outputStream;
    try
    {
      outputStream = new BufferedOutputStream(new FileOutputStream(file));
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      if (!file.delete())
      {
        Debug.debug(Level.WARNING, DebugType.OTHER,
                    "Could not delete temporary file " +
                    file.getAbsolutePath());
      }

      throw new ServerErrorException(
          "Cannot create output stream for temporary file '" + file + "': " +
          e.getMessage());
    }

    try
    {
      if (requestContext.getProduceMediaType().equals(
          MediaType.APPLICATION_JSON_TYPE))
      {
        streamMarshaller = new JsonStreamMarshaller(outputStream);
      }
      else
      {
        streamMarshaller = new XmlStreamMarshaller(outputStream);
      }

      // Bulk responses contain no data so there is only core schema.
      final Set<String> schemaURIs =
          Collections.singleton(SCIMConstants.SCHEMA_URI_CORE);
      streamMarshaller.writeBulkStart(-1, schemaURIs);
    }
    catch (SCIMException e)
    {
      Debug.debugException(e);

      try
      {
        outputStream.close();
      }
      catch (Exception e1)
      {
        Debug.debugException(e1);
      }
      finally
      {
        if (!file.delete())
        {
          Debug.debug(Level.WARNING, DebugType.OTHER,
                      "Could not delete temporary file " +
                      file.getAbsolutePath());
        }
      }

      throw e;
    }
  }



  /**
   * Write a bulk operation to the response.
   *
   * @param o  The bulk operation to write.
   *
   * @throws SCIMException  If the bulk operation could not be written.
   */
  public void writeBulkOperation(final BulkOperation o)
      throws SCIMException
  {
    streamMarshaller.writeBulkOperation(o);
  }



  /**
   * Release resources when this bulk stream response is no longer needed.
   */
  public void finalizeResponse()
  {
    if (file.exists() && !file.delete())
    {
      Debug.debug(Level.WARNING, DebugType.OTHER,
                  "Could not delete temporary file " +
                  file.getAbsolutePath());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void marshal(final Marshaller marshaller,
                      final OutputStream outputStream)
      throws Exception
  {
    try
    {
      // Finish writing the response to the temporary file.
      streamMarshaller.writeBulkFinish();
      streamMarshaller.close();

      // Copy the temporary file to the output stream.
      final FileInputStream inputStream = new FileInputStream(file);
      final byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1)
      {
        outputStream.write(buffer, 0, bytesRead);
      }
    }
    finally
    {
      // Delete the temporary response file.
      finalizeResponse();
    }
  }
}
