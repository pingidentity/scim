/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.data.BaseResource;

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
   * Perform basic authentication using the provided information.
   *
   * @param userID    The user ID to be authenticated.
   * @param password  The user password to be verified.
   *
   * @return {@code true} if the provided user ID and password are valid.
   */
  public abstract boolean authenticate(final String userID,
                                       final String password);



  /**
   * Retrieve all or selected attributes of a resource.
   *
   * @param request  The Get Resource request.
   *
   * @return  The response to the request.
   *
   * @throws SCIMException if an error occurs while processing the request.
   */
  public abstract BaseResource getResource(
      final GetResourceRequest request) throws SCIMException;



  /**
   * Retrieve selected resources.
   *
   * @param request  The Get Resources request.
   *
   * @return  The response to the request.
   *
   * @throws SCIMException if an error occurs while processing the request.
   */
  public abstract Resources getResources(
      final GetResourcesRequest request) throws SCIMException;



  /**
   * Create a new resource.
   *
   *
   * @param request  The Post Resource request.
   *
   * @return  The response to the request.
   *
   * @throws SCIMException if an error occurs while processing the request.
   */
  public abstract BaseResource postResource(
      final PostResourceRequest request) throws SCIMException;



  /**
   * Delete a specific resource.
   *
   *
   * @param request  The Delete Resource request.
   *
   * @throws SCIMException if an error occurs while processing the request.
   */
  public abstract void deleteResource(
      final DeleteResourceRequest request) throws SCIMException;



  /**
   * Replace the contents of an existing resource.
   *
   *
   * @param request  The Put Resource request.
   *
   * @return  The response to the request.
   *
   * @throws SCIMException if an error occurs while processing the request.
   */
  public abstract BaseResource putResource(
      final PutResourceRequest request) throws SCIMException;
}
