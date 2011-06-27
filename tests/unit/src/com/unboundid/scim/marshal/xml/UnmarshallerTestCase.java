/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.config.ResourceDescriptorManager;
import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMRITestCase;
import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_NAME_USER;
import static com.unboundid.scim.sdk.SCIMConstants.SCHEMA_URI_CORE;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;


/**
 * This class provides test coverage for the {@link XmlUnmarshaller}.
 */
@Test
public class UnmarshallerTestCase
  extends SCIMRITestCase {

  /**
   * Bootstrap the unmarshaller with a configured resource descriptor.
   * @throws Exception if error initializing descriptor manager.
   */
  @BeforeMethod
  public void setUp() throws Exception {
    final File schemaFile = getPackageResource("scim-core.xsd");
    ResourceDescriptorManager.init(new File[]{schemaFile});
  }

  /**
   * Verify that a known valid user can be read from XML.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = true)
  public void testUnmarshal()
    throws Exception {
    final File testXML = getTestResource("marshal/core-user.xml");

    final String coreSchema = SCHEMA_URI_CORE;
    final Context context = Context.instance();
    final Unmarshaller unmarshaller = context.unmarshaller();
    final SCIMObject o = unmarshaller.unmarshal(testXML);

    assertTrue(o.hasAttribute(coreSchema, "id"));
    assertTrue(o.hasAttribute(coreSchema, "addresses"));
    assertTrue(o.hasAttribute(coreSchema, "phoneNumbers"));
    assertTrue(o.hasAttribute(coreSchema, "emails"));
    assertTrue(o.hasAttribute(coreSchema, "name"));
    assertNotNull(o.getResourceName());
    assertEquals(o.getResourceName(), RESOURCE_NAME_USER);
  }
}
