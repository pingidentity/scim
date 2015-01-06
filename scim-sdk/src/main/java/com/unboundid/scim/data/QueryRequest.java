/*
 * Copyright 2014-2015 UnboundID Corp.
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

package com.unboundid.scim.data;

import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.SortParameters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Class for generating a SCIM 2.0-style query using POST.
 */
public class QueryRequest {

  private Set<String> schemas;
  private List<String> attributes;
  private List<String> excludedAttributes;
  private String filter;
  private PageParameters pageParameters;
  private SortParameters sortParameters;

  private Map<String,Map<String,Object>> extensionAttributes =
      new HashMap<String, Map<String, Object>>();


  /**
   * SCIM 2.0 standard schema for a search request.
   */
  public static final String SEARCH_REQUEST_SCHEMA =
      "urn:ietf:params:scim:api:messages:2.0:SearchRequest";


  /**
   * Create a query request.
   */
  public QueryRequest()
  {
    this.schemas = new HashSet<String>();
    this.schemas.add(SEARCH_REQUEST_SCHEMA);
  }

  /**
   * Get the schemas for this query request.
   * @return The schemas for this query request.
   */
  public Set<String> getSchemas()
  {
    return schemas;
  }

  /**
   * Get the filter for this query request.
   * @return SCIM filter string, or null if there is no filter.
   */
  public String getFilter()
  {
    return filter;
  }

  /**
   * Set the filter for this query request.
   * @param filter SCIM filter string.
   */
  public void setFilter(final String filter)
  {
    this.filter = filter;
  }

  /**
   * Get the requested attributes for this query.
   * @return A list of requested parameters, or null if all attributes
   * are requested.
   */
  public List<String> getAttributes()
  {
    return attributes;
  }

  /**
   * Set the requested attributes for this query.
   * @param attributes List of attribute names to be returned by the query.
   */
  public void setAttributes(final List<String> attributes)
  {
    this.attributes = attributes;
  }

  /**
   * Get the excluded attributes for this query.
   * @return A list of excluded attributes, or null if no attributes are
   * explicitly to be excluded.
   */
  public List<String> getExcludedAttributes()
  {
    return excludedAttributes;
  }

  /**
   * Set the excluded attributes for this query.
   * @param excludedAttributes List of attribute names to be excluded from
   *                           the query response.
   */
  public void setExcludedAttributes(final List<String> excludedAttributes)
  {
    this.excludedAttributes = excludedAttributes;
  }

  /**
   * Get the paging parameters for this query request.
   * @return The paging parameters for this request, or null if no paging
   * requested.
   */
  public PageParameters getPageParameters()
  {
    return pageParameters;
  }

  /**
   * Set the paging parameters for this query request.
   * @param pageParameters PageParameters instance, or null for no paging.
   */
  public void setPageParameters(final PageParameters pageParameters)
  {
    this.pageParameters = pageParameters;
  }

  /**
   * Get the sorting parameters for this query request.
   * @return The sorting parameters for the request, or null if no sorting
   * is required.
   */
  public SortParameters getSortParameters()
  {
    return sortParameters;
  }

  /**
   * Set the sorting parameters for this request.
   * @param sortParameters SortParameters instance, or null for no sorting.
   */
  public void setSortParameters(final SortParameters sortParameters)
  {
    this.sortParameters = sortParameters;
  }


  /**
   * Add an extension attribute to the query request.
   * @param schema schema URN of the extension attribute.
   * @param attribute the attribute name.
   * @param value the attribute value.
   */
  public void setExtensionAttribute(
      final String schema,
      final String attribute,
      final Object value)
  {
    schemas.add(schema);
    Map<String,Object> schemaAttributes = extensionAttributes.get(schema);
    if (schemaAttributes == null)
    {
      schemaAttributes = new HashMap<String, Object>();
      extensionAttributes.put(schema, schemaAttributes);
    }
    schemaAttributes.put(attribute,value);
  }

  /**
   * Get the value of an extension attribute.
   * @param schema Schema URN of the extension attribute.
   * @param attribute the attribute name.
   * @return the attribute value, or null if not found.
   */
  public Object getExtensionAttribute(
      final String schema,
      final String attribute)
  {
    Map<String,Object> schemaAttributes = extensionAttributes.get(schema);
    return (schemaAttributes == null) ?
        null : schemaAttributes.get(attribute);
  }

  /**
   * Get a map of values for the specified extension schema.
   * @param schema schema URN of the extension attributes.
   * @return Map keyed by attribute name, or null if there are no extension
   * attributes for the specified schema.
   */
  public Map<String, Object> getExtensionAttributes(final String schema)
  {
    return extensionAttributes.get(schema);
  }

}
