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
