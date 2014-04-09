/*
 * Copyright 2011-2014 UnboundID Corp.
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
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SimpleValue;
import com.unboundid.util.ByteString;
import com.unboundid.util.StaticUtils;

import java.text.ParseException;
import java.util.Date;



/**
 * A transformation for LDAP GeneralizedTime syntax. SCIM XML DateTime values
 * are converted to GeneralizedTime and vice-versa.
 */
public class GeneralizedTimeTransformation extends Transformation
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
      case DATETIME:
        final String generalizedTime = byteString.stringValue();
        try
        {
          final Date date = StaticUtils.decodeGeneralizedTime(generalizedTime);
          return SCIMAttributeValue.createDateValue(date);
        }
        catch (ParseException e)
        {
          Debug.debugException(e);
          throw new RuntimeException(
              "Error in transformation from LDAP generalized time value: " +
              e.getMessage());
        }

      case STRING:
      case BOOLEAN:
      case DECIMAL:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The generalized time transformation can not be used on " +
            descriptor.getDataType() + " data");
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
      case DATETIME:
        final Date date = value.getDateValue();
        return new ASN1OctetString(StaticUtils.encodeGeneralizedTime(date));

      case STRING:
      case BOOLEAN:
      case DECIMAL:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The generalized time transformation can not be used on " +
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
    return StaticUtils.encodeGeneralizedTime(simpleValue.getDateValue());
  }
}
