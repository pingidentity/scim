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

package com.unboundid.scim.sdk;

import com.unboundid.scim.data.AttributeValueResolver;

import java.util.ArrayList;
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
 * <li>Simple values can be String, Boolean, DateTime, Integer or Binary.</li>
 * <li>Complex values are composed of a set of subordinate SCIM attributes.</li>
 * </ul>
 */
public final class SCIMAttributeValue
{
  /**
   * The simple attribute value, or {@code null} if the attribute value is
   * complex.
   */
  private final SimpleValue value;

  /**
   * The attributes comprising the complex value, keyed by the lower case
   * name of the attribute, or {@code null} if the attribute value is simple.
   */
  private final Map<String,SCIMAttribute> attributes;



  /**
   * Create a new instance of a SCIM attribute value.
   *
   * @param value  The simple value.
   */
  public SCIMAttributeValue(final SimpleValue value)
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
    return new SCIMAttributeValue(new SimpleValue(value));
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
    return new SCIMAttributeValue(new SimpleValue(value));
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
    return new SCIMAttributeValue(new SimpleValue(value));
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
    return new SCIMAttributeValue(new SimpleValue(value));
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
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("SCIMAttributeValue{");
    if (value != null)
    {
      sb.append("value=").append(value);
    }
    else
    {
      sb.append("attributes=").append(attributes);
    }
    sb.append('}');
    return sb.toString();
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
      final String lowerCaseName = StaticUtils.toLowerCase(a.getName());
      if (map.containsKey(lowerCaseName))
      {
        throw new RuntimeException("Duplicate attribute " + a.getName() +
                                   " in complex attribute value");
      }
      map.put(lowerCaseName, a);
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
   * Retrieves the simple value, or {@code null} if the attribute value is
   * complex.
   *
   * @return  The simple value, or {@code null} if the attribute value is
   * complex.
   */
  public SimpleValue getValue()
  {
    return value;
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
    return value.getBinaryValue();
  }



  /**
   * Retrieves the attributes comprising the complex value, keyed by the lower
   * case name of the attribute, or {@code null} if the attribute value is
   * simple.
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
    if (attributes != null)
    {
      return attributes.containsKey(StaticUtils.toLowerCase(attributeName));
    }
    else
    {
      return false;
    }
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SCIMAttributeValue that = (SCIMAttributeValue) o;

    if (attributes != null ? !attributes.equals(that.attributes) :
        that.attributes != null) {
      return false;
    }
    if (value != null ? !value.equals(that.value) : that.value != null) {
      return false;
    }

    return true;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
    return result;
  }
}
