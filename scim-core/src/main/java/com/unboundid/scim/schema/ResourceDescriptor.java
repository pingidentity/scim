/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.schema;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;



/**
 * This class provides methods that describe the schema for a SCIM resource. It
 * may be used to help read and write SCIM objects in their external XML and
 * JSON representation, and to convert SCIM objects to and from LDAP entries.
 */
public class ResourceDescriptor
{

  private String schema;

  private String name;

  private String description;

  private String queryEndpoint;

  private LinkedHashMap<String,AttributeDescriptor> attributeDescriptors =
      new LinkedHashMap<String,AttributeDescriptor>();


  /**
   * Construct a new resource descriptor with the provided information.
   *
   * @param name The addressable Resource endpoint name.
   * @param description The Resource's human readable description.
   * @param schema The Resource's associated schema URN
   * @param queryEndpoint The Resource's HTTP addressable endpoint relative
   *                      to the Base URL
   */
  public ResourceDescriptor(final String name, final String description,
                            final String schema, final String queryEndpoint) {
    this.name = name;
    this.description = description;
    this.schema = schema;
    this.queryEndpoint = queryEndpoint;
  }

  /**
   * Retrieve the attribute descriptor for a specified attribute.
   *
   * @param schema The attribute descriptor's associated schema URN.
   * @param name The name of the attribute whose descriptor is to be retrieved.
   *
   * @return The attribute descriptor for the specified attribute, or {@code
   *         null} if there is no such attribute.
   */
  public AttributeDescriptor getAttribute(final String schema,
                                          final String name)
  {
    return attributeDescriptors.get(name.toLowerCase());
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
   * Retrieves the list of attribute descriptors for the resource.
   *
   * @return The list of attribute descriptors for the resource. It is never
   *         {@code null}.
   */
  public Collection<AttributeDescriptor> getAttributeDescriptors()
  {
    return Collections.unmodifiableCollection(attributeDescriptors.values());
  }



  /**
   * Adds the provided attribute descriptor to the list of attribute
   * descriptors for the resource.
   *
   * @param descriptor  The attribute descriptor to be added.
   */
  public void addAttributeDescriptor(final AttributeDescriptor descriptor)
  {
    attributeDescriptors.put(descriptor.getName().toLowerCase(), descriptor);
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
   * Retrieves the resource's human readable description.
   *
   * @return The resource's human readable description.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Retrieves the Resource's HTTP addressable endpoint relative to the
   * Base URL.
   *
   * @return The Resource's HTTP addressable endpoint relative to the Base URL.
   */
  public String getQueryEndpoint() {
    return queryEndpoint;
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
