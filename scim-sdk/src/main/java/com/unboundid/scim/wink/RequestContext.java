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

package com.unboundid.scim.wink;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.security.Principal;
import java.util.List;

import static com.unboundid.scim.sdk.SCIMConstants.*;



public class RequestContext
{
  /**
   * The HTTP servlet request.
   */
  private final HttpServletRequest request;

  /**
   * The security context of the request.
   */
  private final SecurityContext securityContext;

  /**
   * The HTTP headers of the request.
   */
  private final HttpHeaders headers;

  /**
   * The URI information of the request.
   */
  private final UriInfo uriInfo;

  /**
   * The HTTP authenticated user ID. Not to be confused with a SCIM user ID.
   */
  private final String authID;

  /**
   * The value of the HTTP Origin header.
   */
  private final String origin;

  /**
   * The media type to be consumed, if any.
   */
  private final MediaType consumeMediaType;

  /**
   * The media type to be produced, if any.
   */
  private final MediaType produceMediaType;

  /**
   * The value of the HTTP Content-Length header.
   */
  private final long contentLength;



  /**
   * Creates the resource implementation for a request.
   *
   * @param request   The servlet context for the request.
   * @param securityContext  The HTTP servlet request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   * @param consumeMediaType The media type to be consumed, if any.
   * @param produceMediaType The media type to be produced, if any.
   */
  public RequestContext(final HttpServletRequest request,
                        final SecurityContext securityContext,
                        final HttpHeaders headers,
                        final UriInfo uriInfo,
                        final MediaType consumeMediaType,
                        final MediaType produceMediaType)
  {
    this.request = request;
    this.securityContext  = securityContext;
    this.headers          = headers;
    this.uriInfo          = uriInfo;
    this.consumeMediaType = consumeMediaType;
    this.produceMediaType = produceMediaType;

    // Determine the authenticated ID for the request.
    final Principal userPrincipal = securityContext.getUserPrincipal();
    if (userPrincipal != null)
    {
      authID = userPrincipal.getName();
    }
    else
    {
      authID = null;
    }

    final List<String> originHeaders =
        headers.getRequestHeader(HEADER_NAME_ORIGIN);
    if (originHeaders != null)
    {
      origin = originHeaders.get(0);
    }
    else
    {
      origin = null;
    }

    final List<String> contentLengthHeaders =
        headers.getRequestHeader(HttpHeaders.CONTENT_LENGTH);
    if (contentLengthHeaders != null)
    {
      contentLength = Long.parseLong(contentLengthHeaders.get(0));
    }
    else
    {
      contentLength = -1;
    }
  }



  /**
   * Retrieve the servlet context of the request.
   * @return The servlet context of the request.
   */
  public HttpServletRequest getRequest()
  {
    return request;
  }



  /**
   * Retrieve the security context for the request.
   * @return The security context for the request.
   */
  public SecurityContext getSecurityContext()
  {
    return securityContext;
  }



  /**
   * Retrieve the request headers.
   * @return The request headers.
   */
  public HttpHeaders getHeaders()
  {
    return headers;
  }



  /**
   * Retrieve the URI info for the request.
   * @return The URI info for the request.
   */
  public UriInfo getUriInfo()
  {
    return uriInfo;
  }



  /**
   * Retrieve the HTTP authenticated user ID.
   * @return The the HTTP authenticated user ID.
   */
  public String getAuthID()
  {
    return authID;
  }



  /**
   * Retrieve the value of the HTTP Origin header.
   * @return The value of the HTTP Origin header.
   */
  public String getOrigin()
  {
    return origin;
  }



  /**
   * Retrieve the media type to be consumed.
   * @return  The media type to be consumed.
   */
  public MediaType getConsumeMediaType()
  {
    return consumeMediaType;
  }



  /**
   * Retrieve the media type to be produced.
   * @return  The media type to be produced.
   */
  public MediaType getProduceMediaType()
  {
    return produceMediaType;
  }



  /**
   * Retrieve the value of the HTTP Content-Length header.
   * @return The value of the HTTP Content-Length header, or -1 if it is
   *         not present.
   */
  public long getContentLength()
  {
    return contentLength;
  }
}
