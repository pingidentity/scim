/*
 * Copyright 2011-2025 Ping Identity Corporation
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
import com.unboundid.scim.data.Meta;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.wink.SCIMApplication;

import jakarta.ws.rs.core.UriBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class provides an implementation of the SCIM server backend API that
 * serves up the resource schema from a collection of ResourceDescriptors.
 */
public class ResourceSchemaBackend extends SCIMBackend
{
  private final SCIMApplication application;

  /**
   * Create a new ResourceSchemaBackend that serves up the schema provided
   * from the ResourceDescriptors.
   *
   * @param application         The SCIM JAX-RS application associated with this
   *                            backend.
   */
  public ResourceSchemaBackend(final SCIMApplication application) {
    this.application = application;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeBackend() {
    // Nothing to do.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ResourceDescriptor getResource(final GetResourceRequest request)
      throws SCIMException {
    ResourceDescriptor resourceDescriptor = null;
    for(ResourceDescriptor rd :
        application.getBackend().getResourceDescriptors())
    {
      String id = rd.getSchema() +
          SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE + rd.getName();
      if(id.equalsIgnoreCase(request.getResourceID()))
      {
        resourceDescriptor = rd;
        break;
      }
    }

    // Try to find a match in case the schema name was not provided.
    if (resourceDescriptor == null)
    {
      for(ResourceDescriptor rd :
          application.getBackend().getResourceDescriptors())
      {
        if(rd.getName().equalsIgnoreCase(request.getResourceID()))
        {
          resourceDescriptor = rd;
          break;
        }
      }
    }

    if(resourceDescriptor == null)
    {
      throw new ResourceNotFoundException("No Resource Schema with ID " +
          request.getResourceID() + " exists");
    }

    return copyAndSetIdAndMetaAttributes(resourceDescriptor, request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Resources getResources(final GetResourcesRequest request)
      throws SCIMException {
    List<BaseResource> rds =
        new ArrayList<BaseResource>(
            application.getBackend().getResourceDescriptors().size());

    for(ResourceDescriptor resourceDescriptor :
        application.getBackend().getResourceDescriptors())
    {
      ResourceDescriptor copy =
          copyAndSetIdAndMetaAttributes(resourceDescriptor, request);
      if(request.getFilter() == null ||
          copy.getScimObject().matchesFilter(request.getFilter()))
      {
        rds.add(copy);
      }
    }

    int fromIndex = 0;
    int total = rds.size();
    if(request.getPageParameters() != null)
    {
      fromIndex = request.getPageParameters().getStartIndex() - 1;
      int endIndex =
          Math.min(request.getPageParameters().getCount() + fromIndex,
                   rds.size());
      rds = rds.subList(fromIndex, endIndex);
    }

    return new Resources<BaseResource>(rds, total, fromIndex + 1);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BaseResource postResource(final PostResourceRequest request)
      throws SCIMException {
    throw new UnsupportedOperationException("POST operations are not allowed " +
        "on the Resource Schema");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(final DeleteResourceRequest request)
      throws SCIMException {
    throw new UnsupportedOperationException("DELETE operations are not " +
        "allowed on the Resource Schema");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BaseResource putResource(final PutResourceRequest request)
      throws SCIMException {
    throw new UnsupportedOperationException("PUT operations are not allowed " +
        "on the Resource Schema");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BaseResource patchResource(final PatchResourceRequest request)
      throws SCIMException {
    throw new UnsupportedOperationException("PATCH operations are not " +
            "allowed on the Resource Schema");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<ResourceDescriptor> getResourceDescriptors()
  {
    return Collections.singleton(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR);
  }


  /**
   * Make a copy of the ResourceDescriptor and set the id and meta attributes
   * from the provided information.
   *
   * @param resource  The SCIM object whose id and meta attributes are to be
   *                    set.
   * @param request   The SCIM request.
   * @return          The copy of the ResourceDescriptor.
   */
  public static ResourceDescriptor copyAndSetIdAndMetaAttributes(
      final ResourceDescriptor resource,
      final ResourceReturningRequest request)
  {
    ResourceDescriptor copy =
        ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY.createResource(
            resource.getResourceDescriptor(),
            new SCIMObject(resource.getScimObject()));

    String id = resource.getSchema() +
        SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE + resource.getName();
    copy.setId(id);

    final UriBuilder uriBuilder = UriBuilder.fromUri(request.getBaseURL());
    if (!request.getBaseURL().getPath().endsWith("v1/"))
    {
      uriBuilder.path("v1");
    }
    uriBuilder.path(resource.getResourceDescriptor().getEndpoint());
    uriBuilder.path(id);

    copy.setMeta(new Meta(null, null, uriBuilder.build(), null));

    // Pare down the attributes to those requested.
    return ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY.createResource(
        resource.getResourceDescriptor(),
        request.getAttributes().pareObject(copy.getScimObject()));
  }
}
