/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

/**
 * This class is the base class for all custom checked exceptions defined in
 * the SCIM server.
 */
public class SCIMException extends Exception
{
  /**
   * The serial version UID required for this serializable class.
   */
  private static final long serialVersionUID = -7530770599624725752L;

  /**
   * The HTTP status code for this SCIM exception.
   */
  private final int statusCode;



  /**
   * Create a new SCIM exception from the provided informatuon.
   *
   * @param statusCode    The HTTP status code for this SCIM exception.
   * @param errorMessage  The error message for this SCIM exception.
   */
  public SCIMException(final int statusCode, final String errorMessage)
  {
    super(errorMessage);

    this.statusCode = statusCode;
  }



  /**
   * Retrieve the HTTP status code for this SCIM exception.
   *
   * @return  The HTTP status code for this SCIM exception.
   */
  public int getStatusCode()
  {
    return statusCode;
  }
}
