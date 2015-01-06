/*
 * Copyright 2011-2015 UnboundID Corp.
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
import com.unboundid.scim.data.Address;
import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Meta;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static org.testng.Assert.*;

/**
 * Test the SCIM resource diff utility.
 */
public class DiffTestCase extends SCIMTestCase
{

  /**
   * Test comparison of single-valued attributes.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testDiffSingularAttribute() throws Exception
  {
    // *** singular ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - unchanged
    source.setUserName("bjensen");
    target.setUserName("bjensen");
    // - added
    target.setNickName("bjj3");
    // - removed
    source.setTitle("hot shot");
    // - updated
    source.setUserType("employee");
    target.setUserType("manager");

    Diff<UserResource> d = Diff.generate(source, target);
    SCIMObject patch = d.toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "nickName").
        getValue().getStringValue().equals("bjj3"));
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).contains("title"));
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "userType").
        getValue().getStringValue().equals("manager"));

    UserResource result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = Diff.generate(source, target, "title").toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "nickName"));
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).contains("title"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userType"));
  }

  /**
   * Test comparison of single-valued complex attributes.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testDiffSingularComplexAttribute() throws Exception
  {
    // *** singular complex ***
    // - unchanged
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    source.setName(new Name("Ms. Barbara J Jensen III",
        "Jensen", "J", "Barbara", "Ms.", "III"));
    target.setName(new Name("Ms. Barbara J Jensen III",
        "Jensen", "J", "Barbara", "Ms.", "III"));

    Diff<UserResource> d = Diff.generate(source, target);
    UserResource result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    SCIMObject patch = d.toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "name"));

    // - added
    source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    target.setName(new Name("Ms. Barbara J Jensen III",
         "Jensen", "J", "Barbara", "Ms.", "III"));

    d = Diff.generate(source, target);
    result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
                UserResource.USER_RESOURCE_FACTORY,false ).getScimObject();
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "name").
        getValue().getAttributes().size(), 6);

    target.setMeta(new Meta(new Date(), new Date(), null, null));
    patch = Diff.generate(source, target, "name.formatted").toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "name").
        getValue().getAttributes().size(), 1);
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta"));

    // - removed
    source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    target = new UserResource(CoreSchema.USER_DESCRIPTOR);
    source.setName(new Name("Ms. Barbara J Jensen III",
        "Jensen", "J", "Barbara", "Ms.", "III"));

    d = Diff.generate(source, target);
    result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).size(), 6);

    patch = Diff.generate(source, target, "name.familyName", "name.givenName")
            .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
              .getScimObject();
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).size(), 2);

    // - removed a sub-attribute
    source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    target = new UserResource(CoreSchema.USER_DESCRIPTOR);
    source.setName(new Name("Ms. Barbara J Jensen III",
            "Jensen", "J", "Barbara", "Ms.", "III"));
    target.setName(new Name("Ms. Barbara J Jensen III",
            "Jensen", "J", "Barbara", "Ms.", null));

    d = Diff.generate(source, target);
    result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
            getValue().getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).size(), 1);

    // - updated
    source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    source.setName(new Name("Ms. Barbara J Jensen III",
        "Jensen", "J", "Barbara", "Ms.", "III"));
    target.setName(new Name("Ms. Barbara J Johnson III",
        "Johnson", "J", "Barbara", "Ms.", "III"));

    d = Diff.generate(source, target);
    result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "name").
        getValue().getAttributes().size(), 2);

    source.setMeta(new Meta(null, new Date(), null, null));
    target.setMeta(new Meta(new Date(), null, null, null));
    patch = Diff.generate(source, target, "name.formatted").toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "name").
        getValue().getAttributes().size(), 1);
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta"));
  }

  /**
   * Test comparison of multi-valued attributes.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testDiffMultiValuedAttribute() throws Exception
  {
    // *** multi-valued ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - unchanged
    Collection<Entry<String>> emails =
        new ArrayList<Entry<String>>(2);
    emails.add(new com.unboundid.scim.data.Entry<String>(
        "bjensen@example.com", "work", true));
    emails.add(new com.unboundid.scim.data.Entry<String>(
        "babs@jensen.org", "home", false));
    source.setEmails(emails);
    target.setEmails(emails);

    // - added
    Collection<com.unboundid.scim.data.Entry<String>> phones =
        new ArrayList<Entry<String>>(1);
    phones.add(new com.unboundid.scim.data.Entry<String>(
        "1234567890", "work", true));
    phones.add(new com.unboundid.scim.data.Entry<String>(
        "0987654321", "home", false));
    target.setPhoneNumbers(phones);

    // - removed
    Collection<com.unboundid.scim.data.Entry<String>> ims =
        new ArrayList<Entry<String>>(1);
    ims.add(new com.unboundid.scim.data.Entry<String>(
        "babs", "aim", true));
    ims.add(new com.unboundid.scim.data.Entry<String>(
        "bjensen", "gtalk", false));
    source.setIms(ims);

    // - updated
    Collection<com.unboundid.scim.data.Entry<String>> sourcePhotos =
        new ArrayList<Entry<String>>(1);
    Collection<com.unboundid.scim.data.Entry<String>> targetPhotos =
        new ArrayList<Entry<String>>(1);
    // -- unchanged
    sourcePhotos.add(new com.unboundid.scim.data.Entry<String>(
        "http://photo0", "photo0", false));
    targetPhotos.add(new com.unboundid.scim.data.Entry<String>(
        "http://photo0", "photo0", false));
    // -- add a new value
    targetPhotos.add(new com.unboundid.scim.data.Entry<String>(
        "http://photo2", "photo2", true));
    // -- update an existing value
    sourcePhotos.add(new com.unboundid.scim.data.Entry<String>(
        "http://photo1", "photo1", true));
    targetPhotos.add(new com.unboundid.scim.data.Entry<String>(
        "http://photo1", "photo1", false));
    // -- remove a value
    sourcePhotos.add(new com.unboundid.scim.data.Entry<String>(
        "http://thumbnail1", "thumbnail", false));
    source.setPhotos(sourcePhotos);
    target.setPhotos(targetPhotos);

    // -- updated with all new values
    Collection<com.unboundid.scim.data.Entry<String>> sourceEntitlements =
        new ArrayList<Entry<String>>(1);
    Collection<com.unboundid.scim.data.Entry<String>> targetEntitlements =
        new ArrayList<Entry<String>>(1);
    sourceEntitlements.add(new com.unboundid.scim.data.Entry<String>(
        "admin", null, false));
    sourceEntitlements.add(new com.unboundid.scim.data.Entry<String>(
        "user", null, false));
    targetEntitlements.add(new com.unboundid.scim.data.Entry<String>(
        "inactive", null, true));
    source.setEntitlements(sourceEntitlements);
    target.setEntitlements(targetEntitlements);

    Diff<UserResource> d = Diff.generate(source, target);
    UserResource result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    SCIMObject patch = d.toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "emails"));
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        "phoneNumbers").
        getValues().length, 2);
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).contains("ims.value"));
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).contains("entitlements.value"));
    SCIMAttributeValue[] values =
        patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "entitlements").getValues();
    assertEquals(values.length, 1);

    values =
        patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "photos").getValues();
    assertEquals(values.length, 4);
    boolean photo2AddFound = false;
    boolean photo1AddFound = false;
    boolean photo1DeleteFound = false;
    boolean thumbnail1DeleteFound = false;

    for(SCIMAttributeValue value : values)
    {
      assertFalse(value.getSubAttributeValue("value",
          AttributeValueResolver.STRING_RESOLVER).equals("http://photo0"));

      if(value.getSubAttributeValue("value",
          AttributeValueResolver.STRING_RESOLVER).equals("http://photo2"))
      {
        photo2AddFound = true;
      }

      if(value.getSubAttributeValue("value",
          AttributeValueResolver.STRING_RESOLVER).equals("http://photo1"))
      {
        if(value.hasAttribute("operation") &&
            value.getSubAttributeValue("operation",
            AttributeValueResolver.STRING_RESOLVER).equals("delete"))
        {
          photo1DeleteFound = true;
        }
        else
        {
          photo1AddFound = true;
        }
      }

      if(value.getSubAttributeValue("value",
          AttributeValueResolver.STRING_RESOLVER).equals("http://thumbnail1") &&
          value.getSubAttributeValue("operation",
              AttributeValueResolver.STRING_RESOLVER).equals("delete"))
      {
        thumbnail1DeleteFound = true;
      }
    }

    assertTrue(photo2AddFound);
    assertTrue(photo1AddFound);
    assertTrue(photo1DeleteFound);
    assertTrue(thumbnail1DeleteFound);

    patch = Diff.generate(source, target,
            "phoneNumbers", "entitlements").toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "emails"));
    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
        "phoneNumbers").getValues().length, 2);
    assertFalse(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).contains("ims.value"));
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).contains("entitlements.value"));
    values =
        patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "entitlements").getValues();
    assertEquals(values.length, 1);
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "photos"));
  }

  /**
   * Test comparison of multi-valued complex attributes.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testDiffMultiValuedComplexAttribute() throws Exception
  {
    Address address1 =
        new Address("100 Universal City Plaza\nHollywood, CA 91608 USA",
                "100 Universal City Plaza",
                "Hollywood",
                "CA",
                "91608",
                "USA",
                "work",
                false);

    Address address2 =
        new Address("456 Hollywood Blvd\nHollywood, CA 91608",
                "456 Hollywood Blvd",
                "Hollywood",
                "CA",
                "91608",
                null,
                "home",
                false);

    Address address3 =
        new Address("1234 Main St\nNew York, NY 10000 USA",
                "1234 Main St",
                "New York",
                "NY",
                "10000",
                "USA",
                "other",
                false);

    Address address3Updated =
        new Address("1234 Main St\nNew York, NY 10000 USA",
                "1234 Main St",
                "New York",
                "NY",
                "10000",
                "USA",
                "other",
                true);

    Address address4 =
        new Address("1234 Wall St\nNew York, NY 10000 USA",
                "1234 Wall St",
                "New York",
                "NY",
                "10000",
                "USA",
                null,
                false);

   // *** multi-valued complex ***
    // - unchanged
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);
    source.setAddresses(Arrays.asList(address1, address2));
    target.setAddresses(Arrays.asList(address1, address2));
    SCIMObject patch = Diff.generate(source, target).toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "addresses"));

    // - added
    source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    target = new UserResource(CoreSchema.USER_DESCRIPTOR);
    target.setAddresses(Arrays.asList(address1));

    Diff<UserResource> d = Diff.generate(source, target);
    UserResource result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "addresses").
        getValue().getAttributes().size(), 7);

    patch = Diff.generate(source, target, "addresses.formatted",
              "addresses.streetAddress").toPartialResource(
                  UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "addresses").
        getValue().getAttributes().size(), 2);

    // - removed
    source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    target = new UserResource(CoreSchema.USER_DESCRIPTOR);
    source.setAddresses(Arrays.asList(address1));

    d = Diff.generate(source, target);
    result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).size(), 6);

    patch = Diff.generate(source, target, "addresses.formatted",
              "addresses.streetAddress").toPartialResource(
                  UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertEquals(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta").
        getValue().getSubAttributeValues("attributes",
        AttributeValueResolver.STRING_RESOLVER).size(), 2);

    // - updated
    source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    Collection<Address> sourceAddresses = new ArrayList<Address>(1);
    Collection<Address> targetAddresses = new ArrayList<Address>(1);
    // -- unchanged
    sourceAddresses.add(address1);
    targetAddresses.add(address1);
    // -- add a new value
    targetAddresses.add(address2);
    // -- update an existing value
    sourceAddresses.add(address3);
    targetAddresses.add(address3Updated);
    // -- remove a value
    sourceAddresses.add(address4);
    source.setAddresses(sourceAddresses);
    target.setAddresses(targetAddresses);

    d = Diff.generate(source, target);
    result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
                UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    SCIMAttributeValue[] values =
        patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "addresses").getValues();
    assertEquals(values.length, 4);
    boolean address2AddFound = false;
    boolean address3AddFound = false;
    boolean address3DeleteFound = false;
    boolean address4DeleteFound = false;

    for(SCIMAttributeValue value : values)
    {
      assertFalse(value.getSubAttributeValue("streetAddress",
          AttributeValueResolver.STRING_RESOLVER).equals(
          "100 Universal City Plaza"));

      if(value.getSubAttributeValue("streetAddress",
          AttributeValueResolver.STRING_RESOLVER).equals("456 Hollywood Blvd"))
      {
        address2AddFound = true;
      }

      if(value.getSubAttributeValue("streetAddress",
          AttributeValueResolver.STRING_RESOLVER).equals("1234 Main St"))
      {
        if(value.hasAttribute("operation") &&
            value.getSubAttributeValue("operation",
            AttributeValueResolver.STRING_RESOLVER).equals("delete"))
        {
          address3DeleteFound = true;
        }
        else
        {
          address3AddFound = true;
        }
      }

      if(value.getSubAttributeValue("streetAddress",
          AttributeValueResolver.STRING_RESOLVER).equals("1234 Wall St") &&
          value.getSubAttributeValue("operation",
              AttributeValueResolver.STRING_RESOLVER).equals("delete"))
      {
        address4DeleteFound = true;
      }
    }

    assertTrue(address2AddFound);
    assertTrue(address3AddFound);
    assertTrue(address3DeleteFound);
    assertTrue(address4DeleteFound);
  }

  /**
   * Test another version of multi-valued complex attribute diffs where the
   * attribute is actually simple, but uses the 'value' sub-attribute and thus
   * is also complex.
   *
   * @throws Exception if an error occurs
   */
  @Test
  public void testDiffMultiValuedComplexAttribute2() throws Exception
  {
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - unchanged
    Collection<Entry<String>> emails = new ArrayList<Entry<String>>(2);
    emails.add(new com.unboundid.scim.data.Entry<String>(
            "bjensen@example.com", null));
    emails.add(new com.unboundid.scim.data.Entry<String>(
            "babs@jensen.org", null));

    source.setEmails(emails);
    target.setEmails(emails);
    Diff<UserResource> d = Diff.generate(source, target);
    UserResource result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    SCIMObject patch = d.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "emails"));

    // - add a new value and delete the two existing values
    Collection<Entry<String>> emails2 = new ArrayList<Entry<String>>(3);
    emails2.add(new com.unboundid.scim.data.Entry<String>(
            "bryanj@yahoo.com", null));
    target.setEmails(emails2);

    d = Diff.generate(source, target);
    result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertTrue(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "emails"));
    assertEquals(result.getEmails().size(), 1);
    assertEquals(result.getEmails(), target.getEmails());

    // - add a value that already exists and two new values
    emails2.clear();
    emails2.add(new com.unboundid.scim.data.Entry<String>(
            "bjensen@example.com", null));
    emails2.add(new com.unboundid.scim.data.Entry<String>(
            "bryanj@gmail.com", null));
    emails2.add(new com.unboundid.scim.data.Entry<String>(
            "bryanj@yahoo.com", null));
    target.setEmails(emails2);

    d = Diff.generate(source, target);
    result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    patch = d.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, false).getScimObject();
    assertTrue(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "emails"));
    assertEquals(result.getEmails().size(), 3);
    assertTrue(result.getEmails()
            .contains(new Entry<String>("bjensen@example.com", null)));
    assertTrue(result.getEmails()
            .contains(new Entry<String>("bryanj@yahoo.com", null)));
    assertTrue(result.getEmails()
            .contains(new Entry<String>("bryanj@gmail.com", null)));
  }

  /**
   * Test comparison against 1st null object.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffNullObject1() throws Exception
  {
    // *** singular ***
    UserResource source = null;
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - unchanged
    target.setUserName("bjensen");
    target.setNickName("bjj3");
    target.setTitle("hot shot");
    target.setUserType("employee");

    try
    {
      Diff.generate(source, target)
          .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
          .getScimObject();
      fail("Expected NullPointerException");
    } catch (NullPointerException e)
    {
      // pass
    }
  }

  /**
   * Test comparison against 2nd null object.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffNullObject2() throws Exception
  {
    // *** singular ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = null;

    // - unchanged
    source.setUserName("bjensen");
    source.setNickName("bjj3");
    source.setTitle("hot shot");
    source.setUserType("employee");

    try
    {
      Diff.generate(source, target)
          .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
          .getScimObject();
      fail("Expected NullPointerException");
    } catch (NullPointerException e)
    {
      // pass
    }
  }

  /**
   * Test comparison of null objects.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffNullObjects() throws Exception
  {
    // *** singular ***
    UserResource source = null;
    UserResource target = null;

    try
    {
      Diff.generate(source, target)
          .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
          .getScimObject();
      fail("Expected NullPointerException");
    } catch (NullPointerException e)
    {
      // pass
    }

    try
    {
      Diff.generate(source, target, "title")
          .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
          .getScimObject();
      fail("Expected NullPointerException");
    } catch (NullPointerException e)
    {
      // pass
    }
  }

  /**
   * Test comparison of objects passing null String attribute argument.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffNullAttributeArgument() throws Exception
  {
    // *** singular ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - unchanged
    source.setUserName("bjensen");
    target.setUserName("bjensen");
    source.setNickName("bjj3");
    target.setNickName("bjj3");
    source.setTitle("hot shot");
    target.setTitle("hot shot");
    source.setUserType("employee");
    target.setUserType("employee");

    try
    {
      Diff.generate(source, target, (String) null)
          .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
          .getScimObject();
      fail("Expected NullPointerException");
    } catch (NullPointerException e)
    {
      // pass
    }
  }

  /**
   * Test comparison of same object with itself.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffSameObject() throws Exception
  {
    // *** singular ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = source;

    // - unchanged
    source.setUserName("bjensen");
    source.setNickName("bjj3");
    source.setTitle("hot shot");
    source.setUserType("employee");

    Diff<UserResource> d = Diff.generate(source, target);
    UserResource result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    SCIMObject patch = d.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertEquals(patch.getAttributes(SCIMConstants.SCHEMA_URI_CORE).size(), 0);

    patch = Diff.generate(source, target, "title")
             .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
                .getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta"));
    assertEquals(patch.getAttributes(SCIMConstants.SCHEMA_URI_CORE).size(), 0);
  }

  /**
   * Test comparison of empty objects.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffEmptyObjects() throws Exception
  {
    // *** singular ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - no attributes
    //

    SCIMObject patch = Diff.generate(source, target)
        .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
            .getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertEquals(patch.getAttributes(SCIMConstants.SCHEMA_URI_CORE).size(), 0);

    patch = Diff.generate(source, target, "title")
        .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
            .getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta"));
    assertEquals(patch.getAttributes(SCIMConstants.SCHEMA_URI_CORE).size(), 0);
  }

  /**
   * Test comparison of objects with equal attributes.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffNoChanges() throws Exception
  {
    // *** singular ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - unchanged
    source.setUserName("bjensen");
    target.setUserName("bjensen");
    source.setNickName("bjj3");
    target.setNickName("bjj3");
    source.setTitle("hot shot");
    target.setTitle("hot shot");
    source.setUserType("employee");
    target.setUserType("employee");

    SCIMObject patch = Diff.generate(source, target)
        .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
            .getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertEquals(patch.getAttributes(SCIMConstants.SCHEMA_URI_CORE).size(), 0);

    patch = Diff.generate(source, target, "title")
        .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
            .getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta"));
    assertEquals(patch.getAttributes(SCIMConstants.SCHEMA_URI_CORE).size(), 0);
  }

  /**
   * Test comparison of objects using
   * Diff#generate(BaseResource, BaseResource, String...)
   * where final argument is an attribute that does not exist in either entry.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffMissingAttribute() throws Exception
  {
    // *** singular ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - unchanged
    source.setUserName("bjensen");
    target.setUserName("bjensen");
    source.setNickName("bjj3");
    target.setNickName("bjj3");
    source.setUserType("employee");
    target.setUserType("employee");

    SCIMObject patch = Diff.generate(source, target, "title")
        .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
            .getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta"));
    assertEquals(patch.getAttributes(SCIMConstants.SCHEMA_URI_CORE).size(), 0);
  }

  /**
   * Test comparison of objects removing all attributes.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testRemoveAll() throws Exception
  {
    // *** singular ***
    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    // - all to be removed
    source.setUserName("bjensen");
    source.setNickName("bjj3");
    source.setTitle("hot shot");
    source.setUserType("employee");

    Diff<UserResource> d = Diff.generate(source, target);
    UserResource result = d.apply(source, UserResource.USER_RESOURCE_FACTORY);
    assertEquals(result, target);

    SCIMObject patch = d.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, false).getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertTrue(patch
        .getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta")
        .getValue()
        .getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).contains("userName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "nickName"));
    assertTrue(patch
        .getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta")
        .getValue()
        .getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).contains("nickName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "title"));
    assertTrue(patch
        .getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta")
        .getValue()
        .getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).contains("title"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userType"));
    assertTrue(patch
        .getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta")
        .getValue()
        .getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).contains("userType"));

    patch = Diff.generate(source, target, "title")
        .toPartialResource(UserResource.USER_RESOURCE_FACTORY, false)
            .getScimObject();

    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userName"));
    assertFalse(patch
        .getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta")
        .getValue()
        .getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).contains("userName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "nickName"));
    assertFalse(patch
        .getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta")
        .getValue()
        .getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).contains("nickName"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "title"));
    assertTrue(patch
        .getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta")
        .getValue()
        .getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).contains("title"));
    assertFalse(patch.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "userType"));
    assertFalse(patch
        .getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta")
        .getValue()
        .getSubAttributeValues("attributes",
            AttributeValueResolver.STRING_RESOLVER).contains("userType"));
  }

  /**
   * Test GroupResource instances.
   *
   * @throws Exception
   *           if an error occurs.
   */
  @Test
  public void testDiffGroupResources() throws Exception
  {
    // *** singular ***
    GroupResource source = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    GroupResource target = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);

    // - unchanged
    source.setDisplayName("mygroup");
    target.setDisplayName("mygroup");

    // - added
    target.setExternalId("mygroupid");

    SCIMObject patch = Diff.generate(source, target)
        .toPartialResource(GroupResource.GROUP_RESOURCE_FACTORY, false)
        .getScimObject();

    assertFalse(patch
        .hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "displayName"));
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "externalId")
        .getValue().getStringValue().equals("mygroupid"));

    patch = Diff.generate(source, target, "externalId")
        .toPartialResource(GroupResource.GROUP_RESOURCE_FACTORY, false)
        .getScimObject();

    assertFalse(patch
        .hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "displayName"));
    assertTrue(patch.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "externalId")
        .getValue().getStringValue().equals("mygroupid"));
  }

  /**
   * Test the fromPartialResource() and toPartialResource() methods to make
   * sure they behave consistently.
   *
   * @throws Exception if an error occurs.
   */
  @Test
  public void testToFromPartialResource() throws Exception
  {
    //Use a multi-valued complex attribute ('address') for this test

    Address address1 =
            new Address("100 Universal City Plaza\nHollywood, CA 91608 USA",
                    "100 Universal City Plaza",
                    "Hollywood",
                    "CA",
                    "91608",
                    "USA",
                    "work",
                    false);

    Address address2 =
            new Address("456 Hollywood Blvd\nHollywood, CA 91608",
                    "456 Hollywood Blvd",
                    "Hollywood",
                    "CA",
                    "91608",
                    null,
                    "home",
                    false);

    Address address3 =
            new Address("1234 Main St\nNew York, NY 10000 USA",
                    "1234 Main St",
                    "New York",
                    "NY",
                    "10000",
                    "USA",
                    "other",
                    false);

    Address address3Updated =
            new Address("1234 Main St\nNew York, NY 10000 USA",
                    "1234 Main St",
                    "New York",
                    "NY",
                    "10000",
                    "USA",
                    "other",
                    true);

    Address address4 =
            new Address("1234 Wall St\nNew York, NY 10000 USA",
                    "1234 Wall St",
                    "New York",
                    "NY",
                    "10000",
                    "USA",
                    null,
                    false);

    UserResource source = new UserResource(CoreSchema.USER_DESCRIPTOR);
    UserResource target = new UserResource(CoreSchema.USER_DESCRIPTOR);

    Collection<Address> sourceAddresses = new ArrayList<Address>(1);
    Collection<Address> targetAddresses = new ArrayList<Address>(1);
    // -- unchanged
    sourceAddresses.add(address1);
    targetAddresses.add(address1);
    // -- add a new value
    targetAddresses.add(address2);
    // -- update an existing value
    sourceAddresses.add(address3);
    targetAddresses.add(address3Updated);
    // -- remove a value
    sourceAddresses.add(address4);
    source.setAddresses(sourceAddresses);
    target.setAddresses(targetAddresses);

    Diff<UserResource> d1 = Diff.generate(source, target);
    UserResource partialResource1 =
            d1.toPartialResource(UserResource.USER_RESOURCE_FACTORY, false);

    Diff<UserResource> d2 = Diff.fromPartialResource(partialResource1, false);
    UserResource partialResource2 =
            d2.toPartialResource(UserResource.USER_RESOURCE_FACTORY, false);

    assertEquals(partialResource1, partialResource2);

    //Test that read-only attributes will be properly filtered out (if desired)
    target.setId("testId");
    d1 = Diff.generate(source, target);
    partialResource1 = d1.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, true);
    assertNotNull(partialResource1.getId());

    d2 = Diff.fromPartialResource(partialResource1, true);
    partialResource2 = d2.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, true);
    assertNotNull(partialResource2.getId());

    d2 = Diff.fromPartialResource(partialResource1, false);
    partialResource2 = d2.toPartialResource(
            UserResource.USER_RESOURCE_FACTORY, false);
    assertNull(partialResource2.getId());
  }
}
