/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.schema.User;
import com.unboundid.scim.sdk.SCIMObject;


/**
 * This class defines an API for a backend that can be plugged into the SCIM
 * server.
 */
public abstract class SCIMBackend
{
  /**
   * Performs any cleanup which may be necessary when this backend is to be
   * taken out of service.
   */
  public abstract void finalizeBackend();



  /**
   * Retrieve all or selected attributes of the resource with the specified ID.
   *
   * @param request      The Get Resource request.
   *
   * @return  The requested resource or {@code null} if the resource does not
   *          exist. The resource contents are partial if selected attributes
   *          were requested.
   */
  public abstract User getUser(final GetResourceRequest request);

  /**
   * Retrieve all or selected attributes of the resource with the specified ID.
   *
   * @param request      The Get Resource request.
   *
   * @return  The requested resource or {@code null} if the resource does not
   *          exist. The resource contents are partial if selected attributes
   *          were requested.
   */
  public abstract SCIMObject getObject(final GetResourceRequest request);

}
