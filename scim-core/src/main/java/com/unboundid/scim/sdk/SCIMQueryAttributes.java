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

package com.unboundid.scim.sdk;

import com.unboundid.scim.schema.AttributeDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



/**
 * This class represents a list of query attributes taken from the attributes
 * query parameter. e.g. attributes=name.formatted,userName
 */
public class SCIMQueryAttributes
{
  /**
   * The set of attributes or sub-attributes explicitly requested, or empty if
   * all attributes and sub-attributes are requested.
   */
  private final List<AttributePath> types;


  /**
   * Create a new instance of query attributes from their string representation.
   *
   * @param attributes     The set of attributes or sub-attributes requested,
   *                       or empty if all attributes and sub-attributes are
   *                       requested. The attributes must be qualified by their
   *                       schema URI if they are not in the core schema.
   */
  public SCIMQueryAttributes(final String ... attributes)
  {
    types = new ArrayList<AttributePath>();
    if (attributes.length > 0)
    {
      for (final String a : attributes)
      {
        types.add(AttributePath.parse(a));
      }
    }
  }



  /**
   * Retrieve the list of query attributes.
   *
   * @return  The list of query attributes, or an empty list if all attributes
   *          are requested.
   */
  public List<AttributePath> getAttributeTypes()
  {
    return Collections.unmodifiableList(types);
  }



  /**
   * Determine whether all attributes and sub-attributes are requested by
   * these query attributes.
   *
   * @return  {@code true} if all attributes and sub-attributes are requested,
   *          and {@code false} otherwise.
   */
  public boolean allAttributesRequested()
  {
    return types.isEmpty();
  }



  /**
   * Determine whether the specified attribute is requested by these query
   * attributes.
   *
   * @param attributeDescriptor  The attribute for which to make the
   *                             determination.
   *
   * @return  {@code true} if the specified attribute is requested, or false
   *          otherwise.
   */
  public boolean isAttributeRequested(
      final AttributeDescriptor attributeDescriptor)
  {
    if (allAttributesRequested())
    {
      return true;
    }

    for (final AttributePath p : types)
    {
      if (p.getAttributeSchema().equals(attributeDescriptor.getSchema()) &&
          p.getAttributeName().equalsIgnoreCase(attributeDescriptor.getName()))
      {
        return true;
      }
    }
    return false;
  }



}
