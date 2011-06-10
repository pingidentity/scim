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
   * The type of SCIM resource represented by this object, or {@code null}
   * if the resource type is unknown.
   */
  private String resourceType;

  /**
   * The set of attributes from the core schema.
   */
  private final LinkedHashMap<String,SCIMAttribute> coreAttributes;

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
    this(null);
  }



  /**
   * Create an empty SCIM object with the specified resource type.
   *
   * @param resourceType  The type of SCIM resource represented by this object,
   *                      or {@code null} if the resource type is unknown.
   */
  public SCIMObject(final String resourceType)
  {
    this.resourceType = resourceType;
    this.coreAttributes = new LinkedHashMap<String, SCIMAttribute>();
    this.attributes =
        new HashMap<String, LinkedHashMap<String, SCIMAttribute>>();
  }



  /**
   * Retrieves the type of SCIM resource represented by this object, or
   * {@code null} if the resource type is unknown.
   *
   * @return  The type of SCIM resource represented by this object, or
   *          {@code null} if the resource type is unknown.
   */
  public String getResourceType()
  {
    return resourceType;
  }



  /**
   * Specifies the type of SCIM resource represented by this object, or
   * {@code null} if the resource type is unknown.
   *
   * @param resourceType  The type of SCIM resource represented by this object,
   *                      or {@code null} if the resource type is unknown.
   */
  public void setResourceType(final String resourceType)
  {
    this.resourceType = resourceType;
  }



  /**
   * Retrieves the set of schemas currently contributing attributes to this
   * object.
   *
   * @return  An immutable collection of the URIs of schemas currently
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
   * @param schema  The URI of the schema containing the attribute to retrieve,
   *                or {@code null} to indicate core schema attributes.
   *
   * @param name    The name of the attribute to retrieve. It must not be
   *                {@code null}.
   *
   * @return  The requested attribute from this object, or {@code null} if the
   *          specified attribute is not present in this object.
   */
  public SCIMAttribute getAttribute(final String schema, final String name)
  {
    final LinkedHashMap<String,SCIMAttribute> attrs;
    if (schema == null)
    {
      attrs = coreAttributes;
    }
    else
    {
      attrs = attributes.get(schema);
    }

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
   * @param schema  The URI of the schema whose attributes are to be retrieved,
   *                or {@code null} to indicate core schema attributes.
   *
   * @return  An immutable collection of the attributes in this object from the
   *          specified schema, or the empty collection if there are no such
   *          attributes.
   */
  public Collection<SCIMAttribute> getAttributes(final String schema)
  {
    final LinkedHashMap<String,SCIMAttribute> attrs;
    if (schema == null)
    {
      attrs = coreAttributes;
    }
    else
    {
      attrs = attributes.get(schema);
    }

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
   * @param schema  The URI of the schema containing the attribute,
   *                or {@code null} to indicate a core schema attribute.
   * @param name    The name of the attribute for which to make the
   *                determination. It must not be {@code null}.
   *
   * @return  {@code true} if this object contains the specified attribute, or
   *          {@code false} if not.
   */
  public boolean hasAttribute(final String schema, final String name)
  {
    final LinkedHashMap<String,SCIMAttribute> attrs;
    if (schema == null)
    {
      attrs = coreAttributes;
    }
    else
    {
      attrs = attributes.get(schema);
    }

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
    if (schema == null)
    {
      if (coreAttributes.containsKey(attribute.getName()))
      {
        return false;
      }
      else
      {
        coreAttributes.put(attribute.getName(), attribute);
        return true;
      }
    }

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
   * @param attribute  The attribute to be added. It must not be {@code null}.
   */
  public void setAttribute(final SCIMAttribute attribute)
  {
    final String schema = attribute.getSchema();
    if (schema == null)
    {
      coreAttributes.put(attribute.getName(), attribute);
      return;
    }

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
   * @param schema  The URI of the schema to which the attribute belongs,
   *                or {@code null} to indicate a core schema attribute.
   * @param name    The name of the attribute to remove. It must not be
   *                {@code null}.
   *
   * @return  {@code true} if the attribute was removed from the object, or
   *          {@code false} if it was not present.
   */
  public boolean removeAttribute(final String schema, final String name)
  {
    if (schema == null)
    {
      return coreAttributes.remove(name) != null;
    }

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