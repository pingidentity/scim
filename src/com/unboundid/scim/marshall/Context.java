/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshall;

import com.unboundid.scim.marshall.xml.XmlMarshaller;
import com.unboundid.scim.marshall.xml.XmlUnmarshaller;

public class Context {
  public static final String DEFAULT_SCHEMA_URN = "urn:scim:schemas:core:1.0";
  public static final String DEFAULT_SCHEMA_PREFIX = "scim";

  private final static Context INSTANCE = new Context();

  public static Context instance() {
    return INSTANCE;
  }

  public Marshaller marshaller() {
    return new XmlMarshaller();
  }

  public Unmarshaller unmarshaller() {
    return new XmlUnmarshaller();
  }

}
