/*
 * Copyright 2011-2015 UnboundID Corp.
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
 * A transformation for LDAP PostalAddress syntax. Any newlines in the SCIM
 * values are replaced with '$' separator characters in the LDAP
 * values and vice-versa.
 */
public class PostalAddressTransformation extends Transformation
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
      case STRING:
        final String value = transformToSCIM(byteString.stringValue());
        return SCIMAttributeValue.createStringValue(value);

      case DATETIME:
      case BOOLEAN:
      case DECIMAL:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The postal address transformation can not be used on " +
            descriptor.getDataType() + " data");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ASN1OctetString toLDAPValue(final AttributeDescriptor descriptor,
                                     final SCIMAttributeValue scimValue)
  {
    switch (descriptor.getDataType())
    {
      case STRING:
        final String value = transformToLDAP(scimValue.getStringValue());
        return new ASN1OctetString(value);

      case DATETIME:
      case BOOLEAN:
      case DECIMAL:
      case INTEGER:
      case BINARY:
      default:
        throw new IllegalArgumentException(
            "The postal address transformation can not be used on " +
            descriptor.getDataType() + " data");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toLDAPFilterValue(final String scimFilterValue)
  {
    return transformToLDAP(scimFilterValue);
  }



  /**
   * From LDAP RFC 4517:
   * Each character string (i.e., <line>) of a postal address value is
   * encoded as a UTF-8 [RFC3629] string, except that "\" and "$"
   * characters, if they occur in the string, are escaped by a "\"
   * character followed by the two hexadecimal digit code for the
   * character.
   */

  /**
   * Transform a SCIM postal address to an LDAP postal address.
   *
   * @param s  The value to be transformed.
   * @return  The LDAP value.
   */
  private String transformToLDAP(final String s)
  {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < s.length(); i++)
    {
      final char c = s.charAt(i);
      switch (c)
      {
        case '\n':
          builder.append('$');
          break;

        case '\\':
          builder.append("\\5C");
          break;

        case '$':
          builder.append("\\24");
          break;

        default:
          builder.append(c);
          break;
      }
    }

    return builder.toString();
  }



  /**
   * Transform an LDAP postal address to a SCIM postal address.
   *
   * @param s  The value to be transformed.
   * @return  The SCIM value.
   */
  private String transformToSCIM(final String s)
  {
    final StringBuilder builder = new StringBuilder();

    int i = 0;
    while (i < s.length())
    {
      final char c = s.charAt(i);
      switch (c)
      {
        case '\\':
          if (i + 3 > s.length())
          {
            // Not valid but let it pass untouched.
            builder.append(c);
            i++;
          }
          else
          {
            final String hex = s.substring(i+1, i+3).toUpperCase();
            if (hex.equals("5C"))
            {
              builder.append('\\');
            }
            else if (hex.equals("24"))
            {
              builder.append('$');
            }
            else
            {
              // Not valid but let it pass untouched.
              builder.append(c);
              builder.append(hex);
            }
            i += 3;
          }
          break;

        case '$':
          builder.append("\n");
          i++;
          break;

        default:
          builder.append(c);
          i++;
          break;
      }
    }

    return builder.toString();
  }
}
