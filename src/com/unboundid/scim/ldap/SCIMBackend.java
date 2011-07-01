/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

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
   * @param request  The Get Resource request.
   *
   * @return  The response to the request.
   */
  public abstract SCIMResponse getResource(final GetResourceRequest request);



  /**
   * Retrieve selected resources.
   *
   * @param request  The Get Resources request.
   *
   * @return  The response to the request.
   */
  public abstract SCIMResponse getResources(final GetResourcesRequest request);



  /**
   * Create a new resource.
   *
   * @param request  The Post Resource request.
   *
   * @return  The response to the request.
   */
  public abstract SCIMResponse postResource(final PostResourceRequest request);



  /**
   * Delete a specific resource.
   *
   * @param request  The Delete Resource request.
   *
   * @return  The response to the request.
   */
  public abstract SCIMResponse deleteResource(
      final DeleteResourceRequest request);



  /**
   * Replace the contents of an existing resource.
   *
   * @param request  The Put Resource request.
   *
   * @return  The response to the request.
   */
  public abstract SCIMResponse putResource(final PutResourceRequest request);
}
