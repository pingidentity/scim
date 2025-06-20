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

package com.unboundid.scim.sdk;

/**
 * Signals the server does not support the requested operation.
 *
 * This exception corresponds to HTTP response code 403 FORBIDDEN.
 */
public class ForbiddenException extends SCIMException
{
  /**
   * Create a new <code>ForbiddenException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public ForbiddenException(final String errorMessage) {
    super(403, errorMessage);
  }

  /**
   * Create a new <code>ForbiddenException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <code>null</code>
   *                      value is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  public ForbiddenException(final String errorMessage,
                            final Throwable cause) {
    super(403, errorMessage, cause);
  }
}
