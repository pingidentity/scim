/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim;

import com.unboundid.scim.config.SchemaManager;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
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
   * Setup the SchemaManager instance so it can be used for
   * testing.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @BeforeSuite()
  public static synchronized void setUpTestSuite()
      throws Exception
  {
    SchemaManager.init();
  }

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
