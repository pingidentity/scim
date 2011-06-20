/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;



/**
 * This class represents a Simple Cloud Identity Management (SCIM) attribute
 * value. Values are categorized as either Simple or Complex.
 *
 * <ul>
 * <li>Simple values can be String, Boolean or DateTime.</li>
 * <li>Complex values are composed of a set of subordinate SCIM attributes.</li>
 * </ul>
 */
public final class SCIMAttributeValue
{
  /**
   * The simple attribute value, or {@code null} if the attribute value is
   * complex.
   */
  private final Object value;

  /**
   * The attributes comprising the complex value, keyed by the name of the
   * attribute, or {@code null} if the attribute value is simple.
   */
  private final Map<String,SCIMAttribute> attributes;



  /**
   * Create a new instance of a SCIM attribute value.
   *
   * @param value       The simple attribute value, or {@code null} if the
   *                    attribute value is complex.
   */
  private SCIMAttributeValue(final Object value)
  {
    this.value      = value;
    this.attributes = null;
  }



  /**
   * Create a new instance of a SCIM complex attribute value.
   *
   * @param attributes  The attributes comprising the complex value, keyed by
   *                    the name of the attribute.
   */
  private SCIMAttributeValue(final Map<String,SCIMAttribute> attributes)
  {
    this.value = null;
    this.attributes = attributes;
  }



  /**
   * Create a new simple String attribute value.
   *
   * @param value  The String attribute value.
   *
   * @return  The new simple attribute.
   */
  public static SCIMAttributeValue createStringValue(final String value)
  {
    return new SCIMAttributeValue(value);
  }



  /**
   * Create a new simple Boolean attribute value.
   *
   * @param value  The Boolean attribute value.
   *
   * @return  The new simple attribute.
   */
  public static SCIMAttributeValue createBooleanValue(final Boolean value)
  {
    return new SCIMAttributeValue(value);
  }



  /**
   * Create a new simple Date attribute value.
   *
   * @param value  The Date attribute value.
   *
   * @return  The new simple attribute.
   */
  public static SCIMAttributeValue createDateValue(final Date value)
  {
    return new SCIMAttributeValue(value);
  }


  @Override
  public String toString() {
    return "SCIMAttributeValue{" +
      "value=" + value +
      ", attributes=" + attributes +
      '}';
  }

  /**
   * Create a new complex attribute value from the provided attributes.
   *
   * @param attributes  The attributes comprising the complex value.
   *
   * @return  The new complex attribute.
   */
  public static SCIMAttributeValue createComplexValue(
      final SCIMAttribute ... attributes)
  {
    final Map<String,SCIMAttribute> map =
        new LinkedHashMap<String, SCIMAttribute>();
    for (final SCIMAttribute a : attributes)
    {
      if (map.containsKey(a.getName()))
      {
        throw new RuntimeException("Duplicate attribute " + a.getName() +
                                   " in complex attribute value");
      }
      map.put(a.getName(), a);
    }
    return new SCIMAttributeValue(Collections.unmodifiableMap(map));
  }



  /**
   * Create a new complex attribute value from a collection of attributes.
   *
   * @param attributes  The attributes comprising the complex value.
   *
   * @return  The new complex attribute value.
   */
  public static SCIMAttributeValue createComplexValue(
      final Collection<SCIMAttribute> attributes)
  {
    final Map<String,SCIMAttribute> map =
        new LinkedHashMap<String, SCIMAttribute>();
    for (final SCIMAttribute a : attributes)
    {
      if (map.containsKey(a.getName()))
      {
        throw new RuntimeException("Duplicate attribute " + a.getName() +
                                   " in complex attribute value");
      }
      map.put(a.getName(), a);
    }
    return new SCIMAttributeValue(Collections.unmodifiableMap(map));
  }



  /**
   * Create a new complex attribute value from a collection of attributes.
   *
   * @param schema   The URI for the schema that defines the attribute.
   * @param value    The string value of the attribute.
   * @param type     The value of the "type" sub-attribute.
   * @param primary  Specifies whether this value is the primary value.
   *
   * @return  The new plural attribute value.
   */
  public static SCIMAttributeValue createPluralStringValue(
      final String schema, final String value,
      final String type, final boolean primary)
  {
    final Map<String,SCIMAttribute> map =
        new LinkedHashMap<String, SCIMAttribute>();

    map.put("value",
            SCIMAttribute.createSingularStringAttribute(
                schema, "value", value));
    map.put("type",
            SCIMAttribute.createSingularStringAttribute(schema, "type", type));

    if (primary)
    {
      map.put("primary",
              SCIMAttribute.createSingularBooleanAttribute(
                  schema, "primary", primary));
    }

    return new SCIMAttributeValue(Collections.unmodifiableMap(map));
  }



  /**
   * Determines whether this attribute value is simple or complex.
   *
   * @return  {@code true} if this attribute value is complex, or {@code false}
   *          otherwise.
   */
  public boolean isComplex()
  {
    return this.value == null;
  }



  /**
   * Retrieves the simple String value, or {@code null} if the attribute
   * value is complex.
   *
   * @return  The simple String value, or {@code null} if the attribute
   *          value is complex.
   */
  public String getStringValue()
  {
    return (String)value;
  }



  /**
   * Retrieves the simple Boolean value, or {@code null} if the attribute
   * value is complex.
   *
   * @return  The simple Boolean value, or {@code null} if the attribute
   *          value is complex.
   */
  public Boolean getBooleanValue()
  {
    return (Boolean)value;
  }



  /**
   * Retrieves the simple Date value, or {@code null} if the attribute
   * value is complex.
   *
   * @return  The simple Date value, or {@code null} if the attribute
   *          value is complex.
   */
  public Date getDateValue()
  {
    return (Date)value;
  }



  /**
   * Retrieves the attributes comprising the complex value, keyed by the name
   * of the attribute, or {@code null} if the attribute value is simple.
   *
   * @return  The attributes comprising the complex value.
   */
  public Map<String, SCIMAttribute> getAttributes()
  {
    return attributes;
  }



  /**
   * Retrieves the attribute with the provided name from the complex value,
   * or {@code null} if there is no such attribute or the attribute value is
   * simple.
   *
   * @param attributeName  The name of the desired attribute.
   *
   * @return  The attribute with the provided name, or {@code null} if there
   *          is no such attribute or the attribute value is simple.
   */
  public SCIMAttribute getAttribute(final String attributeName)
  {
    if (attributes != null)
    {
      return attributes.get(attributeName);
    }
    else
    {
      return null;
    }
  }
}
