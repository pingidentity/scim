/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshall;

import com.unboundid.scim.sdk.SCIMObject;

public interface Unmarshaller {
  SCIMObject unmarshall(java.io.File file) throws Exception;

  SCIMObject unmarshall(java.io.InputStream inputStream) throws Exception;
}
