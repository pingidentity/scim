/*
 * Copyright 2011-2019 Ping Identity Corporation
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

package com.unboundid.scim.facade.org.apache.wink.common.http;

import javax.ws.rs.core.Response;

/**
 *  Wink compatibility layer class - see Wink docs.
 */
public class HttpStatus
{
  private Response.Status status;

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param status Wink compatibility layer class - see Wink docs.
   */
  private HttpStatus(final Response.Status status)
  {
    this.status = status;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param code Wink compatibility layer class - see Wink docs.
   * @param message Wink compatibility layer class - see Wink docs.
   * @param register Wink compatibility layer class - see Wink docs.
   */
  public HttpStatus(final int code, final String message,
                    final boolean register)
  {
    this.status = Response.Status.fromStatusCode(code);
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public int getCode()
  {
    return status.getStatusCode();
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param code Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public static HttpStatus valueOf(final int code)
  {
    Response.Status status = Response.Status.fromStatusCode(code);
    return new HttpStatus(status);
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public String getStatusLine()
  {
    StringBuilder statusLineBuilder = new StringBuilder();
    statusLineBuilder.append(status.getStatusCode());
    statusLineBuilder.append(":");
    if(status.getReasonPhrase() != null)
    {
      statusLineBuilder.append(" ");
      statusLineBuilder.append(status.getReasonPhrase());
    }
    return statusLineBuilder.toString();
  }
}
