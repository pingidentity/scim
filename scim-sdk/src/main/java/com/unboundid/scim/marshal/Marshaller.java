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

package com.unboundid.scim.marshal;


import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.QueryRequest;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.ListResponse;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMException;

import java.io.OutputStream;
import java.util.List;



/**
 * This interface provides methods that may be used to write SCIM objects
 * to an external representation. There are marshaller implementations
 * for XML and JSON. Marshaller implementations are required to be thread-safe.
 */
public interface Marshaller
{
  /**
   * Write a SCIM object to an output stream.
   *
   * @param resource      The SCIM resource to be written.
   * @param outputStream  The output stream to which the SCIM object should
   *                      be written.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void marshal(BaseResource resource, OutputStream outputStream)
    throws SCIMException;

  /**
   * Write a SCIM listing response to an output stream.
   *
   * @param response      The SCIM response to be written.
   * @param outputStream  The output stream to which the SCIM response should
   *                      be written.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void marshal(Resources<? extends BaseResource> response,
               OutputStream outputStream)
    throws SCIMException;

  /**
   * Write a SCIM streamed listing response to an output stream.
   *
   * @param response       The SCIM response to be written.
   * @param outputStream   The output stream to which the SCIM response should
   *                        be written.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void marshal(ListResponse<? extends BaseResource> response,
               OutputStream outputStream)
    throws SCIMException;

  /**
   * Write a SCIM 2.0-style query request to an output stream.
   *
   * @param request         The SCIM query request to be written.
   * @param outputStream    The output stream to which the SCIM query
   *                        request should be written.
   * @throws SCIMException  If the data could not be written.
   */
  void marshal(QueryRequest request, OutputStream outputStream)
    throws SCIMException;

  /**
   * Write a SCIM error response to an output stream.
   *
   * @param response      The SCIM response to be written.
   * @param outputStream  The output stream to which the SCIM response should
   *                      be written.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void marshal(SCIMException response, OutputStream outputStream)
    throws SCIMException;

  /**
   * Write the content of a SCIM bulk operation request or response to an
   * output stream.
   *
   * @param outputStream  The output stream to which the content should be
   *                      written.
   * @param failOnErrors  The value of failOnErrors, or -1 to not provide a
   *                      value.
   * @param operations    The bulk operations to include in the content.
   *
   * @throws SCIMException  If the data could not be written.
   */
  void bulkMarshal(OutputStream outputStream, int failOnErrors,
                   List<BulkOperation> operations)
    throws SCIMException;
}
