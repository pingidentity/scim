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
import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.ResourceDescriptorManager;
import com.unboundid.scim.schema.User;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.List;
import java.util.Set;



/**
 * This abstract class is a base class for implementations of the SCIM server
 * backend API that use an LDAP-based resource storage repository.
 */
public abstract class LDAPBackend
  extends SCIMBackend
{
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



  @Override
  public SCIMObject getObject(final GetResourceRequest request) {
  try
    {
      final Filter filter = Filter.createPresenceFilter("objectclass");
      final SearchRequest searchRequest =
          new SearchRequest(request.getResourceID(), SearchScope.BASE,
                            filter);
      final SearchResultEntry searchResultEntry =
          getLDAPInterface().searchForEntry(searchRequest);
      if (searchResultEntry == null)
      {
        return null;
      }
      else
      {
        final SCIMServer scimServer = SCIMServer.getInstance();
        final Set<ResourceMapper> mappers =
            scimServer.getResourceMappers(request.getResourceName());

        final ResourceDescriptor resourceDescriptor =
            ResourceDescriptorManager.instance().getResourceDescriptor(
                request.getResourceName());

        final SCIMObject scimObject = new SCIMObject();
        scimObject.setResourceType(request.getResourceName());

        if (request.getAttributes().isAttributeRequested("id"))
        {
          scimObject.addAttribute(
              SCIMAttribute.createSingularAttribute(
                  resourceDescriptor.getAttribute("id"),
                  SCIMAttributeValue.createStringValue(
                      searchResultEntry.getDN())));
        }

        for (final ResourceMapper m : mappers)
        {
          final List<SCIMAttribute> attributes =
              m.toSCIMAttributes(request.getResourceName(), searchResultEntry,
                                 request.getAttributes());
          for (final SCIMAttribute a : attributes)
          {
            scimObject.addAttribute(a);
          }
        }

        return scimObject;
      }
    }
    catch (LDAPException e)
    {
      throw new RuntimeException(e); // TODO
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
      final Filter filter = Filter.createPresenceFilter("objectclass");
      final SearchRequest searchRequest =
          new SearchRequest(request.getResourceID(), SearchScope.BASE,
                            filter);
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
