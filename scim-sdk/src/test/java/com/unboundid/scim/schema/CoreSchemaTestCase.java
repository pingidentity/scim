/*
 * Copyright 2011-2013 UnboundID Corp.
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
import com.unboundid.scim.sdk.SCIMConstants;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

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
        AttributeDescriptor.DataType.BOOLEAN, "desc", "com.unboundid", false,
        false, false, new String[]{"home", "work"},
        type, value, display, primary, operation);

    assertEquals(ad.getSubAttributes().size(), 5);
  }
}
