/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.config.AttributeDescriptor;
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

    final String coreSchema =
        SCIMConstants.SCHEMA_URI_CORE;
    final String enterpriseUserSchema =
        SCIMConstants.SCHEMA_URI_ENTERPRISE_USER;
    final String customSchema =
        "http://myextension";

    final SCIMAttribute userID =
        SCIMAttribute.createSingularAttribute(
            new AttributeDescriptor(
                new AttributeDescriptor.Builder(coreSchema, "dn", "id")),
            SCIMAttributeValue.createStringValue(uuid.toString()));

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
                  SCIMAttributeValue.createStringValue("Barbara"))));

    final List<SCIMAttribute> emailAttrs = new ArrayList<SCIMAttribute>();
    emailAttrs.add(SCIMAttribute.createSingularAttribute(
        new AttributeDescriptor(new AttributeDescriptor.Builder(
            coreSchema,"mail","email")),
      SCIMAttributeValue.createStringValue(
          "bjensen@example.com")));
    final SCIMAttribute emails =
        SCIMAttribute.createPluralAttribute(
            new AttributeDescriptor(
                new AttributeDescriptor.Builder(
                    coreSchema,null,"emails").plural(true)),
            SCIMAttributeValue.createComplexValue(emailAttrs));

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

    final SCIMAttribute employeeNumber =
        SCIMAttribute.createSingularAttribute(
          new AttributeDescriptor(
              new AttributeDescriptor.Builder(
                  enterpriseUserSchema,"employeeNumber","employeeNumber")),
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
