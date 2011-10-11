/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.SimpleValue;
import com.unboundid.util.ByteString;



/**
 * A transformation for LDAP Boolean syntax. SCIM XML boolean values
 * are converted to LDAP Boolean and vice-versa.
 */
public class BooleanTransformation extends Transformation
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
      case BOOLEAN:
        final String s = byteString.stringValue();
        final Attribute a = new Attribute("dummy", s);
        return new SimpleValue(a.getValueAsBoolean());

      case DATETIME:
      case STRING:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The boolean transformation can not be used on " +
            descriptor.getDataType() + " data");
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
      case BOOLEAN:
        if (simpleValue.getBooleanValue())
        {
          return new ASN1OctetString("TRUE");
        }
        else
        {
          return new ASN1OctetString("FALSE");
        }

      case DATETIME:
      case STRING:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The boolean transformation can not be used on " +
            descriptor.getDataType() + " data");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toLDAPFilterValue(final String scimFilterValue)
  {
    SimpleValue simpleValue = new SimpleValue(scimFilterValue);
    if (simpleValue.getBooleanValue())
    {
      return "TRUE";
    }
    else
    {
      return "FALSE";
    }
  }
}
