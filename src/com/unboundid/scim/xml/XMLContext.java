/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.xml;

import com.unboundid.scim.schema.Group;
import com.unboundid.scim.schema.ObjectFactory;
import com.unboundid.scim.schema.Resource;
import com.unboundid.scim.schema.Response;
import com.unboundid.scim.schema.User;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;



/**
 * This class provides methods to read and write Simple Cloud Identity
 * Management (SCIM) objects in XML format. This class and its methods are
 * required to be thread-safe.
 */
public class XMLContext
{
  /**
   * A JAXB context used to create marshallers and unmarshallers.
   */
  private final JAXBContext jaxbContext;



  /**
   * Create a new XML context.
   */
  public XMLContext()
  {
    try
    {
      final ObjectFactory jaxbFactory = new ObjectFactory();
      String packageName = jaxbFactory.getClass().getPackage().getName();
      jaxbContext = JAXBContext.newInstance(packageName);
    }
    catch (JAXBException e)
    {
      throw new RuntimeException("Cannot create JAXB contexts for XML", e);
    }
  }



  /**
   * Writes a SCIM resource object to its XML representation.
   *
   * @param writer    The writer to which the XML representation will be
   *                  written.
   * @param resource  The SCIM resource to be written.
   *
   * @throws java.io.IOException  If an error occurs while writing the object.
   */
  public void writeResource(final Writer writer, final Resource resource)
      throws IOException
  {
    try
    {
      final Marshaller marshaller = jaxbContext.createMarshaller();
      if (resource instanceof User)
      {
        marshaller.marshal(
            new ObjectFactory().createUser((User)resource), writer);
      }
      else if (resource instanceof Group)
      {
        marshaller.marshal(
            new ObjectFactory().createGroup((Group) resource), writer);
      }
      else
      {
        throw new IllegalArgumentException("Cannot write resources of class " +
                                           resource.getClass().getName());
      }
    }
    catch (Exception e)
    {
      throw new IOException("Error writing XML to a character stream", e);
    }
  }



  /**
   * Reads a SCIM response from a string containing the XML representation
   * of the response.
   *
   * @param xmlString  The string from which the response will be read.
   *
   * @return  The SCIM response that was read.
   *
   * @throws IOException  If an error occurs while reading the response.
   */
  public Response readResponse(final String xmlString)
      throws IOException
  {
    try
    {
      final Reader reader = new StringReader(xmlString);
      try
      {
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final JAXBElement jaxbElement =
            (JAXBElement) unmarshaller.unmarshal(reader);

        // Note that a single resource is encoded without the response wrapper.
        final Object value = jaxbElement.getValue();
        if (value instanceof Response)
        {
          return (Response) value;
        }
        else if (value instanceof Resource)
        {
          final Response response = new Response();
          response.setResource((Resource) value);

          return response;
        }
        else
        {
          throw new IOException(
              "Expected a Response or Resource in the server response but " +
              "received the element type: " +
              jaxbElement.getDeclaredType().getCanonicalName());
        }
      }
      finally
      {
        reader.close();
      }
    }
    catch (Exception e)
    {
      throw new IOException("Error reading a response from an XML string", e);
    }
  }
}
