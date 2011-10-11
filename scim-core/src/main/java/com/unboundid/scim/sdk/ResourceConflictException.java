/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.sdk;

/**
 * Signals the specified version number does not match the resource's latest
 * version number or a Service Provider refused to create a new,
 * duplicate resource.
 *
 * This exception corresponds to HTTP response code 409 CONFLICT.
 */
public class ResourceConflictException extends SCIMException
{
  /**
   * Create a new <code>ResourceConflictException</code> from the provided
   * information.
   *
   * @param errorMessage  The error message for this SCIM exception.
   */
  public ResourceConflictException(final String errorMessage) {
    super(409, errorMessage);
  }
}
