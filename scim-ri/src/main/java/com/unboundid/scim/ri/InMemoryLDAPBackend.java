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

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
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
   * @param resourceMappers  The resource mappers configured for SCIM resource
   *                         end-points.
   * @param ldapServer       An in-memory LDAP server. The server will be shut
   *                         down by the backend when the backend is taken out
   *                         of service.
   */
  public InMemoryLDAPBackend(
      final Map<ResourceDescriptor, ResourceMapper> resourceMappers,
      final InMemoryDirectoryServer ldapServer)
  {
    super(resourceMappers);
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
      final BindRequest bindRequest =
          new PLAINBindRequest(getSASLAuthenticationID(userID), password);
      final BindResult bindResult = ldapServer.bind(bindRequest);
      return bindResult.getResultCode().equals(ResultCode.SUCCESS);
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
    return new LDAPRequestInterface(
        ldapServer,
        new ProxiedAuthorizationV2RequestControl(
          getSASLAuthenticationID(userID)));
  }



}
