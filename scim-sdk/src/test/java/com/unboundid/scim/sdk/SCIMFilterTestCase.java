/*
 * Copyright 2011-2024 Ping Identity Corporation
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

package com.unboundid.scim.sdk;

import com.unboundid.scim.SCIMTestCase;
import com.unboundid.scim.schema.AttributeDescriptor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;



/**
 * Test coverage for the {@code SCIMFilter} class.
 */
public class SCIMFilterTestCase
    extends SCIMTestCase
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
        new Object[] { "userName Eq \"john\"" },
        new Object[] { "Username eq \"john\"" },
        new Object[] { "userName eq \"bjensen\"" },
        new Object[] { "userName co \"jensen\"" },
        new Object[] { "userName sw \"J\"" },
        new Object[] { "title pr" },
        new Object[] { "meta.lastModified gt \"2011-05-13T04:42:34Z\"" },
        new Object[] { "meta.lastModified ge \"2011-05-13T04:42:34Z\"" },
        new Object[] { "meta.lastModified lt \"2011-05-13T04:42:34Z\"" },
        new Object[] { "meta.lastModified le \"2011-05-13T04:42:34Z\"" },
        new Object[] { " title  pr  and  userType  eq  \"Employee\" " },
        new Object[] { "title pr or userType eq \"Intern\"" },
        new Object[] { "userType eq \"Employee\" and " +
                       "(email co \"example.com\" " +
                       "or email co \"example.org\")" },
        new Object[] { "userName co \"\\ufe00\\\"\\n\\t\\\\\"" },
        new Object[] { "urn:extension:members eq 25" },
        new Object[] { "urn:extension:members eq 25.52" },
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
      new Object[] { "username pr \"bjensen\"" },
      new Object[] { "meta.lastModified lte \"2011-05-13T04:42:34Z\"" },
      new Object[] { "username eq" },
      new Object[] { "title pr and userType eq \"Employee\" eq" },
      new Object[] { "userName eq 'bjensen'" },
      new Object[] { "userName eq \"bjensen" },
      new Object[] { "userName eq \"bjensen\\" },
      new Object[] { "userName eq \"\\a\"" },
      new Object[] { "userName eq bjensen" },
      new Object[] { "userName co \"\\ufe\" or userName co \"a\""},
    };
  }



  /**
   * Tests the {@code parse} method with a valid filter string.
   *
   * @param  filterString  The string representation of the filter to parse.
   *
   * @throws Exception  If the test fails.
   */
  @Test(dataProvider = "testValidFilterStrings")
  public void testParseValidFilter(final String filterString)
      throws Exception
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



  /**
   * Tests that the {@code parse} method observes operator precedence rules.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testOperatorPrecedence()
      throws Exception
  {
    SCIMFilter filter =
        SCIMFilter.parse("title pr and email pr or userType pr");
    assertEquals(filter.getFilterType(), SCIMFilterType.OR);
    assertEquals(filter.getFilterComponents().get(0).getFilterType(),
                 SCIMFilterType.AND);

    filter =
        SCIMFilter.parse("title pr or email pr and userType pr");
    assertEquals(filter.getFilterType(), SCIMFilterType.OR);
    assertEquals(filter.getFilterComponents().get(1).getFilterType(),
                 SCIMFilterType.AND);

    filter =
        SCIMFilter.parse("title pr and (email pr or userType pr)");
    assertEquals(filter.getFilterType(), SCIMFilterType.AND);
    assertEquals(filter.getFilterComponents().get(1).getFilterType(),
                 SCIMFilterType.OR);

    filter =
        SCIMFilter.parse("(title pr or email pr) and userType pr");
    assertEquals(filter.getFilterType(), SCIMFilterType.AND);
    assertEquals(filter.getFilterComponents().get(0).getFilterType(),
                 SCIMFilterType.OR);
  }

  /**
   * Test that filters with binary values work when type is eq, sw, or co.
   */
  @Test
  public void testBinaryFilterValue()
  {
    AttributeDescriptor testAttr = AttributeDescriptor.createAttribute("test",
        AttributeDescriptor.DataType.BINARY, null,
        SCIMConstants.SCHEMA_URI_CORE, false, false, false);

    SCIMAttribute attribute = SCIMAttribute.create(testAttr,
        SCIMAttributeValue.createBinaryValue("abcdefghijk".getBytes()));

    SimpleValue v = new SimpleValue("abcdefghijk".getBytes());
    SCIMFilter filter = new SCIMFilter(SCIMFilterType.EQUALITY,
        AttributePath.parse("test"), v.getStringValue(), true, null);

    assertTrue(attribute.matchesFilter(filter));

    v = new SimpleValue("abcdefgijk".getBytes());
    filter = new SCIMFilter(SCIMFilterType.EQUALITY,
        AttributePath.parse("test"), v.getStringValue(), true, null);

    assertFalse(attribute.matchesFilter(filter));

    v = new SimpleValue("abcdef".getBytes());
        filter = new SCIMFilter(SCIMFilterType.STARTS_WITH,
            AttributePath.parse("test"), v.getStringValue(), true, null);

        assertTrue(attribute.matchesFilter(filter));

    v = new SimpleValue("bcdef".getBytes());
    filter = new SCIMFilter(SCIMFilterType.STARTS_WITH,
        AttributePath.parse("test"), v.getStringValue(), true, null);

    assertFalse(attribute.matchesFilter(filter));

    v = new SimpleValue("abcdef".getBytes());
        filter = new SCIMFilter(SCIMFilterType.CONTAINS,
            AttributePath.parse("test"), v.getStringValue(), true, null);

        assertTrue(attribute.matchesFilter(filter));

    v = new SimpleValue("bcdef".getBytes());
    filter = new SCIMFilter(SCIMFilterType.CONTAINS,
        AttributePath.parse("test"), v.getStringValue(), true, null);

    assertTrue(attribute.matchesFilter(filter));

    v = new SimpleValue("hijkl".getBytes());
    filter = new SCIMFilter(SCIMFilterType.CONTAINS,
        AttributePath.parse("test"), v.getStringValue(), true, null);

    assertFalse(attribute.matchesFilter(filter));
  }

  /**
   * Test that isQuoteFilterValue returns the correct values.
   *
   * @throws Exception if an exception occurred parsing a filter.
   */
  @Test
  public void testIsQuoteFilterValue() throws Exception
  {
    SCIMFilter filter;

    filter = SCIMFilter.parse("foo eq \"quoted-value\"");
    assertTrue(filter.isQuoteFilterValue());

    filter = SCIMFilter.parse("foo eq 123");
    assertFalse(filter.isQuoteFilterValue());

    filter = SCIMFilter.parse("foo eq \"123\"");
    assertTrue(filter.isQuoteFilterValue());

    filter = SCIMFilter.parse("foo eq false");
    assertFalse(filter.isQuoteFilterValue());

    filter = SCIMFilter.parse(
        "myattribute eq \"quoted-value\" and foo eq false");

    assertFalse(filter.isQuoteFilterValue());
  }

}
