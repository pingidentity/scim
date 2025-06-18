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

import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.Version;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;


/**
 * This class is a JAX-RS resource to allow monitor data to be fetched and
 * reset.
 */
@Path("monitor")
public class MonitorResource
{
  private static final String RESOURCE_NAME = "monitor";
  private final SCIMApplication application;

  /**
   * Create a new JAX-RS resource.
   *
   * @param application    The SCIM JAX-RS application associated with this
   *                       resource.
   */
  public MonitorResource(final SCIMApplication application) {
    this.application = application;
  }

  /**
   * Implement the GET operation on the monitor resource to fetch the monitor
   * data in JSON format.
   *
   * @return  The response to the request.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonGet()
  {
    try
    {
      final JSONStringer writer = new JSONStringer();
      writeMonitorData(writer);
      application.getStatsForResource(RESOURCE_NAME).incrementStat(
          ResourceStats.GET_RESPONSE_JSON);
      application.getStatsForResource(RESOURCE_NAME).incrementStat(
          ResourceStats.GET_OK);
      return Response.ok(writer.toString(), MediaType.APPLICATION_JSON).build();
    }
    catch (JSONException e)
    {
      Debug.debugException(e);
      application.getStatsForResource(RESOURCE_NAME).incrementStat(
          ResourceStats.GET_INTERNAL_SERVER_ERROR);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }



  /**
   * Write the monitor data in JSON format.
   *
   * @param writer  A JSON writer where the monitor data is to be written.
   *
   * @throws JSONException  If an error occurs while formatting the data.
   */
  private void writeMonitorData(final JSONWriter writer)
      throws JSONException
  {
    writer.object();
    writer.key("version");
    writer.value(Version.VERSION);
    writer.key("build");
    writer.value(Version.BUILD_TIMESTAMP);
    writer.key("revision");
    writer.value(Version.REVISION_ID);

    writer.key("resources");
    writer.array();
    for(ResourceStats stats : application.getResourceStats())
    {
      writer.object();
      writer.key("name");
      writer.value(stats.getName());
      for(Map.Entry<String, Long> stat : stats.getStats().entrySet())
      {
        writer.key(stat.getKey());
        writer.value(stat.getValue());
      }
      writer.endObject();
    }
    writer.endArray();
    writer.endObject();
  }
}
