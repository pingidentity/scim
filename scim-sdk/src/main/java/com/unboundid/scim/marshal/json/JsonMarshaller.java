/*
 * Copyright 2011-2014 UnboundID Corp.
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

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMException;

import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * This class provides a SCIM object marshaller implementation to write SCIM
 * objects to their Json representation.
 */
public class JsonMarshaller implements Marshaller
{
  /**
   * {@inheritDoc}
   */
  public void marshal(final BaseResource resource,
                      final OutputStream outputStream)
      throws SCIMException
  {
    final JsonStreamMarshaller jsonStreamMarshaller =
        new JsonStreamMarshaller(outputStream);
    try
    {
      jsonStreamMarshaller.marshal(resource);
    }
    finally
    {
      jsonStreamMarshaller.close();
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final Resources<? extends BaseResource> response,
                      final OutputStream outputStream)
      throws SCIMException
  {
    final JsonStreamMarshaller jsonStreamMarshaller =
        new JsonStreamMarshaller(outputStream);
    try
    {
      jsonStreamMarshaller.marshal(response);
    }
    finally
    {
      jsonStreamMarshaller.close();
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMException response,
                      final OutputStream outputStream)
      throws SCIMException
  {
    final JsonStreamMarshaller jsonStreamMarshaller =
        new JsonStreamMarshaller(outputStream);
    try
    {
      jsonStreamMarshaller.marshal(response);
    }
    finally
    {
      jsonStreamMarshaller.close();
    }
  }



  /**
   * {@inheritDoc}
   */
  public void bulkMarshal(final OutputStream outputStream,
                          final int failOnErrors,
                          final List<BulkOperation> operations)
      throws SCIMException
  {
    final JsonStreamMarshaller jsonStreamMarshaller =
        new JsonStreamMarshaller(outputStream);
    try
    {
      // Figure out what schemas are referenced by the resources.
      final Set<String> schemaURIs = new HashSet<String>();
      for (final BulkOperation o : operations)
      {
        final BaseResource resource = o.getData();
        if (resource != null)
        {
          schemaURIs.addAll(
              o.getData().getResourceDescriptor().getAttributeSchemas());
        }
      }

      jsonStreamMarshaller.writeBulkStart(failOnErrors, schemaURIs);
      for (final BulkOperation o : operations)
      {
        jsonStreamMarshaller.writeBulkOperation(o);
      }

      jsonStreamMarshaller.writeBulkFinish();
    }
    finally
    {
      jsonStreamMarshaller.close();
    }
  }
}
