/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import com.unboundid.scim.ldap.LDAPBackend;
import com.unboundid.scim.ldap.ResourceMapper;
import com.unboundid.scim.schema.ResourceDescriptor;
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
  private final ServerContext serverContext;

  /**
   * The set of timestamp attributes that we need to ask for when
   * making requests to the underlying LDAP server. We are overriding this for
   * this backend because we use the PostReadResponseControl to get the entry,
   * but there is a bug with this control in that it won't return virtual
   * attribute values such as 'createTimestamp' or 'modifyTimestamp'. So instead
   * we use the compact form of the lastMod attributes.
   */
  private static final Set<String> LASTMOD_ATTRS;

  static
  {
    HashSet<String> attrs = new HashSet<String>(2);
    attrs.add("ds-create-time");
    attrs.add("ds-update-time");
    LASTMOD_ATTRS = Collections.unmodifiableSet(attrs);
  }


  /**
   * Create a new Server Context LDAP backend.
   *
   * @param resourceMappers  The resource mappers configured for SCIM resource
   *                         end-points.
   * @param serverContext    The server context provided by the extension.
   */
  public ServerContextBackend(
      final Map<ResourceDescriptor, ResourceMapper> resourceMappers,
      final ServerContext serverContext)
  {
    super(resourceMappers);
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
   * {@inheritDoc}
   */
  @Override
  protected Set<String> getLastModAttributes()
  {
    return LASTMOD_ATTRS;
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
