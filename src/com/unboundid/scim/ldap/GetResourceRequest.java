/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;



import com.unboundid.scim.sdk.SCIMQueryAttributes;



/**
 * This class represents a SCIM Get Resource request to retrieve all or
 * selected attributes from a single resource.
 */
public final class GetResourceRequest
{
  /**
   * The name of the resource identified by the request endpoint.
   * e.g. User or Group.
   */
  private final String resourceName;

  /**
   * The requested resource ID.
   */
  private final String resourceID;

  /**
   * The set of requested attributes.
   */
  private final SCIMQueryAttributes attributes;



  /**
   * Create a new SCIM Get Resource request from the provided information.
   *
   * @param resourceName  The name of the resource identified by the request
   *                      endpoint. e.g. User or Group.
   * @param resourceID    The requested resource ID.
   * @param attributes    The set of requested attributes.
   */
  public GetResourceRequest(final String resourceName,
                            final String resourceID,
                            final SCIMQueryAttributes attributes)
  {
    this.resourceName = resourceName;
    this.resourceID   = resourceID;
    this.attributes   = attributes;
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
   * Get the requested resource ID.
   *
   * @return  The requested resource ID.
   */
  public String getResourceID()
  {
    return resourceID;
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
