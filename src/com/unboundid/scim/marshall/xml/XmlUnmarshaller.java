/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshall.xml;

import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.marshall.Unmarshaller;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

public class XmlUnmarshaller implements Unmarshaller {

  public SCIMObject unmarshall(final File file) throws Exception {
    return this.unmarshall(new FileInputStream(file));
  }

  public SCIMObject unmarshall(final InputStream inputStream) throws Exception {
    SCIMObject scimObject = new SCIMObject();
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setNamespaceAware(true);
    dbFactory.setIgnoringElementContentWhitespace(true);
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(inputStream);
    doc.getDocumentElement().normalize();
    Element documentElement = doc.getDocumentElement();
    ResourceDescriptor resourceDescriptor =
      ResourceDescriptor.create(documentElement.getLocalName());
    for (AttributeDescriptor attributeDescriptor : resourceDescriptor
      .getAttributeDescriptors()) {
      String externalAttributeName =
        attributeDescriptor.getExternalAttributeName();
      SCIMAttribute attr;

      NodeList elem = doc.getElementsByTagName(externalAttributeName);
      Node element = elem.item(0);
      if (attributeDescriptor.isPlural()) {
        attr = createPluralAttribute(element, attributeDescriptor);
      } else if (attributeDescriptor.isComplex()) {
        attr = createComplexAttribute(element, attributeDescriptor);
      } else {
        attr = this.createSimpleAttribute(element, attributeDescriptor);
      }
      if (attr != null) {
        scimObject.addAttribute(attr);
      }
    }
    return scimObject;
  }

  private SCIMAttribute createSimpleAttribute(final Node node,
    final AttributeDescriptor attributeDescriptor) {
    return SCIMAttribute.createSingularAttribute(attributeDescriptor,
      SCIMAttributeValue.createStringValue(node.getTextContent()));
  }

  private SCIMAttribute createPluralAttribute(final Node node,
    final AttributeDescriptor attributeDescriptor) {
    NodeList pluralAttributes = node.getChildNodes();
    List<SCIMAttributeValue> pluralScimAttributes =
      new LinkedList<SCIMAttributeValue>();
    for (int i = 0; i < pluralAttributes.getLength(); i++) {
      Node pluralAttribute = pluralAttributes.item(i);
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

  private SCIMAttribute createComplexAttribute(final Node node,
    final AttributeDescriptor attributeDescriptor) {
    SCIMAttribute complexScimAttr;
    NodeList childNodes = node.getChildNodes();
    List<SCIMAttribute> complexAttrs = new LinkedList<SCIMAttribute>();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node item1 = childNodes.item(i);
      if (item1.getNodeType() == Node.ELEMENT_NODE) {
        AttributeDescriptor complexAttr =
          attributeDescriptor.getAttribute(item1.getNodeName());
        SCIMAttribute childAttr = SCIMAttribute
          .createSingularAttribute(complexAttr,
            SCIMAttributeValue.createStringValue(item1.getTextContent()));
        complexAttrs.add(childAttr);
      }
    }
    complexScimAttr = SCIMAttribute.createSingularAttribute(attributeDescriptor,
      SCIMAttributeValue.createComplexValue(complexAttrs));
    return complexScimAttr;
  }
}