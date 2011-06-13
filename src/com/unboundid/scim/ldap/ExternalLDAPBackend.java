/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;



/**
 * This class provides an implementation of the SCIM server backend API that
 * uses an external LDAP server as the resource storage repository.
 */
public class ExternalLDAPBackend extends LDAPBackend
{
  private LDAPExternalServer ldapExternalServer;



  /**
   * Create a new external LDAP backend.
   *
   * @param config  An LDAP external server configuration.
   */
  public ExternalLDAPBackend(final LDAPExternalServerConfig config)
  {
    super(config.getDsBaseDN());
    this.ldapExternalServer = new LDAPExternalServer(config);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected LDAPInterface getLDAPInterface()
      throws LDAPException
  {
    return ldapExternalServer.getLDAPConnectionPool();
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeBackend()
  {
    if (ldapExternalServer != null)
    {
      ldapExternalServer.close();
      ldapExternalServer = null;
    }
  }



}
