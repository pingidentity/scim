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

package com.unboundid.scim.facade.org.apache.wink.client.handlers;

import com.unboundid.scim.facade.org.apache.wink.client.ClientRequest;
import com.unboundid.scim.facade.org.apache.wink.client.ClientResponse;

/**
 *  Wink compatibility layer class - see Wink docs.
 */
public interface ClientHandler
{
  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param request Wink compatibility layer class - see Wink docs.
   * @param context Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   * @throws Exception Wink compatibility layer class - see Wink docs.
   */
  ClientResponse handle(final ClientRequest request,
                               final HandlerContext context) throws Exception;
}
