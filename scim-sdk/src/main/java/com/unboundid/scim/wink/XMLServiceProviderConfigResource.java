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

import com.unboundid.scim.data.ServiceProviderConfig;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.unboundid.scim.sdk.SCIMConstants.
    RESOURCE_ENDPOINT_SERVICE_PROVIDER_CONFIG;
import static com.unboundid.scim.sdk.SCIMConstants.
    RESOURCE_NAME_SERVICE_PROVIDER_CONFIG;


/**
 * This class is a JAX-RS resource for the SCIM Service Provider Configuration
 * where the response format is specified in the URL to be XML.
 */
@Path(RESOURCE_ENDPOINT_SERVICE_PROVIDER_CONFIG + ".xml")
public class XMLServiceProviderConfigResource extends AbstractStaticResource
{
  private final SCIMApplication application;

  /**
   * Create a new JAX-RS resource.
   *
   * @param application    The SCIM JAX-RS application associated with this
   *                       resource.
   */
  public XMLServiceProviderConfigResource(final SCIMApplication application) {
    this.application = application;
  }

  /**
   * Implement the GET operation to fetch the configuration in XML format.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces(MediaType.APPLICATION_XML)
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
