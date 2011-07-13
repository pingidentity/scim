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
 * Xml schema parser that produces SCIM schemas.
 * TODO: There are assumptions made during the parsing that need to be explained
 * in the SCIM specification.
 */
public class XmlSchemaParser
{
  private List<Schema> schemas = Collections.emptyList();



  /**
   * Returns the collection of all supported and known schemas.
   *
   * @return The known schemas.
   */
  public Collection<Schema> getSchemas()
  {
    return schemas;
  }



  /**
   * Initializes the supplied XML .xsd files.
   *
   * @param schemaFiles The XML schema files to parse.
   *
   * @throws IOException  Thrown if error loading schema files.
   * @throws SAXException Thrown if error parsing schema files.
   */
  public XmlSchemaParser(final File[] schemaFiles)
      throws IOException, SAXException
  {
    List<Schema> schemaList = new ArrayList<Schema>();

    for (File file : schemaFiles)
    {
      XSOMParser parser = new XSOMParser();
      parser.parse(file);
      XSSchemaSet result = parser.getResult();

      Iterator<XSSchema> itr = result.iterateSchema();
      while (itr.hasNext())
      {
        XSSchema s = itr.next();

        // Skip non-SCIM schemas.
        String targetNamespace = s.getTargetNamespace();
        if (targetNamespace.equals("http://www.w3.org/2001/XMLSchema"))
        {
          continue;
        }

        Iterator<XSElementDecl> jtr = s.iterateElementDecls();

        Schema schema = new Schema(targetNamespace);
        List<ResourceDescriptor> resourceDescriptors =
            schema.getResourceDescriptors();
        List<AttributeDescriptor> attributeDescriptors =
            schema.getAttributeDescriptors();

        // We assume that top level complex types are SCIM attributes rather
        // than SCIM resources.
        Iterator<XSComplexType> complexTypesIterator =
            s.iterateComplexTypes();
        while (complexTypesIterator.hasNext())
        {
          XSComplexType xsComplexType = complexTypesIterator.next();
          AttributeDescriptor complexAttribute =
              createComplexAttribute(xsComplexType.getName(), xsComplexType);
          if (complexAttribute != null)
          {
            attributeDescriptors.add(complexAttribute);
          }
        }

        while (jtr.hasNext())
        {
          // user || group || extension
          XSElementDecl xsElementDecl = jtr.next();
          String name = xsElementDecl.getName();

          // We assume that elements with complex type are SCIM resources
          // but there is currently no way to indicate that "Response" (for
          // example) in the core schema is not a resource.

          if (!xsElementDecl.getType().isComplexType())
          {
            AttributeDescriptor simpleAttributeDescriptor =
                new AttributeDescriptor(
                    new AttributeDescriptor.Builder(
                        targetNamespace,
                        xsElementDecl.getName()).dataType(
                        AttributeDescriptor.DataType.parse(
                            xsElementDecl.getType().getName()))
                    );
            attributeDescriptors.add(simpleAttributeDescriptor);
            continue;
          }

          XSType xsType = s.getType(name);
          XSComplexType scimResourceType = xsType.asComplexType();

          // create SCIM resource descriptor
          ResourceDescriptor resourceDescriptor = new ResourceDescriptor();
          resourceDescriptor.setName(scimResourceType.getName());
          resourceDescriptor.setSchema(targetNamespace);
          resourceDescriptors.add(resourceDescriptor);

          XSParticle[] children = scimResourceType.getContentType().asParticle
              ().getTerm().asModelGroup()
              .getChildren();

          for (XSParticle p : children)
          {
            XSParticle xsParticle = p.asParticle();
            XSTerm term = xsParticle.getTerm();
            if (term.isModelGroup())
            {
              XSModelGroup xsModelGroup = term.asModelGroup();
              XSParticle[] children1 = xsModelGroup.getChildren();
              for (XSParticle c : children1)
              {
                //
                XSTerm cTerm = c.getTerm();
                if (cTerm != null && cTerm.asElementDecl() != null)
                {
                  XSElementDecl cxsElementDecl = cTerm.asElementDecl();
                  String cname = cxsElementDecl.getName();
                  XSType ctype = cxsElementDecl.getType();

                  if (ctype.isComplexType() &&
                      ctype.asComplexType().getContentType()
                          .asParticle().getTerm().asModelGroup().getChild(0)
                          .isRepeated())
                  {
                    AttributeDescriptor pluralAttribute =
                        this.createPluralAttribute(c);
                    resourceDescriptor.getAttributeDescriptors().add
                        (pluralAttribute);
                    attributeDescriptors.add(pluralAttribute);
                  }
                  else
                  {
                    if (ctype.isComplexType())
                    {
                      // complex; e.g., name
                      XSComplexType complexType = s.getComplexType(cname);
                      AttributeDescriptor complexAttribute =
                          createComplexAttribute(cname, complexType);
                      if (complexAttribute != null)
                      {
                        resourceDescriptor.getAttributeDescriptors().add
                            (complexAttribute);
                        attributeDescriptors.add(complexAttribute);
                      }
                    }
                    else
                    {
                      // simple; e.g., userName
                      AttributeDescriptor simpleAttributeDescriptor =
                          new AttributeDescriptor(
                              new AttributeDescriptor.Builder(
                                  c.getOwnerSchema().getTargetNamespace(),
                                  cxsElementDecl.getName()).dataType(
                                  AttributeDescriptor.DataType.parse(
                                      cxsElementDecl.getType().getName()))
                              );
                      resourceDescriptor.getAttributeDescriptors().add
                          (simpleAttributeDescriptor);
                      attributeDescriptors.add(simpleAttributeDescriptor);
                    }
                  }
                }
              }
            }
          }
        }

        schemaList.add(schema);
      }
    }

    this.schemas = schemaList;
  }



  /**
   * Helper to create AttributeDescriptor.
   *
   * @param particle The XML particle representing a plural attribute.
   *
   * @return The SCIM descriptor equivalent of an XML schema plural attribute.
   */
  private AttributeDescriptor createPluralAttribute(final XSParticle particle)
  {
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

    AttributeDescriptor pluralAttribute =
        new AttributeDescriptor(
            new AttributeDescriptor.Builder(
                complexType.getOwnerSchema().getTargetNamespace(),
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
   *
   * @return The complex SCIM Attribute Descriptor.
   */
  private AttributeDescriptor createComplexAttribute(final String name, final
  XSComplexType complexType)
  {
    AttributeDescriptor attributeDescriptor;
    List<AttributeDescriptor> complexAttributeDescriptors = new
        ArrayList<AttributeDescriptor>();

    XSParticle[] children =
        complexType.getContentType().asParticle().getTerm().asModelGroup()
            .getChildren();
    for (XSParticle particle : children)
    {
      XSElementDecl xsElementDecl = particle.getTerm().asElementDecl();
      if (xsElementDecl != null)
      {
        AttributeDescriptor peerAttributeDescriptor = new AttributeDescriptor
            (new AttributeDescriptor.Builder(
                complexType.getOwnerSchema().getTargetNamespace(),
                xsElementDecl.getName()).complex(false).dataType(
                AttributeDescriptor.DataType.parse(
                    xsElementDecl.getType().getName()))
            );
        complexAttributeDescriptors.add(peerAttributeDescriptor);
      }
    }

    if (!complexAttributeDescriptors.isEmpty())
    {
      return new AttributeDescriptor(new AttributeDescriptor
          .Builder(complexType.getOwnerSchema().getTargetNamespace(),
                   name).complex(true).complexAttributeDescriptors
          (complexAttributeDescriptors)
      );
    }
    else
    {
      return null;
    }
  }
}
