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

package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.QueryRequest;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.sdk.ListResponse;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.BulkOperation;

import java.io.OutputStream;
import java.util.List;



/**
 * This class provides a SCIM object marshaller implementation to write SCIM
 * objects to their XML representation.
 */
public class XmlMarshaller implements Marshaller
{
  /**
   * {@inheritDoc}
   */
  public void marshal(final BaseResource resource,
                      final OutputStream outputStream)
      throws SCIMException
  {
    final XmlStreamMarshaller streamMarshaller =
        new XmlStreamMarshaller(outputStream);
    try
    {
      streamMarshaller.marshal(resource);
    }
    finally
    {
      streamMarshaller.close();
    }
  }


  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMException response,
                      final OutputStream outputStream)
      throws SCIMException
  {
    final XmlStreamMarshaller streamMarshaller =
        new XmlStreamMarshaller(outputStream);
    try
    {
      streamMarshaller.marshal(response);
    }
    finally
    {
      streamMarshaller.close();
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final Resources<? extends BaseResource> resources,
                      final OutputStream outputStream)
      throws SCIMException
  {
    final XmlStreamMarshaller streamMarshaller =
        new XmlStreamMarshaller(outputStream);
    try
    {
      streamMarshaller.marshal(resources);
    }
    finally
    {
      streamMarshaller.close();
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
    final XmlStreamMarshaller streamMarshaller =
        new XmlStreamMarshaller(outputStream);
    try
    {
      streamMarshaller.bulkMarshal(failOnErrors, operations);
    }
    finally
    {
      streamMarshaller.close();
    }
  }


  /**
   * Not available with XML.
   * {@inheritDoc}
   */
  @Override
  public void marshal(final QueryRequest request,
                      final OutputStream outputStream) throws SCIMException {
    throw new UnsupportedOperationException();

  }


  /**
   * Not available with XML.
   * {@inheritDoc}
   */
  @Override
  public void marshal(final ListResponse<? extends BaseResource> response,
                      final OutputStream outputStream)
      throws SCIMException
  {
    throw new UnsupportedOperationException();
  }
}
