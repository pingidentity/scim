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

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DebugType;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;



/**
 * This class provides a derived attribute implementation for the groups
 * attribute in User resources, which may be used when the directory server
 * does not provide the isMemberOf LDAP attribute. The groups are derived by
 * searching the DIT for static group entries whose members include the DN
 * of the User entry.
 */
public class GroupsDerivedAttribute extends DerivedAttribute
{
  /**
   * The name of the LDAP cn attribute.
   */
  private static final String ATTR_CN = "cn";

  /**
   * The name of the LDAP isMemberOf attribute.
   */
  private static final String ATTR_IS_MEMBER_OF = "isMemberOf";

  /**
   * The name of the LDAP member attribute.
   */
  private static final String ATTR_MEMBER = "member";

  /**
   * The name of the LDAP uniqueMember attribute.
   */
  private static final String ATTR_UNIQUE_MEMBER = "uniqueMember";

  /**
   * The attribute descriptor for the derived attribute.
   */
  private AttributeDescriptor descriptor;



  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return Collections.singleton(ATTR_IS_MEMBER_OF);
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry,
                                       final LDAPInterface ldapInterface,
                                       final String searchBaseDN)
      throws InvalidResourceException {
    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();

    if (entry.hasAttribute(ATTR_IS_MEMBER_OF))
    {
      for (final String dnString : entry.getAttributeValues(ATTR_IS_MEMBER_OF))
      {
        try
        {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>();

          final DN groupDN = new DN(dnString);
          final String groupID = groupDN.toNormalizedString();

          subAttributes.add(
              SCIMAttribute.create(
                  getAttributeDescriptor().getSubAttribute("value"),
                  SCIMAttributeValue.createStringValue(groupID)));

          final RDN rdn = groupDN.getRDN();
          String groupDisplayName = null;
          for (int i = 0; i < rdn.getAttributeNames().length; i++)
          {
            if (rdn.getAttributeNames()[i].equalsIgnoreCase(ATTR_CN))
            {
              groupDisplayName = rdn.getAttributeValues()[i];
            }
          }

          if (groupDisplayName != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    getAttributeDescriptor().getSubAttribute("display"),
                    SCIMAttributeValue.createStringValue(groupDisplayName)));
          }

          values.add(SCIMAttributeValue.createComplexValue(subAttributes));
        }
        catch (LDAPException e)
        {
          Debug.debugException(e);
        }
      }
    }
    else
    {
      final Filter filter = Filter.createORFilter(
          Filter.createEqualityFilter(ATTR_MEMBER, entry.getDN()),
          Filter.createEqualityFilter(ATTR_UNIQUE_MEMBER, entry.getDN()));

      try
      {
        final SearchResult searchResult =
            ldapInterface.search(searchBaseDN, SearchScope.SUB,
                                 filter, ATTR_CN);
        for (final SearchResultEntry resultEntry :
            searchResult.getSearchEntries())
        {
          final String groupID = resultEntry.getDN();

          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>();

          subAttributes.add(
              SCIMAttribute.create(
                  getAttributeDescriptor().getSubAttribute("value"),
                  SCIMAttributeValue.createStringValue(groupID)));

          final String groupDisplayName =
              resultEntry.getAttributeValue(ATTR_CN);
          if (groupDisplayName != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    getAttributeDescriptor().getSubAttribute("display"),
                    SCIMAttributeValue.createStringValue(groupDisplayName)));
          }

          values.add(SCIMAttributeValue.createComplexValue(subAttributes));
        }
      }
      catch (LDAPSearchException e)
      {
        Debug.debugException(e);
        Debug.debug(Level.WARNING, DebugType.OTHER,
                    "Error searching for values of the groups attribute", e);
        return null;
      }
    }

    if (values.isEmpty())
    {
      return null;
    }
    else
    {
      return SCIMAttribute.create(
          getAttributeDescriptor(),
          values.toArray(new SCIMAttributeValue[values.size()]));
    }
  }



  @Override
  public void initialize(final AttributeDescriptor descriptor)
  {
    this.descriptor = descriptor;
  }



  @Override
  public AttributeDescriptor getAttributeDescriptor()
  {
    return descriptor;
  }
}
