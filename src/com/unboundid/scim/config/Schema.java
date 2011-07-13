/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.config;

import java.util.ArrayList;
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
   * The set of descriptors for attributes defined in this schema.
   */
  private List<AttributeDescriptor> attributeDescriptors;



  /**
   * Create a new instance of a SCIM schema.
   *
   * @param schemaURI  The URI of the schema.
   */
  Schema(final String schemaURI)
  {
    this.schemaURI = schemaURI;
    this.resourceDescriptors  = new ArrayList<ResourceDescriptor>();
    this.attributeDescriptors = new ArrayList<AttributeDescriptor>();
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
  public List<AttributeDescriptor> getAttributeDescriptors()
  {
    return attributeDescriptors;
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



}
