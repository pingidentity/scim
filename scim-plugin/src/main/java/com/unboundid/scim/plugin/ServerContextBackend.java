/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.plugin;

import com.unboundid.directory.sdk.common.types.InternalConnection;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.UpdatableLDAPRequest;
import com.unboundid.scim.ri.LDAPBackend;
import com.unboundid.scim.sdk.SCIMRequest;


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
   * @param serverContext  The server context provided by the extension.
   */
  public ServerContextBackend(final ServerContext serverContext)
  {
    super();
    this.serverContext = serverContext;
  }



  /**
   * Perform basic authentication using the provided information.
   *
   * @param userID   The user name to be authenticated.
   * @param password The user password to be verified.
   *
   * @return {@code true} if the provided user name and password are valid.
   */
  @Override
  public boolean authenticate(final String userID, final String password)
  {
    // We would like to do a PLAIN SASL Bind but there is no such method
    // on the internal connection.

    final DN bindDN = getUserDN(userID);
    if (bindDN == null)
    {
      return false;
    }

    try
    {
      // Attempt a simple bind as the user for that user ID.
      final InternalConnection connection =
          serverContext.getInternalRootConnection();
      final BindResult bindResult =
          connection.bind(bindDN.toString(), password);
      return bindResult.getResultCode().equals(ResultCode.SUCCESS);
    }
    catch (LDAPException e)
    {
      serverContext.debugCaught(e);
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
    final DN bindDN = getUserDN(userID);
    if (bindDN == null)
    {
      throw new LDAPException(ResultCode.AUTHORIZATION_DENIED);
    }

    return serverContext.getInternalConnection(getUserDN(userID).toString());
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected void addCommonControls(final SCIMRequest scimRequest,
                                   final UpdatableLDAPRequest ldapRequest)
  {
    // No implementation required.
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeBackend()
  {
    // No action required.
  }



  /**
   * Map the provided user name to a user DN.
   *
   * @param userName  The user name to be mapped to a user DN.
   *
   * @return  A user DN for the user name, or {@code null} if the user could
   *          not be mapped.
   */
  private DN getUserDN(final String userName)
  {
    if (userName == null)
    {
      return null;
    }

    // If the user ID can be parsed as a DN then use that as the user entry DN,
    // otherwise try to map the user ID to the uid attribute.
    DN bindDN = null;
    try
    {
      bindDN = new DN(userName);
    }
    catch (LDAPException e)
    {
      // The user ID is not a DN.
    }

    try
    {
      if (bindDN == null)
      {
        final InternalConnection connection =
            serverContext.getInternalRootConnection();
        final Filter f =
            Filter.createEqualityFilter("uid", userName);
        final SearchResult searchResult =
            connection.search("", SearchScope.SUB, f);
        if (searchResult.getEntryCount() == 1)
        {
          bindDN = searchResult.getSearchEntries().get(0).getParsedDN();
        }
      }
    }
    catch (LDAPException e)
    {
      serverContext.debugCaught(e);
    }

    return bindDN;
  }
}
