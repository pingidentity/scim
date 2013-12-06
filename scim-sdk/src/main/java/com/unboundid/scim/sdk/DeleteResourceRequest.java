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
import javax.ws.rs.core.EntityTag;
import java.net.URI;
import java.util.Collection;


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
   * The versions that at least one must be matched to perform the operation.
   */
  private final Collection<EntityTag> versions;


  /**
   * Create a new SCIM Delete Resource request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param resourceID           The target resource ID.
   * @param versions             The versions that at least one must be matched
   *                             to perform the operation.
   */
  public DeleteResourceRequest(final URI baseURL,
                               final String authenticatedUserID,
                               final ResourceDescriptor resourceDescriptor,
                               final String resourceID,
                               final Collection<EntityTag> versions)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor);
    this.resourceID          = resourceID;
    this.versions            = versions;
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
   * @param versions             The versions that at least one must be matched
   *                             to perform the operation.
   * @param httpServletRequest   The HTTP servlet request associated with this
   *                             request or {@code null} if this request is not
   *                             initiated by a servlet.
   */
  public DeleteResourceRequest(final URI baseURL,
                               final String authenticatedUserID,
                               final ResourceDescriptor resourceDescriptor,
                               final String resourceID,
                               final Collection<EntityTag> versions,
                               final HttpServletRequest httpServletRequest)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, httpServletRequest);
    this.resourceID          = resourceID;
    this.versions            = versions;
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


  /**
   * Get the versions that at least one must be matched to perform the
   * operation.
   *
   * @return The versions that at least one must be matched to perform the
   *         operation or {@code null} if the operation should be performed
   *         normally.
   */
  public Collection<EntityTag> getVersions()
  {
    return versions;
  }
}
