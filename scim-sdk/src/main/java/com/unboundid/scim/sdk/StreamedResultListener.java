/*
 * Copyright 2014-2015 UnboundID Corp.
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


import com.unboundid.scim.data.BaseResource;

/**
 * Interface for getting results from a SCIM Streamed Query request.
 */
public interface StreamedResultListener {

  /**
   * Handle a single resource returned from the SCIM query.
   * @param resource SCIM Resource returned by the query.
   * @throws SCIMException on error handling the resource.
   */
  void handleResult(final BaseResource resource) throws SCIMException;

  /**
   * Sets the value of the resume token that may be used to get the
   * next page of query results.
   * @param tokenValue The token value.  An empty string indicates
   *                   that there are no more results from the query.
   */
  void setResumeToken(final String tokenValue);

  /**
   * Sets the total number of results that match the streamed query
   * search criteria.  Does not account for filtering by authorization
   * policy.
   * @param totalResults the total number of entries that match the
   *                     search criteria
   */
  void setTotalResults(final int totalResults);
}
