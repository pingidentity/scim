/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshall;


import com.unboundid.scim.sdk.SCIMObject;

import java.io.Writer;

public interface Marshaller {

  public void marshall(SCIMObject o, java.io.OutputStream outputStream)
    throws Exception;

  public void marshall(SCIMObject o, java.io.File file) throws Exception;

  public void marshall(SCIMObject o, Writer writer) throws Exception;

}
