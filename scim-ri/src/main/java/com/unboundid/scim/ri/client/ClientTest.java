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

package com.unboundid.scim.ri.client;



import com.unboundid.scim.schema.Name;
import com.unboundid.scim.schema.PluralAttribute;
import com.unboundid.scim.schema.User;
import org.apache.wink.client.ClientResponse;

import java.net.URISyntaxException;
import java.util.Date;



/**
 * Tests SCIM operations via the Apache Wink Client.
 */
public class ClientTest
{
  private Client client;



  /**
   * Runs the client test program.
   *
   * @param args  The arguments used to initialize the test program.
   * @throws Exception  If the test program fails.
   */
  public static void main(final String[] args)
      throws Exception
  {
    if(args.length < 4 || args.length > 4)
    {
      System.out.print("Usage: client-test [host] [port] [username] " +
          "[password]");
      System.exit(1);
    }

    final String host = args[0];
    final String port = args[1];
    final String userName = args[2];
    final String password = args[3];

    final Client client =
        new Client(userName, password, host, Integer.parseInt(port));

    final ClientTest clientTest = new ClientTest(client);
    clientTest.testGetUser();
    clientTest.testCreateUser();
    clientTest.testEditUser();
    clientTest.testDeleteUser();

    System.out.println("All tests completed successfully");
    System.exit(0);
  }



  /**
   * Create a new instance of this test client.
   *
   * @param client  The SCIM wink client to be used by this test client.
   */
  public ClientTest(final Client client)
  {
    this.client = client;
  }



  /**
   * Tests creation of a new user via XML.
   *
   * @throws Exception if error creating a new user.
   */
  public void testCreateUser() throws Exception
  {
    assertTrue(createUser().getStatusCode() == 201);
  }



  /**
   * Tests updating a new user via XML.
   *
   * @throws Exception if error editing a new user.
   */
  public void testEditUser() throws Exception
  {
    String changedAttributeValue = "ASSERT_DISPLAY_NAME_CHANGED";
    User user = getUserFromResponse(createUser());
    user.setDisplayName(changedAttributeValue);
    final ClientResponse response = client.editUser(user);
    assertTrue(response.getStatusCode() == 200);

    User userFromResponse = getUserFromResponse(response);
    assertEquals(userFromResponse.getDisplayName(), changedAttributeValue);
  }



  /**
   * Tests updating a new user via XML.
   *
   * @throws Exception if error editing a new user.
   */
  public void testDeleteUser() throws Exception
  {
    User user = this.getUserFromResponse(createUser());
    String id = user.getId();
    ClientResponse clientResponse = client.deleteUser(id);
    assertTrue(clientResponse.getStatusCode() == 200);
  }


  /**
   * Tests user retrieval via XML.
   *
   * @throws Exception if error fetching the User.
   */
  public void testGetUser() throws Exception
  {
    final User user = getUserFromResponse(createUser());
    final ClientResponse clientResponse = client.getUser(user.getId());
    assertTrue(clientResponse.getStatusCode() == 200);
    final User userFromResponse = getUserFromResponse(clientResponse);
    assertNotNull(userFromResponse);
  }



  /**
   * Creates a new SCIM User.
   *
   * @return The newly created SCIM User.
   * @throws URISyntaxException If error parsing the Service Provider endpoint.
   */
  private ClientResponse createUser()
      throws URISyntaxException
  {
    return client.createUser(getTemplateUser());
  }



  /**
   * Pulls the User POJO from the Service Provider response.
   *
   * @param response The Service Provider response.
   * @return The SCIM User POJO.
   */
  private User getUserFromResponse(final ClientResponse response)
  {
    User user = response.getEntity(User.class);
    assertNotNull(user);
    return user;
  }



  /**
   * Throw a runtime exception if the provided condition is not true.
   * @param condition  The condition which is expected to be true.
   */
  private static void assertTrue(final boolean condition)
  {
    if (!condition)
    {
      throw new RuntimeException("assertTrue assertion failed");
    }
  }



  /**
   * Throw a runtime exception if the actual and expected objects are not equal.
   * @param o1  The actual value.
   * @param o2  The expected value.
   */
  private static void assertEquals(final Object o1, final Object o2)
  {
    if (!o1.equals(o2))
    {
      throw new RuntimeException("assertEquals assertion failed");
    }
  }



  /**
   * Throw a runtime exception if the provided value is {@code null}.
   * @param o  The object which is not expected to be null.
   */
  private static void assertNotNull(final Object o)
  {
    if (o == null)
    {
      throw new RuntimeException("assertNotNull assertion failed");
    }
  }



  /**
   * Creates a POJO representing the canonical SCIM User 'Babs'.
   *
   * @return The templated User.
   */
  private static User getTemplateUser()
  {
    // create new user
    final User user = new User();

    // make the user unique enough
    user.setUserName("bjensen" + new Date().getTime());
    user.setExternalId(user.getUserName());
    user.setDisplayName("Ms. Barbara J Jensen III");
    final Name name = new Name();
    name.setFamilyName("Jensen");
    name.setGivenName("Barbara");
    name.setFormatted("Ms. Barbara J Jensen III");
    user.setName(name);

    final User.Emails emails = new User.Emails();
    final PluralAttribute email = new PluralAttribute();
    email.setValue("bjensen@example.com");
    emails.getEmail().add(email);
    user.setEmails(emails);

    return user;
  }
}
