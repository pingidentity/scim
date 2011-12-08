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
import java.util.List;



/**
 * A complex type that specifies BULK configuration options.
 */
public class BulkConfig
{
  private final boolean supported;
  private final long maxOperations;
  private final long maxPayloadSize;



  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>BulkConfig</code> instances.
   */
  public static final AttributeValueResolver<BulkConfig>
      BULK_CONFIG_RESOLVER =
      new AttributeValueResolver<BulkConfig>()
      {
        /**
         * {@inheritDoc}
         */
        @Override
        public BulkConfig toInstance(final SCIMAttributeValue value) {
          return new BulkConfig(
              value.getSubAttributeValue("supported",
                  BOOLEAN_RESOLVER),
              value.getSubAttributeValue("maxOperations",
                  INTEGER_RESOLVER),
              value.getSubAttributeValue("maxPayloadSize",
                  INTEGER_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final BulkConfig value)
            throws InvalidResourceException
        {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(3);

          final AttributeDescriptor supportedDescriptor =
              attributeDescriptor.getSubAttribute("supported");
          subAttributes.add(
              SCIMAttribute.create(
                  supportedDescriptor,
                  BOOLEAN_RESOLVER.fromInstance(supportedDescriptor,
                      value.supported)));

          final AttributeDescriptor maxOperationsDescriptor =
              attributeDescriptor.getSubAttribute("maxOperations");
          subAttributes.add(
              SCIMAttribute.create(
                  maxOperationsDescriptor,
                  INTEGER_RESOLVER.fromInstance(maxOperationsDescriptor,
                      value.maxOperations)));

          final AttributeDescriptor maxPayloadSizeDescriptor =
              attributeDescriptor.getSubAttribute("maxPayloadSize");
          subAttributes.add(
              SCIMAttribute.create(
                  maxPayloadSizeDescriptor,
                  INTEGER_RESOLVER.fromInstance(maxPayloadSizeDescriptor,
                      value.maxPayloadSize)));

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };



  /**
   * Create a <code>BulkConfig</code> instance.
   *
   * @param supported       Specifies whether the BULK operation is supported.
   * @param maxOperations   Specifies the maximum number of operations.
   * @param maxPayloadSize  Specifies the maximum payload size in bytes.
   */
  public BulkConfig(final boolean supported,
                    final long maxOperations,
                    final long maxPayloadSize)
  {
    this.supported = supported;
    this.maxOperations = maxOperations;
    this.maxPayloadSize = maxPayloadSize;
  }



  /**
   * Indicates whether the PATCH operation is supported.
   * @return  {@code true} if the PATCH operation is supported.
   */
  public boolean isSupported()
  {
    return supported;
  }



  /**
   * Retrieves the maximum number of operations.
   * @return The maximum number of operations.
   */
  public long getMaxOperations()
  {
    return maxOperations;
  }



  /**
   * Retrieves the maximum payload size in bytes.
   * @return The maximum payload size in bytes.
   */
  public long getMaxPayloadSize()
  {
    return maxPayloadSize;
  }


  @Override
  public boolean equals(final Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    final BulkConfig that = (BulkConfig) o;

    if (maxOperations != that.maxOperations)
    {
      return false;
    }
    if (maxPayloadSize != that.maxPayloadSize)
    {
      return false;
    }
    if (supported != that.supported)
    {
      return false;
    }

    return true;
  }



  @Override
  public int hashCode()
  {
    int result = (supported ? 1 : 0);
    result = 31 * result + (int) (maxOperations ^ (maxOperations >>> 32));
    result = 31 * result + (int) (maxPayloadSize ^ (maxPayloadSize >>> 32));
    return result;
  }



  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("BulkConfig");
    sb.append("{supported=").append(supported);
    sb.append(", maxOperations=").append(maxOperations);
    sb.append(", maxPayloadSize=").append(maxPayloadSize);
    sb.append('}');
    return sb.toString();
  }
}
