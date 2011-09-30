/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.PLAINBindRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.UpdatableLDAPRequest;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV2RequestControl;
import com.unboundid.scim.sdk.SCIMRequest;


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
    super();
    this.ldapExternalServer = new LDAPExternalServer(config);
  }



  /**
   * Perform basic authentication using the provided information.
   *
   * @param userID   The user ID to be authenticated.
   * @param password The user password to be verified.
   *
   * @return {@code true} if the provided user ID and password are valid.
   */
  @Override
  public boolean authenticate(final String userID, final String password)
  {
    try
    {
      // TODO: use a connection pool
      final LDAPConnection connection = ldapExternalServer.getLDAPConnection();

      try
      {
        final BindRequest bindRequest =
            new PLAINBindRequest(getSASLAuthenticationID(userID), password);
        final BindResult bindResult = connection.bind(bindRequest);
        return bindResult.getResultCode().equals(ResultCode.SUCCESS);
      }
      finally
      {
        connection.close();
      }
    }
    catch (final Exception e)
    {
      // TODO log the failure.
      return false;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected LDAPInterface getLDAPInterface(final String userID)
      throws LDAPException
  {
    return ldapExternalServer.getLDAPConnectionPool();
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected void addCommonControls(final SCIMRequest scimRequest,
                                   final UpdatableLDAPRequest ldapRequest)
  {
    ldapRequest.addControl(
        new ProxiedAuthorizationV2RequestControl(
            getSASLAuthenticationID(scimRequest.getAuthenticatedUserID())));
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
