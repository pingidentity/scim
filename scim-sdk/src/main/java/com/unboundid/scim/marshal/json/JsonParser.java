/*
 * Copyright 2012-2015 UnboundID Corp.
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
import com.unboundid.scim.schema.CoreSchema;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.unboundid.scim.sdk.StaticUtils.toLowerCase;



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
      final boolean implicitSchemaChecking = Boolean.getBoolean(
              SCIMConstants.IMPLICIT_SCHEMA_CHECKING_PROPERTY);

      // The first keyed object ought to be a schemas array, but it may not be
      // present if 1) the attrs are all core and 2) the client decided to omit
      // the schema declaration.
      final JSONArray schemas;
      if (jsonObject.has(SCIMConstants.SCHEMAS_ATTRIBUTE_NAME))
      {
        schemas = jsonObject.getJSONArray(SCIMConstants.SCHEMAS_ATTRIBUTE_NAME);
      }
      else if (defaultSchemas != null)
      {
        schemas = defaultSchemas;
      }
      else
      {
        String[] schemaArray = new String[1];
        schemaArray[0] = resourceDescriptor.getSchema();
        schemas = new JSONArray(schemaArray);
      }

      final Set<String> schemaSet = new HashSet<String>(schemas.length());
      if (implicitSchemaChecking)
      {
        schemaSet.addAll(resourceDescriptor.getAttributeSchemas());
      }
      for (int i = 0; i < schemas.length(); i++)
      {
        schemaSet.add(toLowerCase(schemas.getString(i)));
      }

      final Iterator k = jsonObject.keys();
      while (k.hasNext())
      {
        final String attributeKey = (String) k.next();
        final String attributeKeyLower = toLowerCase(attributeKey);

        if(SCIMConstants.SCHEMAS_ATTRIBUTE_NAME.equals(attributeKeyLower))
        {
          continue;
        }

        if (schemaSet.contains(attributeKeyLower))
        {
          //This key is a container for some extended schema
          JSONObject schemaAttrs = jsonObject.getJSONObject(attributeKey);
          final Iterator keys = schemaAttrs.keys();
          while (keys.hasNext())
          {
            final String attributeName = (String) keys.next();
            final AttributeDescriptor attributeDescriptor =
                   resourceDescriptor.getAttribute(attributeKey, attributeName);
            final Object jsonAttribute = schemaAttrs.get(attributeName);
            scimObject.addAttribute(
                    create(attributeDescriptor, jsonAttribute));
          }
        }
        else
        {
          final Object jsonAttribute = jsonObject.get(attributeKey);
          if (implicitSchemaChecking)
          {
            //Try to determine the schema for this attribute
            final String attributeName = attributeKey;
            final String schema =
                    resourceDescriptor.findAttributeSchema(attributeName);
            final AttributeDescriptor attributeDescriptor =
                    resourceDescriptor.getAttribute(schema, attributeName);
            if (CoreSchema.META_DESCRIPTOR.equals(attributeDescriptor))
            {
              try
              {
                // Special implicit schema processing for meta.attributes
                // which contains the names of the attributes to remove from
                // the Resource during a PATCH operation.  These each should be
                // fully qualified with schema urn by the client, but if they
                // are not we can try to determine the schema here.
                JSONObject jsonMetaObj = ((JSONObject)jsonAttribute);
                JSONArray metaAttrs = null;
                final Iterator keys = jsonMetaObj.keys();
                while (keys.hasNext())
                {
                  final String key = (String) keys.next();
                  if ("attributes".equals(key.toLowerCase()))
                  {
                    Object attrObj = jsonMetaObj.get(key);
                    if (attrObj instanceof JSONArray)
                    {
                      metaAttrs = (JSONArray) attrObj;
                    }
                    break;
                  }
                }
                if (metaAttrs != null)
                {
                  JSONArray newMetaAttrs = new JSONArray();
                  for (int i=0; i < metaAttrs.length(); i++)
                  {
                    String metaAttr = (String) metaAttrs.get(i);
                    String metaSchema = resourceDescriptor.findAttributeSchema(
                            metaAttr);
                    // The schema returned will be null if attribute value was
                    // already fully qualified.
                    if (metaSchema != null)
                    {
                      metaAttr = metaSchema +
                              SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE +
                              metaAttr;
                    }
                    newMetaAttrs.put(metaAttr);
                  }
                  jsonMetaObj.put("attributes", newMetaAttrs);
                }
              }
              catch (Exception ignore)
              {
                // Don't fail because of implicit schema checking
              }
            }
            scimObject.addAttribute(create(attributeDescriptor, jsonAttribute));
          }
          else
          {
            if (!schemaSet.contains(SCIMConstants.SCHEMA_URI_CORE))
            {
              throw new Exception("'" + SCIMConstants.SCHEMA_URI_CORE +
                      "' must be declared in the schemas attribute.");
            }
            final AttributeDescriptor attributeDescriptor =
                      resourceDescriptor.getAttribute(
                              SCIMConstants.SCHEMA_URI_CORE,
                              attributeKey);
            scimObject.addAttribute(create(attributeDescriptor, jsonAttribute));
          }
        }
      }

      return resourceFactory.createResource(resourceDescriptor, scimObject);
    }
    catch (Exception e)
    {
      throw new InvalidResourceException(
          "Resource '" + resourceDescriptor.getName() + "' is malformed: " +
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
  protected SCIMAttribute createSimpleAttribute(
      final Object jsonAttribute,
      final AttributeDescriptor attributeDescriptor)
  {
    return SCIMAttribute.create(attributeDescriptor,
        SCIMAttributeValue.createValue(attributeDescriptor.getDataType(),
                                       jsonAttribute.toString()));
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
            SCIMAttributeValue.createValue(attributeDescriptor.getDataType(),
                                           o.toString()));
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
      JSONArray jsonArray;
      if (jsonAttribute instanceof JSONArray)
      {
        jsonArray = (JSONArray) jsonAttribute;
      }
      else
      {
        String[] s = new String[1];
        s[0] = jsonAttribute.toString();
        jsonArray = new JSONArray(s);
      }

      return createMutiValuedAttribute(jsonArray, descriptor);
    }
    else if (descriptor.getDataType() == AttributeDescriptor.DataType.COMPLEX)
    {
      if (!(jsonAttribute instanceof JSONObject))
      {
        throw new InvalidResourceException(
            "JSON object expected for complex attribute '" +
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



  /**
   * Returns a copy of the specified JSONObject with all the keys lower-cased.
   * This makes it much easier to use methods like JSONObject.opt() to find a
   * key when you don't know what the case is.
   *
   * @param jsonObject the original JSON object.
   * @return a new JSONObject with the keys all lower-cased.
   * @throws JSONException if there is an error creating the new JSONObject.
   */
  static final JSONObject makeCaseInsensitive(final JSONObject jsonObject)
          throws JSONException
  {
    if (jsonObject == null)
    {
      return null;
    }

    Iterator keys = jsonObject.keys();
    Map lowerCaseMap = new HashMap(jsonObject.length());
    while (keys.hasNext())
    {
      String key = keys.next().toString();
      String lowerCaseKey = toLowerCase(key);
      lowerCaseMap.put(lowerCaseKey, jsonObject.get(key));
    }

    return new JSONObject(lowerCaseMap);
  }
}
