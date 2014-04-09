/*
 * Copyright 2011-2014 UnboundID Corp.
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

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SortParameters;

/**
 * This class provides a special attribute mapper that only provides SCIM to
 * LDAP mapping of the password attribute.
 */
public class PasswordAttributeMapper extends SimpleAttributeMapper
{
  /**
   * Create a new instance of a password attribute mapper.
   *
   * @param attributeDescriptor  The SCIM password attribute type that is mapped
   *                             by this attribute mapper.
   * @param transformation     The attribute transformation to be applied
   *                           by this attribute mapper.
   */
  public PasswordAttributeMapper(final AttributeDescriptor attributeDescriptor,
                                 final AttributeTransformation transformation)
  {
    super(attributeDescriptor, transformation);
  }

  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry) {
    // Will never have a value.
    return null;
  }

  @Override
  public ServerSideSortRequestControl toLDAPSortControl(
      final SortParameters sortParameters) throws InvalidResourceException
  {
    // Can't sort on password as it will never have a value.
    return null;
  }

  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
      throws InvalidResourceException
  {
    // Filters on password won't matching anything.
    return null;
  }
}
