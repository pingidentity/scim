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

/**
 * This class represents the response status of an individual operation within
 * a bulk operation.
 */
public class Status
{
  /**
   * The HTTP response code that would have been returned if a single HTTP
   * request had been used.
   */
  private final String code;

  /**
   * A human readable error message, required if the operation was unsuccessful.
   */
  private final String description;



  /**
   * Construct a new Status value.
   *
   * @param code         The HTTP response code that would have been returned
   *                     if a single HTTP request had been used.
   * @param description  A human readable error message, or {@code null} if
   *                     the operation was successful and there is no additional
   *                     information.
   */
  public Status(final String code, final String description)
  {
    this.code = code;
    this.description = description;
  }



  /**
   * Retrieve the HTTP response code that would have been returned if a
   * single HTTP request had been used.
   *
   * @return  The HTTP response code that would have been returned if a
   *          single HTTP request had been used.
   */
  public String getCode()
  {
    return code;
  }



  /**
   * Retrieve the human readable error message, or {@code null} if the
   * operation was successful and there is no additional information.
   *
   * @return  The human readable error message, or {@code null} if the
   *          operation was successful and there is no additional information.
   */
  public String getDescription()
  {
    return description;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("Status");
    sb.append("{code='").append(code).append('\'');
    sb.append(", description='").append(description).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
