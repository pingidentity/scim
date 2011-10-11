/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.sdk;

/**
 * Signals the specified resource; e.g., User, does not exist.
 *
 * This exception corresponds to HTTP response code 404 NOT FOUND.
 */
public class ResourceNotFoundException extends SCIMException
{
  /**
   * Create a new <code>ResourceNotFoundException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public ResourceNotFoundException(final String errorMessage) {
    super(404, errorMessage);
  }
}
