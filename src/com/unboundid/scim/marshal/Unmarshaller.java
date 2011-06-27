/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshal;

import com.unboundid.scim.sdk.SCIMObject;

import java.io.File;
import java.io.InputStream;



/**
 * This interface provides methods that may be used to read SCIM objects
 * from their external representation. There are un-marshaller implementations
 * for XML and JSON. Un-marshaller implementations are required to be
 * thread-safe.
 */
public interface Unmarshaller {
  /**
   * Reads a SCIM object from a file.
   *
   * @param file  The file containing the SCIM object to be read.
   *
   * @return  The SCIM object that was read.
   *
   * @throws Exception  If the object could not be read.
   */
  SCIMObject unmarshal(File file) throws Exception;

  /**
   * Reads a SCIM object from an input stream.
   *
   * @param inputStream  The input stream containing the SCIM object to be read.
   *
   * @return  The SCIM object that was read.
   *
   * @throws Exception  If the object could not be read.
   */
  SCIMObject unmarshal(InputStream inputStream) throws Exception;
}
