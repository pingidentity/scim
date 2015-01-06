/*
 * Copyright 2011-2015 UnboundID Corp.
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

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultListener;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.SCIMException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class provides a search result listener to retrieve SCIM objects.
 */
public class ResourceSearchResultListener extends SCIMSearchResultListener
    implements SearchResultListener
{
  /**
   * The serial version ID required for this serializable class.
   */
  private static final long serialVersionUID = -2028867840959235911L;

  /**
   * The SCIM objects to be returned.
   */
  private final List<BaseResource> resources;


  /**
   * The maximum number of resources that may be returned.
   */
  private final int maxResults;

  /**
   * The total number of resources that were actually returned from the
   * LDAP search (this may be more than we are allowed to return to the
   * SCIM client).
   */
  private final AtomicInteger totalResults;



  /**
   * Create a new search result listener to retrieve SCIM objects.
   *
   * @param backend        The LDAP backend that is processing the SCIM request.
   * @param request        The request that is being processed.
   * @param ldapInterface  An LDAP interface that can be used to
   *                       derive attributes from other entries.
   * @param maxResults     The maximum number of resources that may be returned.
   *
   * @throws SCIMException  Should never be thrown.
   */
  public ResourceSearchResultListener(final LDAPBackend backend,
                                      final GetResourcesRequest request,
                                      final LDAPRequestInterface ldapInterface,
                                      final int maxResults)
      throws SCIMException
  {
    super(backend, request, ldapInterface);
    this.resources      = new ArrayList<BaseResource>();
    this.maxResults     = maxResults;
    this.totalResults   = new AtomicInteger();
  }



  /**
   * Indicates that the provided search result entry has been returned by the
   * server and may be processed by this search result listener.
   *
   * @param searchEntry The search result entry that has been returned by the
   *                    server.
   */
  public void searchEntryReturned(final SearchResultEntry searchEntry)
  {
    if (resources.size() >= maxResults)
    {
      totalResults.incrementAndGet();
      return;
    }

    try
    {
      BaseResource resource = getResourceForSearchResultEntry(searchEntry);
      if (resource != null)
      {
        totalResults.incrementAndGet();
        resources.add(resource);
      }
    }
    catch (SCIMException e)
    {
      Debug.debugException(e);
      // TODO: We should find a way to get this exception back to LDAPBackend.
    }
  }



  /**
   * Indicates that the provided search result reference has been returned by
   * the server and may be processed by this search result listener.
   *
   * @param searchReference The search result reference that has been returned
   *                        by the server.
   */
  public void searchReferenceReturned(
      final SearchResultReference searchReference)
  {
    // No implementation currently required.
  }



  /**
   * Retrieve the SCIM objects to be returned.
   *
   * @return  The SCIM objects to be returned.
   */
  public List<BaseResource> getResources()
  {
    return resources;
  }


  /**
   * Retrieve the total number of LDAP entries that were returned from the
   * search.
   *
   * @return  The number of LDAP entries returned.
   */
  public int getTotalResults()
  {
    return totalResults.get();
  }
}
