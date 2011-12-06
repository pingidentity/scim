/*
 * Copyright 2011 UnboundID Corp.
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

package com.unboundid.scim.sdk;

import com.unboundid.scim.SCIMTestCase;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.CoreSchema;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertNotNull;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;



/**
 * This class provides test coverage for the {@code SCIMAttribute} class.
 */
public class SCIMObjectTestCase
    extends SCIMTestCase
{
  /**
   * Test that valid examples can be created and manipulated.
   * @throws Exception if an error occurs.
   */
  @Test
  public void testExampleAttributes() throws Exception
  {
    final UUID uuid = UUID.randomUUID();

    final String coreSchema =
        SCIMConstants.SCHEMA_URI_CORE;
    final String enterpriseUserSchema =
        SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION;
    final String customSchema =
        "http://myextension";

    final SCIMAttribute userID =
        SCIMAttribute.create(
            CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "id"),
            SCIMAttributeValue.createStringValue(uuid.toString()));

    final SCIMAttribute name = SCIMAttribute.create(
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "name"),
        Name.NAME_RESOLVER.fromInstance(
            CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "name"),
            new Name("Ms. Barbara J Jensen III",
                "Jensen", "Barbara", null, null, null)));

    final AttributeDescriptor emailsDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "emails");
    final SCIMAttribute emails =
        SCIMAttribute.create(emailsDescriptor,
            Entry.STRINGS_RESOLVER.fromInstance(emailsDescriptor,
                new Entry<String>("bjensen@example.com", "work", true)),
            Entry.STRINGS_RESOLVER.fromInstance(emailsDescriptor,
                new Entry<String>("babs@jensen.org", "home", false)));

    final AttributeDescriptor metaDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "meta");
    final Date date = new Date(System.currentTimeMillis());
    final SCIMAttribute meta =
        SCIMAttribute.create(metaDescriptor,
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.create(
                    metaDescriptor.getSubAttribute("created"),
                    SCIMAttributeValue.createDateValue(date)),
                SCIMAttribute.create(
                    metaDescriptor.getSubAttribute("lastModified"),
                    SCIMAttributeValue.createDateValue(date))));

    final SCIMAttribute employeeNumber =
        SCIMAttribute.create(
            CoreSchema.USER_DESCRIPTOR.getAttribute(enterpriseUserSchema,
                "employeeNumber"),
            SCIMAttributeValue.createStringValue("1000001"));

    final SCIMObject user = new SCIMObject();
    assertTrue(user.addAttribute(userID));
    assertTrue(user.addAttribute(meta));
    assertTrue(user.addAttribute(name));
    assertTrue(user.addAttribute(emails));
    user.setAttribute(employeeNumber);

    final Collection<String> schemas = user.getSchemas();
    assertNotNull(schemas);
    assertEquals(schemas.size(), 2);
    assertTrue(schemas.contains(coreSchema));
    assertTrue(schemas.contains(enterpriseUserSchema));

    assertTrue(user.hasSchema(coreSchema));
    assertTrue(user.hasSchema(enterpriseUserSchema));

    assertEquals(user.getAttribute(coreSchema, "id"), userID);
    assertEquals(user.getAttribute(coreSchema, "meta"), meta);

    assertEquals(user.getAttribute(coreSchema, "name"), name);
    assertEquals(user.getAttribute(coreSchema, "emails"), emails);
    assertEquals(user.getAttribute(enterpriseUserSchema, "employeeNumber"),
                 employeeNumber);
    assertNull(user.getAttribute(coreSchema, "employeeNumber"));
    assertNull(user.getAttribute(enterpriseUserSchema, "name"));
    assertNull(user.getAttribute(customSchema, "name"));

    final Collection<SCIMAttribute> coreAttrs = user.getAttributes(coreSchema);
    assertNotNull(coreAttrs);
    assertEquals(coreAttrs.size(), 4);
    assertTrue(coreAttrs.contains(userID));
    assertTrue(coreAttrs.contains(meta));
    assertTrue(coreAttrs.contains(name));
    assertTrue(coreAttrs.contains(emails));

    final Collection<SCIMAttribute> enterpriseUserAttrs =
        user.getAttributes(enterpriseUserSchema);
    assertNotNull(enterpriseUserAttrs);
    assertEquals(enterpriseUserAttrs.size(), 1);
    assertTrue(enterpriseUserAttrs.contains(employeeNumber));

    assertTrue(user.getAttributes(customSchema).isEmpty());

    assertTrue(user.hasAttribute(coreSchema, "id"));
    assertTrue(user.hasAttribute(coreSchema, "meta"));
    assertTrue(user.hasAttribute(coreSchema, "name"));
    assertTrue(user.hasAttribute(coreSchema, "emails"));
    assertTrue(user.hasAttribute(enterpriseUserSchema, "employeeNumber"));
    assertFalse(user.hasAttribute(coreSchema, "employeeNumber"));
    assertFalse(user.hasAttribute(enterpriseUserSchema, "name"));
    assertFalse(user.hasAttribute(customSchema, "name"));

    assertFalse(user.addAttribute(name));
    user.setAttribute(name);
    assertFalse(user.addAttribute(meta));
    user.setAttribute(meta);

    assertTrue(user.removeAttribute(coreSchema, "id"));
    assertFalse(user.removeAttribute(coreSchema, "id"));
    assertTrue(user.removeAttribute(coreSchema, "meta"));
    assertFalse(user.removeAttribute(coreSchema, "meta"));
    assertTrue(user.removeAttribute(coreSchema, "name"));
    assertFalse(user.removeAttribute(coreSchema, "name"));
    assertTrue(user.removeAttribute(coreSchema, "emails"));
    assertFalse(user.removeAttribute(coreSchema, "emails"));
    assertFalse(user.hasSchema(coreSchema));

    assertTrue(user.removeAttribute(enterpriseUserSchema, "employeeNumber"));
    assertFalse(user.hasSchema(enterpriseUserSchema));

    assertTrue(user.getSchemas().isEmpty());
  }
}
