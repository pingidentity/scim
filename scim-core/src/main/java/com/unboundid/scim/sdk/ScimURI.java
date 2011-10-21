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

package com.unboundid.scim.sdk;

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
  private final String filter;

  /**
   * The sorting parameters, or {@code null} if there are no sorting
   * parameters.
   */
  private final SortParameters sortParameters;

  /**
   * The pagination parameters, or {@code null} if there are no pagination
   * parameters.
   */
  private final PageParameters pageParameters;



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
         resourceAttribute, queryAttributes, null, null, null);
  }



  /**
   * Create a new instance of a SCIM URI that supports search parameters.
   *
   * @param baseURI               The base URI.
   * @param resourceEndPoint      The resource end-point that is the object of
   *                              this URI.
   * @param resourceID            The ID of the resource that is the object of
   *                              this URI, or {@code null} if the ID is not
   *                              specified.
   * @param mediaTypeSuffix       The media type suffix included in this URI, or
   *                              {@code null} if there is none.
   * @param resourceAttribute     The resource attribute that is the object of
   *                              this URI, or {@code null} if the object of the
   *                              URI is the entire resource.
   * @param queryAttributes       The query attributes specifying the attributes
   *                              to be returned in the response to an operation
   *                              on this URI.
   * @param filter                The filter parameter, or {@code null} if
   *                              there is no filter parameter.
   * @param sortParameters        The sorting parameters, or {@code null} if
   *                              there are no sorting parameters.
   * @param pageParameters        The pagination parameters, or {@code null} if
   *                              there are no pagination parameters.
   */
  public ScimURI(final String baseURI,
                 final String resourceEndPoint,
                 final String resourceID,
                 final String mediaTypeSuffix,
                 final SCIMAttributeType resourceAttribute,
                 final SCIMQueryAttributes queryAttributes,
                 final String filter,
                 final SortParameters sortParameters,
                 final PageParameters pageParameters)
  {
    this.baseURI              = baseURI;
    this.resourceEndPoint     = resourceEndPoint;
    this.resourceID           = resourceID;
    this.mediaTypeSuffix      = mediaTypeSuffix;
    this.resourceAttribute    = resourceAttribute;
    this.queryAttributes      = queryAttributes;
    this.filter               = filter;
    this.sortParameters       = sortParameters;
    this.pageParameters       = pageParameters;
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
   * Retrieve the filter parameter, if any.
   *
   * @return  The filter parameter, or {@code null} if there is none.
   */
  public String getFilter()
  {
    return filter;
  }



  /**
   * Retrieve the sorting parameters, if any.
   *
   * @return  The sorting parameters, or {@code null} if there are none.
   */
  public SortParameters getSortParameters()
  {
    return sortParameters;
  }



  /**
   * Retrieve the pagination parameters, if any.
   *
   * @return  The pagination parameters, or {@code null} if there are none.
   */
  public PageParameters getPageParameters()
  {
    return pageParameters;
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
        filter != null || sortParameters != null || pageParameters != null)
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
        parameters.put(QUERY_PARAMETER_FILTER, filter);
      }

      if (sortParameters != null)
      {
        parameters.put(QUERY_PARAMETER_SORT_BY,
                       sortParameters.getSortBy().toString());

        final String sortOrder = sortParameters.getSortOrder();
        if (sortOrder != null)
        {
          parameters.put(QUERY_PARAMETER_SORT_ORDER, sortOrder);
        }
      }

      if (pageParameters != null)
      {
        if (pageParameters.getStartIndex() > 0)
        {
          parameters.put(QUERY_PARAMETER_PAGE_START_INDEX,
                         pageParameters.getStartIndex());
        }

        if (pageParameters.getCount() > 0)
        {
          parameters.put(QUERY_PARAMETER_PAGE_SIZE,
                         pageParameters.getCount());
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
