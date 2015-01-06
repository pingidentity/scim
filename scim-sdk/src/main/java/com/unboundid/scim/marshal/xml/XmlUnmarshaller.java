/*
 * Copyright 2011-2015 UnboundID Corp.
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
import com.unboundid.scim.data.QueryRequest;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.BulkContentHandler;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.ListResponse;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.ServerErrorException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



/**
 * This class provides a SCIM object un-marshaller implementation to read SCIM
 * objects from their XML representation.
 */
public class XmlUnmarshaller implements Unmarshaller
{
  /**
   * Create a document builder that is better protected against attacks from
   * XML bombs.
   *
   * @return  A document builder.
   *
   * @throws javax.xml.parsers.ParserConfigurationException
   *         If the document builder could not be created.
   */
  private DocumentBuilder createDocumentBuilder()
      throws javax.xml.parsers.ParserConfigurationException
  {
    final DocumentBuilderFactory dbFactory =
        DocumentBuilderFactory.newInstance();

    // Increase protection against XML bombs (DS-8081).
    dbFactory.setFeature(
        "http://apache.org/xml/features/disallow-doctype-decl",
        true);

    dbFactory.setNamespaceAware(true);
    dbFactory.setIgnoringElementContentWhitespace(true);
    dbFactory.setValidating(false);

    return dbFactory.newDocumentBuilder();
  }



  /**
   * {@inheritDoc}
   */
  public <R extends BaseResource> R unmarshal(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory)
      throws InvalidResourceException
  {
    final Document doc;
    try
    {
      doc = createDocumentBuilder().parse(inputStream);
      doc.getDocumentElement().normalize();
    }
    catch (Exception e)
    {
      throw new InvalidResourceException("Error reading XML: " +
          e.getMessage(), e);
    }

    final Element documentElement = doc.getDocumentElement();

    // TODO: Should we check to make sure the doc name matches the
    // resource name?
    //documentElement.getLocalName());
    if (resourceDescriptor == null)
    {
      throw new RuntimeException("No resource descriptor found for " +
          documentElement.getLocalName());
    }

    final String documentNamespaceURI = documentElement.getNamespaceURI();
    return unmarshal(documentNamespaceURI, documentElement.getChildNodes(),
        resourceDescriptor, resourceFactory);
  }

  /**
   * Read an SCIM resource from the specified node.
   *
   * @param documentNamespaceURI The namespace URI of XML document.
   * @param <R> The type of resource instance.
   * @param nodeList  The attribute nodes to be read.
   * @param resourceDescriptor The descriptor of the SCIM resource to be read.
   * @param resourceFactory The resource factory to use to create the resource
   *                        instance.
   *
   * @return  The SCIM resource that was read.
   * @throws com.unboundid.scim.sdk.InvalidResourceException if an error occurs.
   */
  private <R extends BaseResource> R unmarshal(
      final String documentNamespaceURI,
      final NodeList nodeList, final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory) throws InvalidResourceException
  {
    SCIMObject scimObject = new SCIMObject();
    for (int i = 0; i < nodeList.getLength(); i++)
    {
      final Node element = nodeList.item(i);
      if(element.getNodeType() != Node.ELEMENT_NODE)
      {
        continue;
      }

      String namespaceURI = element.getNamespaceURI();
      if (namespaceURI == null)
      {
        // Try to find the appropriate schema
        namespaceURI =
                resourceDescriptor.findAttributeSchema(element.getLocalName(),
                                                       documentNamespaceURI);
        if (namespaceURI == null)
        {
          // Fall back to this if we couldn't find it above
          namespaceURI = documentNamespaceURI;
        }
      }

      final AttributeDescriptor attributeDescriptor =
          resourceDescriptor.getAttribute(namespaceURI, element.getLocalName());

      final SCIMAttribute attr;
      if (attributeDescriptor.isMultiValued())
      {
        attr = createMultiValuedAttribute(element, attributeDescriptor);
      }
      else if (attributeDescriptor.getDataType() ==
          AttributeDescriptor.DataType.COMPLEX)
      {
        attr = SCIMAttribute.create(attributeDescriptor,
            createComplexAttribute(element, attributeDescriptor));
      }
      else
      {
        attr = createSimpleAttribute(element, attributeDescriptor);
      }

      scimObject.addAttribute(attr);
    }
    return resourceFactory.createResource(resourceDescriptor, scimObject);
  }

  /**
   * {@inheritDoc}
   */
  public <R extends BaseResource> Resources<R> unmarshalResources(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory) throws InvalidResourceException
  {
    final Document doc;
    try
    {
      doc = createDocumentBuilder().parse(inputStream);
      doc.getDocumentElement().normalize();
    }
    catch (Exception e)
    {
      throw new InvalidResourceException("Error reading XML: " +
          e.getMessage(), e);
    }

    final String documentNamespaceURI =
        doc.getDocumentElement().getNamespaceURI();
    final NodeList nodeList = doc.getElementsByTagName("*");

    int totalResults = 0;
    int startIndex = 1;
    List<R> objects = Collections.emptyList();
    for (int i = 0; i < nodeList.getLength(); i++)
    {
      final Node element = nodeList.item(i);
      if(element.getLocalName().equals("totalResults"))
      {
        totalResults = Integer.valueOf(element.getTextContent());
      }
      else if(element.getLocalName().equals("startIndex"))
      {
        startIndex = Integer.valueOf(element.getTextContent());
      }
      else if(element.getLocalName().equals("Resources"))
      {
        NodeList resources = element.getChildNodes();
        objects = new ArrayList<R>(resources.getLength());
        for(int j = 0; j < resources.getLength(); j++)
        {
          Node resource = resources.item(j);
          if(resource.getLocalName().equals("Resource"))
          {
            objects.add(
                unmarshal(documentNamespaceURI, resource.getChildNodes(),
                    resourceDescriptor, resourceFactory));
          }
        }
      }
    }

    return new Resources<R>(objects, totalResults, startIndex);
  }


  /**
   * {@inheritDoc}
   */
  public SCIMException unmarshalError(final InputStream inputStream)
      throws InvalidResourceException
  {
    final Document doc;
    try
    {
      doc = createDocumentBuilder().parse(inputStream);
      doc.getDocumentElement().normalize();
    }
    catch (Exception e)
    {
      throw new InvalidResourceException("Error reading XML: " +
          e.getMessage(), e);
    }

    final NodeList nodeList =
        doc.getDocumentElement().getFirstChild().getChildNodes();

    if(nodeList.getLength() >= 1)
    {
      String code = null;
      String description = null;
      NodeList nodes = nodeList.item(0).getChildNodes();
      for(int j = 0; j < nodes.getLength(); j++)
      {
        Node attr = nodes.item(j);
        if(attr.getLocalName().equals("code"))
        {
          code = attr.getTextContent();
        }
        else if(attr.getLocalName().equals("description"))
        {
          description = attr.getTextContent();
        }
      }
      return SCIMException.createException(Integer.valueOf(code),
          description);
    }

    return null;

  }



  /**
   * {@inheritDoc}
   */
  public void bulkUnmarshal(final InputStream inputStream,
                            final BulkConfig bulkConfig,
                            final BulkContentHandler handler)
      throws SCIMException
  {
    final XmlBulkParser xmlBulkParser =
        new XmlBulkParser(inputStream, bulkConfig, handler);
    xmlBulkParser.unmarshal();
  }



  /**
   * {@inheritDoc}
   */
  public void bulkUnmarshal(final File file,
                            final BulkConfig bulkConfig,
                            final BulkContentHandler handler)
      throws SCIMException
  {
    // First pass: ensure the number of operations is less than the max.
    final BulkContentHandler preProcessHandler = new BulkContentHandler() {};
    try
    {
      final FileInputStream fileInputStream = new FileInputStream(file);
      try
      {
        final BufferedInputStream bufferedInputStream =
            new BufferedInputStream(fileInputStream);
        try
        {
          final XmlBulkParser xmlBulkParser =
              new XmlBulkParser(bufferedInputStream, bulkConfig,
                                preProcessHandler);
          xmlBulkParser.setSkipOperations(true);
          xmlBulkParser.unmarshal();
        }
        finally
        {
          bufferedInputStream.close();
        }
      }
      finally
      {
        fileInputStream.close();
      }
    }
    catch (IOException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Error pre-processing bulk request: " + e.getMessage());
    }

    // Second pass: Parse fully.
    try
    {
      final FileInputStream fileInputStream = new FileInputStream(file);
      try
      {
        final BufferedInputStream bufferedInputStream =
            new BufferedInputStream(fileInputStream);
        try
        {
          final XmlBulkParser xmlBulkParser =
              new XmlBulkParser(bufferedInputStream, bulkConfig, handler);
          xmlBulkParser.unmarshal();
        }
        finally
        {
          bufferedInputStream.close();
        }
      }
      finally
      {
        fileInputStream.close();
      }
    }
    catch (IOException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Error parsing bulk request: " + e.getMessage());
    }
  }


  /**
   * Not available with XML.
   * {@inheritDoc}
   */
  @Override
  public <R extends BaseResource> ListResponse<R>
  unmarshalListResponse(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory)
      throws InvalidResourceException
  {
    throw new UnsupportedOperationException();
  }



  /**
   * Not available with XML.
   * {@inheritDoc}
   */
  @Override
  public QueryRequest unmarshalQueryRequest(
      final InputStream inputStream)
      throws InvalidResourceException
  {
    throw new UnsupportedOperationException();
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
    Node b64Node = node.getAttributes().getNamedItem("base64Encoded");
    String textContent = node.getTextContent();
    if(b64Node != null && Boolean.parseBoolean(b64Node.getTextContent()))
    {
      byte[] bytes = DatatypeConverter.parseBase64Binary(node.getTextContent());
      try
      {
        textContent = new String(bytes, "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
        //This should never happen with UTF-8.
        Debug.debugException(e);
      }
    }
    return SCIMAttribute.create(attributeDescriptor,
            SCIMAttributeValue.createValue(attributeDescriptor.getDataType(),
                    textContent));
  }



  /**
   * Parse a multi-valued attribute from its representation as a DOM node.
   *
   * @param node                The DOM node representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   * @throws InvalidResourceException if an error occurs.
   */
  private SCIMAttribute createMultiValuedAttribute(
      final Node node, final AttributeDescriptor attributeDescriptor)
      throws InvalidResourceException
  {
    final NodeList attributes = node.getChildNodes();
    final List<SCIMAttributeValue> values =
        new ArrayList<SCIMAttributeValue>(attributes.getLength());
    for (int i = 0; i < attributes.getLength(); i++)
    {
      final Node attribute = attributes.item(i);
      if (attribute.getNodeType() != Node.ELEMENT_NODE ||
          !attribute.getLocalName().equals(
              attributeDescriptor.getMultiValuedChildName()))
      {
        continue;
      }
      if(attribute.getChildNodes().getLength() > 1 ||
          attribute.getFirstChild().getNodeType() != Node.TEXT_NODE)
      {
        values.add(
            createComplexAttribute(attribute, attributeDescriptor));
      }
      else
      {
        Node b64Node = attribute.getAttributes().getNamedItem("base64Encoded");
        String textContent = attribute.getTextContent();
        if(b64Node != null && Boolean.parseBoolean(b64Node.getTextContent()))
        {
          byte[] bytes = DatatypeConverter.parseBase64Binary(
                            attribute.getTextContent());
          try
          {
            textContent = new String(bytes, "UTF-8");
          }
          catch(UnsupportedEncodingException e)
          {
            //This should never happen with UTF-8.
            Debug.debugException(e);
          }
        }

        SCIMAttribute subAttr = SCIMAttribute.create(
            attributeDescriptor.getSubAttribute("value"),
                SCIMAttributeValue.createValue(
                        attributeDescriptor.getDataType(), textContent));
        values.add(SCIMAttributeValue.createComplexValue(subAttr));
      }
    }
    SCIMAttributeValue[] vals = new SCIMAttributeValue[values.size()];
    vals = values.toArray(vals);
    return SCIMAttribute.create(attributeDescriptor, vals);
  }



  /**
   * Parse a complex attribute from its representation as a DOM node.
   *
   * @param node                The DOM node representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   * @throws InvalidResourceException if an error occurs.
   */
  private SCIMAttributeValue createComplexAttribute(
      final Node node, final AttributeDescriptor attributeDescriptor)
      throws InvalidResourceException
  {
    NodeList childNodes = node.getChildNodes();
    List<SCIMAttribute> complexAttrs =
        new ArrayList<SCIMAttribute>(childNodes.getLength());
    for (int i = 0; i < childNodes.getLength(); i++)
    {
      Node item1 = childNodes.item(i);
      if (item1.getNodeType() == Node.ELEMENT_NODE)
      {
        if(item1.getNamespaceURI() != null &&
            !item1.getNamespaceURI().equalsIgnoreCase(
            attributeDescriptor.getSchema()))
        {
          // Sub-attributes should have the same namespace URI as the complex
          // attribute.
          throw new InvalidResourceException("Sub-attribute " +
              item1.getNodeName() + " does not use the same namespace as the " +
              "containing complex attribute " + attributeDescriptor.getName());
        }
        SCIMAttribute childAttr;
        AttributeDescriptor subAttribute =
            attributeDescriptor.getSubAttribute(item1.getLocalName());
        // Allow multi-valued sub-attribute as the resource schema needs this.
        if(subAttribute.isMultiValued())
        {
          childAttr = createMultiValuedAttribute(item1, subAttribute);
        }
        else
        {
          childAttr = createSimpleAttribute(item1, subAttribute);
        }
        complexAttrs.add(childAttr);
      }
    }

    return SCIMAttributeValue.createComplexValue(complexAttrs);
  }
}
