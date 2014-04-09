/*
 * Copyright 2011-2014 UnboundID Corp.
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

import com.unboundid.scim.data.Address;
import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.Manager;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.SCIMTestCase;
import org.testng.annotations.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class provides test coverage for the {@link XmlMarshaller}.
 */
@Test
public class MarshallerTestCase
  extends SCIMTestCase
{

  //Set up an XML validator that will throw if the XML does not validate.
  private static final ErrorHandler ERROR_HANDLER = new ErrorHandler()
  {
    public void warning(final SAXParseException exception)
            throws SAXException
    {
      throw exception;
    }


    public void error(final SAXParseException exception)
            throws SAXException
    {
      throw exception;
    }


    public void fatalError(final SAXParseException exception)
            throws SAXException
    {
      throw exception;
    }
  };

  /**
   * Verify that a valid user can be written to XML and then read back.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testMarshal()
    throws Exception
  {
    final File testXML = File.createTempFile("test-", ".xml");
    testXML.deleteOnExit();

    final UserResource user1 = new UserResource(CoreSchema.USER_DESCRIPTOR);

    user1.setId("uid=bjensen,dc=example,dc=com");

    user1.setUserName("bjensen");

    user1.setName(new Name("Ms. Barbara J Jensen III",
        "Jensen", "Barbara", "J", "Ms.", "III"));
    Collection<Entry<String>> emails = new ArrayList<Entry<String>>(2);
    emails.add(new Entry<String>("bjensen@example.com", "work", true));
    emails.add(new Entry<String>("babs@jensen.org", "home", false));
    user1.setEmails(emails);

    Collection<Address> addresses = new ArrayList<Address>(2);
    addresses.add(
        new Address("100 Universal City Plaza\nHollywood, CA 91608 USA",
            "100 Universal City Plaza",
            "Hollywood",
            "CA",
            "91608",
            "USA",
            "work",
            true));
    addresses.add(
        new Address("456 Hollywood Blvd\nHollywood, CA 91608 USA",
            "456 Hollywood Blvd",
            "Hollywood",
            "CA",
            "91608",
            "USA",
            "home",
            false));
    user1.setAddresses(addresses);

    Collection<Entry<String>> phoneNumbers = new ArrayList<Entry<String>>(2);
    phoneNumbers.add(new Entry<String>("800-864-8377", "work", false));
    phoneNumbers.add(new Entry<String>("818-123-4567", "mobile", false));
    user1.setPhoneNumbers(phoneNumbers);

    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION,
        "employeeNumber", AttributeValueResolver.STRING_RESOLVER,"1001");
    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION,
        "organization", AttributeValueResolver.STRING_RESOLVER,"Example");
    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION,
        "division", AttributeValueResolver.STRING_RESOLVER,"People");
    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION,
        "department", AttributeValueResolver.STRING_RESOLVER,"Sales");
    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
        Manager.MANAGER_RESOLVER,
        new Manager("uid=manager,dc=example,dc=com", "The Boss"));

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final Marshaller marshaller = new XmlMarshaller();
    marshaller.marshal(user1, outputStream);
    outputStream.close();
    InputStream inputStream =
        new ByteArrayInputStream(outputStream.toByteArray());
    final Unmarshaller unmarshaller = new XmlUnmarshaller();
    final BaseResource resource = unmarshaller.unmarshal(inputStream,
        CoreSchema.USER_DESCRIPTOR, BaseResource.BASE_RESOURCE_FACTORY);
    final SCIMObject user2 = resource.getScimObject();
    inputStream.close();

    assertEquals(user1, resource);
    assertEquals(resource.getResourceDescriptor(), CoreSchema.USER_DESCRIPTOR);
    for (final String attribute : Arrays.asList("id",
                                                "addresses",
                                                "phoneNumbers",
                                                "emails",
                                                "name"))
    {
      assertTrue(user2.hasAttribute(SCHEMA_URI_CORE, attribute));
    }

    for (final String attribute : Arrays.asList("employeeNumber",
                                                "organization",
                                                "division",
                                                "department",
                                                "manager"))
    {
      assertTrue(user2.hasAttribute(SCHEMA_URI_ENTERPRISE_EXTENSION,
                                    attribute));
    }
  }

  /**
   * Verify that the XML marshaller correctly handles characters that are
   * invalid in XML (even though they may be valid in LDAP).
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testInvalidXML() throws Exception
  {
    final UserResource user1 = new UserResource(CoreSchema.USER_DESCRIPTOR);
    user1.setTitle("\"R&D Manager\"");
    user1.setDisplayName("<Good 'ol boy>");
    user1.setNickName("NickNamePlusBinary\u0013");
    Set<Entry<String>> emails = new HashSet<Entry<String>>();
    emails.add(new Entry<String>("user\u0007@example.com", "work", true));
    emails.add(new Entry<String>("user@example.com", "home", false));
    user1.setEmails(emails);

    final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
    try
    {
      final XmlMarshaller marshaller = new XmlMarshaller();
      marshaller.marshal(user1, byteArrayStream);
    }
    finally
    {
      byteArrayStream.close();
    }

    InputStream inputStream =
            new ByteArrayInputStream(byteArrayStream.toByteArray());

    //Check that the XML parses.
    final DocumentBuilderFactory documentBuilderFactory =
            DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setIgnoringElementContentWhitespace(true);
    documentBuilderFactory.setValidating(false);

    final DocumentBuilder documentBuilder =
            documentBuilderFactory.newDocumentBuilder();
    documentBuilder.setErrorHandler(ERROR_HANDLER);
    documentBuilder.parse(inputStream);

    //Check that the Unmarshaller understands the binary elements.
    inputStream = new ByteArrayInputStream(byteArrayStream.toByteArray());
    final Unmarshaller unmarshaller = new XmlUnmarshaller();
    final BaseResource resource = unmarshaller.unmarshal(inputStream,
            CoreSchema.USER_DESCRIPTOR, BaseResource.BASE_RESOURCE_FACTORY);
    final SCIMObject user2 = resource.getScimObject();
    inputStream.close();

    assertEquals(user1.getScimObject(), user2);
  }

  /**
   *
   * Verify that a bulk request written to XML is valid according to the SCIM
   * core XML schema.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testBulkMarshal()
    throws Exception
  {
    final BaseResource testUser = getTestUser();

    final int failOnErrors = 1;
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.POST, "bulkId", null,
        "/Users", testUser));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.DELETE, null, "W/\"lha5bbazU3fNvfe5\"",
        "/Users/1", null));

    final File xmlFile = File.createTempFile("test-", ".xml");
    xmlFile.deleteOnExit();

    final OutputStream outputStream = new FileOutputStream(xmlFile);
    try
    {
      final XmlMarshaller marshaller = new XmlMarshaller();
      marshaller.bulkMarshal(outputStream,  failOnErrors, operations);
    }
    finally
    {
      outputStream.close();
    }

    // Validate the XML document against the schema.
    final SchemaFactory schemaFactory =
        SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
    final Schema scimCoreSchema = schemaFactory.newSchema(
            getResourceFile("/com/unboundid/scim/schema/scim-core.xsd"));
    final DocumentBuilderFactory documentBuilderFactory =
            DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setNamespaceAware(true);
    documentBuilderFactory.setIgnoringElementContentWhitespace(true);
    documentBuilderFactory.setValidating(false);
    documentBuilderFactory.setSchema(scimCoreSchema);

    final DocumentBuilder documentBuilder =
        documentBuilderFactory.newDocumentBuilder();
    documentBuilder.setErrorHandler(ERROR_HANDLER);
    documentBuilder.parse(xmlFile);
  }
}
