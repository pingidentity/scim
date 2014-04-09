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



import com.unboundid.scim.schema.ResourceDescriptor;



/**
 * This class must be extended to handle the content of a bulk operation.
 */
public abstract class BulkContentHandler
{
  /**
   * Handles the value of failOnErrors.
   *
   * @param failOnErrors  The number of errors that the Service Provider will
   *                      accept before the operation is terminated and an
   *                      error response is returned.
   */
  public void handleFailOnErrors(final int failOnErrors)
  {
    // No implementation by default.
  }



  /**
   * Handle an individual operation.
   *
   * @param opIndex        The index of the operation.
   * @param bulkOperation  The individual operation within the bulk operation.
   *
   * @throws SCIMException  If an error occurs that prevents processing of the
   *                        entire bulk content.
   * @throws BulkException  If an error occurs while processing the individual
   *                        operation within the bulk operation.
   */
  public void handleOperation(final int opIndex,
                              final BulkOperation bulkOperation)
      throws BulkException, SCIMException
  {
    // No implementation by default.
  }



  /**
   * Handle an exception encountered when processing an individual operation.
   *
   * @param opIndex        The index of the operation.
   * @param bulkException  The exception encountered when processing an
   *                       individual operation within the bulk operation.
   *
   * @return  {@code true} if operations should continue to be provided,
   *          or {@code false} if the remaining operations are of no interest.
   *
   * @throws SCIMException  If an error occurs that prevents processing of the
   *                        entire bulk content.
   */
  public boolean handleException(final int opIndex,
                                 final BulkException bulkException)
      throws SCIMException
  {
    throw bulkException.getCause();
  }



  /**
   * Retrieve the resource descriptor for a given endpoint.
   *
   * @param endpoint  A SCIM resource endpoint.
   *
   * @return  The resource descriptor for this endpoint, or {@code null} if the
   *          endpoint is unknown.
   */
  public ResourceDescriptor getResourceDescriptor(final String endpoint)
  {
    // Null implementation by default.
    return null;
  }



  /**
   * Transform a data value. This method may be used to resolve bulkId
   * references to resource IDs.
   *
   * @param opIndex  The index of the bulk operation containing the data value.
   * @param value    The value to be transformed.
   *
   * @return  The transformed value.
   */
  public String transformValue(final int opIndex, final String value)
  {
    // No-op by default.
    return value;
  }
}
