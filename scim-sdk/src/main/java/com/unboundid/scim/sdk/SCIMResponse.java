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


import com.unboundid.scim.marshal.Marshaller;

import java.io.OutputStream;

/**
 * This class represents the response to a SCIM request.
 */
public interface SCIMResponse
{
  /**
   * Marshals this response using the specified <code>Marshaller</code> to the
   * specified <code>OutputStream</code>.
   *
   * @param marshaller The <code>Marshaller</code> to use.
   * @param outputStream The <code>OutputStream</code> to write to.
   * @throws Exception if an error occurs while performing the marshaling.
   */
  void marshal(Marshaller marshaller, OutputStream outputStream)
      throws Exception;
}
