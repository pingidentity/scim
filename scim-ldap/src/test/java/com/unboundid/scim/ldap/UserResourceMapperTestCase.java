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

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim.data.Address;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.SCIMTestCase;
import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_NAME_USER;
import org.testng.annotations.Test;
import static com.unboundid.util.LDAPTestUtils.generateUserEntry;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
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
    final UserResource user = new UserResource(CoreSchema.USER_DESCRIPTOR);

    user.setUserName("bjensen");

    user.setName(new Name("Ms. Barbara J Jensen III",
        "Jensen", "J", "Barbara", "Ms.", "III"));
    Collection<com.unboundid.scim.data.Entry<String>> emails =
        new ArrayList<com.unboundid.scim.data.Entry<String>>(2);
    emails.add(new com.unboundid.scim.data.Entry<String>(
        "bjensen@example.com", "work", true));
    emails.add(new com.unboundid.scim.data.Entry<String>(
        "babs@jensen.org", "home", false));
    user.setEmails(emails);

    Collection<Address> addresses = new ArrayList<Address>(2);
    addresses.add(
        new Address("100 Universal City Plaza\nHollywood, CA 91608 USA",
            "100 Universal City Plaza",
            "Hollywood",
            "CA",
            "91608",
            "USA",
            "work",
            true));
    addresses.add(
        new Address("456 Hollywood Blvd\nHollywood, CA 91608 USA",
            "456 Hollywood Blvd",
            "Hollywood",
            "CA",
            "91608",
            "USA",
            "home",
            false));
    user.setAddresses(addresses);

    Collection<com.unboundid.scim.data.Entry<String>> phoneNumbers =
        new ArrayList<com.unboundid.scim.data.Entry<String>>(2);
    phoneNumbers.add(new com.unboundid.scim.data.Entry<String>(
        "800-864-8377", "work", false));
    phoneNumbers.add(new com.unboundid.scim.data.Entry<String>(
        "818-123-4567", "mobile", false));
    user.setPhoneNumbers(phoneNumbers);

    final ResourceMapper mapper = getUserResourceMapper();

    final Entry entry = new Entry("cn=test",
        mapper.toLDAPAttributes(user.getScimObject()));
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
        mapper.toSCIMAttributes(
            entry,
            new SCIMQueryAttributes(user.getResourceDescriptor(), null), null))
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

    final ResourceDescriptor userResourceDescriptor =
        CoreSchema.USER_DESCRIPTOR;
    final Context context = Context.instance();
    final Unmarshaller unmarshaller = context.unmarshaller();
    final SCIMObject user = unmarshaller.unmarshal(testXML,
        userResourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY).
        getScimObject();

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

    final ResourceDescriptor userResourceDescriptor =
        CoreSchema.USER_DESCRIPTOR;
    final ResourceMapper mapper = getUserResourceMapper();

    List<SCIMAttribute> attributes =
        mapper.toSCIMAttributes(
            entry,
            new SCIMQueryAttributes(userResourceDescriptor, null), null);

    final SCIMObject object = new SCIMObject();
    for (final SCIMAttribute a : attributes)
    {
      object.addAttribute(a);
    }

    final Context context = Context.instance();
    final Marshaller marshaller = context.marshaller();
    final OutputStream outputStream = new ByteArrayOutputStream();
    marshaller.marshal(BaseResource.BASE_RESOURCE_FACTORY.createResource(
        userResourceDescriptor, object), outputStream);
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
      if (m.getResourceDescriptor().getName().equals(RESOURCE_NAME_USER))
      {
        return m;
      }
    }

    throw new RuntimeException("No User resource mapper found");
  }
}
