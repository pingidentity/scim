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
import com.unboundid.scim.sdk.SCIMAttributeValue;

import java.util.Date;

/**
 * Used to resolve SCIM attribute values to Java instances.
 *
 * @param <T> The Java class to resolve.
 */
public abstract class AttributeValueResolver<T>
{
  /**
   * Create an instance from the given attribute value.
   *
   * @param value The value to create an instance from.
   * @return The instance created from the attribute value.
   */
  public abstract T toInstance(final SCIMAttributeValue value);

  /**
   * Create a SCIM attribute value from the given instance.
   *
   * @param attributeDescriptor The descriptor for the attribute to create.
   * @param value The instance.
   * @return The SCIM attribute value created from the instance.
   * @throws InvalidResourceException if the value violates the schema.
   */
  public abstract SCIMAttributeValue fromInstance(
      final AttributeDescriptor attributeDescriptor, final T value)
      throws InvalidResourceException;

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>String</code> instances.
   */
  public static final AttributeValueResolver<String> STRING_RESOLVER =
      new AttributeValueResolver<String>() {

        /**
         * {@inheritDoc}
         */
        public String toInstance(final SCIMAttributeValue value) {
          return value.getStringValue();
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor, final String value) {
          return SCIMAttributeValue.createStringValue(value);
        }
      };


  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>String</code> instances.
   */
  public static final AttributeValueResolver<Date> DATE_RESOLVER =
      new AttributeValueResolver<Date>() {
        /**
         * {@inheritDoc}
         */
        public Date toInstance(final SCIMAttributeValue value) {
          return value.getDateValue();
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor, final Date value) {
          return SCIMAttributeValue.createDateValue(value);
        }
      };

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Boolean</code> instances.
   */
  public static final AttributeValueResolver<Boolean> BOOLEAN_RESOLVER =
      new AttributeValueResolver<Boolean>() {
        /**
         * {@inheritDoc}
         */
        public Boolean toInstance(final SCIMAttributeValue value) {
          return value.getBooleanValue();
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Boolean value) {
          return SCIMAttributeValue.createBooleanValue(value);
        }
      };

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>byte[]</code> instances.
   */
  public static final AttributeValueResolver<byte[]> BINARY_RESOLVER =
      new AttributeValueResolver<byte[]>() {

        /**
         * {@inheritDoc}
         */
        @Override
        public byte[] toInstance(final SCIMAttributeValue value) {
          return value.getBinaryValue();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final byte[] value) {
          return SCIMAttributeValue.createBinaryValue(value);
        }
      };

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Decimal</code> instances.
   */
  public static final AttributeValueResolver<Double> DECIMAL_RESOLVER =
      new AttributeValueResolver<Double>() {
        /**
         * {@inheritDoc}
         */
        public Double toInstance(final SCIMAttributeValue value) {
          return value.getDecimalValue();
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Double value) {
          return SCIMAttributeValue.createStringValue(String.valueOf(value));
        }
      };

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Integer</code> instances.
   */
  public static final AttributeValueResolver<Long> INTEGER_RESOLVER =
      new AttributeValueResolver<Long>() {
        /**
         * {@inheritDoc}
         */
        public Long toInstance(final SCIMAttributeValue value) {
          return value.getIntegerValue();
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Long value) {
          return SCIMAttributeValue.createStringValue(String.valueOf(value));
        }
      };


}
