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

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.BulkContentHandler;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



/**
 * This class provides a SCIM object un-marshaller implementation to read SCIM
 * objects from their JSON representation.
 */
public class JsonUnmarshaller implements Unmarshaller
{
  /**
   * {@inheritDoc}
   */
  public <R extends BaseResource> R unmarshal(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory) throws InvalidResourceException
  {
    try
    {
      final JSONObject jsonObject =
          new JSONObject(new JSONTokener(inputStream));

      final JsonParser parser = new JsonParser();
      return parser.unmarshal(jsonObject, resourceDescriptor, resourceFactory,
                              null);
    }
    catch(JSONException e)
    {
      throw new InvalidResourceException("Error while reading JSON: " +
          e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public <R extends BaseResource> Resources<R> unmarshalResources(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory) throws InvalidResourceException
  {
    try
    {
      final JsonParser parser = new JsonParser();
      final JSONObject jsonObject =
          new JSONObject(new JSONTokener(inputStream));

      int totalResults = 0;
      if(jsonObject.has("totalResults"))
      {
        totalResults = jsonObject.getInt("totalResults");
      }

      int startIndex = 1;
      if(jsonObject.has("startIndex"))
      {
        startIndex = jsonObject.getInt("startIndex");
      }

      final JSONArray schemas = jsonObject.optJSONArray("schemas");

      List<R> resources = Collections.emptyList();
      if(jsonObject.has("Resources"))
      {
        JSONArray resourcesArray = jsonObject.getJSONArray("Resources");
        resources = new ArrayList<R>(resourcesArray.length());
        for(int i = 0; i < resourcesArray.length(); i++)
        {
          R resource =
              parser.unmarshal(resourcesArray.getJSONObject(i),
                               resourceDescriptor,
                               resourceFactory,
                               schemas);
          resources.add(resource);
        }
      }

      return new Resources<R>(resources, totalResults, startIndex);
    }
    catch(JSONException e)
    {
      throw new InvalidResourceException("Error while reading JSON: " +
          e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public SCIMException unmarshalError(final InputStream inputStream)
      throws InvalidResourceException
  {
    try
    {
      final JSONObject jsonObject =
          new JSONObject(new JSONTokener(inputStream));

      if(jsonObject.has("Errors"))
      {
        JSONArray errors = jsonObject.getJSONArray("Errors");
        if(errors.length() >= 1)
        {
          JSONObject error = errors.getJSONObject(0);
          int code = error.optInt("code");
          String description = error.optString("description");
          return SCIMException.createException(code, description);
        }
      }
      return null;
    }
    catch (JSONException e)
    {
      throw new InvalidResourceException("Error while reading JSON: " +
          e.getMessage(), e);
    }
  }



  /**
   * {@inheritDoc}
   */
  public void bulkUnmarshal(final InputStream inputStream,
                            final BulkConfig bulkConfig,
                            final BulkContentHandler handler)
      throws SCIMException
  {
    final JsonBulkParser bulkUnmarshaller =
        new JsonBulkParser(inputStream, bulkConfig, handler);
    bulkUnmarshaller.unmarshal();
  }



}
