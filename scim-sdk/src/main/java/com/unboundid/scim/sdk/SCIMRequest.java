/*
 * Copyright 2011-2014 UnboundID Corp.
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
import javax.ws.rs.core.HttpHeaders;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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
   * The HttpServletRequest that initiated this SCIM request.
   */
  private final HttpServletRequest httpServletRequest;

  private final String ifMatchHeaderValue;

  private final String ifNoneMatchHeaderValue;


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
    this.httpServletRequest = null;
    this.ifMatchHeaderValue = null;
    this.ifNoneMatchHeaderValue = null;
  }



  /**
   * Create a new SCIM request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param httpServletRequest   The HTTP servlet request associated with this
   *                             request or {@code null} if this request is not
   *                             initiated by a servlet.
   */
  public SCIMRequest(final URI baseURL, final String authenticatedUserID,
                     final ResourceDescriptor resourceDescriptor,
                     final HttpServletRequest httpServletRequest)
  {
    this.baseURL             = baseURL;
    this.authenticatedUserID = authenticatedUserID;
    this.resourceDescriptor = resourceDescriptor;
    this.httpServletRequest = httpServletRequest;
    this.ifMatchHeaderValue =
        httpServletRequest.getHeader(HttpHeaders.IF_MATCH);
    this.ifNoneMatchHeaderValue =
        httpServletRequest.getHeader(HttpHeaders.IF_NONE_MATCH);
  }





  /**
   * Create a new SCIM request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param httpServletRequest   The HTTP servlet request associated with this
   *                             request or {@code null} if this request is not
   *                             initiated by a servlet.
   * @param ifMatchHeaderValue   The If-Match header value.
   * @param ifNoneMatchHeaderValue The If-None-Match header value.
   */
  public SCIMRequest(final URI baseURL, final String authenticatedUserID,
                     final ResourceDescriptor resourceDescriptor,
                     final HttpServletRequest httpServletRequest,
                     final String ifMatchHeaderValue,
                     final String ifNoneMatchHeaderValue)
  {
    this.baseURL             = baseURL;
    this.authenticatedUserID = authenticatedUserID;
    this.resourceDescriptor = resourceDescriptor;
    this.httpServletRequest = httpServletRequest;
    this.ifMatchHeaderValue = ifMatchHeaderValue;
    this.ifNoneMatchHeaderValue = ifNoneMatchHeaderValue;
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



  /**
   * Get the HTTP servlet request associated with this request.
   *
   * @return The HTTP servlet request associated with this request or
   *         {@code null} if this request is not initiated by a servlet.
   */
  public HttpServletRequest getHttpServletRequest() {
    return httpServletRequest;
  }



  /**
   * Evaluate request preconditions for a resource that does not currently
   * exist. The primary use of this method is to support the {@link <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.24">
   * If-Match: *</a>} and {@link <a
   * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.26">
   * If-None-Match: *</a>} preconditions.
   *
   * @param exception The ResourceNotFoundException that would've been thrown
   *                  if the preconditions are met.
   * @throws SCIMException if preconditions have not been met.
   */
  public void checkPreconditions(final ResourceNotFoundException exception)
      throws SCIMException
  {
    // According to RFC 2616 14.24, If-Match:
    // if "*" is given and no current entity exists, the server MUST NOT
    // perform the requested method, and MUST return a 412 (Precondition Failed)
    // response.
    if (ifMatchHeaderValue != null &&
        parseMatchHeader(ifMatchHeaderValue).isEmpty())
    {
      throw new PreconditionFailedException(exception.getMessage());
    }
  }



  /**
   * Evaluate request preconditions based on the passed in current version.
   *
   * @param currentVersion an ETag for the current version of the resource
   *
   * @throws SCIMException if preconditions have not been met.
   */
  public void checkPreconditions(final EntityTag currentVersion)
      throws SCIMException
  {
    if (ifMatchHeaderValue != null)
    {
      evaluateIfMatch(currentVersion, ifMatchHeaderValue);
    }
    else if (ifNoneMatchHeaderValue != null)
    {
      evaluateIfNoneMatch(currentVersion, ifNoneMatchHeaderValue);
    }
  }

  /**
   * Evaluate If-Match header against the provided eTag.
   *
   * @param eTag The current eTag.
   * @param headerValue The If-Match header value.
   * @throws SCIMException If a match was not found or parsing error occurs.
   */
  protected void evaluateIfMatch(final EntityTag eTag, final String headerValue)
      throws SCIMException
  {
    List<EntityTag> eTags = parseMatchHeader(headerValue);

    if (!isMatch(eTags, eTag))
    {

      throw new PreconditionFailedException(
          "Resource changed since last retrieved", eTag.toString(), null);
    }
  }

  /**
   * Evaluate If-None-Match header against the provided eTag.
   *
   * @param eTag The current eTag.
   * @param headerValue The If-None-Match header value.
   * @throws SCIMException If a match was found or parsing error occurs.
   */
  protected void evaluateIfNoneMatch(final EntityTag eTag,
                                     final String headerValue)
      throws SCIMException
  {
    List<EntityTag> eTags = parseMatchHeader(headerValue);

    if (isMatch(eTags, eTag))
    {
      throw new PreconditionFailedException(
          "Resource did not change since last retrieved",
          eTag.toString(), null);
    }
  }

  /**
   * Evaluate if the provided eTag matches any of the eTags in the provided
   * list.
   *
   * @param eTags The list of eTags to find matches in.
   * @param eTag The eTag to match.
   * @return {@code true} if a match was found or {@code false} otherwise.
   */
  private boolean isMatch(final List<EntityTag> eTags, final EntityTag eTag)
  {
    if (eTag == null) {
        return false;
    }
    if (eTags.isEmpty()) {
        return true;
    }
    String value = eTag.getValue();
    for (EntityTag e : eTags) {
        if (value.equals(e.getValue())) {
            return true;
        }
    }
    return false;
  }

  /**
   * Parse the value of an If-Match or If-None-Match header value.
   *
   * @param headerValue The header value to parse.
   * @return The parsed eTags or an empty list if a wildcard eTag was parsed.
   * @throws InvalidResourceException If an error occurred during parsing.
   */
  private List<EntityTag> parseMatchHeader(final String headerValue)
      throws InvalidResourceException
  {
    List<EntityTag> versions = null;

    if(headerValue != null)
    {
      String[] valueTokens = headerValue.split(",");
      versions = new ArrayList<EntityTag>(valueTokens.length);
      for(String token : valueTokens)
      {
        EntityTag tag;
        try
        {
          tag = EntityTag.valueOf(token);
        }
        catch(IllegalArgumentException e)
        {
          throw new InvalidResourceException(e.getMessage(), e);
        }
        if(tag.getValue().equals("*"))
        {
          return Collections.emptyList();
        }
        else
        {
          versions.add(tag);
        }
      }
    }

    return versions;
  }
}
