/*
 * Copyright 2011 UnboundID Corp.
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
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.Collection;

/**
 * This class represents a Group resource.
 */
public class GroupResource extends BaseResource
{
  /**
   * A <code>ResourceFactory</code> for creating <code>GroupResource</code>
   * instances.
   */
  public static final ResourceFactory<GroupResource> GROUP_RESOURCE_FACTORY =
      new ResourceFactory<GroupResource>() {
    public GroupResource createResource(
        final ResourceDescriptor resourceDescriptor,
        final SCIMObject scimObject) {
      return new GroupResource(resourceDescriptor, scimObject);
    }
  };

  /**
   * Construct an empty <code>GroupResource</code> with the specified
   * <code>ResourceDescriptor</code>.
   *
   * @param resourceDescriptor The resource descriptor for this SCIM resource.
   */
  public GroupResource(final ResourceDescriptor resourceDescriptor) {
    super(resourceDescriptor);
  }

  /**
   * Construct a <code>GroupResource</code> with the specified
   * <code>ResourceDescriptor</code> and backed by the given
   * <code>SCIMObject</code>.
   *
   * @param resourceDescriptor The resource descriptor for this SCIM resource.
   * @param scimObject         The <code>SCIMObject</code> containing all the
   *                           SCIM attributes and their values.
   */
  public GroupResource(final ResourceDescriptor resourceDescriptor,
                       final SCIMObject scimObject) {
    super(resourceDescriptor, scimObject);
  }

  /**
   * Retrieves the human readable name for the Group.
   *
   * @return the human readable name for the Group.
   */
  public String getDisplayName()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "displayName", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the human readable name for the Group.
   *
   * @param displayName the human readable name for the Group.
   * @return this resource instance.
   */
  public GroupResource setDisplayName(final String displayName)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "displayName",
          AttributeValueResolver.STRING_RESOLVER, displayName);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Retrieves the list of member IDs of the Group.
   *
   * @return the list of member IDs of the Group.
   */
  public Collection<Entry<String>> getMembers()
  {
    return getAttributeValues(SCIMConstants.SCHEMA_URI_CORE,
        "members", Entry.STRINGS_RESOLVER);
  }

  /**
   * Sets the list of member IDs of the Group.
   *
   * @param members the list of member IDs of the Group.
   * @return this resource instance.
   */
  public GroupResource setMembers(final Collection<Entry<String>> members)
  {
    try {
      setAttributeValues(SCIMConstants.SCHEMA_URI_CORE, "members",
          Entry.STRINGS_RESOLVER, members);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }
}
