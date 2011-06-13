/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.scim.schema.User;




/**
 * This abstract class is a base class for implementations of the SCIM server
 * backend API that use an LDAP-based resource storage repository.
 */
public abstract class LDAPBackend
  extends SCIMBackend
{
  /**
   * The name of the LDAP attribute that contains the SCIM User ID.
   */
  private static final String ATTR_ENTRYUUID = "entryUUID";

  /**
   * The base DN of the LDAP server.
   */
  private String baseDN;


  /**
   * Create a new instance of an LDAP backend.
   *
   * @param baseDN  The base DN of the LDAP server.
   */
  public LDAPBackend(final String baseDN)
  {
    this.baseDN = baseDN;
  }



  /**
   * Retrieve an LDAP interface that may be used to interact with the LDAP
   * server.
   *
   * @return  An LDAP interface that may be used to interact with the LDAP
   *          server.
   *
   * @throws LDAPException  If there was a problem retrieving an LDAP interface.
   */
  protected abstract LDAPInterface getLDAPInterface()
      throws LDAPException;



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
          new SearchRequest(baseDN, SearchScope.SUB,
                            filter, "*", ATTR_ENTRYUUID);
      final SearchResultEntry searchResultEntry =
          getLDAPInterface().searchForEntry(searchRequest);
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
