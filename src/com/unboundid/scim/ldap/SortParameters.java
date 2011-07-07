/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMAttributeType;



/**
 * This class represents the sorting parameters in a SCIM request.
 */
public final class SortParameters
{
  /**
   * The attribute whose value is used to order the returned resources.
   */
  private final SCIMAttributeType sortBy;

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
   * @param sortBy    The attribute whose value is used to order the returned
   *                  resources.
   * @param sortOrder The order in which the sortBy parameter is applied. e.g.
   *                  ascending or descending, or {@code null} if no sort order
   *                  was specified.
   */
  public SortParameters(final SCIMAttributeType sortBy, final String sortOrder)
  {
    this.sortBy           = sortBy;
    this.sortOrder        = sortOrder;
    this.isAscendingOrder =
        sortOrder == null || !sortOrder.equalsIgnoreCase("descending");
  }



  /**
   * Retrieve the attribute whose value is used to order the returned
   * resources.
   *
   * @return The attribute whose value is used to order the returned resources.
   */
  public SCIMAttributeType getSortBy()
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
