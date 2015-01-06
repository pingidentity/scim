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
 * This class represents a SCIM Post Resource request to create a new resource.
 */
public final class PostResourceRequest extends ResourceReturningRequest
{
  /**
   * The contents of the resource to be created.
   */
  private final SCIMObject resourceObject;



  /**
   * Create a new SCIM Post Resource request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param resourceObject       The contents of the resource to be created.
   * @param attributes           The set of requested attributes.
   */
  public PostResourceRequest(final URI baseURL,
                             final String authenticatedUserID,
                             final ResourceDescriptor resourceDescriptor,
                             final SCIMObject resourceObject,
                             final SCIMQueryAttributes attributes)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, attributes);
    this.resourceObject      = resourceObject;
  }



  /**
   * Create a new SCIM Post Resource request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param resourceObject       The contents of the resource to be created.
   * @param attributes           The set of requested attributes.
   * @param httpServletRequest   The HTTP servlet request associated with this
   *                             request or {@code null} if this request is not
   *                             initiated by a servlet.
   */
  public PostResourceRequest(final URI baseURL,
                             final String authenticatedUserID,
                             final ResourceDescriptor resourceDescriptor,
                             final SCIMObject resourceObject,
                             final SCIMQueryAttributes attributes,
                             final HttpServletRequest httpServletRequest)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, attributes,
          httpServletRequest);
    this.resourceObject      = resourceObject;
  }



  /**
   * Get the contents of the resource to be created.
   *
   * @return  The contents of the resource to be created.
   */
  public SCIMObject getResourceObject()
  {
    return resourceObject;
  }
}
