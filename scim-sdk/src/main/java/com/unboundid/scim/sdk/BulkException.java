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

/**
 * A wrapper exception for SCIMException for individual bulk operations.
 */
public class BulkException extends Exception
{
  private final String method;
  private final String bulkId;
  private final String location;

  /**
   *
   * @param throwable The SCIMException that caused the bulk operation to fail.
   * @param method The original HTTP method of the operation.
   * @param bulkId The bulk operation identifier.
   * @param location The resource endpoint URL.
   */
  public BulkException(final SCIMException throwable, final String method,
                       final String bulkId, final String location)
  {
    super(throwable);
    this.method = method;
    this.bulkId = bulkId;
    this.location = location;
  }



  /**
   * Retrieve HTTP method of the operation. Possible values are POST, PUT,
   * PATCH or DELETE.
   * @return  The HTTP method of the operation. Possible values are POST, PUT,
   *          PATCH or DELETE.
   */
  public String getMethod()
  {
    return method;
  }



  /**
   * Retrieve the bulk operation identifier, required when the method is POST.
   * @return  The bulk operation identifier, required when the method is POST.
   */
  public String getBulkId()
  {
    return bulkId;
  }



  /**
   * Retrieve the resource endpoint URL, or {@code null} if this is a request,
   * or if this is the response to a failed POST operation.
   * @return  The resource endpoint URL, or {@code null} if this is a request,
   *          or if this is the response to a failed POST operation.
   */
  public String getLocation()
  {
    return location;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SCIMException getCause()
  {
    return (SCIMException)super.getCause();
  }
}
