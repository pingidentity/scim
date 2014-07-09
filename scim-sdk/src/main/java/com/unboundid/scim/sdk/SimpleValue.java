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

package com.unboundid.scim.sdk;

import javax.xml.bind.DatatypeConverter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;



/**
 * This class represents a SCIM simple value. Simple values can be String,
 * Boolean, DateTime or Binary.
 */
public class SimpleValue
{
  /**
   * The UTC time zone.
   */
  private static TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");

  /**
   * The simple value stored as a String or byte[].
   */
  private final Object value;



  /**
   * Create a simple string value.
   *
   * @param stringValue  The string value.
   */
  public SimpleValue(final String stringValue)
  {
    this.value = stringValue;
  }



  /**
   * Create a simple boolean value.
   *
   * @param booleanValue  The boolean value.
   */
  public SimpleValue(final Boolean booleanValue)
  {
    this.value = booleanValue.toString();
  }



  /**
   * Create a simple datetime value.
   *
   * @param dateValue  The datetime value.
   */
  public SimpleValue(final Date dateValue)
  {
    final Calendar calendar = new GregorianCalendar(utcTimeZone);
    calendar.setTime(dateValue);
    this.value = DatatypeConverter.printDateTime(calendar);
  }


  /**
   * Create a simple integer value.
   *
   * @param longValue The integer value, as a Long.
   */
  public SimpleValue(final Long longValue)
  {
    this.value = longValue.toString();
  }


  /**
   * Create a simple integer value.
   *
   * @param intValue the integer value.
   */
  public SimpleValue(final Integer intValue)
  {
    this.value = intValue.toString();
  }

  /**
   * Create a simple decimal value.
   *
   * @param doubleValue The decimal value, as a Double.
   */
  public SimpleValue(final Double doubleValue)
  {
    this.value = doubleValue.toString();
  }


  /**
   * Create a simple binary value.
   *
   * @param bytes  The binary value.
   */
  public SimpleValue(final byte[] bytes)
  {
    this.value = bytes;
  }



  /**
   * Retrieves the simple value as a string.
   *
   * @return  The simple value as a string.
   */
  public String getStringValue()
  {
    if (value instanceof byte[])
    {
      return DatatypeConverter.printBase64Binary((byte[])value);
    }
    else
    {
      return (String)value;
    }
  }



  /**
   * Retrieves the simple value as a boolean.
   *
   * @return  The simple value as a boolean.
   */
  public Boolean getBooleanValue()
  {
    return Boolean.valueOf((String)value);
  }



  /**
   * Retrieves the simple value as a double.
   *
   * @return  The simple value as a double.
   */
  public Double getDoubleValue()
  {
    return Double.valueOf((String)value);
  }



  /**
   * Retrieves the simple value as a long.
   *
   * @return  The simple value as a long.
   */
  public Long getLongValue()
  {
    return Long.valueOf((String)value);
  }



  /**
   * Retrieves the simple value as a date.
   *
   * @return  The simple value as a date.
   */
  public Date getDateValue()
  {
    final Calendar calendar = DatatypeConverter.parseDateTime((String)value);
    calendar.setTimeZone(utcTimeZone);
    return calendar.getTime();
  }



  /**
   * Retrieves the simple binary value.
   *
   * @return  The simple binary value.
   */
  public byte[] getBinaryValue()
  {
    return (byte[])value;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("SimpleValue");
    sb.append("{value=").append(value);
    sb.append('}');
    return sb.toString();
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

    SimpleValue that = (SimpleValue) o;

    if (!value.equals(that.value)) {
      return false;
    }

    return true;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return value.hashCode();
  }
}
