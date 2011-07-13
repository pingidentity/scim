/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap.marshal.json;

import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMRITestCase;
import org.testng.annotations.Test;

import java.io.File;

@Test
public class UnmarshallerTestCase extends SCIMRITestCase {
  /**
   * Verify that a known valid user can be read from JSON.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = true)
  public void testUnmarshal() throws Exception {
    final File testJson = getTestResource("marshal/spec/core-user.json");
    final Context context = Context.instance();
    final Unmarshaller unmarshaller = context.unmarshaller(Context.Format
      .Json);
    final SCIMObject o = unmarshaller.unmarshal(testJson);
    // weak need todo a deep assert
    assertNotNull(o);
  }
}
