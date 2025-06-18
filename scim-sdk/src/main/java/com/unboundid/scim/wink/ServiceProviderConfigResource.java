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

package com.unboundid.scim.wink;

import com.unboundid.scim.data.ServiceProviderConfig;
import org.glassfish.jersey.message.internal.Quality;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import static com.unboundid.scim.sdk.SCIMConstants.
    RESOURCE_ENDPOINT_SERVICE_PROVIDER_CONFIG;
import static com.unboundid.scim.sdk.SCIMConstants.
    RESOURCE_NAME_SERVICE_PROVIDER_CONFIG;


/**
 * This class is a JAX-RS resource for the SCIM Service Provider Configuration.
 */
@Path(RESOURCE_ENDPOINT_SERVICE_PROVIDER_CONFIG)
public class ServiceProviderConfigResource extends AbstractStaticResource
{
  private final SCIMApplication application;

  /**
   * Create a new JAX-RS resource.
   *
   * @param application    The SCIM JAX-RS application associated with this
   *                       resource.
   */
  public ServiceProviderConfigResource(final SCIMApplication application) {
    this.application = application;
  }

  /**
   * Implement the GET operation to fetch the configuration in JSON format.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON + ";"
            + Quality.QUALITY_SOURCE_PARAMETER_NAME + "=1")
  public Response doJsonGet()
  {
    final ServiceProviderConfig config = application.getServiceProviderConfig();
    Response.ResponseBuilder builder = Response.ok();

    setResponseEntity(builder, MediaType.APPLICATION_JSON_TYPE, config);
    application.getStatsForResource(RESOURCE_NAME_SERVICE_PROVIDER_CONFIG).
        incrementStat(ResourceStats.GET_RESPONSE_JSON);
    application.getStatsForResource(RESOURCE_NAME_SERVICE_PROVIDER_CONFIG).
        incrementStat(ResourceStats.GET_OK);
    return builder.build();
  }



  /**
   * Implement the GET operation to fetch the configuration in XML format.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces(MediaType.APPLICATION_XML + ";"
            + Quality.QUALITY_SOURCE_PARAMETER_NAME + "=0.5")
  public Response doXmlGet()
  {
    final ServiceProviderConfig config = application.getServiceProviderConfig();
    Response.ResponseBuilder builder = Response.ok();

    setResponseEntity(builder, MediaType.APPLICATION_XML_TYPE, config);
    application.getStatsForResource(RESOURCE_NAME_SERVICE_PROVIDER_CONFIG).
        incrementStat(ResourceStats.GET_RESPONSE_XML);
    application.getStatsForResource(RESOURCE_NAME_SERVICE_PROVIDER_CONFIG).
        incrementStat(ResourceStats.GET_OK);
    return builder.build();
  }
}
