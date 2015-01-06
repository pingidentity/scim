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

package com.unboundid.scim;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
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
   * Read in a standard test user from a test resource file.
   *
   * @return  The test user.
   *
   * @throws Exception  If the file could not be read.
   */
  protected static BaseResource getTestUser()
      throws Exception
  {
    final InputStream testXML =
        getResource("/com/unboundid/scim/marshal/spec/core-user.xml");

    final ResourceDescriptor userResourceDescriptor =
        CoreSchema.USER_DESCRIPTOR;
    final Unmarshaller unmarshaller = new XmlUnmarshaller();
    return unmarshaller.unmarshal(testXML,
        userResourceDescriptor, BaseResource.BASE_RESOURCE_FACTORY);
  }



}
