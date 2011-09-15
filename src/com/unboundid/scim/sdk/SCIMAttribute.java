/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;


import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.Schema;
import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.ldap.AttributePath;
import com.unboundid.scim.ldap.SCIMFilter;
import com.unboundid.scim.ldap.SCIMFilterType;

import java.util.List;



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
   * The mapping descriptor of this attribute.
   */
  private final AttributeDescriptor attributeDescriptor;

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


  /**
   * Create a new instance of an attribute.
   *
   * @param descriptor    The mapping descriptor of this value.
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
   * Create a simple singular attribute from a string value.
   *
   * @param schemaURI  The URI for the schema that defines the attribute.
   * @param name       The name of the attribute.
   * @param value      The value of the attribute.
   *
   * @return  A new simple singular attribute containing a string value.
   */
  public static SCIMAttribute createSingularStringAttribute(
      final String schemaURI, final String name, final String value)
  {
    final SchemaManager schemaManager = SchemaManager.instance();
    final Schema schema = schemaManager.getSchema(schemaURI);
    return new SCIMAttribute(schema.getAttribute(name),
                             SCIMAttributeValue.createStringValue(value));
  }



  /**
   * Create a simple singular attribute from a boolean value.
   *
   * @param schemaURI  The URI for the schema that defines the attribute.
   * @param name       The name of the attribute.
   * @param value      The value of the attribute.
   *
   * @return  A new simple singular attribute containing a string value.
   */
  public static SCIMAttribute createSingularBooleanAttribute(
      final String schemaURI, final String name, final boolean value)
  {
    final SchemaManager schemaManager = SchemaManager.instance();
    final Schema schema = schemaManager.getSchema(schemaURI);
    return new SCIMAttribute(schema.getAttribute(name),
                             SCIMAttributeValue.createBooleanValue(value));
  }



  /**
   * Create a singular attribute.
   *
   * @param descriptor The mapping descriptor of this attribute.
   * @param value      The value of this attribute.
   *
   * @return  A new singular attribute.
   */
  public static SCIMAttribute createSingularAttribute(
     final AttributeDescriptor descriptor, final SCIMAttributeValue value)
  {
    return new SCIMAttribute(descriptor, value);
  }



  /**
   * Create a singular attribute.
   *
   * @param schemaURI  The URI for the schema that defines the attribute.
   * @param name    The name of the attribute.
   * @param value   The value of the attribute.
   *
   * @return  A new singular attribute.
   */
  public static SCIMAttribute createSingularAttribute(
     final String schemaURI, final String name, final SCIMAttributeValue value)
  {
    final SchemaManager schemaManager = SchemaManager.instance();
    final Schema schema = schemaManager.getSchema(schemaURI);
    return new SCIMAttribute(schema.getAttribute(name), value);
  }



  /**
   * Create a plural attribute.
   *
   * @param descriptor   The mapping descriptor for this attribute.
   * @param values       The values of this attribute.
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
   * Create a plural attribute.
   *
   * @param schemaURI  The URI for the schema that defines the attribute.
   * @param name       The name of the attribute.
   * @param values     The values of this attribute.
   *
   * @return  A new plural attribute.
   */
  public static SCIMAttribute createPluralAttribute(
      final String schemaURI, final String name,
      final SCIMAttributeValue ... values)
  {
    final SchemaManager schemaManager = SchemaManager.instance();
    final Schema schema = schemaManager.getSchema(schemaURI);
    return new SCIMAttribute(schema.getAttribute(name), null, values);
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
    if (!filter.getFilterAttribute().getAttributeSchema().equals(getSchema()))
    {
      return false;
    }

    final String schema = filter.getFilterAttribute().getAttributeSchema();
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

    if (isPlural())
    {
      for (final SCIMAttributeValue v : getPluralValues())
      {
        if (v.isComplex())
        {
          final List<AttributeDescriptor> descriptors =
              attributeDescriptor.getComplexAttributeDescriptors();
          for (AttributeDescriptor descriptor : descriptors)
          {
            final SCIMAttribute a = v.getAttribute(descriptor.getName());

            if (a != null)
            {
              // This is done because the client specifies 'emails' rather
              // than 'emails.email'.
              final AttributePath childPath =
                  new AttributePath(schema, a.getName(), subAttributeName);
              if (a.matchesFilter(new SCIMFilter(filter.getFilterType(),
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
      }
    }
    else
    {
      final SCIMAttributeValue v = getSingularValue();
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
                new SCIMFilter(filter.getFilterType(),
                               childPath,
                               filter.getFilterValue(),
                               filter.isQuoteFilterValue(),
                               filter.getFilterComponents()));
          }
        }
      }
      else
      {
        final SCIMFilterType filterType = filter.getFilterType();
        if (filterType == SCIMFilterType.PRESENCE)
        {
          return true;
        }

        final String filterValue = filter.getFilterValue();
        String attributeValue = v.getStringValue();

        if (attributeValue != null)
        {
          // TODO support caseExact attributes
          switch (filterType)
          {
            case EQUALITY:
              return attributeValue.equalsIgnoreCase(filterValue);
            case CONTAINS:
              return attributeValue.toLowerCase().contains(
                  filterValue.toLowerCase());
            case STARTS_WITH:
              return attributeValue.toLowerCase().startsWith(
                  filterValue.toLowerCase());
          }
        }
      }
    }

    return false;
  }
}
