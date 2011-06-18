/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.xml;

import com.unboundid.scim.schema.ObjectFactory;
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
      jaxbContext = JAXBContext.newInstance(User.class);
    }
    catch (JAXBException e)
    {
      throw new RuntimeException("Cannot create JAXB contexts for XML", e);
    }
  }



  /**
   * Writes a SCIM user object to its XML representation.
   *
   * @param writer  The writer to which the XML representation will be written.
   * @param user    The SCIM user to be written.
   *
   * @throws java.io.IOException  If an error occurs while writing the object.
   */
  public void writeUser(final Writer writer, final User user)
      throws IOException
  {
    try
    {
      final Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.marshal(new ObjectFactory().createUser(user), writer);
    }
    catch (Exception e)
    {
      throw new IOException("Error writing XML to a character stream", e);
    }
  }



  /**
   * Reads a SCIM user object from a string containing the user's XML
   * representation.
   *
   * @param xmlString  The string from which the XML representation will be
   *                   read.
   *
   * @return  The SCIM user that was read.
   *
   * @throws IOException  If an error occurs while reading the object.
   */
  public User readUser(final String xmlString)
      throws IOException
  {
    try
    {
      final Reader reader = new StringReader(xmlString);
      try
      {
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Object unmarshal = unmarshaller.unmarshal(reader);
          return (User)((JAXBElement) unmarshal).getValue();
      }
      finally
      {
        reader.close();
      }
    }
    catch (Exception e)
    {
      throw new IOException("Error reading XML from a string", e);
    }
  }
}
