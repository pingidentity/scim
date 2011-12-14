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
 * <p>
 * The &lt;derivation&gt; element for this derived attribute accepts a special
 * child element, &lt;LDAPSearchRef idref="exampleSearchParams"/&gt;, which
 * specifies the LDAP search parameters to use when searching for group members.
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
   * The name of the LDAP groupOfEntries object class.
   */
  private static final String OC_GROUP_OF_ENTRIES = "groupOfEntries";

  /**
   * The attribute descriptor for the derived attribute.
   */
  private AttributeDescriptor descriptor;

  /**
   * The LDAPSearchParameters to use when looking for members which are part
   * of this attribute.
   */
  private LDAPSearchParameters searchParams;

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
                                       final String groupsBaseDN)
                                              throws InvalidResourceException
  {
    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();

    List<String> members = null;
    Filter isMemberFilter = null;
    Filter usersFilter = null;
    Filter compoundFilter = null;

    if(searchParams != null)
    {
      try
      {
        isMemberFilter = Filter.createEqualityFilter(
                                    ATTR_IS_MEMBER_OF, entry.getDN());
        usersFilter = Filter.create(searchParams.getFilter());
        compoundFilter = Filter.createANDFilter(isMemberFilter, usersFilter);
      }
      catch(LDAPException e)
      {
        Debug.debugException(e);
      }
    }

    if (entry.hasObjectClass(OC_GROUP_OF_NAMES) ||
            entry.hasObjectClass(OC_GROUP_OF_ENTRIES))
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
        Set<DN> memberDNs = new HashSet<DN>();
        if(compoundFilter != null)
        {
          //Search for 'User' group members
          SearchResult searchResult = ldapInterface.search(
              searchParams.getBaseDN(), SearchScope.SUB, compoundFilter, "1.1");

          List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>(2);
          for(SearchResultEntry resultEntry : searchResult.getSearchEntries())
          {
            subAttributes.clear();

            subAttributes.add(SCIMAttribute.create(
                getAttributeDescriptor().getSubAttribute("value"),
                   SCIMAttributeValue.createStringValue(resultEntry.getDN())));

            subAttributes.add(SCIMAttribute.create(
                getAttributeDescriptor().getSubAttribute("type"),
                   SCIMAttributeValue.createStringValue("User")));

            values.add(SCIMAttributeValue.createComplexValue(subAttributes));

            memberDNs.add(resultEntry.getParsedDN());
          }
        }

        //Search for 'Group' group members (note this may pick up 'User' entries
        //as well, but they will be filtered out using the memberDNs list below)
        SearchResult searchResult = ldapInterface.search(
                groupsBaseDN, SearchScope.SUB, isMemberFilter, "1.1");

        List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>(2);
        for(SearchResultEntry resultEntry : searchResult.getSearchEntries())
        {
          if(memberDNs.contains(resultEntry.getParsedDN()))
          {
            //If the entry is already present in the members set, then it is
            //a 'User' member and has already been added to the result set.
            continue;
          }

          subAttributes.clear();

          subAttributes.add(SCIMAttribute.create(
              getAttributeDescriptor().getSubAttribute("value"),
                 SCIMAttributeValue.createStringValue(resultEntry.getDN())));

          subAttributes.add(SCIMAttribute.create(
              getAttributeDescriptor().getSubAttribute("type"),
                 SCIMAttributeValue.createStringValue("Group")));

          values.add(SCIMAttributeValue.createComplexValue(subAttributes));

          memberDNs.add(resultEntry.getParsedDN());
        }
      }
      catch (LDAPException e)
      {
        Debug.debugException(e);
        Debug.debug(Level.WARNING, DebugType.OTHER,
                    "Error searching for values of the members attribute", e);
        return null;
      }
    }


    if (members != null)
    {
      List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>(2);
      for (final String userID : members)
      {
        subAttributes.clear();

        subAttributes.add(SCIMAttribute.create(
            getAttributeDescriptor().getSubAttribute("value"),
               SCIMAttributeValue.createStringValue(userID)));

        try
        {
          //If usersFilter is not null, then we know searchParams is not null.
          if(usersFilter != null)
          {
            if(DN.isDescendantOf(userID, searchParams.getBaseDN(), true))
            {
              SearchResult searchResult = ldapInterface.search(
                      userID, SearchScope.BASE, usersFilter, "1.1");

              //If both the baseDN and filter match that of the 'User' resource,
              //the the type is 'User'
              if(searchResult.getEntryCount() == 1)
              {
                subAttributes.add(SCIMAttribute.create(
                      getAttributeDescriptor().getSubAttribute("type"),
                         SCIMAttributeValue.createStringValue("User")));
                values.add(SCIMAttributeValue.createComplexValue(
                                                    subAttributes));
                continue;
              }
            }

            //If not a 'User', check if it falls under the baseDN for 'Group'
            //resources. Note that it's possible for the entry to be a
            //descendant of both the User and Group base DNs, but if it didn't
            //match the 'User' filter, then it can only be a 'Group'.
            if(DN.isDescendantOf(userID, groupsBaseDN, true))
            {
              subAttributes.add(SCIMAttribute.create(
                      getAttributeDescriptor().getSubAttribute("type"),
                         SCIMAttributeValue.createStringValue("Group")));
              values.add(SCIMAttributeValue.createComplexValue(subAttributes));
            }
            else
            {
              //This group member is not within the scope of Users or Groups
              if(Debug.debugEnabled())
              {
                Debug.debug(Level.INFO, DebugType.OTHER,
                    "Skipping group member '" + userID + "' for group '" +
                    entry.getDN() + "' because it is not within the scope of " +
                    "the SCIM User or Group resources.");
              }
            }
          }
          else if(DN.isDescendantOf(userID, groupsBaseDN, true))
          {
            //In the absence of the LDAPSearchParameters in the
            //DerivedAttribute, we cannot populate the 'type' sub-attribute
            //(i.e. degrade gracefully).
            values.add(SCIMAttributeValue.createComplexValue(subAttributes));
          }
          else
          {
            //This group member is not within the scope of Users or Groups
            if(Debug.debugEnabled())
            {
              Debug.debug(Level.INFO, DebugType.OTHER,
                  "Skipping group member '" + userID + "' for group '" +
                  entry.getDN() + "' because it is not within the scope of " +
                  "the SCIM User or Group resources.");
            }
          }
        }
        catch(LDAPException e)
        {
          Debug.debugException(e);
        }
      }
    }

    if (values.isEmpty())
    {
      return null;
    }
    else
    {
      return SCIMAttribute.create(getAttributeDescriptor(),
                  values.toArray(new SCIMAttributeValue[values.size()]));
    }
  }



  @Override
  public void initialize(final AttributeDescriptor descriptor)
  {
    this.descriptor = descriptor;
    if(getArguments().containsKey(LDAP_SEARCH_REF))
    {
      Object o = getArguments().get(LDAP_SEARCH_REF);
      if(o instanceof LDAPSearchParameters)
      {
        searchParams = (LDAPSearchParameters) o;
      }
    }
  }



  @Override
  public AttributeDescriptor getAttributeDescriptor()
  {
    return descriptor;
  }


  /**
   * Returns the configured LDAPSearchParameters for this derived attribute,
   * or null if none were explicitly set (using the <LDAPSearchRef> element).
   * These can be used to find the users within a certain group.
   *
   * @return an LDAPSearchParameters instance, or null if none was set.
   */
  public LDAPSearchParameters getLDAPSearchParameters()
  {
    return searchParams;
  }
}
