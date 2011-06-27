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
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.PostReadRequestControl;
import com.unboundid.ldap.sdk.controls.PostReadResponseControl;
import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.ResourceDescriptorManager;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;


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
   * A date format to parse LDAP values in Generalized Time syntax.
   */
  private static final SimpleDateFormat generalizedTimeDateFormat;

  static
  {
    generalizedTimeDateFormat = new SimpleDateFormat("yyyyMMddHHmmss.SSS'Z'");
    generalizedTimeDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }


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
                            filter, "*", "createTimestamp", "modifyTimestamp");
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

        return scimObject;
      }
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
  public SCIMObject postObject(final PostResourceRequest request)
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

    return returnObject;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteObject(final DeleteResourceRequest request)
  {
    try
    {
      final LDAPResult result =
          getLDAPInterface().delete(request.getResourceID());
      if (result.getResultCode().equals(ResultCode.SUCCESS))
      {
        return true;
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
        return false;
      }
      throw new RuntimeException(e);
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public SCIMObject putObject(final PutResourceRequest request)
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
        return null;
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

    return returnObject;
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
  private static void setIdAndMetaAttributes(
      final SCIMObject scimObject,
      final Entry entry,
      final SCIMQueryAttributes queryAttributes)
  {
    final ResourceDescriptor resourceDescriptor =
        ResourceDescriptorManager.instance().getResourceDescriptor(
            scimObject.getResourceName());

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
        final Date date = parseGeneralizedTime(createTimestamp);
        if (date != null)
        {
          metaAttrs.add(
              SCIMAttribute.createSingularAttribute(
                  metaDescriptor.getAttribute("created"),
                  SCIMAttributeValue.createDateValue(date)));
        }
      }

      final String modifyTimestamp =
          entry.getAttributeValue("modifyTimestamp");
      if (modifyTimestamp != null)
      {
        final Date date = parseGeneralizedTime(modifyTimestamp);
        if (date != null)
        {
          metaAttrs.add(
              SCIMAttribute.createSingularAttribute(
                  metaDescriptor.getAttribute("lastModified"),
                  SCIMAttributeValue.createDateValue(date)));
        }
      }

      scimObject.addAttribute(
          SCIMAttribute.createSingularAttribute(
              resourceDescriptor.getAttribute("meta"),
              SCIMAttributeValue.createComplexValue(metaAttrs)));
    }
  }



  /**
   * Parse a timestamp value in Generalized Time syntax.
   *
   * @param timestamp  The value in Generalized Time syntax.
   *
   * @return A date representing the parsed timestamp, or {@code null} if the
   *         value could not be parsed.
   */
  private static Date parseGeneralizedTime(final String timestamp)
  {
    // SimpleDateFormat is not thread-safe.
    synchronized (generalizedTimeDateFormat)
    {
      try
      {
        return generalizedTimeDateFormat.parse(timestamp);
      }
      catch (ParseException e)
      {
        return null;
      }
    }
  }
}
