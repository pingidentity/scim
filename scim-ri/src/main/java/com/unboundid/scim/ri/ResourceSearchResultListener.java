/*
 * Copyright 2011 UnboundID Corp.
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

import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultListener;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.ldap.ResourceMapper;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;

import java.util.ArrayList;
import java.util.List;



/**
 * This class provides a search result listener to retrieve SCIM objects.
 */
public class ResourceSearchResultListener implements SearchResultListener
{
  /**
   * The serial version ID required for this serializable class.
   */
  private static final long serialVersionUID = -2028867840959235911L;

  /**
   * A resource mapper to map the result entries into SCIM objects.
   */
  private final ResourceMapper resourceMapper;

  /**
   * The request that is being processed.
   */
  private final GetResourcesRequest request;

  /**
   * The SCIM objects to be returned.
   */
  private final List<BaseResource> resources;

  /**
   * An LDAP interface that can be used to derive attributes from other entries.
   */
  private final LDAPInterface ldapInterface;

  /**
   * The maximum number of resources that may be returned.
   */
  private final int maxResults;


  /**
   * Create a new search result listener to retrieve SCIM objects.
   *
   * @param request  The request that is being processed.
   * @param ldapInterface  An LDAP interface that can be used to
   *                       derive attributes from other entries.
   * @param maxResults  The maximum number of resources that may be returned.
   */
  public ResourceSearchResultListener(final GetResourcesRequest request,
                                      final LDAPInterface ldapInterface,
                                      final int maxResults)
  {
    final SCIMServer scimServer = SCIMServer.getInstance();

    this.resourceMapper =
        scimServer.getResourceMapper(request.getResourceDescriptor());
    this.request        = request;
    this.resources      = new ArrayList<BaseResource>();
    this.ldapInterface  = ldapInterface;
    this.maxResults     = maxResults;
  }



  /**
   * Indicates that the provided search result entry has been returned by the
   * server and may be processed by this search result listener.
   *
   * @param searchEntry The search result entry that has been returned by the
   *                    server.
   */
  public void searchEntryReturned(final SearchResultEntry searchEntry)
  {
    if (resources.size() >= maxResults)
    {
      return;
    }

    // Get all the attributes so we can filter on them.
    // TODO could be too expensive for derived attributes
    final SCIMQueryAttributes allAttributes = new SCIMQueryAttributes();
    final SCIMObject scimObject;
    try {
      scimObject = resourceMapper.toSCIMObject(searchEntry, allAttributes,
          ldapInterface);
      final BaseResource resource =
          new BaseResource(request.getResourceDescriptor(), scimObject);

      LDAPBackend.setIdAndMetaAttributes(resource, request, searchEntry);

      if (request.getFilter() == null ||
          scimObject.matchesFilter(request.getFilter()))
      {
        if (request.getAttributes().allAttributesRequested())
        {
          resources.add(resource);
        }
        else
        {
          // Keep only the requested attributes.
          final BaseResource returnedResource =
              new BaseResource(request.getResourceDescriptor());
          for (final SCIMAttributeType attributeType :
              request.getAttributes().getAttributeTypes())
          {
            final SCIMAttribute a =
                scimObject.getAttribute(attributeType.getSchema(),
                    attributeType.getName());
            if (a != null)
            {
              returnedResource.getScimObject().addAttribute(a);
            }
          }

          resources.add(returnedResource);
        }
      }
    } catch (InvalidResourceException e) {
      Debug.debugException(e);
      // TODO: We should find a way to get this exception back to LDAPBackend.
    }
  }



  /**
   * Indicates that the provided search result reference has been returned by
   * the server and may be processed by this search result listener.
   *
   * @param searchReference The search result reference that has been returned
   *                        by the server.
   */
  public void searchReferenceReturned(
      final SearchResultReference searchReference)
  {
    // No implementation currently required.
  }



  /**
   * Retrieve the SCIM objects to be returned.
   *
   * @return  The SCIM objects to be returned.
   */
  public List<BaseResource> getResources()
  {
    return resources;
  }
}
