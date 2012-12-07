/*
 * Copyright 2011-2012 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;



/**
 * Tests the extension with the resource ID mapped to the LDAP DN (instead of
 * the entryUUID attribute).
 *
 * NOTE: The SCIM-Extension module is being deprecated and moved into the core
 *       build so please do not add new test cases in this class. Instead, add
 *       them to the SCIMServerTestCase class in the SCIM-RI module. If the test
 *       case will only work with a full Directory Server, please add them to
 *       the SCIMHTTPServletExtensionTest class in the core build.
 */
@Test(enabled = false)
public class DNMappingExtensionTestCase extends SCIMExtensionTestCase
{
  /**
   * Obtain a reference to a file anywhere under the scim root directory.
   *
   * @param path  The relative path to the desired file.
   *
   * @return  A reference to the desired file.
   */
  protected static File getFile(final String path)
  {
    final File baseDir = new File(System.getProperty("main.basedir"));
    return new File(baseDir, path);
  }



  @BeforeClass
  @Override
  public void setup() throws Exception
  {
    super.setup();

    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--set", "extension-argument:" +
                 "resourceMappingFile=" +
                 getFile("resource/resources-entryDN.xml"),
        "--set", "extension-argument:contextPath=/",
        "--set", "extension-argument:debugEnabled"
    );
  }



  /**
   * {@inheritDoc}
   */
  @Test(enabled = false)
  public void testConfigurationChange()
      throws Exception
  {
    // The inherited test assumes the default resources.xml configuration file.
    // There is no need to have the same test repeated here anyway.
  }
}
