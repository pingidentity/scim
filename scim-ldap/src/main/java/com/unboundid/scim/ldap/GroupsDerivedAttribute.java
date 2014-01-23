/*
 * Copyright 2011-2013 UnboundID Corp.
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
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPURL;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DebugType;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.util.StaticUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;



/**
 * This class provides a derived attribute implementation for the groups
 * attribute in User resources, which may be used when the directory server
 * does not provide the isMemberOf LDAP attribute. The groups are derived by
 * searching the DIT for static group entries whose members include the DN
 * of the User entry.
 * <p>
 * The &lt;derivation&gt; element for this derived attribute accepts a special
 * child element, &lt;LDAPSearchRef idref="exampleSearchParams"/&gt;, which
 * specifies the LDAP search parameters to use when searching for Group entries.
 */
public class GroupsDerivedAttribute extends DerivedAttribute
{
  /**
   * The name of the argument that indicates whether the backend DS provides
   * the isMemberOf attribute.
   */
  private static final String HAVE_ISMEMBEROF = "haveIsMemberOf";

  /**
   * The name of the LDAP cn attribute.
   */
  private static final String ATTR_CN = "cn";

  /**
   * The name of the LDAP objectClass attribute.
   */
  private static final String ATTR_OBJECT_CLASS = "objectClass";

  /**
   * The name of the LDAP isMemberOf attribute.
   */
  private static final String ATTR_IS_MEMBER_OF = "isMemberOf";

  /**
   * The name of the LDAP member attribute.
   */
  private static final String ATTR_MEMBER = "member";

  /**
   * The name of the LDAP memberURL attribute.
   */
  private static final String ATTR_MEMBER_URL = "memberURL";

  /**
   * The name of the LDAP uniqueMember attribute.
   */
  private static final String ATTR_UNIQUE_MEMBER = "uniqueMember";

  /**
   * The name of the groupOfURLs object class.
   */
  private static final String OC_GROUP_OF_URLS = "groupOfURLs";

  /**
   * The name of the ds-virtual-static-group object class.
   */
  private static final String OC_VIRTUAL_STATIC_GROUP =
      "ds-virtual-static-group";

  /**
   * The "direct" canonical value for the type sub-attribute.
   */
  private static final String DIRECT_GROUP = "direct";

  /**
   * The "indirect" canonical value for the type sub-attribute.
   */
  private static final String INDIRECT_GROUP = "indirect";

  /**
   * The attribute descriptor for the derived attribute.
   */
  private AttributeDescriptor descriptor;

  /**
   * The LDAPSearchResolver to use when looking for groups to which
   * a certain user belongs.
   */
  private LDAPSearchResolver groupResolver;

  /**
   * Indicates whether the backend DS provides the isMemberOf attribute.
   */
  private boolean haveIsMemberOf;



  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return Collections.singleton(ATTR_IS_MEMBER_OF);
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry,
                                       final LDAPRequestInterface ldapInterface,
                                       final LDAPSearchResolver userResolver)
      throws SCIMException
  {
    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();

    if (groupResolver == null)
    {
      Debug.debug(Level.WARNING, DebugType.OTHER,
                  "Cannot provide the groups attribute because there are " +
                  "no LDAPSearch parameters");
      return null;
    }

    try
    {
      if (haveIsMemberOf)
      {
        // We can use the isMemberOf attribute
        if (entry.hasAttribute(ATTR_IS_MEMBER_OF))
        {
          final List<String> attrList = new ArrayList<String>(3);
          attrList.add(ATTR_CN);
          attrList.add(ATTR_OBJECT_CLASS);
          groupResolver.addIdAttribute(attrList);
          final String[] attrsToGet =
              attrList.toArray(new String[attrList.size()]);

          for (final String dnString :
              entry.getAttributeValues(ATTR_IS_MEMBER_OF))
          {
            // Make sure the group is scoped within the base DN.
            if (groupResolver.isDnInScope(dnString))
            {
              // Retrieve the group entry and passing in the search param filter
              // if available.
              SearchRequest searchRequest =
                  new SearchRequest(dnString, SearchScope.BASE,
                                    groupResolver.getFilterString(),
                                    attrsToGet);
              searchRequest.setSizeLimit(1);
              SearchResultEntry groupEntry =
                  ldapInterface.searchForEntry(searchRequest);

              if(groupEntry != null)
              {
                // This group is considered direct iff it is a non-virtual
                // static group and the entry is listed as a member or
                // uniqueMember of this group (i.e. it's not nested).
                boolean isDirect = false;
                if(!groupEntry.hasObjectClass(OC_GROUP_OF_URLS) &&
                   !groupEntry.hasObjectClass(OC_VIRTUAL_STATIC_GROUP))
                {
                  // Make sure the entry DN is listed as a member or
                  // uniqueMember.
                  searchRequest =
                      new SearchRequest(dnString, SearchScope.BASE,
                                        groupsFilter(entry.getDN(), false),
                                        "1.1");
                  searchRequest.setSizeLimit(1);
                  isDirect =
                      ldapInterface.searchForEntry(searchRequest) != null;
                }
                final String resourceID =
                    groupResolver.getIdFromEntry(groupEntry);
                values.add(createGroupValue(
                    resourceID,
                    groupEntry.getAttributeValue(ATTR_CN), isDirect));
              }
            }
          }
        }
      }
      else
      {
        // We can't use isMemberOf so we'll have to find all group entries
        // that satisfies the search param. This should give us all static
        // groups (including virtual static groups) that the entry is a member
        // of as well as all dynamic groups that satisfy the search params.
        final List<DN> visitedGroups = new LinkedList<DN>();
        for (DN baseDN : groupResolver.getBaseDNs())
        {
          findGroupsForMember(entry, ldapInterface, baseDN.toString(),
              groupsFilter(entry.getDN(), true), values, visitedGroups,
              false);
        }
      }
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      throw ResourceMapper.toSCIMException(
          "Error searching for values of the groups attribute: " +
          StaticUtils.getExceptionMessage(e), e);
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



  /**
   * {@inheritDoc}
   */
  @Override
  public void toLDAPAttributes(final SCIMObject scimObject,
                               final Collection<Attribute> attributes,
                               final LDAPRequestInterface ldapInterface,
                               final LDAPSearchResolver groupResolver)
      throws SCIMException
  {
    // No implementation required because this attribute is read-only.
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> toLDAPAttributeTypes(final AttributePath scimAttribute)
      throws InvalidResourceException
  {
    // No implementation required because this attribute is read-only.
    return Collections.emptySet();
  }



  @Override
  public void initialize(final AttributeDescriptor descriptor)
  {
    this.descriptor = descriptor;
    if(getArguments().containsKey(LDAP_SEARCH_REF))
    {
      Object o = getArguments().get(LDAP_SEARCH_REF);
      if(o instanceof LDAPSearchResolver)
      {
        groupResolver = (LDAPSearchResolver) o;
      }
    }

    haveIsMemberOf = true;
    Object o = getArguments().get(HAVE_ISMEMBEROF);
    if (o != null)
    {
      haveIsMemberOf = Boolean.valueOf(o.toString());
    }
  }



  @Override
  public AttributeDescriptor getAttributeDescriptor()
  {
    return descriptor;
  }



  /**
   * Construct a filter that could be used to find all static groups with the
   * provided member DN (and optionally any dynamic groups as well).
   *
   * @param memberDN The member DN used to determining the static groups for
   *                 which it belongs.
   * @param includeDynamicGroups Whether dynamic groups should be included.
   *
   * @return A filter that could be used to find all static groups with the
   * provided member DN.
   * @throws LDAPException if an error occurs while parsing the filter from
   *                       searchParams.
   */
  private Filter groupsFilter(final String memberDN,
                              final boolean includeDynamicGroups)
      throws LDAPException
  {
    Filter filter = null;
    if(groupResolver != null)
    {
      //This will be a filter that handles all the Group object classes
      filter = Filter.create(groupResolver.getFilterString());
    }

    List<Filter> memberFilters = new ArrayList<Filter>(3);
    memberFilters.add(Filter.createEqualityFilter(ATTR_MEMBER, memberDN));
    memberFilters.add(
        Filter.createEqualityFilter(ATTR_UNIQUE_MEMBER, memberDN));

    if(includeDynamicGroups)
    {
      memberFilters.add(
          Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OC_GROUP_OF_URLS));
    }

    if(filter != null)
    {
      return Filter.createANDFilter(filter,
                Filter.createORFilter(memberFilters));
    }
    else
    {
      return Filter.createORFilter(memberFilters);
    }
  }

  /**
   * Add all group entries returned from the provided search parameters and
   * recursively find all nested groups as well.
   *
   * @param entry          An LDAP entry representing the SCIM resource for
   *                       which a SCIM attribute value is to be derived.
   * @param ldapInterface  An LDAP interface that may be used to search the DIT.
   * @param baseDN         The search base DN for the DIT.
   * @param filter         The filter to search the DIT.
   * @param values         The values of the groups attribute.
   * @param visitedGroups  Groups that were already visited.
   * @param nested         Whether the groups found are nested.
   * @throws LDAPException if an error occurs while performing the search.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  private void findGroupsForMember(final Entry entry,
                                   final LDAPRequestInterface ldapInterface,
                                   final String baseDN,
                                   final Filter filter,
                                   final List<SCIMAttributeValue> values,
                                   final List<DN> visitedGroups,
                                   final boolean nested)
      throws LDAPException, InvalidResourceException
  {
    final List<String> attrList = new ArrayList<String>(4);
    attrList.add(ATTR_CN);
    attrList.add(ATTR_OBJECT_CLASS);
    attrList.add(ATTR_MEMBER_URL);
    groupResolver.addIdAttribute(attrList);
    final String[] attrsToGet =
        attrList.toArray(new String[attrList.size()]);

    // Find all groups
    final SearchRequest searchRequest =
        new SearchRequest(baseDN, SearchScope.SUB, filter, attrsToGet);
    final SearchResult searchResult = ldapInterface.search(searchRequest);

    List<SearchResultEntry> entriesToVisit =
        new ArrayList<SearchResultEntry>(searchResult.getEntryCount());

    for (final SearchResultEntry resultEntry :
        searchResult.getSearchEntries())
    {
      // Make sure we haven't visited this group before.
      DN groupDN = resultEntry.getParsedDN();
      if(!visitedGroups.contains(groupDN))
      {
        visitedGroups.add(groupDN);
        entriesToVisit.add(resultEntry);
        final String resourceID = groupResolver.getIdFromEntry(resultEntry);
        if(resultEntry.hasObjectClass(OC_GROUP_OF_URLS))
        {
          // This is a dynamic group, see if the entry should be a member
          String memberUrl = resultEntry.getAttributeValue(ATTR_MEMBER_URL);
          if(memberUrl != null)
          {
            LDAPURL url = new LDAPURL(memberUrl);
            if(entry.matchesBaseAndScope(url.getBaseDN(), url.getScope()) &&
                url.getFilter().matchesEntry(entry))
            {
              values.add(createGroupValue(resourceID,
                  resultEntry.getAttributeValue(ATTR_CN), false));
            }
          }
        }
        else
        {
          // This is a static group that we are a member of.
          values.add(createGroupValue(resourceID,
              resultEntry.getAttributeValue(ATTR_CN), !nested &&
              !resultEntry.hasObjectClass(OC_VIRTUAL_STATIC_GROUP)));
        }
      }
    }

    for (final SearchResultEntry resultEntry : entriesToVisit)
    {
      // Recursively find all groups that nest this group.
      findGroupsForMember(entry, ldapInterface,
          baseDN, groupsFilter(resultEntry.getDN(), false), values,
          visitedGroups, true);
    }
  }

  /**
   * Create a value for the groups multi-valued attribute.
   *
   * @param id The ID of group.
   * @param displayName The displayName of the group.
   * @param isDirect Whether the group is direct or indirect.
   * @return The constructed SCIMAttributeValue for the groups multi-valued
   *         attribute.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  private SCIMAttributeValue createGroupValue(final String id,
                                              final String displayName,
                                              final boolean isDirect)
      throws InvalidResourceException
  {
    final List<SCIMAttribute> subAttributes =
        new ArrayList<SCIMAttribute>(3);

    subAttributes.add(
        SCIMAttribute.create(
            getAttributeDescriptor().getSubAttribute("value"),
            SCIMAttributeValue.createStringValue(id)));

    if (displayName != null)
    {
      subAttributes.add(
          SCIMAttribute.create(
              getAttributeDescriptor().getSubAttribute("display"),
              SCIMAttributeValue.createStringValue(displayName)));
    }

    subAttributes.add(
        SCIMAttribute.create(
            getAttributeDescriptor().getSubAttribute("type"),
            SCIMAttributeValue.createStringValue(
                isDirect ? DIRECT_GROUP : INDIRECT_GROUP)));

    return SCIMAttributeValue.createComplexValue(subAttributes);
  }
}
