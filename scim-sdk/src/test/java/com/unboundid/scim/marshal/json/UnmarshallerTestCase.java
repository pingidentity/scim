/*
 * Copyright 2011 UnboundID Corp.
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

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.SCIMTestCase;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNotNull;

import java.io.InputStream;


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
        CoreSchema.USER_DESCRIPTOR;
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
