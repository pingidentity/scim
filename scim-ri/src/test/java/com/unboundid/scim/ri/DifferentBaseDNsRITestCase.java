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

package com.unboundid.scim.ri;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.SCIMEndpoint;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Verifies that we support Users and Groups with different base DNs.
 */
public class DifferentBaseDNsRITestCase extends SCIMRITestCase
{
  /**
   * Set up the test class to use an alternative resource mapping.
   *
   * @throws Exception  If the test class cannot be set up.
   */
  @BeforeClass
  public void setUp() throws Exception
  {
    File f = getFile(
       "scim-ri/src/test/resources/resources-different-base-dns.xml");
    assertTrue(f.exists());
    SCIMServerConfig config = new SCIMServerConfig();
    config.setResourcesFile(f);
    reconfigureTestSuite(config);
  }

  /**
   * Verifies that we support Users and Groups with different base DNs and that
   * we can still search for a Group and get all of its members (even though
   * they're in a different part of the DIT), and that we can search for a user
   * and get all of its groups (even though they're in a different part of the
   * DIT).
   * @throws Exception  If the test failed.
   */
  @Test
  public void testDifferentBaseDNs() throws Exception
  {
    // Get a reference to the in-memory test DS.
    final InMemoryDirectoryServer testDS = getTestDS();
    testDS.add(generateDomainEntry("example", "dc=com"));
    testDS.add(generateOrgUnitEntry("people", "dc=example,dc=com"));
    testDS.add(generateOrgEntry("example.com", null));

    // Create some users under dc=example,dc=com
    UserResource user1 = new UserResource(CoreSchema.USER_DESCRIPTOR);
    Name name1 = new Name("Ms. Barbara J Jensen III", "Jensen", "J",
        "Barbara", "Ms", "III");
    user1.setUserName("bjensen");
    user1.setName(name1);
    user1.setPassword("4wrj2U81j");

    UserResource user2 = new UserResource(CoreSchema.USER_DESCRIPTOR);
    Name name2 = new Name("Mr. John D. Smith", "Smith", "D",
        "John", "Mr", null);
    user2.setUserName("jsmith");
    user2.setName(name2);
    user2.setPassword("Rls3xj2%4");

    // Post the users via SCIM, returning selected attributes.
    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();
    user1 = endpoint.create(user1, "id", "meta");
    user2 = endpoint.create(user2, "id", "meta");

    //Verify the users were created under ou=people,dc=example,dc=com
    assertEquals(
            DN.getParent(user1.getId()), new DN(userBaseDN));
    assertEquals(
            DN.getParent(user2.getId()), new DN(userBaseDN));

    //Create a group containing both users
    SCIMEndpoint<GroupResource> grpEndpoint = service.getGroupEndpoint();
    GroupResource group1 = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    group1.setDisplayName("group1");
    Set<Entry<String>> members1 =
          new HashSet<Entry<String>>();
    members1.add(new com.unboundid.scim.data.Entry<String>(
                              user1.getId(), "User", false));
    members1.add(new com.unboundid.scim.data.Entry<String>(
                              user2.getId(), "User", false));
    group1.setMembers(members1);
    group1 = grpEndpoint.create(group1, "id", "meta", "members");

    //Verify that the group was created under o=example.com and contains
    //both members
    assertEquals(DN.getParent(group1.getId()), new DN("o=example.com"));
    assertEquals(group1.getMembers().size(), 2);

    //Verify that the members have the correct base DN
    for(com.unboundid.scim.data.Entry<String> entry : group1.getMembers())
    {
      DN dn = new DN(entry.getValue());
      assertTrue(dn.isDescendantOf(userBaseDN, false));
    }

    //Create another group under o=example.com
    GroupResource group2 = new GroupResource(CoreSchema.GROUP_DESCRIPTOR);
    group2.setDisplayName("group2");
    Set<com.unboundid.scim.data.Entry<String>> members2 =
          new HashSet<com.unboundid.scim.data.Entry<String>>();
    members2.add(new com.unboundid.scim.data.Entry<String>(
                                user1.getId(), "User", false));
    members2.add(new com.unboundid.scim.data.Entry<String>(
                                group1.getId(), "Group", false));
    group2.setMembers(members2);
    group2 = grpEndpoint.create(group2, "id", "meta", "members");

    //Verify that the group was created under o=example.com and contains
    //both members
    assertEquals(DN.getParent(group2.getId()), new DN("o=example.com"));
    assertEquals(group2.getMembers().size(), 2);
    for(com.unboundid.scim.data.Entry<String> entry : group2.getMembers())
    {
      DN dn = new DN(entry.getValue());
      if(entry.getType().equalsIgnoreCase("user"))
      {
        assertTrue(dn.isDescendantOf(userBaseDN, false));
      }
      else if(entry.getType().equalsIgnoreCase("group"))
      {
        assertTrue(dn.isDescendantOf("o=example.com", false));
      }
      else
      {
        fail("Unknown base DN for group member: " + dn);
      }
    }

    //Verify that user1 correctly identifies its two groups which should
    //both be direct.
    user1 = endpoint.get(user1.getId(), null, "id", "meta", "groups");
    assertEquals(user1.getGroups().size(), 2);
    for(com.unboundid.scim.data.Entry<String> entry : user1.getGroups())
    {
      DN dn = new DN(entry.getValue());
      assertTrue(dn.isDescendantOf("o=example.com", false));
      assertEquals(entry.getType(), "direct");
    }

    //Verify that user2 correctly identifies its two groups which should
    //both be direct.
    user2 = endpoint.get(user2.getId(), null, "id", "meta", "groups");
    assertEquals(user2.getGroups().size(), 2);
    for(com.unboundid.scim.data.Entry<String> entry : user2.getGroups())
    {
      DN dn = new DN(entry.getValue());
      assertTrue(dn.isDescendantOf("o=example.com", false));
      if(entry.getDisplay().equals("group1"))
      {
        assertEquals(entry.getType(), "direct");
      }
      else if(entry.getDisplay().equals("group2"))
      {
        assertEquals(entry.getType(), "indirect");
      }
    }
  }
}
