/*
 * Copyright 2014 UnboundID Corp.
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
 * Internal request to the SCIM Server for retrieving selected resources
 * using streaming.  UnboundID SCIM extension.
 */
public class GetStreamedResourcesRequest extends GetResourcesRequest {

  private String resumeToken;

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
   * @param resumeToken           Resume token, or null if this is the
   *                              initial request.
   */
  public GetStreamedResourcesRequest(final URI baseURL,
                             final String authenticatedUserID,
                             final ResourceDescriptor resourceDescriptor,
                             final SCIMFilter filter,
                             final String baseID,
                             final String searchScope,
                             final SortParameters sortParameters,
                             final PageParameters pageParameters,
                             final SCIMQueryAttributes attributes,
                             final String resumeToken)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor, filter, baseID,
        searchScope, sortParameters, pageParameters, attributes);
    this.resumeToken = resumeToken;
  }


  /**
   * Get the resume token associated with this request.
   * @return The current resume token, or null if this is an initial request.
   */
  public String getResumeToken()
  {
    return resumeToken;
  }
}
