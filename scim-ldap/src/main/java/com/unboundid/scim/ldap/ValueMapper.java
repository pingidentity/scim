/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMAttributeValue;



/**
 * This class provides a mapping between simple SCIM attribute values and
 * LDAP attribute values. The values are not transformed in any way.
 */
public class ValueMapper
{
  /**
   * The name of the SCIM attribute.
   */
  private final String scimAttribute;

  /**
   * The LDAP attribute type.
   */
  private final String ldapAttributeType;



  /**
   * Create a new instance of a value mapper.
   *
   * @param scimAttribute      The name of the SCIM attribute.
   * @param ldapAttributeType  The LDAP attribute type.
   */
  public ValueMapper(final String scimAttribute,
                     final String ldapAttributeType)
  {
    this.scimAttribute = scimAttribute;
    this.ldapAttributeType = ldapAttributeType;
  }



  /**
   * Retrieve the name of the SCIM attribute.
   *
   * @return  The name of the SCIM attribute.
   */
  public String getScimAttribute()
  {
    return scimAttribute;
  }



  /**
   * Retrieve the LDAP attribute type.
   *
   * @return  The LDAP attribute type.
   */
  public String getLdapAttributeType()
  {
    return ldapAttributeType;
  }



  /**
   * Map the provided SCIM attribute value to an LDAP value.
   *
   * @param scimValue  The SCIM attribute value to be mapped.
   *
   * @return  An LDAP attribute value.
   */
  public String toLDAPValue(final SCIMAttributeValue scimValue)
  {
    return scimValue.getStringValue();
  }



  /**
   * Map the provided SCIM filter value to an LDAP filter value.
   *
   * @param filterValue  The SCIM filter value to be mapped.
   *
   * @return  An LDAP filter value.
   */
  public String toLDAPValue(final String filterValue)
  {
    return filterValue;
  }



  /**
   * Maps the provided LDAP attribute value to a SCIM value.
   *
   * @param ldapValue  The LDAP attribute value to be mapped.
   *
   * @return  A SCIM attribute value.
   */
  public SCIMAttributeValue toSCIMValue(final String ldapValue)
  {
    return SCIMAttributeValue.createStringValue(ldapValue);
  }
}
