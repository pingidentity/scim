/*
 * Copyright 2014 UnboundID Corp.
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

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.Marshaller;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a list of resources as using the SCIM 2.0 ListResponse schema,
 * with support for extension schemas and attributes.
 */
public class ListResponse<R extends BaseResource> extends Resources<R>
{

  private Map<String,Map<String,Object>> extensionAttributes =
      new HashMap<String, Map<String, Object>>();
  private Set<String> schemas = new HashSet<String>();

  /**
   * SCIM 2.0 standard schema URN for a list response.
   */
  public static String LIST_RESPONSE_SCHEMA =
      "urn:ietf:params:scim:api:messages:2.0:ListResponse";

  /**
   * Create a new streamed resources response from the provided list.
   *
   * @param resourceList The list of resources to create the response from.
   * @param totalResults The total number of results matching the query
   */
  public ListResponse(final List<R> resourceList, final int totalResults)
  {
    super(resourceList, totalResults, 0);
    this.schemas.add(LIST_RESPONSE_SCHEMA);
  }


  /**
   * Retrieves the SCIM schemas represented in this  response.
   * @return A set of schema URN strings.
   */
  public Set<String> getSchemas()
  {
    return schemas;
  }

  /**
   * Add an extension attribute to the List Response..
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

  /**
   * {@inheritDoc}
   */
  @Override
  public void marshal(final Marshaller marshaller,
                      final OutputStream outputStream)
      throws Exception
  {
    marshaller.marshal(this, outputStream);
  }
}

