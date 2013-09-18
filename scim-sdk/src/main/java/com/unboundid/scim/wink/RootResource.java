/*
 * Copyright 2013 UnboundID Corp.
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

import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;



/**
 * Default content for the SCIM service base URL.
 */
@Path("")
public class RootResource extends AbstractStaticResource
{
  /**
   * Create a new JAX-RS resource.
   *
   * @param application    The SCIM JAX-RS application associated with this
   *                       resource.
   */
  public RootResource(final SCIMApplication application)
  {
  }



  /**
   * Create a SCIM response for the SCIM service base URL.
   * @return  A SCIM response.
   */
  private SCIMResponse getResponse()
  {
    return SCIMException.createException(
        200,
        "You have accessed the SCIM service base URL. You must append " +
        "a SCIM endpoint name to the URL to access SCIM resources (e.g. " +
        "/Schemas, /Users)");
  }



  /**
   * Implement the GET operation to return JSON format.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonGet()
  {
    final SCIMResponse response = getResponse();
    Response.ResponseBuilder builder = Response.ok();

    setResponseEntity(builder, MediaType.APPLICATION_JSON_TYPE, response);
    return builder.build();
  }



  /**
   * Implement the GET operation to return XML format.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces(MediaType.APPLICATION_XML)
  public Response doXmlGet()
  {
    final SCIMResponse response = getResponse();
    Response.ResponseBuilder builder = Response.ok();

    setResponseEntity(builder, MediaType.APPLICATION_XML_TYPE, response);
    return builder.build();
  }
}
