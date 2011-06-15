/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshall;

import com.unboundid.scim.marshall.xml.XmlMarshaller;
import com.unboundid.scim.marshall.xml.XmlUnmarshaller;



/**
 * This class provides a factory for SCIM object marshaller and un-marshaller
 * instances. Currently a singleton instance creates a marshaller and
 * un-marshaller for XML.
 */
public class Context {
  /**
   * The default schema URN (the SCIM core schema URN).
   */
  public static final String DEFAULT_SCHEMA_URN = "urn:scim:schemas:core:1.0";

  /**
   * The namespace label associated with the default schema.
   */
  public static final String DEFAULT_SCHEMA_PREFIX = "scim";

  /**
   * The singleton instance that can create a marshaller and un-marshaller for
   * XML.
   */
  private static final Context INSTANCE = new Context();



  /**
   * Retrieve a context instance that can create a marshaller and un-marshaller
   * for XML.
   *
   * @return  A context instance that can create a marshaller and un-marshaller
   *          for XML.
   */
  public static Context instance() {
    return INSTANCE;
  }



  /**
   * Retrieve a SCIM object marshaller.
   *
   * @return  A SCIM object marshaller.
   */
  public Marshaller marshaller() {
    return new XmlMarshaller();
  }

  /**
   * Retrieve a SCIM object un-marshaller.
   *
   * @return  A SCIM object un-marshaller.
   */
  public Unmarshaller unmarshaller() {
    return new XmlUnmarshaller();
  }

}
