/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.config;

import javax.xml.bind.annotation.XmlElement;
import java.util.HashSet;
import java.util.Set;

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

  public AttributeDescriptor() {
  }

  public AttributeDescriptor(Builder builder) {
    this.schema = builder.schema;
    this.ldapAttributeName = builder.ldapAttributeName;
    this.externalAttributeName = builder.externalAttributeName;
    this.complex = builder.complex;
    this.plural = builder.plural;
    this.complexAttributeDescriptors = builder.complexAttributeDescriptors;
  }

  public static class Builder {
    private final String schema;
    private final String ldapAttributeName;
    private final String externalAttributeName;
    private boolean complex;
    private boolean plural;
    private Set<AttributeDescriptor> complexAttributeDescriptors;

    public Builder(final String schema, final String ldapAttributeName,
                   final String externalAttributeName) {
      this.schema = schema;
      this.ldapAttributeName = ldapAttributeName;
      this.externalAttributeName = externalAttributeName;
    }

    public Builder complex(boolean complex) {
      this.complex = complex;
      return this;
    }

    public Builder plural(boolean plural) {
      this.plural = plural;
      return this;
    }

    public Builder complexAttributeDescriptors(
      Set<AttributeDescriptor> complexAttributeDescriptors) {
      this.complexAttributeDescriptors = complexAttributeDescriptors;
      return this;
    }
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getLdapAttributeName() {
    return ldapAttributeName;
  }

  public void setLdapAttributeName(String ldapAttributeName) {
    this.ldapAttributeName = ldapAttributeName;
  }

  public String getExternalAttributeName() {
    return externalAttributeName;
  }

  public void setExternalAttributeName(String externalAttributeName) {
    this.externalAttributeName = externalAttributeName;
  }

  public boolean isComplex() {
    return complex;
  }

  public void setComplex(boolean complex) {
    this.complex = complex;
  }

  public boolean isPlural() {
    return plural;
  }

  public void setPlural(boolean plural) {
    this.plural = plural;
  }

  public Set<AttributeDescriptor> getComplexAttributeDescriptors() {
    return complexAttributeDescriptors;
  }

  public void setComplexAttributeDescriptors(
    Set<AttributeDescriptor> complexAttributeDescriptors) {
    this.complexAttributeDescriptors = complexAttributeDescriptors;
  }

  public AttributeDescriptor getAttribute(String externalName) {
    for (AttributeDescriptor r : this.complexAttributeDescriptors) {
      if (r.getExternalAttributeName().equals(externalName)) {
        return r;
      }
    }
    return null;
  }

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
