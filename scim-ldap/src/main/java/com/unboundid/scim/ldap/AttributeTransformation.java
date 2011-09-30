/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

/**
 * A class representing a transformation to a specified LDAP attribute.
 */
public class AttributeTransformation
{
  private final String ldapAttribute;
  private final Transformation transformation;



  /**
   * Create a new attribute transformation.
   *
   * @param ldapAttribute   The LDAP attribute that is the subject of the
   *                        transformation.
   * @param transformation  The transformation to be applied.
   */
  public AttributeTransformation(final String ldapAttribute,
                                 final Transformation transformation)
  {
    this.ldapAttribute = ldapAttribute;
    this.transformation = transformation;
  }



  /**
   * Create a new attribute transformation from the JAXB type that represents
   * an attribute mapping.
   *
   * @param mapping  The JAXB type that represents an attribute mapping.
   *
   * @return  The new attribute transformation.
   */
  public static AttributeTransformation create(final AttributeMapping mapping)
  {
    final String ldapAttribute = mapping.getLdapAttribute();
    final Transformation transformation =
        Transformation.create(mapping.getTransform());

    return new AttributeTransformation(ldapAttribute, transformation);
  }



  /**
   * Retrieve the LDAP attribute that is the subject of the transformation.
   *
   * @return  The LDAP attribute that is the subject of the transformation.
   */
  public String getLdapAttribute()
  {
    return ldapAttribute;
  }



  /**
   * Retrieve the transformation to be applied.
   *
   * @return  The transformation to be applied.
   */
  public Transformation getTransformation()
  {
    return transformation;
  }
}
