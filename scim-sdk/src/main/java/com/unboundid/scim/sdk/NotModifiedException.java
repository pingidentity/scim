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
 * Signals the Resource has not changed on the server since last retrieved
 *
 * This exception corresponds to HTTP response code
 * 304 NOT MODIFIED.
 */
public class NotModifiedException extends SCIMException
{
  private final String version;

  /**
   * Create a new <code>NotModifiedException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public NotModifiedException(final String errorMessage) {
    super(304, errorMessage);
    version = null;
  }

  /**
   * Create a new <code>NotModifiedException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <tt>null</tt> value
   *                      is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  public NotModifiedException(final String errorMessage,
                              final Throwable cause) {
    super(304, errorMessage, cause);
    version = null;
  }

  /**
   * Create a new <code>NotModifiedException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   * @param version       The current version of the Resource.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <tt>null</tt> value
   *                      is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  public NotModifiedException(final String errorMessage,
                              final String version,
                              final Throwable cause) {
    super(304, errorMessage, cause);
    this.version = version;
  }

  /**
   * Retrieves the version of the Resource.
   *
   * @return The current version of the Resource.
   */
  public String getVersion() {
    return version;
  }
}
