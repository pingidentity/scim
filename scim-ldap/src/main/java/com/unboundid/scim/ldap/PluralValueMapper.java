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

package com.unboundid.scim.ldap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * This class maps the sub-attributes of a SCIM plural value for a specified
 * type of value (e.g. "work" or "home").
 */
public class PluralValueMapper
{
  /**
   * The value of the "type" sub-attribute for SCIM attribute values that are
   * mapped by this plural value mapper.
   */
  private final String typeValue;

  /**
   * The set of sub-attribute transformations indexed by the name of the
   * sub-attribute.
   */
  private final Map<String, SubAttributeTransformation> map;



  /**
   * Create a new instance of this plural value mapper.
   *
   * @param typeValue          The value of the "type" sub-attribute for SCIM
   *                           attribute values that are mapped by this
   *                           plural value mapper.
   * @param transformations    The set of sub-attribute transformations.
   */
  public PluralValueMapper(
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
   * Create a plural value mapper from the JAXB type representation of the
   * plural type.
   *
   * @param pluralType  The JAXB type defining the mapping.
   *
   * @return  A new plural value mapper, or {@code null} if there are no
   *          mappings defined for the plural type.
   */
  public static PluralValueMapper create(final PluralType pluralType)
  {
    if (pluralType.getSubMapping().isEmpty())
    {
      return null;
    }

    final String typeValue = pluralType.getName();
    final List<SubAttributeTransformation> transformations =
        new ArrayList<SubAttributeTransformation>();
    for (final SubAttributeMapping m : pluralType.getSubMapping())
    {
      transformations.add(SubAttributeTransformation.create(m));
    }

    return new PluralValueMapper(typeValue, transformations);
  }



  /**
   * Create a plural value mapper from the JAXB type representation of the
   * default mapping.
   *
   * @param mapping  The JAXB type defining the default mapping.
   *
   * @return  A new plural value mapper.
   */
  public static PluralValueMapper create(final AttributeMapping mapping)
  {
    final String typeValue = null;
    final List<SubAttributeTransformation> transformations =
        new ArrayList<SubAttributeTransformation>();
    final AttributeTransformation at = AttributeTransformation.create(mapping);
    transformations.add(new SubAttributeTransformation("value", at));

    return new PluralValueMapper(typeValue, transformations);
  }



  /**
   * Retrieve the value of the "type" sub-attribute for SCIM attribute values
   * that are mapped by this plural value mapper.
   *
   * @return  The value of the "type" sub-attribute for SCIM attribute values
   *          that are mapped by this plural value mapper.
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
    sb.append("PluralValueMapper");
    sb.append("{typeValue='").append(typeValue).append('\'');
    sb.append(", map=").append(map);
    sb.append('}');
    return sb.toString();
  }
}
