/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
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
  SimpleValue(final String stringValue)
  {
    this.value = stringValue;
  }



  /**
   * Create a simple boolean value.
   *
   * @param booleanValue  The boolean value.
   */
  SimpleValue(final Boolean booleanValue)
  {
    this.value = booleanValue.toString();
  }



  /**
   * Create a simple datetime value.
   *
   * @param dateValue  The datetime value.
   */
  SimpleValue(final Date dateValue)
  {
    final Calendar calendar = new GregorianCalendar(utcTimeZone);
    calendar.setTime(dateValue);
    this.value = DatatypeConverter.printDateTime(calendar);
  }


  /**
   * Create a simple binary value.
   *
   * @param bytes  The binary value.
   */
  SimpleValue(final byte[] bytes)
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



  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("SimpleValue");
    sb.append("{value=").append(value);
    sb.append('}');
    return sb.toString();
  }
}
