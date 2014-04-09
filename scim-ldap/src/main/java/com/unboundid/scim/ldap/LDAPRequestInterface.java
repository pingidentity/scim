/*
 * Copyright 2012-2014 UnboundID Corp.
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

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ModifyDNRequest;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.UpdatableLDAPRequest;



/**
 * This class wraps an LDAP interface to allow controls to be inserted into
 * requests.
 */
public class LDAPRequestInterface
{
  private final LDAPInterface ldapInterface;
  private final Control[] controls;


  /**
   * Create a new instance of this LDAP request interface.
   *
   * @param ldapInterface  The LDAP interface to be wrapped.
   * @param controls       A set of controls to be inserted into each request.
   */
  public LDAPRequestInterface(final LDAPInterface ldapInterface,
                              final Control... controls)
  {
    this.ldapInterface = ldapInterface;
    this.controls      = controls;
  }



  /**
   * Add any common controls that may be required for LDAP requests.
   *
   * @param ldapRequest  The LDAP request to which the common controls are to be
   *                     added.
   */
  protected void addControls(final UpdatableLDAPRequest ldapRequest)
  {
    if (controls != null && controls.length > 0)
    {
      ldapRequest.addControls(controls);
    }
  }



  /**
   * Processes the provided search request.  It is expected that at most one
   * entry will be returned from the search, and that no additional content from
   * the successful search result (e.g., diagnostic message or response
   * controls) are needed.
   *
   * @param  searchRequest  The search request to be processed.  If it is
   *                        configured with a search result listener or a size
   *                        limit other than one, then the provided request will
   *                        be duplicated with the appropriate settings.
   *
   * It must not be
   *                        {@code null}, it must not be configured with a
   *                        search result listener, and it should be configured
   *                        with a size limit of one.
   *
   * @return  The entry that was returned from the search, or {@code null} if no
   *          entry was returned or the base entry does not exist.
   *
   * @throws  LDAPSearchException  If the search does not complete successfully,
   *                               if more than a single entry is returned, or
   *                               if a problem is encountered while parsing the
   *                               provided filter string, sending the request,
   *                               or reading the response.
   */
  public SearchResultEntry searchForEntry(final SearchRequest searchRequest)
       throws LDAPSearchException
  {
    addControls(searchRequest);
    return ldapInterface.searchForEntry(searchRequest);
  }



  /**
   * Processes the provided search request.
   *
   * @param  searchRequest  The search request to be processed.  It must not be
   *                        {@code null}.
   *
   * @return  A search result object that provides information about the
   *          processing of the search, potentially including the set of
   *          matching entries and search references returned by the server.
   *
   * @throws  LDAPSearchException  If the search does not complete successfully,
   *                               or if a problem is encountered while sending
   *                               the request or reading the response.
   */
  public SearchResult search(final SearchRequest searchRequest)
       throws LDAPSearchException
  {
    addControls(searchRequest);
    return ldapInterface.search(searchRequest);
  }



  /**
   * Processes the provided modify request.
   *
   * @param  modifyRequest  The modify request to be processed.  It must not be
   *                        {@code null}.
   *
   * @return  The result of processing the modify operation.
   *
   * @throws  LDAPException  If the server rejects the modify request, or if a
   *                         problem is encountered while sending the request or
   *                         reading the response.
   */
  public LDAPResult modify(final ModifyRequest modifyRequest)
       throws LDAPException
  {
    addControls(modifyRequest);
    return ldapInterface.modify(modifyRequest);
  }



  /**
   * Processes the provided modify DN request.
   *
   * @param  modifyDNRequest  The modify DN request to be processed.  It must
   *                          not be {@code null}.
   *
   * @return  The result of processing the modify operation.
   *
   * @throws  LDAPException  If the server rejects the modify DN request, or if
   *                         a problem is encountered while sending the request
   *                         or reading the response.
   */
  public LDAPResult modifyDN(final ModifyDNRequest modifyDNRequest)
       throws LDAPException
  {
    addControls(modifyDNRequest);
    return ldapInterface.modifyDN(modifyDNRequest);
  }



  /**
   * Processes the provided add request.
   *
   * @param  addRequest  The add request to be processed.  It must not be
   *                     {@code null}.
   *
   * @return  The result of processing the add operation.
   *
   * @throws  LDAPException  If the server rejects the add request, or if a
   *                         problem is encountered while sending the request or
   *                         reading the response.
   */
  public LDAPResult add(final AddRequest addRequest)
       throws LDAPException
  {
    addControls(addRequest);
    return ldapInterface.add(addRequest);
  }



  /**
   * Processes the provided delete request.
   *
   * @param  deleteRequest  The delete request to be processed.  It must not be
   *                        {@code null}.
   *
   * @return  The result of processing the delete operation.
   *
   * @throws  LDAPException  If the server rejects the delete request, or if a
   *                         problem is encountered while sending the request or
   *                         reading the response.
   */
  public LDAPResult delete(final DeleteRequest deleteRequest)
       throws LDAPException
  {
    addControls(deleteRequest);
    return ldapInterface.delete(deleteRequest);
  }
}
