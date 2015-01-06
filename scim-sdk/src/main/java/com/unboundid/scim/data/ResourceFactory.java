/*
 * Copyright 2011-2015 UnboundID Corp.
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
