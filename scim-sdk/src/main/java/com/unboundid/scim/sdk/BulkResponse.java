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

import com.unboundid.scim.marshal.Marshaller;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 * This class represents the response to a bulk request.
 */
public class BulkResponse implements SCIMResponse, Iterable<BulkOperation>
{
  private final List<BulkOperation> operations;



  /**
   * Create a new bulk response.
   *
   * @param operations  The operations responses.
   */
  public BulkResponse(final List<BulkOperation> operations)
  {
    this.operations = new ArrayList<BulkOperation>(operations);
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final Marshaller marshaller,
                      final OutputStream outputStream)
      throws Exception
  {
    marshaller.bulkMarshal(outputStream, -1, operations);
  }



  /**
   * {@inheritDoc}
   */
  public Iterator<BulkOperation> iterator()
  {
    return operations.iterator();
  }
}
