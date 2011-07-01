/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.ldap.SCIMFilter;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.UrlEncoded;

import static com.unboundid.scim.sdk.SCIMConstants.*;



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
   * The resource end-point that is the object of this URI.
   */
  private final String resourceEndPoint;

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
   * The filter parameters, or {@code null} if there are no filter parameters.
   */
  private final SCIMFilter filter;



  /**
   * Create a new instance of a SCIM URI.
   *
   * @param baseURI            The base URI.
   * @param resourceEndPoint   The resource end-point that is the object of
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
                 final String resourceEndPoint,
                 final String resourceID,
                 final String mediaTypeSuffix,
                 final SCIMAttributeType resourceAttribute,
                 final SCIMQueryAttributes queryAttributes)
  {
    this(baseURI, resourceEndPoint, resourceID, mediaTypeSuffix,
         resourceAttribute, queryAttributes, null);
  }



  /**
   * Create a new instance of a SCIM URI that supports search parameters.
   *
   * @param baseURI            The base URI.
   * @param resourceEndPoint   The resource end-point that is the object of
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
   * @param filter             The filter parameters, or {@code null} if there
   *                           are no filter parameters.
   */
  public ScimURI(final String baseURI,
                 final String resourceEndPoint,
                 final String resourceID,
                 final String mediaTypeSuffix,
                 final SCIMAttributeType resourceAttribute,
                 final SCIMQueryAttributes queryAttributes,
                 final SCIMFilter filter)
  {
    this.baseURI           = baseURI;
    this.resourceEndPoint  = resourceEndPoint;
    this.resourceID        = resourceID;
    this.mediaTypeSuffix   = mediaTypeSuffix;
    this.resourceAttribute = resourceAttribute;
    this.queryAttributes   = queryAttributes;
    this.filter            = filter;
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
   * Retrieve the resource end-point that is the object of this URI.
   *
   * @return  The resource end-point that is the object of this URI.
   */
  public String getResourceEndPoint()
  {
    return resourceEndPoint;
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
   * Retrieve the filter parameters, if any.
   *
   * @return  The filter parameters, or {@code null} if there are none.
   */
  public SCIMFilter getFilter()
  {
    return filter;
  }



  /**
   * Create a SCIM URI from the provided information.
   *
   * @param baseURI       The component of the URI preceding the resource name.
   * @param pathInfo      The remaining part of the URI preceding any query
   *                      string. e.g. /User/{id}
   * @param queryString   The query string, or {@code null} if there is none.
   *
   * @return  A SCIM URI.
   */
  public static ScimURI parseURI(final String baseURI,
                                 final String pathInfo,
                                 final String queryString)
  {
    String s = pathInfo;
    int pos = s.indexOf('/');
    if (pos == 0)
    {
      s = pathInfo.substring(1);
      pos = s.indexOf('/');
    }

    if (pos == -1)
    {
      return parseURI(baseURI, s.substring(0), null, queryString);
    }
    else
    {
      return parseURI(baseURI, s.substring(0, pos), s.substring(pos),
                      queryString);
    }
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

    if (pathInfo != null && !pathInfo.isEmpty())
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
    SCIMFilter filter = null;
    final String[] attributes;
    if (queryString != null && !queryString.isEmpty())
    {
      final UrlEncoded queryMap = new UrlEncoded(queryString);
      if (queryMap.containsKey(QUERY_PARAMETER_ATTRIBUTES))
      {
        final String commaList = queryMap.getString(QUERY_PARAMETER_ATTRIBUTES);
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

      final String filterBy = queryMap.getString(QUERY_PARAMETER_FILTER_BY);
      if (filterBy != null && !filterBy.isEmpty())
      {
        final String filterBySchemaURI;
        final String attributePath;
        final int lastColonPos =
            filterBy.lastIndexOf(SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE);
        if (lastColonPos == -1)
        {
          filterBySchemaURI = SCHEMA_URI_CORE;
          attributePath = filterBy;
        }
        else
        {
          filterBySchemaURI = filterBy.substring(0, lastColonPos);
          attributePath = filterBy.substring(lastColonPos+1);
        }

        final String[] filterByPath = attributePath.split("\\.");

        final String filterOp =
            queryMap.getString(QUERY_PARAMETER_FILTER_OP);
        final String filterValue =
            queryMap.getString(QUERY_PARAMETER_FILTER_VALUE);

        filter = new SCIMFilter(
            filterOp, filterValue, filterBySchemaURI, filterByPath);
      }
    }
    else
    {
      attributes = new String[0];
    }

    final SCIMQueryAttributes queryAttributes =
        new SCIMQueryAttributes(attributes);

    return new ScimURI(baseURI, resourceName, resourceID, mediaTypeSuffix,
                       resourceAttribute, queryAttributes, filter);
  }



  /**
   * Appends the string representation of this URI to the provided string
   * builder. The URI is suitably encoded for use within a URL.
   *
   * @param builder  The string builder to which the string representation of
   *                 this URI should be appended.
   */
  @SuppressWarnings("unchecked")
  public void toStringURI(final StringBuilder builder)
  {
    builder.append(baseURI);
    if (!baseURI.endsWith("/"))
    {
      builder.append('/');
    }
    builder.append(resourceEndPoint);

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

    if (!queryAttributes.allAttributesRequested() ||
        filter != null)
    {
      final UrlEncoded parameters = new UrlEncoded();

      if (!queryAttributes.allAttributesRequested())
      {
        final StringBuilder attrsBuilder = new StringBuilder();
        boolean first = true;
        for (final SCIMAttributeType t : queryAttributes.getAttributeTypes())
        {
          if (first)
          {
            first = false;
          }
          else
          {
            attrsBuilder.append(',');
          }
          attrsBuilder.append(t.toQualifiedName());
        }

        parameters.put(QUERY_PARAMETER_ATTRIBUTES, attrsBuilder.toString());
      }

      if (filter != null)
      {
        parameters.put(QUERY_PARAMETER_FILTER_OP, filter.getFilterOp());

        final StringBuilder filterBy = new StringBuilder();
        final String schema = filter.getAttributeSchema();
        if (!schema.equals(SCHEMA_URI_CORE))
        {
          filterBy.append(schema);
          filterBy.append(SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE);
        }
        boolean first = true;
        for (final String attributeName : filter.getAttributePath())
        {
          if (first)
          {
            first = false;
          }
          else
          {
            filterBy.append('.');
          }
          filterBy.append(attributeName);
        }
        parameters.put(QUERY_PARAMETER_FILTER_BY, filterBy.toString());

        if (filter.getFilterValue() != null)
        {
          parameters.put(QUERY_PARAMETER_FILTER_VALUE, filter.getFilterValue());
        }
      }

      builder.append('?');
      builder.append(parameters.encode());
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
