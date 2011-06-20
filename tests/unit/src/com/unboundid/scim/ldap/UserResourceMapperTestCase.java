/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim.marshall.Context;
import com.unboundid.scim.marshall.Unmarshaller;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;

import java.io.File;



/**
 * This class provides test coverage for the {@link UserResourceMapper}.
 */
public class UserResourceMapperTestCase
    extends SCIMRITestCase
{
  /**
   * Verify that a core user can be mapped to and from an LDAP entry.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testUserMapper()
      throws Exception
  {
    final String coreSchema = SCIMConstants.SCHEMA_URI_CORE;

    final SCIMObject user = new SCIMObject();
    user.setResourceType("User");

    user.addAttribute(
        SCIMAttribute.createSingularStringAttribute(
            coreSchema, "userName", "bjensen"));

    user.addAttribute(generateName("Ms. Barbara J Jensen III",
                                   "Jensen", "Barbara", "J", "Ms.", "III"));
    final SCIMAttribute emails =
        SCIMAttribute.createPluralAttribute(
            coreSchema,"emails",
            SCIMAttributeValue.createPluralStringValue(
                coreSchema, "bjensen@example.com", "work", true),
            SCIMAttributeValue.createPluralStringValue(
                coreSchema, "babs@jensen.org", "home", false));
    user.addAttribute(emails);

    user.addAttribute(
        SCIMAttribute.createPluralAttribute(
            coreSchema, "addresses",
            generateAddress("work",
                            "100 Universal City Plaza\nHollywood, CA 91608 USA",
                            "100 Universal City Plaza",
                            "Hollywood",
                            "CA",
                            "91608",
                            "USA",
                            true),
            generateAddress("home",
                            "456 Hollywood Blvd\nHollywood, CA 91608 USA",
                            "456 Hollywood Blvd",
                            "Hollywood",
                            "CA",
                            "91608",
                            "USA",
                            true)));

    user.addAttribute(
        SCIMAttribute.createPluralAttribute(
            coreSchema, "phoneNumbers",
            SCIMAttributeValue.createPluralStringValue(
                coreSchema, "800-864-8377", "work", false),
            SCIMAttributeValue.createPluralStringValue(
                coreSchema, "818-123-4567", "mobile", false)));

    final UserResourceMapper mapper = new UserResourceMapper();
    mapper.initializeMapper();

    final Entry entry = new Entry("cn=test", mapper.toLDAPAttributes(user));
    assertTrue(entry.hasAttributeValue("uid", "bjensen"));
    assertTrue(entry.hasAttributeValue("mail", "bjensen@example.com"));
    assertTrue(entry.hasAttributeValue("cn", "Ms. Barbara J Jensen III"));
    assertTrue(entry.hasAttributeValue("sn", "Jensen"));
    assertTrue(entry.hasAttributeValue("givenName", "Barbara"));
    assertTrue(entry.hasAttributeValue(
        "postalAddress",
        "100 Universal City Plaza$Hollywood, CA 91608 USA"));
    assertTrue(entry.hasAttributeValue("street", "100 Universal City Plaza"));
    assertTrue(entry.hasAttributeValue("l", "Hollywood"));
    assertTrue(entry.hasAttributeValue("st", "CA"));
    assertTrue(entry.hasAttributeValue("postalCode", "91608"));
    assertTrue(entry.hasAttributeValue("c", "USA"));
    assertTrue(entry.hasAttributeValue("telephoneNumber", "800-864-8377"));

    final SCIMObject user2 = new SCIMObject();
    for (final SCIMAttribute a :
        mapper.toSCIMAttributes(entry, new SCIMQueryAttributes()))
    {
      user2.addAttribute(a);
    }

    final Entry entry2 = new Entry("cn=test", mapper.toLDAPAttributes(user2));
    assertEquals(entry2, entry);
    assertEquals(entry, entry2);

    mapper.finalizeMapper();
  }



  /**
   * Verify that a core user that was created from XML can be mapped to an
   * LDAP entry.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testMapperWithUnmarshaller()
      throws Exception
  {
    final File testXML = getTestResource("marshal/core-user.xml");

    final Context context = Context.instance();
    final Unmarshaller unmarshaller = context.unmarshaller();
    final SCIMObject user = unmarshaller.unmarshal(testXML);

    final UserResourceMapper mapper = new UserResourceMapper();
    mapper.initializeMapper();

    final Entry entry = new Entry("cn=test", mapper.toLDAPAttributes(user));
    assertTrue(entry.hasAttributeValue("uid", "user.0"));
    assertTrue(entry.hasAttributeValue("mail", "user.0@example.com"));
    assertTrue(entry.hasAttributeValue("cn", "Aaren Atp"));
    assertTrue(entry.hasAttributeValue("sn", "Atp"));
    assertTrue(entry.hasAttributeValue("givenName", "Aaren"));
    assertTrue(entry.hasAttribute("postalAddress"));
    assertTrue(entry.hasAttributeValue("street", "46045 Locust Street"));
    assertTrue(entry.hasAttributeValue("l", "Sioux City"));
    assertTrue(entry.hasAttributeValue("st", "IL"));
    assertTrue(entry.hasAttributeValue("postalCode", "24769"));
    assertTrue(entry.hasAttributeValue("telephoneNumber", "+1 319 805 3070"));
    assertTrue(entry.hasAttributeValue("homePhone", "+1 003 490 8631"));
  }
}
