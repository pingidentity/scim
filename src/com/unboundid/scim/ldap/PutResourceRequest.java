/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;

import java.net.URI;



/**
 * This class represents a SCIM Put Resource request to replace the contents
 * of an existing resource.
 */
public final class PutResourceRequest extends SCIMRequest
{
  /**
   * The name of the resource identified by the request endpoint.
   * e.g. User or Group.
   */
  private final String resourceName;

  /**
   * The target resource ID.
   */
  private final String resourceID;

  /**
   * The new contents of the resource.
   */
  private final SCIMObject resourceObject;

  /**
   * The set of requested attributes.
   */
  private final SCIMQueryAttributes attributes;



  /**
   * Create a new SCIM Put Resource request from the provided information.
   *
   * @param baseURL              The base URL for the SCIM service.
   * @param authenticatedUserID  The authenticated user name or {@code null} if
   *                             the request is not authenticated.
   * @param resourceName         The name of the resource identified by the
   *                             request endpoint. e.g. User or Group.
   * @param resourceID           The target resource ID.
   * @param resourceObject       The new contents of the resource.
   * @param attributes           The set of requested attributes.
   */
  public PutResourceRequest(final URI baseURL,
                            final String authenticatedUserID,
                            final String resourceName,
                            final String resourceID,
                            final SCIMObject resourceObject,
                            final SCIMQueryAttributes attributes)
  {
    super(baseURL, authenticatedUserID);
    this.resourceName        = resourceName;
    this.resourceID          = resourceID;
    this.resourceObject      = resourceObject;
    this.attributes          = attributes;
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
   * Get the target resource ID.
   *
   * @return  The target resource ID.
   */
  public String getResourceID()
  {
    return resourceID;
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
