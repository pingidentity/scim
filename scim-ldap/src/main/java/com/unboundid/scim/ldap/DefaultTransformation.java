/*
 * Copyright 2011-2024 Ping Identity Corporation
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
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.SCIMAttributeValue;
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
  public SCIMAttributeValue toSCIMValue(final AttributeDescriptor descriptor,
                                        final ByteString byteString)
  {
    switch (descriptor.getDataType())
    {
      case BINARY:
        return SCIMAttributeValue.createBinaryValue(byteString.getValue());

      case DATETIME:
        throw new IllegalArgumentException(
          "The default transformation can not be used on " +
            descriptor.getDataType() + " data");
      case STRING:
      case BOOLEAN:
      case DECIMAL:
      case INTEGER:
      default:
        return SCIMAttributeValue.createStringValue(byteString.stringValue());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ASN1OctetString toLDAPValue(final AttributeDescriptor descriptor,
                                     final SCIMAttributeValue value)
  {
    switch (descriptor.getDataType())
    {
      case BINARY:
        return new ASN1OctetString(value.getBinaryValue());

      case DATETIME:
        throw new IllegalArgumentException(
          "The default transformation can not be used on " +
            descriptor.getDataType() + " data");
      case STRING:
      case BOOLEAN:
      case DECIMAL:
      case INTEGER:
      default:
        return new ASN1OctetString(value.getStringValue());
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
