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

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SortParameters;

import java.util.Collection;
import java.util.List;
import java.util.Set;



/**
 * This abstract class defines an interface that may be implemented to
 * perform a mapping between SCIM objects and LDAP entries.
 */
public abstract class ResourceMapper
{
  /**
   * Create a new instance of this resource mapper. All resource mappers must
   * provide a default constructor, but any initialization should be done
   * in the {@code initializeMapper()} method.
   */
  protected ResourceMapper()
  {
    // No implementation required.
  }



  /**
   * Initialize this resource mapper from the provided information.
   *
   * @param resourceDescriptor The ResourceDescriptor of the SCIM resource
   *                           handled by this resource mapper.
   * @param searchParameters   The LDAP Search parameters, or {@code null} if
   *                           there are none defined.
   * @param addParameters      The LDAP Add parameters, or {@code null} if there
   *                           are none defined.
   * @param mappers            The attribute mappers for this resource mapper.
   * @param derivedAttributes  The derived attributes for this resource mapper.
   */
  public abstract void initializeMapper(
      final ResourceDescriptor resourceDescriptor,
      final LDAPSearchParameters searchParameters,
      final LDAPAddParameters addParameters,
      final Collection<AttributeMapper> mappers,
      final Collection<DerivedAttribute> derivedAttributes);



  /**
   * Performs any cleanup which may be necessary when this resource mapper is
   * to be taken out of service.
   */
  public abstract void finalizeMapper();



  /**
   * Retrieve the ResourceDescriptor of the SCIM resource handled by this
   * resource mapper.
   *
   * @return  The ResourceDescriptor of the SCIM resource handled by this
   *          resource mapper.
   */
  public abstract ResourceDescriptor getResourceDescriptor();



  /**
   * Indicates whether this mapper supports querying of resources.
   *
   * @return  {@code true} if this mapper supports resource query.
   */
  public abstract boolean supportsQuery();



  /**
   * Indicates whether this mapper supports creation of new resources through
   * {@link #toLDAPEntry(com.unboundid.scim.sdk.SCIMObject)}.
   *
   * @return  {@code true} if this mapper supports resource creation.
   */
  public abstract boolean supportsCreate();



  /**
   * Retrieve the set of LDAP attribute types that should be requested in order
   * to return the specified query attributes.
   *
   * @param queryAttributes  The requested query attributes.
   *
   * @return  The set of LDAP attribute types that should be requested.
   */
  public abstract Set<String> toLDAPAttributeTypes(
      final SCIMQueryAttributes queryAttributes);



  /**
   * Construct an LDAP entry from the provided SCIM object.
   *
   * @param scimObject  The SCIM object to form the contents of the entry.
   *
   * @return  An LDAP entry.
   *
   * @throws LDAPException  If the entry could not be constructed.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public abstract Entry toLDAPEntry(final SCIMObject scimObject)
      throws LDAPException, InvalidResourceException;




  /**
   * Map the attributes in a SCIM object to LDAP attributes.
   *
   * @param scimObject  The object containing attributes to be mapped.
   *
   * @return  A list of LDAP attributes mapped from the SCIM object. This should
   *          never be {@code null} but may be empty.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public abstract List<Attribute> toLDAPAttributes(final SCIMObject scimObject)
      throws InvalidResourceException;



  /**
   * Map the replacement attributes in a SCIM object to LDAP modifications.
   *
   * @param currentEntry  The current LDAP entry representing the SCIM object.
   * @param scimObject  The object containing attributes to be mapped.
   *
   * @return  A list of LDAP modifications mapped from the SCIM object. This
   *          should never be {@code null} but may be empty.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public abstract List<Modification> toLDAPModifications(
      final Entry currentEntry,
      final SCIMObject scimObject) throws InvalidResourceException;



  /**
   * Map the provided SCIM filter to an LDAP filter.
   *
   * @param filter  The SCIM filter to be mapped, or {@code null} if no filter
   *                parameters were provided.
   *
   * @return  An LDAP filter.
   */
  public abstract Filter toLDAPFilter(final SCIMFilter filter);



  /**
   * Get the search base DN to be used for querying.
   *
   * @return  The search base DN to be used for querying.
   */
  public abstract String getSearchBaseDN();



  /**
   * Map the provided SCIM sort parameters to an LDAP sort control.
   *
   * @param sortParameters  The SCIM sort parameters to be mapped.
   *
   * @return  An LDAP sort control.
   */
  public abstract Control toLDAPSortControl(
      final SortParameters sortParameters);



  /**
   * Map the attributes in an LDAP entry to SCIM attributes.
   *
   * @param entry            The LDAP entry containing attributes to be
   *                         mapped.
   * @param queryAttributes  The set of SCIM attributes that are requested
   *                         to be returned.
   * @param ldapInterface    An optional LDAP interface that can be used to
   *                         derive attributes from other entries.
   *
   * @return  A list of SCIM attributes mapped from the LDAP entry. This should
   *          never be {@code null} but may be empty.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public abstract List<SCIMAttribute> toSCIMAttributes(
      final Entry entry, final SCIMQueryAttributes queryAttributes,
      final LDAPInterface ldapInterface) throws InvalidResourceException;



  /**
   * Map an LDAP entry to a SCIM resource.
   *
   * @param entry            The LDAP entry containing attributes to be
   *                         mapped.
   * @param queryAttributes  The set of SCIM attributes that are requested
   *                         to be returned.
   * @param ldapInterface    An optional LDAP interface that can be used to
   *                         derive attributes from other entries.
   *
   * @return  A SCIM object mapped from the LDAP entry, or {@code null} if this
   *          entry cannot be mapped to a SCIM object.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public abstract SCIMObject toSCIMObject(
      final Entry entry, final SCIMQueryAttributes queryAttributes,
      final LDAPInterface ldapInterface) throws InvalidResourceException;



  /**
   * Create a resource mapper from the name of a class that extends the
   * {@code ResourceMapper} abstract class.
   *
   * @param className  The name of a class that extends {@code ResourceMapper}.
   *
   * @return  A new instance of the ResourceMapper class.
   */
  public static ResourceMapper create(final String className)
  {
    if (className == null)
    {
      return new ConfigurableResourceMapper();
    }

    Class clazz;
    try
    {
      clazz = Class.forName(className);
    }
    catch (ClassNotFoundException e)
    {
      Debug.debugException(e);
      throw new IllegalArgumentException(
          "Class '" + className + "' not found", e);
    }

    final Object object;
    try
    {
      object = clazz.newInstance();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new IllegalArgumentException(
          "Cannot create instance of class '" + className + "'", e);
    }

    if (!(object instanceof ResourceMapper))
    {
      throw new IllegalArgumentException(
          "Class '" + className + "' is not a ResourceMapper");
    }

    return (ResourceMapper)object;
  }
}
