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

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPSearchException;
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;



/**
 * This class provides a derived attribute implementation for the members
 * attribute in Group resources. For static groups, the members are taken from
 * the member or uniqueMember attributes in the group entry. For dynamic groups,
 * the members are derived by searching the DIT for user entries containing the
 * group DN as a value of the isMemberOf attribute.
 */
public class MembersDerivedAttribute extends DerivedAttribute
{
  /**
   * The name of the LDAP isMemberOf attribute.
   */
  private static final String ATTR_IS_MEMBER_OF = "isMemberOf";

  /**
   * The name of the LDAP member attribute.
   */
  private static final String ATTR_MEMBER = "member";

  /**
   * The name of the LDAP objectClass attribute.
   */
  private static final String ATTR_OBJECT_CLASS = "objectClass";

  /**
   * The name of the LDAP uniqueMember attribute.
   */
  private static final String ATTR_UNIQUE_MEMBER = "uniqueMember";

  /**
   * The name of the LDAP groupOfNames object class.
   */
  private static final String OC_GROUP_OF_NAMES = "groupOfNames";

  /**
   * The name of the LDAP groupOfUniqueNames object class.
   */
  private static final String OC_GROUP_OF_UNIQUE_NAMES = "groupOfUniqueNames";

  /**
   * The attribute descriptor for the derived attribute.
   */
  private AttributeDescriptor descriptor;

  /**
   * The set of LDAP attribute types needed in the group entry.
   */
  private static Set<String> ldapAttributeTypes = new HashSet<String>();
  static
  {
    ldapAttributeTypes.add(ATTR_MEMBER);
    ldapAttributeTypes.add(ATTR_UNIQUE_MEMBER);
    ldapAttributeTypes.add(ATTR_OBJECT_CLASS);
  }


  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return ldapAttributeTypes;
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry,
                                       final LDAPInterface ldapInterface,
                                       final String searchBaseDN)
      throws InvalidResourceException {
    List<String> members = null;
    if (entry.hasObjectClass(OC_GROUP_OF_NAMES))
    {
      if (entry.hasAttribute(ATTR_MEMBER))
      {
        members = Arrays.asList(entry.getAttributeValues(ATTR_MEMBER));
      }
    }
    else if (entry.hasObjectClass(OC_GROUP_OF_UNIQUE_NAMES))
    {
      if (entry.hasAttribute(ATTR_UNIQUE_MEMBER))
      {
        members = Arrays.asList(entry.getAttributeValues(ATTR_UNIQUE_MEMBER));
      }
    }
    else
    {
      try
      {
        final Filter filter =
            Filter.createEqualityFilter(ATTR_IS_MEMBER_OF, entry.getDN());
        final SearchResult searchResult =
            ldapInterface.search(searchBaseDN, SearchScope.SUB,
                                 filter, "1.1");
        members = new ArrayList<String>(searchResult.getEntryCount());
        for (final SearchResultEntry resultEntry :
            searchResult.getSearchEntries())
        {
          members.add(resultEntry.getDN());
        }
      }
      catch (LDAPSearchException e)
      {
        Debug.debugException(e);
        Debug.debug(Level.WARNING, DebugType.OTHER,
                    "Error searching for values of the members attribute", e);
        return null;
      }
    }

    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();
    if (members != null)
    {
      for (final String userID : members)
      {
        final List<SCIMAttribute> subAttributes =
            new ArrayList<SCIMAttribute>();

        subAttributes.add(
            SCIMAttribute.create(
                getAttributeDescriptor().getSubAttribute("value"),
                SCIMAttributeValue.createStringValue(userID)));

        values.add(SCIMAttributeValue.createComplexValue(subAttributes));
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
