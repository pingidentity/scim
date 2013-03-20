/*
 * Copyright 2012-2013 UnboundID Corp.
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
import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.marshal.BulkInputStreamWrapper;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.BulkContentHandler;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.Status;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;



/**
 * This class is a helper class to handle parsing of XML bulk operations.
 */
public class XmlBulkParser
{
  private final BulkInputStreamWrapper bulkInputStream;
  private final BulkConfig bulkConfig;
  private final BulkContentHandler handler;
  private XMLStreamReader xmlStreamReader;
  private int operationIndex = 0;
  private String defaultNamespaceURI;
  private boolean skipOperations;

  /**
   * Create a new instance of this bulk unmarshaller.
   *
   * @param inputStream  The input stream containing the bulk content to be
   *                     read.
   * @param bulkConfig   The bulk configuration settings to be enforced.
   * @param handler      A bulk operation listener to handle the content as it
   *                     is read.
   */
  public XmlBulkParser(final InputStream inputStream,
                       final BulkConfig bulkConfig,
                       final BulkContentHandler handler)
  {
    this.bulkInputStream     = new BulkInputStreamWrapper(inputStream);
    this.bulkConfig          = bulkConfig;
    this.handler             = handler;
    this.operationIndex      = 0;
    this.defaultNamespaceURI = null;
  }



  /**
   * Specify whether bulk operations should be skipped.
   *
   * @param skipOperations  {@code true} if bulk operations should be skipped.
   */
  public void setSkipOperations(final boolean skipOperations)
  {
    this.skipOperations = skipOperations;
  }



  /**
   * Reads a SCIM bulk request or response from the input stream.
   *
   * @throws SCIMException If the bulk content could not be read.
   */
  public void unmarshal()
      throws SCIMException
  {
    final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
    try
    {
      // Increase protection against XML bombs (DS-8081).
      xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

      xmlStreamReader =
          xmlInputFactory.createXMLStreamReader(bulkInputStream, "UTF-8");
      try
      {
        xmlStreamReader.require(START_DOCUMENT, null, null);

        while (xmlStreamReader.hasNext())
        {
          switch (xmlStreamReader.next())
          {
            case START_ELEMENT:
              if (xmlStreamReader.getLocalName().equals("Bulk"))
              {
                if (xmlStreamReader.getNamespaceURI() != null)
                {
                  defaultNamespaceURI = xmlStreamReader.getNamespaceURI();
                }
                if (!parseBulk())
                {
                  return;
                }
              }
              else
              {
                skipElement();
              }
              break;
          }
        }
      }
      finally
      {
        xmlStreamReader.close();
      }
    }
    catch (SCIMException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new InvalidResourceException("Error reading XML Bulk operation: " +
          e.getMessage(), e);
    }
  }



  /**
   * Parse a Bulk element, and leave the reader positioned on the
   * END_ELEMENT.
   *
   * @return  {@code true} if operations should continue to be provided,
   *          or {@code false} if the remaining operations are of no interest.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private boolean parseBulk()
      throws XMLStreamException, SCIMException
  {
    while (xmlStreamReader.hasNext())
    {
      switch (xmlStreamReader.next())
      {
        case START_ELEMENT:
          if (xmlStreamReader.getLocalName().equals("failOnErrors"))
          {
            handler.handleFailOnErrors(
                Integer.parseInt(xmlStreamReader.getElementText()));
          }
          else if (xmlStreamReader.getLocalName().equals("Operations"))
          {
            if (!parseOperations())
            {
              return false;
            }
          }
          else
          {
            skipElement();
          }
          break;

        case END_ELEMENT:
          return true;
      }
    }

    return true;
  }



  /**
   * Parse an Operations element, and leave the reader positioned on the
   * END_ELEMENT.
   *
   * @return  {@code true} if operations should continue to be provided,
   *          or {@code false} if the remaining operations are of no interest.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private boolean parseOperations()
      throws XMLStreamException, SCIMException
  {
    while (xmlStreamReader.hasNext())
    {
      switch (xmlStreamReader.next())
      {
        case START_ELEMENT:
          if (xmlStreamReader.getLocalName().equals("Operation"))
          {
            if (operationIndex >= bulkConfig.getMaxOperations())
            {
              throw SCIMException.createException(
                  413,
                  "The number of operations in the bulk operation exceeds " +
                  "maxOperations (" + bulkConfig.getMaxOperations() + ")");
            }
            if (bulkInputStream.getBytesRead() > bulkConfig.getMaxPayloadSize())
            {
              throw SCIMException.createException(
                  413,
                  "The size of the bulk operation exceeds the maxPayloadSize " +
                  "(" + bulkConfig.getMaxPayloadSize() + ")");
            }
            if (skipOperations)
            {
              skipElement();
            }
            else
            {
              if (!parseOperation())
              {
                return false;
              }
            }
            operationIndex++;
          }
          else
          {
            skipElement();
          }
          break;

        case END_ELEMENT:
          return true;
      }
    }

    return true;
  }



  /**
   * Parse an Operation element, and leave the reader positioned on the
   * END_ELEMENT.
   *
   * @return  {@code true} if operations should continue to be provided,
   *          or {@code false} if the remaining operations are of no interest.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private boolean parseOperation()
      throws XMLStreamException, SCIMException
  {
    BulkOperation.Method method = null;
    String bulkId = null;
    String version = null;
    String path = null;
    String location = null;
    BaseResource resource = null;
    Status status = null;

    String endpoint = null;

    loop:
    while (xmlStreamReader.hasNext())
    {
      switch (xmlStreamReader.next())
      {
        case START_ELEMENT:
          if (xmlStreamReader.getLocalName().equals("method"))
          {
            final String httpMethod = xmlStreamReader.getElementText();
            try
            {
              method = BulkOperation.Method.valueOf(httpMethod);
            }
            catch (IllegalArgumentException e)
            {
              throw SCIMException.createException(
                  405, "Bulk operation " + operationIndex + " specifies an " +
                       "invalid HTTP method '" + httpMethod + "'. " +
                       "Allowed methods are " +
                       Arrays.asList(BulkOperation.Method.values()));
            }
          }
          else if (xmlStreamReader.getLocalName().equals("bulkId"))
          {
            bulkId = xmlStreamReader.getElementText();
          }
          else if (xmlStreamReader.getLocalName().equals("version"))
          {
            version = xmlStreamReader.getElementText();
          }
          else if (xmlStreamReader.getLocalName().equals("path"))
          {
            path = xmlStreamReader.getElementText();
            int startPos = 0;
            if (path.charAt(startPos) == '/')
            {
              startPos++;
            }

            int endPos = path.indexOf('/', startPos);
            if (endPos == -1)
            {
              endPos = path.length();
            }

            endpoint = path.substring(startPos, endPos);
          }
          else if (xmlStreamReader.getLocalName().equals("location"))
          {
            location = xmlStreamReader.getElementText();
          }
          else if (xmlStreamReader.getLocalName().equals("data"))
          {
            if (path == null)
            {
              throw new InvalidResourceException(
                  "Bulk operation " + operationIndex + " has data but no " +
                  "path");
            }

            final ResourceDescriptor descriptor =
                handler.getResourceDescriptor(endpoint);
            if (descriptor == null)
            {
              throw new InvalidResourceException(
                  "Bulk operation " + operationIndex + " specifies an " +
                  "unknown resource endpoint '" + endpoint + "'");
            }

            resource = parseData(descriptor,
                                 BaseResource.BASE_RESOURCE_FACTORY);
          }
          else if (xmlStreamReader.getLocalName().equals("status"))
          {
            status = parseStatus();
          }
          else
          {
            skipElement();
          }
          break;

        case END_ELEMENT:
          break loop;
      }
    }

    return handler.handleOperation(
        operationIndex,
        new BulkOperation(method, bulkId, version, path, location,
                          resource, status));
  }



  /**
   * Parse a Status element, and leave the reader positioned on the
   * END_ELEMENT.
   *
   * @return The parsed status.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private Status parseStatus()
      throws XMLStreamException, SCIMException
  {
    String code = null;
    String description = null;

    loop:
    while (xmlStreamReader.hasNext())
    {
      switch (xmlStreamReader.next())
      {
        case START_ELEMENT:
          if (xmlStreamReader.getLocalName().equals("code"))
          {
            code = xmlStreamReader.getElementText();
          }
          else if (xmlStreamReader.getLocalName().equals("description"))
          {
            description = xmlStreamReader.getElementText();
          }
          else
          {
            skipElement();
          }
          break;

        case END_ELEMENT:
          break loop;
      }
    }

    return new Status(code, description);
  }



  /**
   * Parse a data element, and leave the reader positioned on the
   * END_ELEMENT.
   *
   * @param descriptor       The resource descriptor for this data element.
   * @param resourceFactory  The resource factory to use to create the resource.
   *
   * @return The resource parsed from the data element.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private BaseResource parseData(final ResourceDescriptor descriptor,
                                 final ResourceFactory resourceFactory)
      throws XMLStreamException, SCIMException
  {
    final SCIMObject scimObject = new SCIMObject();

    loop:
    while (xmlStreamReader.hasNext())
    {
      switch (xmlStreamReader.next())
      {
        case START_ELEMENT:
          scimObject.addAttribute(parseAttribute(descriptor));
          break;

        case END_ELEMENT:
          break loop;
      }
    }

    return resourceFactory.createResource(descriptor, scimObject);
  }



  /**
   * Parse a SCIM attribute element, and leave the reader positioned on the
   * END_ELEMENT.
   *
   * @param resourceDescriptor  The resource descriptor for this attribute.
   *
   * @return The SCIM object parsed from the data element.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private SCIMAttribute parseAttribute(
      final ResourceDescriptor resourceDescriptor)
      throws XMLStreamException, SCIMException
  {
    String namespaceURI = xmlStreamReader.getNamespaceURI();
    if (namespaceURI == null)
    {
      namespaceURI = defaultNamespaceURI;
    }

    final AttributeDescriptor attributeDescriptor =
        resourceDescriptor.getAttribute(namespaceURI,
                                xmlStreamReader.getLocalName());

    if (attributeDescriptor.isMultiValued())
    {
      return parseMultiValuedAttribute(attributeDescriptor);
    }
    else if (attributeDescriptor.getDataType() ==
        AttributeDescriptor.DataType.COMPLEX)
    {
      return SCIMAttribute.create(
          attributeDescriptor,
          parseComplexAttributeValue(attributeDescriptor));
    }
    else
    {
      return parseSimpleAttribute(attributeDescriptor);
    }
  }



  /**
   * Parse a SCIM simple attribute element, and leave the reader
   * positioned on the END_ELEMENT.
   *
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private SCIMAttribute parseSimpleAttribute(
      final AttributeDescriptor attributeDescriptor)
      throws XMLStreamException, SCIMException
  {
    return SCIMAttribute.create(
        attributeDescriptor,
        SCIMAttributeValue.createValue(attributeDescriptor.getDataType(),
            handler.transformValue(operationIndex,
                                   xmlStreamReader.getElementText())));
  }



  /**
   * Parse a SCIM multi-valued attribute element, and leave the reader
   * positioned on the END_ELEMENT.
   *
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private SCIMAttribute parseMultiValuedAttribute(
      final AttributeDescriptor attributeDescriptor)
      throws XMLStreamException, SCIMException
  {
    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();

    loop:
    while (xmlStreamReader.hasNext())
    {
      switch (xmlStreamReader.next())
      {
        case START_ELEMENT:
          if (xmlStreamReader.getLocalName().equals(
              attributeDescriptor.getMultiValuedChildName()))
          {
            values.add(parseComplexAttributeValue(attributeDescriptor));
          }
          break;

        case END_ELEMENT:
          break loop;
      }
    }

    SCIMAttributeValue[] vals = new SCIMAttributeValue[values.size()];
    return SCIMAttribute.create(attributeDescriptor, values.toArray(vals));
  }



  /**
   * Parse a SCIM complex attribute value element, and leave the reader
   * positioned on the END_ELEMENT.
   *
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   * @throws SCIMException       If some other error occurred.
   */
  private SCIMAttributeValue parseComplexAttributeValue(
      final AttributeDescriptor attributeDescriptor)
      throws XMLStreamException, SCIMException
  {
    List<SCIMAttribute> complexAttrs = new ArrayList<SCIMAttribute>();

    loop:
    while (xmlStreamReader.hasNext())
    {
      switch (xmlStreamReader.next())
      {
        case START_ELEMENT:
          if(xmlStreamReader.getNamespaceURI() != null &&
             !xmlStreamReader.getNamespaceURI().equalsIgnoreCase(
                 attributeDescriptor.getSchema()))
          {
            // Sub-attributes should have the same namespace URI as the complex
            // attribute.
            throw new InvalidResourceException("Sub-attribute " +
                xmlStreamReader.getLocalName() +
                " does not use the same namespace as the containing complex " +
                "attribute " + attributeDescriptor.getName());
          }

          final AttributeDescriptor subAttribute =
              attributeDescriptor.getSubAttribute(
                  xmlStreamReader.getLocalName());

          // Allow multi-valued sub-attribute as the resource schema needs this.
          final SCIMAttribute childAttr;
          if (subAttribute.isMultiValued())
          {
            childAttr = parseMultiValuedAttribute(subAttribute);
          }
          else
          {
            childAttr = parseSimpleAttribute(subAttribute);
          }
          complexAttrs.add(childAttr);
          break;

        case END_ELEMENT:
          break loop;
      }
    }

    return SCIMAttributeValue.createComplexValue(complexAttrs);
  }



  /**
   * Skip over the current element, and leave the reader positioned on the
   * END_ELEMENT.
   *
   * @throws XMLStreamException  If the XML could not be parsed.
   */
  private void skipElement()
      throws XMLStreamException
  {
    int nesting = 1;

    while (xmlStreamReader.hasNext())
    {
      switch (xmlStreamReader.next())
      {
        case START_ELEMENT:
          nesting++;
          break;
        case END_ELEMENT:
          if (--nesting == 0)
          {
            return;
          }
          break;
      }
    }
  }
}
