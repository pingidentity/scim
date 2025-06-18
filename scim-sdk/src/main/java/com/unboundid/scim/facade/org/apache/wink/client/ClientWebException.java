/*
 * Copyright 2011-2025 Ping Identity Corporation
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

package com.unboundid.scim.facade.org.apache.wink.client;

/**
 *  Wink compatibility layer class - see Wink docs.
 */
public class ClientWebException extends ClientRuntimeException
{
  private final ClientResponse response;

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param response Wink compatibility layer class - see Wink docs.
   */
  public ClientWebException(final ClientResponse response)
  {
    this.response = response;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public ClientResponse getResponse()
  {
    return response;
  }

}
