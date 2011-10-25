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

import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.UpdatableLDAPRequest;
import com.unboundid.ldap.sdk.controls.PostReadRequestControl;
import com.unboundid.ldap.sdk.controls.PostReadResponseControl;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.VirtualListViewRequestControl;
import com.unboundid.ldap.sdk.controls.VirtualListViewResponseControl;
import com.unboundid.scim.data.Meta;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMRequest;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.ServerErrorException;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.GetResourceRequest;
import com.unboundid.scim.sdk.GetResourcesRequest;
import com.unboundid.scim.sdk.PostResourceRequest;
import com.unboundid.scim.sdk.DeleteResourceRequest;
import com.unboundid.scim.sdk.PutResourceRequest;
import com.unboundid.scim.ldap.ResourceMapper;
import com.unboundid.scim.sdk.UnsupportedOperationException;
import com.unboundid.util.StaticUtils;

import javax.ws.rs.core.UriBuilder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;


/**
 * This abstract class is a base class for implementations of the SCIM server
 * backend API that use an LDAP-based resource storage repository.
 */
public abstract class LDAPBackend
    extends SCIMBackend
{
  /**
   * Create a new instance of an LDAP backend.
   */
  public LDAPBackend()
  {
  }



  /**
   * Retrieve an LDAP interface that may be used to interact with the LDAP
   * server.
   *
   * @param userID  The authenticated user ID for the request being processed.
   *
   * @return  An LDAP interface that may be used to interact with the LDAP
   *          server.
   *
   * @throws LDAPException  If there was a problem retrieving an LDAP interface.
   */
  protected abstract LDAPInterface getLDAPInterface(final String userID)
      throws LDAPException;



  /**
   * Add any common controls that may be required for LDAP requests.
   *
   * @param scimRequest  The SCIM request being processed.
   * @param ldapRequest  The LDAP request to which the common controls are to be
   *                     added.
   */
  protected abstract void addCommonControls(
      final SCIMRequest scimRequest,
      final UpdatableLDAPRequest ldapRequest);



  @Override
  public BaseResource getResource(
      final GetResourceRequest request) throws SCIMException
  {
    try
    {
      final SCIMServer scimServer = SCIMServer.getInstance();
      final ResourceMapper mapper =
          scimServer.getResourceMapper(
              request.getResourceDescriptor());

      final Set<String> requestAttributeSet = new HashSet<String>();
      requestAttributeSet.addAll(
          mapper.toLDAPAttributeTypes(request.getAttributes()));
      requestAttributeSet.add("createTimestamp");
      requestAttributeSet.add("modifyTimestamp");

      final String[] requestAttributes = new String[requestAttributeSet.size()];
      requestAttributeSet.toArray(requestAttributes);

      final Filter filter = Filter.createPresenceFilter("objectclass");
      final SearchRequest searchRequest =
          new SearchRequest(request.getResourceID(), SearchScope.BASE,
              filter, requestAttributes);
      addCommonControls(request, searchRequest);

      final LDAPInterface ldapInterface =
          getLDAPInterface(request.getAuthenticatedUserID());

      final SearchResultEntry searchResultEntry =
          ldapInterface.searchForEntry(searchRequest);
      if (searchResultEntry == null)
      {
        throw new ResourceNotFoundException(
            "Resource " + request.getResourceID() + " not found");
      }
      else
      {
        final BaseResource resource =
            new BaseResource(request.getResourceDescriptor());

        setIdAndMetaAttributes(resource, request, searchResultEntry);

        final List<SCIMAttribute> attributes = mapper.toSCIMAttributes(
            searchResultEntry, request.getAttributes(), ldapInterface);
        for (final SCIMAttribute a : attributes)
        {
          resource.getScimObject().addAttribute(a);
        }

        return resource;
      }
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(e.getMessage());
    }
  }



  @Override
  public Resources getResources(final GetResourcesRequest request)
      throws SCIMException
  {
    final SCIMServer scimServer = SCIMServer.getInstance();
    final ResourceMapper resourceMapper =
        scimServer.getResourceMapper(request.getResourceDescriptor());
    if (resourceMapper == null || !resourceMapper.supportsQuery())
    {
      throw new UnsupportedOperationException(
          "The requested operation is not supported on resource end-point '" +
              request.getResourceDescriptor().getQueryEndpoint() + "'");
    }

    try
    {
      final SCIMFilter scimFilter = request.getFilter();

      final Set<String> requestAttributeSet =
          resourceMapper.toLDAPAttributeTypes(request.getAttributes());
      requestAttributeSet.add("createTimestamp");
      requestAttributeSet.add("modifyTimestamp");
      requestAttributeSet.add("objectClass");

      final int maxResults = getConfig().getMaxResults();
      final LDAPInterface ldapInterface =
          getLDAPInterface(request.getAuthenticatedUserID());
      final ResourceSearchResultListener resultListener =
          new ResourceSearchResultListener(request, ldapInterface, maxResults);
      SearchRequest searchRequest = null;
      if (scimFilter != null)
      {
        if (scimFilter.getFilterType() == SCIMFilterType.EQUALITY)
        {
          final AttributePath path = scimFilter.getFilterAttribute();
          if (path.getAttributeSchema().equals(SCIMConstants.SCHEMA_URI_CORE) &&
              path.getAttributeName().equalsIgnoreCase("id"))
          {
            final String[] requestAttributes =
                new String[requestAttributeSet.size()];
            requestAttributeSet.toArray(requestAttributes);

            searchRequest =
                new SearchRequest(resultListener, scimFilter.getFilterValue(),
                    SearchScope.BASE,
                    Filter.createPresenceFilter("objectclass"),
                    requestAttributes);
          }
        }
      }

      if (searchRequest == null)
      {
        // Map the SCIM filter to an LDAP filter.
        final Filter filter = resourceMapper.toLDAPFilter(scimFilter);

        // The LDAP filter results will still need to be filtered using the
        // SCIM filter, so we need to request all the filter attributes.
        addFilterAttributes(requestAttributeSet, filter);

        final String[] requestAttributes =
            new String[requestAttributeSet.size()];
        requestAttributeSet.toArray(requestAttributes);

        searchRequest =
            new SearchRequest(resultListener, resourceMapper.getSearchBaseDN(),
                SearchScope.SUB, filter, requestAttributes);
      }

      final SortParameters sortParameters = request.getSortParameters();
      if (sortParameters != null)
      {
        searchRequest.addControl(
            resourceMapper.toLDAPSortControl(sortParameters));
      }

      // Use the VLV control to perform pagination.
      final PageParameters pageParameters = request.getPageParameters();
      if (pageParameters != null)
      {
        final int count;
        if (pageParameters.getCount() <= 0)
        {
          count = getConfig().getMaxResults();
        }
        else
        {
          count = Math.min(pageParameters.getCount(),
                           getConfig().getMaxResults());
        }
        searchRequest.addControl(
            new VirtualListViewRequestControl(
                (int) pageParameters.getStartIndex(),
                0, count-1, 0, null, true));

        // VLV requires a sort control.
        if (sortParameters == null)
        {
          searchRequest.addControl(
              new ServerSideSortRequestControl(new SortKey("uid"))); // TODO
        }
      }

      // Invoke a search operation.
      addCommonControls(request, searchRequest);
      final SearchResult searchResult = ldapInterface.search(searchRequest);

      // Prepare the response.
      final List<BaseResource> scimObjects = resultListener.getResources();
      final Resources<BaseResource> resources;
      final VirtualListViewResponseControl vlvResponseControl =
          getVLVResponseControl(searchResult);
      if (vlvResponseControl != null)
      {
        int startIndex = 1;
        if (pageParameters != null)
        {
          startIndex = (int)pageParameters.getStartIndex();
        }
        resources = new Resources<BaseResource>(scimObjects,
            vlvResponseControl.getContentCount(), startIndex);
      }
      else
      {
        resources = new Resources<BaseResource>(scimObjects);
      }

      return resources;
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public BaseResource postResource(
      final PostResourceRequest request) throws SCIMException
  {
    final SCIMServer scimServer = SCIMServer.getInstance();
    final ResourceMapper mapper =
        scimServer.getResourceMapper(request.getResourceDescriptor());

    final Set<String> requestAttributeSet = new HashSet<String>();
    requestAttributeSet.addAll(
        mapper.toLDAPAttributeTypes(request.getAttributes()));
    requestAttributeSet.add("createTimestamp");
    requestAttributeSet.add("modifyTimestamp");

    final String[] requestAttributes = new String[requestAttributeSet.size()];
    requestAttributeSet.toArray(requestAttributes);

    Entry entry = null;
    Entry addedEntry = null;
    List<Attribute> attributes = new ArrayList<Attribute>();
    try
    {
      if (entry == null && mapper.supportsCreate())
      {
        entry = mapper.toLDAPEntry(request.getResourceObject());
      }
      else
      {
        attributes.addAll(mapper.toLDAPAttributes(request.getResourceObject()));
      }

      if (entry == null)
      {
        throw new ServerErrorException(
            "There are no resource mappers that support creation of " +
                request.getResourceDescriptor().getName() + " resources");
      }

      for (final Attribute a : attributes)
      {
        entry.addAttribute(a);
      }

      final AddRequest addRequest = new AddRequest(entry);
      addRequest.addControl(
          new PostReadRequestControl(requestAttributes));
      addCommonControls(request, addRequest);

      final LDAPInterface ldapInterface =
          getLDAPInterface(request.getAuthenticatedUserID());
      final LDAPResult addResult = ldapInterface.add(addRequest);

      final PostReadResponseControl c = getPostReadResponseControl(addResult);
      if (c != null)
      {
        addedEntry = c.getEntry();
      }

      final BaseResource resource =
          new BaseResource(request.getResourceDescriptor());

      setIdAndMetaAttributes(resource, request, addedEntry);

      final List<SCIMAttribute> scimAttributes = mapper.toSCIMAttributes(
          addedEntry, request.getAttributes(), ldapInterface);
      for (final SCIMAttribute a : scimAttributes)
      {
        resource.getScimObject().addAttribute(a);
      }

      return resource;
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteResource(final DeleteResourceRequest request)
      throws SCIMException
  {
    try
    {
      final DeleteRequest deleteRequest =
          new DeleteRequest(request.getResourceID());
      addCommonControls(request, deleteRequest);
      final LDAPResult result =
          getLDAPInterface(request.getAuthenticatedUserID()).delete(
              deleteRequest);
      if (result.getResultCode().equals(ResultCode.SUCCESS))
      {
        return;
      }
      else if (result.getResultCode().equals(ResultCode.NO_SUCH_OBJECT))
      {
        throw new ResourceNotFoundException(
            "Resource " + request.getResourceID() + " not found");
      }
      else
      {
        throw new LDAPException(result.getResultCode());
      }
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      if (e.getResultCode().equals(ResultCode.NO_SUCH_OBJECT))
      {
        throw new ResourceNotFoundException(
            "Resource " + request.getResourceID() + " not found");
      }
      throw new ServerErrorException(e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public BaseResource putResource(final PutResourceRequest request)
      throws SCIMException
  {
    final SCIMServer scimServer = SCIMServer.getInstance();
    final ResourceMapper mapper =
        scimServer.getResourceMapper(request.getResourceDescriptor());

    final Set<String> requestAttributeSet = new HashSet<String>();
    requestAttributeSet.addAll(
        mapper.toLDAPAttributeTypes(request.getAttributes()));
    requestAttributeSet.add("createTimestamp");
    requestAttributeSet.add("modifyTimestamp");

    final String[] requestAttributes = new String[requestAttributeSet.size()];
    requestAttributeSet.toArray(requestAttributes);

    final String entryDN = request.getResourceID();
    final List<Modification> mods = new ArrayList<Modification>();
    Entry modifiedEntry = null;
    try
    {
      final LDAPInterface ldapInterface =
          getLDAPInterface(request.getAuthenticatedUserID());
      final Entry currentEntry = ldapInterface.getEntry(entryDN,
                                                        requestAttributes);
      if (currentEntry == null)
      {
        throw new ResourceNotFoundException(
            "Resource " + request.getResourceID() + " not found");
      }

      mods.addAll(mapper.toLDAPModifications(currentEntry,
          request.getResourceObject()));

      if(!mods.isEmpty())
      {
        final ModifyRequest modifyRequest = new ModifyRequest(entryDN, mods);
        modifyRequest.addControl(new PostReadRequestControl(requestAttributes));
        addCommonControls(request, modifyRequest);
        final LDAPResult addResult = ldapInterface.modify(modifyRequest);

        final PostReadResponseControl c = getPostReadResponseControl(addResult);
        if (c != null)
        {
          modifiedEntry = c.getEntry();
        }
      }
      else
      {
        //No modifications necessary (the mod set is empty)
        modifiedEntry = currentEntry;
      }

      final BaseResource resource =
                  new BaseResource(request.getResourceDescriptor());
      setIdAndMetaAttributes(resource, request, modifiedEntry);

      final List<SCIMAttribute> scimAttributes = mapper.toSCIMAttributes(
        modifiedEntry, request.getAttributes(), ldapInterface);

      for (final SCIMAttribute a : scimAttributes)
      {
        resource.getScimObject().addAttribute(a);
      }

      return resource;
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(e.getMessage());
    }
  }



  /**
   * Set the id and meta attributes in a SCIM object from the provided
   * information.
   *
   * @param resource  The SCIM object whose id and meta attributes are to be
   *                    set.
   * @param request     The SCIM request.
   * @param entry       The LDAP entry from which the attribute values are to
   *                    be derived.
   */
  public static void setIdAndMetaAttributes(
      final BaseResource resource,
      final SCIMRequest request,
      final Entry entry)
  {
    try
    {
      resource.setId(entry.getParsedDN().toNormalizedString());
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      resource.setId(entry.getDN());
    }

    final String createTimestamp =
        entry.getAttributeValue("createTimestamp");
    Date createDate = null;
    if (createTimestamp != null)
    {
      try
      {
        createDate = StaticUtils.decodeGeneralizedTime(createTimestamp);
      }
      catch (ParseException e)
      {
        Debug.debugException(e);
        // Unlikely to come here.
      }
    }

    final String modifyTimestamp =
        entry.getAttributeValue("modifyTimestamp");
    Date modifyDate = null;
    if (modifyTimestamp != null)
    {
      try
      {
        modifyDate = StaticUtils.decodeGeneralizedTime(modifyTimestamp);
      }
      catch (ParseException e)
      {
        Debug.debugException(e);
        // Unlikely to come here.
      }
    }

    final UriBuilder uriBuilder = UriBuilder.fromUri(request.getBaseURL());
    uriBuilder.path(resource.getResourceDescriptor().getName());
    uriBuilder.path(entry.getDN());

    resource.setMeta(new Meta(createDate, modifyDate,
        uriBuilder.build(), null));
  }



  /**
   * Retrieve a SASL Authentication ID from a HTTP Basic Authentication user ID.
   * We need this because the HTTP Authentication user ID can not include the
   * ':' character.
   *
   * @param userID  The HTTP user ID for which a SASL Authentication ID is
   *                required. It may be {@code null} if the request was not
   *                authenticated.
   *
   * @return  A SASL Authentication ID.
   */
  protected String getSASLAuthenticationID(final String userID)
  {
    if (userID == null)
    {
      return "";
    }

    // If the user ID can be parsed as a DN then prefix it with "dn:", otherwise
    // prefix it with "u:".
    try
    {
      final DN dn = new DN(userID);

      return "dn:" + dn.toString();
    }
    catch (LDAPException e)
    {
      Debug.debugException(Level.FINE, e);
      return "u:" + userID;
    }
  }



  /**
   * Extracts a virtual list view response control from the provided result.
   *
   * @param  result  The result from which to retrieve the virtual list view
   *                 response control.
   *
   * @return  The virtual list view response  control contained in the provided
   *          result, or {@code null} if the result did not contain a virtual
   *          list view response control.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         decode the virtual list view response  control
   *                         contained in the provided result.
   */
  private static VirtualListViewResponseControl getVLVResponseControl(
      final SearchResult result) throws LDAPException
  {
    final Control c = result.getResponseControl(
        VirtualListViewResponseControl.VIRTUAL_LIST_VIEW_RESPONSE_OID);
    if (c == null)
    {
      return null;
    }

    if (c instanceof VirtualListViewResponseControl)
    {
      return (VirtualListViewResponseControl) c;
    }
    else
    {
      return new VirtualListViewResponseControl(c.getOID(), c.isCritical(),
          c.getValue());
    }
  }



  /**
   * Extracts a post-read response control from the provided result.
   *
   * @param  result  The result from which to retrieve the post-read response
   *                 control.
   *
   * @return  The post-read response control contained in the provided result,
   *          or {@code null} if the result did not contain a post-read response
   *          control.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         decode the post-read response control contained in
   *                         the provided result.
   */
  public static PostReadResponseControl getPostReadResponseControl(
      final LDAPResult result)
      throws LDAPException
  {
    final Control c = result.getResponseControl(
        PostReadResponseControl.POST_READ_RESPONSE_OID);
    if (c == null)
    {
      return null;
    }

    if (c instanceof PostReadResponseControl)
    {
      return (PostReadResponseControl) c;
    }
    else
    {
      return new PostReadResponseControl(c.getOID(), c.isCritical(),
          c.getValue());
    }
  }



  /**
   * Add all the attributes used in the specified filter to the provided
   * set of attributes.
   *
   * @param attributes  The set of attributes to which the filter attributes
   *                    should be added.
   *
   * @param filter      The filter whose attributes are of interest.
   */
  private static void addFilterAttributes(final Set<String> attributes,
                                          final Filter filter)
  {
    switch (filter.getFilterType())
    {
      case Filter.FILTER_TYPE_AND:
      case Filter.FILTER_TYPE_OR:
        for (final Filter f : filter.getComponents())
        {
          addFilterAttributes(attributes, f);
        }
        break;

      case Filter.FILTER_TYPE_NOT:
        addFilterAttributes(attributes, filter.getNOTComponent());
        break;

      case Filter.FILTER_TYPE_APPROXIMATE_MATCH:
      case Filter.FILTER_TYPE_EQUALITY:
      case Filter.FILTER_TYPE_GREATER_OR_EQUAL:
      case Filter.FILTER_TYPE_LESS_OR_EQUAL:
      case Filter.FILTER_TYPE_PRESENCE:
      case Filter.FILTER_TYPE_SUBSTRING:
        attributes.add(filter.getAttributeName());
        break;
    }
  }
}
