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
import com.unboundid.ldap.sdk.LDAPURL;
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
import java.util.TreeSet;
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
   * The name of the LDAP memberUrl attribute.
   */
  private static final String ATTR_MEMBER_URL = "memberURL";

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
   * The name of the groupOfURLs object class.
   */
  private static final String OC_GROUP_OF_URLS = "groupOfURLs";

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
    ldapAttributeTypes.add(ATTR_MEMBER_URL);
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
    Filter usersFilter = null;
    String usersBaseDN = null;

    if(searchParams != null)
    {
      try
      {
        usersBaseDN = searchParams.getBaseDN();
        usersFilter = Filter.create(searchParams.getFilter());
      }
      catch(LDAPException e)
      {
        Debug.debugException(e);
        Debug.debug(Level.SEVERE, DebugType.OTHER,
                "The MembersDerivedAttribute does not contain a reference " +
                "to the LDAP search parameters for the User resource.");
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
    else if (entry.hasObjectClass(OC_GROUP_OF_URLS))
    {
      //Determine the dynamic group members and their 'type'
      if(entry.hasAttribute(ATTR_MEMBER_URL))
      {
        String[] attrsToGet;
        if(usersFilter != null)
        {
          Set<String> attrSet = getAllFilterAttributes(usersFilter);
          attrsToGet = attrSet.toArray(new String[attrSet.size()]);
        }
        else
        {
          attrsToGet = new String[] { "objectClass" };
        }

        String[] memberURLs = entry.getAttributeValues(ATTR_MEMBER_URL);
        for(String url : memberURLs)
        {
          try
          {
            LDAPURL ldapURL = new LDAPURL(url);
            SearchResult searchResult = ldapInterface.search(
                    ldapURL.getBaseDN().toString(),
                    SearchScope.SUB, ldapURL.getFilter(), attrsToGet);

            if(searchResult.getEntryCount() > 0)
            {
              List<SCIMAttribute> subAttributes =
                        new ArrayList<SCIMAttribute>(2);
              for(SearchResultEntry rEntry : searchResult.getSearchEntries())
              {
                subAttributes.clear();

                subAttributes.add(SCIMAttribute.create(
                    getAttributeDescriptor().getSubAttribute("value"),
                       SCIMAttributeValue.createStringValue(rEntry.getDN())));

                if(usersFilter == null)
                {
                  //In the absence of the LDAPSearchParameters in the
                  //DerivedAttribute, we cannot populate the 'type'
                  //sub-attribute (i.e. degrade gracefully). Furthermore,
                  //if the given entry is not below the Groups base DN, then
                  //it is out of scope.
                  if(DN.isDescendantOf(rEntry.getDN(), groupsBaseDN, true))
                  {
                    values.add(
                          SCIMAttributeValue.createComplexValue(subAttributes));
                    continue;
                  }
                  else
                  {
                    //This group member is not within any SCIM scope
                    if(Debug.debugEnabled())
                    {
                      Debug.debug(Level.INFO, DebugType.OTHER,
                        "Skipping group member '" + rEntry.getDN() +
                        "' for group '" + entry.getDN() + "' because it is " +
                        "not within the scope of the SCIM User or Group " +
                        "resources.");
                    }
                    continue;
                  }
                }
                else if(usersFilter.matchesEntry(rEntry))
                {
                  if(DN.isDescendantOf(rEntry.getDN(), usersBaseDN, true))
                  {
                    subAttributes.add(SCIMAttribute.create(
                            getAttributeDescriptor().getSubAttribute("type"),
                               SCIMAttributeValue.createStringValue("User")));
                  }
                  else
                  {
                    //The group member is not within the Users scope
                    if(Debug.debugEnabled())
                    {
                      Debug.debug(Level.INFO, DebugType.OTHER,
                        "Skipping group member '" + rEntry.getDN() +
                        "' for group '" + entry.getDN() + "' because it is " +
                        "not within the scope of the SCIM User resource.");
                    }
                    continue;
                  }
                }
                else
                {
                  if(DN.isDescendantOf(rEntry.getDN(), groupsBaseDN, true))
                  {
                    subAttributes.add(SCIMAttribute.create(
                            getAttributeDescriptor().getSubAttribute("type"),
                               SCIMAttributeValue.createStringValue("Group")));
                  }
                  else
                  {
                    //The group member is not within the Groups scope
                    if(Debug.debugEnabled())
                    {
                      Debug.debug(Level.INFO, DebugType.OTHER,
                        "Skipping group member '" + rEntry.getDN() +
                        "' for group '" + entry.getDN() + "' because it is " +
                        "not within the scope of the SCIM Group resource.");
                    }
                    continue;
                  }
                }
                values.add(
                        SCIMAttributeValue.createComplexValue(subAttributes));
              }
            }
          }
          catch(LDAPException e)
          {
            Debug.debugException(e);
            Debug.debug(Level.WARNING, DebugType.OTHER,
                    "Error searching for values of the members attribute", e);
            return null;
          }
        }
      }
    }

    //Determine the 'type' for static and virtual static groups
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
          //If usersFilter is not null, then we know usersBaseDN is not null.
          if(usersFilter != null)
          {
            if(DN.isDescendantOf(userID, usersBaseDN, true))
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


  /**
   * Returns a set of all the attributes used in the given filter. This method
   * invokes itself recursively if the given filter is a compound (AND/OR)
   * filter.
   *
   * @param filter the Filter whose attribute names to get
   * @return a set of attribute names
   */
  private Set<String> getAllFilterAttributes(final Filter filter)
  {
    Set<String> attrNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    if(filter.getFilterType() == Filter.FILTER_TYPE_AND ||
            filter.getFilterType() == Filter.FILTER_TYPE_OR)
    {
      for(Filter component : filter.getComponents())
      {
        attrNames.addAll(getAllFilterAttributes(component));
      }
      return attrNames;
    }
    else
    {
      attrNames.add(filter.getAttributeName());
      return attrNames;
    }
  }
}
