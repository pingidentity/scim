/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.Collection;
import java.util.Set;



/**
 * This abstract class defines an interface that may be implemented to perform
 * a mapping between a SCIM attribute and LDAP attributes. There may be several
 * attribute mapper instances defined for a single SCIM attribute type, in order
 * to support, for example, different mappings for work addresses and home
 * addresses.
 */
public abstract class AttributeMapper
{
  /**
   * The SCIM attribute type that is mapped by this attribute mapper.
   */
  private SCIMAttributeType scimAttributeType;



  /**
   * Create a new instance of an attribute mapper.
   *
   * @param scimAttributeType  The SCIM attribute type that is mapped by this
   *                           attribute mapper.
   */
  protected AttributeMapper(final SCIMAttributeType scimAttributeType)
  {
    this.scimAttributeType = scimAttributeType;
  }



  /**
   * Retrieve the SCIM attribute type that is mapped by this attribute mapper.
   *
   * @return  The SCIM attribute type that is mapped by this attribute mapper.
   */
  public SCIMAttributeType getSCIMAttributeType()
  {
    return scimAttributeType;
  }



  /**
   * Retrieve the set of LDAP attribute types used by this attribute mapper.
   *
   * @return  The set of LDAP attribute types used by this attribute mapper.
   */
  public abstract Set<String> getLDAPAttributeTypes();



  /**
   * Map the provided SCIM filter to an LDAP filter.
   *
   * @param filter  The SCIM filter to be mapped. The filter identifies the
   *                SCIM attribute that is mapped by this attribute mapper,
   *                or one of its sub-attributes.
   *
   * @return  An LDAP filter.
   */
  public abstract Filter toLDAPFilter(final SCIMFilter filter);



  /**
   * Maps this SCIM attribute type to an LDAP attribute type that should be
   * used as a sort key when the SCIM attribute type has been specified in
   * sort parameters.
   *
   * @return  The LDAP attribute type that should be used as a sort key when
   *          the SCIM attribute type has been specified in sort parameters.
   *          The method returns {@code null} if the SCIM attribute type
   *          cannot be used in sort parameters.
   */
  public abstract String toLDAPSortAttributeType();



  /**
   * Map the SCIM attribute in the provided SCIM object to LDAP attributes.
   *
   * @param scimObject  The SCIM object containing the attribute to be mapped.
   * @param attributes  A collection of LDAP attributes to hold any attributes
   *                    created by this mapping.
   */
  public abstract void toLDAPAttributes(
      final SCIMObject scimObject,
      final Collection<Attribute> attributes);



  /**
   * Map the LDAP attributes in the provided LDAP entry to a SCIM attribute.
   *
   * @param entry  The LDAP entry whose attributes are to be mapped.
   *
   * @return  A SCIM attribute, or {@code null} if no attribute was created.
   */
  public abstract SCIMAttribute toSCIMAttribute(final Entry entry);
}
