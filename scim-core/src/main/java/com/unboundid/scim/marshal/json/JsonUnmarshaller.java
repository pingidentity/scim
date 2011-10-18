/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.MarshalException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
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
      final ResourceFactory<R> resourceFactory) throws MarshalException
  {
    try
    {
      final JSONObject jsonObject =
          new JSONObject(new JSONTokener(inputStream));

      return unmarshal(jsonObject, resourceDescriptor, resourceFactory);
    }
    catch(JSONException e)
    {
      throw new MarshalException("Error while reading JSON: " +
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
   */
  private <R extends BaseResource> R unmarshal(final JSONObject jsonObject,
                               final ResourceDescriptor resourceDescriptor,
                               final ResourceFactory<R> resourceFactory)
      throws JSONException
  {
    final SCIMObject scimObject = new SCIMObject();

    // The first keyed object ought to be a schemas array, but it may not be
    // present if 1) the attrs are all core and 2) the client decided to omit
    // the schema declaration.
    //Object schemas = jsonObject.get(SCIMObject.SCHEMAS_ATTRIBUTE_NAME);

    for (AttributeDescriptor attributeDescriptor : resourceDescriptor
        .getAttributes())
    {
      final String externalAttributeName = attributeDescriptor.getName();
      final Object jsonAttribute = jsonObject.opt(externalAttributeName);
      if (jsonAttribute != null)
      {
        final SCIMAttribute attr;
        if (attributeDescriptor.isPlural())
        {
          attr = createPluralAttribute((JSONArray) jsonAttribute,
                                       attributeDescriptor);
        }
        else if (attributeDescriptor.getDataType() ==
            AttributeDescriptor.DataType.COMPLEX)
        {
          attr = SCIMAttribute.createSingularAttribute(attributeDescriptor,
              createComplexAttribute((JSONObject) jsonAttribute,
                  attributeDescriptor));
        }
        else
        {
          attr = this.createSimpleAttribute(jsonAttribute, attributeDescriptor);
        }
        scimObject.addAttribute(attr);
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
      final ResourceFactory<R> resourceFactory) throws MarshalException
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
      throw new MarshalException("Error while reading JSON: " +
          e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   */
  public SCIMException unmarshalError(final InputStream inputStream)
      throws MarshalException
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
          String description = null;
          if(error.has("description"))
          {
            description = error.getString("description");
          }
          return SCIMException.createException(code, description);
        }
      }
      return null;
    }
    catch (JSONException e)
    {
      throw new MarshalException("Error while reading JSON: " +
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
    return SCIMAttribute.createSingularAttribute(attributeDescriptor,
        SCIMAttributeValue.createStringValue(jsonAttribute.toString()));
  }



  /**
   * Parse a plural attribute from its representation as a JSON Object.
   *
   * @param jsonAttribute       The JSON object representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   *
   * @throws org.json.JSONException Thrown if error creating plural attribute.
   */
  private SCIMAttribute createPluralAttribute(
      final JSONArray jsonAttribute,
      final AttributeDescriptor attributeDescriptor)
      throws JSONException
  {
    final List<SCIMAttributeValue> pluralScimAttributes =
        new ArrayList<SCIMAttributeValue>(jsonAttribute.length());

    for (int i = 0; i < jsonAttribute.length(); i++)
    {
      final JSONObject jsonObject = jsonAttribute.getJSONObject(i);
      pluralScimAttributes.add(
          createComplexAttribute(jsonObject, attributeDescriptor));
    }
    SCIMAttributeValue[] vals =
        new SCIMAttributeValue[pluralScimAttributes.size()];
    vals = pluralScimAttributes.toArray(vals);
    return SCIMAttribute.createPluralAttribute(attributeDescriptor, vals);
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
   */
  private SCIMAttributeValue createComplexAttribute(
      final JSONObject jsonAttribute,
      final AttributeDescriptor attributeDescriptor) throws JSONException
  {
    final Iterator keys = jsonAttribute.keys();
    final List<SCIMAttribute> complexAttrs =
        new ArrayList<SCIMAttribute>(jsonAttribute.length());
    while (keys.hasNext())
    {
      final String key = (String) keys.next();
      final Object o = jsonAttribute.get(key);
      final AttributeDescriptor complexAttr =
          attributeDescriptor.getSubAttribute(key);
      if (complexAttr != null)
      {
        SCIMAttribute childAttr = createSimpleAttribute(o, complexAttr);
        complexAttrs.add(childAttr);
      }
    }

    return SCIMAttributeValue.createComplexValue(complexAttrs);
  }
}
