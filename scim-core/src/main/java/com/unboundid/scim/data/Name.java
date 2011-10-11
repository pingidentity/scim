/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.data;

import com.unboundid.scim.schema.AttributeDescriptor;
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
              getSingularSubAttributeValue(value, "formatted",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "familyName",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "middleName",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "givenName",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "honorificPrefix",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "honorificSuffix",
                  STRING_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor, final Name value) {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(6);

          if (value.formatted != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("formatted"),
                    SCIMAttributeValue.createStringValue(value.formatted)));
          }

          if (value.givenName != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("givenName"),
                    SCIMAttributeValue.createStringValue(value.givenName)));
          }

          if (value.familyName != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("familyName"),
                    SCIMAttributeValue.createStringValue(value.familyName)));
          }

          if (value.middleName != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("middleName"),
                    SCIMAttributeValue.createStringValue(value.middleName)));
          }

          if (value.honorificPrefix != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("honorificPrefix"),
                    SCIMAttributeValue.createStringValue(
                        value.honorificPrefix)));
          }

          if (value.honorificSuffix != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("honorificSuffix"),
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
}
