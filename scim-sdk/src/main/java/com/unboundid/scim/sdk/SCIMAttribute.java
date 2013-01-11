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

package com.unboundid.scim.sdk;


import com.unboundid.scim.schema.AttributeDescriptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.xml.bind.DatatypeConverter;


/**
 * This class represents a System for Cross-Domain Identity Management (SCIM)
 * attribute. Attributes are categorized as either single-valued or
 * multi-valued. This class allows for the following kinds of attributes.
 *
 * <ol>
 * <li>Simple type (String, Boolean, DateTime, Integer or Binary).
 *     An example is the 'userName' attribute in the core schema.</li>
 *
 * <li>Complex type. An example is the 'name' attribute in the core
 *     schema.</li>
 *
 * <li>Multi-valued simple type. Represented using multi-valued complex values,
 *     because the values may have 'type' and 'primary' sub-attributes to
 *     distinguish each primitive value. Examples of this are the 'emails' and
 *     'photos' attributes in the core schema.</li>
 *
 * <li>Multi-valued complex type. An examples is the 'addresses' attribute in
 *     the core schema.</li>
 * </ol>
 *
 */
public final class SCIMAttribute
{
  /**
   * The mapping descriptor of this attribute.
   */
  private final AttributeDescriptor attributeDescriptor;

  /**
   * The value(s) of this attribute.
   */
  private final SCIMAttributeValue[] values;


  /**
   * Create a new instance of an attribute.
   *
   * @param descriptor    The mapping descriptor of this value.
   * @param values        The value(s) of this attribute.
   */
  private SCIMAttribute(final AttributeDescriptor descriptor,
                        final SCIMAttributeValue ... values)
  {
    this.attributeDescriptor = descriptor;
    this.values = values;
  }



  /**
   * Create an attribute.
   *
   * @param descriptor   The mapping descriptor for this attribute.
   * @param values       The value(s) of this attribute.
   *
   * @return  A new attribute.
   */
  public static SCIMAttribute create(
      final AttributeDescriptor descriptor, final SCIMAttributeValue... values)
  {
    return new SCIMAttribute(descriptor, values);
  }



  /**
   * Retrieve the name of the schema to which this attribute belongs.
   *
   * @return  The name of the schema to which this attribute belongs.
   */
  public String getSchema()
  {
    return this.attributeDescriptor.getSchema();
  }



  /**
   * Retrieve the name of this attribute. The name does not indicate which
   * schema the attribute belongs to.
   *
   * @return  The name of this attribute.
   */
  public String getName()
  {
    return this.attributeDescriptor.getName();
  }



  /**
   * Retrieves the value of this attribute. This method should only be
   * called if the attribute is single valued.
   *
   * @return  The value of this attribute.
   */
  public SCIMAttributeValue getValue()
  {
    return values[0];
  }



  /**
   * Retrieves the values of this attribute. This method should only be
   * called if the attribute is multi-valued.
   *
   * @return  The values of this attribute.
   */
  public SCIMAttributeValue[] getValues()
  {
    return values;
  }

  /**
   * Retrieves the SCIM attribute mapping of this this attribute.
   *
   * @return The attribute descriptor
   */
  public AttributeDescriptor getAttributeDescriptor() {
    return attributeDescriptor;
  }



  /**
   * Determine whether this attribute matches the provided filter parameters.
   *
   * @param filter  The filter parameters to be compared against this attribute.
   *
   * @return  {@code true} if this attribute matches the provided filter, and
   *          {@code false} otherwise.
   */
  public boolean matchesFilter(final SCIMFilter filter)
  {
    final SCIMFilterType type = filter.getFilterType();
    final List<SCIMFilter> components = filter.getFilterComponents();

    switch(type)
    {
      case AND:
        for(SCIMFilter component : components)
        {
          if(!matchesFilter(component))
          {
            return false;
          }
        }
        return true;
      case OR:
        for(SCIMFilter component : components)
        {
          if(matchesFilter(component))
          {
            return true;
          }
        }
        return false;
    }

    final String schema = filter.getFilterAttribute().getAttributeSchema();
    if (!schema.equalsIgnoreCase(getSchema()))
    {
      return false;
    }

    final String attributeName = filter.getFilterAttribute().getAttributeName();
    String subAttributeName =
        filter.getFilterAttribute().getSubAttributeName();
    if (subAttributeName == null)
    {
      subAttributeName = "value";
    }

    if (!attributeName.equalsIgnoreCase(getName()))
    {
      return false;
    }

    if (attributeDescriptor.isMultiValued())
    {
      for (final SCIMAttributeValue v : getValues())
      {
        if (v.isComplex())
        {
          final Collection<AttributeDescriptor> descriptors =
              attributeDescriptor.getSubAttributes();
          for (AttributeDescriptor descriptor : descriptors)
          {
            final SCIMAttribute a = v.getAttribute(descriptor.getName());

            if (a != null)
            {
              // This is done because the client specifies 'emails' rather
              // than 'emails.email'.
              final AttributePath childPath =
                  new AttributePath(schema, a.getName(), subAttributeName);
              if (a.matchesFilter(new SCIMFilter(type,
                                                 childPath,
                                                 filter.getFilterValue(),
                                                 filter.isQuoteFilterValue(),
                                                 filter.getFilterComponents())))
              {
                return true;
              }
            }
          }
        }
        else
        {
          AttributeDescriptor singularDescriptor =
                  AttributeDescriptor.createAttribute(getName(),
                          attributeDescriptor.getDataType(),
                          attributeDescriptor.getDescription(), getSchema(),
                          attributeDescriptor.isReadOnly(),
                          attributeDescriptor.isRequired(),
                          attributeDescriptor.isCaseExact());

          final SCIMAttribute singularAttr = create(singularDescriptor, v);
          if (singularAttr.matchesFilter(filter))
          {
            return true;
          }
        }
      }
    }
    else
    {
      final SCIMAttributeValue v = getValue();
      if (v.isComplex())
      {
        if (subAttributeName != null)
        {
          final SCIMAttribute a = v.getAttribute(subAttributeName);
          if (a != null)
          {
            final AttributePath childPath =
                new AttributePath(schema, subAttributeName, null);
            return a.matchesFilter(
                new SCIMFilter(type,
                               childPath,
                               filter.getFilterValue(),
                               filter.isQuoteFilterValue(),
                               filter.getFilterComponents()));
          }
        }
      }
      else
      {
        if (type == SCIMFilterType.PRESENCE)
        {
          return true;
        }

        final AttributeDescriptor.DataType dataType =
                  attributeDescriptor.getDataType();

        String stringValue = null;
        Double doubleValue = null;
        Long longValue = null;
        Date dateValue = null;
        Boolean boolValue = null;
        byte[] binValue = null;

        switch(dataType)
        {
          case BINARY:
            binValue = v.getBinaryValue();
            if(binValue == null)
            {
              return false;
            }
            break;
          case BOOLEAN:
            boolValue = v.getBooleanValue();
            if(boolValue == null)
            {
              return false;
            }
            break;
          case DATETIME:
            dateValue = v.getDateValue();
            if(dateValue == null)
            {
              return false;
            }
            break;
          case DECIMAL:
            doubleValue = v.getDecimalValue();
            if(doubleValue == null)
            {
              return false;
            }
            break;
          case INTEGER:
            longValue = v.getIntegerValue();
            if(longValue == null)
            {
              return false;
            }
            break;
          case STRING:
            stringValue = v.getStringValue();
            if(stringValue == null)
            {
              return false;
            }
            break;
          default:
            throw new RuntimeException(
                    "Invalid attribute data type: " + dataType);
        }

        final String filterValue = filter.getFilterValue();

        // TODO support caseExact attributes

        //Note: The code below explicitly unboxes the objects before comparing
        //      to avoid auto-unboxing and make it clear that it is just
        //      primitives being compared.
        switch (type)
        {
          case EQUALITY:
            if(stringValue != null)
            {
              return stringValue.equalsIgnoreCase(filterValue);
            }
            else if(doubleValue != null)
            {
              try
              {
                double filterValueDouble = Double.parseDouble(filterValue);
                return doubleValue.doubleValue() == filterValueDouble;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(longValue != null)
            {
              try
              {
                long filterValueLong = Long.parseLong(filterValue);
                return longValue.longValue() == filterValueLong;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(boolValue != null)
            {
              return boolValue.booleanValue() ==
                            Boolean.parseBoolean(filterValue);
            }
            else if(dateValue != null)
            {
              try
              {
                SimpleValue filterValueDate = new SimpleValue(filterValue);
                return dateValue.equals(filterValueDate.getDateValue());
              }
              catch(IllegalArgumentException e)
              {
                return false;
              }
            }
            else if(binValue != null)
            {
              //TODO: It's debatable whether this ought to just check whether
              //      the base-64 encoded string is equal, rather than checking
              //      if the bytes are equal. This seems more correct.
              try
              {
                byte[] filterValueBytes =
                            DatatypeConverter.parseBase64Binary(filterValue);
                return Arrays.equals(binValue, filterValueBytes);
              }
              catch(IllegalArgumentException e)
              {
                return false;
              }
            }
            return false;
          case CONTAINS:
            if(stringValue != null)
            {
              return StaticUtils.toLowerCase(stringValue).contains(
                        StaticUtils.toLowerCase(filterValue));
            }
            else if(doubleValue != null)
            {
              try
              {
                double filterValueDouble = Double.parseDouble(filterValue);
                return doubleValue.doubleValue() == filterValueDouble;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(longValue != null)
            {
              try
              {
                long filterValueLong = Long.parseLong(filterValue);
                return longValue.longValue() == filterValueLong;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(boolValue != null)
            {
              return boolValue.booleanValue() ==
                            Boolean.parseBoolean(filterValue);
            }
            else if(dateValue != null)
            {
              try
              {
                SimpleValue filterValueDate = new SimpleValue(filterValue);
                return dateValue.equals(filterValueDate.getDateValue());
              }
              catch(IllegalArgumentException e)
              {
                return false;
              }
            }
            else if(binValue != null)
            {
              try
              {
                byte[] filterValueBytes =
                          DatatypeConverter.parseBase64Binary(filterValue);
                return Arrays.equals(binValue, filterValueBytes);
              }
              catch(IllegalArgumentException e)
              {
                return false;
              }
            }
            return false;
          case STARTS_WITH:
            if(stringValue != null)
            {
              return StaticUtils.toLowerCase(stringValue).startsWith(
                        StaticUtils.toLowerCase(filterValue));
            }
            else if(doubleValue != null)
            {
              return false;
            }
            else if(longValue != null)
            {
              return false;
            }
            else if(boolValue != null)
            {
              return false;
            }
            else if(dateValue != null)
            {
              return false;
            }
            else if(binValue != null)
            {
              return false;
            }
            return false;
          case GREATER_THAN:
            if(stringValue != null)
            {
              return stringValue.compareToIgnoreCase(filterValue) > 0;
            }
            else if(doubleValue != null)
            {
              try
              {
                double filterValueDouble = Double.parseDouble(filterValue);
                return doubleValue.doubleValue() > filterValueDouble;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(longValue != null)
            {
              try
              {
                long filterValueLong = Long.parseLong(filterValue);
                return longValue.longValue() > filterValueLong;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(boolValue != null)
            {
              return false;
            }
            else if(dateValue != null)
            {
              try
              {
                SimpleValue filterValueDate = new SimpleValue(filterValue);
                return dateValue.after(filterValueDate.getDateValue());
              }
              catch(IllegalArgumentException e)
              {
                return false;
              }
            }
            else if(binValue != null)
            {
              return false;
            }
            return false;
          case GREATER_OR_EQUAL:
            if(stringValue != null)
            {
              return stringValue.compareToIgnoreCase(filterValue) >= 0;
            }
            else if(doubleValue != null)
            {
              try
              {
                double filterValueDouble = Double.parseDouble(filterValue);
                return doubleValue.doubleValue() >= filterValueDouble;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(longValue != null)
            {
              try
              {
                long filterValueLong = Long.parseLong(filterValue);
                return longValue.longValue() >= filterValueLong;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(boolValue != null)
            {
              return false;
            }
            else if(dateValue != null)
            {
              try
              {
                SimpleValue filterValueDate = new SimpleValue(filterValue);
                return dateValue.after(filterValueDate.getDateValue()) ||
                         dateValue.equals(filterValueDate.getDateValue());
              }
              catch(IllegalArgumentException e)
              {
                return false;
              }
            }
            else if(binValue != null)
            {
              return false;
            }
            return false;
          case LESS_THAN:
            if(stringValue != null)
            {
              return stringValue.compareToIgnoreCase(filterValue) < 0;
            }
            else if(doubleValue != null)
            {
              try
              {
                double filterValueDouble = Double.parseDouble(filterValue);
                return doubleValue.doubleValue() < filterValueDouble;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(longValue != null)
            {
              try
              {
                long filterValueLong = Long.parseLong(filterValue);
                return longValue.longValue() < filterValueLong;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(boolValue != null)
            {
              return false;
            }
            else if(dateValue != null)
            {
              try
              {
                SimpleValue filterValueDate = new SimpleValue(filterValue);
                return dateValue.before(filterValueDate.getDateValue());
              }
              catch(IllegalArgumentException e)
              {
                return false;
              }
            }
            else if(binValue != null)
            {
              return false;
            }
            return false;
          case LESS_OR_EQUAL:
            if(stringValue != null)
            {
              return stringValue.compareToIgnoreCase(filterValue) <= 0;
            }
            else if(doubleValue != null)
            {
              try
              {
                double filterValueDouble = Double.parseDouble(filterValue);
                return doubleValue.doubleValue() <= filterValueDouble;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(longValue != null)
            {
              try
              {
                long filterValueLong = Long.parseLong(filterValue);
                return longValue.longValue() <= filterValueLong;
              }
              catch(NumberFormatException e)
              {
                return false;
              }
            }
            else if(boolValue != null)
            {
              return false;
            }
            else if(dateValue != null)
            {
              try
              {
                SimpleValue filterValueDate = new SimpleValue(filterValue);
                return dateValue.before(filterValueDate.getDateValue()) ||
                         dateValue.equals(filterValueDate.getDateValue());
              }
              catch(IllegalArgumentException e)
              {
                return false;
              }
            }
            else if(binValue != null)
            {
              return false;
            }
            return false;
        }
      }
    }

    return false;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SCIMAttribute that = (SCIMAttribute) o;

    return attributeDescriptor.equals(that.attributeDescriptor) &&
        Arrays.equals(values, that.values);
  }

  @Override
  public int hashCode() {
    int result = attributeDescriptor.hashCode();
    result = 31 * result + (values != null ?
        Arrays.hashCode(values) : 0);
    return result;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("SCIMAttribute");
    sb.append("{attribute=").append(attributeDescriptor.getSchema());
    sb.append(SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE);
    sb.append(attributeDescriptor.getName());
    sb.append(", values=").append(values == null ? "null" :
        Arrays.asList(values).toString());
    sb.append('}');
    return sb.toString();
  }
}
