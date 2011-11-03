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
        SCIMAttribute.createSingularAttribute(
            CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "id"),
            SCIMAttributeValue.createStringValue(uuid.toString()));
    assertEquals(userID.getSchema(), coreSchema);
    assertEquals(userID.getName(), "id");
    assertFalse(userID.isPlural());
    assertNull(userID.getPluralValues());
    assertNotNull(userID.getSingularValue());
    assertFalse(userID.getSingularValue().isComplex());
    assertNull(userID.getSingularValue().getAttribute("wrong"));
    assertEquals(userID.getSingularValue().getStringValue(), uuid.toString());

    AttributeDescriptor nameDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "name");
    final SCIMAttribute name =
        SCIMAttribute.createSingularAttribute(nameDescriptor,
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    nameDescriptor.getSubAttribute("formatted"),
                    SCIMAttributeValue.createStringValue(
                        "Ms. Barbara J Jensen III")),
                SCIMAttribute.createSingularAttribute(
                    nameDescriptor.getSubAttribute("familyName"),
                    SCIMAttributeValue.createStringValue("Jensen")),
                SCIMAttribute.createSingularAttribute(
                    nameDescriptor.getSubAttribute("givenName"),
                    SCIMAttributeValue.createStringValue("Barbara")),
                SCIMAttribute.createSingularAttribute(
                    nameDescriptor.getSubAttribute("middleName"),
                    SCIMAttributeValue.createStringValue("Jane")),
                SCIMAttribute.createSingularAttribute(
                    nameDescriptor.getSubAttribute("honorificPrefix"),
                    SCIMAttributeValue.createStringValue("Ms.")),
                SCIMAttribute.createSingularAttribute(
                    nameDescriptor.getSubAttribute("honorificSuffix"),
                    SCIMAttributeValue.createStringValue("III"))));
    assertEquals(name.getSchema(), coreSchema);
    assertEquals(name.getName(), "name");
    assertFalse(name.isPlural());
    assertNull(name.getPluralValues());
    assertNotNull(name.getSingularValue());
    assertTrue(name.getSingularValue().isComplex());
    assertEquals(name.getSingularValue().getAttributes().size(), 6);
    assertEquals(name.getSingularValue().getAttribute("formatted").
        getSingularValue().getStringValue(), "Ms. Barbara J Jensen III");
    assertEquals(name.getSingularValue().getAttribute("familyName").
        getSingularValue().getStringValue(), "Jensen");
    assertEquals(name.getSingularValue().getAttribute("FAMILYNAME").
        getSingularValue().getStringValue(), "Jensen");
    assertEquals(name.getSingularValue().getAttribute("givenName").
        getSingularValue().getStringValue(), "Barbara");
    assertEquals(name.getSingularValue().getAttribute("middleName").
        getSingularValue().getStringValue(), "Jane");
    assertEquals(name.getSingularValue().getAttribute("honorificPrefix").
        getSingularValue().getStringValue(), "Ms.");
    assertEquals(name.getSingularValue().getAttribute("honorificSuffix").
        getSingularValue().getStringValue(), "III");

    AttributeDescriptor emailsDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "emails");
    final List<SCIMAttribute> emailAttrs = new ArrayList<SCIMAttribute>();
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        emailsDescriptor.getSubAttribute("value"),
        SCIMAttributeValue.createStringValue(
            "bjensen@example.com")));
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        emailsDescriptor.getSubAttribute("type"),
        SCIMAttributeValue.createStringValue("work")));
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        emailsDescriptor.getSubAttribute("primary"),
        SCIMAttributeValue.createBooleanValue(true)));
    final SCIMAttribute emails =
        SCIMAttribute.createPluralAttribute(
            emailsDescriptor,
            SCIMAttributeValue.createComplexValue(emailAttrs));
    assertEquals(emails.getName(), "emails");
    assertTrue(emails.isPlural());
    assertNotNull(emails.getPluralValues());
    assertNull(emails.getSingularValue());
    assertEquals(emails.getPluralValues().length, 1);
    assertEquals(
        emails.getPluralValues()[0].getAttribute("value").getSingularValue().
            getStringValue(),
        "bjensen@example.com");
    assertEquals(
        emails.getPluralValues()[0].getAttribute("type").getSingularValue().
            getStringValue(),
        "work");
    assertEquals(
        emails.getPluralValues()[0].getAttribute("primary").getSingularValue().
            getBooleanValue(),
        Boolean.TRUE);

    AttributeDescriptor metaDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(coreSchema, "meta");
    final Date date = new Date(System.currentTimeMillis());
    final SCIMAttribute meta =
        SCIMAttribute.createSingularAttribute(
            metaDescriptor,
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    metaDescriptor.getSubAttribute("created"),
                    SCIMAttributeValue.createDateValue(date)),
                SCIMAttribute.createSingularAttribute(
                    metaDescriptor.getSubAttribute("lastModified"),
                    SCIMAttributeValue.createDateValue(date))));
    assertEquals(meta.getName(), "meta");
    assertFalse(meta.isPlural());
    assertNull(meta.getPluralValues());
    assertNotNull(meta.getSingularValue());
    assertTrue(meta.getSingularValue().isComplex());
    assertEquals(meta.getSingularValue().getAttribute("created").
        getSingularValue().getDateValue(), date);
    assertEquals(meta.getSingularValue().getAttribute("lastModified").
        getSingularValue().getDateValue(), date);
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
        AttributeDescriptor.singularSimple("a",
            AttributeDescriptor.DataType.COMPLEX, "description", customSchema,
            false, false, false);

    try
    {
      SCIMAttributeValue.createComplexValue(
          SCIMAttribute.createSingularAttribute(
              descriptor, SCIMAttributeValue.createStringValue("1")),
          SCIMAttribute.createSingularAttribute(
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
      attrs.add(SCIMAttribute.createSingularAttribute(
          descriptor, SCIMAttributeValue.createStringValue("1")));
      attrs.add(SCIMAttribute.createSingularAttribute(
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
