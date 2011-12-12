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
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.CoreSchema;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;



/**
 * This class provides test coverage for the {@code SCIMAttribute} class.
 */
public class SCIMAttributeTestCase
  extends SCIMTestCase
{
  /**
   * Test that valid examples can be created and inspected.
   * @throws Exception if an error occurs.
   */
  @Test
  public void testExampleAttributes() throws Exception
  {
    final String coreSchema =
        SCIMConstants.SCHEMA_URI_CORE;
    final String customSchema =
        "http://myextension";

    final UUID uuid = UUID.randomUUID();

    final SCIMAttribute userID =
        SCIMAttribute.create(
            CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "id"),
            SCIMAttributeValue.createStringValue(uuid.toString()));
    assertEquals(userID.getSchema(), coreSchema);
    assertEquals(userID.getName(), "id");
    assertFalse(userID.getValues().length > 1);
    assertNotNull(userID.getValue());
    assertFalse(userID.getValue().isComplex());
    assertNull(userID.getValue().getAttribute("wrong"));
    assertEquals(userID.getValue().getStringValue(), uuid.toString());

    AttributeDescriptor nameDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "name");
    final SCIMAttribute name =
        SCIMAttribute.create(nameDescriptor,
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.create(
                    nameDescriptor.getSubAttribute("formatted"),
                    SCIMAttributeValue.createStringValue(
                        "Ms. Barbara J Jensen III")),
                SCIMAttribute.create(
                    nameDescriptor.getSubAttribute("familyName"),
                    SCIMAttributeValue.createStringValue("Jensen")),
                SCIMAttribute.create(
                    nameDescriptor.getSubAttribute("givenName"),
                    SCIMAttributeValue.createStringValue("Barbara")),
                SCIMAttribute.create(
                    nameDescriptor.getSubAttribute("middleName"),
                    SCIMAttributeValue.createStringValue("Jane")),
                SCIMAttribute.create(
                    nameDescriptor.getSubAttribute("honorificPrefix"),
                    SCIMAttributeValue.createStringValue("Ms.")),
                SCIMAttribute.create(
                    nameDescriptor.getSubAttribute("honorificSuffix"),
                    SCIMAttributeValue.createStringValue("III"))));
    assertEquals(name.getSchema(), coreSchema);
    assertEquals(name.getName(), "name");
    assertFalse(name.getValues().length > 1);
    assertNotNull(name.getValue());
    assertTrue(name.getValue().isComplex());
    assertEquals(name.getValue().getAttributes().size(), 6);
    assertEquals(name.getValue().getAttribute("formatted").
        getValue().getStringValue(), "Ms. Barbara J Jensen III");
    assertEquals(name.getValue().getAttribute("familyName").
        getValue().getStringValue(), "Jensen");
    assertEquals(name.getValue().getAttribute("FAMILYNAME").
        getValue().getStringValue(), "Jensen");
    assertEquals(name.getValue().getAttribute("givenName").
        getValue().getStringValue(), "Barbara");
    assertEquals(name.getValue().getAttribute("middleName").
        getValue().getStringValue(), "Jane");
    assertEquals(name.getValue().getAttribute("honorificPrefix").
        getValue().getStringValue(), "Ms.");
    assertEquals(name.getValue().getAttribute("honorificSuffix").
        getValue().getStringValue(), "III");

    AttributeDescriptor emailsDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "emails");
    final List<SCIMAttribute> emailAttrs = new ArrayList<SCIMAttribute>();
    emailAttrs.add(SCIMAttribute.create(
        emailsDescriptor.getSubAttribute("value"),
        SCIMAttributeValue.createStringValue(
            "bjensen@example.com")));
    emailAttrs.add(SCIMAttribute.create(
        emailsDescriptor.getSubAttribute("type"),
        SCIMAttributeValue.createStringValue("work")));
    emailAttrs.add(SCIMAttribute.create(
        emailsDescriptor.getSubAttribute("primary"),
        SCIMAttributeValue.createBooleanValue(true)));
    final SCIMAttribute emails =
        SCIMAttribute.create(
            emailsDescriptor,
            SCIMAttributeValue.createComplexValue(emailAttrs));
    assertEquals(emails.getName(), "emails");
    assertEquals(emails.getValues().length, 1);
    assertEquals(
        emails.getValues()[0].getAttribute("value").getValue().
            getStringValue(),
        "bjensen@example.com");
    assertEquals(
        emails.getValues()[0].getAttribute("type").getValue().
            getStringValue(),
        "work");
    assertEquals(
        emails.getValues()[0].getAttribute("primary").getValue().
            getBooleanValue(),
        Boolean.TRUE);

    AttributeDescriptor metaDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "meta");
    final Date date = new Date(System.currentTimeMillis());
    final SCIMAttribute meta =
        SCIMAttribute.create(
            metaDescriptor,
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.create(
                    metaDescriptor.getSubAttribute("created"),
                    SCIMAttributeValue.createDateValue(date)),
                SCIMAttribute.create(
                    metaDescriptor.getSubAttribute("lastModified"),
                    SCIMAttributeValue.createDateValue(date))));
    assertEquals(meta.getName(), "meta");
    assertFalse(meta.getValues().length > 1);
    assertNotNull(meta.getValue());
    assertTrue(meta.getValue().isComplex());
    assertEquals(meta.getValue().getAttribute("created").
        getValue().getDateValue(), date);
    assertEquals(meta.getValue().getAttribute("lastModified").
        getValue().getDateValue(), date);
  }



  /**
   * Ensure that an attempt to create a complex value containing a duplicate
   * attribute throws an exception.
   */
  @Test
  public void testDuplicateInComplexValue()
  {
    final String customSchema =
        "http://myextension";

    final AttributeDescriptor descriptor =
        AttributeDescriptor.createAttribute("a",
            AttributeDescriptor.DataType.COMPLEX, "description", customSchema,
            false, false, false);

    try
    {
      SCIMAttributeValue.createComplexValue(
          SCIMAttribute.create(
              descriptor, SCIMAttributeValue.createStringValue("1")),
          SCIMAttribute.create(
              descriptor, SCIMAttributeValue.createStringValue("2")));
      fail("Expected creation of a complex value containing a duplicate " +
           "attribute to throw an exception");
    }
    catch (Exception e)
    {
      // This is expected.
    }

    try
    {
      final List<SCIMAttribute> attrs = new ArrayList<SCIMAttribute>();
      attrs.add(SCIMAttribute.create(
          descriptor, SCIMAttributeValue.createStringValue("1")));
      attrs.add(SCIMAttribute.create(
          descriptor, SCIMAttributeValue.createStringValue("2")));
      SCIMAttributeValue.createComplexValue(attrs);
      fail("Expected creation of a complex value containing a duplicate " +
           "attribute to throw an exception");
    }
    catch (Exception e)
    {
      // This is expected.
    }
  }
}
