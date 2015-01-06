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
 * This class is the base class for SCIM requests that return resources in the
 * response.
 */
public class ResourceReturningRequest extends SCIMRequest
{
  /**
   * The set of requested attributes.
   */
  private final SCIMQueryAttributes attributes;



  /**
   * Create a new SCIM request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param attributes           The set of requested attributes.
   */
  public ResourceReturningRequest(final URI baseURL,
                                  final String authenticatedUserID,
                                  final ResourceDescriptor resourceDescriptor,
                                  final SCIMQueryAttributes attributes)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor);
    this.attributes = attributes;
  }



  /**
   * Create a new SCIM request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param attributes           The set of requested attributes.
   * @param httpServletRequest   The HTTP servlet request associated with this
   *                             request or {@code null} if this request is not
   *                             initiated by a servlet.
   */
  public ResourceReturningRequest(final URI baseURL,
                                  final String authenticatedUserID,
                                  final ResourceDescriptor resourceDescriptor,
                                  final SCIMQueryAttributes attributes,
                                  final HttpServletRequest httpServletRequest)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, httpServletRequest);
    this.attributes = attributes;
  }



  /**
   * Create a new SCIM request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param attributes           The set of requested attributes.
   * @param httpServletRequest   The HTTP servlet request associated with this
   *                             request or {@code null} if this request is not
   *                             initiated by a servlet.
   * @param ifMatchHeaderValue   The If-Match header value.
   * @param ifNoneMatchHeaderValue The If-None-Match header value.
   */
  public ResourceReturningRequest(final URI baseURL,
                                  final String authenticatedUserID,
                                  final ResourceDescriptor resourceDescriptor,
                                  final SCIMQueryAttributes attributes,
                                  final HttpServletRequest httpServletRequest,
                                  final String ifMatchHeaderValue,
                                  final String ifNoneMatchHeaderValue)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, httpServletRequest,
        ifMatchHeaderValue, ifNoneMatchHeaderValue);
    this.attributes = attributes;
  }







  /**
   * Get the set of requested attributes.
   *
   * @return  The set of requested attributes.
   */
  public SCIMQueryAttributes getAttributes()
  {
    return attributes;
  }
}
