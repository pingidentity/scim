/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.json;

import com.unboundid.scim.schema.User;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;



/**
 * This class provides methods to read and write Simple Cloud Identity
 * Management (SCIM) objects in JSON format. This class and its methods are
 * required to be thread-safe.
 */
public class JSONContext
{
  /**
   * A JAXB context used to create marshallers and unmarshallers.
   */
  private final JAXBContext jaxbContext;

  /**
   * A jettison configuration.
   */
  private final Configuration config;



  /**
   * Create a new JSON context.
   */
  public JSONContext()
  {
    try
    {
      jaxbContext = JAXBContext.newInstance(User.class);
    }
    catch (JAXBException e)
    {
      throw new RuntimeException("Cannot create JAXB contexts for JSON", e);
    }

    final Map<String, String> xmlToJsonNamespaces =
        new HashMap<String,String>();
    xmlToJsonNamespaces.put("urn:scim:schemas:core:1.0", "");

    config = new Configuration();
    config.setXmlToJsonNamespaces(xmlToJsonNamespaces);
  }



  /**
   * Writes a SCIM user object to its JSON representation.
   *
   * @param writer  The writer to which the JSON representation will be written.
   * @param user    The SCIM user to be written.
   *
   * @throws IOException  If an error occurs while writing the object.
   */
  public void writeUser(final Writer writer, final User user)
      throws IOException
  {
    try
    {
      final MappedNamespaceConvention convention =
          new MappedNamespaceConvention(config);
      final XMLStreamWriter xmlStreamWriter =
          new MappedXMLStreamWriter(convention, writer);
      try
      {
        final Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.marshal(user, xmlStreamWriter);
      }
      finally
      {
        xmlStreamWriter.close();
      }
    }
    catch (Exception e)
    {
      throw new IOException("Error writing JSON to a character stream", e);
    }
  }



  /**
   * Reads a SCIM user object from a character stream containing the user's
   * JSON representation.
   *
   * @param reader  The reader from which the JSON representation will be read.
   *
   * @return  The SCIM user that was read.
   *
   * @throws IOException  If an error occurs while reading the object.
   */
  public User readUser(final Reader reader)
      throws IOException
  {
    // jettison does not appear to do this.
    throw new UnsupportedOperationException("Reading JSON from a character " +
                                            "stream is not implemented");
  }



  /**
   * Reads a SCIM user object from a string containing the user's JSON
   * representation.
   *
   * @param jsonString  The string from which the JSON representation will be
   *                    read.
   *
   * @return  The SCIM user that was read.
   *
   * @throws IOException  If an error occurs while reading the object.
   */
  public User readUser(final String jsonString)
      throws IOException
  {
    try
    {
      final MappedNamespaceConvention convention =
          new MappedNamespaceConvention(config);
      final XMLStreamReader xmlStreamReader =
          new MappedXMLStreamReader(new JSONObject(jsonString), convention);
      try
      {
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (User)unmarshaller.unmarshal(xmlStreamReader);
      }
      finally
      {
        xmlStreamReader.close();
      }
    }
    catch (Exception e)
    {
      throw new IOException("Error reading JSON from a string", e);
    }
  }
}
