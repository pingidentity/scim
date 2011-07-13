/*
 * Copyright 2008-2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.xml;

import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.Schema;
import com.unboundid.scim.config.XmlSchemaParser;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;



/**
 * Verifies xsd based resource descriptor parsing.
 */
@Test
public class XmlSchemaParserTestCase extends SCIMRITestCase
{

  /**
   * Tests parsing the SCIM core schema.
   *
   * @throws Exception if error parsing schema file
   */
  @Test
  public void testParseCore() throws Exception
  {
    final File schemaFile = getPackageResource("schema/scim-core.xsd");
    final File[] coreSchemas = new File[]{schemaFile};
    final XmlSchemaParser parser = new XmlSchemaParser(coreSchemas);
    final Collection<Schema> schemas = parser.getSchemas();

    assertEquals(schemas.size(), 1);
    final Map<String, Schema> schemaMap = new HashMap<String, Schema>();
    for (final Schema schema : schemas)
    {
      schemaMap.put(schema.getSchemaURI(), schema);
    }

    final Schema coreSchema = schemaMap.get(SCIMConstants.SCHEMA_URI_CORE);
    assertNotNull(coreSchema);

    assertTrue(coreSchema.getResourceDescriptors().size() > 1);

    final Map<String, ResourceDescriptor> resourceDescriptorMap =
        new HashMap<String, ResourceDescriptor>();
    for (final ResourceDescriptor descriptor :
        coreSchema.getResourceDescriptors())
    {
      resourceDescriptorMap.put(descriptor.getName(), descriptor);
    }

    assertNotNull(resourceDescriptorMap.containsKey("User"));
    assertNotNull(resourceDescriptorMap.containsKey("Group"));
  }
}
