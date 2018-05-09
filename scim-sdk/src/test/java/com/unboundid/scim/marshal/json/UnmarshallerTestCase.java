/*
 * Copyright 2011-2018 Ping Identity Corporation
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

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.SCIMTestCase;
import org.testng.annotations.Test;

import static com.unboundid.scim.sdk.SCIMConstants.SCHEMA_URI_CORE;
import static org.testng.Assert.*;

import java.io.InputStream;
import java.util.Iterator;


@Test
public class UnmarshallerTestCase extends SCIMTestCase {
  /**
   * Verify that a known valid user can be read from JSON.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testUnmarshal() throws Exception {
    final ResourceDescriptor userResourceDescriptor =
        CoreSchema.USER_DESCRIPTOR;
    InputStream testJson =
        getResource("/com/unboundid/scim/marshal/spec/core-user.json");
    final Unmarshaller unmarshaller = new JsonUnmarshaller();
    final SCIMObject o = unmarshaller.unmarshal(
        testJson, userResourceDescriptor,
        BaseResource.BASE_RESOURCE_FACTORY).getScimObject();
    assertNotNull(o);
    SCIMAttribute roles =
        o.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "roles");
    assertNotNull(roles);
    assertEquals(roles.getValues().length, 3);
    assertEquals(
        roles.getValues()[0].getAttribute("value").getValue().getStringValue(),
        "Employee");
    assertEquals(
        roles.getValues()[1].getAttribute("value").getValue().getStringValue(),
        "Accounting");
    assertEquals(
        roles.getValues()[2].getAttribute("value").getValue().getStringValue(),
        "Web");
    SCIMAttribute groups =
        o.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "groups");
    assertNotNull(groups);
    assertEquals(groups.getValues().length, 3);
    assertEquals(
        groups.getValues()[0].getAttribute("value").getValue().getStringValue(),
        "00300000005N2Y6AA");
    assertEquals(
     groups.getValues()[0].getAttribute("primary").getValue().getBooleanValue(),
        Boolean.TRUE);
    assertEquals(
        groups.getValues()[0].getAttribute("type").getValue().getStringValue(),
        "Tour Guides");
    assertEquals(
        groups.getValues()[1].getAttribute("value").getValue().getStringValue(),
        "00300000005N34H78");
    assertEquals(
        groups.getValues()[1].getAttribute("type").getValue().getStringValue(),
        "Employees");
    assertEquals(
        groups.getValues()[2].getAttribute("value").getValue().getStringValue(),
        "00300000005N98YT1");
    assertEquals(
        groups.getValues()[2].getAttribute("type").getValue().getStringValue(),
        "US Employees");

    SCIMAttribute x509Certificates =
        o.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "x509Certificates");
    assertNotNull(x509Certificates);
    assertEquals(x509Certificates.getValues().length, 1);
    final SCIMAttributeValue binaryAttributeValue =
        x509Certificates.getValues()[0].getAttribute("value").getValue();
    assertEquals(
        binaryAttributeValue.getStringValue(),
        "MIIDQzCCAqygAwIBAgICEAAwDQYJKoZIhvcNAQEFBQAwTjELMAkGA1UEBhMCVVMx" +
        "EzARBgNVBAgMCkNhbGlmb3JuaWExFDASBgNVBAoMC2V4YW1wbGUuY29tMRQwEgYD" +
        "VQQDDAtleGFtcGxlLmNvbTAeFw0xMTEwMjIwNjI0MzFaFw0xMjEwMDQwNjI0MzFa" +
        "MH8xCzAJBgNVBAYTAlVTMRMwEQYDVQQIDApDYWxpZm9ybmlhMRQwEgYDVQQKDAtl" +
        "eGFtcGxlLmNvbTEhMB8GA1UEAwwYTXMuIEJhcmJhcmEgSiBKZW5zZW4gSUlJMSIw" +
        "IAYJKoZIhvcNAQkBFhNiamVuc2VuQGV4YW1wbGUuY29tMIIBIjANBgkqhkiG9w0B" +
        "AQEFAAOCAQ8AMIIBCgKCAQEA7Kr+Dcds/JQ5GwejJFcBIP682X3xpjis56AK02bc" +
        "1FLgzdLI8auoR+cC9/Vrh5t66HkQIOdA4unHh0AaZ4xL5PhVbXIPMB5vAPKpzz5i" +
        "PSi8xO8SL7I7SDhcBVJhqVqr3HgllEG6UClDdHO7nkLuwXq8HcISKkbT5WFTVfFZ" +
        "zidPl8HZ7DhXkZIRtJwBweq4bvm3hM1Os7UQH05ZS6cVDgweKNwdLLrT51ikSQG3" +
        "DYrl+ft781UQRIqxgwqCfXEuDiinPh0kkvIi5jivVu1Z9QiwlYEdRbLJ4zJQBmDr" +
        "SGTMYn4lRc2HgHO4DqB/bnMVorHB0CC6AV1QoFK4GPe1LwIDAQABo3sweTAJBgNV" +
        "HRMEAjAAMCwGCWCGSAGG+EIBDQQfFh1PcGVuU1NMIEdlbmVyYXRlZCBDZXJ0aWZp" +
        "Y2F0ZTAdBgNVHQ4EFgQU8pD0U0vsZIsaA16lL8En8bx0F/gwHwYDVR0jBBgwFoAU" +
        "dGeKitcaF7gnzsNwDx708kqaVt0wDQYJKoZIhvcNAQEFBQADgYEAA81SsFnOdYJt" +
        "Ng5Tcq+/ByEDrBgnusx0jloUhByPMEVkoMZ3J7j1ZgI8rAbOkNngX8+pKfTiDz1R" +
        "C4+dx8oU6Za+4NJXUjlL5CvV6BEYb1+QAEJwitTVvxB/A67g42/vzgAtoRUeDov1" +
        "+GFiBZ+GNF/cAYKcMtGcrs2i97ZkJMo=");
    binaryAttributeValue.getBinaryValue();  // Should not throw.

    assertFalse(o.hasAttribute(SCHEMA_URI_CORE, "title"));
    assertFalse(o.hasAttribute(SCHEMA_URI_CORE, "ims"));
    SCIMAttribute phoneNumbers =
            o.getAttribute(SCHEMA_URI_CORE, "phoneNumbers");
    assertNotNull(phoneNumbers);
    assertEquals(phoneNumbers.getValues().length, 2);

    UserResource user = new UserResource(CoreSchema.USER_DESCRIPTOR, o);
    assertNotNull(user.getName());
    assertNotNull(user.getName().getFamilyName());
    assertNull(user.getName().getMiddleName());
  }


  /**
   * Verify that JSON with missing or malformed schema is handled appropriately.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testUnmarshalBadSchema() throws Exception {
    final ResourceDescriptor userResourceDescriptor =
        CoreSchema.USER_DESCRIPTOR;
    final Unmarshaller unmarshaller = new JsonUnmarshaller();
    InputStream testJson;
    // Test unmarshalling a JSON user entry
    testJson = getResource("/com/unboundid/scim/marshal/spec/mixed-user.json");
    try
    {
      unmarshaller.unmarshal(testJson, userResourceDescriptor,
              BaseResource.BASE_RESOURCE_FACTORY);
      fail("Expected JSONUnmarshaller to detect an ambiguous " +
              "resource representation.");
    }
    catch(InvalidResourceException e)
    {
      //expected
      System.err.println(e.getMessage());
    }
    testJson =
      getResource("/com/unboundid/scim/marshal/spec/mixed-user-malformed.json");
    try
    {
      unmarshaller.unmarshal(testJson, userResourceDescriptor,
              BaseResource.BASE_RESOURCE_FACTORY);
      fail("Expected JSONUnmarshaller to detect an ambiguous " +
              "resource representation.");
    }
    catch(InvalidResourceException e)
    {
      //expected
      System.err.println(e.getMessage());
    }

    // Try with implicit schema checking enabled
    testJson = getResource("/com/unboundid/scim/marshal/spec/mixed-user.json");
    try
    {
      System.setProperty(SCIMConstants.IMPLICIT_SCHEMA_CHECKING_PROPERTY,
                         "true");
      final SCIMObject o = unmarshaller.unmarshal(
                    testJson, userResourceDescriptor,
                    BaseResource.BASE_RESOURCE_FACTORY).getScimObject();
            assertNotNull(o);
            SCIMAttribute username =
                    o.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName");
            assertEquals(username.getValue().getStringValue(), "babs");
            SCIMAttribute employeeNumber =
                    o.getAttribute(
                            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION,
                            "employeeNumber");
            assertEquals(employeeNumber.getValue().getStringValue(), "1");
    }
    finally
    {
      System.setProperty(SCIMConstants.IMPLICIT_SCHEMA_CHECKING_PROPERTY, "");
    }
    testJson =
      getResource("/com/unboundid/scim/marshal/spec/mixed-user-malformed.json");
    boolean saveStrictMode = userResourceDescriptor.isStrictMode();
    try
    {
      System.setProperty(SCIMConstants.IMPLICIT_SCHEMA_CHECKING_PROPERTY,
                         "true");
      userResourceDescriptor.setStrictMode(true);
      unmarshaller.unmarshal(testJson, userResourceDescriptor,
              BaseResource.BASE_RESOURCE_FACTORY);
      fail("Expected JSONUnmarshaller to fail because no schema can be found " +
                   "for attribute.");
    }
    catch(InvalidResourceException e)
    {
      //expected
      System.err.println(e.getMessage());
    }
    finally
    {
      System.setProperty(SCIMConstants.IMPLICIT_SCHEMA_CHECKING_PROPERTY, "");
      userResourceDescriptor.setStrictMode(saveStrictMode);
    }

    // Test unmarshalling a JSON user patch with meta data
    testJson =
          getResource("/com/unboundid/scim/marshal/spec/meta-user-patch.json");
    try
    {
      unmarshaller.unmarshal(testJson, userResourceDescriptor,
              BaseResource.BASE_RESOURCE_FACTORY);
      fail("Expected JSONUnmarshaller to detect an ambiguous " +
              "resource representation.");
    }
    catch(InvalidResourceException e)
    {
      //expected
      System.err.println(e.getMessage());
    }
    // Try with implicit schema checking enabled
    testJson =
          getResource("/com/unboundid/scim/marshal/spec/meta-user-patch.json");
    try
    {
      System.setProperty(SCIMConstants.IMPLICIT_SCHEMA_CHECKING_PROPERTY,
                         "true");
      final SCIMObject o = unmarshaller.unmarshal(
                    testJson, userResourceDescriptor,
                    BaseResource.BASE_RESOURCE_FACTORY).getScimObject();
      assertNotNull(o);
      SCIMAttribute metaAttr =
              o.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta");
      String metaString = metaAttr.toString();
      assertTrue(metaString.contains("department"));
      assertTrue(metaString.contains(
              SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE +
                      "department"));
    }
    finally
    {
      System.setProperty(SCIMConstants.IMPLICIT_SCHEMA_CHECKING_PROPERTY, "");
    }
  }


  /**
   * Verify that an empty list response can be unmarshalled to a Resources
   * object.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testEmptyResources() throws Exception
  {
    Debug.setEnabled(true);
    final ResourceDescriptor userResourceDescriptor =
        CoreSchema.USER_DESCRIPTOR;
    InputStream testJson =
        getResource("/com/unboundid/scim/marshal/empty-list-response.json");
    final Unmarshaller unmarshaller = new JsonUnmarshaller();
    final Resources<BaseResource> resources =
        unmarshaller.unmarshalResources(
            testJson, userResourceDescriptor,
            BaseResource.BASE_RESOURCE_FACTORY);
    assertNotNull(resources);
    assertEquals(resources.getItemsPerPage(), 0);
    assertEquals(resources.getTotalResults(), 0);
    assertEquals(resources.getStartIndex(), 1);
    final Iterator<BaseResource> iterator = resources.iterator();
    assertFalse(iterator.hasNext());
  }


  /**
   * Verify that a SCIM schemas list response can be unmarshalled to a
   * Resources<ResourceDescriptor> object.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testUnmarshalSchemaResources() throws Exception
  {
    Debug.setEnabled(true);
    final ResourceDescriptor schemaResourceDescriptor =
        CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR;
    // This is the output of requesting 'Schemas?filter=name eq "User"' from
    // a Ping Identity Directory Server.
    InputStream testJson =
        getResource("/com/unboundid/scim/marshal/" +
            "user-schema-resource-list.json");
    final Unmarshaller unmarshaller = new JsonUnmarshaller();
    final Resources<ResourceDescriptor> resources =
        unmarshaller.unmarshalResources(
            testJson, schemaResourceDescriptor,
            ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY);
    assertNotNull(resources);
    assertEquals(resources.getTotalResults(), 1);
    for(final ResourceDescriptor schema : resources)
    {
      assertNotNull(schema);
      assertEquals(schema.getName(), "User");
      assertEquals(schema.getDescription(),
          "SCIM core resource for representing users");
      assertEquals(schema.getEndpoint(), "Users");
    }
  }


  /**
   * Verify that a SCIM schema response that is not formatted as a list
   * response can be unmarshalled to a Resources<ResourceDescriptor> object.
   * This accommodates SCIM server implementations that respond to a /Schemas
   * request with a single schema resource.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testUnmarshalResourceToResources() throws Exception
  {
    Debug.setEnabled(true);
    final ResourceDescriptor schemaResourceDescriptor =
        CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR;
    // This is the output of requesting 'Schemas?filter=name eq "User"' from
    // PingFederate.
    InputStream testJson =
        getResource("/com/unboundid/scim/marshal/user-schema-resource.json");
    final Unmarshaller unmarshaller = new JsonUnmarshaller();
    final Resources<ResourceDescriptor> resources =
        unmarshaller.unmarshalResources(
            testJson, schemaResourceDescriptor,
            ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY);
    assertNotNull(resources);
    assertEquals(resources.getTotalResults(), 1);
    for(final ResourceDescriptor schema : resources)
    {
      assertNotNull(schema);
      assertEquals(schema.getName(), "User");
      assertEquals(schema.getDescription(), "Core User");
      assertEquals(schema.getEndpoint(), "/Users");
    }
  }

  /**
   * Verify that a SCIM schema response that is not formatted as a list
   * response will be unmarshalled to a Resources object, even if it can
   * neither be parsed as a list response nor as an expected SCIM resource type.
   * This is similar to {@link #testUnmarshalResourceToResources()} but
   * confirms that the failure case is handled gracefully.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testUnmarshalMalformedResourceToResources() throws Exception
  {
    Debug.setEnabled(true);
    final ResourceDescriptor schemaResourceDescriptor =
        CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR;
    // This is a User resource, but the unmarshaller will expect a Schema
    // resource, so parsing will fail and it will fall back to returning an
    // empty Resources object.
    InputStream testJson =
        getResource("/com/unboundid/scim/marshal/spec/core-user.json");
    final Unmarshaller unmarshaller = new JsonUnmarshaller();
    final Resources<ResourceDescriptor> resources =
        unmarshaller.unmarshalResources(
            testJson, schemaResourceDescriptor,
            ResourceDescriptor.RESOURCE_DESCRIPTOR_FACTORY);
    assertNotNull(resources);
    assertEquals(resources.getItemsPerPage(), 0);
    assertEquals(resources.getTotalResults(), 0);
    assertEquals(resources.getStartIndex(), 1);
    final Iterator<ResourceDescriptor> iterator = resources.iterator();
    assertFalse(iterator.hasNext());
  }

}
