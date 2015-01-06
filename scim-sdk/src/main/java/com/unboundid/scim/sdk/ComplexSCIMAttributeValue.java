/*
 * Copyright 2014-2015 UnboundID Corp.
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

import java.util.Map;

/**
 * SCIMAttributeValue implementation for a complex attribute value.
 */
class ComplexSCIMAttributeValue extends SCIMAttributeValue
{
  /**
   * The attributes comprising the complex value, keyed by the lower case
   * name of the attribute, or {@code null} if the attribute value is simple.
   */
  private final Map<String,SCIMAttribute> attributes;


  /**
   * Create a new instance of a SCIM complex attribute value.
   *
   * @param attributes  The attributes comprising the complex value, keyed by
   *                    the name of the attribute.
   */
  ComplexSCIMAttributeValue(final Map<String, SCIMAttribute> attributes)
  {
    this.attributes = attributes;
  }


  /**
   * Determines whether this attribute value is simple or complex.
   *
   * @return  {@code true} if this attribute value is complex, or
   *          {@code false} otherwise.
   */
  public boolean isComplex()
  {
    return true;
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
   * Retrieves the simple value, or {@code null} if the attribute value is
   * complex.
   *
   * @return  The simple value, or {@code null} if the attribute value is
   * complex.
   */
  public SimpleValue getValue()
  {
    return null;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("ComplexSCIMAttributeValue{");
    sb.append("attributes=").append(attributes);
    sb.append('}');
    return sb.toString();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof
        com.unboundid.scim.sdk.ComplexSCIMAttributeValue)) {
      return false;
    }

    com.unboundid.scim.sdk.ComplexSCIMAttributeValue that =
        (com.unboundid.scim.sdk.ComplexSCIMAttributeValue) o;

    if (attributes != null ? !attributes.equals(that.attributes) :
        that.attributes != null) {
      return false;
    }

    return true;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return attributes != null ? attributes.hashCode() : 0;
  }
}
