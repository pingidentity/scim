/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
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
import com.unboundid.ldap.sdk.controls.PostReadRequestControl;
import com.unboundid.ldap.sdk.controls.PostReadResponseControl;
import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.ResourceDescriptorManager;
import com.unboundid.scim.schema.Error;
import com.unboundid.scim.schema.Response;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.util.StaticUtils;
import org.eclipse.jetty.http.HttpStatus;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;


// TODO Throw checked exceptions instead of runtime exceptions

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



  /**
   * Create a SCIM response indicating that a specified resource was not found.
   *
   * @param resourceID  The ID of the resource that was not found.
   *
   * @return  A SCIM response indicating that a specified resource was not
   *          found.
   */
  private SCIMResponse notFoundResponse(final String resourceID)
  {
    final Response.Errors errors = new Response.Errors();
    final Error error = new Error();
    error.setDescription("Resource " + resourceID + " not found");
    errors.getError().add(error);

    final Response response = new Response();
    response.setErrors(errors);

    return new SCIMResponse(HttpStatus.NOT_FOUND_404, response);
  }



  @Override
  public SCIMResponse getResource(final GetResourceRequest request)
  {
    try
    {
      final Filter filter = Filter.createPresenceFilter("objectclass");
      final SearchRequest searchRequest =
          new SearchRequest(request.getResourceID(), SearchScope.BASE,
                            filter, "*", "createTimestamp", "modifyTimestamp");
      final SearchResultEntry searchResultEntry =
          getLDAPInterface().searchForEntry(searchRequest);
      if (searchResultEntry == null)
      {
        return notFoundResponse(request.getResourceID());
      }
      else
      {
        final SCIMServer scimServer = SCIMServer.getInstance();
        final Set<ResourceMapper> mappers =
            scimServer.getResourceMappers(request.getResourceName());

        final SCIMObject scimObject = new SCIMObject();
        scimObject.setResourceName(request.getResourceName());

        setIdAndMetaAttributes(scimObject, searchResultEntry,
                               request.getAttributes());

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

        final Response response = new Response();
        response.setResource(new GenericResource(scimObject));

        return new SCIMResponse(HttpStatus.OK_200, response);
      }
    }
    catch (LDAPException e)
    {
      throw new RuntimeException(e);
    }
  }



  @Override
  public SCIMResponse getResources(final GetResourcesRequest request)
  {
    final SCIMServer scimServer = SCIMServer.getInstance();
    final ResourceMapper resourceMapper =
        scimServer.getResourceMapper(request.getEndPoint());
    if (resourceMapper == null)
    {
      throw new RuntimeException("The resource end-point " +
                                 request.getEndPoint() + " is not supported");
    }

    try
    {
      final SCIMFilter scimFilter = request.getFilter();

      final String[] attributes =
          new String[] { "*", "createTimestamp", "modifyTimestamp"};
      final ResourceSearchResultListener resultListener =
          new ResourceSearchResultListener(request);
      SearchRequest searchRequest = null;
      if (scimFilter != null)
      {
        final String[] attrPath = scimFilter.getAttributePath();
        if (attrPath.length == 1 && attrPath[0].equalsIgnoreCase("id"))
        {
          searchRequest =
              new SearchRequest(resultListener, scimFilter.getFilterValue(),
                                SearchScope.BASE,
                                Filter.createPresenceFilter("objectclass"),
                                attributes);
        }
      }

      if (searchRequest == null)
      {
        final Filter filter = resourceMapper.toLDAPFilter(request.getFilter());
        searchRequest =
            new SearchRequest(resultListener, baseDN, SearchScope.SUB,
                              filter, attributes);
      }

      final SearchResult searchResult =
          getLDAPInterface().search(searchRequest);

      final List<SCIMObject> scimObjects = resultListener.getResources();
      final Response.Resources resources = new Response.Resources();
      for (final SCIMObject o : scimObjects)
      {
        resources.getResource().add(new GenericResource(o));
      }

      final Response response = new Response();
      response.setTotalResults((long) scimObjects.size());
      response.setResources(resources);

      return new SCIMResponse(HttpStatus.OK_200, response);
    }
    catch (LDAPException e)
    {
      throw new RuntimeException(e);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public SCIMResponse postResource(final PostResourceRequest request)
  {
    final SCIMServer scimServer = SCIMServer.getInstance();
    final Set<ResourceMapper> mappers =
        scimServer.getResourceMappers(request.getResourceName());

    Entry entry = null;
    Entry addedEntry = null;
    List<Attribute> attributes = new ArrayList<Attribute>();
    try
    {
      for (final ResourceMapper m : mappers)
      {
        if (entry == null && m.supportsCreate())
        {
          entry = m.toLDAPEntry(request.getResourceObject(), baseDN);
        }
        else
        {
          attributes.addAll(m.toLDAPAttributes(request.getResourceObject()));
        }
      }

      if (entry == null)
      {
        throw new RuntimeException(
            "There are no resource mappers that support creation of " +
            request.getResourceName() + " resources");
      }

      for (final Attribute a : attributes)
      {
        entry.addAttribute(a);
      }

      final AddRequest addRequest = new AddRequest(entry);
      addRequest.addControl(
          new PostReadRequestControl("*", "createTimestamp",
                                     "modifyTimestamp"));
      LDAPResult addResult = getLDAPInterface().add(addRequest);

      final PostReadResponseControl c = PostReadResponseControl.get(addResult);
      if (c != null)
      {
        addedEntry = c.getEntry();
      }
    }
    catch (LDAPException e)
    {
      throw new RuntimeException(e);
    }

    final SCIMObject returnObject = new SCIMObject();
    returnObject.setResourceName(request.getResourceName());

    setIdAndMetaAttributes(returnObject, addedEntry, request.getAttributes());

    for (final ResourceMapper m : mappers)
    {
      final List<SCIMAttribute> scimAttributes =
          m.toSCIMAttributes(request.getResourceName(), addedEntry,
                             request.getAttributes());
      for (final SCIMAttribute a : scimAttributes)
      {
        returnObject.addAttribute(a);
      }
    }

    final Response response = new Response();
    response.setResource(new GenericResource(returnObject));
    return new SCIMResponse(HttpStatus.OK_200, response);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public SCIMResponse deleteResource(final DeleteResourceRequest request)
  {
    try
    {
      final LDAPResult result =
          getLDAPInterface().delete(request.getResourceID());
      if (result.getResultCode().equals(ResultCode.SUCCESS))
      {
        return new SCIMResponse(HttpStatus.OK_200, new Response());
      }
      else if (result.getResultCode().equals(ResultCode.NO_SUCH_OBJECT))
      {
        return notFoundResponse(request.getResourceID());
      }
      else
      {
        throw new LDAPException(result.getResultCode());
      }
    }
    catch (LDAPException e)
    {
      if (e.getResultCode().equals(ResultCode.NO_SUCH_OBJECT))
      {
        return notFoundResponse(request.getResourceID());
      }
      throw new RuntimeException(e);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public SCIMResponse putResource(final PutResourceRequest request)
  {
    final SCIMServer scimServer = SCIMServer.getInstance();
    final Set<ResourceMapper> mappers =
        scimServer.getResourceMappers(request.getResourceName());

    final String entryDN = request.getResourceID();
    final List<Modification> mods = new ArrayList<Modification>();
    Entry modifiedEntry = null;
    try
    {
      final Entry currentEntry = getLDAPInterface().getEntry(entryDN);
      if (currentEntry == null)
      {
        return notFoundResponse(request.getResourceID());
      }

      for (final ResourceMapper m : mappers)
      {
        mods.addAll(m.toLDAPModifications(currentEntry,
                                          request.getResourceObject()));
      }

      final ModifyRequest modifyRequest = new ModifyRequest(entryDN, mods);
      modifyRequest.addControl(
          new PostReadRequestControl("*", "createTimestamp",
                                     "modifyTimestamp"));
      LDAPResult addResult = getLDAPInterface().modify(modifyRequest);

      final PostReadResponseControl c = PostReadResponseControl.get(addResult);
      if (c != null)
      {
        modifiedEntry = c.getEntry();
      }
    }
    catch (LDAPException e)
    {
      throw new RuntimeException(e);
    }

    final SCIMObject returnObject = new SCIMObject();
    returnObject.setResourceName(request.getResourceName());

    setIdAndMetaAttributes(returnObject, modifiedEntry,
                           request.getAttributes());

    for (final ResourceMapper m : mappers)
    {
      final List<SCIMAttribute> scimAttributes =
          m.toSCIMAttributes(request.getResourceName(), modifiedEntry,
                             request.getAttributes());
      for (final SCIMAttribute a : scimAttributes)
      {
        returnObject.addAttribute(a);
      }
    }

    final Response response = new Response();
    response.setResource(new GenericResource(returnObject));
    return new SCIMResponse(HttpStatus.OK_200, response);
  }



  /**
   * Set the id and meta attributes in a SCIM object from the provided LDAP
   * entry.
   *
   * @param scimObject       The SCIM object whose id and meta attributes are
   *                         to be set.
   * @param entry            The LDAP entry from which the attribute values are
   *                         to be derived.
   * @param queryAttributes  The attributes requested by the client.
   */
  public static void setIdAndMetaAttributes(
      final SCIMObject scimObject,
      final Entry entry,
      final SCIMQueryAttributes queryAttributes)
  {
    final ResourceDescriptor resourceDescriptor =
        ResourceDescriptorManager.instance().getResourceDescriptor(
            scimObject.getResourceName());
    if (resourceDescriptor == null)
    {
      return;
    }

    if (queryAttributes.isAttributeRequested("id"))
    {
      scimObject.addAttribute(
          SCIMAttribute.createSingularAttribute(
              resourceDescriptor.getAttribute("id"),
              SCIMAttributeValue.createStringValue(
                  entry.getDN())));
    }

    if (queryAttributes.isAttributeRequested("meta"))
    {
      final AttributeDescriptor metaDescriptor =
          resourceDescriptor.getAttribute("meta");
      final List<SCIMAttribute> metaAttrs = new ArrayList<SCIMAttribute>();

      final String createTimestamp =
          entry.getAttributeValue("createTimestamp");
      if (createTimestamp != null)
      {
        try
        {
          final Date date = StaticUtils.decodeGeneralizedTime(createTimestamp);
          metaAttrs.add(
              SCIMAttribute.createSingularAttribute(
                  metaDescriptor.getAttribute("created"),
                  SCIMAttributeValue.createDateValue(date)));
        }
        catch (ParseException e)
        {
          // Unlikely to come here.
        }
      }

      final String modifyTimestamp =
          entry.getAttributeValue("modifyTimestamp");
      if (modifyTimestamp != null)
      {
        try
        {
          final Date date = StaticUtils.decodeGeneralizedTime(modifyTimestamp);
          metaAttrs.add(
              SCIMAttribute.createSingularAttribute(
                  metaDescriptor.getAttribute("lastModified"),
                  SCIMAttributeValue.createDateValue(date)));
        }
        catch (ParseException e)
        {
          // Unlikely to come here.
        }
      }

      scimObject.addAttribute(
          SCIMAttribute.createSingularAttribute(
              resourceDescriptor.getAttribute("meta"),
              SCIMAttributeValue.createComplexValue(metaAttrs)));
    }
  }
}
