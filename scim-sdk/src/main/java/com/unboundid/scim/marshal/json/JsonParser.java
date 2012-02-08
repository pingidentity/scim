/*
 * Copyright 2012 UnboundID Corp.
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
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;



/**
 * Helper class for JSON unmarshalling.
 */
public class JsonParser
{
  /**
   * Read a SCIM resource from the specified JSON object.
   *
   * @param <R> The type of resource instance.
   * @param jsonObject  The JSON object to be read.
   * @param resourceDescriptor The descriptor of the SCIM resource to be read.
   * @param resourceFactory The resource factory to use to create the resource
   *                        instance.
   * @param defaultSchemas  The set of schemas used by attributes of the
   *                        resource, or {@code null} if the schemas must be
   *                        provided in the resource object.
   *
   * @return  The SCIM resource that was read.
   *
   * @throws JSONException If an error occurred.
   * @throws InvalidResourceException if a schema error occurs.
   */
  protected <R extends BaseResource> R unmarshal(
      final JSONObject jsonObject,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory,
      final JSONArray defaultSchemas)
      throws JSONException, InvalidResourceException
  {
    try
    {
      final SCIMObject scimObject = new SCIMObject();

      // The first keyed object ought to be a schemas array, but it may not be
      // present if 1) the attrs are all core and 2) the client decided to omit
      // the schema declaration.
      final JSONArray schemas;
      if (jsonObject.has("schemas"))
      {
        schemas = jsonObject.getJSONArray("schemas");
      }
      else
      {
        schemas = defaultSchemas;
      }

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
    catch (Exception e)
    {
      throw new InvalidResourceException(
          "Resource '" + resourceDescriptor.getName() + "' is malformed: " +
          e.getMessage());
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
  protected SCIMAttribute createSimpleAttribute(
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
  protected SCIMAttribute createMutiValuedAttribute(
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
  protected SCIMAttributeValue createComplexAttribute(
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
  protected SCIMAttribute create(
      final AttributeDescriptor descriptor, final Object jsonAttribute)
      throws JSONException, InvalidResourceException
  {
    if (descriptor.isMultiValued())
    {
      if (!(jsonAttribute instanceof JSONArray))
      {
        throw new InvalidResourceException(
            "JSON array expected for multi-valued attribute '" +
            descriptor.getName() + "'");
      }
      return createMutiValuedAttribute((JSONArray) jsonAttribute, descriptor);
    }
    else if (descriptor.getDataType() == AttributeDescriptor.DataType.COMPLEX)
    {
      if (!(jsonAttribute instanceof JSONObject))
      {
        throw new InvalidResourceException(
            "JSON object expected for multi-valued attribute '" +
            descriptor.getName() + "'");
      }
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
