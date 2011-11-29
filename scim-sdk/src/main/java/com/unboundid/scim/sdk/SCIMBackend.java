/*
 * Copyright 2011 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
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
   * The mutable configuration settings for the backend.
   */
  private final SCIMBackendConfig config = new SCIMBackendConfig();



  /**
   * Performs any cleanup which may be necessary when this backend is to be
   * taken out of service.
   */
  public abstract void finalizeBackend();



  /**
   * Retrieve the mutable configuration settings for the backend.
   * @return  The mutable configuration settings for the backend.
   */
  public SCIMBackendConfig getConfig()
  {
    return config;
  }



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
