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

package com.unboundid.scim.sdk;

import com.unboundid.scim.schema.ResourceDescriptor;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;



/**
 * This class represents a SCIM Get Resources request to retrieve selected
 * resources.
 */
public class GetResourcesRequest extends ResourceReturningRequest
{
  /**
   * The filter parameters of the request.
   */
  private final SCIMFilter filter;

  /**
   * The SCIM resource ID of the search base entry.
   */
  private final String baseID;

  /**
   * The LDAP search scope to use.
   */
  private final String searchScope;

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
   * @param baseID                The SCIM resource ID of the search base entry,
   *                              or {@code null}.
   * @param searchScope           The LDAP search scope to use, or {@code null}
   *                              if the default (whole-subtree) should be used.
   * @param sortParameters        The sorting parameters of the request.
   * @param pageParameters        The pagination parameters of the request.
   * @param attributes            The set of requested attributes.
   */
  public GetResourcesRequest(final URI baseURL,
                             final String authenticatedUserID,
                             final ResourceDescriptor resourceDescriptor,
                             final SCIMFilter filter,
                             final String baseID,
                             final String searchScope,
                             final SortParameters sortParameters,
                             final PageParameters pageParameters,
                             final SCIMQueryAttributes attributes)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, attributes);
    this.filter         = filter;
    this.baseID         = baseID;
    this.searchScope    = searchScope;
    this.sortParameters = sortParameters;
    this.pageParameters = pageParameters;
  }



  /**
   * Create a new SCIM Get Resource request from the provided information.
   *
   * @param baseURL               The base URL for the SCIM service.
   * @param authenticatedUserID   The authenticated user name or {@code null} if
   *                              the request is not authenticated.
   * @param resourceDescriptor    The ResourceDescriptor associated with this
   *                              request.
   * @param filter                The filter parameters of the request.
   * @param baseID                The SCIM resource ID of the search base entry,
   *                              or {@code null}.
   * @param searchScope           The LDAP search scope to use, or {@code null}
   *                              if the default (whole-subtree) should be used.
   * @param sortParameters        The sorting parameters of the request.
   * @param pageParameters        The pagination parameters of the request.
   * @param attributes            The set of requested attributes.
   * @param httpServletRequest   The HTTP servlet request associated with this
   *                             request or {@code null} if this request is not
   *                             initiated by a servlet.
   */
  public GetResourcesRequest(final URI baseURL,
                             final String authenticatedUserID,
                             final ResourceDescriptor resourceDescriptor,
                             final SCIMFilter filter,
                             final String baseID,
                             final String searchScope,
                             final SortParameters sortParameters,
                             final PageParameters pageParameters,
                             final SCIMQueryAttributes attributes,
                             final HttpServletRequest httpServletRequest)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, attributes,
          httpServletRequest);
    this.filter         = filter;
    this.baseID         = baseID;
    this.searchScope    = searchScope;
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
   * Retrieve the base-id parameter of the request.
   *
   * @return  The base-id parameter of the request.
   */
  public String getBaseID()
  {
    return baseID;
  }



  /**
   * Retrieve the scope parameter of the request.
   *
   * @return  The scope parameter of the request.
   */
  public String getSearchScope()
  {
    return searchScope;
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
