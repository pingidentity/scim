/*
 * Copyright 2011-2025 Ping Identity Corporation
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

import java.util.concurrent.atomic.AtomicBoolean;
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
   * Whether to perform schema checking.
   */
  private final AtomicBoolean checkSchema = new AtomicBoolean(true);



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



  /**
   * Whether to perform schema checking.
   *
   * @return {@code true} to perform schema checking and
   *         {@code false} otherwise.
   */
  public boolean isCheckSchema()
  {
    return checkSchema.get();
  }



  /**
   * Specify whether to perform schema checking.
   *
   * @param checkSchema {@code true} to perform schema checking and
   *                    {@code false} otherwise.
   */
  public void setCheckSchema(final boolean checkSchema)
  {
    this.checkSchema.set(checkSchema);
  }
}
