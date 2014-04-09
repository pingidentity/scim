/*
 * Copyright 2012-2014 UnboundID Corp.
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

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DebugType;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.util.ByteString;

import java.util.logging.Level;

/**
 * A transformation for LDAP Telephone Number syntax (E.123) to RFC3966 and
 * vice-versa. The default country will be used to prefix the country code
 * to any local numbers where global uniqueness can not be derived (ie. E.123
 * "local" numbers stored on the LDAP side).
 */
public class TelephoneNumberTransformation extends Transformation
{
  private static final String DEFAULT_COUNTRY_ARG = "defaultCountry";
  private static final String DEFAULT_COUNTRY = "US";
  private static final String LDAP_FORMAT_ARG = "ldapFormat";
  private static final PhoneNumberUtil.PhoneNumberFormat DEFAULT_LDAP_FORMAT =
      PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL;

  private String defaultCountry = null;
  private PhoneNumberUtil.PhoneNumberFormat ldapFormat = null;

  @Override
  public String toLDAPFilterValue(final String scimFilterValue)
  {
    PhoneNumberUtil util = PhoneNumberUtil.getInstance();
    try
    {
      final Phonenumber.PhoneNumber number = util.parse(scimFilterValue,
          getDefaultCountry());
      if(!util.isPossibleNumber(number))
      {
        if(Debug.debugEnabled())
        {
          Debug.debug(Level.WARNING, DebugType.OTHER,
              number.getRawInput() + " doesn't seem to be a valid phone " +
                  "number and will not be canonicalized to LDAP format");
        }
        return scimFilterValue;
      }
      return util.format(number, getLdapFormat());
    }
    catch(NumberParseException e)
    {
      // Don't fail but just pass the string as is to LDAP
      Debug.debugException(e);
      return scimFilterValue;
    }
  }

  @Override
  public SCIMAttributeValue toSCIMValue(final AttributeDescriptor descriptor,
                                        final ByteString byteString)
  {
    PhoneNumberUtil util = PhoneNumberUtil.getInstance();
    switch (descriptor.getDataType())
    {
      case STRING:
        try
        {
          final Phonenumber.PhoneNumber number =
              util.parse(byteString.stringValue(), getDefaultCountry());
          if(!util.isPossibleNumber(number))
          {
            if(Debug.debugEnabled())
            {
              Debug.debug(Level.WARNING, DebugType.OTHER,
                  number.getRawInput() + " doesn't seem to be a valid phone " +
                      "number and will not be canonicalized to RFC3966 format");
            }
            return SCIMAttributeValue.createStringValue(
                byteString.stringValue());
          }
          return SCIMAttributeValue.createStringValue(
              util.format(number, PhoneNumberUtil.PhoneNumberFormat.RFC3966));
        }
        catch(NumberParseException e)
        {
          // Don't fail but just pass the string as is to SCIM.
          Debug.debugException(e);
          return SCIMAttributeValue.createStringValue(byteString.stringValue());
        }

      case DATETIME:
      case BOOLEAN:
      case DECIMAL:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The RFC3966 phone number transformation can not be used on " +
            descriptor.getDataType() + " data");
    }

  }

  @Override
  public ASN1OctetString toLDAPValue(final AttributeDescriptor descriptor,
                                     final SCIMAttributeValue value)
  {
    PhoneNumberUtil util = PhoneNumberUtil.getInstance();
    switch (descriptor.getDataType())
    {
      case STRING:
        try
        {
          final Phonenumber.PhoneNumber number =
              util.parse(value.getStringValue(), getDefaultCountry());
          if(!util.isPossibleNumber(number))
          {
            if(Debug.debugEnabled())
            {
              Debug.debug(Level.WARNING, DebugType.OTHER,
                  number.getRawInput() + " doesn't seem to be a valid phone " +
                      "number and will not be canonicalized to LDAP format");
            }
            return new ASN1OctetString(value.getStringValue());
          }
          return new ASN1OctetString(
              util.format(number, getLdapFormat()));
        }
        catch(NumberParseException e)
        {
          // Don't fail but just pass the string as is to LDAP and let it
          // figure it out.
          Debug.debugException(e);
          return new ASN1OctetString(value.getStringValue());
        }

      case DATETIME:
      case BOOLEAN:
      case DECIMAL:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The RFC3966 phone number transformation can not be used on " +
            descriptor.getDataType() + " data");
    }
  }

  /**
   * Retrieve the default country that should be used when parsing phone
   * numbers.
   *
   * @return The default country that should be used when parsing phone
   *         numbers.
   */
  private String getDefaultCountry()
  {
    if(defaultCountry == null)
    {
      defaultCountry = getArguments().get(DEFAULT_COUNTRY_ARG);
      if(defaultCountry == null || defaultCountry.isEmpty())
      {
        defaultCountry = DEFAULT_COUNTRY;
      }
    }
    return defaultCountry;
  }

  /**
   * Retrieve the phone number format that should be used when values are
   * transformed to LDAP.
   *
   * @return The phone number format that should be used when values are
   *         transformed to LDAP.
   */
  private PhoneNumberUtil.PhoneNumberFormat getLdapFormat()
  {
    if(ldapFormat == null)
    {
      String format = getArguments().get(LDAP_FORMAT_ARG);
      if(format != null)
      {
        ldapFormat =
            PhoneNumberUtil.PhoneNumberFormat.valueOf(format.toUpperCase());
        if(ldapFormat == null)
        {
          ldapFormat = DEFAULT_LDAP_FORMAT;
        }
      }
    }
    return ldapFormat;
  }
}
