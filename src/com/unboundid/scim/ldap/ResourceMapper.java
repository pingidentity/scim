/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;

import java.util.List;



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
   * Indicates whether this mapper supports creation of new resources through
   * {@link #toLDAPEntry(com.unboundid.scim.sdk.SCIMObject, String)}.
   *
   * @return  {@code true} if this mapper supports resource creation.
   */
  public abstract boolean supportsCreate();



  /**
   * Construct an LDAP entry from the provided SCIM object.
   *
   * @param scimObject  The SCIM object to form the contents of the entry.
   * @param baseDN      The base DN below which all entries are created.
   *
   * @return  An LDAP entry.
   *
   * @throws LDAPException  If the entry could not be constructed.
   */
  public abstract Entry toLDAPEntry(final SCIMObject scimObject,
                                    final String baseDN)
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
   * Map the attributes in an LDAP entry to SCIM attributes.
   *
   * @param resourceName     The name of the resource that is being mapped.
   * @param entry            The LDAP entry containing attributes to be
   *                         mapped.
   * @param queryAttributes  The set of SCIM attributes that are requested
   *                         to be returned.
   *
   * @return  A list of SCIM attributes mapped from the LDAP entry. This should
   *          never be {@code null} but may be empty.
   */
  public abstract List<SCIMAttribute> toSCIMAttributes(
      final String resourceName,
      final Entry entry, final SCIMQueryAttributes queryAttributes);
}
