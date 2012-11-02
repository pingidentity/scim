/*
 * Copyright 2012 UnboundID Corp.
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

package com.unboundid.scim.ri;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * Tests deleting all values of a required multi-valued attribute using PATCH
 * results in error.
 */
public class RequiredMultivaluedPatchTestCase extends SCIMRITestCase
{
  /**
   * Set up the test class to use an alternative resource mapping.
   */
  @BeforeClass
  public void setUp()
  {
    File f = getFile(
        "scim-ri/src/test/resources/resources-emails-required.xml");
    assertTrue(f.exists());
    SCIMServerConfig config = new SCIMServerConfig();
    config.setResourcesFile(f);
    try
    {
      reconfigureTestSuite(config);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Retrieve the user resource that has the provided userName value.
   *
   * @param userName  The userName of the user to be retrieved.
   *
   * @return  The user resource.
   *
   * @throws SCIMException  If the resource could not be retrieved.
   */
  private UserResource getUser(final String userName)
      throws SCIMException
  {
    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();

    final Resources<UserResource> resources =
        endpoint.query("userName eq \"" + userName + "\"");

    return resources.iterator().next();
  }

  /**
   * Tests deleting all values of a required multi-valued attribute using PATCH
   * results in error.
   *
   * @throws Exception If the test fails.
   */
  @Test
  public void testPatchUser()
      throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));

    //Add an entry to the Directory
    testDS.add("dn: uid=testModifyWithPatch," + userBaseDN,
        "objectclass: top",
        "objectclass: person",
        "objectclass: organizationalPerson",
        "objectclass: inetOrgPerson",
        "uid: testModifyWithPatch",
        "userPassword: oldPassword",
        "cn: testModifyWithPatch",
        "givenname: Test",
        "sn: User",
        "telephoneNumber: 512-123-4567",
        "homePhone: 972-987-6543",
        "mail: testEmail.1@example.com",
        "mail: testEmail.2@example.com");

    //Update the entry via SCIM
    UserResource user = getUser("testModifyWithPatch");
    assertNotNull(user);

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    SCIMAttributeValue value =
        SCIMAttributeValue.createStringValue("testEmail.1@example.com");
    SCIMAttribute email1Value = SCIMAttribute.create(
        CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "emails").getSubAttribute("value"), value);

    value = SCIMAttributeValue.createStringValue("delete");
    SCIMAttribute email1Operation = SCIMAttribute.create(
        CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "emails").getSubAttribute("operation"), value);

    SCIMAttributeValue email1 = SCIMAttributeValue.createComplexValue(
        email1Value, email1Operation);

    SCIMAttribute emails = SCIMAttribute.create(
        CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "emails"), email1);

    List<SCIMAttribute> attrsToUpdate = Collections.singletonList(emails);

    // This should work fine as we are deleting just one value.
    userEndpoint.update(user.getId(), null, attrsToUpdate, null);

    value = SCIMAttributeValue.createStringValue("testEmail.2@example.com");
    SCIMAttribute email2Value = SCIMAttribute.create(
        CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "emails").getSubAttribute("value"), value);

    value = SCIMAttributeValue.createStringValue("delete");
    SCIMAttribute email2Operation = SCIMAttribute.create(
        CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "emails").getSubAttribute("operation"), value);

    SCIMAttributeValue email2 = SCIMAttributeValue.createComplexValue(
        email2Value, email2Operation);

    emails = SCIMAttribute.create(
        CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
            "emails"), email2);

    attrsToUpdate = Collections.singletonList(emails);

    try
    {
      userEndpoint.update(user.getId(), null, attrsToUpdate, null);
      fail("Expected a 400 response when trying to patch user by " +
          "deleting all values of a required multi-valued attr");
    }
    catch (InvalidResourceException e)
    {
      // expected (error code is 400)
    }
  }
}
