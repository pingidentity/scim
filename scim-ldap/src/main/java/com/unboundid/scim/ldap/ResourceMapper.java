/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SortParameters;

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
   * in the {@link #initializeMapper()} method.
   */
  protected ResourceMapper()
  {
    // No implementation required.
  }



  /**
   * Initialize this resource mapper.
   *
   * TODO need a way to provide configuration.
   */
  public abstract void initializeMapper();



  /**
   * Performs any cleanup which may be necessary when this resource mapper is
   * to be taken out of service.
   */
  public abstract void finalizeMapper();



  /**
   * Retrieve the name of the SCIM resource handled by this resource mapper.
   * This is also the resource endpoint in the REST protocol.
   *
   * @return  The name of the SCIM resource handled by this resource mapper.
   */
  public abstract String getResourceName();



  /**
   * Retrieve the query endpoint for resources handled by this resource mapper.
   *
   * @return  The query endpoint for resources handled by this resource mapper.
   */
  public abstract String getQueryEndpoint();



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
   */
  public abstract Entry toLDAPEntry(final SCIMObject scimObject)
      throws LDAPException;




  /**
   * Map the attributes in a SCIM object to LDAP attributes.
   *
   * @param scimObject  The object containing attributes to be mapped.
   *
   * @return  A list of LDAP attributes mapped from the SCIM object. This should
   *          never be {@code null} but may be empty.
   */
  public abstract List<Attribute> toLDAPAttributes(final SCIMObject scimObject);



  /**
   * Map the replacement attributes in a SCIM object to LDAP modifications.
   *
   * @param currentEntry  The current LDAP entry representing the SCIM object.
   * @param scimObject  The object containing attributes to be mapped.
   *
   * @return  A list of LDAP modifications mapped from the SCIM object. This
   *          should never be {@code null} but may be empty.
   */
  public abstract List<Modification> toLDAPModifications(
      final Entry currentEntry,
      final SCIMObject scimObject);



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
   * @param resourceName     The name of the resource that is being mapped.
   * @param entry            The LDAP entry containing attributes to be
   *                         mapped.
   * @param queryAttributes  The set of SCIM attributes that are requested
   *                         to be returned.
   * @param ldapInterface    An optional LDAP interface that can be used to
   *                         derive attributes from other entries.
   *
   * @return  A list of SCIM attributes mapped from the LDAP entry. This should
   *          never be {@code null} but may be empty.
   */
  public abstract List<SCIMAttribute> toSCIMAttributes(
      final String resourceName,
      final Entry entry, final SCIMQueryAttributes queryAttributes,
      final LDAPInterface ldapInterface);



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
   */
  public abstract SCIMObject toSCIMObject(
      final Entry entry, final SCIMQueryAttributes queryAttributes,
      final LDAPInterface ldapInterface);
}
