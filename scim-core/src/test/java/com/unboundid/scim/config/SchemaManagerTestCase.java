/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.config;

import com.unboundid.scim.SCIMTestCase;
import com.unboundid.scim.sdk.SCIMConstants;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;



/**
 * Provides test coverage for the Schema Manager.
 */
public class SchemaManagerTestCase extends SCIMTestCase
{
  /**
   * Tests the ability of the Schema Manager to parse the schema properly.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testParse() throws Exception
  {
    // The known schema has already been parsed in the test set-up,
    // so we just need to validate the resulting contents of the schema manager.
    final SchemaManager manager =
        SchemaManager.instance();
    final Schema coreSchema =
        manager.getSchema(SCIMConstants.SCHEMA_URI_CORE);
    final Schema enterpriseSchema =
        manager.getSchema(SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION);

    assertNotNull(coreSchema);
    assertNotNull(enterpriseSchema);
    assertNotNull(manager.getResourceDescriptor("User") != null);
    assertNotNull(manager.getResourceDescriptor("Group") != null);

    assertTrue(coreSchema.getAttributeDescriptors().size() >= 21);
    assertTrue(enterpriseSchema.getAttributeDescriptors().size() >= 6);
  }
}
