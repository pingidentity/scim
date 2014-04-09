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

import com.unboundid.scim.sdk.BulkOperation.Method;

/**
 * A wrapper exception for SCIMException for individual bulk operations.
 */
public class BulkException extends Exception
{
  private static final long serialVersionUID = -734244253288139013L;

  private final Method method;
  private final String bulkId;
  private final String path;

  /**
   *
   * @param throwable The SCIMException that caused the bulk operation to fail.
   * @param method The original HTTP method of the operation.
   * @param bulkId The bulk operation identifier.
   * @param path   The relative path of the resource from the bulk operation
   *               request.
   */
  public BulkException(final SCIMException throwable, final Method method,
                       final String bulkId, final String path)
  {
    super(throwable);
    this.method = method;
    this.bulkId = bulkId;
    this.path   = path;
  }



  /**
   * Retrieve HTTP method of the operation. Possible values are POST, PUT,
   * PATCH or DELETE.
   * @return  The HTTP method of the operation. Possible values are POST, PUT,
   *          PATCH or DELETE.
   */
  public Method getMethod()
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
   * Retrieve the relative path of the resource from the bulk operation request.
   * Could be {@code null} if no path was specified.
   *
   * @return  The relative path of the resource from the bulk operation request,
   *          or {@code null} if no path was specified.
   */
  public String getPath()
  {
    return path;
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
