/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.config;

import java.util.ArrayList;
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
    STRING("xs:string"),
    /**
     * Boolean data type.
     */
    BOOLEAN("xs:boolean"),
    /**
     * Date Time data type.
     */
    DATETIME("xs:dateTime"),
    /**
     * Integer data type.
     */
    INTEGER("xs:integer"),;

    private final String xmlType;

    /**
     * Constructs a new DataType.
     * @param type The external data type.
     */
    DataType(final String type) {
      xmlType = type;
    }

    /**
     * Returns the type in XML schema syntax; e.g., 'string' > 'xs:string'.
     *
     * @return XML The external data type.
     */
    public String getXmlType() {
      return xmlType;
    }

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

  private String schema;

  private String name;

  private boolean complex;

  private boolean plural;

  private DataType dataType;

  private List<AttributeDescriptor> complexAttributeDescriptors =
    new ArrayList<AttributeDescriptor>();


  /**
   * Create a new attribute descriptor.
   */
  public AttributeDescriptor() {
  }


  /**
   * Create a new attribute descriptor from its builder.
   *
   * @param builder The attribute descriptor builder.
   */
  public AttributeDescriptor(final Builder builder) {
    this.schema = builder.schema;
    this.name = builder.externalAttributeName;
    this.complex = builder.complex;
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
    private boolean complex;
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
     */
    public Builder(final String schema, final String externalAttributeName) {
      this.schema = schema;
      this.externalAttributeName = externalAttributeName;
    }


    /**
     * Specifies whether the attribute is a complex attribute.
     *
     * @param complex {@code true} if the attribute is complex.
     * @return This attribute descriptor builder.
     */
    public Builder complex(final boolean complex) {
      this.complex = complex;
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
   * Specifies the URI for the schema that defines the SCIM attribute.
   *
   * @param schema The URI for the schema that defines the SCIM attribute.
   *               It must not be {@code null}.
   */
  public void setSchema(final String schema) {
    this.schema = schema;
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
   * Specifies the attribute name to be used in any external representation of
   * the SCIM attribute.
   *
   * @param name The attribute name to be used in any external
   *             representation of the SCIM attribute. It must
   *             not be {@code null}.
   */
  public void setName(final String name) {
    this.name = name;
  }


  /**
   * Indicates whether the attribute is a complex attribute.
   *
   * @return {@code true} if the attribute is complex.
   */
  public boolean isComplex() {
    return complex;
  }

  /**
   * Specifies whether the attribute is a complex attribute.
   *
   * @param complex {@code true} if the attribute is complex.
   */
  public void setComplex(final boolean complex) {
    this.complex = complex;
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
   * Specifies whether the attribute is a plural attribute.
   *
   * @param plural {@code true} if the attribute is plural.
   */
  public void setPlural(final boolean plural) {
    this.plural = plural;
  }


  /**
   * Retrieves the set of descriptors for subordinate attributes of a complex
   * attribute.
   *
   * @return The set of descriptors for subordinate attributes of a complex
   *         attribute, or {@code null} if the attribute is not a complex
   *         attribute.
   */
  public List<AttributeDescriptor> getComplexAttributeDescriptors() {
    return complexAttributeDescriptors;
  }


  /**
   * Specifies the set of descriptors for subordinate attributes of a complex
   * attribute.
   *
   * @param descriptors The set of descriptors for subordinate attributes of
   *                    a complex attribute, or {@code null} if the attribute
   *                    is not a complex attribute.
   */
  public void setComplexAttributeDescriptors(
    final List<AttributeDescriptor> descriptors) {
    this.complexAttributeDescriptors = descriptors;
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
  public AttributeDescriptor getAttribute(final String externalName) {
    for (AttributeDescriptor r : this.complexAttributeDescriptors) {
      if (r.getName().equals(externalName)) {
        return r;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return "AttributeDescriptor{" +
      "schema='" + schema + '\'' +
      ", name='" + name + '\'' +
      ", complex=" + complex +
      ", plural=" + plural +
      ", dataType=" + dataType +
      ", complexAttributeDescriptors=" + complexAttributeDescriptors +
      '}';
  }
}
