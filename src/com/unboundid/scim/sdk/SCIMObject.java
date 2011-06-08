/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;



/**
 * This class represents a Simple Cloud Identity Management (SCIM) object.
 * A SCIM object may be composed of common schema attributes and a collection
 * of attributes from one or more additional schema definitions. The common
 * schema attributes for resource objects (which includes the 'id' and 'meta'
 * attributes) do not have a schema name because they are implicit.
 * This class is intentionally not thread-safe.
 */
public class SCIMObject
{
  /**
   * The resource identifier, or {@code null} if this object does not have an
   * identifier.
   */
  private String id;

  /**
   * The meta-data attribute, or {@code null} if this object does not have any
   * meta-data.
   */
  private SCIMAttribute meta;

  /**
   * The set of attributes in this object grouped by the name of the schema to
   * which they belong.
   * TODO: Do schema names have to be normalized?
   */
  private final HashMap<String,LinkedHashMap<String,SCIMAttribute>> attributes;



  /**
   * Create a SCIM object from the provided information. The object initially
   * has no attributes other than any provided common schema attributes.
   *
   * @param id  The resource identifier, or {@code null} if this object does
   *            not have an identifier.
   * @param meta  The meta-data attribute, or {@code null} if this object does
   *              not have any meta-data.
   */
  public SCIMObject(final String id, final SCIMAttribute meta)
  {
    this.id   = id;
    this.meta = meta;
    this.attributes =
        new HashMap<String, LinkedHashMap<String, SCIMAttribute>>();
  }



  /**
   * Retrieves the resource identifier for this object.
   *
   * @return  The resource identifier, or {@code null} if this object does not
   *          have an identifier.
   */
  public String getId()
  {
    return id;
  }



  /**
   * Specifies the resource identifier for this object.
   *
   * @param id  The resource identifier for this object, or {@code null} if
   *            this object does not have an identifier.
   */
  public void setId(final String id)
  {
    this.id = id;
  }



  /**
   * Retrieves the meta-data attribute for this object.
   *
   * @return  The meta-data attribute for this object, or {@code null} if this
   *          object does not have any meta-data.
   */
  public SCIMAttribute getMeta()
  {
    return meta;
  }



  /**
   * Specifies the meta-data attribute for this object.
   *
   * @param meta  The meta-data attribute for this object, or {@code null} if
   *              this object does not have any meta-data.
   */
  public void setMeta(final SCIMAttribute meta)
  {
    this.meta = meta;
  }



  /**
   * Retrieves the set of schemas currently contributing attributes to this
   * object.
   *
   * @return  An immutable collection of the names of schemas currently
   *          contributing attributes to this object.
   */
  public Collection<String> getSchemas()
  {
    return Collections.unmodifiableCollection(attributes.keySet());
  }



  /**
   * Determines whether this object contains any attributes in the specified
   * schema.
   *
   * @param schema  The name of the schema for which to make the determination.
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
   * @param schema  The name of the schema containing the attribute to retrieve.
   *                It must not be {@code null}.
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
      return attrs.get(name);
    }
  }



  /**
   * Retrieves the set of attributes in this object from the specified schema.
   *
   * @param schema  The name of the schema whose attributes are to be retrieved.
   * @return  An immutable collection of the attributes in this object from the
   *          specified schema, or the empty collection if there are no such
   *          attributes.
   */
  public Collection<SCIMAttribute> getAttributes(final String schema)
  {
    final LinkedHashMap<String,SCIMAttribute> attrs = attributes.get(schema);
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
   * @param schema  The name of the schema containing the attribute.
   *                It must not be {@code null}.
   * @param name    The name of the attribute for which to make the
   *                determination. It must not be {@code null}.
   *
   * @return  {@code true} if this object contains the specified attribute, or
   *          {@code false} if not.
   */
  public boolean hasAttribute(final String schema, final String name)
  {
    final LinkedHashMap<String,SCIMAttribute> attrs = attributes.get(schema);
    if (attrs == null)
    {
      return false;
    }
    else
    {
      return attrs.containsKey(name);
    }
  }



  /**
   * Adds the provided attribute to this object. If this object already contains
   * an attribute with the same name, then the provided attribute will not be
   * added.
   *
   * @param schema     The name of the schema to which the attribute belongs.
   *                   It must not be {@code null}.
   * @param attribute  The attribute to be added. It must not be {@code null}.
   *
   * @return  {@code true} if the object was updated, or {@code false} if the
   *          object already contained an attribute with the same name.
   */
  public boolean addAttribute(final String schema,
                              final SCIMAttribute attribute)
  {
    LinkedHashMap<String,SCIMAttribute> attrs = attributes.get(schema);
    if (attrs == null)
    {
      attrs = new LinkedHashMap<String, SCIMAttribute>();
      attrs.put(attribute.getName(), attribute);
      attributes.put(schema, attrs);
      return true;
    }
    else
    {
      if (attrs.containsKey(attribute.getName()))
      {
        return false;
      }
      else
      {
        attrs.put(attribute.getName(), attribute);
        return true;
      }
    }
  }



  /**
   * Adds the provided attribute to this object, replacing any existing
   * attribute with the same name.
   *
   * @param schema     The name of the schema to which the attribute belongs.
   *                   It must not be {@code null}.
   * @param attribute  The attribute to be added. It must not be {@code null}.
   */
  public void setAttribute(final String schema,
                           final SCIMAttribute attribute)
  {
    LinkedHashMap<String,SCIMAttribute> attrs = attributes.get(schema);
    if (attrs == null)
    {
      attrs = new LinkedHashMap<String, SCIMAttribute>();
      attrs.put(attribute.getName(), attribute);
      attributes.put(schema, attrs);
    }
    else
    {
      attrs.put(attribute.getName(), attribute);
    }
  }



  /**
   * Removes the specified attribute from this object.
   *
   * @param schema  The name of the schema to which the attribute belongs.
   *                It must not be {@code null}.
   * @param  name   The name of the attribute to remove. It must not be
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
      final boolean removed = attrs.remove(name) != null;
      if (removed && attrs.isEmpty())
      {
        attributes.remove(schema);
      }
      return removed;
    }
  }
}
