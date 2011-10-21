/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.sdk;

/**
 * Signals an error while marshaling or unmarshaling a SCIM entity because it is
 * unparseable, syntactically incorrect, or violates schema.
 *
 * This exception corresponds to HTTP response code 400 BAD REQUEST.
 */
public class InvalidResourceException extends SCIMException
{
  /**
   * Create a new <code>MarshalException</code> from the provided information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public InvalidResourceException(final String errorMessage) {
    super(400, errorMessage);
  }

  /**
   * Create a new <code>MarshalException</code> from the provided information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <tt>null</tt> value
   *                      is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  public InvalidResourceException(final String errorMessage,
                                  final Throwable cause) {
    super(400, errorMessage, cause);
  }
}
