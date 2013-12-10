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

package com.unboundid.scim.sdk;



/**
 * This class represents the sorting parameters in a SCIM request.
 */
public final class SortParameters
{
  /**
   * The attribute or sub-attribute whose value is used to order the returned
   * resources.
   */
  private final AttributePath sortBy;

  /**
   * The order in which the sortBy parameter is applied. e.g. ascending or
   * descending, or {@code null} if no sort order was specified.
   */
  private final String sortOrder;

  /**
   * Indicates whether the sort order implies ascending or descending order.
   */
  private final boolean isAscendingOrder;



  /**
   * Create a new instance of sort parameters.
   *
   * @param sortBy    The attribute or sub-attribute whose value is used to
   *                  order the returned resources.
   * @param sortOrder The order in which the sortBy parameter is applied. e.g.
   *                  ascending or descending, or {@code null} if no sort order
   *                  was specified.
   */
  public SortParameters(final String sortBy, final String sortOrder)
  {
    this(sortBy, sortOrder, SCIMConstants.SCHEMA_URI_CORE);
  }



  /**
   * Create a new instance of sort parameters.
   *
   * @param sortBy    The attribute or sub-attribute whose value is used to
   *                  order the returned resources.
   * @param sortOrder The order in which the sortBy parameter is applied. e.g.
   *                  ascending or descending, or {@code null} if no sort order
   *                  was specified.
   * @param defaultSchema The default schema that should be assumed when parsing
   *                      attributes without the schema explicitly defined in
   *                      the URN.
   */
  public SortParameters(final String sortBy, final String sortOrder,
                        final String defaultSchema)
  {
    this(AttributePath.parse(sortBy, defaultSchema), sortOrder);
  }



  /**
   * Create a new instance of sort parameters.
   *
   * @param sortBy    The attribute or sub-attribute whose value is used to
   *                  order the returned resources.
   * @param sortOrder The order in which the sortBy parameter is applied. e.g.
   *                  ascending or descending, or {@code null} if no sort order
   *                  was specified.
   */
  public SortParameters(final AttributePath sortBy, final String sortOrder)
  {
    this.sortBy           = sortBy;
    this.sortOrder        = sortOrder;
    this.isAscendingOrder =
        sortOrder == null || !sortOrder.equalsIgnoreCase("descending");
  }



  /**
   * Retrieve the attribute or sub-attribute whose value is used to order the
   * returned resources.
   *
   * @return The attribute or sub-attribute whose value is used to order the
   *         returned resources.
   */
  public AttributePath getSortBy()
  {
    return sortBy;
  }



  /**
   * Retrieve order in which the sortBy parameter is applied. e.g. ascending or
   * descending.
   *
   * @return The order in which the sortBy parameter is applied. e.g. ascending
   *         or descending.
   */
  public String getSortOrder()
  {
    return sortOrder;
  }



  /**
   * Determine whether the sort order implies ascending or descending order.
   *
   * @return {@code true} if the sort order implies ascending order.
   */
  public boolean isAscendingOrder()
  {
    return isAscendingOrder;
  }
}
