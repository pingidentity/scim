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
 * Signals server failed to update as Resource changed on the server since last
 * retrieved
 *
 * This exception corresponds to HTTP response code
 * 412 PRECONDITION FAILED.
 */
public class PreconditionFailedException extends SCIMException
{
  private final String version;

  /**
   * Create a new <code>PreconditionFailedException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public PreconditionFailedException(final String errorMessage) {
    super(412, errorMessage);
    this.version = null;
  }

  /**
   * Create a new <code>PreconditionFailedException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <code>null</code>
   *                      value is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  public PreconditionFailedException(final String errorMessage,
                                     final Throwable cause) {
    super(412, errorMessage, cause);
    this.version = null;
  }

  /**
   * Create a new <code>PreconditionFailedException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   * @param version       The current version of the Resource.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <code>null</code>
   *                      value is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  public PreconditionFailedException(final String errorMessage,
                                     final String version,
                                     final Throwable cause) {
    super(412, errorMessage, cause);
    this.version = version;
  }

  /**
   * Retrieves the current version of the Resource.
   *
   * @return The current version of the Resource.
   */
  public String getVersion() {
    return version;
  }
}
