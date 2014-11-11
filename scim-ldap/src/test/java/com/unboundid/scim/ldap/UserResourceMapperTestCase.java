/*
 * Copyright 2011-2014 UnboundID Corp.
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
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.scim.data.Address;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.SCIMTestCase;
import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_NAME_USER;

import com.unboundid.scim.sdk.SortParameters;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static com.unboundid.util.LDAPTestUtils.generateUserEntry;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



/**
 * This class provides test coverage for the {@link ResourceMapper}.
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
        "tel:+1-800-864-8377", "work", false));
    phoneNumbers.add(new com.unboundid.scim.data.Entry<String>(
        "tel:+1-818-123-4567", "mobile", false));
    phoneNumbers.add(new com.unboundid.scim.data.Entry<String>(
        "tel:+1-324-231-4567", "pager", false));
    user.setPhoneNumbers(phoneNumbers);

    final ResourceMapper mapper = getUserResourceMapper();

    final Entry entry = new Entry("cn=test",
        mapper.toLDAPAttributes(user.getScimObject(), null));
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
    assertTrue(entry.hasAttributeValue("telephoneNumber", "+1 800-864-8377"));
    assertTrue(entry.hasAttributeValue("mobile", "+1 818-123-4567"));
    assertTrue(entry.hasAttributeValue("pager", "+1 324-231-4567"));
    final SCIMObject user2 = new SCIMObject();
    for (final SCIMAttribute a :
        mapper.toSCIMAttributes(
            entry,
            new SCIMQueryAttributes(user.getResourceDescriptor(), null), null))
    {
      user2.addAttribute(a);
    }

    UserResource resource = new UserResource(CoreSchema.USER_DESCRIPTOR, user2);
    assertTrue(resource.getPhoneNumbers().containsAll(phoneNumbers));

    final Entry entry2 =
        new Entry("cn=test", mapper.toLDAPAttributes(user2, null));
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
    final Unmarshaller unmarshaller = new XmlUnmarshaller();
    final SCIMObject user = unmarshaller.unmarshal(testXML,
        userResourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY).
        getScimObject();

    final ResourceMapper mapper = getUserResourceMapper();

    final Entry entry =
        new Entry("cn=test", mapper.toLDAPAttributes(user, null));
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
    assertTrue(entry.hasAttributeValue("telephoneNumber", "+1 319-805-3070"));
    // This number is not a valid number so it shouldn't be reformatted.
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
            new SCIMQueryAttributes(null), null);

    final SCIMObject object = new SCIMObject();
    for (final SCIMAttribute a : attributes)
    {
      object.addAttribute(a);
    }

    final Marshaller marshaller = new XmlMarshaller();
    final OutputStream outputStream = new ByteArrayOutputStream();
    marshaller.marshal(BaseResource.BASE_RESOURCE_FACTORY.createResource(
        userResourceDescriptor, object), outputStream);
  }



  /**
   * Verify that filter mapping is working correctly.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testFilterMappings()
      throws Exception
  {
    final ResourceMapper mapper = getUserResourceMapper();

    Filter filter = mapper.toLDAPFilter(
        SCIMFilter.parse("userName eq \"test\""), null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_EQUALITY);
    assertEquals(filter.getAttributeName(), "uid");
    assertEquals(filter.getAssertionValue(), "test");

    filter = mapper.toLDAPFilter(
        SCIMFilter.parse("name.formatted lt \"test\""), null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    Filter andFilter = filter.getComponents()[0];
    assertEquals(andFilter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = andFilter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_LESS_OR_EQUAL);
    assertEquals(filter.getAttributeName(), "cn");
    assertEquals(filter.getAssertionValue(), "test");
    Filter notFilter = andFilter.getComponents()[1];
    assertEquals(notFilter.getFilterType(), Filter.FILTER_TYPE_NOT);
    filter = notFilter.getNOTComponent();
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_EQUALITY);
    assertEquals(filter.getAttributeName(), "cn");
    assertEquals(filter.getAssertionValue(), "test");

    filter = mapper.toLDAPFilter(
        SCIMFilter.parse("name.familyName ge \"test\""), null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_GREATER_OR_EQUAL);
    assertEquals(filter.getAttributeName(), "sn");
    assertEquals(filter.getAssertionValue(), "test");

    // No mapping for name.middleName so we should get match nothing
    filter = mapper.toLDAPFilter(
        SCIMFilter.parse("name.middleName eq \"test\""), null);
    assertNull(filter);

    filter = mapper.toLDAPFilter(SCIMFilter.parse("name.givenName pr"), null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_PRESENCE);
    assertEquals(filter.getAttributeName(), "givenName");
    assertNull(filter.getAssertionValue());

    filter = mapper.toLDAPFilter(SCIMFilter.parse("emails eq \"test\""), null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_EQUALITY);
    assertEquals(filter.getAttributeName(), "mail");
    assertEquals(filter.getAssertionValue(), "test");

    filter = mapper.toLDAPFilter(
        SCIMFilter.parse("phoneNumbers eq \"tel:+1-512-456-7890;ext=123\""),
        null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_OR);
    assertEquals(filter.getComponents().length, 5);
    assertEquals(filter.getComponents()[0].getAttributeName(),
        "pager");
    assertEquals(filter.getComponents()[0].getAssertionValue(),
        "+1 512-456-7890 ext. 123");
    assertEquals(filter.getComponents()[1].getAttributeName(),
        "telephoneNumber");
    assertEquals(filter.getComponents()[1].getAssertionValue(),
        "+1 512-456-7890 ext. 123");
    assertEquals(filter.getComponents()[2].getAttributeName(),
        "homePhone");
    assertEquals(filter.getComponents()[2].getAssertionValue(),
        "+1 512-456-7890 ext. 123");
    assertEquals(filter.getComponents()[3].getAttributeName(),
        "facsimileTelephoneNumber");
    assertEquals(filter.getComponents()[3].getAssertionValue(),
        "+1 512-456-7890 ext. 123");
    assertEquals(filter.getComponents()[4].getAttributeName(),
        "mobile");
    assertEquals(filter.getComponents()[4].getAssertionValue(),
        "+1 512-456-7890 ext. 123");

    try
    {
      mapper.toLDAPFilter(SCIMFilter.parse("addresses eq \"test\""), null);
      fail("The complex filter attribute 'addresses' without a sub-attribute " +
           "should cause an exception to be thrown");
    }
    catch (InvalidResourceException e)
    {
      // Expected.
    }

    filter = mapper.toLDAPFilter(
        SCIMFilter.parse("addresses.formatted eq \"test\""), null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_OR);
    assertEquals(filter.getComponents().length, 2);
    assertEquals(filter.getComponents()[0].getAttributeName(),
        "postalAddress");
    assertEquals(filter.getComponents()[0].getAssertionValue(), "test");
    assertEquals(filter.getComponents()[1].getAttributeName(),
        "homePostalAddress");
    assertEquals(filter.getComponents()[1].getAssertionValue(), "test");

    filter = mapper.toLDAPFilter(SCIMFilter.parse(
            "name.formatted eq \"test\" and userName eq \"test\""), null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    assertEquals(filter.getComponents().length, 2);

    filter = mapper.toLDAPFilter(
        SCIMFilter.parse("name.formatted eq \"test\" or userName eq \"test\""),
        null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_OR);
    assertEquals(filter.getComponents().length, 2);

    // Attributes in the filter w/o mapping and results in match nothing should
    // be correctly short-circuited when there is an AND
    filter = mapper.toLDAPFilter(
        SCIMFilter.parse(
            "name.middleName eq \"test\" and userName eq \"test\""), null);
    assertNull(filter);

    filter = mapper.toLDAPFilter(
        SCIMFilter.parse(
            "name.middleName eq \"test\" or userName eq \"test\""), null);
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_AND);
    filter = filter.getComponents()[0];
    assertEquals(filter.getFilterType(), Filter.FILTER_TYPE_OR);
    assertEquals(filter.getComponents().length, 1);
  }

  /**
   * Provider for invalid filters.
   *
   * @return A set of invalid filters.
   * @throws SCIMException if an error occurs while parsing the filter string.
   */
  @DataProvider(name = "invalidFilters")
  public Object[][] invalidFilters() throws SCIMException {
    return new Object[][] {
        {SCIMFilter.parse("name.invalid eq \"test\"")},
        {SCIMFilter.parse("invalid eq \"test\"")},
        // Addresses is a complex multi-valued attribute w/o a value mapping so
        // it should fail.
        {SCIMFilter.parse("addresses.invalid eq \"test\"")},
        {SCIMFilter.parse("name eq \"test\"")},
        {SCIMFilter.parse("name.invalid eq \"test\" or userName eq \"test\"")}
    };
  }

  /**
   * Verify that filter mapping is working correctly.
   *
   * @param filter the invalid filter to test.
   * @throws Exception  If the test fails.
   */
  @Test(dataProvider = "invalidFilters",
      expectedExceptions = InvalidResourceException.class)
  public void testInvalidFilterMappings(SCIMFilter filter)
      throws Exception
  {
    final ResourceMapper mapper = getUserResourceMapper();

    mapper.toLDAPFilter(filter, null);
  }



  /**
   * Verify that sort parameter mapping is working correctly.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testSortParameterMappings()
      throws Exception
  {
    final ResourceMapper mapper = getUserResourceMapper();

    Control control = mapper.toLDAPSortControl(
        new SortParameters("userName", "ascending"));
    ServerSideSortRequestControl sss = (ServerSideSortRequestControl) control;
    assertEquals(sss.getSortKeys().length, 1);
    assertEquals(sss.getSortKeys()[0].getAttributeName(), "uid");
    assertEquals(sss.getSortKeys()[0].getMatchingRuleID(), "2.5.13.3");
    assertEquals(sss.getSortKeys()[0].reverseOrder(), false);

    control = mapper.toLDAPSortControl(
        new SortParameters("name.formatted", "descending"));
    sss = (ServerSideSortRequestControl) control;
    assertEquals(sss.getSortKeys().length, 1);
    assertEquals(sss.getSortKeys()[0].getAttributeName(), "cn");
    assertEquals(sss.getSortKeys()[0].getMatchingRuleID(), "2.5.13.3");
    assertEquals(sss.getSortKeys()[0].reverseOrder(), true);

    try
    {
      mapper.toLDAPSortControl(new SortParameters("addresses", "ascending"));
      fail("sortBy=addresses should cause an exception to be thrown because " +
           "it is not valid");
    }
    catch (InvalidResourceException e)
    {
      // Expected.
    }

    control = mapper.toLDAPSortControl(
        new SortParameters("name.middleName", "ascending"));
    assertNull(control);

    control = mapper.toLDAPSortControl(
        new SortParameters("addresses.locality", "ascending"));
    sss = (ServerSideSortRequestControl) control;
    assertEquals(sss.getSortKeys().length, 1);
    assertEquals(sss.getSortKeys()[0].getAttributeName(), "l");

  }

  /**
   * Provider for invalid sort parameters.
   *
   * @return A set of invalid sort parameters.
   */
  @DataProvider(name = "invalidSortParameters")
  public Object[][] invalidSortParameters()  {
    return new Object[][] {
        {new SortParameters("name.invalid", "ascending")},
        {new SortParameters("invalid", "ascending")},
        // Addresses is a complex multi-valued attribute w/o a value mapping so
        // it should fail.
        {new SortParameters("addresses.invalid", "ascending")},
        {new SortParameters("name", "ascending")},
        // Addresses.formatted uses a custom transformation
        {new SortParameters("addresses.formatted", "ascending")}
    };
  }

  /**
   * Verify that sort parameter mapping is working correctly.
   *
   * @param sortParameters the invalid sort parameter.
   * @throws Exception  If the test fails.
   */
  @Test(dataProvider = "invalidSortParameters",
      expectedExceptions = InvalidResourceException.class)
  public void testInvalidSortParameterMappings(SortParameters sortParameters)
      throws Exception
  {
    final ResourceMapper mapper = getUserResourceMapper();

    mapper.toLDAPSortControl(sortParameters);
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
    List<ResourceMapper> mappers = ResourceMapper.parse(
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
