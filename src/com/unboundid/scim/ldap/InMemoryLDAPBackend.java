/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;



/**
 * This class provides an implementation of the SCIM server backend API that
 * uses an in-memory LDAP server as the resource storage repository.
 */
public class InMemoryLDAPBackend
  extends LDAPBackend
{
  /**
   * An in-memory LDAP server providing the resource storage repository.
   */
  private InMemoryDirectoryServer ldapServer;


  /**
   * Create a new in-memory LDAP backend.
   *
   * @param baseDN      The base DN for the LDAP server.
   * @param ldapServer  An in-memory LDAP server. The server will be shut down
   *                    by the backend when the backend is taken out of service.
   */
  public InMemoryLDAPBackend(final String baseDN,
                             final InMemoryDirectoryServer ldapServer)
  {
    super(baseDN);
    this.ldapServer = ldapServer;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeBackend()
  {
    if (ldapServer != null)
    {
      ldapServer.shutDown(true);
      ldapServer = null;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected LDAPInterface getLDAPInterface()
      throws LDAPException
  {
    return ldapServer;
  }
}
