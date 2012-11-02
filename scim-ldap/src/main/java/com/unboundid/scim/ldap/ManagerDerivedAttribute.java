/*
 * Copyright 2012 UnboundID Corp.
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

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;



/**
 * This class provides a derived attribute implementation for the manager
 * extension attribute in User resources.
 * <p>
 * The &lt;derivation&gt; element for this derived attribute accepts a special
 * child element, &lt;LDAPSearchRef idref="exampleSearchParams"/&gt;, which
 * specifies the LDAP search parameters to use when resolving resource IDs.
 */
public class ManagerDerivedAttribute extends DerivedAttribute
{
  private AttributeDescriptor descriptor;



  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(final AttributeDescriptor descriptor)
  {
    this.descriptor = descriptor;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public AttributeDescriptor getAttributeDescriptor()
  {
    return descriptor;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return Collections.singleton("manager");
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry,
                                       final LDAPRequestInterface ldapInterface,
                                       final LDAPSearchResolver searchResolver)
      throws SCIMException
  {
    if (entry.hasAttribute("manager"))
    {
      final String dn = entry.getAttributeValue("manager");
      final String resourceID = searchResolver.getIdFromDn(ldapInterface, dn);

      final List<SCIMAttribute> attributes = new ArrayList<SCIMAttribute>(1);
      attributes.add(SCIMAttribute.create(
          descriptor.getSubAttribute("managerId"),
          SCIMAttributeValue.createStringValue(resourceID)));

      return SCIMAttribute.create(
          descriptor, SCIMAttributeValue.createComplexValue(attributes));
    }

    return null;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void toLDAPAttributes(final SCIMObject scimObject,
                               final Collection<Attribute> attributes,
                               final LDAPRequestInterface ldapInterface,
                               final LDAPSearchResolver searchResolver)
      throws SCIMException
  {
    final SCIMAttribute scimAttribute =
        scimObject.getAttribute(getAttributeDescriptor().getSchema(),
                                getAttributeDescriptor().getName());
    if (scimAttribute != null)
    {
      final SCIMAttribute managerId =
          scimAttribute.getValue().getAttribute("managerId");
      if (managerId == null)
      {
        throw new InvalidResourceException(
            "The manager attribute does not have a managerId");
      }
      final String resourceID = managerId.getValue().getStringValue();
      final String dn = searchResolver.getDnFromId(ldapInterface, resourceID);
      attributes.add(new Attribute("manager", dn));
    }
  }
}
