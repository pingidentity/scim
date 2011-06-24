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
   * The URI of the SCIM Enterprise User schema extension.
   */
  public static final String SCHEMA_URI_ENTERPRISE_USER =
      "urn:scim:schemas:extension:user:enterprise:1.0";

  /**
   * The resource name for the User resource in the core schema.
   */
  public static final String RESOURCE_NAME_USER = "User";

  /**
   * The HTTP query string value used in a URI to select specific attributes.
   */
  public static final String ATTRIBUTES_QUERY_STRING = "attributes";

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
}
