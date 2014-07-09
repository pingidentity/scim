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

import java.util.Date;
import java.util.HashMap;

/**
 * This class represents a SCIM complex attribute value.
 */
public class ComplexValue
    extends HashMap<String,SimpleValue> {


  /**
   * Overridden from HashMap.  Converts string-valued keys to lower-case.
   * @param name the sub-attribute name.
   * @param value the sub-attribute value as a SimpleValue object.
   * @return the previous value of the sub-attribute, or null if there was no
   * previous mapping for the sub-attribute name.
   */
  @Override
  public SimpleValue put(
      final String name,
      final SimpleValue value) {

    return super.put(name.toLowerCase(), value);
  }


  /**
   * Returns a SimpleValue for the specified sub-attribute name.
   * The attribute name is case-insensitive.
   * @param name sub-attribute name.
   * @return SimpleValue object, or null if no sub-attribute
   * exists with the specified name.
   */
  public SimpleValue get(final String name) {
    // overridden from HashMap to convert key to lower-case
    return super.get(name.toLowerCase());
  }


  /**
   * Returns true if the ComplexAttribute contains a sub-attribute with the
   * specified name.
   * @param name sub-attribute name.
   * @return true if the sub-attribute exists, false otherwise.
   */
  public boolean containsKey(final String name) {
    // overridden from HashMap to convert key to lower-case
    return super.containsKey(name.toLowerCase());
  }


  /**
   * Set the value of a sub-attribute as a string.
   * @param name sub-attribute name.
   * @param value sub-attribute value.
   */
  public void putStringValue(final String name, final String value) {
    put(name, new SimpleValue(value));
  }


  /**
   * Set the value of a Boolean-valued sub-attribute.
   * @param name sub-attribute name.
   * @param value sub-attribute value.
   */
  public void putBooleanValue(final String name, final Boolean value) {
    put(name, new SimpleValue(value));
  }


  /**
   * Set the value of a Double-valued sub-attribute. This method can be used
   * for attributes of the SCIM type 'Decimal'.
   * @param name sub-attribute name.
   * @param value sub-attribute value.
   */
  public void putDoubleValue(final String name, final Double value) {
    put(name, new SimpleValue(value));
  }


  /**
   * Set the value of a Long-valued sub-attribute.  This method can be used
   * for attributes of the SCIM type 'Integer'.
   * @param name sub-attribute name.
   * @param value sub-attribute value.
   */
  public void putLongValue(final String name, final Long value) {
    put(name, new SimpleValue(value));
  }

  /**
   * Set the value of a Date-valued sub-attribute.  This method can be used
   * for attributes of the SCIM type 'DateTime'.
   * @param name sub-attribute name.
   * @param value sub-attribute value.
   */
  public void puDateValue(final String name, final Date value) {
    put(name, new SimpleValue(value));
  }


  /**
   * Set the value of a Binary-valued sub-attribute.
   * @param name sub-attribute name.
   * @param value sub-attribute value.
   */
  public void putBinaryValue(final String name, final byte[] value) {
    put(name, new SimpleValue(value));
  }


  /**
   * Get the value of a sub-attribute as a string.
   * @param name sub-attribute name.
   * @return the sub-attribute value as a String, or null if no sub-attribute
   * exists with the specified name.
   */
  public String getStringValue(final String name) {
    SimpleValue sub = get(name);
    if (sub == null) {
      return null;
    }
    return sub.getStringValue();
  }


  /**
   * Get the value of a Boolean-valued sub-attribute.
   * @param name sub-attribute name.
   * @return the sub-attribute value as a Boolean, or null if no sub-attribute
   * exists with the specified name.
   */
  public Boolean getBooleanValue(final String name) {
    SimpleValue sub = get(name);
    if (sub == null) {
      return null;
    }
    return sub.getBooleanValue();
  }


  /**
   * Get the value of a Double-valued sub-attribute.  This method can be used
   * for attributes of the SCIM type 'Decimal'.
   * @param name sub-attribute name.
   * @return the sub-attribute value as a Double, or null if no sub-attribute
   * exists with the specified name.
   */
  public Double getDoubleValue(final String name) {
    SimpleValue sub = get(name);
    if (sub == null) {
      return null;
    }
    return sub.getDoubleValue();
  }


  /**
   * Get the value of a Long-valued sub-attribute.  This method can be used
   * for attributes of the SCIM type 'Integer'.
   * @param name sub-attribute name.
   * @return the sub-attribute value as a Long, or null if no sub-attribute
   * exists with the specified name.
   */
  public Long getLongValue(final String name) {
    SimpleValue sub = get(name);
    if (sub == null) {
      return null;
    }
    return sub.getLongValue();
  }


  /**
   * Get the value of a Date-valued sub-attribute.  This method can be used
   * for attributes of the SCIM type 'DateTime'.
   * @param name sub-attribute name.
   * @return the sub-attribute value as a Date object, or null if no
   * sub-attribute exists with the specified name.
   */
  public Date getDateValue(final String name) {
    SimpleValue sub = get(name);
    if (sub == null) {
      return null;
    }
    return sub.getDateValue();
  }


  /**
   * Get the value of a Binary-valued sub-attribute.
   * @param name sub-attribute name.
   * @return the sub-attribute value as a byte array, or null if no
   * sub-attribute exists with the specified name.
   */
  public byte[] getBinaryValue(final String name) {
    SimpleValue sub = get(name);
    if (sub == null) {
      return null;
    }
    return sub.getBinaryValue();
  }
}
