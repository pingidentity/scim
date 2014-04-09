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

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.Address;
import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.SCIMTestCase;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class provides test coverage for the {@link JsonMarshaller}.
 */
@Test
public class MarshallerTestCase
  extends SCIMTestCase
{
  /**
   * Verify that a valid user can be written to JSON and then read back.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = true)
  public void testMarshal()
    throws Exception
  {
    final File testJSON = File.createTempFile("test-", ".json");
    testJSON.deleteOnExit();

    final UserResource user1 = new UserResource(CoreSchema.USER_DESCRIPTOR);

    user1.setId("uid=bjensen,dc=example,dc=com");

    user1.setUserName("bjensen");

    user1.setName(new Name("Ms. Barbara J Jensen III",
        "Jensen", "Barbara", "J", "Ms.", "III"));
    Collection<Entry<String>> emails = new ArrayList<Entry<String>>(2);
    emails.add(new Entry<String>("bjensen@example.com", "work", true));
    emails.add(new Entry<String>("babs@jensen.org", "home", false));
    user1.setEmails(emails);

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
    user1.setAddresses(addresses);

    Collection<Entry<String>> phoneNumbers = new ArrayList<Entry<String>>(2);
    phoneNumbers.add(new Entry<String>("800-864-8377", "work", false));
    phoneNumbers.add(new Entry<String>("818-123-4567", "mobile", false));
    user1.setPhoneNumbers(phoneNumbers);

    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION,
        "employeeNumber", AttributeValueResolver.STRING_RESOLVER,"1001");
    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION,
        "organization", AttributeValueResolver.STRING_RESOLVER,"Example");
    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION,
        "division", AttributeValueResolver.STRING_RESOLVER,"People");
    user1.setSingularAttributeValue(SCHEMA_URI_ENTERPRISE_EXTENSION,
        "department", AttributeValueResolver.STRING_RESOLVER,"Sales");

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final Marshaller marshaller = new JsonMarshaller();
    marshaller.marshal(user1, outputStream);
    outputStream.close();
    InputStream inputStream =
        new ByteArrayInputStream(outputStream.toByteArray());
    final Unmarshaller unmarshaller = new JsonUnmarshaller();
    final BaseResource resource = unmarshaller.unmarshal(inputStream,
        CoreSchema.USER_DESCRIPTOR, BaseResource.BASE_RESOURCE_FACTORY);
    final SCIMObject user2 = resource.getScimObject();
    inputStream.close();

    assertEquals(user1, resource);
    assertEquals(resource.getResourceDescriptor(), CoreSchema.USER_DESCRIPTOR);
    for (final String attribute : Arrays.asList("id",
                                                "addresses",
                                                "phoneNumbers",
                                                "emails",
                                                "name"))
    {
      assertTrue(user2.hasAttribute(SCHEMA_URI_CORE, attribute));
    }

    for (final String attribute : Arrays.asList("employeeNumber",
                                                "organization",
                                                "division",
                                                "department"))
    {
      assertTrue(user2.hasAttribute(SCHEMA_URI_ENTERPRISE_EXTENSION,
                                    attribute));
    }
  }
}
