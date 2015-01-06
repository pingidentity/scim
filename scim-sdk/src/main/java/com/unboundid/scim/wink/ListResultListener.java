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


package com.unboundid.scim.wink;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.sdk.StreamedResultListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of StreamedResultListener that stores the results
 * in a list.
 */
public class ListResultListener implements StreamedResultListener
{
  // this list must be synchronized as handleResult may be called
  // from multiple threads.
  private List<BaseResource> resources =
      Collections.synchronizedList(new ArrayList<BaseResource>());
  private String resumeToken = null;
  private int totalResults;

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleResult(final BaseResource resource)
  {
    resources.add(resource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setResumeToken(final String tokenValue)
  {
    this.resumeToken = tokenValue;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setTotalResults(final int totalResults)
  {
    this.totalResults = totalResults;
  }

  /**
   * Get the resources that have been collected by this listener.
   * @return a list of SCIM resource objects.
   */
  public List<BaseResource> getResources()
  {
    return Collections.unmodifiableList(resources);
  }

  /**
   * Get the count of resources that have been collected by this listener.
   * @return the number of SCIM resources
   */
  public int getResourceCount()
  {
    return resources.size();
  }

  /**
   * Get the resume token that can be used to retrieve the next page
   * of results for the current query.
   * @return resume token value.   An empty or null token indicates
   * that there are no more results from the current query.
   */
  public String getResumeToken()
  {
    return resumeToken;
  }

  /**
   * Gets the total number of results that match the streamed query
   * search criteria.  Does not account for filtering by authorization
   * policy, so the actual number returned may be fewer.
   * @return the total number of results that match the search criteria.
   */
  public int getTotalResults()
  {
    return totalResults;
  }
}
