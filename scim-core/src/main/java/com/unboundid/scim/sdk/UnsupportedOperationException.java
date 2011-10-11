/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.sdk;

/**
 * Signals the service provider does not support the requested operation.
 *
 * This exception corresponds to HTTP response code 403 FORBIDDEN.
 */
public class UnsupportedOperationException extends SCIMException
{
  /**
   * Create a new <code>UnsupportedOperationException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public UnsupportedOperationException(final String errorMessage) {
    super(403, errorMessage);
  }
}
