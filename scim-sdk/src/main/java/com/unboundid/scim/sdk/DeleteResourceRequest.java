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

import javax.servlet.http.HttpServletRequest;
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
   * Create a new SCIM Delete Resource request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param resourceID           The target resource ID.
   * @param httpServletRequest   The HTTP servlet request associated with this
   *                             request or {@code null} if this request is not
   *                             initiated by a servlet.
   */
  public DeleteResourceRequest(final URI baseURL,
                               final String authenticatedUserID,
                               final ResourceDescriptor resourceDescriptor,
                               final String resourceID,
                               final HttpServletRequest httpServletRequest)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, httpServletRequest);
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
