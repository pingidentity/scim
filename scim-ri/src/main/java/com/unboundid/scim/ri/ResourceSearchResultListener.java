/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri;

import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultListener;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.ldap.ResourceMapper;
import com.unboundid.scim.sdk.GetResourcesRequest;
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
   * Create a new search result listener to retrieve SCIM objects.
   *
   * @param request  The request that is being processed.
   * @param ldapInterface  An LDAP interface that can be used to
   *                       derive attributes from other entries.
   */
  public ResourceSearchResultListener(final GetResourcesRequest request,
                                      final LDAPInterface ldapInterface)
  {
    final SCIMServer scimServer = SCIMServer.getInstance();

    this.resourceMapper =
        scimServer.getQueryResourceMapper(
            request.getResourceDescriptor().getQueryEndpoint());
    this.request        = request;
    this.resources      = new ArrayList<BaseResource>();
    this.ldapInterface  = ldapInterface;
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
    // Get all the attributes so we can filter on them.
    // TODO could be too expensive for derived attributes
    final SCIMQueryAttributes allAttributes = new SCIMQueryAttributes();
    final SCIMObject scimObject =
        resourceMapper.toSCIMObject(searchEntry, allAttributes, ldapInterface);
    final BaseResource resource =
        new BaseResource(request.getResourceDescriptor(),
        scimObject);

    if (scimObject != null)
    {
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
