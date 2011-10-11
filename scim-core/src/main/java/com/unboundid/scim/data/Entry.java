/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.data;

import com.unboundid.scim.schema.AttributeDescriptor;
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
        Entry<String> toInstance(final SCIMAttributeValue value) {
          return new Entry<String>(
              getSingularSubAttributeValue(value, "value", STRING_RESOLVER),
              getSingularSubAttributeValue(value, "type", STRING_RESOLVER),
              getSingularSubAttributeValue(value, "primary",
                  BOOLEAN_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Entry<String> value) {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(3);

          if (value.value != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("value"),
                    SCIMAttributeValue.createStringValue(value.value)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };

    /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>String</code> valued <code>Entry</code> instances.
   */
  public static final AttributeValueResolver<Entry<Boolean>>
      BOOLEANS_RESOLVER =
      new AttributeValueResolver<Entry<Boolean>>() {
        /**
         * {@inheritDoc}
         */
        @Override
        Entry<Boolean> toInstance(final SCIMAttributeValue value) {
          return new Entry<Boolean>(
              getSingularSubAttributeValue(value, "value",
                  BOOLEAN_RESOLVER),
              getSingularSubAttributeValue(value, "type", STRING_RESOLVER),
              getSingularSubAttributeValue(value, "primary",
                  BOOLEAN_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Entry<Boolean> value) {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(3);

          if (value.value != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("value"),
                    SCIMAttributeValue.createBooleanValue(value.value)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };

    /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>String</code> valued <code>Entry</code> instances.
   */
  public static final AttributeValueResolver<Entry<Date>>
      DATES_RESOLVER =
      new AttributeValueResolver<Entry<Date>>() {
        /**
         * {@inheritDoc}
         */
        @Override
        Entry<Date> toInstance(final SCIMAttributeValue value) {
          return new Entry<Date>(
              getSingularSubAttributeValue(value, "value", DATE_RESOLVER),
              getSingularSubAttributeValue(value, "type", STRING_RESOLVER),
              getSingularSubAttributeValue(value, "primary",
                  BOOLEAN_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Entry<Date> value) {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(3);

          if (value.value != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("value"),
                    SCIMAttributeValue.createDateValue(value.value)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.createSingularAttribute(
                    attributeDescriptor.getAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };


  private T value;
  private boolean primary;
  private String type;

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
}
