/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
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
   * @param entry            The LDAP entry containing attributes to be
   *                         mapped.
   * @param queryAttributes  The set of SCIM attributes that are requested
   *                         to be returned.
   *
   * @return  A list of SCIM attributes mapped from the LDAP entry. This should
   *          never be {@code null} but may be empty.
   */
  public abstract List<SCIMAttribute> toSCIMAttributes(
      final Entry entry, final SCIMQueryAttributes queryAttributes);
}
