/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.schema.Resource;



/**
 * This class represents a successful response from a POST to create a new
 * resource.
 */
public final class PostResourceResponse
{
  /**
   * The resource URI provided by the Location header field.
   */
  private final String resourceURI;

  /**
   * The returned contents of the created resource.
   */
  private final Resource resource;



  /**
   * Create a new Post Resource Response.
   *
   * @param resourceURI  The resource URI provided by the Location header field.
   * @param resource     The returned contents of the created resource.
   */
  public PostResourceResponse(final String resourceURI, final Resource resource)
  {
    this.resourceURI = resourceURI;
    this.resource = resource;
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
   * Retrieve the returned contents of the created resource.
   * @return  The returned contents of the created resource.
   */
  public Resource getResource()
  {
    return resource;
  }
}
