/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.marshall.xml;

import com.unboundid.scim.marshall.Context;
import com.unboundid.scim.marshall.Unmarshaller;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;

import java.io.File;



/**
 * This clas provides test coverage for the {@link XmlUnmarshaller}.
 */
public class UnmarshallerTestCase
    extends SCIMRITestCase
{
  /**
   * Verify that a known valid user can be read from XML.
   *
   * @throws Exception  If the test fails.
   */
  @Test(enabled=false)
  public void testUnmarshal()
      throws Exception
  {
    final File testXML = getTestResource("marshal/core-user.xml");

    final String coreSchema = SCIMConstants.SCHEMA_URI_CORE;
    final Context context = Context.instance();
    final Unmarshaller unmarshaller = context.unmarshaller();
    final SCIMObject o = unmarshaller.unmarshal(testXML);

    assertTrue(o.hasAttribute(coreSchema, "id"));
    assertTrue(o.hasAttribute(coreSchema, "addresses"));
    assertTrue(o.hasAttribute(coreSchema, "phoneNumbers"));
    assertTrue(o.hasAttribute(coreSchema, "emails"));
    assertTrue(o.hasAttribute(coreSchema, "name"));
    assertNotNull(o.getResourceType());
    assertEquals(o.getResourceType(), "user");
  }
}
