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

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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

      return unmarshal(jsonObject, resourceDescriptor, resourceFactory);
    }
    catch(JSONException e)
    {
      throw new InvalidResourceException("Error while reading JSON: " +
          e.getMessage(), e);
    }
  }

  /**
   * Read an SCIM resource from the specified JSON object.
   *
   * @param <R> The type of resource instance.
   * @param jsonObject  The JSON object to be read.
   * @param resourceDescriptor The descriptor of the SCIM resource to be read.
   * @param resourceFactory The resource factory to use to create the resource
   *                        instance.
   *
   * @return  The SCIM resource that was read.
   *
   * @throws JSONException If an error occurred.
   * @throws InvalidResourceException if a schema error occurs.
   */
  private <R extends BaseResource> R unmarshal(final JSONObject jsonObject,
                               final ResourceDescriptor resourceDescriptor,
                               final ResourceFactory<R> resourceFactory)
      throws JSONException, InvalidResourceException
  {
    final SCIMObject scimObject = new SCIMObject();

    // The first keyed object ought to be a schemas array, but it may not be
    // present if 1) the attrs are all core and 2) the client decided to omit
    // the schema declaration.
    final JSONArray schemas = jsonObject.optJSONArray("schemas");

    // Read the core attributes.
    for (AttributeDescriptor attributeDescriptor : resourceDescriptor
        .getAttributes())
    {
      final String externalAttributeName = attributeDescriptor.getName();
      final Object jsonAttribute = jsonObject.opt(externalAttributeName);
      if (jsonAttribute != null)
      {
        scimObject.addAttribute(
            create(attributeDescriptor, jsonAttribute));
      }
    }

    // Read the extension attributes.
    if (schemas != null)
    {
      for (int i = 0; i < schemas.length(); i++)
      {
        final String schema = schemas.getString(i);
        if (schema.equalsIgnoreCase(SCIMConstants.SCHEMA_URI_CORE))
        {
          continue;
        }

        final JSONObject schemaAttrs = jsonObject.optJSONObject(schema);
        if (schemaAttrs != null)
        {
          if (resourceDescriptor.getAttributeSchemas().contains(schema))
          {
            final Iterator keys = schemaAttrs.keys();
            while (keys.hasNext())
            {
              final String attributeName = (String) keys.next();
              final AttributeDescriptor attributeDescriptor =
                  resourceDescriptor.getAttribute(schema, attributeName);
              final Object jsonAttribute = schemaAttrs.get(attributeName);
              scimObject.addAttribute(
                  create(attributeDescriptor, jsonAttribute));
            }
          }
        }
      }
    }

    return resourceFactory.createResource(resourceDescriptor, scimObject);
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

      List<R> resources = Collections.emptyList();
      if(jsonObject.has("Resources"))
      {
        JSONArray resourcesArray = jsonObject.getJSONArray("Resources");
        resources = new ArrayList<R>(resourcesArray.length());
        for(int i = 0; i < resourcesArray.length(); i++)
        {
          R resource =
              unmarshal(resourcesArray.getJSONObject(i), resourceDescriptor,
                  resourceFactory);
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
          int code = error.getInt("code"); // TODO: code is optional!
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
   * Parse a simple attribute from its representation as a JSON Object.
   *
   * @param jsonAttribute       The JSON object representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   */
  private SCIMAttribute createSimpleAttribute(
      final Object jsonAttribute,
      final AttributeDescriptor attributeDescriptor)
  {
    return SCIMAttribute.create(attributeDescriptor,
        SCIMAttributeValue.createStringValue(jsonAttribute.toString()));
  }



  /**
   * Parse a multi-valued attribute from its representation as a JSON Object.
   *
   * @param jsonAttribute       The JSON object representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   *
   * @throws JSONException Thrown if error creating multi-valued attribute.
   * @throws InvalidResourceException if a schema error occurs.
   */
  private SCIMAttribute createMutiValuedAttribute(
      final JSONArray jsonAttribute,
      final AttributeDescriptor attributeDescriptor)
      throws JSONException, InvalidResourceException
  {
    final List<SCIMAttributeValue> values =
        new ArrayList<SCIMAttributeValue>(jsonAttribute.length());

    for (int i = 0; i < jsonAttribute.length(); i++)
    {
      Object o = jsonAttribute.get(i);
      SCIMAttributeValue value;
      if(o instanceof JSONObject)
      {
        value = createComplexAttribute((JSONObject) o, attributeDescriptor);
      }
      else
      {
        SCIMAttribute subAttr = SCIMAttribute.create(
            attributeDescriptor.getSubAttribute("value"),
            SCIMAttributeValue.createStringValue(o.toString()));
        value = SCIMAttributeValue.createComplexValue(subAttr);
      }
      values.add(value);
    }
    SCIMAttributeValue[] vals =
        new SCIMAttributeValue[values.size()];
    vals = values.toArray(vals);
    return SCIMAttribute.create(attributeDescriptor, vals);
  }



  /**
   * Parse a complex attribute from its representation as a JSON Object.
   *
   * @param jsonAttribute       The JSON object representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   *
   * @throws org.json.JSONException Thrown if error creating complex attribute.
   * @throws InvalidResourceException if a schema error occurs.
   */
  private SCIMAttributeValue createComplexAttribute(
      final JSONObject jsonAttribute,
      final AttributeDescriptor attributeDescriptor)
      throws JSONException, InvalidResourceException
  {
    final Iterator keys = jsonAttribute.keys();
    final List<SCIMAttribute> complexAttrs =
        new ArrayList<SCIMAttribute>(jsonAttribute.length());
    while (keys.hasNext())
    {
      final String key = (String) keys.next();
      final AttributeDescriptor subAttribute =
          attributeDescriptor.getSubAttribute(key);
      if (subAttribute != null)
      {
        SCIMAttribute childAttr;
        // Allow multi-valued sub-attribute as the resource schema needs this.
        if (subAttribute.isMultiValued())
        {
          final JSONArray o = jsonAttribute.getJSONArray(key);
          childAttr = createMutiValuedAttribute(o, subAttribute);
        }
        else
        {
          final Object o = jsonAttribute.get(key);
          childAttr = createSimpleAttribute(o, subAttribute);
        }
        complexAttrs.add(childAttr);
      }
    }

    return SCIMAttributeValue.createComplexValue(complexAttrs);
  }



  /**
   * Create a SCIM attribute from its JSON object representation.
   *
   * @param descriptor     The attribute descriptor.
   * @param jsonAttribute  The JSON object representing the attribute.
   *
   * @return  The created SCIM attribute.
   *
   * @throws JSONException If the JSON object is not valid.
   * @throws InvalidResourceException If a schema error occurs.
   */
  private SCIMAttribute create(
      final AttributeDescriptor descriptor, final Object jsonAttribute)
      throws JSONException, InvalidResourceException
  {
    if (descriptor.isMultiValued())
    {
      return createMutiValuedAttribute((JSONArray) jsonAttribute, descriptor);
    }
    else if (descriptor.getDataType() == AttributeDescriptor.DataType.COMPLEX)
    {
      return SCIMAttribute.create(
          descriptor,
          createComplexAttribute((JSONObject) jsonAttribute, descriptor));
    }
    else
    {
      return this.createSimpleAttribute(jsonAttribute, descriptor);
    }
  }
}
