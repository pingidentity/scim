/*
 * Copyright 2011-2014 UnboundID Corp.
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

package com.unboundid.scim.sdk;

import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.schema.AttributeDescriptor;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;



/**
 * This class represents a System for Cross-Domain Identity Management (SCIM)
 * attribute value. Values are categorized as either Simple or Complex.
 *
 * <ul>
 * <li>Simple values can be String, Boolean, DateTime, Integer or Binary.</li>
 * <li>Complex values are composed of a set of subordinate SCIM attributes.</li>
 * </ul>
 */
public abstract class SCIMAttributeValue
{
  /**
   * Create a new simple attribute value of the specified data type.
   *
   * @param dataType  The data type of the value.
   * @param value     The string representation of the value.
   *
   * @return  The new simple attribute value.
   */
  public static SCIMAttributeValue createValue(
      final AttributeDescriptor.DataType dataType,
      final String value)
  {
    switch (dataType)
    {
      case BINARY:
        return createBinaryValue(DatatypeConverter.parseBase64Binary(value));
      default:
        return createStringValue(value);
    }
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
    return new SimpleSCIMAttributeValue(new SimpleValue(value));
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
    return new SimpleSCIMAttributeValue(new SimpleValue(value));
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
    return new SimpleSCIMAttributeValue(new SimpleValue(value));
  }



  /**
   * Create a new simple binary attribute value.
   *
   * @param value  The binary attribute value.
   *
   * @return  The new simple attribute.
   */
  public static SCIMAttributeValue createBinaryValue(final byte[] value)
  {
    return new SimpleSCIMAttributeValue(new SimpleValue(value));
  }

  /**
   * Retrieves the value of a sub-attribute.
   *
   * @param <T>          The type of the resolved instance representing the
   *                     value of sub-attribute.
   * @param name         The name of the sub-attribute.
   * @param resolver     The <code>AttributeValueResolver</code> that should
   *                     be used to resolve the value into an instance.
   * @return             The resolved instance representing the value of
   *                     sub-attribute.
   */
  public <T> T getSubAttributeValue(final String name,
                                    final AttributeValueResolver<T> resolver)
  {
    SCIMAttribute attribute = getAttribute(name);
    if(attribute != null)
    {
      SCIMAttributeValue v = attribute.getValue();
      if(v != null)
      {
        return resolver.toInstance(v);
      }
    }
    return null;
  }

  /**
   * Retrieves the value of a multi-valued sub-attribute value.
   *
   * @param <T>    The type of the resolved instance representing the value of
   *               sub-attribute.
   * @param name The name of the attribute value to retrieve.
   * @param resolver The <code>AttributeValueResolver</code> the should be used
   *                 to resolve the value to an instance.
   * @return The collection of resolved value instances or <code>null</code> if
   *         the specified attribute does not exist.
   */
  public <T> Collection<T> getSubAttributeValues(
      final String name, final AttributeValueResolver<T> resolver)
  {
    SCIMAttribute attribute = getAttribute(name);
    if(attribute != null)
    {
      SCIMAttributeValue[] values = attribute.getValues();
      if(values != null)
      {
        Collection<T> entries = new ArrayList<T>(values.length);
        for(SCIMAttributeValue v : values)
        {
          entries.add(resolver.toInstance(v));
        }
        return entries;
      }
    }
    return null;
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
      final String lowerCaseName = StaticUtils.toLowerCase(a.getName());
      if (map.containsKey(lowerCaseName))
      {
        throw new RuntimeException("Duplicate attribute " + a.getName() +
                                   " in complex attribute value");
      }
      map.put(lowerCaseName, a);
    }
    return new ComplexSCIMAttributeValue(Collections.unmodifiableMap(map));
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
      final String lowerCaseName = StaticUtils.toLowerCase(a.getName());
      if (map.containsKey(lowerCaseName))
      {
        throw new RuntimeException("Duplicate attribute " + a.getName() +
                                   " in complex attribute value");
      }
      map.put(lowerCaseName, a);
    }
    return new ComplexSCIMAttributeValue(Collections.unmodifiableMap(map));
  }



  /**
   * Determines whether this attribute value is simple or complex.
   *
   * @return  {@code true} if this attribute value is complex, or {@code false}
   *          otherwise.
   */
  public abstract boolean isComplex();



  /**
   * Retrieves the simple value, or {@code null} if the attribute value is
   * complex.
   *
   * @return  The simple value, or {@code null} if the attribute value is
   * complex.
   */
  abstract SimpleValue getValue();



  /**
   * Retrieves the simple String value, or {@code null} if the attribute
   * value is complex.
   *
   * @return  The simple String value, or {@code null} if the attribute
   *          value is complex.
   */
  public String getStringValue()
  {
    SimpleValue value = getValue();
    if(value == null)
    {
      return null;
    }
    return value.getStringValue();
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
    SimpleValue value = getValue();
    if(value == null)
    {
      return null;
    }
    return value.getBooleanValue();
  }



  /**
   * Retrieves the simple Decimal value, or {@code null} if the attribute
   * value is complex.
   *
   * @return  The simple Decimal value, or {@code null} if the attribute
   *          value is complex.
   */
  public Double getDecimalValue()
  {
    SimpleValue value = getValue();
    if(value == null)
    {
      return null;
    }
    return value.getDoubleValue();
  }



  /**
   * Retrieves the simple Long value, or {@code null} if the attribute
   * value is complex.
   *
   * @return  The simple Long value, or {@code null} if the attribute
   *          value is complex.
   */
  public Long getIntegerValue()
  {
    SimpleValue value = getValue();
    if(value == null)
    {
      return null;
    }
    return value.getLongValue();
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
    SimpleValue value = getValue();
    if(value == null)
    {
      return null;
    }
    return value.getDateValue();
  }



  /**
   * Retrieves the simple Binary value, or {@code null} if the attribute
   * value is complex.
   *
   * @return  The simple Binary value, or {@code null} if the attribute
   *          value is complex.
   */
  public byte[] getBinaryValue()
  {
    SimpleValue value = getValue();
    if(value == null)
    {
      return null;
    }
    return value.getBinaryValue();
  }



  /**
   * Retrieves the attributes comprising the complex value, keyed by the lower
   * case name of the attribute, or {@code null} if the attribute value is
   * simple.
   *
   * @return  The attributes comprising the complex value.
   */
  public abstract Map<String, SCIMAttribute> getAttributes();



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
    Map<String, SCIMAttribute> attributes = getAttributes();
    if (attributes != null)
    {
      return attributes.get(StaticUtils.toLowerCase(attributeName));
    }
    else
    {
      return null;
    }
  }



  /**
   * Indicates whether a complex value has an attribute with the provided name.
   *
   * @param attributeName  The attribute name for which to make the
   *                       determination.
   *
   * @return  {@code true} if there is an attribute with the provided name,
   *          {@code false} if there is no such attribute or this attribute
   *          value is simple.
   */
  public boolean hasAttribute(final String attributeName)
  {
    Map<String, SCIMAttribute> attributes = getAttributes();
    if (attributes != null)
    {
      return attributes.containsKey(StaticUtils.toLowerCase(attributeName));
    }
    else
    {
      return false;
    }
  }

}
