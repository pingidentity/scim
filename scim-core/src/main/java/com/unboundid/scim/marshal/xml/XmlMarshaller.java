/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMException;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;



/**
 * This class provides a SCIM object marshaller implementation to write SCIM
 * objects to their XML representation.
 */
public class XmlMarshaller implements Marshaller
{
  /**
   * {@inheritDoc}
   */
  public void marshal(final BaseResource resource,
                      final OutputStream outputStream) throws Exception
  {
    final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    final XMLStreamWriter xmlStreamWriter =
      outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
    marshal(resource, xmlStreamWriter);
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMException response,
                      final OutputStream outputStream) throws Exception {
    final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    final XMLStreamWriter xmlStreamWriter =
        outputFactory.createXMLStreamWriter(outputStream, "UTF-8");

    final String xsiURI = "http://www.w3.org/2001/XMLSchema-instance";

    xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

    xmlStreamWriter.setPrefix(Context.DEFAULT_SCHEMA_PREFIX,
        SCIMConstants.SCHEMA_URI_CORE);
    xmlStreamWriter.setPrefix("xsi", xsiURI);
    xmlStreamWriter.writeStartElement(SCIMConstants.SCHEMA_URI_CORE,
        "Response");
    xmlStreamWriter.writeNamespace(Context.DEFAULT_SCHEMA_PREFIX,
        SCIMConstants.SCHEMA_URI_CORE);
    xmlStreamWriter.writeNamespace("xsi", xsiURI);

    xmlStreamWriter.writeStartElement(
        SCIMConstants.SCHEMA_URI_CORE, "Errors");

    xmlStreamWriter.writeStartElement(
        SCIMConstants.SCHEMA_URI_CORE, "Error");

    xmlStreamWriter.writeStartElement(
        SCIMConstants.SCHEMA_URI_CORE, "code");
    xmlStreamWriter.writeCharacters(String.valueOf(response.getStatusCode()));
    xmlStreamWriter.writeEndElement();

    final String description = response.getMessage();
    if (description != null)
    {
      xmlStreamWriter.writeStartElement(
          SCIMConstants.SCHEMA_URI_CORE, "description");
      xmlStreamWriter.writeCharacters(description);
      xmlStreamWriter.writeEndElement();
    }

    xmlStreamWriter.writeEndElement();
    xmlStreamWriter.writeEndElement();
    xmlStreamWriter.writeEndElement();
    xmlStreamWriter.writeEndDocument();
    xmlStreamWriter.flush();
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final Resources<? extends BaseResource> response,
                      final OutputStream outputStream) throws Exception {
    final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    final XMLStreamWriter xmlStreamWriter =
        outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
    final String xsiURI = "http://www.w3.org/2001/XMLSchema-instance";
    final SchemaManager descriptorManager =
        SchemaManager.instance();

    xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

    xmlStreamWriter.setPrefix(Context.DEFAULT_SCHEMA_PREFIX,
        SCIMConstants.SCHEMA_URI_CORE);
    xmlStreamWriter.setPrefix("xsi", xsiURI);
    xmlStreamWriter.writeStartElement(SCIMConstants.SCHEMA_URI_CORE,
        "Response");
    xmlStreamWriter.writeNamespace(Context.DEFAULT_SCHEMA_PREFIX,
        SCIMConstants.SCHEMA_URI_CORE);
    xmlStreamWriter.writeNamespace("xsi", xsiURI);

    xmlStreamWriter.writeStartElement("totalResults");
    xmlStreamWriter.writeCharacters(
        Long.toString(response.getTotalResults()));
    xmlStreamWriter.writeEndElement();

    xmlStreamWriter.writeStartElement("itemsPerPage");
    xmlStreamWriter.writeCharacters(
        Integer.toString(response.getItemsPerPage()));
    xmlStreamWriter.writeEndElement();

    xmlStreamWriter.writeStartElement("startIndex");
    xmlStreamWriter.writeCharacters(
        Long.toString(response.getStartIndex()));
    xmlStreamWriter.writeEndElement();

    xmlStreamWriter.writeStartElement("Resources");

    for (final BaseResource resource : response)
    {
      xmlStreamWriter.writeStartElement("Resource");

      final ResourceDescriptor resourceDescriptor =
          resource.getResourceDescriptor();
      final String resourceSchemaURI = resourceDescriptor.getSchema();

      // Make a list of schemas referenced by extension attributes.
      final List<String> extensionSchemas = new ArrayList<String>();
      for (final String schemaURI : resource.getScimObject().getSchemas())
      {
        if (!schemaURI.equals(resourceSchemaURI))
        {
          extensionSchemas.add(schemaURI);
        }
      }

      for (int i = 0; i < extensionSchemas.size(); i++)
      {
        final String schemaURI = extensionSchemas.get(i);
        final String prefix = "ns" + String.valueOf(i + 1);
        xmlStreamWriter.setPrefix(prefix, schemaURI);
        xmlStreamWriter.writeNamespace(prefix, schemaURI);
      }

      xmlStreamWriter.writeAttribute(xsiURI, "type",
          Context.DEFAULT_SCHEMA_PREFIX + ':' +
              resourceDescriptor.getName());

      // Write the resource attributes first in the order defined by the
      // resource descriptor.
        for (final AttributeDescriptor attributeDescriptor :
            resourceDescriptor.getAttributeDescriptors())
        {
          final SCIMAttribute a =
              resource.getScimObject().
                  getAttribute(attributeDescriptor.getSchema(),
                      attributeDescriptor.getName());
          if (a != null)
          {
            if (a.isPlural())
            {
              writePluralAttribute(a, xmlStreamWriter);
            }
            else
            {
              writeSingularAttribute(a, xmlStreamWriter);
            }
          }
        }

      // Now write any extension attributes, grouped by the schema
      // extension they belong to.
      for (final String schemaURI : resource.getScimObject().getSchemas())
      {
        // Skip the resource schema.
        if (!schemaURI.equals(resourceSchemaURI))
        {
          for (final SCIMAttribute a :
              resource.getScimObject().getAttributes(schemaURI))
          {
            if (a.isPlural())
            {
              writePluralAttribute(a, xmlStreamWriter);
            }
            else
            {
              writeSingularAttribute(a, xmlStreamWriter);
            }
          }
        }
      }

      xmlStreamWriter.writeEndElement();
    }

    xmlStreamWriter.writeEndElement();

    xmlStreamWriter.writeEndElement();
    xmlStreamWriter.writeEndDocument();
    xmlStreamWriter.flush();
  }




  /**
   * Write a SCIM object to an XML stream.
   *
   * @param resource      The SCIM resource to be written.
   * @param xmlStreamWriter The stream to which the SCIM object should be
   *                        written.
   * @throws XMLStreamException If the object could not be written.
   */
  private void marshal(final BaseResource resource,
                       final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException {
    xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

    xmlStreamWriter.setDefaultNamespace(SCIMConstants.SCHEMA_URI_CORE);

    final String resourceSchemaURI =
        resource.getResourceDescriptor().getSchema();

    // Make a list of schemas referenced by extension attributes.
    final List<String> extensionSchemas = new ArrayList<String>();
    for (final String schemaURI : resource.getScimObject().getSchemas())
    {
      if (!schemaURI.equals(resourceSchemaURI))
      {
        extensionSchemas.add(schemaURI);
      }
    }

    xmlStreamWriter.writeStartElement(Context.DEFAULT_SCHEMA_PREFIX,
      resource.getResourceDescriptor().getName(), resourceSchemaURI);
    xmlStreamWriter.setPrefix(Context.DEFAULT_SCHEMA_PREFIX,
      resourceSchemaURI);
    xmlStreamWriter.writeNamespace(Context.DEFAULT_SCHEMA_PREFIX,
                                   resourceSchemaURI);

    for (int i = 0; i < extensionSchemas.size(); i++)
    {
      final String schemaURI = extensionSchemas.get(i);
      final String prefix = "ns" + String.valueOf(i + 1);
      xmlStreamWriter.setPrefix(prefix, schemaURI);
      xmlStreamWriter.writeNamespace(prefix, schemaURI);
    }

    // Write the resource attributes first in the order defined by the
    // resource descriptor.
    for (final AttributeDescriptor attributeDescriptor :
        resource.getResourceDescriptor().getAttributeDescriptors())
    {
      final SCIMAttribute a =
          resource.getScimObject().getAttribute(
              attributeDescriptor.getSchema(), attributeDescriptor.getName());
      if (a != null)
      {
        if (a.isPlural())
        {
          writePluralAttribute(a, xmlStreamWriter);
        }
        else
        {
          writeSingularAttribute(a, xmlStreamWriter);
        }
      }
    }

    // Now write any extension attributes, grouped by the schema
    // extension they belong to.
    for (final String schemaURI : extensionSchemas)
    {
      // Skip the resource schema.
      if (!schemaURI.equals(resourceSchemaURI))
      {
        for (final SCIMAttribute a :
            resource.getScimObject().getAttributes(schemaURI))
        {
          if (a.isPlural())
          {
            writePluralAttribute(a, xmlStreamWriter);
          }
          else
          {
            writeSingularAttribute(a, xmlStreamWriter);
          }
        }
      }
    }

    xmlStreamWriter.writeEndElement();
    xmlStreamWriter.writeEndDocument();
    xmlStreamWriter.flush();
  }



  /**
   * Write a plural attribute to an XML stream.
   *
   * @param scimAttribute   The attribute to be written.
   * @param xmlStreamWriter The stream to which the attribute should be
   *                        written.
   * @throws XMLStreamException If the attribute could not be written.
   */
  private void writePluralAttribute(final SCIMAttribute scimAttribute,
                                    final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException
  {
    final SCIMAttributeValue[] pluralValues = scimAttribute.getPluralValues();

    writeStartElement(scimAttribute, xmlStreamWriter);

    final List<AttributeDescriptor> mappedAttributeDescriptors =
      scimAttribute.getAttributeDescriptor().getComplexAttributeDescriptors();
    for (final SCIMAttributeValue pluralValue : pluralValues)
    {
      for (AttributeDescriptor attributeDescriptor : mappedAttributeDescriptors)
      {
        final SCIMAttribute attribute =
            pluralValue.getAttribute(attributeDescriptor.getName());

        if (attribute != null)
        {
          writeComplexAttribute(attribute, xmlStreamWriter);
        }
      }
    }

    xmlStreamWriter.writeEndElement();
  }



  /**
   * Write a singular attribute to an XML stream.
   *
   * @param scimAttribute   The attribute to be written.
   * @param xmlStreamWriter The stream to which the attribute should be
   *                        written.
   * @throws XMLStreamException If the attribute could not be written.
   */
  private void writeSingularAttribute(final SCIMAttribute scimAttribute,
                                      final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException
  {
    final AttributeDescriptor attributeDescriptor =
        scimAttribute.getAttributeDescriptor();

    writeStartElement(scimAttribute, xmlStreamWriter);

    final SCIMAttributeValue val = scimAttribute.getSingularValue();

    if (val.isComplex())
    {
      // Write the subordinate attributes in the order defined by the schema.
      for (final AttributeDescriptor ad :
          attributeDescriptor.getComplexAttributeDescriptors())
      {
        final SCIMAttribute a = val.getAttribute(ad.getName());
        if (a != null)
        {
          writeSingularAttribute(a, xmlStreamWriter);
        }
      }
    }
    else
    {
      final String stringValue =
          scimAttribute.getSingularValue().getStringValue();
      xmlStreamWriter.writeCharacters(stringValue);
    }

    xmlStreamWriter.writeEndElement();
  }



  /**
   * Write a complex attribute to an XML stream.
   *
   * @param scimAttribute   The attribute to be written.
   * @param xmlStreamWriter The stream to which the attribute should be
   *                        written.
   * @throws XMLStreamException If the attribute could not be written.
   */
  private void writeComplexAttribute(final SCIMAttribute scimAttribute,
                                     final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException
  {
    final AttributeDescriptor attributeDescriptor =
        scimAttribute.getAttributeDescriptor();
    final SCIMAttributeValue value = scimAttribute.getSingularValue();

    writeStartElement(scimAttribute, xmlStreamWriter);

    // Write the subordinate attributes in the order defined by the schema.
    for (final AttributeDescriptor descriptor :
        attributeDescriptor.getComplexAttributeDescriptors())
    {
      final SCIMAttribute a = value.getAttribute(descriptor.getName());
      if (a != null)
      {
        writeSingularAttribute(a, xmlStreamWriter);
      }
    }

    xmlStreamWriter.writeEndElement();
  }



  /**
   * Helper that writes namespace when needed.
   * @param scimAttribute Attribute tag to write.
   * @param xmlStreamWriter Writer to write with.
   * @throws XMLStreamException thrown if error writing the tag element.
   */
  private void writeStartElement(final SCIMAttribute scimAttribute,
                                 final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException
  {
    if (scimAttribute.getSchema().equals(SCIMConstants.SCHEMA_URI_CORE))
    {
      xmlStreamWriter.writeStartElement(scimAttribute.getName());
    }
    else
    {
      xmlStreamWriter.writeStartElement(scimAttribute.getSchema(),
        scimAttribute.getName());
    }
  }
}
