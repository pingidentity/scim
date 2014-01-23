/*
 * Copyright 2014 UnboundID Corp.
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
 * SCIMAttributeValue implementation for the a value of a multi-valued
 * attribute. This is a hybrid simple and complex attribute value where
 * getting the simple value will return the the normative value sub-attribute
 * if available.
 */
class MultiValuedSCIMAttributeValue extends ComplexSCIMAttributeValue
{
  /**
   * Create a new instance of a SCIM multi-valued complex attribute value.
   *
   * @param attributes  The attributes comprising the complex value, keyed by
   *                    the name of the attribute.
   */
  MultiValuedSCIMAttributeValue(final Map<String, SCIMAttribute> attributes)
  {
    super(attributes);
  }

  @Override
  public SimpleValue getValue()
  {
    SCIMAttribute valueSubAttribute = getAttribute("value");
    if(valueSubAttribute != null)
    {
      return valueSubAttribute.getValue().getValue();
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
    sb.append("MultiValuedSCIMAttributeValue{");
    sb.append("attributes=").append(getAttributes());
    sb.append('}');
    return sb.toString();
  }
}
