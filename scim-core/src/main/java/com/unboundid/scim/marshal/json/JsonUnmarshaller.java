/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshal.json;

import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
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
  public SCIMObject unmarshal(final File file, final String resourceName)
      throws Exception
  {
    final FileInputStream fileInputStream = new FileInputStream(file);
    try
    {
      return unmarshal(fileInputStream, resourceName);
    }
    finally
    {
      fileInputStream.close();
    }
  }



  /**
   * {@inheritDoc}
   */
  public SCIMObject unmarshal(final InputStream inputStream,
                              final String resourceName)
      throws Exception
  {
    final SCIMObject scimObject = new SCIMObject();
    final JSONObject jsonObject = new JSONObject(new JSONTokener(inputStream));

    // The first keyed object ought to be a schemas array, but it may not be
    // present if 1) the attrs are all core and 2) the client decided to omit
    // the schema declaration.
    //Object schemas = jsonObject.get(SCIMObject.SCHEMAS_ATTRIBUTE_NAME);

    final ResourceDescriptor resourceDescriptor =
        SchemaManager.instance().getResourceDescriptor(resourceName);
    if (resourceDescriptor == null)
    {
      throw new RuntimeException("No resource descriptor found for " +
                                 resourceName);
    }

    scimObject.setResourceName(resourceDescriptor.getName());

    for (AttributeDescriptor attributeDescriptor : resourceDescriptor
        .getAttributeDescriptors())
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
        else if (attributeDescriptor.isComplex())
        {
          attr = createComplexAttribute((JSONObject) jsonAttribute,
                                        attributeDescriptor);
        }
        else
        {
          attr = this.createSimpleAttribute(jsonAttribute, attributeDescriptor);
        }
        scimObject.addAttribute(attr);
      }
    }
    return scimObject;
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
    return SCIMAttribute.createSimpleAttribute(attributeDescriptor,
                                               jsonAttribute.toString());
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
        new LinkedList<SCIMAttributeValue>();

    final List<AttributeDescriptor> complexAttributeDescriptors =
        attributeDescriptor.getComplexAttributeDescriptors();
    // for a plural there should only be a single child. For example,
    // in the case of the plural attribute 'emails' we should find 'email'
    final String pluralsChildAttributeName =
        complexAttributeDescriptors.get(0).getName();

    for (int i = 0; i < jsonAttribute.length(); i++)
    {
      final JSONObject jsonObject = jsonAttribute.getJSONObject(i);
      final AttributeDescriptor pluralAttributeDescriptorInstance =
          attributeDescriptor.getAttribute(pluralsChildAttributeName);
      pluralScimAttributes.add(SCIMAttributeValue.createComplexValue(
          createComplexAttribute(jsonObject,
                                 pluralAttributeDescriptorInstance)));
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
  private SCIMAttribute createComplexAttribute(
      final JSONObject jsonAttribute,
      final AttributeDescriptor attributeDescriptor) throws JSONException
  {
    final SCIMAttribute complexScimAttr;
    final Iterator keys = jsonAttribute.keys();
    final List<SCIMAttribute> complexAttrs = new LinkedList<SCIMAttribute>();
    while (keys.hasNext())
    {
      final String key = (String) keys.next();
      final Object o = jsonAttribute.get(key);
      final AttributeDescriptor complexAttr =
          attributeDescriptor.getAttribute(key);
      if (complexAttr != null)
      {
        SCIMAttribute childAttr = createSimpleAttribute(o, complexAttr);
        complexAttrs.add(childAttr);
      }
    }

    complexScimAttr = SCIMAttribute.createSingularAttribute(
        attributeDescriptor,
        SCIMAttributeValue.createComplexValue(complexAttrs));
    return complexScimAttr;
  }
}
