/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;



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
