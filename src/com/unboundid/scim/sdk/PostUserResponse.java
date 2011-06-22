/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.schema.User;



/**
 * This class represents a successful response from a POST to create a new User.
 */
public final class PostUserResponse
{
  /**
   * The resource URI provided by the Location header field.
   */
  private final String resourceURI;

  /**
   * The returned contents of the created user.
   */
  private final User user;



  /**
   * Create a new Post User Response.
   *
   * @param resourceURI  The resource URI provided by the Location header field.
   * @param user         The returned contents of the created user.
   */
  public PostUserResponse(final String resourceURI, final User user)
  {
    this.resourceURI = resourceURI;
    this.user        = user;
  }



  /**
   * Retrieve the resource URI provided by the Location header field.
   * @return  The resource URI provided by the Location header field.
   */
  public String getResourceURI()
  {
    return resourceURI;
  }



  /**
   * Retrieve the returned contents of the created user.
   * @return  The returned contents of the created user.
   */
  public User getUser()
  {
    return user;
  }
}
