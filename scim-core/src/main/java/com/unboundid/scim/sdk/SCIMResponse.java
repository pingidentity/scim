/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;


import com.unboundid.scim.marshal.Marshaller;

import java.io.OutputStream;

/**
 * This class represents the response to a SCIM request.
 */
public interface SCIMResponse
{
  /**
   * Marshals this response using the specified <code>Marshaller</code> to the
   * specified <code>OutputStream</code>.
   *
   * @param marshaller The <code>Marshaller</code> to use.
   * @param outputStream The <code>OutputStream</code> to write to.
   * @throws Exception if an error occurs while performing the marshaling.
   */
  void marshal(Marshaller marshaller, OutputStream outputStream)
      throws Exception;
}
