/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class provides test coverage for the {@link XmlMarshaller}.
 */
@Test
public class MarshallerTestCase
  extends SCIMRITestCase
{
  /**
   * Verify that a valid user can be written to XML and then read back.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = true)
  public void testMarshal()
    throws Exception
  {
    final Context context = Context.instance();
    final File testXML = File.createTempFile("test-", ".xml");
    testXML.deleteOnExit();

    final SCIMObject user1 = new SCIMObject(RESOURCE_NAME_USER);

    user1.addAttribute(
        SCIMAttribute.createSingularStringAttribute(
            SCHEMA_URI_CORE, "id", "uid=bjensen,dc=example,dc=com"));

    user1.addAttribute(
        SCIMAttribute.createSingularStringAttribute(
            SCHEMA_URI_CORE, "userName", "bjensen"));

    user1.addAttribute(generateName("Ms. Barbara J Jensen III",
                                    "Jensen", "Barbara", "J", "Ms.", "III"));
    final SCIMAttribute emails =
        SCIMAttribute.createPluralAttribute(
            SCHEMA_URI_CORE,"emails",
            SCIMAttributeValue.createPluralStringValue(
                SCHEMA_URI_CORE, "bjensen@example.com", "work", true),
            SCIMAttributeValue.createPluralStringValue(
                SCHEMA_URI_CORE, "babs@jensen.org", "home", false));
    user1.addAttribute(emails);

    user1.addAttribute(
        SCIMAttribute.createPluralAttribute(
            SCHEMA_URI_CORE, "addresses",
            generateAddress("work",
                            "100 Universal City Plaza\nHollywood, CA 91608 USA",
                            "100 Universal City Plaza",
                            "Hollywood",
                            "CA",
                            "91608",
                            "USA",
                            true),
            generateAddress("home",
                            "456 Hollywood Blvd\nHollywood, CA 91608 USA",
                            "456 Hollywood Blvd",
                            "Hollywood",
                            "CA",
                            "91608",
                            "USA",
                            true)));

    user1.addAttribute(
        SCIMAttribute.createPluralAttribute(
            SCHEMA_URI_CORE, "phoneNumbers",
            SCIMAttributeValue.createPluralStringValue(
                SCHEMA_URI_CORE, "800-864-8377", "work", false),
            SCIMAttributeValue.createPluralStringValue(
                SCHEMA_URI_CORE, "818-123-4567", "mobile", false)));

    user1.addAttribute(
        SCIMAttribute.createSingularStringAttribute(
            SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber", "1001"));
    user1.addAttribute(
        SCIMAttribute.createSingularStringAttribute(
            SCHEMA_URI_ENTERPRISE_EXTENSION, "organization", "Example"));
    user1.addAttribute(
        SCIMAttribute.createSingularStringAttribute(
            SCHEMA_URI_ENTERPRISE_EXTENSION, "division", "People"));
    user1.addAttribute(
        SCIMAttribute.createSingularStringAttribute(
            SCHEMA_URI_ENTERPRISE_EXTENSION, "department", "Sales"));

    final Marshaller marshaller = context.marshaller();
    marshaller.marshal(user1, testXML);

    final Unmarshaller unmarshaller = context.unmarshaller();
    final SCIMObject user2 =
        unmarshaller.unmarshal(testXML, RESOURCE_NAME_USER);

    assertEquals(user2.getResourceName(), RESOURCE_NAME_USER);
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
                                                "department"))
    {
      assertTrue(user2.hasAttribute(SCHEMA_URI_ENTERPRISE_EXTENSION,
                                    attribute));
    }
  }
}
