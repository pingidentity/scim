/*
 * Copyright 2012-2013 UnboundID Corp.
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

import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.util.StaticUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;



/**
 * This class wraps LDAPSearchParameters and provides additional helper methods
 * for ID mapping.
 */
public class LDAPSearchResolver
{
  private final LDAPSearchParameters ldapSearchParameters;
  private final Filter filter;
  private final Set<DN> baseDNs;
  private final Set<DN> excludeBaseDNs;

  /**
   * Create a new instance of LDAPSearchResolver.
   *
   * @param ldapSearchParameters  The LDAP search parameters.
   * @param excludeBaseDNs The base DNs that should be excluded.
   *
   * @throws LDAPException  If the filter property is not a valid filter, or
   *                        the base DN is not a valid DN.
   */
  public LDAPSearchResolver(final LDAPSearchParameters ldapSearchParameters,
                            final Set<DN> excludeBaseDNs)
      throws LDAPException
  {
    this.ldapSearchParameters = ldapSearchParameters;
    this.filter = Filter.create(ldapSearchParameters.getFilter());
    Set<DN> dnSet = new HashSet<DN>();
    for (String dn : ldapSearchParameters.getBaseDN())
    {
      dnSet.add(new DN(dn));
    }
    this.baseDNs = Collections.unmodifiableSet(dnSet);
    this.excludeBaseDNs = Collections.unmodifiableSet(excludeBaseDNs);
  }



  /**
   * Retrieves the value of the baseDN property.
   *
   * @return  The value of the baseDN property.
   */
  public Set<DN> getBaseDNs()
  {
    return baseDNs;
  }



  /**
   * Retrieves the value of the filter property.
   *
   * @return  The value of the filter property.
   */
  public String getFilterString()
  {
    return ldapSearchParameters.getFilter().trim();
  }



  /**
   * Retrieves the value of the filter property as an LDAP SDK Filter.
   *
   * @return  The value of the filter property as an LDAP SDK Filter.
   */
  public Filter getFilter()
  {
    return filter;
  }



  /**
   * Determines whether the SCIM resource ID maps to the LDAP DN.
   *
   * @return  {@code true} if the SCIM resource ID maps to the LDAP DN or
   *          {@code false} if the ID maps to some other attribute.
   */
  public boolean idMapsToDn()
  {
    return ldapSearchParameters.getResourceIDMapping() == null;
  }



  /**
   * Returns the LDAP attribute that the SCIM resource ID maps to.
   *
   * @return  The LDAP attribute that the SCIM resource ID maps to, or
   *          {@code null} if the SCIM ID maps to the LDAP DN.
   */
  public String getIdAttribute()
  {
    if (ldapSearchParameters.getResourceIDMapping() == null)
    {
      return null;
    }

    return ldapSearchParameters.getResourceIDMapping().getLdapAttribute();
  }



  /**
   * Retrieve a resource ID from an LDAP entry.
   *
   * @param entry  The LDAP entry, which must contain a value for the
   *               resource ID attribute unless the resource ID maps to the
   *               LDAP DN.
   *
   * @return  The resource ID of the entry.
   *
   * @throws InvalidResourceException  If the resource ID could not be
   *                                   determined.
   */
  public String getIdFromEntry(final Entry entry)
      throws InvalidResourceException
  {
    if (idMapsToDn())
    {
      try
      {
        // We are obliged to normalize the DN because the DS does so for
        // group entries.
        return entry.getParsedDN().toNormalizedString();
      }
      catch (LDAPException e)
      {
        Debug.debugException(e);
        return entry.getDN();
      }
    }
    else
    {
      final String idAttribute = getIdAttribute();
      if (!entry.hasAttribute(idAttribute))
      {
        throw new InvalidResourceException(
            "Unable to determine a resource ID for entry '" + entry.getDN() +
            "' because it does not have a value for the '" + idAttribute +
            "' attribute");
      }
      return entry.getAttributeValue(idAttribute);
    }
  }



  /**
   * Adds the ID attribute to the provided collection.
   *
   * @param attributes  The collection of attributes to add to.
   */
  public void addIdAttribute(final Collection<String> attributes)
  {
    if (!idMapsToDn())
    {
      attributes.add(getIdAttribute());
    }
  }



  /**
   * Returns a set of all the attributes used by the resolver, including the
   * filter attributes and the ID attribute.
   *
   * @return a set of attribute names
   */
  public Set<String> getFilterAndIdAttributes()
  {
    Set<String> attributes = getAllFilterAttributes(filter);

    addIdAttribute(attributes);

    return attributes;
  }



  /**
   * Returns a set of all the attributes used in the given filter. This method
   * invokes itself recursively if the given filter is a compound (AND/OR)
   * filter.
   *
   * @param filter the Filter whose attribute names to get
   * @return a set of attribute names
   */
  private static Set<String> getAllFilterAttributes(final Filter filter)
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



  /**
   * Determine whether the provided DN could be a resource entry that
   * satisfies the criteria for this resolver.
   *
   * @param dn  The DN for which to make the determination.
   *
   * @return  {@code true} if the LDAP entry satisfies the criteria for this
   *          resolver.
   */
  public boolean isDnInScope(final String dn)
  {
    try
    {
      for (DN baseDN : baseDNs)
      {
        if (baseDN.isAncestorOf(dn, true))
        {
          for (DN excludeBaseDN : excludeBaseDNs)
          {
            if (excludeBaseDN.isAncestorOf(dn, true))
            {
              return false;
            }
          }
          return true;
        }
      }
      return false;
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      return false;
    }
  }



  /**
   * Determine whether the provided LDAP entry is a resource entry that
   * satisfies the criteria for this resolver.
   *
   * @param entry  The LDAP entry for which to make the determination.
   *
   * @return  {@code true} if the LDAP entry satisfies the criteria for this
   *          resolver.
   */
  public boolean isResourceEntry(final Entry entry)
  {
    try
    {
      return isDnInScope(entry.getDN()) &&
             filter.matchesEntry(entry) &&
             (idMapsToDn() || entry.hasAttribute(getIdAttribute()));
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      return false;
    }
  }



  /**
   * Process a resource entry before it is added.
   *
   * @param entry  The entry to be added.
   *
   * @return  The processed entry.
   *
   * @throws InvalidResourceException  If the entry is unacceptable.
   */
  public Entry preProcessAddEntry(final Entry entry)
      throws InvalidResourceException
  {
    if (!idMapsToDn() &&
        ldapSearchParameters.getResourceIDMapping().getCreatedBy() ==
        CreatedBy.SCIM_CONSUMER &&
        !entry.hasAttribute(getIdAttribute()))
    {
      throw new InvalidResourceException(
          "The entry to be added does not have a value for the '" +
          getIdAttribute() + "' LDAP attribute, which is required for the " +
          "SCIM resource ID");
    }

    return entry;
  }



  /**
   * Read the LDAP entry identified by the given resource ID.
   *
   * @param ldapInterface  The LDAP interface to use to read the entry.
   * @param resourceID     The requested SCIM resource ID.
   * @param controls       A set of search controls, which may be empty.
   * @param attributes     The requested LDAP attributes.
   *
   * @return  The LDAP entry for the given resource ID.
   *
   * @throws SCIMException  If there was an error retrieving the resource entry.
   */
  public SearchResultEntry getEntry(final LDAPRequestInterface ldapInterface,
                                    final String resourceID,
                                    final List<Control> controls,
                                    final String... attributes)
      throws SCIMException
  {
    SearchResultEntry entry = null;

    if (idMapsToDn())
    {
      if (isDnInScope(resourceID))
      {
        try
        {
          final SearchRequest searchRequest =
              new SearchRequest(resourceID, SearchScope.BASE,
                  getFilter(), attributes);
          searchRequest.setSizeLimit(1);
          searchRequest.addControls(
              controls.toArray(new Control[controls.size()]));
          entry = ldapInterface.searchForEntry(searchRequest);
        }
        catch (LDAPSearchException e)
        {
          Debug.debugException(e);
          throw ResourceMapper.toSCIMException(
              "Error searching for resource '" + resourceID + "': " +
                  StaticUtils.getExceptionMessage(e), e);
        }
      }
    }
    else
    {
      final Filter compoundFilter = Filter.createANDFilter(
               Filter.createEqualityFilter(getIdAttribute(), resourceID),
                 getFilter());

      for (DN baseDN : baseDNs)
      {
        try
        {
          final SearchRequest searchRequest =
              new SearchRequest(baseDN.toString(), SearchScope.SUB,
                      compoundFilter, attributes);
          searchRequest.setSizeLimit(1);
          searchRequest.addControls(
              controls.toArray(new Control[controls.size()]));
          entry = ldapInterface.searchForEntry(searchRequest);

          if (entry != null)
          {
            for (DN excludeBaseDN : excludeBaseDNs)
            {
              if (excludeBaseDN.isAncestorOf(entry.getParsedDN(), true))
              {
                entry = null;
                break;
              }
            }
            if(entry != null)
            {
              break;
            }
          }
        }
        catch (LDAPException e)
        {
          Debug.debugException(e);
          if(e.getResultCode() != ResultCode.INVALID_ATTRIBUTE_SYNTAX)
          {
            throw ResourceMapper.toSCIMException(
                "Error searching for resource '" + resourceID + "': " +
                    StaticUtils.getExceptionMessage(e), e);
          }
          // This is likely if the provided resource ID value violates
          // the mapped LDAP attribute's syntax. This should map to 404
          // instead of 400 since SCIM treats the resource ID as an opaque
          // value and shouldn't enforce any syntax on it.
          entry = null;
        }
      }
    }

    if (entry == null)
    {
      throw new ResourceNotFoundException(
          "Resource '" + resourceID + "' not found");
    }

    return entry;
  }



  /**
   * Determine the DN of the LDAP entry identified by the given resource ID.
   *
   * @param ldapInterface  The LDAP interface to use to read the entry.
   * @param resourceID     The requested SCIM resource ID.
   *
   * @return  The LDAP DN for the given resource ID.
   *
   * @throws SCIMException  If there was an error determining the resource DN.
   */
  public String getDnFromId(final LDAPRequestInterface ldapInterface,
                            final String resourceID)
      throws SCIMException
  {
    String dn = null;

    if (idMapsToDn())
    {
      if (isDnInScope(resourceID))
      {
        dn = resourceID;
      }
    }
    else
    {
      final Filter compoundFilter = Filter.createANDFilter(
              Filter.createEqualityFilter(getIdAttribute(), resourceID),
              getFilter());

      for (DN baseDN : baseDNs)
      {
        try
        {
          final SearchRequest searchRequest =
             new SearchRequest(baseDN.toString(), SearchScope.SUB,
                  compoundFilter, getIdAttribute());
          searchRequest.setSizeLimit(1);
          Entry entry = ldapInterface.searchForEntry(searchRequest);
          if (entry != null)
          {
            dn = entry.getDN();
            break;
          }
        }
        catch(LDAPSearchException e)
        {
          Debug.debugException(e);
          if(e.getResultCode() != ResultCode.INVALID_ATTRIBUTE_SYNTAX)
          {
            throw ResourceMapper.toSCIMException(
                "Error searching for resource '" + resourceID + "': " +
                   StaticUtils.getExceptionMessage(e), e);
          }
          // This is likely if the provided resource ID value violates
          // the mapped LDAP attribute's syntax. This should map to 404
          // instead of 400 since SCIM treats the resource ID as an opaque
          // value and shouldn't enforce any syntax on it.
        }
      }
    }

    if (dn == null)
    {
      throw new ResourceNotFoundException(
          "Resource '" + resourceID + "' not found");
    }

    return dn;
  }



  /**
   * Determine the resource ID of the resource identified by the given DN.
   *
   * @param ldapInterface  The LDAP interface to use to read the entry if
   *                       necessary.
   * @param dn             The DN of the requested resource.
   *
   * @return  The resource ID for the given DN.
   *
   * @throws SCIMException  If there was an error determining the resource ID.
   */
  public String getIdFromDn(final LDAPRequestInterface ldapInterface,
                            final String dn)
      throws SCIMException
  {
    if (idMapsToDn())
    {
      return getIdFromEntry(new Entry(dn));
    }
    else
    {
      final Entry entry;
      try
      {
        final SearchRequest searchRequest =
            new SearchRequest(dn, SearchScope.BASE,
                getFilter(), getIdAttribute());
        searchRequest.setSizeLimit(1);
        entry = ldapInterface.searchForEntry(searchRequest);
      }
      catch (LDAPSearchException e)
      {
        Debug.debugException(e);
        throw ResourceMapper.toSCIMException(
            "Error searching for resource with DN '" + dn + "': " +
                StaticUtils.getExceptionMessage(e), e);
      }
      if (entry != null)
      {
        return getIdFromEntry(entry);
      }
    }

    throw new ResourceNotFoundException(
        "Resource with DN '" + dn + "' not found");
  }



  /**
   * Retrieve an attribute mapper for the id attribute.
   *
   * @return  An attribute mapper for the id attribute, or {@code null} if the
   *          id maps to the DN.
   */
  public AttributeMapper getIdAttributeMapper()
  {
    if (idMapsToDn())
    {
      return null;
    }
    else
    {
      final AttributeTransformation attributeTransformation =
          AttributeTransformation.create(
              ldapSearchParameters.getResourceIDMapping());

      return new SimpleAttributeMapper(CoreSchema.ID_DESCRIPTOR,
                                       attributeTransformation);
    }
  }
}
