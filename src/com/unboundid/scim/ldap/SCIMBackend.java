/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMObject;


/**
 * This class defines an API for a backend that can be plugged into the SCIM
 * server.
 */
public abstract class SCIMBackend
{
  /**
   * Performs any cleanup which may be necessary when this backend is to be
   * taken out of service.
   */
  public abstract void finalizeBackend();



  /**
   * Retrieve all or selected attributes of a resource.
   *
   * @param request      The Get Resource request.
   *
   * @return  The requested resource or {@code null} if the resource does not
   *          exist. The resource contents are partial if selected attributes
   *          were requested.
   */
  public abstract SCIMObject getObject(final GetResourceRequest request);



  /**
   * Create a new resource.
   *
   * @param request  The Post Resource request.
   *
   * @return  The resource that was created.
   */
  public abstract SCIMObject postObject(final PostResourceRequest request);



  /**
   * Delete a specific resource.
   *
   * @param request      The Delete Resource request.
   *
   * @return  {@code true} if the resource was deleted, or {@code false} if the
   *          resource did not exist.
   */
  public abstract boolean deleteObject(final DeleteResourceRequest request);



  /**
   * Replace the contents of an existing resource.
   *
   * @param request  The Put Resource request.
   *
   * @return  The updated contents of the resource, or {@code null} if the
   *          resource did not exist.
   */
  public abstract SCIMObject putObject(final PutResourceRequest request);
}
