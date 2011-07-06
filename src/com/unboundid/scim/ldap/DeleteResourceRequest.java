/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

/**
 * This class represents a SCIM Delete Resource request delete a single
 * resource.
 */
public final class DeleteResourceRequest extends SCIMRequest
{
  /**
   * The name of the resource identified by the request endpoint.
   * e.g. User or Group.
   */
  private final String resourceName;

  /**
   * The target resource ID.
   */
  private final String resourceID;



  /**
   * Create a new SCIM Delete Resource request from the provided information.
   *
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceName  The name of the resource identified by the request
   *                      endpoint. e.g. User or Group.
   * @param resourceID    The target resource ID.
   */
  public DeleteResourceRequest(final String authenticatedUserID,
                               final String resourceName,
                               final String resourceID)
  {
    super(authenticatedUserID);
    this.resourceName        = resourceName;
    this.resourceID          = resourceID;
  }



  /**
   * Get the name of the resource identified by the request endpoint. e.g.
   * User or Group.
   *
   * @return  The name of the resource identified by the request endpoint.
   */
  public String getResourceName()
  {
    return resourceName;
  }



  /**
   * Get the requested resource ID.
   *
   * @return  The requested resource ID.
   */
  public String getResourceID()
  {
    return resourceID;
  }
}
