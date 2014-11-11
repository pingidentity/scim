/*
 * Copyright 2011-2014 UnboundID Corp.
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

import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for scim-ldap search result listeners.
 */
public abstract class SCIMSearchResultListener {

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
   * An LDAP interface that can be used to derive attributes from other entries.
   */
  private final LDAPRequestInterface ldapInterface;

  /**
   * The requested attributes plus the filter attributes.
   */
  private final SCIMQueryAttributes attributes;


  /**
   * The LDAPBackend that is processing the SCIM request.
   */
  private final LDAPBackend ldapBackend;



  /**
   * Create a new search result listener to retrieve SCIM objects.
   *
   * @param backend        The LDAP backend that is processing the SCIM request.
   * @param request        The request that is being processed.
   * @param ldapInterface  An LDAP interface that can be used to
   *                       derive attributes from other entries.
   *
   * @throws com.unboundid.scim.sdk.SCIMException  Should never be thrown.
   */
  public SCIMSearchResultListener(final LDAPBackend backend,
                                  final GetResourcesRequest request,
                                  final LDAPRequestInterface ldapInterface)
      throws SCIMException
  {
    this.ldapBackend    = backend;
    this.resourceMapper =
        backend.getResourceMapper(request.getResourceDescriptor());
    this.request        = request;
    this.ldapInterface  = ldapInterface;
    this.attributes     = getFilterAttributes().merge(request.getAttributes());
  }



  /**
   * Create a SCIMQueryAttributes instance representing the attributes
   * referenced by the request filter.
   *
   * @return  A SCIMQueryAttributes instance representing the attributes
   *          referenced by the request filter.
   *
   * @throws InvalidResourceException  If there is an error constructing the
   *                                   SCIMQueryAttributes instance.
   */
  private SCIMQueryAttributes getFilterAttributes()
      throws InvalidResourceException
  {
    final List<AttributePath> attributePaths = new ArrayList<AttributePath>();
    insertFilterAttributes(request.getFilter(), attributePaths);

    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < attributePaths.size(); i++)
    {
      if (i != 0)
      {
        builder.append(',');
      }

      builder.append(attributePaths.get(i));
    }

    return new SCIMQueryAttributes(request.getResourceDescriptor(),
        builder.toString());
  }



  /**
   * Insert the attribute paths referenced by the provided filter into a
   * provided list of attribute paths.
   *
   * @param filter          The filter whose attribute paths are of interest.
   * @param attributePaths  The list of attribute paths to be updated.
   */
  private void insertFilterAttributes(
      final SCIMFilter filter,
      final List<AttributePath> attributePaths)
  {
    if (filter == null)
    {
      return;
    }

    switch (filter.getFilterType())
    {
      case AND:
      case OR:
        for (final SCIMFilter f : filter.getFilterComponents())
        {
          insertFilterAttributes(f, attributePaths);
        }
        break;

      default:
        attributePaths.add(filter.getFilterAttribute());
        break;
    }
  }



  /**
   * Convert an LDAP search result entry into a SCIM Resource.
   *
   * @param searchEntry The search result entry that has been returned by the
   *                    server.
   * @return            A SCIM BaseResource object, or null if the entry
   * does not match the SCIM search filter.
   *
   * @throws SCIMException if an error occurs mapping the LDAP entry into
   * a SCIM resource.
   */
  protected BaseResource getResourceForSearchResultEntry(
      final SearchResultEntry searchEntry) throws SCIMException
  {
    if (!resourceMapper.isDnInScope(searchEntry.getDN()))
    {
      return null;
    }

    // Get the request and filter attributes so we can filter on them.
    final SCIMObject scimObject =
        resourceMapper.toSCIMObject(searchEntry, attributes, ldapInterface);
    final BaseResource resource =
        new BaseResource(request.getResourceDescriptor(), scimObject);

    ldapBackend.setIdAndMetaAttributes(resourceMapper, resource, request,
        searchEntry, null);

    if (request.getFilter() == null ||
        scimObject.matchesFilter(request.getFilter()))
    {
      if (request.getAttributes().allAttributesRequested() ||
          resourceMapper.getDefaultSchemaURI().equals(
              "urn:unboundid:schemas:scim:ldap:1.0"))
      {
        //If we are using the Identity Access API, the paring was already
        //done inside the LDAPResourceMapper.toSCIMAttributes() method.
        return resource;
      }
      else
      {
        // Keep only the requested attributes.
        final SCIMObject paredObject =
            request.getAttributes().pareObject(scimObject);
        return new BaseResource(request.getResourceDescriptor(), paredObject);
      }
    }
    return null;
  }
}
