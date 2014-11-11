/*
 * Copyright 2014 UnboundID Corp.
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
import com.unboundid.scim.sdk.GetStreamedResourcesRequest;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.StreamedResultListener;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * An LDAP SearchResultListener implementation that maps LDAP to SCIM and
 * then streams the results to an upstream SCIM SearchResultListener.
 */
public class StreamingSearchResultListener extends SCIMSearchResultListener
    implements SearchResultListener
{

  private StreamedResultListener upstreamListener;
  private final AtomicInteger totalResults;

  /**
   * Create a new StreamingSearchResultListener.
   *
   * @param backend        The LDAP backend that is processing the SCIM request.
   * @param request        The request that is being processed.
   * @param ldapInterface  An LDAP interface that can be used to
   *                       derive attributes from other entries.
   * @param upstreamListener  upstream listener that is to receive
   *                          SCIM resource objects.
   *
   * @throws com.unboundid.scim.sdk.SCIMException  Should never be thrown.
   */
  public StreamingSearchResultListener(
      final LDAPBackend backend,
      final GetStreamedResourcesRequest request,
      final LDAPRequestInterface ldapInterface,
      final StreamedResultListener upstreamListener)
      throws SCIMException
  {
    super(backend, request, ldapInterface);
    this.upstreamListener = upstreamListener;
    this.totalResults = new AtomicInteger();
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
    try
    {
      BaseResource resource = getResourceForSearchResultEntry(searchEntry);
      if (resource != null)
      {
        totalResults.incrementAndGet();
        upstreamListener.handleResult(resource);
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
   * Retrieve the total number of LDAP entries that have been returned from the
   * search.
   *
   * @return  The number of LDAP entries returned.
   */
  public int getTotalResults()
  {
    return totalResults.get();
  }
}
