/*
 * Copyright 2008-2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.xml;

import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.XmlSchemaParser;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;

/**
 * Verifies xsd based resource descriptor parsing.
 */
@Test
public class XmlSchemaParserTest extends SCIMRITestCase {

  /**
   * Tests parsing the SCIM core schema.
   *
   * @throws Exception if error parsing schema file
   */
  @Test
  public void testParseCore() throws Exception {
    final File schemaFile = getPackageResource("scim-core.xsd");
    final File[] coreSchemas = new File[]{schemaFile};
    XmlSchemaParser parser = new
      XmlSchemaParser(coreSchemas);
    Collection<ResourceDescriptor> descriptors = parser.getDescriptors();
    assertTrue(descriptors.size() == 2);
  }
}
