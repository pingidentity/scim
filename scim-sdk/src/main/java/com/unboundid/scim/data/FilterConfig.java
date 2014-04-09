/*
 * Copyright 2011-2014 UnboundID Corp.
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
 * A complex type that specifies FILTER configuration options.
 */
public class FilterConfig
{
  private final boolean supported;
  private final long maxResults;



  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>FilterConfig</code> instances.
   */
  public static final AttributeValueResolver<FilterConfig>
      FILTER_CONFIG_RESOLVER =
      new AttributeValueResolver<FilterConfig>()
      {
        /**
         * {@inheritDoc}
         */
        @Override
        public FilterConfig toInstance(final SCIMAttributeValue value) {
          return new FilterConfig(
              value.getSubAttributeValue("supported",
                  BOOLEAN_RESOLVER),
              value.getSubAttributeValue("maxResults",
                  INTEGER_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final FilterConfig value)
            throws InvalidResourceException
        {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(2);

          final AttributeDescriptor supportedDescriptor =
              attributeDescriptor.getSubAttribute("supported");
          subAttributes.add(
              SCIMAttribute.create(
                  supportedDescriptor,
                  BOOLEAN_RESOLVER.fromInstance(supportedDescriptor,
                      value.supported)));

          final AttributeDescriptor maxResultsDescriptor =
              attributeDescriptor.getSubAttribute("maxResults");
          subAttributes.add(
              SCIMAttribute.create(
                  maxResultsDescriptor,
                  INTEGER_RESOLVER.fromInstance(maxResultsDescriptor,
                      value.maxResults)));

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };



  /**
   * Create a <code>FilterConfig</code> instance.
   *
   * @param supported    Specifies whether the FILTER operation is supported.
   * @param maxResults   Specifies the maximum number of resources returned in
   *                     a response.
   */
  public FilterConfig(final boolean supported,
                      final long maxResults)
  {
    this.supported = supported;
    this.maxResults = maxResults;
  }



  /**
   * Indicates whether the FILTER operation is supported.
   * @return  {@code true} if the FILTER operation is supported.
   */
  public boolean isSupported()
  {
    return supported;
  }



  /**
   * Retrieves the maximum number of resources returned in a response.
   * @return The maximum number of resources returned in a response.
   */
  public long getMaxResults()
  {
    return maxResults;
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

    final FilterConfig that = (FilterConfig) o;

    if (maxResults != that.maxResults)
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
    result = 31 * result + (int) (maxResults ^ (maxResults >>> 32));
    return result;
  }



  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("FilterConfig");
    sb.append("{supported=").append(supported);
    sb.append(", maxResults=").append(maxResults);
    sb.append('}');
    return sb.toString();
  }
}
