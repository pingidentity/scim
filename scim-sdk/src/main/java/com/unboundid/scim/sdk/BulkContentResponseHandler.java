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

package com.unboundid.scim.sdk;

import java.util.ArrayList;
import java.util.List;



/**
 * This class implements the bulk operation handler to process bulk operation
 * responses in the SCIM client. This handler accumulates all the operation
 * responses in memory.
 */
public class BulkContentResponseHandler extends BulkContentHandler
{
  private final List<BulkOperation> operations = new ArrayList<BulkOperation>();

  /**
   * {@inheritDoc}
   */
  public boolean handleOperation(final int opIndex,
                                 final BulkOperation bulkOperation)
      throws SCIMException
  {
    operations.add(bulkOperation);
    return true;
  }



  /**
   * Retrieve the list of operation responses.
   *
   * @return  The list of operation responses.
   */
  public List<BulkOperation> getOperations()
  {
    return operations;
  }
}
