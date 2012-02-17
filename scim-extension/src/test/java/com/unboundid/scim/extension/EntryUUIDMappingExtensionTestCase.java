/*
 * Copyright 2011-2012 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;



/**
 * Tests the extension with the resource ID mapped to the LDAP entryUUID
 * attribute.
 */
public class EntryUUIDMappingExtensionTestCase extends SCIMExtensionTestCase
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
                 getFile("resource/resources-entryUUID.xml"),
        "--set", "extension-argument:contextPath=/",
        "--set", "extension-argument:debugEnabled"
    );
  }



  /**
   * {@inheritDoc}
   */
  @Test
  public void testConfigurationChange()
      throws Exception
  {
    // The inherited test assumes the default resources.xml configuration file.
    // There is no need to have the same test repeated here anyway.
  }
}
