/*
 * Copyright 2011-2013 UnboundID Corp.
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
import com.unboundid.scim.sdk.OAuthTokenHandler;
import com.unboundid.scim.schema.CoreSchema;
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
  private final Map<String,ResourceStats> resourceStats;
  private final SCIMBackend backend;
  private final boolean supportsOAuth;
  private volatile long bulkMaxOperations = Long.MAX_VALUE;
  private volatile long bulkMaxPayloadSize = Long.MAX_VALUE;
  private volatile File tmpDataDir = null;
  private AdjustableSemaphore bulkMaxConcurrentRequestsSemaphore =
      new AdjustableSemaphore(Integer.MAX_VALUE);


  /**
   * Create a new SCIMApplication that defines the endpoints provided by the
   * ResourceDescriptors and uses the provided backend to process the request.
   *
   * @param backend The backend that should be used to process the requests.
   * @param tokenHandler The OAuthTokenHandler implementation to use (this may
   *                     be {@code null}.
   */
  public SCIMApplication(
      final SCIMBackend backend,
      final OAuthTokenHandler tokenHandler)
  {
    instances = new HashSet<Object>();

    instances.add(new RootResource(this));
    instances.add(new MonitorResource(this));

    instances.add(new ServiceProviderConfigResource(this));
    instances.add(new XMLServiceProviderConfigResource(this));
    instances.add(new JSONServiceProviderConfigResource(this));

    // The Bulk operation endpoint.
    instances.add(new BulkResource(this, tokenHandler));
    instances.add(new JSONBulkResource(this, tokenHandler));
    instances.add(new XMLBulkResource(this, tokenHandler));

    instances.add(new SCIMResource(this, tokenHandler));
    instances.add(new XMLQueryResource(this, tokenHandler));
    instances.add(new JSONQueryResource(this, tokenHandler));

    this.resourceStats = new HashMap<String, ResourceStats>();
    this.backend = backend;

    if (tokenHandler != null)
    {
      supportsOAuth = true;
    }
    else
    {
      supportsOAuth = false;
    }
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
    ResourceStats stats = resourceStats.get(resourceName);
    if(stats == null)
    {
      stats = new ResourceStats(resourceName);
      resourceStats.put(resourceName, stats);
    }
    return stats;
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
    serviceProviderConfig.setSortConfig(
        new SortConfig(backend.supportsSorting()));
    serviceProviderConfig.setETagConfig(new ETagConfig(false));

    final List<AuthenticationScheme> authenticationSchemes =
        new ArrayList<AuthenticationScheme>();
    authenticationSchemes.addAll(backend.getSupportedAuthenticationSchemes());

    if (supportsOAuth)
    {
      authenticationSchemes.add(AuthenticationScheme.createOAuth2(false));
    }

    serviceProviderConfig.setAuthenticationSchemes(authenticationSchemes);

    serviceProviderConfig.setXmlDataFormatConfig(new XmlDataFormatConfig(true));

    return serviceProviderConfig;
  }



  /**
   * Retrieves the SCIMBackend used by this SCIMApplication.
   *
   * @return The SCIMBackend used by this SCIMApplication.
   */
  public SCIMBackend getBackend()
  {
    return backend;
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
