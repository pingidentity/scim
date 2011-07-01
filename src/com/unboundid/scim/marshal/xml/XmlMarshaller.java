/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.ResourceDescriptorManager;
import com.unboundid.scim.ldap.GenericResource;
import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.schema.Error;
import com.unboundid.scim.schema.Resource;
import com.unboundid.scim.schema.Response;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;

import javax.xml.bind.DatatypeConverter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
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
  public void marshal(final SCIMObject o, final OutputStream outputStream)
    throws Exception
  {
    final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    final XMLStreamWriter xmlStreamWriter =
      outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
    marshal(o, xmlStreamWriter);
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final File file)
    throws Exception
  {
    throw new UnsupportedOperationException(
        "XML marshal to file is not implemented");
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final Writer writer)
    throws Exception
  {
    final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    final XMLStreamWriter xmlStreamWriter =
      outputFactory.createXMLStreamWriter(writer);
    marshal(o, xmlStreamWriter);
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final Response response, final OutputStream outputStream)
    throws Exception
  {
    final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    final XMLStreamWriter xmlStreamWriter =
      outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
    marshal(response, xmlStreamWriter);
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final Response response, final Writer writer)
    throws Exception
  {
    final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
    final XMLStreamWriter xmlStreamWriter =
      outputFactory.createXMLStreamWriter(writer);
    marshal(response, xmlStreamWriter);
  }



  /**
   * Write a SCIM object to an XML stream.
   *
   * @param o               The SCIM object to be written.
   * @param xmlStreamWriter The stream to which the SCIM object should be
   *                        written.
   * @throws XMLStreamException If the object could not be written.
   */
  private void marshal(final SCIMObject o,
                       final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException {
    xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
    int i = 0;

    final ResourceDescriptorManager descriptorManager =
        ResourceDescriptorManager.instance();

    xmlStreamWriter.setDefaultNamespace(SCIMConstants.SCHEMA_URI_CORE);

    // todo: at the moment the scim object is assumed to be a core schema
    // object
    // need to be able to identify schema properly
    xmlStreamWriter.writeStartElement(Context.DEFAULT_SCHEMA_PREFIX,
      o.getResourceName(), SCIMConstants.SCHEMA_URI_CORE);
    xmlStreamWriter.setPrefix(Context.DEFAULT_SCHEMA_PREFIX,
      SCIMConstants.SCHEMA_URI_CORE);
    xmlStreamWriter.writeNamespace(Context.DEFAULT_SCHEMA_PREFIX,
                                   SCIMConstants.SCHEMA_URI_CORE);

    final ResourceDescriptor resourceDescriptor =
        descriptorManager.getResourceDescriptor(o.getResourceName());

    for (String schema : o.getSchemas())
    {
      if (resourceDescriptor != null)
      {
        for (final AttributeDescriptor attributeDescriptor :
            resourceDescriptor.getAttributeDescriptors())
        {
          final SCIMAttribute a =
              o.getAttribute(attributeDescriptor.getSchema(),
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
      }

      // TODO Revisit this to support schema extensions
      if (!schema.equals(SCIMConstants.SCHEMA_URI_CORE))
      {
        final String nsPrefix = "n" + i++;
        xmlStreamWriter.writeNamespace(nsPrefix, schema);

        final Collection<SCIMAttribute> attributes = o.getAttributes(schema);
        for (SCIMAttribute attr : attributes)
        {
          if (attr.isPlural())
          {
            this.writePluralAttribute(attr, xmlStreamWriter);
          }
          else
          {
            this.writeSingularAttribute(attr, xmlStreamWriter);
          }
        }
      }
    }

    xmlStreamWriter.writeEndElement();
    xmlStreamWriter.writeEndDocument();
    xmlStreamWriter.flush();
  }



  /**
   * Write a SCIM Response to an XML stream.
   *
   * @param response        The SCIM response to be written.
   * @param xmlStreamWriter The stream to which the SCIM response should be
   *                        written.
   *
   * @throws XMLStreamException If the response could not be written.
   */
  private void marshal(final Response response,
                       final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException
  {
    // If the response is a single resource then we omit the response wrapper.
    if (response.getResource() != null)
    {
      final GenericResource resource = (GenericResource)response.getResource();
      marshal(resource.getScimObject(), xmlStreamWriter);
      return;
    }

    // This marshaller is hand crafted rather than making use of JAXB because
    // a resource on the server is carried in a SCIMObject, which is not
    // JAXB-enabled.

    final String xsiURI = "http://www.w3.org/2001/XMLSchema-instance";
    final ResourceDescriptorManager descriptorManager =
        ResourceDescriptorManager.instance();

    xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

    xmlStreamWriter.setPrefix(Context.DEFAULT_SCHEMA_PREFIX,
                              SCIMConstants.SCHEMA_URI_CORE);
    xmlStreamWriter.setPrefix("xsi", xsiURI);
    xmlStreamWriter.writeStartElement(SCIMConstants.SCHEMA_URI_CORE,
                                      "Response");
    xmlStreamWriter.writeNamespace(Context.DEFAULT_SCHEMA_PREFIX,
                                   SCIMConstants.SCHEMA_URI_CORE);
    xmlStreamWriter.writeNamespace("xsi", xsiURI);

    final Response.Resources resources = response.getResources();
    final Response.Errors errors = response.getErrors();
    if (resources != null)
    {
      if (response.getTotalResults() != null)
      {
        xmlStreamWriter.writeStartElement("totalResults");
        xmlStreamWriter.writeCharacters(
            Long.toString(response.getTotalResults()));
        xmlStreamWriter.writeEndElement();
      }

      xmlStreamWriter.writeStartElement("Resources");

      for (final Resource resource : resources.getResource())
      {
        xmlStreamWriter.writeStartElement("Resource");

        // Each resource is carried as a SCIMObject wrapped into a Resource
        // instance.
        if (resource instanceof GenericResource)
        {
          final GenericResource genericResource = (GenericResource) resource;
          final SCIMObject scimObject = genericResource.getScimObject();

          xmlStreamWriter.writeAttribute(xsiURI, "type",
                                         Context.DEFAULT_SCHEMA_PREFIX + ':' +
                                         scimObject.getResourceName());

          final ResourceDescriptor resourceDescriptor =
              descriptorManager.getResourceDescriptor(
                  scimObject.getResourceName());
          if (resourceDescriptor != null)
          {
            for (final AttributeDescriptor attributeDescriptor :
                resourceDescriptor.getAttributeDescriptors())
            {
              final SCIMAttribute a =
                  scimObject.getAttribute(attributeDescriptor.getSchema(),
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
          }
        }

        xmlStreamWriter.writeEndElement();
      }

      xmlStreamWriter.writeEndElement();
    }
    else if (errors != null)
    {
      xmlStreamWriter.writeStartElement(
          SCIMConstants.SCHEMA_URI_CORE, "Errors");

      for (final Error error : errors.getError())
      {
        xmlStreamWriter.writeStartElement(
            SCIMConstants.SCHEMA_URI_CORE, "Error");

        final String description = error.getDescription();
        if (description != null)
        {
          xmlStreamWriter.writeStartElement(
              SCIMConstants.SCHEMA_URI_CORE, "description");
          xmlStreamWriter.writeCharacters(description);
          xmlStreamWriter.writeEndElement();
        }

        final String code = error.getCode();
        if (code != null)
        {
          xmlStreamWriter.writeStartElement(
              SCIMConstants.SCHEMA_URI_CORE, "code");
          xmlStreamWriter.writeCharacters(code);
          xmlStreamWriter.writeEndElement();
        }

        final String uri = error.getUri();
        if (uri != null)
        {
          xmlStreamWriter.writeStartElement(
              SCIMConstants.SCHEMA_URI_CORE, "uri");
          xmlStreamWriter.writeCharacters(uri);
          xmlStreamWriter.writeEndElement();
        }

        xmlStreamWriter.writeEndElement();
      }

      xmlStreamWriter.writeEndElement();
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
      if (attributeDescriptor.getDataType() != null)
      {
        switch (scimAttribute.getAttributeDescriptor().getDataType())
        {
          case DATETIME:
            xmlStreamWriter.writeCharacters(
                scimAttribute.getSingularValue().getDateStringValue());
            break;

          case BOOLEAN:
            Boolean booleanValue =
                scimAttribute.getSingularValue().getBooleanValue();
            xmlStreamWriter.writeCharacters(
                DatatypeConverter.printBoolean(booleanValue));
            break;

          case INTEGER:
          case STRING:
          default:
            final String stringValue =
                scimAttribute.getSingularValue().getStringValue();
            xmlStreamWriter.writeCharacters(stringValue);
            break;
        }
      }
      else
      {
        final String stringValue =
            scimAttribute.getSingularValue().getStringValue();
        xmlStreamWriter.writeCharacters(stringValue);
      }
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
