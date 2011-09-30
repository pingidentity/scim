/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.sdk.SimpleValue;
import com.unboundid.util.ByteString;



/**
 * A transformation that transforms SCIM values to LDAP values and vice-versa
 * without altering the values.
 */
public class DefaultTransformation extends Transformation
{
  /**
   * {@inheritDoc}
   */
  @Override
  public SimpleValue toSCIMValue(final AttributeDescriptor descriptor,
                                 final ByteString byteString)
  {
    switch (descriptor.getDataType())
    {
      case BINARY:
        return new SimpleValue(byteString.getValue());

      case DATETIME:
      case STRING:
      case BOOLEAN:
      case INTEGER:
      default:
        return new SimpleValue(byteString.stringValue());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ASN1OctetString toLDAPValue(final AttributeDescriptor descriptor,
                                     final SimpleValue simpleValue)
  {
    switch (descriptor.getDataType())
    {
      case BINARY:
        return new ASN1OctetString(simpleValue.getBinaryValue());

      case DATETIME:
      case STRING:
      case BOOLEAN:
      case INTEGER:
      default:
        return new ASN1OctetString(simpleValue.getStringValue());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toLDAPFilterValue(final String scimFilterValue)
  {
    return scimFilterValue;
  }
}
