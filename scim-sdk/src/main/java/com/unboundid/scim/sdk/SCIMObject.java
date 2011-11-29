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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * This class represents a Simple Cloud Identity Management (SCIM) object.
 * A SCIM object may be composed of common schema attributes and a collection
 * of attributes from one or more additional schema definitions.
 * This class is not designed to be thread-safe.
 */
public class SCIMObject
{

  /**
   * The set of attributes in this object grouped by the URI of the schema to
   * which they belong.
   * TODO: Do schema URIs have to be normalized?
   */
  private final HashMap<String,LinkedHashMap<String,SCIMAttribute>> attributes;



  /**
   * Create an empty SCIM object that initially has no attributes. The type of
   * resource is not specified.
   */
  public SCIMObject()
  {
    this.attributes =
        new HashMap<String, LinkedHashMap<String, SCIMAttribute>>();
  }


  /**
   * Create a new copy of the provided SCIM object.
   *
   * @param scimObject The SCIMObject to copy.
   */
  public SCIMObject(final SCIMObject scimObject)
  {
    // Since SCIMAttribute is immutable, just copy the maps.
    this.attributes =
        new HashMap<String, LinkedHashMap<String, SCIMAttribute>>();
    for(Map.Entry<String, LinkedHashMap<String, SCIMAttribute>> entry :
        scimObject.attributes.entrySet())
    {
      this.attributes.put(entry.getKey(),
          new LinkedHashMap<String, SCIMAttribute>(entry.getValue()));
    }
  }



  /**
   * Retrieves the set of schemas currently contributing attributes to this
   * object.
   *
   * @return  An immutable collection of the URIs of schemas currently
   *          contributing attributes to this object.
   */
  public Set<String> getSchemas()
  {
    return Collections.unmodifiableSet(attributes.keySet());
  }



  /**
   * Determines whether this object contains any attributes in the specified
   * schema.
   *
   * @param schema  The URI of the schema for which to make the determination.
   *                It must not be {@code null}.
   *
   * @return  {@code true} if this object contains any attributes in the
   *          specified schema, or {@code false} if not.
   */
  public boolean hasSchema(final String schema)
  {
    return attributes.containsKey(schema);
  }



  /**
   * Retrieves the attribute with the specified name.
   *
   * @param schema  The URI of the schema containing the attribute to retrieve.
   *
   * @param name    The name of the attribute to retrieve. It must not be
   *                {@code null}.
   *
   * @return  The requested attribute from this object, or {@code null} if the
   *          specified attribute is not present in this object.
   */
  public SCIMAttribute getAttribute(final String schema, final String name)
  {
    final LinkedHashMap<String,SCIMAttribute> attrs = attributes.get(schema);

    if (attrs == null)
    {
      return null;
    }
    else
    {
      return attrs.get(StaticUtils.toLowerCase(name));
    }
  }



  /**
   * Retrieves the set of attributes in this object from the specified schema.
   *
   * @param schema  The URI of the schema whose attributes are to be retrieved.
   *
   * @return  An immutable collection of the attributes in this object from the
   *          specified schema, or the empty collection if there are no such
   *          attributes.
   */
  public Collection<SCIMAttribute> getAttributes(final String schema)
  {
    final LinkedHashMap<String, SCIMAttribute> attrs = attributes.get(schema);

    if (attrs == null)
    {
      return Collections.emptyList();
    }
    else
    {
      return Collections.unmodifiableCollection(attrs.values());
    }
  }



  /**
   * Determines whether this object contains the specified attribute.
   *
   * @param schema  The URI of the schema containing the attribute.
   * @param name    The name of the attribute for which to make the
   *                determination. It must not be {@code null}.
   *
   * @return  {@code true} if this object contains the specified attribute, or
   *          {@code false} if not.
   */
  public boolean hasAttribute(final String schema, final String name)
  {
    final LinkedHashMap<String, SCIMAttribute> attrs = attributes.get(schema);

    if (attrs == null)
    {
      return false;
    }
    else
    {
      return attrs.containsKey(StaticUtils.toLowerCase(name));
    }
  }



  /**
   * Adds the provided attribute to this object. If this object already contains
   * an attribute with the same name from the same schema, then the provided
   * attribute will not be added.
   *
   * @param attribute  The attribute to be added. It must not be {@code null}.
   *
   * @return  {@code true} if the object was updated, or {@code false} if the
   *          object already contained an attribute with the same name.
   */
  public boolean addAttribute(final SCIMAttribute attribute)
  {
    final String schema = attribute.getSchema();
    final String lowerCaseName = StaticUtils.toLowerCase(attribute.getName());

    LinkedHashMap<String,SCIMAttribute> attrs = attributes.get(schema);
    if (attrs == null)
    {
      attrs = new LinkedHashMap<String, SCIMAttribute>();
      attrs.put(lowerCaseName, attribute);
      attributes.put(schema, attrs);
      return true;
    }
    else
    {
      if (attrs.containsKey(lowerCaseName))
      {
        return false;
      }
      else
      {
        attrs.put(lowerCaseName, attribute);
        return true;
      }
    }
  }



  /**
   * Adds the provided attribute to this object, replacing any existing
   * attribute with the same name.
   *
   * @param attribute  The attribute to be added. It must not be {@code null}.
   */
  public void setAttribute(final SCIMAttribute attribute)
  {
    final String schema = attribute.getSchema();
    final String lowerCaseName = StaticUtils.toLowerCase(attribute.getName());

    LinkedHashMap<String,SCIMAttribute> attrs = attributes.get(schema);
    if (attrs == null)
    {
      attrs = new LinkedHashMap<String, SCIMAttribute>();
      attrs.put(lowerCaseName, attribute);
      attributes.put(schema, attrs);
    }
    else
    {
      attrs.put(lowerCaseName, attribute);
    }
  }



  /**
   * Removes the specified attribute from this object.
   *
   * @param schema  The URI of the schema to which the attribute belongs.
   * @param name    The name of the attribute to remove. It must not be
   *                {@code null}.
   *
   * @return  {@code true} if the attribute was removed from the object, or
   *          {@code false} if it was not present.
   */
  public boolean removeAttribute(final String schema, final String name)
  {
    LinkedHashMap<String,SCIMAttribute> attrs = attributes.get(schema);
    if (attrs == null)
    {
      return false;
    }
    else
    {
      final boolean removed =
          attrs.remove(StaticUtils.toLowerCase(name)) != null;
      if (removed && attrs.isEmpty())
      {
        attributes.remove(schema);
      }
      return removed;
    }
  }



  /**
   * Determine whether this object matches the provided filter parameters.
   *
   * @param filter  The filter parameters to compare against the object.
   *
   * @return  {@code true} if this object matches the provided filter, and
   *          {@code false} otherwise.
   */
  public boolean matchesFilter(final SCIMFilter filter)
  {
    final SCIMFilterType type = filter.getFilterType();
    final List<SCIMFilter> components = filter.getFilterComponents();

    switch(type)
    {
      case AND:
        for(SCIMFilter component : components)
        {
          if(!matchesFilter(component))
          {
            return false;
          }
        }
        return true;
      case OR:
        for(SCIMFilter component : components)
        {
          if(matchesFilter(component))
          {
            return true;
          }
        }
        return false;
    }

    final String schema = filter.getFilterAttribute().getAttributeSchema();
    final String attributeName = filter.getFilterAttribute().getAttributeName();

    final SCIMAttribute attribute = getAttribute(schema, attributeName);
    if (attribute == null)
    {
      return false;
    }

    return attribute.matchesFilter(filter);
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

    SCIMObject that = (SCIMObject) o;

    if (!attributes.equals(that.attributes)) {
      return false;
    }

    return true;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return attributes.hashCode();
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "SCIMObject{" +
      "attributes=" + attributes +
      '}';
  }
}
