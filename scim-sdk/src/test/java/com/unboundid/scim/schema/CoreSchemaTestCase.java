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

package com.unboundid.scim.schema;

import com.unboundid.scim.SCIMTestCase;
import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.sdk.SCIMConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Tests for the ResourceDescriptor class.
 */
public class CoreSchemaTestCase extends SCIMTestCase
{

  /**
   * Create a resource descriptor with the common attributes and make sure
   * duplicates are not created.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testDuplicateCommonAttrs() throws Exception
  {
    ResourceDescriptor rd =
        ResourceDescriptor.create("test", "desc", "com.unboundid", "/test",
            CoreSchema.ID_DESCRIPTOR, CoreSchema.EXTERNAL_ID_DESCRIPTOR,
            CoreSchema.META_DESCRIPTOR);

    assertEquals(rd.getAttributes().size(), 3);
    assertEquals(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.META_DESCRIPTOR.getName()).getSubAttributes().size(), 5);
  }

  /**
   * Create a multi-valued attribute descriptor with the common attributes and
   * make sure duplicates are not created.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testDuplicateCommonMultivaluedSubAttrs() throws Exception
  {
    AttributeDescriptor type = AttributeDescriptor.createSubAttribute(
        "type", AttributeDescriptor.DataType.STRING, "A label indicating the " +
        "attribute's function; e.g., \"work\" or " + "\"home\"",
        SCIMConstants.SCHEMA_URI_CORE, false, false, false, "home", "work");

    AttributeDescriptor value = AttributeDescriptor.createSubAttribute(
        "value",  AttributeDescriptor.DataType.BOOLEAN,
        "The attribute's significant value",
        SCIMConstants.SCHEMA_URI_CORE, false, true, false);

    AttributeDescriptor display =
        AttributeDescriptor.createSubAttribute("display",
            AttributeDescriptor.DataType.STRING,
            "A human readable name, primarily used for display purposes",
            SCIMConstants.SCHEMA_URI_CORE, true, false, false);

    AttributeDescriptor primary =
        AttributeDescriptor.createSubAttribute("primary",
        AttributeDescriptor.DataType.BOOLEAN,
        "A Boolean value indicating the 'primary' or preferred attribute " +
            "value for this attribute",
        SCIMConstants.SCHEMA_URI_CORE, false, false, false);

    AttributeDescriptor operation =
        AttributeDescriptor.createSubAttribute("operation",
            AttributeDescriptor.DataType.STRING,
            "The operation to perform on the multi-valued attribute during a " +
                "PATCH request",
            SCIMConstants.SCHEMA_URI_CORE, false, false, false);

    AttributeDescriptor ad =
        AttributeDescriptor.createMultiValuedAttribute("tests", "test",
        "desc", "com.unboundid", false, false, false,
        type, value, display, primary, operation);

    assertEquals(ad.getSubAttributes().size(), 5);
  }

  /**
   * Create a resource descriptor without the common attributes and make sure
   * they are still available.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testAddCommonAttrs() throws Exception
  {
    ResourceDescriptor rd =
        ResourceDescriptor.create("test", "desc", "com.unboundid", "/test",
            AttributeDescriptor.createAttribute("testAttr",
                AttributeDescriptor.DataType.INTEGER, "desc", "com.unboundid",
                false, false, false));

    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.ID_DESCRIPTOR.getName()));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.EXTERNAL_ID_DESCRIPTOR.getName()));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.META_DESCRIPTOR.getName()));

    assertEquals(rd.getAttributes().size(), 4);

  }

  /**
   * Create a resource descriptor without the common attributes and make sure
   * they are still available.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testAddCommonAttrsFromJson() throws Exception
  {
    String schemaText = "{\n" +
        "  \"id\":\"urn:unboundid:scim:testCreateSchema\",\n" +
        "  \"name\":\"Test\",\n" +
        "  \"description\":\"Test Create Schema\",\n" +
        "  \"schema\":\"urn:scim:schemas:test:1.0\",\n" +
        "  \"endpoint\":\"/Tests\",\n" +
        "  \"attributes\":[\n" +
        "    {\n" +
        "      \"name\":\"attr1\",\n" +
        "      \"type\":\"string\",\n" +
        "      \"multiValued\":false,\n" +
        "      \"description\":\"string attribute.\",\n" +
        "      \"schema\":\"urn:scim:schemas:test:1.0\",\n" +
        "      \"readOnly\":false,\n" +
        "      \"required\":false,\n" +
        "      \"caseExact\":false\n" +
        "    }\n" +
        "  ]\n" +
        "}\n";

    ByteArrayInputStream is = new ByteArrayInputStream(schemaText.getBytes());
    JsonUnmarshaller unmarshaller = new JsonUnmarshaller();
    ResourceDescriptor rd = unmarshaller.unmarshal(is,
        CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY);

    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.ID_DESCRIPTOR.getName()));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.EXTERNAL_ID_DESCRIPTOR.getName()));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.META_DESCRIPTOR.getName()));

    assertEquals(rd.getAttributes().size(), 4);

    ByteArrayOutputStream os = new ByteArrayOutputStream();
    JsonMarshaller marshaller = new JsonMarshaller();
    marshaller.marshal(rd, os);

    // Make sure the marshalled schema did not change (added attributes should
    // only be added in memory at runtime).
    String marshalledJson = os.toString();
    JSONObject jo = new JSONObject(new JSONTokener(marshalledJson));
    JSONArray attributes = jo.getJSONArray("attributes");
    for(int i = 0; i < attributes.length(); i++)
    {
      JSONObject attribute = (JSONObject) attributes.get(i);
      assertNotEquals(attribute.getString("name"), "id");
      assertNotEquals(attribute.getString("name"), "externalId");
      assertNotEquals(attribute.getString("name"), "meta");
    }
  }

  /**
   * Create a multi-valued attribute descriptor with the common attributes and
   * make sure duplicates are not created.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testAddCommonMultivaluedSubAttrs() throws Exception
  {
    ResourceDescriptor rd =
        ResourceDescriptor.create("test", "desc", "com.unboundid", "/test",
            // Create the standard meta attribute w/o all the core
            // sub-attributes
            AttributeDescriptor.createAttribute(
                CoreSchema.META_DESCRIPTOR.getName(),
                CoreSchema.META_DESCRIPTOR.getDataType(),
                CoreSchema.META_DESCRIPTOR.getDescription(),
                CoreSchema.META_DESCRIPTOR.getSchema(),
                CoreSchema.META_DESCRIPTOR.isReadOnly(),
                CoreSchema.META_DESCRIPTOR.isRequired(),
                CoreSchema.META_DESCRIPTOR.isCaseExact(),
                CoreSchema.META_DESCRIPTOR.getSubAttribute("created")),
            // Create a multi-valued attribute w/o any sub-attributes
            AttributeDescriptor.createMultiValuedAttribute(
                "testAttrs",
                "testAttr",
                "desc",
                "com.unboundid",
                false,
                false,
                false));

    // Make sure the missing meta sub-attributes are available.
    assertEquals(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.META_DESCRIPTOR.getName()).getSubAttributes().size(), 5);
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            CoreSchema.META_DESCRIPTOR.getName()).getSubAttribute(
        "lastModified"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            CoreSchema.META_DESCRIPTOR.getName()).getSubAttribute(
        "location"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            CoreSchema.META_DESCRIPTOR.getName()).getSubAttribute(
        "version"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            CoreSchema.META_DESCRIPTOR.getName()).getSubAttribute(
        "attributes"));

    assertEquals(rd.getAttribute("com.unboundid",
        "testAttrs").getSubAttributes().size(), 4);
    assertNotNull(rd.getAttribute("com.unboundid",
        "testAttrs").getSubAttribute("type"));
    assertNotNull(rd.getAttribute("com.unboundid",
        "testAttrs").getSubAttribute("primary"));
    assertNotNull(rd.getAttribute("com.unboundid",
        "testAttrs").getSubAttribute("display"));
    assertNotNull(rd.getAttribute("com.unboundid",
        "testAttrs").getSubAttribute("operation"));

    // value sub-attribute is not expected to be added for multi-valued
    // attributes unless that attribute was incorrectly typed as non-complex.
  }

  /**
   * Create a resource descriptor without the common attributes and make sure
   * they are still available.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testAddCommonSubAttrsFromJson() throws Exception
  {
    String schemaText = "{\n" +
        "  \"id\":\"urn:unboundid:scim:testCreateSchema\",\n" +
        "  \"name\":\"Test\",\n" +
        "  \"description\":\"Test Create Schema\",\n" +
        "  \"schema\":\"urn:scim:schemas:test:1.0\",\n" +
        "  \"endpoint\":\"/Tests\",\n" +
        "  \"attributes\":[\n" +
        "    {\n" +
        "      \"name\":\"meta\",\n" +
        "      \"type\":\"complex\",\n" +
        "      \"multiValued\":false,\n" +
        "      \"description\":\"Resource metadata.\",\n" +
        "      \"schema\":\"urn:scim:schemas:core:1.0\",\n" +
        "      \"readOnly\":true,\n" +
        "      \"required\":false,\n" +
        "      \"caseExact\":false,\n" +
        "      \"subAttributes\":[\n" +
        "        {\n" +
        "          \"name\":\"created\",\n" +
        "          \"type\":\"DateTime\",\n" +
        "          \"multiValued\":false,\n" +
        "          \"description\":\"The DateTime the user was added to the " +
        "Service Provider.\",\n" +
        "          \"readOnly\":true,\n" +
        "          \"required\":false,\n" +
        "          \"caseExact\":false\n" +
        "        }" +
        "      ]" +
        "    }," +
        // Technically this is incorrect as all multi-valued attributes should
        // be typed as complex with sub-attributes. However, it is common for
        // people to declare simple multi-valued like this so test to make
        // sure it is treated as having a value sub-attribute declared with the
        // same type as the parent attribute.
        "    {\n" +
        "       \"name\":\"descriptions\",\n" +
        "       \"type\":\"string\",\n" +
        "       \"multiValued\":true,\n" +
        "       \"description\":\"Description of the user.\",\n" +
        "       \"schema\":\"urn:unboundid:qa:custom:1.0\",\n" +
        "       \"readOnly\":false,\n" +
        "       \"required\":false,\n" +
        "       \"caseExact\":false\n" +
        "     }," +
        // It might also be common for people to forget to declare the
        // operations sub-attribute. This is crucial for PATCH operations
        // so make sure it is dynicmally added.
        "     {\n" +
        "       \"name\":\"entitlements\",\n" +
        "       \"type\":\"complex\",\n" +
        "       \"multiValued\":true,\n" +
        "       \"multiValuedAttributeChildName\":\"entitlement\",\n" +
        "       \"description\":\"Entitlements that the user has.\",\n" +
        "       \"schema\":\"urn:scim:schemas:core:1.0\",\n" +
        "       \"readOnly\":false,\n" +
        "       \"required\":false,\n" +
        "       \"caseExact\":false,\n" +
        "       \"subAttributes\":[\n" +
        "         {\n" +
        "           \"name\":\"value\",\n" +
        "           \"type\":\"string\",\n" +
        "           \"multiValued\":false,\n" +
        "           \"description\":\"The attribute's significant value.\",\n" +
        "           \"readOnly\":false,\n" +
        "           \"required\":false,\n" +
        "           \"caseExact\":false\n" +
        "         }\n" +
        "       ]\n" +
        "     }" +
        "  ]\n" +
        "}\n";

    ByteArrayInputStream is = new ByteArrayInputStream(schemaText.getBytes());
    JsonUnmarshaller unmarshaller = new JsonUnmarshaller();
    ResourceDescriptor rd = unmarshaller.unmarshal(is,
        CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR,
        ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY);

    // Make sure the missing meta sub-attributes are available.
    assertEquals(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        CoreSchema.META_DESCRIPTOR.getName()).getSubAttributes().size(), 5);
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            CoreSchema.META_DESCRIPTOR.getName()).getSubAttribute(
        "lastModified"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            CoreSchema.META_DESCRIPTOR.getName()).getSubAttribute(
        "location"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            CoreSchema.META_DESCRIPTOR.getName()).getSubAttribute(
        "version"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            CoreSchema.META_DESCRIPTOR.getName()).getSubAttribute(
        "attributes"));

    assertEquals(rd.getAttribute("urn:unboundid:qa:custom:1.0",
        "descriptions").getSubAttributes().size(), 5);
    assertNotNull(rd.getAttribute("urn:unboundid:qa:custom:1.0",
        "descriptions").getSubAttribute("type"));
    assertNotNull(rd.getAttribute("urn:unboundid:qa:custom:1.0",
        "descriptions").getSubAttribute("primary"));
    assertNotNull(rd.getAttribute("urn:unboundid:qa:custom:1.0",
        "descriptions").getSubAttribute("display"));
    assertNotNull(rd.getAttribute("urn:unboundid:qa:custom:1.0",
        "descriptions").getSubAttribute("operation"));
    assertNotNull(rd.getAttribute("urn:unboundid:qa:custom:1.0",
        "descriptions").getSubAttribute("value"));

    assertEquals(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        "entitlements").getSubAttributes().size(), 5);
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        "entitlements").getSubAttribute("type"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        "entitlements").getSubAttribute("primary"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        "entitlements").getSubAttribute("display"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        "entitlements").getSubAttribute("operation"));
    assertNotNull(rd.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        "entitlements").getSubAttribute("value"));

  }
}
