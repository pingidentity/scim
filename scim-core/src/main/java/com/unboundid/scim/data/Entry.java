/*
 * Copyright 2011 UnboundID Corp.
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
import java.util.Date;
import java.util.List;

/**
 * Represents a standard value of a plural attribute.
 *
 * @param <T> The value type.
 */
public final class Entry<T>
{
  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>String</code> valued <code>Entry</code> instances.
   */
  public static final AttributeValueResolver<Entry<String>>
      STRINGS_RESOLVER =
      new AttributeValueResolver<Entry<String>>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Entry<String> toInstance(final SCIMAttributeValue value) {
          String v =
              value.getSingularSubAttributeValue("value", STRING_RESOLVER);
          String t =
              value.getSingularSubAttributeValue("type", STRING_RESOLVER);
          Boolean p =
              value.getSingularSubAttributeValue("primary", BOOLEAN_RESOLVER);
          String d =
              value.getSingularSubAttributeValue("display", STRING_RESOLVER);


          return new Entry<String>(v, t, p == null ? false : p, d);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Entry<String> value) throws InvalidResourceException {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(4);

          if (value.value != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("value"),
                    SCIMAttributeValue.createStringValue(value.value)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          if(value.display != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("display"),
                    SCIMAttributeValue.createStringValue(value.display)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Boolean</code> valued <code>Entry</code> instances.
   */
  public static final AttributeValueResolver<Entry<Boolean>>
      BOOLEANS_RESOLVER =
      new AttributeValueResolver<Entry<Boolean>>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Entry<Boolean> toInstance(final SCIMAttributeValue value) {
          Boolean v =
              value.getSingularSubAttributeValue("value", BOOLEAN_RESOLVER);
          String t =
              value.getSingularSubAttributeValue("type", STRING_RESOLVER);
          Boolean p =
              value.getSingularSubAttributeValue("primary", BOOLEAN_RESOLVER);
          String d =
              value.getSingularSubAttributeValue("display", STRING_RESOLVER);


          return new Entry<Boolean>(v, t, p == null ? false : p, d);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Entry<Boolean> value) throws InvalidResourceException {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(4);

          if (value.value != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("value"),
                    SCIMAttributeValue.createBooleanValue(value.value)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          if(value.display != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("display"),
                    SCIMAttributeValue.createStringValue(value.display)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>byte[]</code> valued <code>Entry</code> instances.
   */
  public static final AttributeValueResolver<Entry<byte[]>>
      BINARIES_RESOLVER =
      new AttributeValueResolver<Entry<byte[]>>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Entry<byte[]> toInstance(final SCIMAttributeValue value) {
          byte[] v =
              value.getSingularSubAttributeValue("value", BINARY_RESOLVER);
          String t =
              value.getSingularSubAttributeValue("type", STRING_RESOLVER);
          Boolean p =
              value.getSingularSubAttributeValue("primary", BOOLEAN_RESOLVER);
          String d =
              value.getSingularSubAttributeValue("display", STRING_RESOLVER);


          return new Entry<byte[]>(v, t, p == null ? false : p, d);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Entry<byte[]> value) throws InvalidResourceException {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(4);

          if (value.value != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("value"),
                    SCIMAttributeValue.createBinaryValue(value.value)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          if(value.display != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("display"),
                    SCIMAttributeValue.createStringValue(value.display)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Date</code> valued <code>Entry</code> instances.
   */
  public static final AttributeValueResolver<Entry<Date>>
      DATES_RESOLVER =
      new AttributeValueResolver<Entry<Date>>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Entry<Date> toInstance(final SCIMAttributeValue value) {
          Date v =
              value.getSingularSubAttributeValue("value", DATE_RESOLVER);
          String t =
              value.getSingularSubAttributeValue("type", STRING_RESOLVER);
          Boolean p =
              value.getSingularSubAttributeValue("primary", BOOLEAN_RESOLVER);
          String d =
              value.getSingularSubAttributeValue("display", STRING_RESOLVER);


          return new Entry<Date>(v, t, p == null ? false : p, d);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Entry<Date> value) throws InvalidResourceException {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(4);

          if (value.value != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("value"),
                    SCIMAttributeValue.createDateValue(value.value)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          if(value.display != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getSubAttribute("display"),
                    SCIMAttributeValue.createStringValue(value.display)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };


  private T value;
  private boolean primary;
  private String type;
  private String display;

  /**
   * Constructs an entry instance with the specified information.
   *
   * @param value The primary value of this attribute.
   * @param type The type of attribute for this instance, usually used to
   *             label the preferred function of the given resource.
   */
  public Entry(final T value, final String type) {
    this(value, type, false);
  }

  /**
   * Constructs an entry instance with the specified information.
   *
   * @param value The primary value of this attribute.
   * @param type The type of attribute for this instance, usually used to
   *             label the preferred function of the given resource.
   * @param primary A Boolean value indicating whether this instance of the
   *                Plural Attribute is the primary or preferred value of for
   *                this attribute.
   */
  public Entry(final T value, final String type, final boolean primary) {
    this.value = value;
    this.type = type;
    this.primary = primary;
  }

  /**
   * Constructs an entry instance with the specified information.
   *
   * @param value The primary value of this attribute.
   * @param type The type of attribute for this instance, usually used to
   *             label the preferred function of the given resource.
   * @param primary A Boolean value indicating whether this instance of the
   *                Plural Attribute is the primary or preferred value of for
   *                this attribute.
   * @param display A human readable name, primarily used for display purposes
   *                where the value is an opaque or complex type such as an id.
   */
  public Entry(final T value, final String type, final boolean primary,
               final String display) {
    this.value = value;
    this.type = type;
    this.primary = primary;
    this.display = display;
  }

  /**
   * Whether this instance of the Plural Attribute is the primary or
   * preferred value of for this attribute.
   *
   * @return <code>true</code> if this instance of the Plural Attribute is
   *         the primary or preferred value of for this attribute or
   *         <code>false</code> otherwise
   */
  public boolean isPrimary() {
    return primary;
  }

  /**
   * Sets whether this instance of the Plural Attribute is the primary or
   * preferred value of for this attribute.
   *
   * @param primary <code>true</code> if this instance of the Plural Attribute
   *                is the primary or preferred value of for this attribute or
   *                <code>false</code> otherwise
   */
  public void setPrimary(final boolean primary) {
    this.primary = primary;
  }

  /**
   * Retrieves the type of attribute for this instance, usually used to label
   * the preferred function of the given resource.
   *
   * @return The type of attribute for this instance, usually used to label
   *         the preferred function of the given resource.
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of attribute for this instance, usually used to label
   * the preferred function of the given resource.
   *
   * @param type The type of attribute for this instance, usually used to label
   * the preferred function of the given resource.
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * Retrieves the primary value of this attribute.
   *
   * @return The primary value of this attribute.
   */
  public T getValue() {
    return value;
  }

  /**
   * Sets the primary value of this attribute.
   *
   * @param value The primary value of this attribute.
   */
  public void setValue(final T value) {
    this.value = value;
  }

  /**
   * Retrieves the human readable name, primarily used for display purposes
   * where the value is an opaque or complex type such as an id.
   *
   * @return The human readable name.
   */
  public String getDisplay() {
    return this.display;
  }

  /**
   * Sets the human readable name, primarily used for display purposes
   * where the value is an opaque or complex type such as an id.
   *
   * @param display The human readable name.
   */
  public void setDisplay(final String display) {
    this.display = display;
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

    Entry entry = (Entry) o;

    if (primary != entry.primary) {
      return false;
    }
    if (type != null ? !type.equals(entry.type) : entry.type != null) {
      return false;
    }
    if (value != null ? !value.equals(entry.value) : entry.value != null) {
      return false;
    }
    if (display != null ? !display.equals(entry.display) :
        entry.display != null) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = value != null ? value.hashCode() : 0;
    result = 31 * result + (primary ? 1 : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (display != null ? display.hashCode() : 0);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Entry{" +
        "value=" + value +
        ", type='" + type + '\'' +
        ", primary=" + primary +
        ", display=" + display +
        '}';
  }
}
