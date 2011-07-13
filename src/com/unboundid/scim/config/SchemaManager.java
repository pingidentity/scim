/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.config;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;



/**
 * Manages the set of well known, configured SCIM schemas.
 */
public final class SchemaManager
{
  private static SchemaManager instance = new
      SchemaManager();

  private Map<String, Schema> schemas = new HashMap<String, Schema>();

  private Map<String, ResourceDescriptor> descriptors =
      new HashMap<String, ResourceDescriptor>();



  /**
   * Constructs the Schema Manager singleton.
   */
  private SchemaManager()
  {
  }



  /**
   * Initializes the manager with the specified SCIM Resource Descriptors.
   *
   * @param schemas The schema files containing SCIM Resources.
   *
   * @throws Exception Throw if error encountered processing specified files.
   */
  public static void init(final File[] schemas) throws Exception
  {
    if (schemas != null)
    {
      XmlSchemaParser parser = new XmlSchemaParser(schemas);
      for (final Schema schema : parser.getSchemas())
      {
        instance.schemas.put(schema.getSchemaURI(), schema);

        for (ResourceDescriptor r : schema.getResourceDescriptors())
        {
          instance.descriptors.put(r.getName(), r);
        }
      }
    }
  }



  /**
   * Retrieve the specified schema.
   *
   * @param schemaURI  The URI of the desired schema.
   *
   * @return  The specified schema, or {@code null} if the URI is not known.
   */
  public Schema getSchema(final String schemaURI)
  {
    return schemas.get(schemaURI);
  }



  /**
   * Returns the specified Resource Descriptor.
   *
   * @param name The SCIM resource name; e.g., User or Group.
   *
   * @return The SCIM resource descriptor, or {@code null} if not found.
   */
  public ResourceDescriptor getResourceDescriptor(final String name)
  {
    return descriptors.get(name);
  }



  /**
   * Returns the collection of all known descriptors indexes by the resource
   * name.
   *
   * @return All configured Resource Descriptors.
   */
  public Map<String, ResourceDescriptor> getDescriptors()
  {
    return Collections.unmodifiableMap(this.descriptors);
  }



  /**
   * Returns an instance of the Resource Descriptor Manager.
   *
   * @return The SchemaManager.
   */
  public static SchemaManager instance()
  {
    return instance;
  }

}
