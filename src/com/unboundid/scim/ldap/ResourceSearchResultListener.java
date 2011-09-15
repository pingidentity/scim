/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultListener;
import com.unboundid.ldap.sdk.SearchResultReference;
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
  private final List<SCIMObject> resources;



  /**
   * Create a new search result listener to retrieve SCIM objects.
   *
   * @param request  The request that is being processed.
   */
  public ResourceSearchResultListener(final GetResourcesRequest request)
  {
    final SCIMServer scimServer = SCIMServer.getInstance();

    this.resourceMapper =
        scimServer.getQueryResourceMapper(request.getEndPoint());
    this.request        = request;
    this.resources      = new ArrayList<SCIMObject>();
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
    final SCIMQueryAttributes allAttributes = new SCIMQueryAttributes();
    final SCIMObject scimObject =
        resourceMapper.toSCIMObject(searchEntry, allAttributes);

    if (scimObject != null)
    {
      LDAPBackend.setIdAndMetaAttributes(scimObject, request, searchEntry);

      if (request.getFilter() == null ||
          scimObject.matchesFilter(request.getFilter()))
      {
        if (request.getAttributes().allAttributesRequested())
        {
          resources.add(scimObject);
        }
        else
        {
          // Keep only the requested attributes.
          final SCIMObject returnObject =
              new SCIMObject(scimObject.getResourceName());
          for (final SCIMAttributeType attributeType :
              request.getAttributes().getAttributeTypes())
          {
            final SCIMAttribute a =
                scimObject.getAttribute(attributeType.getSchema(),
                                        attributeType.getName());
            if (a != null)
            {
              returnObject.addAttribute(a);
            }
          }

          resources.add(returnObject);
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
  public List<SCIMObject> getResources()
  {
    return resources;
  }
}
