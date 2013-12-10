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

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SimpleValue;
import com.unboundid.util.ByteString;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Attribute mapping transformations may be used to alter the value of mapped
 * attributes. Transformations may be performed on an SCIM attribute value
 * when mapping SCIM resources to LDAP entries and vice versa. When no
 * transformation class is specified in the resources configuration file,
 * the DefaultTransformation implementation is used. It will simply return
 * the value as is without any alterations.
 * <BR><BR>
 * Transformations are often useful when the syntax of an attribute is different
 * between SCIM and LDAP. The LDAP <tt>GeneralizedTime</tt> attribute syntax is
 * a good example where transformations are necessary when mapping those
 * attributes.
 * <BR><BR>
 * To use a custom transformation class, use the <tt>transform</tt> attribute to
 * specify the implementation class in any <tt>mapping</tt> or
 * <tt>subMapping</tt> configuration elements. For example:
 * <BR><BR>
 * <PRE>
 * &lt;subMapping name=&quot;formatted&quot;
 *  ldapAttribute=&quot;postalAddress&quot;
 *  transform=&quot;com.unboundid.scim.ldap.PostalAddressTransformation&quot;
 * &#47;&gt;
 * </PRE>
 * <BR><BR>
 * This API is volatile and could change in future releases.
 */
public abstract class Transformation
{
  /**
   * The map of arguments provided to this transformation. This maps the
   * element name to its value. See getArguments() for an example.
   */
  private final Map<String,String> arguments = new HashMap<String,String>();

  /**
   * Create a transformation from the name of a class that extends the
   * {@code Transformation} abstract class.
   *
   * @param className  The name of a class that extends {@code Transformation}.
   * @param args       The set of arguments that are configured for this
   *                   {@code Transformation}.
   *
   * @return  A new instance of the transformation class.
   */
  public static Transformation create(final String className,
                                      final List<Object> args)
  {
    if (className == null)
    {
      return new DefaultTransformation();
    }

    Class clazz;
    try
    {
      clazz = Class.forName(className, true,
          Transformation.class.getClassLoader());
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

    Transformation t = (Transformation)object;
    if(args != null)
    {
      for(Object o : args)
      {
        Element e = (Element) o;
        String name = e.getTagName();
        String value = e.getTextContent();
        t.arguments.put(name, value);
      }
    }
    return t;
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

  /**
   * Returns the arguments map for this Transformation. The arguments map is
   * constructed from the child elements of the <mapping> element.
   * For example, a derivation like the following:
   * <PRE>
   *   &lt;mapping ldapAttribute="title"
   *               transform="com.example.ExampleTransformation"&gt;
   *     &lt;key1&gt;value1&lt;/key1&gt;
   *     &lt;key2&gt;value2&lt;/key2&gt;
   *   &lt;/mapping&gt;
   * </PRE>
   * would have an arguments map containing 'key1' and 'key2' with values
   * 'value1' and 'value2'.
   *
   * @return a map of argument names to argument values. This is modifiable,
   *         and will never be null.
   */
  public final Map<String,String> getArguments()
  {
    return arguments;
  }
}
