/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.SCIMTestCase;
import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_NAME_USER;
import static com.unboundid.scim.sdk.SCIMConstants.SCHEMA_URI_CORE;
import org.testng.annotations.Test;
import static com.unboundid.scim.SCIMTestUtils.generateName;
import static com.unboundid.scim.SCIMTestUtils.generateAddress;
import static com.unboundid.util.LDAPTestUtils.generateUserEntry;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;



/**
 * This class provides test coverage for the {@link ConfigurableResourceMapper}.
 */
public class UserResourceMapperTestCase
    extends SCIMTestCase
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
    final String coreSchema = SCHEMA_URI_CORE;

    final SCIMObject user = new SCIMObject();
    user.setResourceName(RESOURCE_NAME_USER);

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

    final ResourceMapper mapper = getUserResourceMapper();

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
    assertTrue(entry.hasAttributeValue("telephoneNumber", "800-864-8377"));

    final SCIMObject user2 = new SCIMObject();
    for (final SCIMAttribute a :
        mapper.toSCIMAttributes(RESOURCE_NAME_USER,
                                entry, new SCIMQueryAttributes()))
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
    final InputStream testXML =
        getResource("/com/unboundid/scim/marshal/core-user.xml");

    final Context context = Context.instance();
    final Unmarshaller unmarshaller = context.unmarshaller();
    final SCIMObject user = unmarshaller.unmarshal(testXML, RESOURCE_NAME_USER);

    final ResourceMapper mapper = getUserResourceMapper();

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



  /**
   * Verify that a core user that was mapped from an LDAP entry can be written
   * to XML.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testMapperWithMarshaller()
      throws Exception
  {
    final Entry entry =
        generateUserEntry(
            "user.0", "ou=People,dc=example,dc=com", "Aaren",
            "Atp", "password",
            new Attribute(
                "postalAddress",
                "Aaren Atp$46045 Locust Street$Sioux City, IL  24769"),
            new Attribute("mail", "user.0@example.com"),
            new Attribute("street", "46045 Locust Street"),
            new Attribute("l", "Sioux City"),
            new Attribute("st", "IL"),
            new Attribute("postalCode", "24769"),
            new Attribute("telephoneNumber", "+1 319 805 3070"),
            new Attribute("homePhone", "+1 003 490 8631"));

    final ResourceMapper mapper = getUserResourceMapper();

    List<SCIMAttribute> attributes =
        mapper.toSCIMAttributes(RESOURCE_NAME_USER, entry,
                                new SCIMQueryAttributes());

    final SCIMObject object = new SCIMObject();
    object.setResourceName(RESOURCE_NAME_USER);
    for (final SCIMAttribute a : attributes)
    {
      object.addAttribute(a);
    }

    final Context context = Context.instance();
    final Marshaller marshaller = context.marshaller();
    final Writer writer = new StringWriter();
    marshaller.marshal(object, writer);
  }



  /**
   * Get a User resource mapper.
   *
   * @return  A User resource mapper.
   * @throws Exception  If the resource mapper could not be created.
   */
  private ResourceMapper getUserResourceMapper()
      throws Exception
  {
    List<ResourceMapper> mappers = ConfigurableResourceMapper.parse(
        getResourceFile("/com/unboundid/scim/ldap/resources.xml"));
    for (final ResourceMapper m : mappers)
    {
      if (m.getResourceName().equals(RESOURCE_NAME_USER))
      {
        return m;
      }
    }

    throw new RuntimeException("No User resource mapper found");
  }
}
