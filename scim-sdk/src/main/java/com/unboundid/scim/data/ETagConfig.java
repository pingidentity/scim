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
 * A complex type that specifies ETag configuration options.
 */
public class ETagConfig
{
  private final boolean supported;



  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>ETagConfig</code> instances.
   */
  public static final AttributeValueResolver<ETagConfig>
      ETAG_CONFIG_RESOLVER =
      new AttributeValueResolver<ETagConfig>()
      {
        /**
         * {@inheritDoc}
         */
        @Override
        public ETagConfig toInstance(final SCIMAttributeValue value) {
          return new ETagConfig(
              value.getSubAttributeValue("supported",
                  BOOLEAN_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final ETagConfig value)
            throws InvalidResourceException
        {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(1);

          final AttributeDescriptor supportedDescriptor =
              attributeDescriptor.getSubAttribute("supported");
          subAttributes.add(
              SCIMAttribute.create(
                  supportedDescriptor,
                  BOOLEAN_RESOLVER.fromInstance(supportedDescriptor,
                      value.supported)));

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };



  /**
   * Create a <code>ETagConfig</code> instance.
   *
   * @param supported  Specifies whether the ETag resource versions are
   *                   supported.
   */
  public ETagConfig(final boolean supported)
  {
    this.supported = supported;
  }



  /**
   * Indicates whether the ETag resource versions are supported.
   * @return  {@code true} if ETag resource versions are supported.
   */
  public boolean isSupported()
  {
    return supported;
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

    final ETagConfig that = (ETagConfig) o;

    if (supported != that.supported)
    {
      return false;
    }

    return true;
  }



  @Override
  public int hashCode()
  {
    return (supported ? 1 : 0);
  }



  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("ETagConfig");
    sb.append("{supported=").append(supported);
    sb.append('}');
    return sb.toString();
  }
}
