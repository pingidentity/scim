/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.schema.ResourceDescriptor;

import java.net.URI;



/**
 * This class represents a SCIM Delete Resource request delete a single
 * resource.
 */
public final class DeleteResourceRequest extends SCIMRequest
{
  /**
   * The target resource ID.
   */
  private final String resourceID;



  /**
   * Create a new SCIM Delete Resource request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param resourceID    The target resource ID.
   */
  public DeleteResourceRequest(final URI baseURL,
                               final String authenticatedUserID,
                               final ResourceDescriptor resourceDescriptor,
                               final String resourceID)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor);
    this.resourceID          = resourceID;
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
