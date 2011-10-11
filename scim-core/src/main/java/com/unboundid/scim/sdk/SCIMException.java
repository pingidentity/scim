/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.marshal.Marshaller;

import java.io.OutputStream;

/**
 * This class is the base class for all custom checked exceptions defined in
 * the SCIM server.
 */
public class SCIMException extends Exception implements SCIMResponse
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
  protected SCIMException(final int statusCode, final String errorMessage)
  {
    super(errorMessage);

    this.statusCode = statusCode;
  }



  /**
   * Create a new SCIM exception from the provided informatuon.
   *
   * @param statusCode    The HTTP status code for this SCIM exception.
   * @param errorMessage  The error message for this SCIM exception.
   * @param cause         The cause (which is saved for later retrieval by the
   *                      {@link #getCause()} method).  (A <tt>null</tt> value
   *                      is permitted, and indicates that the cause is
   *                      nonexistent or unknown.)
   */
  protected SCIMException(final int statusCode, final String errorMessage,
                          final Throwable cause)
  {
    super(errorMessage, cause);

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

  /**
   * {@inheritDoc}
   */
  public final void marshal(final Marshaller marshaller,
                            final OutputStream outputStream)
      throws Exception {
    marshaller.marshal(this, outputStream);
  }

  /**
   * Create the appropriate SCIMException from the provided information.
   *
   * @param statusCode    The HTTP status code for this SCIM exception.
   * @param errorMessage  The error message for this SCIM exception.
   * @return The appropriate SCIMException from the provided information.
   */
  public static SCIMException createException(final int statusCode,
                                              final String errorMessage)
  {
    switch(statusCode)
    {
      case 400 : return new MarshalException(errorMessage);
      case 403 : return new UnsupportedOperationException(errorMessage);
      case 404 : return new ResourceNotFoundException(errorMessage);
      case 409 : return new ResourceConflictException(errorMessage);
      case 500 : return new ServerErrorException(errorMessage);
      default : return new SCIMException(statusCode, errorMessage);
    }
  }
}
