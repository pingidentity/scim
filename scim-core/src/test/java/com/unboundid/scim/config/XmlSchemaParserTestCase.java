/*
 * Copyright 2008-2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.config;

import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.SCIMTestCase;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;



/**
 * Verifies xsd based resource descriptor parsing.
 */
@Test
public class XmlSchemaParserTestCase extends SCIMTestCase
{

  /**
   * Tests parsing the SCIM core schema.
   *
   * @throws Exception if error parsing schema file
   */
  @Test
  public void testParseCore() throws Exception
  {
    final InputStream schemaFile =
        getResource(SCIMConstants.SCHEMA_FILE_URI_CORE);
    final InputStream[] coreSchemas = new InputStream[]{schemaFile};
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
