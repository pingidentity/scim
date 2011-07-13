/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.Schema;
import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;



/**
 * This class provides a SCIM object un-marshaller implementation to read SCIM
 * objects from their XML representation.
 */
public class XmlUnmarshaller implements Unmarshaller
{

  /**
   * {@inheritDoc}
   */
  public SCIMObject unmarshal(final File file) throws Exception
  {
    return this.unmarshal(new FileInputStream(file));
  }



  /**
   * {@inheritDoc}
   */
  public SCIMObject unmarshal(final InputStream inputStream) throws Exception
  {
    SCIMObject scimObject = new SCIMObject();
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setNamespaceAware(true);
    dbFactory.setIgnoringElementContentWhitespace(true);
    dbFactory.setValidating(true);
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(inputStream);
    doc.getDocumentElement().normalize();

    final SchemaManager schemaManager = SchemaManager.instance();
    Element documentElement = doc.getDocumentElement();
    ResourceDescriptor resourceDescriptor =
        schemaManager.getResourceDescriptor(documentElement.getLocalName());
    if (resourceDescriptor == null)
    {
      throw new RuntimeException("No resource descriptor found for " +
                                 SCIMConstants.RESOURCE_NAME_USER);
    }

    scimObject.setResourceName(resourceDescriptor.getName());

    final String documentNamespaceURI = documentElement.getNamespaceURI();

    NodeList nodeList = doc.getElementsByTagName("*");
    for (int i = 0; i < nodeList.getLength(); i++)
    {
      final Node element = nodeList.item(i);

      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI == null)
      {
        namespaceURI = documentNamespaceURI; // TODO: not sure about this
      }

      final Schema schema = schemaManager.getSchema(namespaceURI);

      if (schema != null)
      {
        final AttributeDescriptor attributeDescriptor =
            schema.getAttribute(element.getLocalName());

        if (attributeDescriptor != null)
        {
          final SCIMAttribute attr;
          if (attributeDescriptor.isPlural())
          {
            attr = createPluralAttribute(element, attributeDescriptor);
          }
          else if (attributeDescriptor.isComplex())
          {
            attr = createComplexAttribute(element, attributeDescriptor);
          }
          else
          {
            attr = this.createSimpleAttribute(element, attributeDescriptor);
          }

          scimObject.addAttribute(attr);
        }
      }
    }

    return scimObject;
  }



  /**
   * Parse a simple attribute from its representation as a DOM node.
   *
   * @param node                The DOM node representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   */
  private SCIMAttribute createSimpleAttribute(
      final Node node,
      final AttributeDescriptor attributeDescriptor)
  {
    final SCIMAttributeValue v;

    if (attributeDescriptor.getDataType() != null)
    {
      switch (attributeDescriptor.getDataType())
      {
        case DATETIME:
          final Calendar calendar =
              DatatypeConverter.parseDateTime(node.getTextContent());
          v = SCIMAttributeValue.createDateValue(calendar.getTime());
          break;

        case BOOLEAN:
          v = SCIMAttributeValue.createBooleanValue(
              Boolean.valueOf(node.getTextContent()));
          break;

        default:
          v = SCIMAttributeValue.createStringValue(node.getTextContent());
          break;
      }
    }
    else
    {
      v = SCIMAttributeValue.createStringValue(node.getTextContent());
    }

    return SCIMAttribute.createSingularAttribute(attributeDescriptor, v);
  }



  /**
   * Parse a plural attribute from its representation as a DOM node.
   *
   * @param node                The DOM node representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   */
  private SCIMAttribute createPluralAttribute(
      final Node node,
      final AttributeDescriptor attributeDescriptor)
  {
    NodeList pluralAttributes = node.getChildNodes();
    List<SCIMAttributeValue> pluralScimAttributes =
        new LinkedList<SCIMAttributeValue>();
    for (int i = 0; i < pluralAttributes.getLength(); i++)
    {
      Node pluralAttribute = pluralAttributes.item(i);
      if (pluralAttribute.getNodeType() != Node.ELEMENT_NODE)
      {
        continue;
      }
      AttributeDescriptor pluralAttributeDescriptorInstance =
          attributeDescriptor.getAttribute(pluralAttribute.getNodeName());
      pluralScimAttributes.add(SCIMAttributeValue.createComplexValue(
          createComplexAttribute(pluralAttributes.item(i),
                                 pluralAttributeDescriptorInstance)));
    }
    SCIMAttributeValue[] vals =
        new SCIMAttributeValue[pluralScimAttributes.size()];
    vals = pluralScimAttributes.toArray(vals);
    return SCIMAttribute.createPluralAttribute(attributeDescriptor, vals);
  }



  /**
   * Parse a complex attribute from its representation as a DOM node.
   *
   * @param node                The DOM node representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   */
  private SCIMAttribute createComplexAttribute(
      final Node node,
      final AttributeDescriptor attributeDescriptor)
  {
    SCIMAttribute complexScimAttr;
    NodeList childNodes = node.getChildNodes();
    List<SCIMAttribute> complexAttrs = new LinkedList<SCIMAttribute>();
    for (int i = 0; i < childNodes.getLength(); i++)
    {
      Node item1 = childNodes.item(i);
      if (item1.getNodeType() == Node.ELEMENT_NODE)
      {
        AttributeDescriptor complexAttr =
            attributeDescriptor.getAttribute(item1.getNodeName());
        SCIMAttribute childAttr = createSimpleAttribute(item1, complexAttr);
        complexAttrs.add(childAttr);
      }
    }
    complexScimAttr =
        SCIMAttribute.createSingularAttribute(
            attributeDescriptor,
            SCIMAttributeValue.createComplexValue(complexAttrs));
    return complexScimAttr;
  }
}
