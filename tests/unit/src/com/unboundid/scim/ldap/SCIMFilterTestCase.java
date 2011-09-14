/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;



/**
 * Test coverage for the {@code SCIMFilter} class.
 */
public class SCIMFilterTestCase
    extends SCIMRITestCase
{
  /**
   * Retrieves a set of valid filter strings.
   *
   * @return  A set of valid filter strings.
   */
  @DataProvider(name = "testValidFilterStrings")
  public Object[][] getTestValidFilterStrings()
  {
    return new Object[][]
    {
        new Object[] { "userName Eq 'john'" },
        new Object[] { "Username eq 'john'" },
        new Object[] { "userName eq 'bjensen'" },
        new Object[] { "userName co 'jensen'" },
        new Object[] { "userName sw 'J'" },
        new Object[] { "title pr" },
        new Object[] { "meta.lastModified gt '2011-05-13T04:42:34Z'" },
        new Object[] { "meta.lastModified ge '2011-05-13T04:42:34Z'" },
        new Object[] { "meta.lastModified lt '2011-05-13T04:42:34Z'" },
        new Object[] { "meta.lastModified le '2011-05-13T04:42:34Z'" },
        new Object[] { " title  pr  and  userType  eq  'Employee' " },
        new Object[] { "title pr or userType eq 'Intern'" },
        new Object[] { "userType eq 'Employee' and (email co 'example.com' " +
                       "or email co 'example.org')" },
        new Object[] { "userName co '\\'\\n\\t\\\\'" },
        new Object[] { "urn:extension:members eq 25" },
        new Object[] { "urn:extension:isActive eq true" },
        new Object[] { "urn:extension:isActive eq false" },
    };
  }



  /**
   * Retrieves a set of invalid filter strings.
   *
   * @return  A set of invalid filter strings.
   */
  @DataProvider(name = "testInvalidFilterStrings")
  public Object[][] getTestInvalidFilterStrings()
  {
    return new Object[][]
    {
      new Object[] { "" },
      new Object[] { "(" },
      new Object[] { ")" },
      new Object[] { "()" },
      new Object[] { "foo" },
      new Object[] { "( title pr ) eq " },
      new Object[] { "username pr bjensen" },
      new Object[] { "meta.lastModified lte '2011-05-13T04:42:34Z'" },
      new Object[] { "username eq" },
      new Object[] { "title pr and userType eq 'Employee' eq" },
      new Object[] { "userName eq 'bjensen" },
      new Object[] { "userName eq 'bjensen\\'" },
      new Object[] { "userName eq '\\a'" },
      new Object[] { "userName eq bjensen" },
    };
  }



  /**
   * Tests the {@code parse} method with a valid filter string.
   *
   * @param  filterString  The string representation of the filter to parse.
   */
  @Test(dataProvider = "testValidFilterStrings")
  public void testParseValidFilter(final String filterString)
  {
    final SCIMFilter filter = SCIMFilter.parse(filterString);
//    System.out.println("Parse filter string: " + filterString);
//    System.out.println("Parsed filter: " + filter);
  }



  /**
   * Tests the {@code parse} method with an invalid filter string.
   *
   * @param  filterString  The string representation of the filter to parse.
   *
   * @throws Exception If the test fails.
   */
  @Test(dataProvider = "testInvalidFilterStrings")
  public void testParseInvalidFilter(final String filterString)
      throws Exception
  {
    try
    {
      SCIMFilter.parse(filterString);
      fail("Unexpected successful parse of invalid filter: " + filterString);
    }
    catch (Exception e)
    {
//      System.out.println("Parse invalid filter: " + filterString);
//      System.out.println("Error message: " + e.getMessage());
    }
  }
}
