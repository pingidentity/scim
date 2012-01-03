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

import com.unboundid.scim.schema.ResourceDescriptor;

import java.net.URI;



/**
 * This class represents a SCIM Get Resources request to retrieve selected
 * resources.
 */
public final class GetResourcesRequest extends ResourceReturningRequest
{
  /**
   * The filter parameters of the request.
   */
  private final SCIMFilter filter;

  /**
   * The sorting parameters of the request.
   */
  private final SortParameters sortParameters;

  /**
   * The pagination parameters of the request.
   */
  private final PageParameters pageParameters;



  /**
   * Create a new SCIM Get Resource request from the provided information.
   *
   * @param baseURL               The base URL for the SCIM service.
   * @param authenticatedUserID   The authenticated user name or {@code null} if
   *                              the request is not authenticated.
   * @param resourceDescriptor    The ResourceDescriptor associated with this
   *                              request.
   * @param filter                The filter parameters of the request.
   * @param sortParameters        The sorting parameters of the request.
   * @param pageParameters        The pagination parameters of the request.
   * @param attributes            The set of requested attributes.
   */
  public GetResourcesRequest(final URI baseURL,
                             final String authenticatedUserID,
                             final ResourceDescriptor resourceDescriptor,
                             final SCIMFilter filter,
                             final SortParameters sortParameters,
                             final PageParameters pageParameters,
                             final SCIMQueryAttributes attributes)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, attributes);
    this.filter         = filter;
    this.sortParameters = sortParameters;
    this.pageParameters = pageParameters;
  }



  /**
   * Retrieve the filter parameters of the request.
   *
   * @return  The filter parameters of the request.
   */
  public SCIMFilter getFilter()
  {
    return filter;
  }



  /**
   * Retrieve the sorting parameters of the request.
   *
   * @return  The sorting parameters of the request.
   */
  public SortParameters getSortParameters()
  {
    return sortParameters;
  }



  /**
   * Retrieve the pagination parameters of the request.
   *
   * @return  The pagination parameters of the request.
   */
  public PageParameters getPageParameters()
  {
    return pageParameters;
  }
}
