/*
 * Copyright 2011-2012 UnboundID Corp.
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



/**
 * This class defines a number of constants used in Simple Cloud Identity
 * Management (SCIM) interfaces.
 */
public final class SCIMConstants
{
  /**
   * The SCIM name for the schemas attribute.
   */
  public static final String SCHEMAS_ATTRIBUTE_NAME = "schemas";
  /**
   * The namespace label associated with the default schema.
   */
  public static final String DEFAULT_SCHEMA_PREFIX = "scim";

  /**
   * Prevent this class from being instantiated.
   */
  private SCIMConstants()
  {
    // No implementation is required.
  }



  /**
   * The URI of the SCIM Core schema.
   */
  public static final String SCHEMA_URI_CORE =
      "urn:scim:schemas:core:1.0";

  /**
   * The URI of the SCIM Enterprise schema extension.
   */
  public static final String SCHEMA_URI_ENTERPRISE_EXTENSION =
      "urn:scim:schemas:extension:enterprise:1.0";

  /**
   * The resource name for the Service Provider Configuration in the core
   * schema.
   */
  public static final String RESOURCE_NAME_SERVICE_PROVIDER_CONFIG =
      "ServiceProviderConfig";

  /**
   * The resource name for the Group resource in the core schema.
   */
  public static final String RESOURCE_NAME_GROUP = "Group";

  /**
   * The resource name for the User resource in the core schema.
   */
  public static final String RESOURCE_NAME_USER = "User";

  /**
   * The resource name for the Schema resource in the core schema.
   */
  public static final String RESOURCE_NAME_SCHEMA = "Schema";

  /**
   * The end point for the Service Provider Configuration in the core
   * schema.
   */
  public static final String RESOURCE_ENDPOINT_SERVICE_PROVIDER_CONFIG =
      "ServiceProviderConfigs";

  /**
   * The end point for Groups in the REST protocol.
   */
  public static final String RESOURCE_ENDPOINT_GROUPS = "Groups";

  /**
   * The end point for Users in the REST protocol.
   */
  public static final String RESOURCE_ENDPOINT_USERS = "Users";

  /**
   * The end point for Schemas in the REST protocol.
   */
  public static final String RESOURCE_ENDPOINT_SCHEMAS = "Schemas";

  /**
   * The HTTP query parameter used in a URI to select specific SCIM attributes.
   */
  public static final String QUERY_PARAMETER_ATTRIBUTES = "attributes";

  /**
   * The HTTP query parameter used in a URI to provide a filter expression.
   */
  public static final String QUERY_PARAMETER_FILTER = "filter";

  /**
   * The HTTP query parameter used in a URI to sort by a SCIM attribute.
   */
  public static final String QUERY_PARAMETER_SORT_BY = "sortBy";

  /**
   * The HTTP query parameter used in a URI to specify the sort order.
   */
  public static final String QUERY_PARAMETER_SORT_ORDER = "sortOrder";

  /**
   * The HTTP query parameter used in a URI to specify the starting index
   * for page results.
   */
  public static final String QUERY_PARAMETER_PAGE_START_INDEX = "startIndex";

  /**
   * The HTTP query parameter used in a URI to specify the maximum size of
   * a page of results.
   */
  public static final String QUERY_PARAMETER_PAGE_SIZE = "count";

  /**
   * The name of the HTTP Origin field.
   */
  public static final String HEADER_NAME_ORIGIN  =
      "Origin";

  /**
   * The name of the HTTP Access-Control-Allow-Origin field.
   */
  public static final String HEADER_NAME_ACCESS_CONTROL_ALLOW_ORIGIN =
      "Access-Control-Allow-Origin";

  /**
   * The name of the HTTP Access-Control-Allow-Credentials field.
   */
  public static final String HEADER_NAME_ACCESS_CONTROL_ALLOW_CREDENTIALS  =
      "Access-Control-Allow-Credentials";

  /**
   * The character that separates the schema URI from the basic attribute name
   * in a fully qualified attribute name. e.g. urn:scim:schemas:core:1.0:name
   * TODO: Should it be ':' or '.'?
   */
  public static final char SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE = ':';
}
