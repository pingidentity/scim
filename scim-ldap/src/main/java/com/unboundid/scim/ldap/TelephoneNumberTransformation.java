/*
 * Portions Copyright 2012-2014 UnboundID Corp.
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
/*
 * Copyright (C) 2009 The Libphonenumber Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.util.ByteString;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A transformation for LDAP Telephone Number syntax (E.123) to RFC3966 and
 * vice-versa.
 */
public class TelephoneNumberTransformation extends Transformation
{
  /** Flags to use when compiling regular expressions for phone numbers. */
  static final int REGEX_FLAGS =
      Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE;
  private static final String RFC3966_EXTN_PREFIX = ";ext=";
  private static final String RFC3966_PREFIX = "tel:";
  private static final String DIGITS = "\\p{Nd}";

  // Default extension prefix to use when formatting. This will be put in front
  // of any extension component of the number, after the main national number is
  // formatted. For example, if you wish the default extension formatting to be
  // " extn: 3456", then you should specify " extn: " here as the default
  // extension prefix. This can be overridden by region-specific preferences.
  private static final String DEFAULT_EXTN_PREFIX = " ext. ";

  // Pattern to capture digits used in an extension. Places a maximum length of
  // "7" for an extension.
  private static final String CAPTURING_EXTN_DIGITS = "(" + DIGITS + "{1,7})";

  // Regexp of all possible ways to write extensions, for use when parsing. This
  // will be run as a case-insensitive regexp match. Wide character versions
  // are also provided after each ASCII version.
  // There are three regular expressions here. The first covers RFC 3966 format,
  // where the extension is added using ";ext=". The second more generic one
  // starts with optional white space and ends with an optional full stop (.),
  // followed by zero or more spaces/tabs and then the numbers themselves.
  // The other one covers the special case of American numbers where the
  // extension is written with a hash at the end, such as "- 503#". Note that
  // the only capturing groups should be around the digits that you want to
  // capture as part of the extension, or else parsing will fail!
  // Canonical-equivalence doesn't seem to be an option with Android java,
  // so we allow two options for representing the accented o - the character
  // itself, and one in the unicode decomposed form with the combining acute
  // accent.
  private static final String EXTN_PATTERNS_FOR_PARSING =
      (RFC3966_EXTN_PREFIX + CAPTURING_EXTN_DIGITS + "|" + "[ \u00A0\\t,]*" +
          "(?:e?xt(?:ensi(?:o\u0301?|\u00F3))?n?|\uFF45?\uFF58\uFF54\uFF4E?|" +
          "[,x\uFF58#\uFF03~\uFF5E]|int|anexo|\uFF49\uFF4E\uFF54)" +
          "[:\\.\uFF0E]?[ \u00A0\\t,-]*" + CAPTURING_EXTN_DIGITS + "#?|" +
          "[- ]+(" + DIGITS + "{1,5})#");

  // Regexp of all known extension prefixes used by different regions followed
  // by 1 or more valid digits, for use when parsing.
  private static final Pattern EXTN_PATTERN =
      Pattern.compile("(?:" + EXTN_PATTERNS_FOR_PARSING + ")$", REGEX_FLAGS);

  @Override
  public String toLDAPFilterValue(final String scimFilterValue)
  {
    return toE123(scimFilterValue);
  }

  @Override
  public SCIMAttributeValue toSCIMValue(final AttributeDescriptor descriptor,
                                        final ByteString byteString)
  {
    switch (descriptor.getDataType())
    {
      case STRING:
        return SCIMAttributeValue.createStringValue(
            toRFC3966(byteString.stringValue()));

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
    switch (descriptor.getDataType())
    {
      case STRING:
        return new ASN1OctetString(toE123(value.getStringValue()));
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
   * Convert a E.123 formatted number to RFC 3966 format.
   *
   * @param number The E.123 formatted number.
   * @return The RFC 3966 formatted number.
   */
  private String toRFC3966(final String number)
  {
    StringBuilder stringBuilder = new StringBuilder(number);
    String ext = maybeStripExtension(stringBuilder);
    String num = stringBuilder.toString().trim();
    stringBuilder.setLength(0);

    for(int i = 0; i < num.length(); i++)
    {
      char c = num.charAt(i);
      if(c == ' ')
      {
        stringBuilder.append('-');
      }
      if((i == 0 && c == '+') || c == '-' || Character.isDigit(c))
      {
        stringBuilder.append(c);
      }
    }

    if(ext.length() > 0)
    {
      stringBuilder.append(RFC3966_EXTN_PREFIX);
      stringBuilder.append(ext);
    }
    stringBuilder.insert(0, RFC3966_PREFIX);

    return stringBuilder.toString();
  }

  /**
   * Convert a RFC 3966 formatted number to E.123 format.
   *
   * @param number The RFC 3966 formatted number.
   * @return The E.123 formatted number.
   */
  private String toE123(final String number)
  {
    StringBuilder stringBuilder = new StringBuilder(number);
    String ext = maybeStripExtension(stringBuilder);
    String num = stringBuilder.toString().trim();
    stringBuilder.setLength(0);

    num = num.replaceFirst(RFC3966_PREFIX, "");
    for(int i = 0; i < num.length(); i++)
    {
      char c = num.charAt(i);
      if(c == '-')
      {
        stringBuilder.append(' ');
      }
      if((i == 0 && c == '+') || c == ' ' || Character.isDigit(c))
      {
        stringBuilder.append(num.charAt(i));
      }
    }

    if(ext.length() > 0)
    {
      stringBuilder.append(DEFAULT_EXTN_PREFIX);
      stringBuilder.append(ext);
    }

    return stringBuilder.toString();
  }

  /**
   * Strips any extension (as in, the part of the number dialled after the call
   * is connected, usually indicated with extn, ext, x or similar) from the end
   * of the number, and returns it.
   *
   * @param number  the non-normalized telephone number that we wish to strip
   *                the extension from
   * @return        the phone extension
   */
  private String maybeStripExtension(final StringBuilder number) {
    Matcher m = EXTN_PATTERN.matcher(number);
    // If we find a potential extension, and the number preceding this is at
    // least 2 chars in length, we assume it is an extension.
    if (m.find() && m.start() >= 2) {
      // The numbers are captured into groups in the regular expression.
      for (int i = 1, length = m.groupCount(); i <= length; i++) {
        if (m.group(i) != null) {
          // We go through the capturing groups until we find one that captured
          // some digits. If none did, then we will return the empty string.
          String extension = m.group(i);
          number.delete(m.start(), number.length());
          return extension;
        }
      }
    }
    return "";
  }
}
