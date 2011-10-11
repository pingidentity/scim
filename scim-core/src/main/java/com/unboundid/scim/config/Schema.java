/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.config;

import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;



/**
 * This class represents a SCIM schema after it has been parsed
 * from configuration.
 */
public class Schema
{
  /**
   * The URI of the schema.
   */
  private String schemaURI;

  /**
   * The set of descriptors for resources defined in this schema.
   */
  private List<ResourceDescriptor> resourceDescriptors;

  /**
   * The set of descriptors for attributes defined in this schema, indexed
   * by the lower case name of the attribute.
   */
  private LinkedHashMap<String,AttributeDescriptor> attributeDescriptorMap;



  /**
   * Create a new instance of a SCIM schema.
   *
   * @param schemaURI             The URI of the schema.
   * @param resourceDescriptors   The set of descriptors for resources defined
   *                              in this schema.
   * @param attributeDescriptors  The set of descriptors for attributes defined
   *                              in this schema.
   */
  Schema(final String schemaURI,
         final List<ResourceDescriptor> resourceDescriptors,
         final List<AttributeDescriptor> attributeDescriptors)
  {
    this.schemaURI = schemaURI;
    this.resourceDescriptors  = resourceDescriptors;

    attributeDescriptorMap = new LinkedHashMap<String, AttributeDescriptor>();
    for (final AttributeDescriptor attributeDescriptor : attributeDescriptors)
    {
      attributeDescriptorMap.put(attributeDescriptor.getName().toLowerCase(),
                                 attributeDescriptor);
    }
  }



  /**
   * Retrieve the URI of the schema.
   *
   * @return  The URI of the schema.
   */
  public String getSchemaURI()
  {
    return schemaURI;
  }



  /**
   * Retrieve the set of descriptors for resources defined in this schema.
   *
   * @return  The set of descriptors for resources defined in this schema.
   */
  public List<ResourceDescriptor> getResourceDescriptors()
  {
    return resourceDescriptors;
  }



  /**
   * Retrieve the set of descriptors for attributes defined in this schema.
   *
   * @return  The set of descriptors for attributes defined in this schema.
   */
  public Collection<AttributeDescriptor> getAttributeDescriptors()
  {
    return attributeDescriptorMap.values();
  }



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
    return attributeDescriptorMap.get(name.toLowerCase());
  }



}
