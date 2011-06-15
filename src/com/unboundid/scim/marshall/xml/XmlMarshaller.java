/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshall.xml;

import com.unboundid.scim.marshall.Context;
import com.unboundid.scim.marshall.Marshaller;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.config.AttributeDescriptor;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;



/**
 * This class provides a SCIM object marshaller implementation to write SCIM
 * objects to their XML representation.
 */
public class XmlMarshaller implements Marshaller {
  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final OutputStream outputStream)
      throws Exception {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    XMLStreamWriter xmlStreamWriter =
        outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
    this.marshal(o, xmlStreamWriter);
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final File file)
      throws Exception {
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final Writer writer)
      throws Exception {
    XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    XMLStreamWriter xmlStreamWriter =
        outputFactory.createXMLStreamWriter(writer);
    this.marshal(o, xmlStreamWriter);
  }

  /**
   * Write a SCIM object to an XML stream.
   *
   * @param o                The SCIM object to be written.
   * @param xmlStreamWriter  The stream to which the SCIM object should be
   *                         written.
   *
   * @throws XMLStreamException  If the object could not be written.
   */
  private void marshal(final SCIMObject o,
                       final XMLStreamWriter xmlStreamWriter)
      throws XMLStreamException {
    xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
    int i = 0;
    String nsPrefix;
    xmlStreamWriter.setDefaultNamespace(Context.DEFAULT_SCHEMA_URN);
    xmlStreamWriter.writeStartElement("user");
    for (String schema : o.getSchemas()) {
      if (schema.equals(Context.DEFAULT_SCHEMA_URN)) {
        xmlStreamWriter.setPrefix(Context.DEFAULT_SCHEMA_PREFIX,
                                  Context.DEFAULT_SCHEMA_URN);

        xmlStreamWriter.writeNamespace(Context.DEFAULT_SCHEMA_PREFIX,
                                       Context.DEFAULT_SCHEMA_URN);
        nsPrefix = Context.DEFAULT_SCHEMA_PREFIX;
      } else {
        nsPrefix = "n" + i++;
        xmlStreamWriter.writeNamespace(nsPrefix, schema);
      }
      Collection<SCIMAttribute> attributes = o.getAttributes(schema);
      for (SCIMAttribute attr : attributes) {
        if (attr.isPlural()) {
          this.writePluralAttribute(attr, xmlStreamWriter);
        } else {
          this.writeSingularAttribute(attr, xmlStreamWriter);
        }
      }
      xmlStreamWriter.writeEndElement();
    }
    xmlStreamWriter.writeEndDocument();
    xmlStreamWriter.flush();
  }

  /**
   * Write a plural attribute to an XML stream.
   *
   * @param scimAttribute    The attribute to be written.
   * @param xmlStreamWriter  The stream to which the attribute should be
   *                         written.
   *
   * @throws XMLStreamException  If the attribute could not be written.
   */
  private void writePluralAttribute(final SCIMAttribute scimAttribute,
                                    final XMLStreamWriter xmlStreamWriter)
      throws XMLStreamException {
    SCIMAttributeValue[] pluralValues = scimAttribute.getPluralValues();
    xmlStreamWriter.writeStartElement(scimAttribute.getName());
    Set<AttributeDescriptor> mappedAttributeDescriptors =
        scimAttribute.getAttributeDescriptor().getComplexAttributeDescriptors();
    for (SCIMAttributeValue pluralValue : pluralValues) {
      for (AttributeDescriptor attributeDescriptor :
          mappedAttributeDescriptors) {
        SCIMAttribute attribute =
            pluralValue.getAttribute(
                attributeDescriptor.getExternalAttributeName());
        this.writeComplexAttribute(attribute, xmlStreamWriter);
      }
    }
    xmlStreamWriter.writeEndElement();
  }

  /**
   * Write a singular attribute to an XML stream.
   *
   * @param scimAttribute    The attribute to be written.
   * @param xmlStreamWriter  The stream to which the attribute should be
   *                         written.
   *
   * @throws XMLStreamException  If the attribute could not be written.
   */
  private void writeSingularAttribute(final SCIMAttribute scimAttribute,
                                      final XMLStreamWriter xmlStreamWriter)
      throws XMLStreamException {
    xmlStreamWriter.writeStartElement(Context.DEFAULT_SCHEMA_PREFIX,
                                      scimAttribute.getName(),
                                      Context.DEFAULT_SCHEMA_URN);
    SCIMAttributeValue val = scimAttribute.getSingularValue();
    if(val.isComplex()) {
      for(SCIMAttribute a:val.getAttributes().values()) {
        this.writeSingularAttribute(a,xmlStreamWriter);
      }
    } else {
      String stringValue = scimAttribute.getSingularValue().getStringValue();
      xmlStreamWriter.writeCharacters(stringValue);
    }
    xmlStreamWriter.writeEndElement();
  }

  /**
   * Write a complex attribute to an XML stream.
   *
   * @param scimAttribute    The attribute to be written.
   * @param xmlStreamWriter  The stream to which the attribute should be
   *                         written.
   *
   * @throws XMLStreamException  If the attribute could not be written.
   */
  private void writeComplexAttribute(final SCIMAttribute scimAttribute,
                                     final XMLStreamWriter xmlStreamWriter)
      throws XMLStreamException {
    SCIMAttributeValue value = scimAttribute.getSingularValue();
    Map<String, SCIMAttribute> attributes = value.getAttributes();
    xmlStreamWriter.writeStartElement(scimAttribute.getName());
    for (SCIMAttribute attribute : attributes.values()) {
      writeSingularAttribute(attribute, xmlStreamWriter);
    }
    xmlStreamWriter.writeEndElement();
  }
}
