/*
 * Copyright 2011-2014 UnboundID Corp.
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
import com.unboundid.scim.marshal.Marshaller;

import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a list of SCIM resources returned by the service provider from
 * a query/listing request.
 */
public class Resources<R extends BaseResource> implements Iterable<R>,
    SCIMResponse
{
  private final int totalResults;
  private final int startIndex;
  private final List<R> resourceList;

  /**
   * Create a new resources response from the provided list. The number of
   * total results will be set to the size of the list and the default value of
   * 1 for start index.
   *
   * @param resourceList The list of resources to create the response from.
   */
  public Resources(final List<R> resourceList)
  {
    this(resourceList, resourceList.size(), 1);
  }

  /**
   * Create a new resources response from the provided list.
   *
   * @param resourceList The list of resources to create the response from.
   * @param totalResults The total number of results matching the Consumer query
   * @param startIndex The 1-based index of the first result in the current set
   *                   of search results
   */
  public Resources(final List<R> resourceList, final int totalResults,
                   final int startIndex)
  {
    this.totalResults = totalResults;
    this.startIndex = startIndex;
    this.resourceList = resourceList;
  }

  /**
   * Retrieves the 1-based index of the first result in the current set of
   * search results.
   *
   * @return The 1-based index of the first result in the current set of
   *         search results.
   */
  public long getStartIndex()
  {
    return startIndex;
  }

  /**
   * Retrieves the total number of results matching the Consumer query.
   *
   * @return The total number of results matching the Consumer query.
   */
  public long getTotalResults()
  {
    return totalResults;
  }

  /**
   * Retrieves the number of search results returned in this query response.
   *
   * @return The number of search results returned in this query response.
   */
  public int getItemsPerPage()
  {
    return resourceList.size();
  }

  /**
   * {@inheritDoc}
   */
  public Iterator<R> iterator() {
    return resourceList.iterator();
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final Marshaller marshaller,
                      final OutputStream outputStream)
    throws Exception
  {
    marshaller.marshal(this, outputStream);
  }
}
