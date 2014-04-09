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

/**
 * Signals the request entity is larger than then the limit imposed by the
 * server.
 *
 * This exception corresponds to HTTP response code
 * 413 REQUEST ENTITY TOO LARGE.
 */
public class RequestEntityTooLargeException extends SCIMException
{
  /**
   * Create a new <code>RequestEntityTooLargeException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public RequestEntityTooLargeException(final String errorMessage) {
    super(413, errorMessage);
  }

  /**
   * Create a new <code>RequestEntityTooLargeException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <tt>null</tt> value
   *                      is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  public RequestEntityTooLargeException(final String errorMessage,
                                        final Throwable cause) {
    super(413, errorMessage, cause);
  }
}
