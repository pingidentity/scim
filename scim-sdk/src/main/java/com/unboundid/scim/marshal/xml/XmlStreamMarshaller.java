/*
 * Copyright 2012 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */

package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.StreamMarshaller;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.ServerErrorException;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;



/**
 * This class provides a stream marshaller implementation to write a stream of
 * SCIM objects to their XML representation.
 */
public class XmlStreamMarshaller implements StreamMarshaller
{
  private static final String xsiURI =
      XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

  private final OutputStream outputStream;
  private final XMLStreamWriter xmlStreamWriter;



  /**
   * Create a new XML marshaller that writes to the provided output stream.
   * The resulting marshaller must be closed after use.
   *
   * @param outputStream  The output stream to be written by this marshaller.
   *
   * @throws SCIMException  If the marshaller could not be created.
   */
  public XmlStreamMarshaller(final OutputStream outputStream)
      throws SCIMException
  {
    this.outputStream = outputStream;

    try
    {
      final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      xmlStreamWriter =
          outputFactory.createXMLStreamWriter(outputStream, "UTF-8");
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot create XML marshaller: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void close() throws SCIMException
  {
    try
    {
      xmlStreamWriter.close();
    }
    catch (XMLStreamException e)
    {
      Debug.debugException(e);
    }

    try
    {
      outputStream.close();
    }
    catch (IOException e)
    {
      Debug.debugException(e);
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final BaseResource resource)
      throws SCIMException
  {
    try
    {
      xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
      xmlStreamWriter.setDefaultNamespace(SCIMConstants.SCHEMA_URI_CORE);

      final String resourceSchemaURI =
          resource.getResourceDescriptor().getSchema();

      xmlStreamWriter.writeStartElement(
          SCIMConstants.DEFAULT_SCHEMA_PREFIX,
          resource.getResourceDescriptor().getName(), resourceSchemaURI);
      marshal(resource, xmlStreamWriter, null);
      xmlStreamWriter.writeEndElement();

      xmlStreamWriter.writeEndDocument();
    }
    catch (XMLStreamException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write resource: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMException response)
      throws SCIMException
  {
    try
    {
      xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

      xmlStreamWriter.setPrefix(SCIMConstants.DEFAULT_SCHEMA_PREFIX,
          SCIMConstants.SCHEMA_URI_CORE);
      xmlStreamWriter.setPrefix("xsi", xsiURI);
      xmlStreamWriter.writeStartElement(SCIMConstants.SCHEMA_URI_CORE,
          "Response");
      xmlStreamWriter.writeNamespace(SCIMConstants.DEFAULT_SCHEMA_PREFIX,
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
    }
    catch (XMLStreamException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write error response: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final Resources<? extends BaseResource> response)
      throws SCIMException
  {
    try
    {
      xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

      xmlStreamWriter.setPrefix(SCIMConstants.DEFAULT_SCHEMA_PREFIX,
          SCIMConstants.SCHEMA_URI_CORE);
      xmlStreamWriter.setPrefix("xsi", xsiURI);
      xmlStreamWriter.writeStartElement(SCIMConstants.SCHEMA_URI_CORE,
          "Response");
      xmlStreamWriter.writeNamespace(SCIMConstants.DEFAULT_SCHEMA_PREFIX,
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
        marshal(resource, xmlStreamWriter, xsiURI);
        xmlStreamWriter.writeEndElement();
      }

      xmlStreamWriter.writeEndElement();

      xmlStreamWriter.writeEndElement();
      xmlStreamWriter.writeEndDocument();
    }
    catch (XMLStreamException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write resources: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void writeBulkStart(final int failOnErrors,
                             final Set<String> schemaURIs)
      throws SCIMException
  {
    try
    {
      xmlStreamWriter.writeStartDocument("UTF-8", "1.0");

      xmlStreamWriter.setPrefix(SCIMConstants.DEFAULT_SCHEMA_PREFIX,
          SCIMConstants.SCHEMA_URI_CORE);
      xmlStreamWriter.setPrefix("xsi", xsiURI);
      xmlStreamWriter.writeStartElement(SCIMConstants.SCHEMA_URI_CORE,
          "Bulk");
      xmlStreamWriter.writeNamespace(SCIMConstants.DEFAULT_SCHEMA_PREFIX,
          SCIMConstants.SCHEMA_URI_CORE);
      xmlStreamWriter.writeNamespace("xsi", xsiURI);

      if (failOnErrors >= 0)
      {
        xmlStreamWriter.writeStartElement("failOnErrors");
        xmlStreamWriter.writeCharacters(
            Integer.toString(failOnErrors));
        xmlStreamWriter.writeEndElement();
      }

      xmlStreamWriter.writeStartElement("Operations");
    }
    catch (XMLStreamException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write start of bulk operations: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void writeBulkOperation(final BulkOperation o)
      throws SCIMException
  {
    try
    {
      xmlStreamWriter.writeStartElement("Operation");
      if (o.getMethod() != null)
      {
        xmlStreamWriter.writeStartElement("method");
        xmlStreamWriter.writeCharacters(o.getMethod().toString());
        xmlStreamWriter.writeEndElement();
      }
      if (o.getBulkId() != null)
      {
        xmlStreamWriter.writeStartElement("bulkId");
        xmlStreamWriter.writeCharacters(o.getBulkId());
        xmlStreamWriter.writeEndElement();
      }
      if (o.getVersion() != null)
      {
        xmlStreamWriter.writeStartElement("version");
        xmlStreamWriter.writeCharacters(o.getVersion());
        xmlStreamWriter.writeEndElement();
      }
      if (o.getPath() != null)
      {
        xmlStreamWriter.writeStartElement("path");
        xmlStreamWriter.writeCharacters(o.getPath());
        xmlStreamWriter.writeEndElement();
      }
      if (o.getLocation() != null)
      {
        xmlStreamWriter.writeStartElement("location");
        xmlStreamWriter.writeCharacters(o.getLocation());
        xmlStreamWriter.writeEndElement();
      }
      if (o.getData() != null)
      {
        xmlStreamWriter.writeStartElement("data");
        marshal(o.getData(), xmlStreamWriter, xsiURI);
        xmlStreamWriter.writeEndElement();
      }
      if (o.getStatus() != null)
      {
        xmlStreamWriter.writeStartElement("status");
        xmlStreamWriter.writeStartElement("code");
        xmlStreamWriter.writeCharacters(o.getStatus().getCode());
        xmlStreamWriter.writeEndElement();
        if (o.getStatus().getDescription() != null)
        {
          xmlStreamWriter.writeStartElement("description");
          xmlStreamWriter.writeCharacters(o.getStatus().getDescription());
          xmlStreamWriter.writeEndElement();
        }
        xmlStreamWriter.writeEndElement();
      }
      xmlStreamWriter.writeEndElement();
    }
    catch (XMLStreamException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write bulk operation: " + e.getMessage());
    }
  }


  /**
   * {@inheritDoc}
   */
  public void writeBulkFinish()
      throws SCIMException
  {
    try
    {
      xmlStreamWriter.writeEndElement();
      xmlStreamWriter.writeEndElement();
      xmlStreamWriter.writeEndDocument();
    }
    catch (XMLStreamException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write end of bulk operations: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void bulkMarshal(final int failOnErrors,
                          final List<BulkOperation> operations)
      throws SCIMException
  {
    writeBulkStart(failOnErrors, Collections.<String>emptySet());
    for (final BulkOperation o : operations)
    {
      writeBulkOperation(o);
    }
    writeBulkFinish();
  }



  /**
   * Write a SCIM object to an XML stream.
   *
   * @param resource        The SCIM resource to be written.
   * @param xmlStreamWriter The stream to which the SCIM object should be
   *                        written.
   * @param xsiURI          The xsi URI to use for the type attribute.
   * @throws XMLStreamException If the object could not be written.
   */
  private void marshal(final BaseResource resource,
                       final XMLStreamWriter xmlStreamWriter,
                       final String xsiURI)
    throws XMLStreamException
  {
    final String resourceSchemaURI =
        resource.getResourceDescriptor().getSchema();

    int i = 1;
    for (final String schemaURI :
        resource.getResourceDescriptor().getAttributeSchemas())
    {
      if (schemaURI.equalsIgnoreCase(resourceSchemaURI))
      {
        final String prefix = SCIMConstants.DEFAULT_SCHEMA_PREFIX;
        xmlStreamWriter.setPrefix(prefix, schemaURI);
        xmlStreamWriter.writeNamespace(prefix, schemaURI);
      }
      else if (resource.getScimObject().hasSchema(schemaURI))
      {
        final String prefix = "ns" + String.valueOf(i++);
        xmlStreamWriter.setPrefix(prefix, schemaURI);
        xmlStreamWriter.writeNamespace(prefix, schemaURI);
      }
    }

    if (xsiURI != null)
    {
      xmlStreamWriter.writeAttribute(xsiURI, "type",
          SCIMConstants.DEFAULT_SCHEMA_PREFIX + ':' +
              resource.getResourceDescriptor().getName());
    }

    // Write the resource attributes in the order defined by the
    // resource descriptor.
    for (final AttributeDescriptor attributeDescriptor :
        resource.getResourceDescriptor().getAttributes())
    {
      final SCIMAttribute a =
          resource.getScimObject().getAttribute(
              attributeDescriptor.getSchema(), attributeDescriptor.getName());
      if (a != null)
      {
        if (a.getAttributeDescriptor().isMultiValued())
        {
          writeMultiValuedAttribute(a, xmlStreamWriter);
        }
        else
        {
          writeSingularAttribute(a, xmlStreamWriter);
        }
      }
    }
  }



  /**
   * Write a multi-valued attribute to an XML stream.
   *
   * @param scimAttribute   The attribute to be written.
   * @param xmlStreamWriter The stream to which the attribute should be
   *                        written.
   * @throws XMLStreamException If the attribute could not be written.
   */
  private void writeMultiValuedAttribute(final SCIMAttribute scimAttribute,
                                         final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException
  {
    final SCIMAttributeValue[] values = scimAttribute.getValues();

    writeStartElement(scimAttribute, xmlStreamWriter);

    for (final SCIMAttributeValue value : values)
    {
      writeChildStartElement(scimAttribute, xmlStreamWriter);

      // Write the subordinate attributes in the order defined by the schema.
      for (final AttributeDescriptor descriptor :
          scimAttribute.getAttributeDescriptor().getSubAttributes())
      {
        final SCIMAttribute a = value.getAttribute(descriptor.getName());
        if (a != null)
        {
          if (a.getAttributeDescriptor().isMultiValued())
          {
            writeMultiValuedAttribute(a, xmlStreamWriter);
          }
          else
          {
            writeSingularAttribute(a, xmlStreamWriter);
          }
        }
      }

      xmlStreamWriter.writeEndElement();
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

    final SCIMAttributeValue val = scimAttribute.getValue();

    if (val.isComplex())
    {
      // Write the subordinate attributes in the order defined by the schema.
      for (final AttributeDescriptor ad :
          attributeDescriptor.getSubAttributes())
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
          scimAttribute.getValue().getStringValue();
      xmlStreamWriter.writeCharacters(stringValue);
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
    if (scimAttribute.getSchema().equalsIgnoreCase(
        SCIMConstants.SCHEMA_URI_CORE))
    {
      xmlStreamWriter.writeStartElement(scimAttribute.getName());
    }
    else
    {
      xmlStreamWriter.writeStartElement(scimAttribute.getSchema(),
        scimAttribute.getName());
    }
  }





  /**
   * Helper that writes namespace when needed.
   * @param scimAttribute Attribute tag to write.
   * @param xmlStreamWriter Writer to write with.
   * @throws XMLStreamException thrown if error writing the tag element.
   */
  private void writeChildStartElement(final SCIMAttribute scimAttribute,
                                      final XMLStreamWriter xmlStreamWriter)
    throws XMLStreamException
  {
    if (scimAttribute.getSchema().equalsIgnoreCase(
        SCIMConstants.SCHEMA_URI_CORE))
    {
      xmlStreamWriter.writeStartElement(scimAttribute.getAttributeDescriptor().
          getMultiValuedChildName());
    }
    else
    {
      xmlStreamWriter.writeStartElement(scimAttribute.getSchema(),
        scimAttribute.getAttributeDescriptor().getMultiValuedChildName());
    }
  }
}
