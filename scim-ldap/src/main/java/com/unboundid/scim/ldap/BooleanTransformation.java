/*
 * Copyright 2011-2012 UnboundID Corp.
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
      case DECIMAL:
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
      case DECIMAL:
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
