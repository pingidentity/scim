/*
 * Copyright 2012-2014 UnboundID Corp.
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

/**
 * Signals a problem connecting to the service provider.
 *
 * This exception corresponds to special HTTP response code -1. This is not
 * defined by any RFC but instead is used internally to express the case
 * when the target server cannot be reached at all and thus there is no real
 * response code.
 */
public class ConnectException extends SCIMException
{
  /**
   * Create a new <code>ConnectException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public ConnectException(final String errorMessage) {
    super(-1, errorMessage);
  }
}
