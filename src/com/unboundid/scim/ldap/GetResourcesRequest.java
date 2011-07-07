/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMQueryAttributes;



/**
 * This class represents a SCIM Get Resources request to retrieve selected
 * resources.
 */
public final class GetResourcesRequest extends SCIMRequest
{
  /**
   * The endpoint identified in the request. e.g. Users or Groups.
   */
  private final String endPoint;

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
   * The set of requested attributes.
   */
  private final SCIMQueryAttributes attributes;



  /**
   * Create a new SCIM Get Resource request from the provided information.
   *
   * @param authenticatedUserID   The authenticated user name or {@code null} if
   *                              the request is not authenticated.
   * @param endPoint              The endpoint identified in the request. e.g.
   *                              Users or Groups.
   * @param filter                The filter parameters of the request.
   * @param sortParameters        The sorting parameters of the request.
   * @param pageParameters        The pagination parameters of the request.
   * @param attributes            The set of requested attributes.
   */
  public GetResourcesRequest(final String authenticatedUserID,
                             final String endPoint,
                             final SCIMFilter filter,
                             final SortParameters sortParameters,
                             final PageParameters pageParameters,
                             final SCIMQueryAttributes attributes)
  {
    super(authenticatedUserID);
    this.endPoint       = endPoint;
    this.filter         = filter;
    this.sortParameters = sortParameters;
    this.pageParameters = pageParameters;
    this.attributes     = attributes;
  }



  /**
   * Retrieve the endpoint identified in the request. e.g. Users or Groups.
   *
   * @return  The endpoint identified in the request. e.g. Users or Groups.
   */
  public String getEndPoint()
  {
    return endPoint;
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



  /**
   * Retrieve the set of requested attributes.
   *
   * @return  The set of requested attributes.
   */
  public SCIMQueryAttributes getAttributes()
  {
    return attributes;
  }
}
