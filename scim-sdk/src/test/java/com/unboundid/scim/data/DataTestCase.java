/*
 * Copyright 2011-2013 UnboundID Corp.
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

package com.unboundid.scim.data;

import com.unboundid.scim.SCIMTestCase;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import org.testng.annotations.Test;

import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Test the data model classes.
 */
public class DataTestCase extends SCIMTestCase
{

  /**
   * Test the UserResource model class.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testUserResource() throws Exception
  {
    UserResource user = new UserResource(CoreSchema.USER_DESCRIPTOR);
    assertNull(user.getEntitlements());
    assertNull(user.getAddresses());
    assertNull(user.getIms());
    assertNull(user.getPhotos());
    assertNull(user.getRoles());
    assertNull(user.getDisplayName());
    assertNull(user.getEmails());
    assertNull(user.getGroups());
    assertNull(user.getName());
    assertNull(user.getNickName());
    assertNull(user.getPassword());
    assertNull(user.getPhoneNumbers());
    assertNull(user.getLocale());
    assertNull(user.getPreferredLanguage());
    assertNull(user.getProfileUrl());
    assertNull(user.getTimeZone());
    assertNull(user.getTitle());
    assertNull(user.getUserName());
    assertNull(user.getUserType());
    assertNull(user.isActive());

    user.setActive(true);
    user.setDisplayName("displayName");
    user.setLocale("locale");
    user.setNickName("nickName");
    user.setProfileUrl("profileUrl");
    user.setUserType("userType");
    user.setPreferredLanguage("preferredLanguage");
    user.setTitle("title");
    user.setUserName("userName");
    user.setPassword("password");

    assertTrue(user.isActive());
    assertEquals(user.getDisplayName(), "displayName");
    assertEquals(user.getLocale(), "locale");
    assertEquals(user.getNickName(), "nickName");
    assertEquals(user.getProfileUrl(), "profileUrl");
    assertEquals(user.getUserType(), "userType");
    assertEquals(user.getPreferredLanguage(), "preferredLanguage");
    assertEquals(user.getTitle(), "title");
    assertEquals(user.getUserName(), "userName");
    assertEquals(user.getPassword(), "password");
  }

  /**
   * Test the Name model class.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testName() throws Exception
  {
    final Name name1 = new Name("Ms. Barbara Jane Jensen III",
        "Jensen", "Jane", "Barbara", "Ms.", "III");

    final AttributeDescriptor nameDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(
            SCIMConstants.SCHEMA_URI_CORE, "name");

    final SCIMAttributeValue name1Attr =
        SCIMAttributeValue.createComplexValue(
            SCIMAttribute.create(
                nameDescriptor.getSubAttribute("formatted"),
                SCIMAttributeValue.createStringValue(
                    "Ms. Barbara Jane Jensen III")),
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
                SCIMAttributeValue.createStringValue("III")));

    SCIMAttributeValue name2Attr =
        Name.NAME_RESOLVER.fromInstance(nameDescriptor, name1);

    assertEquals(name1Attr, name2Attr);
    assertEquals(name1Attr.hashCode(), name2Attr.hashCode());

    Name name2 = Name.NAME_RESOLVER.toInstance(name1Attr);

    assertEquals(name1, name2);
    assertEquals(name1.hashCode(), name2.hashCode());
  }

  /**
   * Test the Address model class.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testAddress() throws Exception
  {
    final Address address1 = new Address(
        "100 Universal City Plaza\nHollywood, CA 91608 USA",
        "100 Universal City Plaza",
        "Hollywood",
        "CA",
        "91608",
        "USA",
        "work",
        true);

    final AttributeDescriptor addressDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(
            SCIMConstants.SCHEMA_URI_CORE, "addresses");

    final SCIMAttributeValue address1Attr =
        SCIMAttributeValue.createComplexValue(
            SCIMAttribute.create(
                addressDescriptor.getSubAttribute("formatted"),
                SCIMAttributeValue.createStringValue(
                    "100 Universal City Plaza\nHollywood, CA 91608 USA")),
            SCIMAttribute.create(
                addressDescriptor.getSubAttribute("streetAddress"),
                SCIMAttributeValue.createStringValue(
                    "100 Universal City Plaza")),
            SCIMAttribute.create(
                addressDescriptor.getSubAttribute("locality"),
                SCIMAttributeValue.createStringValue("Hollywood")),
            SCIMAttribute.create(
                addressDescriptor.getSubAttribute("region"),
                SCIMAttributeValue.createStringValue("CA")),
            SCIMAttribute.create(
                addressDescriptor.getSubAttribute("postalCode"),
                SCIMAttributeValue.createStringValue("91608")),
            SCIMAttribute.create(
                addressDescriptor.getSubAttribute("country"),
                SCIMAttributeValue.createStringValue("USA")),
            SCIMAttribute.create(
                addressDescriptor.getSubAttribute("type"),
                SCIMAttributeValue.createStringValue("work")),
            SCIMAttribute.create(
                addressDescriptor.getSubAttribute("primary"),
                SCIMAttributeValue.createBooleanValue(true)));

    SCIMAttributeValue address2Attr =
        Address.ADDRESS_RESOLVER.fromInstance(addressDescriptor, address1);

    assertEquals(address1Attr, address2Attr);
    assertEquals(address1Attr.hashCode(), address2Attr.hashCode());


    Address address2 = Address.ADDRESS_RESOLVER.toInstance(address1Attr);

    assertEquals(address1, address2);
    assertEquals(address1.hashCode(), address2.hashCode());
  }

  /**
   * Test the Meta model class.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testMeta() throws Exception
  {
    Date date = new Date();
    final Meta meta1 = new Meta(date, date, null, null);

    final AttributeDescriptor metaDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(
            SCIMConstants.SCHEMA_URI_CORE, "meta");

    final SCIMAttributeValue meta1Attr =
        SCIMAttributeValue.createComplexValue(
            SCIMAttribute.create(
                metaDescriptor.getSubAttribute("created"),
                SCIMAttributeValue.createDateValue(date)),
            SCIMAttribute.create(
                metaDescriptor.getSubAttribute("lastModified"),
                SCIMAttributeValue.createDateValue(date)));

    SCIMAttributeValue meta2Attr =
        Meta.META_RESOLVER.fromInstance(metaDescriptor, meta1);

    assertEquals(meta1Attr, meta2Attr);
    assertEquals(meta1Attr.hashCode(), meta2Attr.hashCode());

    Meta meta2 = Meta.META_RESOLVER.toInstance(meta1Attr);

    assertEquals(meta1, meta2);
    assertEquals(meta1.hashCode(), meta2.hashCode());
  }

  /**
   * Test the Manager model class.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testManager() throws Exception
  {
    final Manager manager1 = new Manager("id1", "Bob");

    final AttributeDescriptor managerDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager");

    final SCIMAttributeValue manager1Attr =
        SCIMAttributeValue.createComplexValue(
            SCIMAttribute.create(
                managerDescriptor.getSubAttribute("managerId"),
                SCIMAttributeValue.createStringValue("id1")),
            SCIMAttribute.create(
                managerDescriptor.getSubAttribute("displayName"),
                SCIMAttributeValue.createStringValue("Bob")));

    SCIMAttributeValue manager2Attr =
        Manager.MANAGER_RESOLVER.fromInstance(managerDescriptor, manager1);

    assertEquals(manager1Attr, manager2Attr);
    assertEquals(manager1Attr.hashCode(), manager2Attr.hashCode());

    Manager manager2 = Manager.MANAGER_RESOLVER.toInstance(manager1Attr);

    assertEquals(manager1, manager2);
    assertEquals(manager1.hashCode(), manager2.hashCode());
  }

  /**
   * Test the Entry model class.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testEntry() throws Exception
  {
    final Entry<String> entry1 = new Entry<String>("test@test.com",
        "work", true, "display", "delete");

    final AttributeDescriptor emailsDescriptor =
        CoreSchema.USER_DESCRIPTOR.getAttribute(
            SCIMConstants.SCHEMA_URI_CORE, "emails");

    final SCIMAttributeValue entry1Attr =
        SCIMAttributeValue.createComplexValue(
                SCIMAttribute.create(
                    emailsDescriptor.getSubAttribute("value"),
                    SCIMAttributeValue.createStringValue("test@test.com")),
                SCIMAttribute.create(
                    emailsDescriptor.getSubAttribute("type"),
                    SCIMAttributeValue.createStringValue("work")),
                SCIMAttribute.create(
                    emailsDescriptor.getSubAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(true)),
                SCIMAttribute.create(
                    emailsDescriptor.getSubAttribute("display"),
                    SCIMAttributeValue.createStringValue("display")),
                SCIMAttribute.create(
                    emailsDescriptor.getSubAttribute("operation"),
                    SCIMAttributeValue.createStringValue("delete")));

    SCIMAttributeValue entry2Attr =
        Entry.STRINGS_RESOLVER.fromInstance(emailsDescriptor, entry1);

    assertEquals(entry1Attr, entry2Attr);
    assertEquals(entry1Attr.hashCode(), entry2Attr.hashCode());

    Entry<String> entry2 = Entry.STRINGS_RESOLVER.toInstance(entry1Attr);

    assertEquals(entry1, entry2);
    assertEquals(entry1.hashCode(), entry2.hashCode());
  }
}
