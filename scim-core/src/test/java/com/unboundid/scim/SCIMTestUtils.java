/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim;

import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for SCIM test cases.
 */
public class SCIMTestUtils
{
  /**
   * Generate a value of the SCIM addresses attribute.
   *
   * @param type           The type of address, "work", "home" or "other".
   * @param formatted      The full mailing address, formatted for display or
   *                       use with a mailing label.
   * @param streetAddress  The full street address component, which may include
   *                       house number, street name, PO BOX, and multi-line
   *                       extended street address information.
   * @param locality       The city or locality component.
   * @param region         The state or region component.
   * @param postalCode     The zip code or postal code component.
   * @param country        The country name component.
   * @param primary        Specifies whether this value is the primary value.
   *
   * @return  An attribute values constructed from the provided information.
   */
  public static SCIMAttributeValue generateAddress(
      final String type,
      final String formatted,
      final String streetAddress,
      final String locality,
      final String region,
      final String postalCode,
      final String country,
      final boolean primary)
  {
    final List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>();

    final SchemaManager manager = SchemaManager.instance();
    final ResourceDescriptor resourceDescriptor =
        manager.getResourceDescriptor(SCIMConstants.RESOURCE_NAME_USER);

    final AttributeDescriptor addressesDescriptor =
        resourceDescriptor.getAttribute(
            SCIMConstants.SCHEMA_URI_CORE, "addresses");
    final AttributeDescriptor addressDescriptor =
        addressesDescriptor.getAttribute("address");

    if (type != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              addressDescriptor.getAttribute("type"),
              SCIMAttributeValue.createStringValue(type)));
    }

    if (formatted != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              addressDescriptor.getAttribute("formatted"),
              SCIMAttributeValue.createStringValue(formatted)));
    }

    if (streetAddress != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              addressDescriptor.getAttribute("streetAddress"),
              SCIMAttributeValue.createStringValue(streetAddress)));
    }

    if (locality != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              addressDescriptor.getAttribute("locality"),
              SCIMAttributeValue.createStringValue(locality)));
    }

    if (region != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              addressDescriptor.getAttribute("region"),
              SCIMAttributeValue.createStringValue(region)));
    }

    if (postalCode != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              addressDescriptor.getAttribute("postalCode"),
              SCIMAttributeValue.createStringValue(postalCode)));
    }

    if (country != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              addressDescriptor.getAttribute("country"),
              SCIMAttributeValue.createStringValue(country)));
    }

    if (primary)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              addressDescriptor.getAttribute("primary"),
              SCIMAttributeValue.createBooleanValue(primary)));
    }

    return SCIMAttributeValue.createComplexValue(
        SCIMAttribute.createSingularAttribute(
            addressDescriptor,
            SCIMAttributeValue.createComplexValue(subAttributes)));
  }

  /**
   * Generate a SCIM core user 'name' attribute. Any of the arguments may be
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
   *
   * @return  A name attribute constructed from the provided values.
   */
  public static SCIMAttribute generateName(final String formatted,
                                              final String familyName,
                                              final String givenName,
                                              final String middleName,
                                              final String honorificPrefix,
                                              final String honorificSuffix)
  {
    final String coreSchema = SCIMConstants.SCHEMA_URI_CORE;

    final SchemaManager manager = SchemaManager.instance();
    final ResourceDescriptor resourceDescriptor =
        manager.getResourceDescriptor(SCIMConstants.RESOURCE_NAME_USER);

    final AttributeDescriptor attributeDescriptor =
        resourceDescriptor.getAttribute(
            SCIMConstants.SCHEMA_URI_CORE, "name");

    final List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>();

    if (formatted != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              attributeDescriptor.getAttribute("formatted"),
              SCIMAttributeValue.createStringValue(formatted)));
    }

    if (givenName != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              attributeDescriptor.getAttribute("givenName"),
              SCIMAttributeValue.createStringValue(givenName)));
    }

    if (familyName != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              attributeDescriptor.getAttribute("familyName"),
              SCIMAttributeValue.createStringValue(familyName)));
    }

    if (middleName != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              attributeDescriptor.getAttribute("middleName"),
              SCIMAttributeValue.createStringValue(middleName)));
    }

    if (honorificPrefix != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              attributeDescriptor.getAttribute("honorificPrefix"),
              SCIMAttributeValue.createStringValue(honorificPrefix)));
    }

    if (honorificSuffix != null)
    {
      subAttributes.add(
          SCIMAttribute.createSingularAttribute(
              attributeDescriptor.getAttribute("honorificSuffix"),
              SCIMAttributeValue.createStringValue(honorificSuffix)));
    }

    return SCIMAttribute.createSingularAttribute(
        coreSchema,"name",
        SCIMAttributeValue.createComplexValue(subAttributes));
  }
}
