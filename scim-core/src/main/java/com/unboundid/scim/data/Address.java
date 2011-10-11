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
 * This class represents the address complex attribute in user resources.
 */
public class Address
{
  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Address</code> instances.
   */
  public static final AttributeValueResolver<Address> ADDRESS_RESOLVER =
      new AttributeValueResolver<Address>()
      {
        public Address toInstance(final SCIMAttributeValue value) {
          return new Address(
              getSingularSubAttributeValue(value, "formatted",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "type",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "primary",
                  BOOLEAN_RESOLVER),
              getSingularSubAttributeValue(value, "locality",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "streetAddress",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "postalCode",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "region",
                  STRING_RESOLVER),
              getSingularSubAttributeValue(value, "country",
                  STRING_RESOLVER));
        }

        @Override
        SCIMAttributeValue fromInstance(
            final AttributeDescriptor addressDescriptor, final Address value) {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(8);

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor.getAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          if (value.formatted != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor.getAttribute("formatted"),
                    SCIMAttributeValue.createStringValue(value.formatted)));
          }

          if (value.streetAddress != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor.getAttribute("streetAddress"),
                    SCIMAttributeValue.createStringValue(value.streetAddress)));
          }

          if (value.locality != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor.getAttribute("locality"),
                    SCIMAttributeValue.createStringValue(value.locality)));
          }

          if (value.region != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor.getAttribute("region"),
                    SCIMAttributeValue.createStringValue(value.region)));
          }

          if (value.postalCode != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor.getAttribute("postalCode"),
                    SCIMAttributeValue.createStringValue(value.postalCode)));
          }

          if (value.country != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor.getAttribute("country"),
                    SCIMAttributeValue.createStringValue(value.country)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor.getAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };

  private String country;
  private String formatted;
  private String locality;
  private String postalCode;
  private String region;
  private String streetAddress;
  private String type;
  private boolean primary;

  /**
   * Create an instance of the SCIM addresses attribute.
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
   */
  public Address(final String formatted, final String type,
                 final boolean primary, final String locality,
                 final String streetAddress,  final String postalCode,
                 final String region, final String country) {
    this.country = country;
    this.formatted = formatted;
    this.locality = locality;
    this.postalCode = postalCode;
    this.primary = primary;
    this.region = region;
    this.streetAddress = streetAddress;
    this.type = type;
  }

  /**
   * Retrieves the country name component.
   *
   * @return The country name component.
   */
  public String getCountry() {
    return country;
  }

  /**
   * Sets the country name component.
   *
   * @param country The country name component.
   */
  public void setCountry(final String country) {
    this.country = country;
  }

  /**
   * Retrieves the full mailing address, formatted for display or use with a
   * mailing label.
   *
   * @return The full mailing address
   */
  public String getFormatted() {
    return formatted;
  }

  /**
   * Sets the full mailing address, formatted for display or use with a
   * mailing label.
   *
   * @param formatted The full mailing address.
   */
  public void setFormatted(final String formatted) {
    this.formatted = formatted;
  }

  /**
   * Retrieves the city or locality component.
   * @return The city or locality component.
   */
  public String getLocality() {
    return locality;
  }

  /**
   * Sets the city or locality component.
   * @param locality The city or locality component.
   */
  public void setLocality(final String locality) {
    this.locality = locality;
  }

  /**
   * Retrieves the zip code or postal code component.
   * @return The zip code or postal code component.
   */
  public String getPostalCode() {
    return postalCode;
  }

  /**
   * Sets the zip code or postal code component.
   * @param postalCode The zip code or postal code component.
   */
  public void setPostalCode(final String postalCode) {
    this.postalCode = postalCode;
  }

  /**
   * Whether this value is the primary value.
   *
   * @return <code>true</code> if this value is the primary value or
   * <code>false</code> otherwise.
   */
  public boolean isPrimary() {
    return primary;
  }

  /**
   * Specifies whether this value is the primary value.
   *
   * @param primary Whether this value is the primary value.
   */
  public void setPrimary(final boolean primary) {
    this.primary = primary;
  }

  /**
   * Retrieves the state or region component.
   *
   * @return The state or region component.
   */
  public String getRegion() {
    return region;
  }

  /**
   * Sets the state or region component.
   *
   * @param region The state or region component.
   */
  public void setRegion(final String region) {
    this.region = region;
  }

  /**
   * Retrieves the full street address component, which may include house
   * number, street name, PO BOX, and multi-line.
   *
   * @return The full street address component.
   */
  public String getStreetAddress() {
    return streetAddress;
  }

  /**
   * Sets The full street address component, which may include house number,
   * street name, PO BOX, and multi-line.
   *
   * @param streetAddress The full street address component.
   */
  public void setStreetAddress(final String streetAddress) {
    this.streetAddress = streetAddress;
  }

  /**
   * Retrieves the type of address, "work", "home" or "other".
   *
   * @return The type of address.
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of address, "work", "home" or "other".
   *
   * @param type he type of address.
   */
  public void setType(final String type) {
    this.type = type;
  }
}
