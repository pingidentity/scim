/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.SCIMTestCase;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNotNull;

import java.io.InputStream;

import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_NAME_USER;



@Test
public class UnmarshallerTestCase extends SCIMTestCase {
  /**
   * Verify that a known valid user can be read from JSON.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = true)
  public void testUnmarshal() throws Exception {
    final ResourceDescriptor userResourceDescriptor =
        SchemaManager.instance().getResourceDescriptor(RESOURCE_NAME_USER);
    final InputStream testJson =
        getResource("/com/unboundid/scim/marshal/spec/core-user.json");
    final Context context = Context.instance();
    final Unmarshaller unmarshaller = context.unmarshaller(Context.Format
      .Json);
    final SCIMObject o = unmarshaller.unmarshal(
        testJson, userResourceDescriptor,
        BaseResource.BASE_RESOURCE_FACTORY).getScimObject();
    // weak need todo a deep assert
    assertNotNull(o);
  }
}
