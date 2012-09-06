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
import com.unboundid.scim.data.XmlDataFormatConfig;
import com.unboundid.scim.sdk.ResourceSchemaBackend;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import org.apache.wink.common.WinkApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.unboundid.scim.sdk.SCIMConstants.SCHEMA_URI_CORE;


/**
 * This class is a JAX-RS Application that returns the SCIM resource
 * implementations.
 */
public class SCIMApplication extends WinkApplication
{
  private final Set<Object> instances;
  private final Map<String,ResourceDescriptor> descriptors;
  private final Map<String,ResourceStats> resourceStats;
  private final SCIMBackend backend;
  private volatile long bulkMaxOperations = Long.MAX_VALUE;
  private volatile long bulkMaxPayloadSize = Long.MAX_VALUE;
  private volatile File tmpDataDir = null;
  private AdjustableSemaphore bulkMaxConcurrentRequestsSemaphore =
      new AdjustableSemaphore(Integer.MAX_VALUE);

  /**
   * Create a new SCIMApplication that defines the endpoints provided by the
   * ResourceDescriptors and uses the provided backend to process the request.
   *
   * @param resourceDescriptors The ResourceDescriptors to serve.
   * @param backend The backend that should be used to process the requests.
   */
  public SCIMApplication(
      final Collection<ResourceDescriptor> resourceDescriptors,
      final SCIMBackend backend)
  {
    descriptors =
        new HashMap<String, ResourceDescriptor>(resourceDescriptors.size());
    for (final ResourceDescriptor descriptor : resourceDescriptors)
    {
      descriptors.put(descriptor.getEndpoint(), descriptor);
    }

    instances = new HashSet<Object>(resourceDescriptors.size() * 4 + 12);
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

    // The Bulk operation endpoint.
    stats = new ResourceStats("Bulk");
    instances.add(new BulkResource(this, stats, backend));
    instances.add(new JSONBulkResource(this, stats, backend));
    instances.add(new XMLBulkResource(this, stats, backend));
    statsCollection.add(stats);

    this.resourceStats =
        new HashMap<String, ResourceStats>(statsCollection.size());
    for (final ResourceStats s : statsCollection)
    {
      resourceStats.put(s.getName(), s);
    }
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
    return Collections.unmodifiableCollection(resourceStats.values());
  }



  /**
   * Retrieve the stats for a given resource.
   *
   * @param resourceName  The name of the resource.
   *
   * @return  The stats for the requested resource.
   */
  public ResourceStats getStatsForResource(final String resourceName)
  {
    return resourceStats.get(resourceName);
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
    serviceProviderConfig.setPatchConfig(new PatchConfig(true));
    serviceProviderConfig.setBulkConfig(
        new BulkConfig(true, bulkMaxOperations, bulkMaxPayloadSize));
    serviceProviderConfig.setFilterConfig(new FilterConfig(true,
        backend.getConfig().getMaxResults()));
    serviceProviderConfig.setChangePasswordConfig(
        new ChangePasswordConfig(true));
    serviceProviderConfig.setSortConfig(new SortConfig(true));
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

    serviceProviderConfig.setXmlDataFormatConfig(new XmlDataFormatConfig(true));

    return serviceProviderConfig;
  }



  /**
   * Specify the maximum number of operations permitted in a bulk request.
   * @param bulkMaxOperations  The maximum number of operations permitted in a
   *                           bulk request.
   */
  public void setBulkMaxOperations(final long bulkMaxOperations)
  {
    this.bulkMaxOperations = bulkMaxOperations;
  }



  /**
   * Specify the maximum payload size in bytes of a bulk request.
   * @param bulkMaxPayloadSize  The maximum payload size in bytes of a bulk
   *                            request.
   */
  public void setBulkMaxPayloadSize(final long bulkMaxPayloadSize)
  {
    this.bulkMaxPayloadSize = bulkMaxPayloadSize;
  }



  /**
   * Specify the maximum number of concurrent bulk requests.
   * @param bulkMaxConcurrentRequests  The maximum number of concurrent bulk
   *                                   requests.
   */
  public void setBulkMaxConcurrentRequests(final int bulkMaxConcurrentRequests)
  {
    bulkMaxConcurrentRequestsSemaphore.setMaxPermits(bulkMaxConcurrentRequests);
  }



  /**
   * Retrieve the resource descriptors keyed by name of endpoint.
   * @return  The resource descriptors keyed by name of endpoint.
   */
  public Map<String, ResourceDescriptor> getDescriptors()
  {
    return descriptors;
  }



  /**
   * Return the directory that should be used to store temporary files, or
   * {@code null} for the system dependent default temporary-file
   * directory (specified by the system property {@code java.io.tmpdir}.
   *
   * @return  The directory that should be used to store temporary files, or
   *          {@code null} for the system dependent default temporary-file
   *          directory.
   */
  public File getTmpDataDir()
  {
    return tmpDataDir;
  }



  /**
   * Return the directory that should be used to store temporary files, or
   * {@code null} for the system dependent default temporary-file
   * directory (specified by the system property {@code java.io.tmpdir}.
   *
   * @param tmpDataDir  The directory that should be used to store temporary
   *                    files, or {@code null} for the system dependent default
   *                    temporary-file directory.
   */
  public void setTmpDataDir(final File tmpDataDir)
  {
    this.tmpDataDir = tmpDataDir;
  }



  /**
   * Attempt to acquire a permit to process a bulk request.
   *
   * @throws SCIMException  If a permit cannot be immediately obtained.
   */
  public void acquireBulkRequestPermit()
      throws SCIMException
  {
    if (!bulkMaxConcurrentRequestsSemaphore.tryAcquire())
    {
      throw SCIMException.createException(
          503, "The server is currently processing the maximum number " +
               "of concurrent bulk requests (" +
               bulkMaxConcurrentRequestsSemaphore.getMaxPermits() + ")");
    }
  }



  /**
   * Release a permit to process a bulk request.
   */
  public void releaseBulkRequestPermit()
  {
    bulkMaxConcurrentRequestsSemaphore.release();
  }
}
