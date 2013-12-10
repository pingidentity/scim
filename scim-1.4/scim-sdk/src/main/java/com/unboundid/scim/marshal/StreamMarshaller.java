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

/*
 * Copyright 2011-2013 UnboundID Corp.
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

package com.unboundid.scim.marshal;


import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMException;

import java.util.List;
import java.util.Set;



/**
 * This interface provides methods that may be used to write a stream of
 * SCIM objects to an external representation. There are stream marshaller
 * implementations for XML and JSON. Stream marshaller implementations are
 * not required to be thread-safe.
 */
public interface StreamMarshaller
{
  /**
   * Write a SCIM object.
   *
   * @param resource      The SCIM resource to be written.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void marshal(BaseResource resource)
    throws SCIMException;

  /**
   * Write a SCIM query response.
   *
   * @param response      The SCIM response to be written.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void marshal(Resources<? extends BaseResource> response)
    throws SCIMException;

  /**
   * Write a SCIM error response.
   *
   * @param response      The SCIM response to be written.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void marshal(SCIMException response)
    throws SCIMException;

  /**
   * Write the content of a SCIM bulk operation request or response.
   *
   * @param failOnErrors  The value of failOnErrors, or -1 to not provide a
   *                      value.
   * @param operations    The bulk operations to include in the content.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void bulkMarshal(int failOnErrors,
                   List<BulkOperation> operations)
    throws SCIMException;



  /**
   * Write the start of a bulk request or response.
   *
   * @param failOnErrors  The value of failOnErrors, or -1 to not provide a
   *                      value.
   * @param schemaURIs    The set of schema URIs used by the bulk request or
   *                      response.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void writeBulkStart(final int failOnErrors,
                      final Set<String> schemaURIs)
      throws SCIMException;



  /**
   * Write a bulk operation to a bulk request or response.
   *
   * @param o  The bulk operation to write.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void writeBulkOperation(final BulkOperation o)
      throws SCIMException;



  /**
   * Write the end of a bulk request or response.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void writeBulkFinish()
      throws SCIMException;



  /**
   * Close the marshaller.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void close()
      throws SCIMException;
}
