/*
 * Copyright 2011-2013 UnboundID Corp.
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
          Boolean p = value.getSubAttributeValue("primary",
              BOOLEAN_RESOLVER);
          return new Address(
              value.getSubAttributeValue("formatted",
                  STRING_RESOLVER),
              value.getSubAttributeValue("streetAddress",
                  STRING_RESOLVER),
              value.getSubAttributeValue("locality",
                  STRING_RESOLVER),
              value.getSubAttributeValue("region",
                  STRING_RESOLVER),
              value.getSubAttributeValue("postalCode",
                  STRING_RESOLVER),
              value.getSubAttributeValue("country",
                  STRING_RESOLVER),
              value.getSubAttributeValue("type",
                  STRING_RESOLVER),
              p == null ? false : p);
        }

        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor addressDescriptor, final Address value)
            throws InvalidResourceException {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(8);

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          if (value.formatted != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("formatted"),
                    SCIMAttributeValue.createStringValue(value.formatted)));
          }

          if (value.streetAddress != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("streetAddress"),
                    SCIMAttributeValue.createStringValue(value.streetAddress)));
          }

          if (value.locality != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("locality"),
                    SCIMAttributeValue.createStringValue(value.locality)));
          }

          if (value.region != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("region"),
                    SCIMAttributeValue.createStringValue(value.region)));
          }

          if (value.postalCode != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("postalCode"),
                    SCIMAttributeValue.createStringValue(value.postalCode)));
          }

          if (value.country != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("country"),
                    SCIMAttributeValue.createStringValue(value.country)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("primary"),
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
   * @param primary        Specifies whether this value is the primary value.
   * @param streetAddress  The full street address component, which may include
   *                       house number, street name, PO BOX, and multi-line
   *                       extended street address information.
   * @param locality       The city or locality component.
   * @param region         The state or region component.
   * @param postalCode     The zip code or postal code component.
   * @param country        The country name component.
   * @param formatted      The full mailing address, formatted for display or
   *                       use with a mailing label.
   * @param type           The type of address, "work", "home" or "other".
   */
  public Address(final String formatted, final String streetAddress,
                 final String locality, final String region,
                 final String postalCode, final String country,
                 final String type, final boolean primary) {
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

    Address address = (Address) o;

    if (primary != address.primary) {
      return false;
    }
    if (country != null ? !country.equals(address.country) :
        address.country != null) {
      return false;
    }
    if (formatted != null ? !formatted.equals(address.formatted) :
        address.formatted != null) {
      return false;
    }
    if (locality != null ? !locality.equals(address.locality) :
        address.locality != null) {
      return false;
    }
    if (postalCode != null ? !postalCode.equals(address.postalCode) :
        address.postalCode != null) {
      return false;
    }
    if (region != null ? !region.equals(address.region) :
        address.region != null) {
      return false;
    }
    if (streetAddress != null ? !streetAddress.equals(address.streetAddress) :
        address.streetAddress != null) {
      return false;
    }
    if (type != null ? !type.equals(address.type) : address.type != null) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = country != null ? country.hashCode() : 0;
    result = 31 * result + (formatted != null ? formatted.hashCode() : 0);
    result = 31 * result + (locality != null ? locality.hashCode() : 0);
    result = 31 * result + (postalCode != null ? postalCode.hashCode() : 0);
    result = 31 * result + (region != null ? region.hashCode() : 0);
    result = 31 * result + (streetAddress != null ?
        streetAddress.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (primary ? 1 : 0);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Address{" +
        "formatted='" + formatted + '\'' +
        ", streetAddress='" + streetAddress + '\'' +
        ", locality='" + locality + '\'' +
        ", region='" + region + '\'' +
        ", postalCode='" + postalCode + '\'' +
        ", country='" + country + '\'' +
        ", type='" + type + '\'' +
        ", primary=" + primary +
        '}';
  }
}
