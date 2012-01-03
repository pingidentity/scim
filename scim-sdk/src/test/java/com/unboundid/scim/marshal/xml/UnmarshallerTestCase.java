/*
 * Copyright 2011-2012 UnboundID Corp.
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

package com.unboundid.scim.marshal.xml;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.SCIMTestCase;
import static com.unboundid.scim.sdk.SCIMConstants.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import java.io.InputStream;
import java.util.Arrays;



/**
 * This class provides test coverage for the {@link XmlUnmarshaller}.
 */
@Test
public class UnmarshallerTestCase
  extends SCIMTestCase {

  /**
   * Verify that a known valid user can be read from XML.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = true)
  public void testUnmarshal()
    throws Exception {
    final InputStream testXML =
        getResource("/com/unboundid/scim/marshal/core-user.xml");

    final ResourceDescriptor userResourceDescriptor =
        CoreSchema.USER_DESCRIPTOR;
    final Unmarshaller unmarshaller = new XmlUnmarshaller();
    final BaseResource resource = unmarshaller.unmarshal(testXML,
        userResourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY);
    final SCIMObject o = resource.getScimObject();

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

    assertEquals(resource.getResourceDescriptor(), userResourceDescriptor);
  }
}
