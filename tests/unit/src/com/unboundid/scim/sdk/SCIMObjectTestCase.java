/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;



/**
 * This class provides test coverage for the {@code SCIMAttribute} class.
 */
public class SCIMObjectTestCase
    extends SCIMRITestCase
{
  /**
   * Test that valid examples can be created and manipulated.
   */
  @Test
  public void testExampleAttributes()
  {
    final UUID uuid = UUID.randomUUID();

    final String userSchema =
        "urn:scim:schemas:core:user:1.0";
    final String enterpriseUserSchema =
        "urn:scim:schemas:extension:user:enterprise:1.0";
    final String customSchema =
        "http://myextension";

    final SCIMAttribute name =
        SCIMAttribute.createSingularAttribute(
            "name",
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    "formatted",
                    SCIMAttributeValue.createStringValue(
                        "Ms. Barbara J Jensen III")),
                SCIMAttribute.createSingularAttribute(
                    "familyName",
                    SCIMAttributeValue.createStringValue("Jensen")),
                SCIMAttribute.createSingularAttribute(
                    "givenName",
                    SCIMAttributeValue.createStringValue("Barbara")),
                SCIMAttribute.createSingularAttribute(
                    "middleName",
                    SCIMAttributeValue.createStringValue("Jane")),
                SCIMAttribute.createSingularAttribute(
                    "honorificPrefix",
                    SCIMAttributeValue.createStringValue("Ms.")),
                SCIMAttribute.createSingularAttribute(
                    "honorificSuffix",
                    SCIMAttributeValue.createStringValue("III"))));

    final List<SCIMAttribute> emailAttrs = new ArrayList<SCIMAttribute>();
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        "value",
        SCIMAttributeValue.createStringValue(
            "bjensen@example.com")));
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        "type",
        SCIMAttributeValue.createStringValue("work")));
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        "primary",
        SCIMAttributeValue.createBooleanValue(true)));
    final SCIMAttribute emails =
        SCIMAttribute.createPluralAttribute(
            "emails",
            SCIMAttributeValue.createComplexValue(emailAttrs));

    final Date date = new Date(System.currentTimeMillis());
    final SCIMAttribute meta =
        SCIMAttribute.createSingularAttribute(
            "meta",
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    "created",
                    SCIMAttributeValue.createDateValue(date)),
                SCIMAttribute.createSingularAttribute(
                    "lastModified",
                    SCIMAttributeValue.createDateValue(date))));

    final SCIMAttribute employeeNumber =
        SCIMAttribute.createSingularAttribute(
            "employeeNumber",
            SCIMAttributeValue.createStringValue("1000001"));

    final SCIMObject user = new SCIMObject(uuid.toString(), meta);
    assertTrue(user.addAttribute(userSchema, name));
    assertTrue(user.addAttribute(userSchema, emails));
    user.setAttribute(enterpriseUserSchema, employeeNumber);

    assertEquals(user.getId(), uuid.toString());
    user.setId(null);
    assertNull(user.getId());
    user.setId(uuid.toString());
    assertEquals(user.getId(), uuid.toString());

    assertEquals(user.getMeta(), meta);
    user.setMeta(null);
    assertNull(user.getMeta());
    user.setMeta(meta);
    assertEquals(user.getMeta(), meta);

    final Collection<String> schemas = user.getSchemas();
    assertNotNull(schemas);
    assertEquals(schemas.size(), 2);
    assertTrue(schemas.contains(userSchema));
    assertTrue(schemas.contains(enterpriseUserSchema));

    assertTrue(user.hasSchema(userSchema));
    assertTrue(user.hasSchema(enterpriseUserSchema));

    assertEquals(user.getAttribute(userSchema, "name"), name);
    assertEquals(user.getAttribute(userSchema, "emails"), emails);
    assertEquals(user.getAttribute(enterpriseUserSchema, "employeeNumber"),
                 employeeNumber);
    assertNull(user.getAttribute(userSchema, "employeeNumber"));
    assertNull(user.getAttribute(enterpriseUserSchema, "name"));
    assertNull(user.getAttribute(customSchema, "name"));

    final Collection<SCIMAttribute> userAttrs = user.getAttributes(userSchema);
    assertNotNull(userAttrs);
    assertEquals(userAttrs.size(), 2);
    assertTrue(userAttrs.contains(name));
    assertTrue(userAttrs.contains(emails));

    final Collection<SCIMAttribute> enterpriseUserAttrs =
        user.getAttributes(enterpriseUserSchema);
    assertNotNull(enterpriseUserAttrs);
    assertEquals(enterpriseUserAttrs.size(), 1);
    assertTrue(enterpriseUserAttrs.contains(employeeNumber));

    assertTrue(user.getAttributes(customSchema).isEmpty());

    assertTrue(user.hasAttribute(userSchema, "name"));
    assertTrue(user.hasAttribute(userSchema, "emails"));
    assertTrue(user.hasAttribute(enterpriseUserSchema, "employeeNumber"));
    assertFalse(user.hasAttribute(userSchema, "employeeNumber"));
    assertFalse(user.hasAttribute(enterpriseUserSchema, "name"));
    assertFalse(user.hasAttribute(customSchema, "name"));

    assertFalse(user.addAttribute(userSchema, name));
    user.setAttribute(userSchema, name);

    assertTrue(user.removeAttribute(userSchema, "name"));
    assertFalse(user.removeAttribute(userSchema, "name"));
    assertTrue(user.removeAttribute(userSchema, "emails"));
    assertFalse(user.removeAttribute(userSchema, "emails"));
    assertFalse(user.hasSchema(userSchema));

    assertTrue(user.removeAttribute(enterpriseUserSchema, "employeeNumber"));
    assertFalse(user.hasSchema(enterpriseUserSchema));

    assertTrue(user.getSchemas().isEmpty());
  }
}
