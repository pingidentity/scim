/*
 * Copyright 2011-2014 UnboundID Corp.
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

import com.unboundid.scim.data.AuthenticationScheme;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.schema.ResourceDescriptor;

import java.util.Collection;
import java.util.Collections;

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



  /**
   * Update the contents of an existing resource with attributes specified.
   *
   *
   * @param request  The Patch Resource request.
   *
   * @return  The response to the request.
   *
   * @throws SCIMException if an error occurs while processing the request.
   */
  public abstract BaseResource patchResource(
          final PatchResourceRequest request) throws SCIMException;


  /**
   * Retrieve selected resources using the UnboundID streamed query
   * SCIM extension.
   * @param request  The Get Streamed Resources request.
   * @param listener Listener object to receive results.
   *
   * @throws SCIMException if an error occurs while processing the request.
   */
  public abstract void getStreamedResources(
      final GetStreamedResourcesRequest request,
      final StreamedResultListener listener) throws SCIMException;


  /**
   * Retrieves whether this backend supports sorting.
   *
   * @return {@code true} if sorting is supported or {@code false} otherwise.
   */
  public boolean supportsSorting()
  {
    return true;
  }


  /**
   * Retrieves whether this backends supports resource versioning.
   *
   * @return {@code true} if versioning is supported or {@code false} otherwise.
   */
  public boolean supportsVersioning()
  {
    return false;
  }



  /**
   * Retrieves the authentication schemes supported by this backend.
   *
   * @return The authentication schemes supported by this backend.
   */
  public Collection<AuthenticationScheme> getSupportedAuthenticationSchemes()
  {
    return Collections.singleton(AuthenticationScheme.createBasic(true));
  }



  /**
   * Retrieve the resource descriptors served by this backend.
   *
   * @return  The resource descriptors served by this backend.
   */
  public abstract Collection<ResourceDescriptor> getResourceDescriptors();



  /**
   * Retrieve the resource descriptor for the specified endpoint.
   *
   * @param endpoint The endpoint of the resource descriptor to retrieve.
   *
   * @return The resource descriptor for the specified endpoint or {@code null}
   *         if no resource descriptors with the specified endpoint was found.
   */
  public ResourceDescriptor getResourceDescriptor(final String endpoint)
  {
    for(ResourceDescriptor resourceDescriptor : getResourceDescriptors())
    {
      if(resourceDescriptor.getEndpoint().equals(endpoint))
      {
        return resourceDescriptor;
      }
    }

    return null;
  }
}
