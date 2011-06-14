/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;


import com.unboundid.scim.config.AttributeDescriptor;

/**
 * This class represents a Simple Cloud Identity Management (SCIM) attribute.
 * Attributes are categorized as either Singular (those that may take only a
 * single value), or Plural (those that may take multiple values). This class
 * allows for the following kinds of attributes.
 *
 * <ol>
 * <li>Singular simple type (String, Boolean, DateTime). An example is the
 *     'userName' attribute in the core schema.</li>
 *
 * <li>Singular complex type. An example is the 'name' attribute in the core
 *     schema.</li>
 *
 * <li>Plural simple type. The only example is the 'schemas' attribute, which
 *     is used in JSON representation but not in XML representation.</li>
 *
 * <li>Plural complex type. Examples are the 'emails' attribute and the
 *     'addresses' attribute. Although each value of 'emails' is a string, it
 *     is complex by virtue of the standard 'type' and 'primary'
 *     sub-attributes.</li>
 * </ol>
 *
 */
public final class SCIMAttribute
{
  /**
   * The single value of this attribute, or {@code null} if this attribute is
   * a plural attribute.
   */
  private final SCIMAttributeValue singleValue;

  /**
   * The plural values of this attribute, or {@code null} if this attribute is
   * a singular attribute.
   */
  private final SCIMAttributeValue[] pluralValues;

  private final AttributeDescriptor attributeDescriptor;


  /**
   * Create a new instance of an attribute.
   *
   * @param descriptor  The mappibg descriptor of this value
   * @param singleValue   The single value of this attribute, or {@code null}
   *                      if this attribute is a plural attribute.
   * @param pluralValues  The plural values of this attribute, or empty if this
   *                      attribute is a singular attribute.
   */
  private SCIMAttribute(final AttributeDescriptor descriptor,
                        final SCIMAttributeValue singleValue,
                        final SCIMAttributeValue ... pluralValues)
  {
    this.attributeDescriptor = descriptor;
    this.singleValue = singleValue;
    if (singleValue == null)
    {
      this.pluralValues = pluralValues;
    }
    else
    {
      this.pluralValues = null;
    }
  }



  /**
   * Create a singular attribute.
   * @param descriptor The mapping descriptorof this attribute.
   * @param value     The value of this attribute.
   *
   * @return  A new singular attribute.
   */
  public static SCIMAttribute createSingularAttribute(
     final AttributeDescriptor descriptor, final SCIMAttributeValue value)
  {
    return new SCIMAttribute(descriptor, value);
  }



  /**
   * Create a plural attribute.
   *
   * @param values    The mapping descriptor for this attribute.
   * @param values    The values of this attribute.
   *
   * @return  A new plural attribute.
   */
  public static SCIMAttribute createPluralAttribute(
      final AttributeDescriptor descriptor,
      final SCIMAttributeValue ... values)
  {
    return new SCIMAttribute(descriptor, null, values);
  }



  /**
   * Retrieve the name of the schema to which this attribute belongs, or
   * {@code null} if the attribute is in the core schema.
   *
   * @return  The name of the schema to which this attribute belongs, or
   *          {@code null} if the attribute is in the core schema.
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
    return this.attributeDescriptor.getExternalAttributeName();
  }



  /**
   * Indicates whether this attribute is singular or plural. This method
   * determines which of the {@link #getSingularValue()} or
   * {@link #getPluralValues()} methods may be used.
   *
   * @return {@code true} if this attribute is a plural attribute, or {@code
   *         false} if this attribute is a singular attribute.
   */
  public boolean isPlural()
  {
    return singleValue == null;
  }



  /**
   * Retrieves the singular value of this attribute. This method should only be
   * called if the {@link #isPlural()} method returns {@code false}.
   *
   * @return  The singular value of this attribute, or {@code null} if this
   *          attribute is a plural attribute.
   */
  public SCIMAttributeValue getSingularValue()
  {
    return singleValue;
  }



  /**
   * Retrieves the plural values of this attribute. This method should only be
   * called if the {@link #isPlural()} method returns {@code true}.
   *
   * @return  The plural values of this attribute, or {@code null} if this
   *          attribute is a singular attribute.
   */
  public SCIMAttributeValue[] getPluralValues()
  {
    return pluralValues;
  }

  /**
   * Retrieves the SCIM attribute mapping of this this attribute.
   *
   * @return The attribute descriptor
   */
  public AttributeDescriptor getAttributeDescriptor() {
    return attributeDescriptor;
  }
}
