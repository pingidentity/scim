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

package com.unboundid.scim.sdk;

import com.unboundid.scim.schema.ResourceDescriptor;

import java.net.URI;



/**
 * This class is the base class for all SCIM requests.
 */
public abstract class SCIMRequest
{
  /**
   * The base URL for the SCIM service.
   */
  private final URI baseURL;

  /**
   * The authenticated user ID or {@code null} if the request is not
   * authenticated.
   */
  private final String authenticatedUserID;

  /**
   * The ResourceDescriptor associated with this request.
   */
  private final ResourceDescriptor resourceDescriptor;



  /**
   * Create a new SCIM request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   */
  public SCIMRequest(final URI baseURL, final String authenticatedUserID,
                     final ResourceDescriptor resourceDescriptor)
  {
    this.baseURL             = baseURL;
    this.authenticatedUserID = authenticatedUserID;
    this.resourceDescriptor = resourceDescriptor;
  }



  /**
   * Retrieve the base URL for the SCIM service.
   *
   * @return The base URL for the SCIM service.
   */
  public URI getBaseURL()
  {
    return baseURL;
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



  /**
   * Get ResourceDescriptor associated with this request.
   *
   * @return The ResourceDescriptor associated with this request.
   */
  public ResourceDescriptor getResourceDescriptor() {
    return resourceDescriptor;
  }
}
