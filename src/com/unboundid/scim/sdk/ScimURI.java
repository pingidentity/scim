/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.UrlEncoded;

import static com.unboundid.scim.sdk.SCIMConstants.ATTRIBUTES_QUERY_STRING;



/**
 * This class represents a SCIM URI.
 *
 * e.g. <ul><li>{baseURI}/User/{id}.xml?attributes=userName,id</li>
 *      <li>{baseURI}/User/{id}/{attribute}</li></ul>
 */
public final class ScimURI
{
  /**
   * The base URI.
   */
  private final String baseURI;

  /**
   * The name of the resource that is the object of this URI.
   */
  private final String resourceName;

  /**
   * The ID of the resource that is the object of this URI, or {@code null} if
   * the ID is not specified.
   */
  private final String resourceID;

  /**
   * The media type suffix included in this URI, or {@code null} if there is
   * none. e.g. "json" or "xml".
   */
  private final String mediaTypeSuffix;

  /**
   * The resource attribute that is the object of this URI, or {@code null} if
   * the object of the URI is the entire resource.
   */
  private final SCIMAttributeType resourceAttribute;

  /**
   * The query attributes specifying the attributes to be returned in the
   * response to an operation on this URI.
   */
  private final SCIMQueryAttributes queryAttributes;



  /**
   * Create a new instance of a SCIM URI.
   *
   * @param baseURI            The base URI.
   * @param resourceName       The name of the resource that is the object of
   *                           this URI.
   * @param resourceID         The ID of the resource that is the object of
   *                           this URI, or {@code null} if the ID is not
   *                           specified.
   * @param mediaTypeSuffix    The media type suffix included in this URI, or
   *                           {@code null} if there is none.
   * @param resourceAttribute  The resource attribute that is the object of
   *                           this URI, or {@code null} if the object of the
   *                           URI is the entire resource.
   * @param queryAttributes    The query attributes specifying the attributes
   *                           to be returned in the response to an operation
   *                           on this URI.
   */
  public ScimURI(final String baseURI,
                 final String resourceName,
                 final String resourceID,
                 final String mediaTypeSuffix,
                 final SCIMAttributeType resourceAttribute,
                 final SCIMQueryAttributes queryAttributes)
  {
    this.baseURI           = baseURI;
    this.resourceName      = resourceName;
    this.resourceID        = resourceID;
    this.mediaTypeSuffix   = mediaTypeSuffix;
    this.resourceAttribute = resourceAttribute;
    this.queryAttributes   = queryAttributes;
  }



  /**
   * Retrieve the base URI.
   *
   * @return  The base URI.
   */
  public String getBaseURI()
  {
    return baseURI;
  }



  /**
   * Retrieve the name of the resource that is the object of this URI.
   *
   * @return  The the name of the resource that is the object of this URI.
   */
  public String getResourceName()
  {
    return resourceName;
  }



  /**
   * Retrieve the ID of the resource that is the object of this URI, or
   * {@code null} if the ID is not specified.
   *
   * @return  The ID of the resource that is the object of this URI, or
   *          {@code null} if the ID is not specified.
   */
  public String getResourceID()
  {
    return resourceID;
  }



  /**
   * Retreive the media type suffix included in this URI, or {@code null} if
   * there is none.
   *
   * @return  The media type suffix included in this URI, or {@code null} if
   *          there is none.
   */
  public String getMediaTypeSuffix()
  {
    return mediaTypeSuffix;
  }



  /**
   * Retrieve the resource attribute that is the object of this URI, or
   * {@code null} if the object of the URI is the entire resource.
   *
   * @return  The resource attribute that is the object of this URI, or
   *          {@code null} if the object of the URI is the entire resource.
   */
  public SCIMAttributeType getResourceAttribute()
  {
    return resourceAttribute;
  }



  /**
   * Retrieve the query attributes specifying the attributes to be returned in
   * the response to an operation on this URI.
   *
   * @return  The query attributes specifying the attributes to be returned in
   *          the response to an operation on this URI.
   */
  public SCIMQueryAttributes getQueryAttributes()
  {
    return queryAttributes;
  }



  /**
   * Create a SCIM URI from the provided information.
   *
   * @param baseURI       The component of the URI preceding the resource name.
   * @param resourceName  The name of the resource.
   * @param pathInfo      The subsequent part of the URI preceding any query
   *                      string, or {@code null} if there is none.
   * @param queryString   The query string, or {@code null} if there is none.
   *
   * @return  A SCIM URI.
   */
  public static ScimURI parseURI(final String baseURI,
                                 final String resourceName,
                                 final String pathInfo,
                                 final String queryString)
  {
    String resourceID = null;
    String mediaTypeSuffix = null;
    SCIMAttributeType resourceAttribute = null;

    if (pathInfo != null)
    {
      // Split up the components of the resource identifier.
      final String[] split = pathInfo.split("/");
      if (split.length < 2 || split[1].isEmpty())
      {
        throw new RuntimeException("The operation does not specify a user ID");
      }

      if (split.length > 3 || !split[0].isEmpty())
      {
        throw new RuntimeException(
            "The URI does not specify a valid user operation");
      }

      // Determine the resource ID and media type suffix.
      final String resource = split[1];
      if (resource.endsWith(".xml"))
      {
        mediaTypeSuffix = "xml";
        if (resource.length() > 4)
        {
          resourceID =
              URIUtil.decodePath(resource.substring(0, resource.length() - 4));
        }
        else
        {
          resourceID = "";
        }
      }
      else if (resource.endsWith(".json"))
      {
        mediaTypeSuffix = "json";
        if (resource.length() > 5)
        {
          resourceID =
              URIUtil.decodePath(resource.substring(0, resource.length() - 5));
        }
        else
        {
          resourceID = "";
        }
      }
      else
      {
        resourceID = resource;
      }

      if (split.length > 2)
      {
        resourceAttribute =
            SCIMAttributeType.fromQualifiedName(URIUtil.decodePath(split[2]));
      }
    }

    // Parse the query string.
    final String[] attributes;
    if (queryString != null && !queryString.isEmpty())
    {
      final UrlEncoded queryMap = new UrlEncoded(queryString);
      if (queryMap.containsKey(ATTRIBUTES_QUERY_STRING))
      {
        final String commaList = queryMap.getString(ATTRIBUTES_QUERY_STRING);
        if (commaList != null && !commaList.isEmpty())
        {
          attributes = commaList.split(",");
        }
        else
        {
          attributes = new String[0];
        }
      }
      else
      {
        attributes = new String[0];
      }
    }
    else
    {
      attributes = new String[0];
    }

    final SCIMQueryAttributes queryAttributes =
        new SCIMQueryAttributes(attributes);

    return new ScimURI(baseURI, resourceName, resourceID, mediaTypeSuffix,
                       resourceAttribute, queryAttributes);
  }



  /**
   * Appends the string representation of this URI to the provided string
   * builder. The URI is suitably encoded for use within a URL.
   *
   * @param builder  The string builder to which the string representation of
   *                 this URI should be appended.
   */
  public void toStringURI(final StringBuilder builder)
  {
    builder.append(baseURI);
    if (!baseURI.endsWith("/"))
    {
      builder.append('/');
    }
    builder.append(resourceName);

    if (resourceID != null)
    {
      builder.append('/');
      URIUtil.encodePath(builder, resourceID);
    }

    if (mediaTypeSuffix != null)
    {
      builder.append('.');
      builder.append(mediaTypeSuffix);
    }

    if (resourceAttribute != null)
    {
      builder.append('/');
      URIUtil.encodePath(builder, resourceAttribute.toQualifiedName());
    }

    if (!queryAttributes.allAttributesRequested())
    {
      builder.append('?');
      builder.append(ATTRIBUTES_QUERY_STRING);
      builder.append('=');

      boolean first = true;
      for (final SCIMAttributeType t : queryAttributes.getAttributeTypes())
      {
        if (first)
        {
          first = false;
        }
        else
        {
          builder.append(',');
        }
        builder.append(UrlEncoded.encodeString(t.toQualifiedName()));
      }
    }

  }



  /**
   * {@inheritDoc}
   */
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();

    toStringURI(builder);
    return builder.toString();
  }



}
