/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.config.AttributeDescriptor;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;



/**
 * This class provides test coverage for the {@code SCIMAttribute} class.
 */
public class SCIMAttributeTestCase
  extends SCIMRITestCase
{
  /**
   * Test that valid examples can be created and inspected.
   */
  @Test
  public void testExampleAttributes()
  {
    final String coreSchema =
        SCIMConstants.SCHEMA_URI_CORE;
    final String customSchema =
        "http://myextension";

    final UUID uuid = UUID.randomUUID();

    final SCIMAttribute userID =
        SCIMAttribute.createSingularAttribute(
            new AttributeDescriptor(
                new AttributeDescriptor.Builder(coreSchema,"dn", "id")),
            SCIMAttributeValue.createStringValue(uuid.toString()));
    assertEquals(userID.getSchema(), coreSchema);
    assertEquals(userID.getName(), "id");
    assertFalse(userID.isPlural());
    assertNull(userID.getPluralValues());
    assertNotNull(userID.getSingularValue());
    assertFalse(userID.getSingularValue().isComplex());
    assertNull(userID.getSingularValue().getAttribute("wrong"));
    assertEquals(userID.getSingularValue().getStringValue(), uuid.toString());

    final SCIMAttribute schemas =
        SCIMAttribute.createPluralAttribute(
            new AttributeDescriptor(
                new AttributeDescriptor.Builder(
                    coreSchema, null, "schemas").plural(true)),
            SCIMAttributeValue.createStringValue(coreSchema),
            SCIMAttributeValue.createStringValue(customSchema));
    assertEquals(schemas.getName(), "schemas");
    assertTrue(schemas.isPlural());
    assertNotNull(schemas.getPluralValues());
    assertNull(schemas.getSingularValue());
    assertEquals(schemas.getPluralValues().length, 2);
    assertEquals(schemas.getPluralValues()[0].getStringValue(),
                 coreSchema);
    assertEquals(schemas.getPluralValues()[1].getStringValue(),
                 customSchema);

    final SCIMAttribute name =
        SCIMAttribute.createSingularAttribute(
            new AttributeDescriptor(
                new AttributeDescriptor.Builder(coreSchema,null,"name")),
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    new AttributeDescriptor(
                        new AttributeDescriptor.Builder(
                            coreSchema,"displayName","formatted")),
                    SCIMAttributeValue.createStringValue(
                        "Ms. Barbara J Jensen III")),
                SCIMAttribute.createSingularAttribute(
                    new AttributeDescriptor(
                        new AttributeDescriptor.Builder(
                            coreSchema,"sn","familyName")),
                    SCIMAttributeValue.createStringValue("Jensen")),
                SCIMAttribute.createSingularAttribute(
                    new AttributeDescriptor(
                        new AttributeDescriptor.Builder(
                            coreSchema,"givenName","givenName")),
                    SCIMAttributeValue.createStringValue("Barbara")),
                SCIMAttribute.createSingularAttribute(
                    new AttributeDescriptor(
                        new AttributeDescriptor.Builder(
                            coreSchema,null,"middleName")),
                    SCIMAttributeValue.createStringValue("Jane")),
                SCIMAttribute.createSingularAttribute(
                    new AttributeDescriptor(
                        new AttributeDescriptor.Builder(
                            coreSchema,null,"honorificPrefix")),
                    SCIMAttributeValue.createStringValue("Ms.")),
                SCIMAttribute.createSingularAttribute(
                    new AttributeDescriptor(
                        new AttributeDescriptor.Builder(
                            coreSchema,null,"honorificSuffix")),
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
    assertNull(name.getSingularValue().getAttribute("familyname"));
    assertEquals(name.getSingularValue().getAttribute("givenName").
        getSingularValue().getStringValue(), "Barbara");
    assertEquals(name.getSingularValue().getAttribute("middleName").
        getSingularValue().getStringValue(), "Jane");
    assertEquals(name.getSingularValue().getAttribute("honorificPrefix").
        getSingularValue().getStringValue(), "Ms.");
    assertEquals(name.getSingularValue().getAttribute("honorificSuffix").
        getSingularValue().getStringValue(), "III");

    final List<SCIMAttribute> emailAttrs = new ArrayList<SCIMAttribute>();
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        new AttributeDescriptor(
            new AttributeDescriptor.Builder(coreSchema,"mail","value")),
        SCIMAttributeValue.createStringValue(
            "bjensen@example.com")));
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        new AttributeDescriptor(
            new AttributeDescriptor.Builder(coreSchema,null,"type")),
        SCIMAttributeValue.createStringValue("work")));
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        new AttributeDescriptor(
            new AttributeDescriptor.Builder(coreSchema,null,"primary")),
        SCIMAttributeValue.createBooleanValue(true)));
    final SCIMAttribute emails =
        SCIMAttribute.createPluralAttribute(
            new AttributeDescriptor(
                new AttributeDescriptor.Builder(
                    coreSchema,null,"emails").plural(true)),
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

    final Date date = new Date(System.currentTimeMillis());
    final SCIMAttribute meta =
        SCIMAttribute.createSingularAttribute(
            new AttributeDescriptor(
                new AttributeDescriptor.Builder(
                    coreSchema,null,"meta").complex(true)),
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    new AttributeDescriptor(
                        new AttributeDescriptor.Builder(
                            coreSchema,null,"created")),
                    SCIMAttributeValue.createDateValue(date)),
                SCIMAttribute.createSingularAttribute(
                    new AttributeDescriptor(
                        new AttributeDescriptor.Builder(
                            coreSchema,null,"lastModified")),
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

    final AttributeDescriptor descriptor = new AttributeDescriptor(
        new AttributeDescriptor.Builder(
            customSchema,null,"a").complex(true));

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
