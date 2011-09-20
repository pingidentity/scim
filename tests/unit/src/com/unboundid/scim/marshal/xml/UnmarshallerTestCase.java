/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMRITestCase;
import static com.unboundid.scim.sdk.SCIMConstants.*;

import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;



/**
 * This class provides test coverage for the {@link XmlUnmarshaller}.
 */
@Test
public class UnmarshallerTestCase
  extends SCIMRITestCase {

  /**
   * Verify that a known valid user can be read from XML.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = true)
  public void testUnmarshal()
    throws Exception {
    final File testXML = getTestResource("marshal/core-user.xml");

    final Context context = Context.instance();
    final Unmarshaller unmarshaller = context.unmarshaller();
    final SCIMObject o = unmarshaller.unmarshal(testXML, RESOURCE_NAME_USER);

    for (final String attribute : Arrays.asList("id",
                                                "addresses",
                                                "phoneNumbers",
                                                "emails",
                                                "name"))
    {
      assertTrue(o.hasAttribute(SCHEMA_URI_CORE, attribute));
    }

    for (final String attribute : Arrays.asList("employeeNumber",
                                                "organization",
                                                "division",
                                                "department",
                                                "manager"))
    {
      assertTrue(o.hasAttribute(SCHEMA_URI_ENTERPRISE_EXTENSION, attribute));
    }

    assertNotNull(o.getResourceName());
    assertEquals(o.getResourceName(), RESOURCE_NAME_USER);
  }
}
