/*
 * Copyright 2011-2015 UnboundID Corp.
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

package com.unboundid.scim.schema;

import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.unboundid.scim.sdk.StaticUtils.toLowerCase;



/**
 * This class provides methods that describe the schema for a SCIM attribute.
 * It may be used to help read and write SCIM attributes in their external XML
 * and JSON representation, and to convert SCIM attributes to and from LDAP
 * attributes.
 */
public final class AttributeDescriptor {

  /**
   * Defines the set of well known SCIM supported datatypes.
   */
  public static enum DataType {
    /**
     * String data type.
     */
    STRING,
    /**
     * Boolean data type.
     */
    BOOLEAN,
    /**
     * Date Time data type.
     */
    DATETIME,
    /**
     * Decimal data type.
     */
    DECIMAL,
    /**
     * Integer data type.
     */
    INTEGER,
    /**
     * Binary data type.
     */
    BINARY,
    /**
     * Complex data type.
     */
    COMPLEX;

    /**
     * Parses a supplied data type into a SCIM defined data type.
     * @param type The type to convert
     *
     * @return The DataType or null if not supported
     */
    public static DataType parse(final String type) {
      try {
        return DataType.valueOf(type.toUpperCase());
      } catch (Exception e) {
        return null;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  static final class SubAttributeDescriptorResolver
      extends AttributeValueResolver<AttributeDescriptor>
  {
    private final String schema;

    /**
     * Construct a new sub attribute resolver.
     *
     * @param schema The schema of the parent attribute.
     */
    SubAttributeDescriptorResolver(final String schema) {
      this.schema = schema;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SCIMAttributeValue fromInstance(
        final AttributeDescriptor attributeDescriptor,
        final AttributeDescriptor value) throws InvalidResourceException {

      final List<SCIMAttribute> attributes = new ArrayList<SCIMAttribute>(9);

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("name"),
          SCIMAttributeValue.createStringValue(value.getName())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("type"),
          SCIMAttributeValue.createStringValue(
              value.getDataType().toString())));


      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("multiValued"),
          SCIMAttributeValue.createBooleanValue(value.isMultiValued())));

      if (value.isMultiValued())
      {
        attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("multiValuedAttributeChildName"),
          SCIMAttributeValue.createStringValue(
                  value.getMultiValuedChildName())));
      }

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("description"),
          SCIMAttributeValue.createStringValue(value.getDescription())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("readOnly"),
          SCIMAttributeValue.createBooleanValue(value.isReadOnly())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("required"),
          SCIMAttributeValue.createBooleanValue(value.isRequired())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("caseExact"),
          SCIMAttributeValue.createBooleanValue(value.isCaseExact())));

      if(value.getCanonicalValues() != null)
      {
        final AttributeDescriptor canonicalValuesAttributeDescriptor =
            attributeDescriptor.getSubAttribute("canonicalValues");
        final SCIMAttributeValue[] canonicalValues =
            new SCIMAttributeValue[value.getCanonicalValues().size()];
        int i = 0;
        for(Entry<String> canonicalValue : value.getCanonicalValues())
        {
          canonicalValues[i++] = Entry.STRINGS_RESOLVER.fromInstance(
              canonicalValuesAttributeDescriptor, canonicalValue);
        }
        attributes.add(SCIMAttribute.create(
            canonicalValuesAttributeDescriptor, canonicalValues));
      }

      return SCIMAttributeValue.createComplexValue(attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeDescriptor toInstance(final SCIMAttributeValue value) {

      DataType dataType = DataType.parse(value.getSubAttributeValue(
              "type", AttributeValueResolver.STRING_RESOLVER));

      return new AttributeDescriptor(

          value.getSubAttributeValue(
             "name", AttributeValueResolver.STRING_RESOLVER),

          dataType,

          value.getSubAttributeValue(
              "multiValued", AttributeValueResolver.BOOLEAN_RESOLVER),

          value.getSubAttributeValue(
              "multiValuedAttributeChildName",
                  AttributeValueResolver.STRING_RESOLVER),

          value.getSubAttributeValue(
              "description", AttributeValueResolver.STRING_RESOLVER),

          schema,

          value.getSubAttributeValue(
              "readOnly", AttributeValueResolver.BOOLEAN_RESOLVER),

          value.getSubAttributeValue(
              "required", AttributeValueResolver.BOOLEAN_RESOLVER),

          value.getSubAttributeValue(
              "caseExact", AttributeValueResolver.BOOLEAN_RESOLVER),

          value.getSubAttributeValues(
              "canonicalValues", Entry.STRINGS_RESOLVER),

          Arrays.asList(AttributeDescriptor.createSubAttribute(
               "value", dataType, "The attribute's significant value",
                SCIMConstants.SCHEMA_URI_CORE, false, true, false)));
    }
  }

  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>AttributeDescriptor</code> instances.
   */
  public static final AttributeValueResolver<AttributeDescriptor>
      ATTRIBUTE_DESCRIPTOR_RESOLVER = new AttributeDescriptorResolver(false);

  static final class AttributeDescriptorResolver extends
      AttributeValueResolver<AttributeDescriptor>
  {
    private final boolean allowNesting;

    /**
     * Create a new AttributeDescriptorResolver.
     *
     * @param allowNesting <code>true</code> to allow nesting of complex
     *                     types or <code>false</code> otherwise.
     */
    AttributeDescriptorResolver(final boolean allowNesting) {
      this.allowNesting = allowNesting;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AttributeDescriptor toInstance(final SCIMAttributeValue value) {
      String schemaValue = value.getSubAttributeValue(
          "schema", AttributeValueResolver.STRING_RESOLVER);

      boolean multiValued = value.getSubAttributeValue(
          "multiValued", AttributeValueResolver.BOOLEAN_RESOLVER);

      String multiValuedChildName = null;
      if (multiValued)
      {
        multiValuedChildName = value.getSubAttributeValue(
            "multiValuedAttributeChildName",
            AttributeValueResolver.STRING_RESOLVER);
      }

      return new AttributeDescriptor(

          value.getSubAttributeValue(
              "name", AttributeValueResolver.STRING_RESOLVER),

          DataType.parse(value.getSubAttributeValue(
              "type", AttributeValueResolver.STRING_RESOLVER)),

          multiValued, multiValuedChildName,

          value.getSubAttributeValue(
              "description", AttributeValueResolver.STRING_RESOLVER),

          schemaValue,

          value.getSubAttributeValue(
              "readOnly", AttributeValueResolver.BOOLEAN_RESOLVER),

          value.getSubAttributeValue(
              "required", AttributeValueResolver.BOOLEAN_RESOLVER),

          value.getSubAttributeValue(
              "caseExact", AttributeValueResolver.BOOLEAN_RESOLVER),

          null,

          value.getSubAttributeValues(
              allowNesting ? "attributes" : "subAttributes",
              allowNesting ? this :
                  new SubAttributeDescriptorResolver(schemaValue)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SCIMAttributeValue fromInstance(
        final AttributeDescriptor attributeDescriptor,
        final AttributeDescriptor value) throws InvalidResourceException {

      final List<SCIMAttribute> attributes =
          new ArrayList<SCIMAttribute>(10);

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("name"),
          SCIMAttributeValue.createStringValue(value.getName())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("type"),
          SCIMAttributeValue.createStringValue(
              value.getDataType().toString())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("multiValued"),
          SCIMAttributeValue.createBooleanValue(value.isMultiValued())));

      if(value.isMultiValued())
      {
        attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("multiValuedAttributeChildName"),
          SCIMAttributeValue.createStringValue(
              value.getMultiValuedChildName())));
      }

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("description"),
          SCIMAttributeValue.createStringValue(value.getDescription())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("schema"),
          SCIMAttributeValue.createStringValue(value.getSchema())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("readOnly"),
          SCIMAttributeValue.createBooleanValue(value.isReadOnly())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("required"),
          SCIMAttributeValue.createBooleanValue(value.isRequired())));

      attributes.add(SCIMAttribute.create(
          attributeDescriptor.getSubAttribute("caseExact"),
          SCIMAttributeValue.createBooleanValue(value.isCaseExact())));

      if(value.subAttributes != null)
      {
        final AttributeDescriptor subAttributesDescriptor =
            allowNesting ? attributeDescriptor :
            attributeDescriptor.getSubAttribute("subAttributes");
        final SCIMAttributeValue[] subAttributeValues =
            new SCIMAttributeValue[value.subAttributes.size()];
        int i = 0;
        for(AttributeDescriptor subAttribute : value.subAttributes.values())
        {
          subAttributeValues[i++] = (allowNesting ? this :
              new SubAttributeDescriptorResolver(value.getSchema())).
              fromInstance(subAttributesDescriptor, subAttribute);
        }
        attributes.add(SCIMAttribute.create(
            subAttributesDescriptor, subAttributeValues));
      }

      return SCIMAttributeValue.createComplexValue(attributes);
    }
  }

  private final String schema;

  private final String name;

  private final String description;

  private final boolean readOnly;

  private final boolean required;

  private final boolean multiValued;

  private final String multiValuedChildName;

  private final boolean caseExact;

  private final DataType dataType;

  private final Map<String, AttributeDescriptor> subAttributes;

  private final Collection<Entry<String>> canonicalValues;

  /**
   * Construct a new AttributeDescriptor instance with the provided info.
   *
   * @param name                 The attribute's name.
   * @param dataType             The attribute's data type.
   * @param multiValued          Whether the attribute is multiValued.
   * @param multiValuedChildName The child XML element name for multi-valued
   *                             attributes.
   * @param description          The attribute's human readable description.
   * @param schema               The attribute's associated schema.
   * @param readOnly             Whether the attribute is mutable.
   * @param required             Whether the attribute is required.
   * @param caseExact            Whether the string attribute is case sensitive.
   * @param canonicalValues      A list of canonical type values.
   * @param subAttributes        A list specifying the contained attributes.
   */
  private AttributeDescriptor(final String name, final DataType dataType,
                              final boolean multiValued,
                              final String multiValuedChildName,
                              final String description, final String schema,
                              final boolean readOnly, final boolean required,
                              final boolean caseExact,
                              final Collection<Entry<String>> canonicalValues,
                            final Collection<AttributeDescriptor> subAttributes)
  {
    this.name = name;
    this.dataType = dataType;
    this.multiValued = multiValued;
    this.multiValuedChildName = multiValuedChildName;
    this.description = description;
    this.schema = schema;
    this.readOnly = readOnly;
    this.required = required;
    this.caseExact = caseExact;

    if(canonicalValues != null && !canonicalValues.isEmpty())
    {
      this.canonicalValues = canonicalValues;
    }
    else
    {
      this.canonicalValues = null;
    }

    if(subAttributes != null && !subAttributes.isEmpty())
    {
      this.subAttributes =
          new LinkedHashMap<String, AttributeDescriptor>(subAttributes.size());
      for(AttributeDescriptor attributeDescriptor : subAttributes)
      {
        this.subAttributes.put(toLowerCase(
            attributeDescriptor.getName()),
            attributeDescriptor);
      }
    }
    else
    {
      this.subAttributes = null;
    }
  }

  /**
   * The URI for the schema that defines the SCIM attribute.
   *
   * @return The URI for the schema that defines the SCIM attribute.
   *         It is never {@code null}.
   */
  public String getSchema() {
    return schema;
  }

  /**
   * The attribute name to be used in any external representation of the SCIM
   * attribute.
   *
   * @return The attribute name to be used in any external representation of
   *         the SCIM attribute. It is never {@code null}.
   */
  public String getName() {
    return name;
  }

  /**
   * Indicates whether the attribute is a multi-valued attribute.
   *
   * @return {@code true} if the attribute is multi-valued.
   */
  public boolean isMultiValued() {
    return multiValued;
  }

  /**
   * The child XML element name for multi-valued attributes; e.g., the
   * 'emails' attribute value is 'email', 'phoneNumbers', is 'phoneNumber'.
   *
   * @return  The child XML element name or {@code null} if this attribute
   *          is not multi-valued.
   */
  public String getMultiValuedChildName() {
    return multiValuedChildName;
  }

  /**
   * Retrieves the set of descriptors for subordinate attributes of a complex
   * attribute.
   *
   * @return The set of descriptors for subordinate attributes of a complex
   *         attribute, or {@code null} if the attribute is not a complex
   *         attribute.
   */
  public Collection<AttributeDescriptor> getSubAttributes()
  {
    Map<String, AttributeDescriptor> allSubAttributes =
        CoreSchema.addNormativeSubAttributes(this, subAttributes);
    return allSubAttributes == null ? null : allSubAttributes.values();
  }

  /**
   * Retrieves the set of descriptors for subordinate attributes of a
   * complex attribute.  This method does  not return normative
   * sub-attributes that were not declared in the schema.
   *
   * @return The set of descriptors for subordinate attributes of a complex
   *         attribute, or {@code null} if the attribute is not a complex
   *         attribute.
   */
  public Collection<AttributeDescriptor> getDeclaredSubAttributes()
  {
    return subAttributes == null ? null : subAttributes.values();
  }

  /**
   * Retrieves the attribute descriptor for a specified subordinate attribute
   * of a complex attribute.
   *
   * @param externalName The external name of the subordinate attribute for
   *                     which a descriptor is required.
   * @return The attribute descriptor for the specified subordinate attribute.
   * @throws InvalidResourceException if there is no such attribute.
   */
  public AttributeDescriptor getSubAttribute(final String externalName)
      throws InvalidResourceException
  {
    // TODO: Should we have a strict and non strict mode?
    Map<String, AttributeDescriptor> allSubAttributes =
        CoreSchema.addNormativeSubAttributes(this, subAttributes);
    AttributeDescriptor subAttribute =
        allSubAttributes == null ? null :
            allSubAttributes.get(toLowerCase(externalName));
    if(subAttribute == null)
    {
      throw new InvalidResourceException("Sub-attribute " + externalName +
          " is not defined for attribute " + schema +
        SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE + name);
    }
    return subAttribute;
  }

  /**
   * Retrieves a list of canonical type values.
   *
   * @return A list of canonical type values or <code>null</code> if the
   *         attribute is not a multi-valued attribute or if they are not
   *         specified.
   */
  public Collection<Entry<String>> getCanonicalValues()
  {
    return canonicalValues;
  }

  /**
   * Retrieve the data type for this attribute.
   *
   * @return  The data type for this attribute, or {@code null} if the attribute
   *          is not a simple attribute.
   */
  public DataType getDataType()
  {
    return dataType;
  }

  /**
   * Retrieves this attribute's human readable description.
   *
   * @return This attribute's human redable description.
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * Specifies if this attribute is mutable.
   *
   * @return <code>false</code> if this attribute is mutable or
   *         <code>true</code> otherwise.
   */
  public boolean isReadOnly()
  {
    return readOnly;
  }

  /**
   * Specifies if this attribute is required.
   *
   * @return <code>true</code> if this attribute is required for
   *         <code>false</code> otherwise.
   */
  public boolean isRequired()
  {
    return required;
  }

  /**
   * Specifies if the string attribute is case sensitive.
   *
   * @return <code>true</code> if this attribute is case sensitive or
   *         <code>false</code> otherwise.
   */
  public boolean isCaseExact()
  {
    return caseExact;
  }

  @Override
  public String toString()
  {
    return "AttributeDescriptor{" +
        "schema='" + getSchema() + '\'' +
        ", name='" + getName() + '\'' +
        ", description='" + getDescription() + '\'' +
        ", multiValued=" + isMultiValued() +
        ", dataType=" + getDataType() +
        ", isRequired=" + isRequired() +
        ", isReadOnly=" + isReadOnly() +
        ", isCaseExact=" + isCaseExact() +
        '}';
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    int hashCode = 0;

    hashCode += toLowerCase(schema).hashCode();
    hashCode += toLowerCase(name).hashCode();

    return hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
    {
      return true;
    }

    if (!(obj instanceof AttributeDescriptor))
    {
      return false;
    }

    final AttributeDescriptor that = (AttributeDescriptor)obj;
    if (this.schema == null && that.schema == null)
    {
      return this.name.equalsIgnoreCase(that.name);
    }
    else
    {
      return this.schema != null && that.schema != null &&
          this.schema.equalsIgnoreCase(that.schema) &&
          this.name.equalsIgnoreCase(that.name);
    }
  }

  /**
   * Create a new sub-attribute descriptor with the provided information.
   *
   * @param name            The attribute's name.
   * @param dataType        The attribute's data type.
   * @param description     The attribute's human readable description.
   * @param schema          The attribute's associated schema.
   * @param readOnly        Whether the attribute is mutable.
   * @param required        Whether the attribute is required.
   * @param caseExact       Whether the string attribute is case sensitive.
   * @param canonicalValues A collection of canonical values.
   * @return                A new singular sub-attribute descriptor with the
   *                        provided information.
   */
  public static AttributeDescriptor createSubAttribute(
      final String name, final DataType dataType, final String description,
      final String schema, final boolean readOnly, final boolean required,
      final boolean caseExact, final String... canonicalValues)
  {
    final Collection<Entry<String>> values;
    if(canonicalValues != null && canonicalValues.length > 0)
    {
      values = new ArrayList<Entry<String>>(canonicalValues.length);
      for(String canonicalValue : canonicalValues)
      {
        values.add(new Entry<String>(canonicalValue, null, false));
      }
    }
    else
    {
      values = null;
    }
    return new AttributeDescriptor(name, dataType, false, null,
        description, schema, readOnly, required, caseExact, values, null);
  }

  /**
   * Create a new singular attribute descriptor with the provided information.
   *
   * @param name                 The attribute's name.
   * @param dataType             The attribute's data type.
   * @param description          The attribute's human readable description.
   * @param schema               The attribute's associated schema.
   * @param readOnly             Whether the attribute is mutable.
   * @param required             Whether the attribute is required.
   * @param caseExact            Whether the string attribute is case sensitive.
   * @param subAttributes        A list specifying the contained attributes.
   * @return                     A new singular attribute descriptor
   *                             with the provided information.
   */
  public static AttributeDescriptor createAttribute(
      final String name, final DataType dataType, final String description,
      final String schema, final boolean readOnly, final boolean required,
      final boolean caseExact, final AttributeDescriptor... subAttributes)
  {
    if(subAttributes != null)
    {
      for(AttributeDescriptor subAttribute : subAttributes)
      {
        if(subAttribute.getDataType() == DataType.COMPLEX)
        {
          throw new IllegalArgumentException("Complex sub-attributes are not " +
              "allowed");
        }
      }
    }
    return newAttribute(name, null, dataType, description, schema, false,
        readOnly, required, caseExact, subAttributes);
  }

  /**
   * Create a new multi-valued attribute descriptor with the provided
   * information. The normative sub-attributes for multi-valued attributes
   * (ie. type, primary, display, operation, value) will be added.
   *
   * @param name                 The attribute's name.
   * @param multiValuedChildName The child XML element name for multi-valued
   *                             attributes.
   * @param description          The attribute's human readable description.
   * @param schema               The attribute's associated schema.
   * @param readOnly             Whether the attribute is mutable.
   * @param required             Whether the attribute is required.
   * @param caseExact            Whether the string attribute is case sensitive.
   * @param subAttributes        A list specifying the contained attributes.
   * @return                     A new multi-valued attribute descriptor
   *                             with the provided information.
   */
  public static AttributeDescriptor createMultiValuedAttribute(
      final String name, final String multiValuedChildName,
      final String description, final String schema,
      final boolean readOnly, final boolean required, final boolean caseExact,
      final AttributeDescriptor... subAttributes)
  {
    if(subAttributes != null)
    {
      for(AttributeDescriptor subAttribute : subAttributes)
      {
        if(subAttribute.getDataType() == DataType.COMPLEX)
        {
          throw new IllegalArgumentException("Complex sub-attributes are not " +
              "allowed");
        }
      }
    }
    return newAttribute(name, multiValuedChildName, DataType.COMPLEX,
        description, schema, true, readOnly, required, caseExact,
        subAttributes);
  }

  /**
   * Create a new attribute descriptor with the provided information.
   *
   * @param name                 The attribute's name.
   * @param multiValuedChildName The child XML element name for multi-valued
   *                             attributes.
   * @param dataType             The attribute's data type.
   * @param description          The attribute's human readable description.
   * @param schema               The attribute's associated schema.
   * @param multiValued          Whether the attribute is multiValued.
   * @param readOnly             Whether the attribute is mutable.
   * @param required             Whether the attribute is required.
   * @param caseExact            Whether the string attribute is case sensitive.
   * @param subAttributes        A list specifying the contained attributes.
   * @return                     A new attribute descriptor with the provided
   *                             information.
   */
  static AttributeDescriptor newAttribute(
      final String name, final String multiValuedChildName,
      final DataType dataType, final String description, final String schema,
      final boolean multiValued, final boolean readOnly, final boolean required,
      final boolean caseExact, final AttributeDescriptor... subAttributes)
  {
    Collection<AttributeDescriptor> subAttrs = null;
    if (subAttributes != null)
    {
      subAttrs = Arrays.asList(subAttributes);
    }
    return new AttributeDescriptor(name, dataType, multiValued,
        multiValuedChildName, description, schema, readOnly, required,
        caseExact, null, subAttrs);
  }
}
