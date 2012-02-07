/*
 * Copyright 2011-2012 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */

package com.unboundid.scim.ri;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PLAINBindRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV2RequestControl;
import com.unboundid.scim.ldap.LDAPBackend;
import com.unboundid.scim.ldap.LDAPRequestInterface;
import com.unboundid.scim.ldap.ResourceMapper;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMException;

import java.util.Map;



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
   * @param resourceMappers  The resource mappers configured for SCIM resource
   *                         end-points.
   * @param config           An LDAP external server configuration.
   */
  public ExternalLDAPBackend(
      final Map<ResourceDescriptor, ResourceMapper> resourceMappers,
      final LDAPExternalServerConfig config)
  {
    super(resourceMappers);
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
      Debug.debugException(e);
      return false;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected LDAPRequestInterface getLDAPRequestInterface(final String userID)
      throws SCIMException
  {
    try
    {
      return new LDAPRequestInterface(
          ldapExternalServer.getLDAPConnectionPool(),
          new ProxiedAuthorizationV2RequestControl(
            getSASLAuthenticationID(userID)));
    }
    catch (LDAPException e)
    {
      throw toSCIMException(e);
    }
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
