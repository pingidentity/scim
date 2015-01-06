/*
 * Copyright 2011-2015 UnboundID Corp.
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

package com.unboundid.scim.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * This class maps the sub-attributes of a SCIM value for a specified
 * type of canonical value (e.g. "work" or "home").
 */
public class CanonicalValueMapper
{
  /**
   * The value of the "type" sub-attribute for SCIM attribute values that are
   * mapped by this canonical value mapper.
   */
  private final String typeValue;

  /**
   * The set of sub-attribute transformations indexed by the name of the
   * sub-attribute.
   */
  private final Map<String, SubAttributeTransformation> map;



  /**
   * Create a new instance of this canonical value mapper.
   *
   * @param typeValue          The value of the "type" sub-attribute for SCIM
   *                           attribute values that are mapped by this
   *                           canonical value mapper.
   * @param transformations    The set of sub-attribute transformations.
   */
  public CanonicalValueMapper(
      final String typeValue,
      final List<SubAttributeTransformation> transformations)
  {
    this.typeValue    = typeValue;
    map = new HashMap<String, SubAttributeTransformation>();
    for (final SubAttributeTransformation t : transformations)
    {
      map.put(t.getSubAttribute(), t);
    }
  }



  /**
   * Create a canonical value mapper from the JAXB type representation of the
   * canonical value.
   *
   * @param CanonicalValue  The JAXB type defining the mapping.
   *
   * @return  A new canonical value mapper, or {@code null} if there are no
   *          mappings defined for the canonical value.
   */
  public static CanonicalValueMapper create(final CanonicalValue CanonicalValue)
  {
    if (CanonicalValue.getSubMapping().isEmpty())
    {
      return null;
    }

    final String typeValue = CanonicalValue.getName();
    final List<SubAttributeTransformation> transformations =
        new ArrayList<SubAttributeTransformation>();
    for (final SubAttributeMapping m : CanonicalValue.getSubMapping())
    {
      transformations.add(SubAttributeTransformation.create(m));
    }

    return new CanonicalValueMapper(typeValue, transformations);
  }



  /**
   * Create a canonical value mapper from the JAXB type representation of the
   * default mapping.
   *
   * @param mapping  The JAXB type defining the default mapping.
   *
   * @return  A new canonical value mapper.
   */
  public static CanonicalValueMapper create(final AttributeMapping mapping)
  {
    final String typeValue = null;
    final List<SubAttributeTransformation> transformations =
        new ArrayList<SubAttributeTransformation>();
    final AttributeTransformation at = AttributeTransformation.create(mapping);
    transformations.add(new SubAttributeTransformation("value", at));

    return new CanonicalValueMapper(typeValue, transformations);
  }



  /**
   * Retrieve the value of the "type" sub-attribute for SCIM attribute values
   * that are mapped by this canonical value mapper.
   *
   * @return  The value of the "type" sub-attribute for SCIM attribute values
   *          that are mapped by this canonical value mapper.
   */
  public String getTypeValue()
  {
    return typeValue;
  }



  /**
   * Retrieve the set of sub-attribute transformations.
   *
   * @return  The set of sub-attribute transformations.
   */
  public Collection<SubAttributeTransformation> getTransformations()
  {
    return map.values();
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("CanonicalValueMapper");
    sb.append("{typeValue='").append(typeValue).append('\'');
    sb.append(", map=").append(map);
    sb.append('}');
    return sb.toString();
  }
}
