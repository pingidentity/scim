/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.scim.schema.User;




/**
 * This class provides an implementation of the SCIM server backend API that
 * uses an external LDAP server as the resource storage repository.
 */
public class LDAPBackend
  extends SCIMBackend
{
  /**
   * The name of the LDAP attribute that contains the SCIM User ID.
   */
  private static final String ATTR_ENTRYUUID = "entryUUID";

  /**
   * An LDAP external server to provide the resource storage repository.
   */
  private LDAPExternalServer ldapExternalServer;

  /**
   * The LDAP external server configuration.
   */
  private LDAPExternalServerConfig ldapConfig;



  /**
   * Create a new LDAP backend.
   *
   * @param ldapConfig  An LDAP external server configuration.
   */
  public LDAPBackend(final LDAPExternalServerConfig ldapConfig)
  {
    this.ldapConfig = ldapConfig;
    this.ldapExternalServer = new LDAPExternalServer(ldapConfig);
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



  /**
   * {@inheritDoc}
   */
  @Override
  public User getUser(final GetResourceRequest request)
  {
    try
    {
      final Filter filter =
          Filter.createANDFilter(
              Filter.createEqualityFilter(ATTR_ENTRYUUID,
                                          request.getResourceID()),
              Filter.createEqualityFilter("objectclass", "inetorgperson"));
      final SearchRequest searchRequest =
          new SearchRequest(ldapConfig.getDsBaseDN(), SearchScope.SUB,
                            filter, "*", ATTR_ENTRYUUID);
      final SearchResultEntry searchResultEntry =
          ldapExternalServer.searchForEntry(searchRequest);
      if (searchResultEntry == null)
      {
        return null;
      }
      else
      {
        return LDAPUtil.userFromInetOrgPersonEntry(searchResultEntry,
                                                   request.getAttributes());
      }
    }
    catch (LDAPException e)
    {
      throw new RuntimeException(e); // TODO
    }
  }
}
