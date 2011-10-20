/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import java.util.concurrent.atomic.AtomicInteger;



/**
 * This class contains mutable configuration settings for a SCIM Backend.
 */
public class SCIMBackendConfig
{
  /**
   * The maximum number of resources that are returned in a response.
   */
  private final AtomicInteger maxResults = new AtomicInteger(Integer.MAX_VALUE);



  /**
   * Retrieve the maximum number of resources that are returned in a response.
   * @return  The maximum number of resources that are returned in a response.
   */
  public int getMaxResults()
  {
    return maxResults.intValue();
  }



  /**
   * Specify the maximum number of resources that are returned in a response.
   * @param maxResults  The maximum number of resources that are returned in
   *                    a response.
   */
  public void setMaxResults(final int maxResults)
  {
    this.maxResults.set(maxResults);
  }
}
