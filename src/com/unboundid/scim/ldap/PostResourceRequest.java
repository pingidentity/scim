/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;



/**
 * This class represents a SCIM Post Resource request to create a new resource.
 */
public final class PostResourceRequest
{
  /**
   * The name of the resource identified by the request endpoint.
   * e.g. User or Group.
   */
  private final String resourceName;

  /**
   * The contents of the resource to be created.
   */
  private final SCIMObject resourceObject;

  /**
   * The set of requested attributes.
   */
  private final SCIMQueryAttributes attributes;



  /**
   * Create a new SCIM Post Resource request from the provided information.
   *
   * @param resourceName    The name of the resource identified by the request
   *                        endpoint. e.g. User or Group.
   * @param resourceObject  The contents of the resource to be created.
   * @param attributes      The set of requested attributes.
   */
  public PostResourceRequest(final String resourceName,
                             final SCIMObject resourceObject,
                             final SCIMQueryAttributes attributes)
  {
    this.resourceName   = resourceName;
    this.resourceObject = resourceObject;
    this.attributes     = attributes;
  }



  /**
   * Get the name of the resource identified by the request endpoint. e.g.
   * User or Group.
   *
   * @return  The name of the resource identified by the request endpoint.
   */
  public String getResourceName()
  {
    return resourceName;
  }



  /**
   * Get the contents of the resource to be created.
   *
   * @return  The contents of the resource to be created.
   */
  public SCIMObject getResourceObject()
  {
    return resourceObject;
  }



  /**
   * Get the set of requested attributes.
   *
   * @return  The set of requested attributes.
   */
  public SCIMQueryAttributes getAttributes()
  {
    return attributes;
  }
}
