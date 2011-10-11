/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.sdk;

/**
 * Signals an internal error from the service provider.
 *
 * This exception corresponds to HTTP response code 500 INTERNAL SERVER ERROR.
 */
public class ServerErrorException extends SCIMException
{
  /**
   * Create a new <code>ServerErrorException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public ServerErrorException(final String errorMessage) {
    super(500, errorMessage);
  }
}
