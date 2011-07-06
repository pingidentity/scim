/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

/**
 * This class is the base class for all SCIM requests.
 */
public abstract class SCIMRequest
{
  /**
   * The authenticated user ID or {@code null} if the request is not
   * authenticated.
   */
  private final String authenticatedUserID;



  /**
   * Create a new SCIM request from the provided information.
   *
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   */
  public SCIMRequest(final String authenticatedUserID)
  {
    this.authenticatedUserID = authenticatedUserID;
  }



  /**
   * Get the authenticated user ID.
   *
   * @return  The authenticated user ID or {@code null} if the request is
   *          not authenticated.
   */
  public String getAuthenticatedUserID()
  {
    return authenticatedUserID;
  }
}
