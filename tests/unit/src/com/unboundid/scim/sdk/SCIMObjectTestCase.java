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

    final String coreSchema = null;
    final String userSchema =
        SCIMConstants.SCHEMA_URI_CORE_USER;
    final String enterpriseUserSchema =
        SCIMConstants.SCHEMA_URI_ENTERPRISE_USER;
    final String customSchema =
        "http://myextension";

    final SCIMAttribute userID =
        SCIMAttribute.createSingularAttribute(
            null, "id", SCIMAttributeValue.createStringValue(uuid.toString()));

    final SCIMAttribute name =
        SCIMAttribute.createSingularAttribute(
            userSchema,
            "name",
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    userSchema,
                    "formatted",
                    SCIMAttributeValue.createStringValue(
                        "Ms. Barbara J Jensen III")),
                SCIMAttribute.createSingularAttribute(
                    userSchema,
                    "familyName",
                    SCIMAttributeValue.createStringValue("Jensen")),
                SCIMAttribute.createSingularAttribute(
                    userSchema,
                    "givenName",
                    SCIMAttributeValue.createStringValue("Barbara")),
                SCIMAttribute.createSingularAttribute(
                    userSchema,
                    "middleName",
                    SCIMAttributeValue.createStringValue("Jane")),
                SCIMAttribute.createSingularAttribute(
                    userSchema,
                    "honorificPrefix",
                    SCIMAttributeValue.createStringValue("Ms.")),
                SCIMAttribute.createSingularAttribute(
                    userSchema,
                    "honorificSuffix",
                    SCIMAttributeValue.createStringValue("III"))));

    final List<SCIMAttribute> emailAttrs = new ArrayList<SCIMAttribute>();
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        coreSchema,
        "value",
        SCIMAttributeValue.createStringValue(
            "bjensen@example.com")));
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        coreSchema,
        "type",
        SCIMAttributeValue.createStringValue("work")));
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        coreSchema,
        "primary",
        SCIMAttributeValue.createBooleanValue(true)));
    final SCIMAttribute emails =
        SCIMAttribute.createPluralAttribute(
            userSchema,
            "emails",
            SCIMAttributeValue.createComplexValue(emailAttrs));

    final Date date = new Date(System.currentTimeMillis());
    final SCIMAttribute meta =
        SCIMAttribute.createSingularAttribute(
            coreSchema,
            "meta",
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    coreSchema,
                    "created",
                    SCIMAttributeValue.createDateValue(date)),
                SCIMAttribute.createSingularAttribute(
                    coreSchema,
                    "lastModified",
                    SCIMAttributeValue.createDateValue(date))));

    final SCIMAttribute employeeNumber =
        SCIMAttribute.createSingularAttribute(
            enterpriseUserSchema,
            "employeeNumber",
            SCIMAttributeValue.createStringValue("1000001"));

    final SCIMObject user = new SCIMObject();
    user.setResourceType("User");
    assertTrue(user.addAttribute(userID));
    assertTrue(user.addAttribute(meta));
    assertTrue(user.addAttribute(name));
    assertTrue(user.addAttribute(emails));
    user.setAttribute(employeeNumber);

    assertEquals(user.getResourceType(), "User");
    final Collection<String> schemas = user.getSchemas();
    assertNotNull(schemas);
    assertEquals(schemas.size(), 2);
    assertTrue(schemas.contains(userSchema));
    assertTrue(schemas.contains(enterpriseUserSchema));

    assertTrue(user.hasSchema(userSchema));
    assertTrue(user.hasSchema(enterpriseUserSchema));

    assertEquals(user.getAttribute(coreSchema, "id"), userID);
    assertEquals(user.getAttribute(coreSchema, "meta"), meta);

    assertEquals(user.getAttribute(userSchema, "name"), name);
    assertEquals(user.getAttribute(userSchema, "emails"), emails);
    assertEquals(user.getAttribute(enterpriseUserSchema, "employeeNumber"),
                 employeeNumber);
    assertNull(user.getAttribute(userSchema, "employeeNumber"));
    assertNull(user.getAttribute(enterpriseUserSchema, "name"));
    assertNull(user.getAttribute(customSchema, "name"));

    final Collection<SCIMAttribute> coreAttrs = user.getAttributes(coreSchema);
    assertNotNull(coreAttrs);
    assertEquals(coreAttrs.size(), 2);
    assertTrue(coreAttrs.contains(userID));
    assertTrue(coreAttrs.contains(meta));

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

    assertTrue(user.hasAttribute(coreSchema, "id"));
    assertTrue(user.hasAttribute(coreSchema, "meta"));
    assertTrue(user.hasAttribute(userSchema, "name"));
    assertTrue(user.hasAttribute(userSchema, "emails"));
    assertTrue(user.hasAttribute(enterpriseUserSchema, "employeeNumber"));
    assertFalse(user.hasAttribute(userSchema, "employeeNumber"));
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
