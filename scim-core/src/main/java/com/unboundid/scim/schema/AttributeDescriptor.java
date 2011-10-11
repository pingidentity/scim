/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.schema;

import java.util.List;

/**
 * This class provides methods that describe the schema for a SCIM attribute.
 * It may be used to help read and write SCIM attributes in their external XML
 * and JSON representation, and to convert SCIM attributes to and from LDAP
 * attributes.
 */
public class AttributeDescriptor {

  /**
   * Defines the set of well known SCIM supported datatypes.
   */
  public enum DataType {
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

  }

  private final String schema;

  private final String name;

  private final String description;

  private final boolean readOnly;

  private final boolean required;

  private final boolean plural;

  private final DataType dataType;

  private final List<AttributeDescriptor> complexAttributeDescriptors;


  /**
   * Create a new attribute descriptor from its builder.
   *
   * @param builder The attribute descriptor builder.
   */
  public AttributeDescriptor(final Builder builder) {
    this.schema = builder.schema;
    this.name = builder.externalAttributeName;
    this.description = builder.description;
    this.readOnly = builder.readOnly;
    this.required = builder.required;
    this.plural = builder.plural;
    this.dataType = builder.dataType;
    this.complexAttributeDescriptors = builder.complexAttributeDescriptors;
  }


  /**
   * A helper class that may be used to construct new attribute descriptors.
   */
  public static class Builder {
    private final String schema;
    private final String externalAttributeName;
    private final String description;
    private boolean readOnly;
    private boolean required;
    private boolean plural;
    private DataType dataType;
    private List<AttributeDescriptor> complexAttributeDescriptors;


    /**
     * Create a new attribute descriptor builder.
     *
     * @param schema                The URI for the schema that defines the
     *                              SCIM attribute. It must not be {@code
     *                              null}.
     * @param externalAttributeName The attribute name to be used in any
     *                              external representation of the SCIM
     *                              attribute. It must not be {@code null}.
     * @param description           The attribute's human readable description.
     */
    public Builder(final String schema, final String externalAttributeName,
                   final String description) {
      this.schema = schema;
      this.externalAttributeName = externalAttributeName;
      this.description = description;
    }

    /**
     * Specifies whether the attribute is read only.
     *
     * @param readOnly {@code true} if the attribute is read only.
     * @return This attribute descriptor builder.
     */
    public Builder readOnly(final boolean readOnly)
    {
      this.readOnly = readOnly;
      return this;
    }

    /**
     * Specifies whether the attribute is required.
     *
     * @param required {@code true} if the attribute is required.
     * @return This attribute descriptor builder.
     */
    public Builder required(final boolean required)
    {
      this.required = required;
      return this;
    }

    /**
     * Specifies whether the attribute is a plural attribute.
     *
     * @param plural {@code true} if the attribute is plural.
     * @return This attribute descriptor builder.
     */
    public Builder plural(final boolean plural) {
      this.plural = plural;
      return this;
    }

    /**
     * Specifies the attribute data type.
     * @param dataType the attribute descriptor data type.
     * @return This attribute descriptor builder.
     */
    public Builder dataType(final DataType dataType) {
      this.dataType = dataType;
      return this;
    }

    /**
     * Specifies the set of descriptors for subordinate attributes of a
     * complex attribute.
     *
     * @param descriptors The set of descriptors for subordinate attributes of
     *                    a complex attribute, or {@code null} if the attribute
     *                    is not a complex attribute.
     * @return This attribute descriptor builder.
     */
    public Builder complexAttributeDescriptors(
      final List<AttributeDescriptor> descriptors) {
      this.complexAttributeDescriptors = descriptors;
      return this;
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
   * Indicates whether the attribute is a plural attribute.
   *
   * @return {@code true} if the attribute is plural.
   */
  public boolean isPlural() {
    return plural;
  }

  /**
   * Retrieves the set of descriptors for subordinate attributes of a complex
   * attribute.
   *
   * @return The set of descriptors for subordinate attributes of a complex
   *         attribute, or {@code null} if the attribute is not a complex
   *         attribute.
   */
  public List<AttributeDescriptor> getComplexAttributeDescriptors()
  {
    return complexAttributeDescriptors;
  }


  /**
   * Retrieves the attribute descriptor for a specified subordinate attribute
   * of a complex attribute.
   *
   * @param externalName The external name of the subordinate attribute for
   *                     which a descriptor is required.
   * @return The attribute descriptor for the specified subordinate attribute,
   *         or {@code null} if there is no such subordinate attribute.
   */
  public AttributeDescriptor getAttribute(final String externalName)
  {
    for (AttributeDescriptor r : this.complexAttributeDescriptors)
    {
      if (r.getName().equals(externalName))
      {
        return r;
      }
    }
    return null;
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

  @Override
  public String toString()
  {
    return "AttributeDescriptor{" +
      "schema='" + getSchema() + '\'' +
      ", name='" + getName() + '\'' +
      ", description='" + getDescription() + '\'' +
      ", plural=" + isPlural() +
      ", dataType=" + getDataType() +
      ", isRequired=" + isRequired() +
      ", isReadOnly=" + isReadOnly() +
      '}';
  }
}
