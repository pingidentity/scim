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

package com.unboundid.scim.ri.wink;

import com.unboundid.scim.ri.ResourceSchemaBackend;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMBackend;
import org.apache.wink.common.WinkApplication;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;



/**
 * This class is a JAX-RS Application that returns the SCIM resource
 * implementations.
 */
public class SCIMApplication extends WinkApplication
{
  private static final Set<Class<?>> CLASSES = new HashSet<Class<?>>();
  static
  {
    CLASSES.add(MonitorResource.class);
  }

  private final Set<Object> instances;

  /**
   * Create a new SCIMApplication that defines the endpoints provided by the
   * ResourceDescriptors and uses the provided backend to process the request.
   *
   * @param resourceDescriptors The ResourceDescriptors to serve.
   * @param backend The backend that should be used to process the requests.
   */
  public SCIMApplication(
      final Collection<ResourceDescriptor> resourceDescriptors,
      final SCIMBackend backend) {
    instances = new HashSet<Object>(resourceDescriptors.size() * 4 + 4);

    // The resources for the /Schema and /Schemas endpoints.
    ResourceSchemaBackend resourceSchemaBackend =
        new ResourceSchemaBackend(resourceDescriptors);
    instances.add(new CRUDResource(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        resourceSchemaBackend));
    instances.add(new QueryResource(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        resourceSchemaBackend));
    instances.add(new XMLQueryResource(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        resourceSchemaBackend));
    instances.add(new JSONQueryResource(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        resourceSchemaBackend));

    for(ResourceDescriptor resourceDescriptor : resourceDescriptors)
    {
      instances.add(new CRUDResource(resourceDescriptor, backend));
      instances.add(new QueryResource(resourceDescriptor, backend));
      instances.add(new XMLQueryResource(resourceDescriptor, backend));
      instances.add(new JSONQueryResource(resourceDescriptor, backend));
    }
  }

  @Override
  public Set<Class<?>> getClasses()
  {
    return CLASSES;
  }



  @Override
  public Set<Object> getInstances()
  {
    return instances;
  }
}
