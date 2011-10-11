/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.schema.ResourceDescriptor;

import java.net.URI;



/**
 * This class represents a SCIM Put Resource request to replace the contents
 * of an existing resource.
 */
public final class PutResourceRequest extends SCIMRequest
{
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
   * @param resourceDescriptor   The ResourceDescriptor associated with this
   *                             request.
   * @param resourceID           The target resource ID.
   * @param resourceObject       The new contents of the resource.
   * @param attributes           The set of requested attributes.
   */
  public PutResourceRequest(final URI baseURL,
                            final String authenticatedUserID,
                            final ResourceDescriptor resourceDescriptor,
                            final String resourceID,
                            final SCIMObject resourceObject,
                            final SCIMQueryAttributes attributes)
  {
    super(baseURL, authenticatedUserID, resourceDescriptor);
    this.resourceID          = resourceID;
    this.resourceObject      = resourceObject;
    this.attributes          = attributes;
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
