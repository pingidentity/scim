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

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
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
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.util.StaticUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
   * The name of the LDAP member attribute.
   */
  public static final String ATTR_MEMBER = "member";

  /**
   * The name of the LDAP uniqueMember attribute.
   */
  public static final String ATTR_UNIQUE_MEMBER = "uniqueMember";

  /**
   * The name of the LDAP memberUrl attribute.
   */
  public static final String ATTR_MEMBER_URL = "memberURL";

  /**
   * The name of the LDAP groupOfNames object class.
   */
  public static final String OC_GROUP_OF_NAMES = "groupOfNames";

  /**
   * The name of the LDAP groupOfUniqueNames object class.
   */
  public static final String OC_GROUP_OF_UNIQUE_NAMES = "groupOfUniqueNames";

  /**
   * The name of the LDAP groupOfEntries object class.
   */
  public static final String OC_GROUP_OF_ENTRIES = "groupOfEntries";

  /**
   * The name of the groupOfURLs object class.
   */
  public static final String OC_GROUP_OF_URLS = "groupOfURLs";

  /**
   * The attribute descriptor for the derived attribute.
   */
  protected AttributeDescriptor descriptor;

  /**
   * The LDAPSearchResolver to use for user resource when looking for
   * members which are part of this attribute.
   */
  protected LDAPSearchResolver userResolver;

  /**
   * The set of LDAP attribute types needed in the group entry.
   */
  protected static Set<String> ldapAttributeTypes = new HashSet<String>();
  static
  {
    ldapAttributeTypes.add(ATTR_MEMBER);
    ldapAttributeTypes.add(ATTR_UNIQUE_MEMBER);
    ldapAttributeTypes.add(ATTR_MEMBER_URL);
  }


  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return ldapAttributeTypes;
  }



  @Override
  public SCIMAttribute toSCIMAttribute(
      final Entry entry,
      final LDAPRequestInterface ldapInterface,
      final LDAPSearchResolver groupResolver)
      throws SCIMException
  {
    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();

    try
    {
      final Set<String> attrSet = groupResolver.getFilterAndIdAttributes();
      if(userResolver != null)
      {
        attrSet.addAll(userResolver.getFilterAndIdAttributes());
      }
      final String[] attrsToGet = attrSet.toArray(new String[attrSet.size()]);

      String[] members = null;

      if (entry.hasObjectClass(OC_GROUP_OF_NAMES) ||
              entry.hasObjectClass(OC_GROUP_OF_ENTRIES))
      {
        if (entry.hasAttribute(ATTR_MEMBER))
        {
          members = entry.getAttributeValues(ATTR_MEMBER);
        }
      }
      else if (entry.hasObjectClass(OC_GROUP_OF_UNIQUE_NAMES))
      {
        if (entry.hasAttribute(ATTR_UNIQUE_MEMBER))
        {
          members = entry.getAttributeValues(ATTR_UNIQUE_MEMBER);
        }
      }
      else if (entry.hasObjectClass(OC_GROUP_OF_URLS))
      {
        // Determine the dynamic group members, their 'type' and their
        // resource ID.
        if(entry.hasAttribute(ATTR_MEMBER_URL))
        {
          final String[] memberURLs = entry.getAttributeValues(ATTR_MEMBER_URL);
          for(String url : memberURLs)
          {
            final LDAPURL ldapURL = new LDAPURL(url);
            final SearchRequest searchRequest =
                new SearchRequest(ldapURL.getBaseDN().toString(),
                                  SearchScope.SUB, ldapURL.getFilter(),
                                  attrsToGet);
            SearchResult searchResult = ldapInterface.search(searchRequest);

            if (searchResult.getEntryCount() > 0)
            {
              for(SearchResultEntry rEntry : searchResult.getSearchEntries())
              {
                final SCIMAttributeValue v =
                    createMemberValue(groupResolver, rEntry);
                if (v != null)
                {
                  values.add(v);
                }
              }
            }
          }
        }
      }

      // Determine the 'type' and resource ID for static and virtual static
      // groups.
      if (members != null)
      {
        for (final String memberDN : members)
        {
          if ((userResolver != null &&
               userResolver.isDnInScope(memberDN)) ||
              groupResolver.isDnInScope(memberDN))
          {
            final SearchRequest searchRequest =
                new SearchRequest(memberDN, SearchScope.BASE,
                                  "(objectclass=*)", attrsToGet);
            SearchResult searchResult = ldapInterface.search(searchRequest);

            if(searchResult.getEntryCount() == 1)
            {
              final SearchResultEntry rEntry =
                  searchResult.getSearchEntries().get(0);
              final SCIMAttributeValue v =
                  createMemberValue(groupResolver, rEntry);
              if (v != null)
              {
                values.add(v);
              }
            }
          }
        }
      }
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      throw ResourceMapper.toSCIMException(
          "Error searching for values of the members attribute: " +
          StaticUtils.getExceptionMessage(e), e);
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
      if(o instanceof LDAPSearchResolver)
      {
        userResolver = (LDAPSearchResolver) o;
      }
    }
  }



  @Override
  public AttributeDescriptor getAttributeDescriptor()
  {
    return descriptor;
  }



  /**
   * Create a SCIM value for a group member.
   *
   * @param groupResolver  The group resolver.
   * @param entry          The member entry.
   *
   * @return  The member value created, or {@code null} if the member entry
   *          is not a SCIM group or user resource.
   *
   * @throws  SCIMException  If the attribute descriptor for the derived
   *                         attribute is missing a sub-attribute.
   */
  protected SCIMAttributeValue createMemberValue(
      final LDAPSearchResolver groupResolver,
      final Entry entry)
      throws SCIMException
  {
    List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>(2);

    if (userResolver != null)
    {
      if(userResolver.isResourceEntry(entry))
      {
        final String resourceID =
            userResolver.getIdFromEntry(entry);
        subAttributes.add(SCIMAttribute.create(
                getAttributeDescriptor().getSubAttribute("type"),
                   SCIMAttributeValue.createStringValue("User")));
        subAttributes.add(SCIMAttribute.create(
            getAttributeDescriptor().getSubAttribute("value"),
            SCIMAttributeValue.createStringValue(resourceID)));
        return SCIMAttributeValue.createComplexValue(subAttributes);
      }
    }

    if(groupResolver.isResourceEntry(entry))
    {
      final String resourceID =
          groupResolver.getIdFromEntry(entry);
      subAttributes.add(SCIMAttribute.create(
              getAttributeDescriptor().getSubAttribute("type"),
                 SCIMAttributeValue.createStringValue("Group")));
      subAttributes.add(SCIMAttribute.create(
          getAttributeDescriptor().getSubAttribute("value"),
             SCIMAttributeValue.createStringValue(resourceID)));
      return SCIMAttributeValue.createComplexValue(subAttributes);
    }

    //This group member is not within any SCIM scope
    if(Debug.debugEnabled())
    {
      Debug.debug(Level.INFO, DebugType.OTHER,
                  "Skipping group member '" + entry.getDN() +
                  "' for group '" + entry.getDN() +
                  "' because it is not within the scope of the " +
                  "SCIM User or Group resources.");
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
                               final LDAPSearchResolver groupResolver)
      throws InvalidResourceException
  {
    final SCIMAttribute scimAttribute =
        scimObject.getAttribute(getAttributeDescriptor().getSchema(),
                                getAttributeDescriptor().getName());
    if (scimAttribute != null)
    {
      for (SCIMAttributeValue v : scimAttribute.getValues())
      {
        String type = null;
        final SCIMAttribute typeAttr = v.getAttribute("type");
        if (typeAttr != null)
        {
          type = typeAttr.getValue().getStringValue();
        }

        final String resourceID =
            v.getAttribute("value").getValue().getStringValue();

        // Determine the DN for this member.
        try
        {
          String dn = null;
          if (type == null)
          {
            if (userResolver != null)
            {
              try
              {
                dn = userResolver.getDnFromId(ldapInterface, resourceID);
              }
              catch (ResourceNotFoundException e)
              {
                // That's OK. It might be a group.
              }
            }

            if (dn == null)
            {
              dn = groupResolver.getDnFromId(ldapInterface, resourceID);
            }
          }
          else if (type.equalsIgnoreCase("User"))
          {
            dn = userResolver.getDnFromId(ldapInterface, resourceID);
          }
          else if (type.equalsIgnoreCase("Group"))
          {
            dn = groupResolver.getDnFromId(ldapInterface, resourceID);
          }
          else
          {
            throw new InvalidResourceException(
                "Group member type '" + type + " is not valid. Member values " +
                "must be of type 'User' or 'Group'");
          }

          attributes.add(new Attribute(ATTR_UNIQUE_MEMBER, dn));
        }
        catch (Exception e)
        {
          Debug.debugException(e);
          throw new InvalidResourceException(e.getMessage());
        }
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

    return Collections.singleton(ATTR_UNIQUE_MEMBER);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Filter toLDAPFilter(final SCIMFilter filter,
                             final LDAPRequestInterface ldapInterface,
                             final LDAPSearchResolver groupResolver)
      throws InvalidResourceException
  {
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
          String dn = null;
          if (userResolver != null)
          {
            try
            {
              dn = userResolver.getDnFromId(ldapInterface, filterValue);
            }
            catch (ResourceNotFoundException e)
            {
              // That's OK. It might be a group.
            }
          }

          if (dn == null)
          {
            try
            {
              dn = groupResolver.getDnFromId(ldapInterface, filterValue);
            }
            catch (ResourceNotFoundException e)
            {
              // Value is not a valid user or group. Will not match anything.
              return null;
            }
          }
          final List<Filter> filters = new ArrayList<Filter>(2);
          filters.add(Filter.createEqualityFilter(ATTR_MEMBER, dn));
          filters.add(Filter.createEqualityFilter(ATTR_UNIQUE_MEMBER, dn));
          filters.add(Filter.createPresenceFilter(ATTR_MEMBER_URL));
          return Filter.createORFilter(filters);
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
