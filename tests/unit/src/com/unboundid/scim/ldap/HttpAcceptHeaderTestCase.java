/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;



/**
 * This class provides test coverage for the {@link HttpAcceptHeader} class.
 */
public class HttpAcceptHeaderTestCase extends SCIMRITestCase
{
  /**
   * Verify that the best media type match can be chosen according to the
   * accept header.
   */
  @Test
  public void testBestMatch()
  {
    final HttpAcceptHeader acceptHeader =
        HttpAcceptHeader.parse(
            "text/*;q=0.3, text/html;q=0.7, text/html;level=1,  " +
            "text/html;level=2;q=0.4, */*;q=0.5");

    assertEquals(acceptHeader.findBestMatch("text/plain", "text/html"),
                 "text/html");
    assertEquals(acceptHeader.findBestMatch("text/html", "text/plain"),
                 "text/html");
    assertEquals(acceptHeader.findBestMatch("text/plain", "image/jpeg"),
                 "image/jpeg");
    assertEquals(acceptHeader.findBestMatch("image/jpeg", "text/plain"),
                 "image/jpeg");
    assertEquals(acceptHeader.findBestMatch("text/plain", "application/xml"),
                 "application/xml");
    assertEquals(acceptHeader.findBestMatch("application/xml", "text/plain"),
                 "application/xml");
    assertEquals(acceptHeader.findBestMatch("application/xml", "text/html"),
                 "text/html");
    assertEquals(acceptHeader.findBestMatch("text/html", "application/xml"),
                 "text/html");
  }
}
