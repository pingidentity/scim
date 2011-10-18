/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.data;

import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMObject;

/**
 * Factory interface for creating SCIM resource instances for a given resource
 * descriptor.
 */
public interface ResourceFactory<R extends BaseResource>
{
  /**
   * Creates a new SCIM resource instance from a <code>SCIMObject</code>for
   * the specified resource descriptor.
   *
   * @param resourceDescriptor The resource descriptor for the SCIM resource
   *                           instance.
   * @param scimObject         The <code>SCIMObject</code> containing all the
   *                           SCIM attributes and their values.
   * @return                   A new SCIM resource instance.
   */
  R createResource(ResourceDescriptor resourceDescriptor,
                          SCIMObject scimObject);
}
