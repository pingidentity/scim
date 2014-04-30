/*
 * Copyright 2012-2014 UnboundID Corp.
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
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
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

  /**
   * The name of the LDAP manager attribute.
   */
  public static final String ATTR_MANAGER = "manager";

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
    return Collections.singleton(ATTR_MANAGER);
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
    if (entry.hasAttribute(ATTR_MANAGER))
    {
      final String dn = entry.getAttributeValue(ATTR_MANAGER);
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
            "The manager attribute does not have a managerId.");
      }

      final String resourceID = managerId.getValue().getStringValue();
      try
      {
        final String dn = searchResolver.getDnFromId(ldapInterface, resourceID);
        attributes.add(new Attribute(ATTR_MANAGER, dn));
      }
      catch(ResourceNotFoundException e)
      {
        //If the manager id is non-existent, we want to return a 400 to the
        //client, not a 404.
        throw new InvalidResourceException("The managerId '" + resourceID +
                "' does not exist.");
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> toLDAPAttributeTypes(final AttributePath scimAttribute)
      throws InvalidResourceException
  {
    String subAttributeName = scimAttribute.getSubAttributeName();
    if (subAttributeName != null)
    {
      // Just to make sure the sub-attribute is a valid one for this attribute.
      descriptor.getSubAttribute(subAttributeName);
    }

    return Collections.singleton(ATTR_MANAGER);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Filter toLDAPFilter(final SCIMFilter filter,
                             final LDAPRequestInterface ldapInterface,
                             final LDAPSearchResolver userResolver)
      throws InvalidResourceException
  {
    // Only the managerId sub-attribute will ever have a value so filter
    // must target that sub-attribute.
    String subAttribute = filter.getFilterAttribute().getSubAttributeName();
    if(subAttribute == null || !subAttribute.equals("managerId"))
    {
      return null;
    }

    final String ldapAttributeType = ATTR_MANAGER;
    final SCIMFilterType filterType = filter.getFilterType();
    final String filterValue = filter.getFilterValue();

    // Determine the DN for this member.
    try
    {
      switch (filterType)
      {
        // We don't have to worry about AND and OR filter types since they are
        // handled earlier by the resource mapper.
        case EQUALITY:
        {
          String dn;
          try
          {
            dn = userResolver.getDnFromId(ldapInterface, filterValue);
          }
          catch (ResourceNotFoundException e)
          {
            // Value is not a valid user. Will not match anything.
            return null;
          }
          return Filter.createEqualityFilter(ldapAttributeType, dn);
        }

        default:
          throw new InvalidResourceException(
              "Filter type " + filterType + " is not supported for attribute " +
                  getAttributeDescriptor().getName());
      }
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new InvalidResourceException(e.getMessage());
    }
  }
}
