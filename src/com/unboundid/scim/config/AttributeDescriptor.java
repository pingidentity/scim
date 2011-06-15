/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.config;

import javax.xml.bind.annotation.XmlElement;
import java.util.HashSet;
import java.util.Set;



/**
 * This class provides methods that describe the schema for a SCIM attribute.
 * It may be used to help read and write SCIM attributes in their external XML
 * and JSON representation, and to convert SCIM attributes to and from LDAP
 * attributes.
 */
public class AttributeDescriptor {

  @XmlElement
  private String schema;

  @XmlElement
  private String ldapAttributeName;

  @XmlElement
  private String externalAttributeName;

  @XmlElement
  private boolean complex;

  @XmlElement
  private boolean plural;

  @XmlElement
  private Set<AttributeDescriptor> complexAttributeDescriptors =
    new HashSet<AttributeDescriptor>();



  /**
   * Create a new attribute descriptor.
   */
  public AttributeDescriptor() {
  }



  /**
   * Create a new attribute descriptor from its builder.
   *
   * @param builder  The attribute descriptor builder.
   */
  public AttributeDescriptor(final Builder builder) {
    this.schema = builder.schema;
    this.ldapAttributeName = builder.ldapAttributeName;
    this.externalAttributeName = builder.externalAttributeName;
    this.complex = builder.complex;
    this.plural = builder.plural;
    this.complexAttributeDescriptors = builder.complexAttributeDescriptors;
  }



  /**
   * A helper class that may be used to construct new attribute descriptors.
   */
  public static class Builder {
    private final String schema;
    private final String ldapAttributeName;
    private final String externalAttributeName;
    private boolean complex;
    private boolean plural;
    private Set<AttributeDescriptor> complexAttributeDescriptors;



    /**
     * Create a new attribute descriptor builder.
     *
     * @param schema                 The URI for the schema that defines the
     *                               SCIM attribute. It must not be {@code
     *                               null}.
     * @param ldapAttributeName      The name of the LDAP attribute to which
     *                               the SCIM attribute can be mapped, or
     *                               {@code null} if there is no mapping.
     * @param externalAttributeName  The attribute name to be used in any
     *                               external representation of the SCIM
     *                               attribute. It must not be {@code null}.
     */
    public Builder(final String schema, final String ldapAttributeName,
                   final String externalAttributeName) {
      this.schema = schema;
      this.ldapAttributeName = ldapAttributeName;
      this.externalAttributeName = externalAttributeName;
    }



    /**
     * Specifies whether the attribute is a complex attribute.
     *
     * @param complex  {@code true} if the attribute is complex.
     *
     * @return  This attribute descriptor builder.
     */
    public Builder complex(final boolean complex) {
      this.complex = complex;
      return this;
    }

    /**
     * Specifies whether the attribute is a plural attribute.
     *
     * @param plural  {@code true} if the attribute is plural.
     *
     * @return  This attribute descriptor builder.
     */
    public Builder plural(final boolean plural) {
      this.plural = plural;
      return this;
    }



    /**
     * Specifies the set of descriptors for subordinate attributes of a
     * complex attribute.
     *
     * @param descriptors  The set of descriptors for subordinate attributes of
     *                     a complex attribute, or {@code null} if the attribute
     *                     is not a complex attribute.
     *
     * @return  This attribute descriptor builder.
     */
    public Builder complexAttributeDescriptors(
      final Set<AttributeDescriptor> descriptors) {
      this.complexAttributeDescriptors = descriptors;
      return this;
    }
  }



  /**
   * The URI for the schema that defines the SCIM attribute.
   *
   * @return  The URI for the schema that defines the SCIM attribute.
   *          It is never {@code null}.
   */
  public String getSchema() {
    return schema;
  }



  /**
   * Specifies the URI for the schema that defines the SCIM attribute.
   *
   * @param schema  The URI for the schema that defines the SCIM attribute.
   *                It must not be {@code null}.
   */
  public void setSchema(final String schema) {
    this.schema = schema;
  }



  /**
   * Retrieves the name of the LDAP attribute to which the SCIM attribute can
   * be mapped.
   *
   * @return  The  name of the LDAP attribute to which the SCIM attribute can
   *          be mapped, or {@code null} if there is no mapping.
   */
  public String getLdapAttributeName() {
    return ldapAttributeName;
  }



  /**
   * Specifies the name of the LDAP attribute to which the SCIM attribute can
   * be mapped.
   *
   * @param ldapAttributeName  The name of the LDAP attribute to which the
   *                           SCIM attribute can be mapped, or {@code null}
   *                           if there is no mapping.
   */
  public void setLdapAttributeName(final String ldapAttributeName) {
    this.ldapAttributeName = ldapAttributeName;
  }



  /**
   * The attribute name to be used in any external representation of the SCIM
   * attribute.
   *
   * @return  The attribute name to be used in any external representation of
   *          the SCIM attribute. It is never {@code null}.
   */
  public String getExternalAttributeName() {
    return externalAttributeName;
  }



  /**
   * Specifies the attribute name to be used in any external representation of
   * the SCIM attribute.
   *
   * @param externalAttributeName  The attribute name to be used in any external
   *                               representation of the SCIM attribute. It must
   *                               not be {@code null}.
   */
  public void setExternalAttributeName(final String externalAttributeName) {
    this.externalAttributeName = externalAttributeName;
  }



  /**
   * Indicates whether the attribute is a complex attribute.
   *
   * @return  {@code true} if the attribute is complex.
   */
  public boolean isComplex() {
    return complex;
  }

  /**
   * Specifies whether the attribute is a complex attribute.
   *
   * @param complex  {@code true} if the attribute is complex.
   */
  public void setComplex(final boolean complex) {
    this.complex = complex;
  }

  /**
   * Indicates whether the attribute is a plural attribute.
   *
   * @return  {@code true} if the attribute is plural.
   */
  public boolean isPlural() {
    return plural;
  }

  /**
   * Specifies whether the attribute is a plural attribute.
   *
   * @param plural  {@code true} if the attribute is plural.
   */
  public void setPlural(final boolean plural) {
    this.plural = plural;
  }



  /**
   * Retrieves the set of descriptors for subordinate attributes of a complex
   * attribute.
   *
   * @return  The set of descriptors for subordinate attributes of a complex
   *          attribute, or {@code null} if the attribute is not a complex
   *          attribute.
   */
  public Set<AttributeDescriptor> getComplexAttributeDescriptors() {
    return complexAttributeDescriptors;
  }



  /**
   * Specifies the set of descriptors for subordinate attributes of a complex
   * attribute.
   *
   * @param descriptors  The set of descriptors for subordinate attributes of
   *                     a complex attribute, or {@code null} if the attribute
   *                     is not a complex attribute.
   */
  public void setComplexAttributeDescriptors(
    final Set<AttributeDescriptor> descriptors) {
    this.complexAttributeDescriptors = descriptors;
  }



  /**
   * Retrieves the attribute descriptor for a specified subordinate attribute
   * of a complex attribute.
   *
   * @param externalName  The external name of the subordinate attribute for
   *                      which a descriptor is required.
   *
   * @return  The attribute descriptor for the specified subordinate attribute,
   *          or {@code null} if there is no such subordinate attribute.
   */
  public AttributeDescriptor getAttribute(final String externalName) {
    for (AttributeDescriptor r : this.complexAttributeDescriptors) {
      if (r.getExternalAttributeName().equals(externalName)) {
        return r;
      }
    }
    return null;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "AttributeDescriptor{" +
      "schema='" + schema + '\'' +
      ", ldapAttributeName='" + ldapAttributeName + '\'' +
      ", externalAttributeName='" + externalAttributeName + '\'' +
      ", complex=" + complex +
      ", plural=" + plural +
      ", complexAttributeDescriptors=" + complexAttributeDescriptors +
      '}';
  }
}
