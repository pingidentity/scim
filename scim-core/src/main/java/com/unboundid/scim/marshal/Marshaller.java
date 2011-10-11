/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.marshal;


import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMException;

import java.io.OutputStream;


/**
 * This interface provides methods that may be used to write SCIM objects
 * to an external representation. There are marshaller implementations
 * for XML and JSON. Marshaller implementations are required to be thread-safe.
 */
public interface Marshaller {
  /**
   * Write a SCIM object to an output stream.
   *
   * @param resource      The SCIM resource to be written.
   * @param outputStream  The output stream to which the SCIM object should
   *                      be written.
   *
   * @throws Exception  If the object could not be written.
   */
  void marshal(BaseResource resource, OutputStream outputStream)
    throws Exception;

  /**
   * Write a SCIM listing response to an output stream.
   *
   * @param response      The SCIM response to be written.
   * @param outputStream  The output stream to which the SCIM response should
   *                      be written.
   *
   * @throws Exception  If the response could not be written.
   */
  void marshal(Resources<? extends BaseResource> response,
               OutputStream outputStream)
    throws Exception;

  /**
   * Write a SCIM error response to an output stream.
   *
   * @param response      The SCIM response to be written.
   * @param outputStream  The output stream to which the SCIM response should
   *                      be written.
   *
   * @throws Exception  If the response could not be written.
   */
  void marshal(SCIMException response, OutputStream outputStream)
    throws Exception;

}
