/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;



/**
 * This class provides an implementation of the SCIM server backend API that
 * uses an external LDAP server as the resource storage repository.
 */
public class ServerContextBackend extends LDAPBackend
{
  /**
   * The server context provided by the extension.
   */
  private ServerContext serverContext;



  /**
   * Create a new external LDAP backend.
   *
   * @param baseDN         The base DN for entries representing SCIM resources.
   * @param serverContext  The server context provided by the extension.
   */
  public ServerContextBackend(final String baseDN,
                              final ServerContext serverContext)
  {
    super(baseDN);
    this.serverContext = serverContext;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected LDAPInterface getLDAPInterface()
      throws LDAPException
  {
    // TODO: Authentication!
    return serverContext.getInternalRootConnection();
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeBackend()
  {
    // No action required.
  }
}
