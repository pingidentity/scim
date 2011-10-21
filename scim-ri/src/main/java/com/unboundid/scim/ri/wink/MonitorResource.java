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

import com.unboundid.scim.ri.SCIMMonitorData;
import com.unboundid.scim.ri.SCIMServer;
import com.unboundid.scim.sdk.Debug;
import org.json.JSONException;
import org.json.JSONStringer;
import org.json.JSONWriter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.Collection;



/**
 * This class is a JAX-RS resource to allow monitor data to be fetched and
 * reset.
 */
@Path("monitor")
public class MonitorResource
{
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
      return Response.ok(writer.toString(), MediaType.APPLICATION_JSON).build();
    }
    catch (JSONException e)
    {
      Debug.debugException(e);
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
    final SCIMServer scimServer = SCIMServer.getInstance();
    final SCIMMonitorData monitorData = scimServer.getMonitorData();

    writer.object();
    for (final Method m : monitorData.getClass().getMethods())
    {
      String name = m.getName();
      if (name.startsWith("get") && (m.getParameterTypes().length == 0))
      {
        if (name.equals("getClass"))
        {
          continue;
        }

        if (m.getReturnType().isArray())
        {
          continue;
        }

        try
        {
          final StringBuilder builder = new StringBuilder();
          builder.append(Character.toLowerCase(name.charAt(3)));
          if (name.length() > 4)
          {
            builder.append(name.substring(4));
          }

          final String statName = builder.toString();
          final Object value = m.invoke(monitorData);
          if (value == null)
          {
            continue;
          }

          writer.key(statName);
          if (value instanceof Collection)
          {
            writer.array();
            for (final Object o : (Collection)value)
            {
              writer.value(o);
            }
            writer.endArray();
          }
          else
          {
            writer.value(value);
          }
        }
        catch (Exception e)
        {
          Debug.debugException(e);
        }
      }
    }
    writer.endObject();
  }
}
