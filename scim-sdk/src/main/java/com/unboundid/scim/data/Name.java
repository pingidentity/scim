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

package com.unboundid.scim.data;

import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;

import java.util.ArrayList;
import java.util.List;

/**
 * A complex type containing the components of the User's real name.
 */
public class Name
{
  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Name</code> instances.
   */
  public static final AttributeValueResolver<Name> NAME_RESOLVER =
      new AttributeValueResolver<Name>()
      {
        /**
         * {@inheritDoc}
         */
        @Override
        public Name toInstance(final SCIMAttributeValue value) {
          return new Name(
              value.getSubAttributeValue("formatted",
                  STRING_RESOLVER),
              value.getSubAttributeValue("familyName",
                  STRING_RESOLVER),
              value.getSubAttributeValue("middleName",
                  STRING_RESOLVER),
              value.getSubAttributeValue("givenName",
                  STRING_RESOLVER),
              value.getSubAttributeValue("honorificPrefix",
                  STRING_RESOLVER),
              value.getSubAttributeValue("honorificSuffix",
                  STRING_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor, final Name value)
            throws InvalidResourceException {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(6);

          if (value.formatted != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    attributeDescriptor.getSubAttribute("formatted"),
                    SCIMAttributeValue.createStringValue(value.formatted)));
          }

          if (value.givenName != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    attributeDescriptor.getSubAttribute("givenName"),
                    SCIMAttributeValue.createStringValue(value.givenName)));
          }

          if (value.familyName != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    attributeDescriptor.getSubAttribute("familyName"),
                    SCIMAttributeValue.createStringValue(value.familyName)));
          }

          if (value.middleName != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    attributeDescriptor.getSubAttribute("middleName"),
                    SCIMAttributeValue.createStringValue(value.middleName)));
          }

          if (value.honorificPrefix != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    attributeDescriptor.getSubAttribute("honorificPrefix"),
                    SCIMAttributeValue.createStringValue(
                        value.honorificPrefix)));
          }

          if (value.honorificSuffix != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    attributeDescriptor.getSubAttribute("honorificSuffix"),
                    SCIMAttributeValue.createStringValue(
                        value.honorificSuffix)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };

  private String formatted;
  private String familyName;
  private String middleName;
  private String givenName;
  private String honorificPrefix;
  private String honorificSuffix;

  /**
   * Creates a SCIM core user 'name' attribute. Any of the arguments may be
   * {@code null} if they are not to be included.
   *
   * @param formatted        The The full name, including all middle names,
   *                         titles, and suffixes as appropriate, formatted
   *                         for display.
   * @param familyName       The family name of the User, or "Last Name" in
   *                         most Western languages.
   * @param givenName        The given name of the User, or "First Name" in
   *                         most Western languages.
   * @param middleName       The middle name(s) of the User.
   * @param honorificPrefix  The honorific prefix(es) of the User, or "Title"
   *                         in most Western languages.
   * @param honorificSuffix  The honorifix suffix(es) of the User, or "Suffix"
   *                         in most Western languages.
   */
  public Name(final String formatted, final String familyName,
              final String middleName, final String givenName,
              final String honorificPrefix, final String honorificSuffix) {
    this.formatted = formatted;
    this.familyName = familyName;
    this.middleName = middleName;
    this.givenName = givenName;
    this.honorificPrefix = honorificPrefix;
    this.honorificSuffix = honorificSuffix;
  }

  /**
   * Retrieves the family name of the User, or "Last Name" in  most Western
   * languages.
   *
   * @return The family name of the User, or "Last Name" in  most Western
   * languages.
   */
  public String getFamilyName() {
    return familyName;
  }

  /**
   * Sets the family name of the User, or "Last Name" in  most Western
   * languages.
   *
   * @param familyName The family name of the User, or "Last Name" in  most
   * Western languages.
   */
  public void setFamilyName(final String familyName) {
    this.familyName = familyName;
  }

  /**
   * Retrieves the full name, including all middle names, titles, and
   * suffixes as appropriate, formatted for display.
   *
   * @return The full name, including all middle names, titles, and
   * suffixes as appropriate, formatted for display.
   */
  public String getFormatted() {
    return formatted;
  }

  /**
   * Sets the full name, including all middle names, titles, and
   * suffixes as appropriate, formatted for display.
   *
   * @param formatted The full name, including all middle names, titles, and
   * suffixes as appropriate, formatted for display.
   */
  public void setFormatted(final String formatted) {
    this.formatted = formatted;
  }

  /**
   * Retrieves the given name of the User, or "First Name" in most Western
   * languages.
   *
   * @return The given name of the User, or "First Name" in most Western
   * languages.
   */
  public String getGivenName() {
    return givenName;
  }

  /**
   * Sets the given name of the User, or "First Name" in most Western
   * languages.
   *
   * @param givenName The given name of the User, or "First Name" in most
   * Western languages.
   */
  public void setGivenName(final String givenName) {
    this.givenName = givenName;
  }

  /**
   * Retrieves the honorific prefix(es) of the User, or "Title" in most Western
   * languages.
   *
   * @return The honorific prefix(es) of the User, or "Title" in most Western
   * languages.
   */
  public String getHonorificPrefix() {
    return honorificPrefix;
  }

  /**
   * Sets the honorific prefix(es) of the User, or "Title" in most Western
   * languages.
   *
   * @param honorificPrefix The honorific prefix(es) of the User, or "Title"
   * in most Western languages.
   */
  public void setHonorificPrefix(final String honorificPrefix) {
    this.honorificPrefix = honorificPrefix;
  }

  /**
   * Retrieves the honorific suffix(es) of the User, or "Suffix" in most
   * Western languages.
   *
   * @return The honorific suffix(es) of the User, or "Suffix" in most
   * Western languages.
   */
  public String getHonorificSuffix() {
    return honorificSuffix;
  }

  /**
   * Sets the honorific suffix(es) of the User, or "Suffix" in most
   * Western languages.
   *
   * @param honorificSuffix The honorific suffix(es) of the User, or "Suffix"
   * in most Western languages.
   */
  public void setHonorificSuffix(final String honorificSuffix) {
    this.honorificSuffix = honorificSuffix;
  }

  /**
   * Retrieves the middle name(s) of the User.
   *
   * @return The middle name(s) of the User.
   */
  public String getMiddleName() {
    return middleName;
  }

  /**
   * Retrieves the middle name(s) of the User.
   *
   * @param middleName The middle name(s) of the User.
   */
  public void setMiddleName(final String middleName) {
    this.middleName = middleName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Name name = (Name) o;

    if (familyName != null ? !familyName.equals(name.familyName) :
        name.familyName != null) {
      return false;
    }
    if (formatted != null ? !formatted.equals(name.formatted) :
        name.formatted != null) {
      return false;
    }
    if (givenName != null ? !givenName.equals(name.givenName) :
        name.givenName != null) {
      return false;
    }
    if (honorificPrefix != null ?
        !honorificPrefix.equals(name.honorificPrefix) :
        name.honorificPrefix != null) {
      return false;
    }
    if (honorificSuffix != null ?
        !honorificSuffix.equals(name.honorificSuffix) :
        name.honorificSuffix != null) {
      return false;
    }
    if (middleName != null ? !middleName.equals(name.middleName) :
        name.middleName != null) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = formatted != null ? formatted.hashCode() : 0;
    result = 31 * result + (familyName != null ? familyName.hashCode() : 0);
    result = 31 * result + (middleName != null ? middleName.hashCode() : 0);
    result = 31 * result + (givenName != null ? givenName.hashCode() : 0);
    result = 31 * result + (honorificPrefix != null ?
        honorificPrefix.hashCode() : 0);
    result = 31 * result + (honorificSuffix != null ?
        honorificSuffix.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Name{" +
        "formatted='" + formatted + '\'' +
        ", familyName='" + familyName + '\'' +
        ", middleName='" + middleName + '\'' +
        ", givenName='" + givenName + '\'' +
        ", honorificPrefix='" + honorificPrefix + '\'' +
        ", honorificSuffix='" + honorificSuffix + '\'' +
        '}';
  }
}
