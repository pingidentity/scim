/*
 * Copyright 2011-2012 UnboundID Corp.
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
 * This class represents the pagination parameters in a SCIM request.
 */
public final class PageParameters
{
  /**
   * The offset of the first resource to be returned.
   */
  private final long startIndex;

  /**
   * The maximum number of resources to return in a single page, or zero if the
   * server should choose how many to return.
   */
  private final int count;



  /**
   * Create a new instance of pagination parameters.
   *
   * @param startIndex The offset of the first resource to be returned.
   * @param count      The maximum number of resources to return in a single
   *                   page, or zero if the server should choose how many to
   *                   return.
   */
  public PageParameters(final long startIndex, final int count)
  {
    this.startIndex = startIndex;
    this.count = count;
  }



  /**
   * Retrieve the offset of the first resource to be returned.
   *
   * @return The offset of the first resource to be returned.
   */
  public long getStartIndex()
  {
    return startIndex;
  }



  /**
   * Retrieve the maximum number of resources to return in a single page, or
   * zero if the server should choose how many to return.
   *
   * @return The maximum number of resources to return in a single page, or
   *         zero if the server should choose how many to return.
   */
  public int getCount()
  {
    return count;
  }
}
