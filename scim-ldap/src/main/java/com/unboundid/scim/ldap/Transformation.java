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

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SimpleValue;
import com.unboundid.util.ByteString;



/**
 * An abstract class defining an interface for transforming SCIM values to
 * LDAP values and vice-versa.
 */
public abstract class Transformation
{
  /**
   * Create a transformation from the name of a class that extends the
   * {@code Transformation} abstract class.
   *
   * @param className  The name of a class that extends {@code Transformation}.
   *
   * @return  A new instance of the transformation class.
   */
  public static Transformation create(final String className)
  {
    if (className == null)
    {
      return new DefaultTransformation();
    }

    Class clazz;
    try
    {
      clazz = Class.forName(className);
    }
    catch (ClassNotFoundException e)
    {
      Debug.debugException(e);
      throw new IllegalArgumentException(
          "Class '" + className + "' not found", e);
    }

    final Object object;
    try
    {
      object = clazz.newInstance();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new IllegalArgumentException(
          "Cannot create instance of class '" + className + "'", e);
    }

    if (!(object instanceof Transformation))
    {
      throw new IllegalArgumentException(
          "Class '" + className + "' is not a Transformation");
    }

    return (Transformation)object;
  }



  /**
   * Transform an LDAP value to a SCIM value.
   *
   * @param descriptor  The SCIM attribute descriptor for the value.
   * @param byteString  The LDAP value as a byte string.
   *
   * @return  The SCIM value.
   */
  public abstract SimpleValue toSCIMValue(
      final AttributeDescriptor descriptor,
      final ByteString byteString);

  /**
   * Transform a SCIM value to an LDAP value.
   *
   * @param descriptor   The SCIM attribute descriptor for the value.
   * @param simpleValue  The SCIM value.
   *
   * @return  The LDAP value as an ASN1 octet string.
   */
  public abstract ASN1OctetString toLDAPValue(
      final AttributeDescriptor descriptor,
      final SimpleValue simpleValue);

  /**
   * Transform a SCIM filter value to an LDAP filter value.
   *
   * @param scimFilterValue  The SCIM filter value.
   *
   * @return  The LDAP filter value as a string.
   */
  public abstract String toLDAPFilterValue(final String scimFilterValue);
}
