/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.config;

import java.util.LinkedList;
import java.util.List;



/**
 * This class provides methods that describe the schema for a SCIM resource. It
 * may be used to help read and write SCIM objects in their external XML and
 * JSON representation, and to convert SCIM objects to and from LDAP entries.
 */
public class ResourceDescriptor
{

  private String schema;

  private String name;

  private List<AttributeDescriptor> attributeDescriptors =
      new LinkedList<AttributeDescriptor>();



  /**
   * Retrieve the attribute descriptor for a specified attribute.
   *
   * @param name The name of the attribute whose descriptor is to be retrieved.
   *
   * @return The attribute descriptor for the specified attribute, or {@code
   *         null} if there is no such attribute.
   */
  public AttributeDescriptor getAttribute(final String name)
  {
    // TODO use a map
    for (AttributeDescriptor attributeDescriptor : attributeDescriptors)
    {
      if (attributeDescriptor.getName().equals(name))
      {
        return attributeDescriptor;
      }
    }
    return null;
  }



  /**
   * Retrieve the name of the resource to be used in any external representation
   * of the resource.
   *
   * @return Retrieve the name of the resource to be used in any external
   *         representation of the resource. It is never {@code null}.
   */
  public String getName()
  {
    return name;
  }



  /**
   * Specifies the name of the resource to be used in any external
   * representation of the resource.
   *
   * @param name Specifies the name of the resource to be used in any external
   *             representation of the resource. It must not be {@code null}.
   */
  public void setName(final String name)
  {
    this.name = name;
  }



  /**
   * Retrieves the list of attribute descriptors for the resource.
   *
   * @return The list of attribute descriptors for the resource. It is never
   *         {@code null}.
   */
  public List<AttributeDescriptor> getAttributeDescriptors()
  {
    return attributeDescriptors;
  }



  /**
   * Specifies the list of attribute descriptors for the resource.
   *
   * @param attributeDescriptors The list of attribute descriptors for the
   *                             resource. It must not be {@code null}.
   */
  public void setAttributeDescriptors(
      final List<AttributeDescriptor> attributeDescriptors)
  {
    this.attributeDescriptors = attributeDescriptors;
  }



  /**
   * Returns the resource's XML schema (namespace) name.
   *
   * @return The XML namespace name.
   */
  public String getSchema()
  {
    return schema;
  }



  /**
   * Sets the resource's XML schema (namespace) name.
   *
   * @param schema The XML namespace name.
   */
  public void setSchema(final String schema)
  {
    this.schema = schema;
  }



  @Override
  public String toString()
  {
    return "ResourceDescriptor{" +
           "schema='" + schema + '\'' +
           ", name='" + name + '\'' +
           ", attributeDescriptors=" + attributeDescriptors +
           '}';
  }
}
