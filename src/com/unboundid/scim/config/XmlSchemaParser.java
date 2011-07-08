/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.config;

import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSElementDecl;
import com.sun.xml.xsom.XSModelGroup;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSSchema;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSTerm;
import com.sun.xml.xsom.XSType;
import com.sun.xml.xsom.parser.XSOMParser;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Xml schema parser that produces SCIM Resource Descriptors.
 */
public class XmlSchemaParser {
  private List<ResourceDescriptor> descriptors = Collections.emptyList();

  /**
   * Returns the collection of all supported and known Resource Descriptors.
   *
   * @return The known Resource Descriptors.
   */
  public Collection<ResourceDescriptor> getDescriptors() {
    return descriptors;
  }

  /**
   * Initializes the supplied XML .xsd files.
   *
   * @param schemas The XML schema files to parse.
   * @throws IOException  Thrown if error loading schema files.
   * @throws SAXException Thrown if error parsing schema files.
   */
  public XmlSchemaParser(final File[] schemas) throws IOException,
    SAXException {
    List<ResourceDescriptor> resourceDescriptors = new
      ArrayList<ResourceDescriptor>();
    for (File file : schemas) {
      XSOMParser parser = new XSOMParser();
      parser.parse(file);
      XSSchemaSet result = parser.getResult();

      Iterator itr = result.iterateSchema();
      while (itr.hasNext()) {
        XSSchema s = (XSSchema) itr.next();
        Iterator jtr = s.iterateElementDecls();
        while (jtr.hasNext()) {
          // user || group || extension
          XSComplexType scimResourceType =
            s.getType(((XSElementDecl) jtr.next())
              .getName()).asComplexType();
          // create scim descriptor
          ResourceDescriptor resourceDescriptor = new ResourceDescriptor();
          resourceDescriptor.setName(scimResourceType.getName());
          resourceDescriptor.setSchema(s.getTargetNamespace());
          resourceDescriptors.add(resourceDescriptor);

          XSParticle[] children = scimResourceType.getContentType().asParticle
            ().getTerm().asModelGroup()
            .getChildren();

          for (XSParticle p : children) {
            XSParticle xsParticle = p.asParticle();
            XSTerm term = xsParticle.getTerm();
            if (term.isModelGroup()) {
              XSModelGroup xsModelGroup = term.asModelGroup();
              XSParticle[] children1 = xsModelGroup.getChildren();
              for (XSParticle c : children1) {
                //
                XSTerm cTerm = c.getTerm();
                if (cTerm != null && cTerm.asElementDecl() != null) {
                  XSElementDecl cxsElementDecl = cTerm.asElementDecl();
                  String cname = cxsElementDecl.getName();
                  XSType ctype = cxsElementDecl.getType();

                  if (ctype.isComplexType() &&
                    ctype.asComplexType().getContentType()
                      .asParticle().getTerm().asModelGroup().getChild(0)
                      .isRepeated()) {
                    AttributeDescriptor pluralAttribute =
                      this.createPluralAttribute(c);
                    resourceDescriptor.getAttributeDescriptors().add
                      (pluralAttribute);
                  } else {
                    if (ctype.isComplexType()) {
                      // complex; e.g., name
                      XSComplexType complexType = s.getComplexType(cname);
                      AttributeDescriptor complexAttribute =
                        this.createComplexAttribute(cname, complexType);
                      resourceDescriptor.getAttributeDescriptors().add
                        (complexAttribute);
                    } else {
                      // simple; e.g., userName
                      AttributeDescriptor simpleAttributeDescriptor =
                        new AttributeDescriptor
                          (new AttributeDescriptor
                            .Builder(c.getOwnerSchema().getTargetNamespace(),
                            cxsElementDecl.getName()).dataType(
                            AttributeDescriptor.DataType
                              .parse(cxsElementDecl.getType().getName()))
                          );
                      resourceDescriptor.getAttributeDescriptors().add
                        (simpleAttributeDescriptor);

                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    this.descriptors = resourceDescriptors;
  }

  /**
   * Helper to create AttributeDescriptor.
   *
   * @param particle The XML particle representing a plural attribute.
   * @return The SCIM descriptor equivalent of an XML schema plural attribute.
   */
  private AttributeDescriptor createPluralAttribute(final XSParticle
                                                      particle) {
    XSTerm term = particle.getTerm();
    XSElementDecl elementDeclaration = term.asElementDecl();
    String name = elementDeclaration.getName();

    XSType type = elementDeclaration.getType();
    XSParticle xsParticle = type.asComplexType().getContentType().asParticle();
    XSParticle[] children = xsParticle.getTerm().asModelGroup().getChildren();

    // there must only be a single child; e.g., in the case of emails the
    // child is 'email'
    XSParticle child = children[0];
    XSType type1 = child.getTerm().asElementDecl().getType();
    String name1 = type1.getName();

    XSComplexType complexType = particle.getOwnerSchema().getComplexType(name1);

    // get the name as it appears in the schema - we want the element name
    // not the type name; e.g., email not pluralAttribute
    String complexAttributeName =
      child.asParticle().getTerm().asElementDecl().getName();

    AttributeDescriptor complexAttribute = this.createComplexAttribute
      (complexAttributeName, complexType);

    List<AttributeDescriptor> complexAttributeDescriptors = new
      ArrayList<AttributeDescriptor>();
    complexAttributeDescriptors.add(complexAttribute);

    AttributeDescriptor pluralAttribute = new AttributeDescriptor(new
      AttributeDescriptor
        .Builder(complexType.getOwnerSchema().getTargetNamespace(),
      name).complex(true).plural(true)
      .complexAttributeDescriptors(complexAttributeDescriptors)
    );

    return pluralAttribute;
  }

  /**
   * Helper that creates a Complex SCIM Attribute Descriptor.
   *
   * @param name        The SCIM attribute name.
   * @param complexType The XML schema representation of a SCIM attribute.
   * @return The complex SCIM Attribute Descriptor.
   */
  private AttributeDescriptor createComplexAttribute(final String name, final
  XSComplexType complexType) {
    AttributeDescriptor attributeDescriptor;
    List<AttributeDescriptor> complexAttributeDescriptors = new
      ArrayList<AttributeDescriptor>();

    XSParticle[] children =
      complexType.getContentType().asParticle().getTerm().asModelGroup()
        .getChildren();
    for (XSParticle particle : children) {
      XSElementDecl xsElementDecl = particle.getTerm().asElementDecl();
      AttributeDescriptor peerAttributeDescriptor = new AttributeDescriptor
        (new AttributeDescriptor.Builder(
          complexType.getOwnerSchema().getTargetNamespace(),
          xsElementDecl.getName()).complex(false).dataType(
          AttributeDescriptor.DataType.parse(xsElementDecl.getType().getName()))
        );
      complexAttributeDescriptors.add(peerAttributeDescriptor);
    }

    attributeDescriptor = new AttributeDescriptor(new AttributeDescriptor
      .Builder(complexType.getOwnerSchema().getTargetNamespace(),
      name).complex(true).complexAttributeDescriptors
      (complexAttributeDescriptors)
    );

    return attributeDescriptor;
  }
}
