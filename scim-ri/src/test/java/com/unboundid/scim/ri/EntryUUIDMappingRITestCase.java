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

import org.testng.annotations.BeforeClass;



/**
 * Tests the server with the resource ID mapped to the LDAP entryUUID attribute.
 */
public class EntryUUIDMappingRITestCase extends SCIMServerTestCase
{
  /**
   * Set up the test class to use an alternative resource mapping.
   *
   * @throws Exception  If the test class cannot be set up.
   */
  @BeforeClass
  public void setup() throws Exception
  {
    reconfigureTestSuite(getFile("resource/resources-entryUUID.xml"));
  }
}
