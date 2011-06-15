/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.config;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;



/**
 * This class provides methods that describe the schema for a SCIM resource.
 * It may be used to help read and write SCIM objects in their external XML
 * and JSON representation, and to convert SCIM objects to and from LDAP
 * entries.
 */
@XmlRootElement
public class ResourceDescriptor {
  private String objectClass;
  private String externalName;
  private List<AttributeDescriptor> attributeDescriptors =
    new LinkedList<AttributeDescriptor>();



  /**
   * Retrieve the attribute descriptor for a specified attribute.
   *
   * @param name  The name of the attribute whose descriptor is to be retrieved.
   * @return  The attribute descriptor for the specified attribute, or {@code
   *          null} if there is no such attribute.
   */
  public AttributeDescriptor getAttribute(final String name) {
    for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
      if (attributeDescriptor.getExternalAttributeName().equals(name)) {
        return attributeDescriptor;
      }
    }
    return null;
  }



  /**
   * Retrieve the name of the LDAP structural object class that may be used
   * for LDAP entries representing the SCIM resource.
   *
   * @return  The name of the LDAP structural object class that may be used
   *          for LDAP entries representing the SCIM resource, or {@code null}
   *          if there is none.
   */
  public String getObjectClass() {
    return objectClass;
  }

  /**
   * Specifies the name of the LDAP structural object class that may be used
   * for LDAP entries representing the SCIM resource.
   *
   * @param objectClass  The name of the LDAP structural object class that may
   *                     be used for LDAP entries representing the SCIM
   *                     resource, or {@code null} if there is none.
   */
  public void setObjectClass(final String objectClass) {
    this.objectClass = objectClass;
  }

  /**
   * Retrieve the name of the resource to be used in any external
   * representation of the resource.
   *
   * @return  Retrieve the name of the resource to be used in any external
   *          representation of the resource. It is never {@code null}.
   */
  public String getExternalName() {
    return externalName;
  }

  /**
   * Specifies the name of the resource to be used in any external
   * representation of the resource.
   *
   * @param externalName  Specifies the name of the resource to be used in any
   *                      external representation of the resource. It must not
   *                      be {@code null}.
   */
  public void setExternalName(final String externalName) {
    this.externalName = externalName;
  }



  /**
   * Retrieves the list of attribute descriptors for the resource.
   *
   * @return  The list of attribute descriptors for the resource.
   *          It is never {@code null}.
   */
  public List<AttributeDescriptor> getAttributeDescriptors() {
    return attributeDescriptors;
  }

  /**
   * Specifies the list of attribute descriptors for the resource.
   *
   * @param attributeDescriptors  The list of attribute descriptors for the
   *                              resource. It must not be {@code null}.
   */
  public void setAttributeDescriptors(
    final List<AttributeDescriptor> attributeDescriptors) {
    this.attributeDescriptors = attributeDescriptors;
  }



  /**
   * Retrieve a resource descriptor for a specified resource type.
   *
   * @param externalName  The external name of the resource for which a
   *                      descriptor is required.
   *
   * @return  A resource descriptor for the specified resource type.
   */
  public static ResourceDescriptor create(final String externalName) {
    // read config file/ldap to determine what objectclass the external
    // resource is associated to.
    // presumably this stuff is relatively static (cacheable)

    ResourceDescriptor resourceDescriptor = new ResourceDescriptor();
    resourceDescriptor.setExternalName("user");
    resourceDescriptor.setObjectClass("inetorgperson");

    AttributeDescriptor resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("id");
    resourceAttributeDescriptor.setLdapAttributeName("entryUUID");
    resourceDescriptor.getAttributeDescriptors()
      .add(resourceAttributeDescriptor);


    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("userName");
    resourceAttributeDescriptor.setLdapAttributeName("uid");
    resourceDescriptor.getAttributeDescriptors()
      .add(resourceAttributeDescriptor);

    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("title");
    resourceAttributeDescriptor.setLdapAttributeName("title");
    resourceDescriptor.getAttributeDescriptors()
      .add(resourceAttributeDescriptor);

    AttributeDescriptor name = new AttributeDescriptor();
    name.setExternalAttributeName("name");
    name.setComplex(true);

    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("formatted");
    resourceAttributeDescriptor.setLdapAttributeName("display");
    name.getComplexAttributeDescriptors().add(resourceAttributeDescriptor);

    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("familyName");
    resourceAttributeDescriptor.setLdapAttributeName("sn");
    name.getComplexAttributeDescriptors().add(resourceAttributeDescriptor);

    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("givenName");
    resourceAttributeDescriptor.setLdapAttributeName("givenName");
    name.getComplexAttributeDescriptors().add(resourceAttributeDescriptor);

    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("middleName");
    resourceAttributeDescriptor.setLdapAttributeName("middle");
    name.getComplexAttributeDescriptors().add(resourceAttributeDescriptor);

    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("honorificPrefix");
    resourceAttributeDescriptor.setLdapAttributeName("x");

    name.getComplexAttributeDescriptors().add(resourceAttributeDescriptor);
    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("honorificSuffix");
    resourceAttributeDescriptor.setLdapAttributeName("y");
    name.getComplexAttributeDescriptors().add(resourceAttributeDescriptor);

    resourceDescriptor.getAttributeDescriptors().add(name);


    AttributeDescriptor emails = new AttributeDescriptor();
    emails.setExternalAttributeName("emails");
    emails.setPlural(true);

    AttributeDescriptor email = new AttributeDescriptor();
    email.setExternalAttributeName("email");
    email.setLdapAttributeName("mail");
    email.setComplex(true);

    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("primary");
    email.getComplexAttributeDescriptors().add(resourceAttributeDescriptor);
    resourceAttributeDescriptor = new AttributeDescriptor();
    resourceAttributeDescriptor.setExternalAttributeName("value");
    email.getComplexAttributeDescriptors().add(resourceAttributeDescriptor);


    emails.getComplexAttributeDescriptors().add(email);

    resourceDescriptor.getAttributeDescriptors().add(emails);


    return resourceDescriptor;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "ResourceDescriptor{" +
      "objectClass='" + objectClass + '\'' +
      ", externalName='" + externalName + '\'' +
      ", attributeDescriptors=" + attributeDescriptors +
      '}';
  }
}
