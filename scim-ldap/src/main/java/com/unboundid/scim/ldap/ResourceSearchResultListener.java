/*
 * Copyright 2011-2013 UnboundID Corp.
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
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class provides a search result listener to retrieve SCIM objects.
 */
public class ResourceSearchResultListener implements SearchResultListener
{
  /**
   * The serial version ID required for this serializable class.
   */
  private static final long serialVersionUID = -2028867840959235911L;

  /**
   * A resource mapper to map the result entries into SCIM objects.
   */
  private final ResourceMapper resourceMapper;

  /**
   * The request that is being processed.
   */
  private final GetResourcesRequest request;

  /**
   * The SCIM objects to be returned.
   */
  private final List<BaseResource> resources;

  /**
   * An LDAP interface that can be used to derive attributes from other entries.
   */
  private final LDAPRequestInterface ldapInterface;

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
   * The requested attributes plus the filter attributes.
   */
  private final SCIMQueryAttributes attributes;



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
    this.resourceMapper =
        backend.getResourceMapper(request.getResourceDescriptor());
    this.request        = request;
    this.resources      = new ArrayList<BaseResource>();
    this.ldapInterface  = ldapInterface;
    this.maxResults     = maxResults;
    this.totalResults   = new AtomicInteger();
    this.attributes     = getFilterAttributes().merge(request.getAttributes());
  }



  /**
   * Create a SCIMQueryAttributes instance representing the attributes
   * referenced by the request filter.
   *
   * @return  A SCIMQueryAttributes instance representing the attributes
   *          referenced by the request filter.
   *
   * @throws InvalidResourceException  If there is an error constructing the
   *                                   SCIMQueryAttributes instance.
   */
  private SCIMQueryAttributes getFilterAttributes()
      throws InvalidResourceException
  {
    final List<AttributePath> attributePaths = new ArrayList<AttributePath>();
    insertFilterAttributes(request.getFilter(), attributePaths);

    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < attributePaths.size(); i++)
    {
      if (i != 0)
      {
        builder.append(',');
      }

      builder.append(attributePaths.get(i));
    }

    return new SCIMQueryAttributes(request.getResourceDescriptor(),
                                   builder.toString());
  }



  /**
   * Insert the attribute paths referenced by the provided filter into a
   * provided list of attribute paths.
   *
   * @param filter          The filter whose attribute paths are of interest.
   * @param attributePaths  The list of attribute paths to be updated.
   */
  private void insertFilterAttributes(
      final SCIMFilter filter,
      final List<AttributePath> attributePaths)
  {
    if (filter == null)
    {
      return;
    }

    switch (filter.getFilterType())
    {
      case AND:
      case OR:
        for (final SCIMFilter f : filter.getFilterComponents())
        {
          insertFilterAttributes(f, attributePaths);
        }
        break;

      default:
        attributePaths.add(filter.getFilterAttribute());
        break;
    }
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
    totalResults.incrementAndGet();

    if (resources.size() >= maxResults)
    {
      return;
    }

    try
    {
      // Get the request and filter attributes so we can filter on them.
      final SCIMObject scimObject =
          resourceMapper.toSCIMObject(searchEntry, attributes, ldapInterface);
      final BaseResource resource =
          new BaseResource(request.getResourceDescriptor(), scimObject);

      LDAPBackend.setIdAndMetaAttributes(resourceMapper, resource, request,
                                         searchEntry, null);

      if (request.getFilter() == null ||
          scimObject.matchesFilter(request.getFilter()))
      {
        if (request.getAttributes().allAttributesRequested())
        {
          resources.add(resource);
        }
        else
        {
          // Keep only the requested attributes.
          final SCIMObject paredObject =
              request.getAttributes().pareObject(scimObject);
          final BaseResource returnedResource =
              new BaseResource(request.getResourceDescriptor(), paredObject);

          resources.add(returnedResource);
        }
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
   * @return  Th number of LDAP entries returned.
   */
  public int getTotalResults()
  {
    return totalResults.get();
  }
}
