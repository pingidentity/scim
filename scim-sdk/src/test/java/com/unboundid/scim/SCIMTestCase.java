/*
 * Copyright 2011-2012 UnboundID Corp.
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

package com.unboundid.scim;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.Test;

import java.io.File;
import java.io.InputStream;

/**
 * This class provides the superclass for all SCIM test cases.
 */
@Test(sequential=true)
public class SCIMTestCase
{
  /**
   * Obtain a reference to a file in the test resources.
   *
   * @param fileName  The name of the desired file.
   *
   * @return  A reference to the desired test resource file.
   */
  protected static InputStream getResource(final String fileName)
  {
    return SCIMTestCase.class.getResourceAsStream(fileName);
  }

  /**
   * Obtain a reference to a file in the test resources.
   *
   * @param fileName  The name of the desired file.
   *
   * @return  A reference to the desired test resource file.
   */
  protected static File getResourceFile(final String fileName)
  {
    return new File(SCIMTestCase.class.getResource(fileName).getFile());
  }

  /**
   * Clean up after the test suite.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @AfterSuite
  public static void cleanUpTestSuite()
      throws Exception
  {
    // Nothing needed by default
  }
}
