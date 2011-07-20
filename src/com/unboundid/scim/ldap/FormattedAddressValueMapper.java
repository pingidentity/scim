/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMAttributeValue;



/**
 * This class provides a mapping between simple SCIM attribute values and
 * LDAP attribute values for formatted postal addresses. The LDAP value uses
 * '$' as the line separator, and the SCIM value uses '\n'.
 */
public class FormattedAddressValueMapper extends ValueMapper
{
  /**
   * Create a new instance of this formatted address value mapper.
   *
   * @param scimAttribute      The name of the SCIM attribute.
   * @param ldapAttributeType  The LDAP attribute type.
   */
  public FormattedAddressValueMapper(final String scimAttribute,
                                     final String ldapAttributeType)
  {
    super(scimAttribute, ldapAttributeType);
  }



  @Override
  public String toLDAPValue(final SCIMAttributeValue scimValue)
  {
    return scimValue.getStringValue().replaceAll("\n", "\\$");
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
    return filterValue.replaceAll("\n", "\\$");
  }



  @Override
  public SCIMAttributeValue toSCIMValue(final String ldapValue)
  {
    return SCIMAttributeValue.createStringValue(
        ldapValue.replaceAll("\\$", "\n"));
  }
}
