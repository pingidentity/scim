/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.config;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement
public class ResourceDescriptor {
  private String objectClass;
  private String externalName;
  private List<AttributeDescriptor> attributeDescriptors =
    new LinkedList<AttributeDescriptor>();

  public AttributeDescriptor getAttribute(String name) {
    for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
      if (attributeDescriptor.getExternalAttributeName().equals(name)) {
        return attributeDescriptor;
      }
    }
    return null;
  }

  public String getObjectClass() {
    return objectClass;
  }

  public void setObjectClass(String objectClass) {
    this.objectClass = objectClass;
  }

  public String getExternalName() {
    return externalName;
  }

  public void setExternalName(String externalName) {
    this.externalName = externalName;
  }

  public List<AttributeDescriptor> getAttributeDescriptors() {
    return attributeDescriptors;
  }

  public void setAttributeDescriptors(
    List<AttributeDescriptor> attributeDescriptors) {
    this.attributeDescriptors = attributeDescriptors;
  }

  public static ResourceDescriptor create(String externalName) {
    // read config file/ldap to determine what objectclass the external resource is associated to
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

  @Override
  public String toString() {
    return "ResourceDescriptor{" +
      "objectClass='" + objectClass + '\'' +
      ", externalName='" + externalName + '\'' +
      ", attributeDescriptors=" + attributeDescriptors +
      '}';
  }
}
