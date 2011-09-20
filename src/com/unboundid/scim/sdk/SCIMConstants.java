/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;



/**
 * This class defines a number of constants used in Simple Cloud Identity
 * Management (SCIM) interfaces.
 */
public final class SCIMConstants
{
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
   * The resource name for the Group resource in the core schema.
   */
  public static final String RESOURCE_NAME_GROUP = "Group";

  /**
   * The resource name for the User resource in the core schema.
   */
  public static final String RESOURCE_NAME_USER = "User";

  /**
   * The end point for Groups in the REST protocol.
   */
  public static final String RESOURCE_ENDPOINT_GROUPS = "Groups";

  /**
   * The end point for Users in the REST protocol.
   */
  public static final String RESOURCE_ENDPOINT_USERS = "Users";

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
   * The name of the JSON media type for HTTP.
   */
  public static final String MEDIA_TYPE_JSON = "application/json";

  /**
   * The name of the XML media type for HTTP.
   */
  public static final String MEDIA_TYPE_XML = "application/xml";

  /**
   * The name of the HTTP Accept header field.
   */
  public static final String HEADER_NAME_ACCEPT = "Accept";

  /**
   * The name of the HTTP Location header field.
   */
  public static final String HEADER_NAME_LOCATION = "Location";

  /**
   * The name of the HTTP Method Override field.
   */
  public static final String HEADER_NAME_METHOD_OVERRIDE =
      "X-HTTP-Method-Override";

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
