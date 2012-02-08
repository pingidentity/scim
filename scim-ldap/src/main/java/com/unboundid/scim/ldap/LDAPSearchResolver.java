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

import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.SCIMException;

import java.util.Collection;
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
  private final DN baseDN;


  /**
   * Create a new instance of LDAPSearchResolver.
   *
   * @param ldapSearchParameters  The LDAP search parameters.
   *
   * @throws LDAPException  If the filter property is not a valid filter, or
   *                        the base DN is not a valid DN.
   */
  public LDAPSearchResolver(final LDAPSearchParameters ldapSearchParameters)
      throws LDAPException
  {
    this.ldapSearchParameters = ldapSearchParameters;
    this.filter = Filter.create(ldapSearchParameters.getFilter());
    this.baseDN = new DN(ldapSearchParameters.getBaseDN().trim());
  }



  /**
   * Retrieves the value of the baseDN property.
   *
   * @return  The value of the baseDN property.
   */
  public String getBaseDN()
  {
    return baseDN.toString();
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
      return baseDN.isAncestorOf(dn, true);
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
   * @param attributes     The requested LDAP attributes.
   *
   * @return  The LDAP entry for the given resource ID.
   *
   * @throws ResourceNotFoundException  If the resource ID was not found.
   */
  public Entry getEntry(final LDAPRequestInterface ldapInterface,
                        final String resourceID,
                        final String... attributes)
      throws ResourceNotFoundException
  {
    Entry entry = null;

    try
    {
      if (idMapsToDn())
      {
        if (isDnInScope(resourceID))
        {
          final SearchRequest searchRequest =
              new SearchRequest(resourceID, SearchScope.BASE,
                                getFilter(), attributes);
          searchRequest.setSizeLimit(1);
          entry = ldapInterface.searchForEntry(searchRequest);
        }
      }
      else
      {
        final Filter compoundFilter =
            Filter.createANDFilter(
                Filter.createEqualityFilter(getIdAttribute(),
                                            resourceID),
                getFilter());
        final SearchRequest searchRequest =
            new SearchRequest(getBaseDN(), SearchScope.SUB,
                              compoundFilter, attributes);
        searchRequest.setSizeLimit(1);
        entry = ldapInterface.searchForEntry(searchRequest);
      }
    }
    catch (LDAPSearchException e)
    {
      Debug.debugException(e);
      throw new ResourceNotFoundException(
          "Resource '" + resourceID + "' not found: " + e.getMessage());
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
   * @throws SCIMException  If the resource ID was not found.
   * @throws LDAPException  If an LDAP exception is thrown.
   */
  public String getDnFromId(final LDAPRequestInterface ldapInterface,
                            final String resourceID)
      throws SCIMException, LDAPException
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
      final Filter compoundFilter =
          Filter.createANDFilter(
              Filter.createEqualityFilter(getIdAttribute(),
                                          resourceID),
              getFilter());
      final SearchRequest searchRequest =
          new SearchRequest(getBaseDN(), SearchScope.SUB,
                            compoundFilter, getIdAttribute());
      searchRequest.setSizeLimit(1);
      final Entry entry = ldapInterface.searchForEntry(searchRequest);
      if (entry != null)
      {
        dn = entry.getDN();
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
   * @throws InvalidResourceException  If the DN does not represent a resource.
   */
  public String getIdFromDn(final LDAPRequestInterface ldapInterface,
                            final String dn)
      throws InvalidResourceException
  {
    try
    {
      if (idMapsToDn())
      {
        return getIdFromEntry(new Entry(dn));
      }
      else
      {
        final SearchRequest searchRequest =
            new SearchRequest(dn, SearchScope.BASE,
                              getFilter(), getIdAttribute());
        searchRequest.setSizeLimit(1);
        final Entry entry = ldapInterface.searchForEntry(searchRequest);
        if (entry != null)
        {
          return getIdFromEntry(entry);
        }
      }
    }
    catch (LDAPSearchException e)
    {
      Debug.debugException(e);
      throw new InvalidResourceException(
          "Error searching for resource with DN '" + dn + "'");
    }

    throw new InvalidResourceException(
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
