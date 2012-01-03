/*
 * Copyright 2011-2012 UnboundID Corp.
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

package com.unboundid.scim.wink;

import com.unboundid.scim.data.AuthenticationScheme;
import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.data.ChangePasswordConfig;
import com.unboundid.scim.data.ETagConfig;
import com.unboundid.scim.data.FilterConfig;
import com.unboundid.scim.data.PatchConfig;
import com.unboundid.scim.data.ServiceProviderConfig;
import com.unboundid.scim.data.SortConfig;
import com.unboundid.scim.sdk.ResourceSchemaBackend;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMObject;
import org.apache.wink.common.WinkApplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.unboundid.scim.sdk.SCIMConstants.SCHEMA_URI_CORE;


/**
 * This class is a JAX-RS Application that returns the SCIM resource
 * implementations.
 */
public class SCIMApplication extends WinkApplication
{
  private final Set<Object> instances;
  private final Collection<ResourceStats> resourceStats;
  private final SCIMBackend backend;

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
    instances = new HashSet<Object>(resourceDescriptors.size() * 4 + 8);
    Collection<ResourceStats> statsCollection =
        new ArrayList<ResourceStats>(resourceDescriptors.size() + 2);

    ResourceStats stats = new ResourceStats("monitor");
    instances.add(new MonitorResource(this, stats));
    statsCollection.add(stats);

    stats = new ResourceStats(
        CoreSchema.SERVICE_PROVIDER_CONFIG_SCHEMA_DESCRIPTOR.getName());
    instances.add(new ServiceProviderConfigResource(this, stats));
    instances.add(new XMLServiceProviderConfigResource(this, stats));
    instances.add(new JSONServiceProviderConfigResource(this, stats));
    statsCollection.add(stats);

    stats = new ResourceStats(
        CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR.getName());
    // The resources for the /Schema and /Schemas endpoints.
    ResourceSchemaBackend resourceSchemaBackend =
        new ResourceSchemaBackend(resourceDescriptors);
    instances.add(new SCIMResource(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        stats, resourceSchemaBackend));
    instances.add(new XMLQueryResource(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        stats, resourceSchemaBackend));
    instances.add(new JSONQueryResource(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        stats, resourceSchemaBackend));
    statsCollection.add(stats);

    for(ResourceDescriptor resourceDescriptor : resourceDescriptors)
    {
      stats = new ResourceStats(resourceDescriptor.getName());
      instances.add(new SCIMResource(resourceDescriptor, stats, backend));
      instances.add(new XMLQueryResource(resourceDescriptor, stats, backend));
      instances.add(new JSONQueryResource(resourceDescriptor, stats, backend));
      statsCollection.add(stats);
    }
    this.resourceStats = Collections.unmodifiableCollection(statsCollection);
    this.backend = backend;
  }



  @Override
  public Set<Object> getInstances()
  {
    return instances;
  }


  /**
   * Retrieves the statistics for the resources being served by this
   * application instance.
   *
   * @return The statistics for the resources being served by this
   * application instance.
   */
  public Collection<ResourceStats> getResourceStats()
  {
    return resourceStats;
  }

  /**
   * Retrieve the service provider configuration.
   * @return  The service provider configuration.
   */
  public ServiceProviderConfig getServiceProviderConfig()
  {
    final SCIMObject scimObject = new SCIMObject();
    final ServiceProviderConfig serviceProviderConfig =
        ServiceProviderConfig.SERVICE_PROVIDER_CONFIG_RESOURCE_FACTORY.
            createResource(CoreSchema.SERVICE_PROVIDER_CONFIG_SCHEMA_DESCRIPTOR,
                           scimObject);

    serviceProviderConfig.setId(SCHEMA_URI_CORE);
    serviceProviderConfig.setPatchConfig(new PatchConfig(false));
    serviceProviderConfig.setBulkConfig(new BulkConfig(false, 0, 0));
    serviceProviderConfig.setFilterConfig(new FilterConfig(true,
        backend.getConfig().getMaxResults()));
    serviceProviderConfig.setChangePasswordConfig(
        new ChangePasswordConfig(false));
    serviceProviderConfig.setSortConfig(new SortConfig(false));
    serviceProviderConfig.setETagConfig(new ETagConfig(false));

    final List<AuthenticationScheme> authenticationSchemes =
        new ArrayList<AuthenticationScheme>();
    authenticationSchemes.add(
        new AuthenticationScheme(
            "HttpBasic",
            "The HTTP Basic Access Authentication scheme. This scheme is not " +
            "considered to be a secure method of user authentication (unless " +
            "used in conjunction with some external secure system such as " +
            "SSL), as the user name and password are passed over the network " +
            "as cleartext.",
            "http://www.ietf.org/rfc/rfc2617.txt",
            "http://en.wikipedia.org/wiki/Basic_access_authentication",
            null, false));
    serviceProviderConfig.setAuthenticationSchemes(authenticationSchemes);

    return serviceProviderConfig;
  }
}
