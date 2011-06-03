/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.schema.User;



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
   * Retrieve all or selected attributes of the user with the specified ID.
   *
   * @param userID      The ID of the user to be retrieved.
   * @param attributes  The set of attributes to be retrieved. If empty, then
   *                    return all attributes.
   *
   * @return  The requested user or {@code null} if the user does not exist.
   *          The user contents are partial if selected attributes were
   *          requested.
   */
  public abstract User getUser(final String userID,
                               final String ... attributes);
}
