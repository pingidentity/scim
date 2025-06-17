/*
 * Copyright 2011-2024 Ping Identity Corporation
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
 * Signals an error while looking up resources and attributes.
 *
 * This exception corresponds to HTTP response code 400 BAD REQUEST.
 */
public class InvalidResourceException extends SCIMException
{
  /**
   * Create a new <code>InvalidResourceException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public InvalidResourceException(final String errorMessage) {
    super(400, errorMessage);
  }

  /**
   * Create a new <code>InvalidResourceException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <code>null</code>
   *                      value is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  public InvalidResourceException(final String errorMessage,
                                  final Throwable cause) {
    super(400, errorMessage, cause);
  }
}
