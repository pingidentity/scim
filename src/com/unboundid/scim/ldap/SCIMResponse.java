/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.schema.Response;



/**
 * This class represents the response to a SCIM request.
 */
public final class SCIMResponse
{
  /**
   * The HTTP status code of the response.
   */
  private final int statusCode;

  /**
   * The content of the response.
   */
  private final Response response;




  /**
   * Create a new instance of a SCIM response.
   *
   * @param statusCode  The HTTP status code of the response.
   * @param response    The content of the response.
   */
  public SCIMResponse(final int statusCode, final Response response)
  {
    this.statusCode = statusCode;
    this.response   = response;
  }



  /**
   * Retrieve the HTTP status code of the response.
   * @return  The HTTP status code of the response.
   */
  public int getStatusCode()
  {
    return statusCode;
  }



  /**
   * Retrieve the content of the response.
   * @return  The content of the response.
   */
  public Response getResponse()
  {
    return response;
  }
}
