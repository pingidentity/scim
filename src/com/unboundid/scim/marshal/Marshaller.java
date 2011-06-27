/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshal;


import com.unboundid.scim.sdk.SCIMObject;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;



/**
 * This interface provides methods that may be used to write SCIM objects
 * to an external representation. There are marshaller implementations
 * for XML and JSON. Marshaller implementations are required to be thread-safe.
 */
public interface Marshaller {

  /**
   * Write a SCIM object to a file.
   *
   * @param object  The SCIM object to be written.
   * @param file    The file to which the SCIM object should be written.
   *
   * @throws Exception  If the object could not be written.
   */
  void marshal(SCIMObject object, File file) throws Exception;

  /**
   * Write a SCIM object to an output stream.
   *
   * @param object        The SCIM object to be written.
   * @param outputStream  The output stream to which the SCIM object should
   *                      be written.
   *
   * @throws Exception  If the object could not be written.
   */
  void marshal(SCIMObject object, OutputStream outputStream)
    throws Exception;

  /**
   * Write a SCIM object to a character stream writer.
   *
   * @param object  The SCIM object to be written.
   * @param writer  The character stream writer to which the SCIM object should
   *                be written.
   *
   * @throws Exception  If the object could not be written.
   */
  void marshal(SCIMObject object, Writer writer) throws Exception;

}
