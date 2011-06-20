/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.config;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages the set of well known, configured SCIM Resource Descriptors.
 */
public final class ResourceDescriptorManager {
  private static ResourceDescriptorManager instance = new
    ResourceDescriptorManager();

  private Map<String, ResourceDescriptor> descriptors =
    new HashMap<String, ResourceDescriptor>();


  /**
   * Constructs the Resource Descriptor Manager singleton.
   */
  private ResourceDescriptorManager() {
  }

  /**
   * Initializes the manager with the specified SCIM Resource Descriptors.
   * @param schemas The schema files containing SCIM Resources.
   * @throws Exception Throw if error encountered processing specified files.
   */
  public static void init(final File[] schemas) throws Exception {
    XmlSchemaParser parser = new XmlSchemaParser(schemas);
    Collection<ResourceDescriptor> descriptors = parser.getDescriptors();
    for (ResourceDescriptor r : descriptors) {
      instance.descriptors.put(r.getName(), r);
    }
  }

  /**
   * Returns the specified Resource Descriptor.
   *
   * @param name The SCIM resource name; e.g., user or group.
   * @return The SCIM resource or null if not found.
   */
  public ResourceDescriptor getResourceDescriptor(final String name) {
    return this.descriptors.get(name);
  }

  /**
   * Returns the collection of all known descriptors indexes by the resource
   * name.
   *
   * @return All configured Resource Descriptors.
   */
  public Map<String, ResourceDescriptor> getDescriptors() {
    return Collections.unmodifiableMap(this.descriptors);
  }

  /**
   * Returns an instance of the Resource Descriptor Manager.
   *
   * @return The ResourceDescriptorManager.
   */
  public static ResourceDescriptorManager instance() {
    return instance;
  }

}
