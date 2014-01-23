/*
 * Copyright 2011-2013 UnboundID Corp.
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

package com.unboundid.scim.schema;

import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.unboundid.scim.sdk.StaticUtils.toLowerCase;

/**
 * Contains the resources and their attributes defined in
 * Core Schema 1.1.
 */
public class CoreSchema
{
  //// 5.  SCIM Core Schema ////

  //// 5.1.  Common Schema Attributes ////
  private static final AttributeDescriptor ID =
      AttributeDescriptor.createAttribute("id",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the SCIM Resource as defined by the " +
              "Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor EXTERNAL_ID =
      AttributeDescriptor.createAttribute("externalId",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the Resource as defined by the " +
              "Service Consumer",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);


  private static final AttributeDescriptor META_CREATED =
      AttributeDescriptor.createSubAttribute("created",
          AttributeDescriptor.DataType.DATETIME,
          "The DateTime the Resource was added to the Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_LAST_MODIFIED =
      AttributeDescriptor.createSubAttribute("lastModified",
          AttributeDescriptor.DataType.DATETIME,
          "The most recent DateTime the details of this Resource were " +
              "updated at the Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_LOCATION =
      AttributeDescriptor.createSubAttribute("location",
          AttributeDescriptor.DataType.STRING,
          "The URI of the Resource being returned",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_VERSION =
      AttributeDescriptor.createSubAttribute("version",
          AttributeDescriptor.DataType.STRING,
          "The version of the Resource being returned",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_ATTRIBUTES =
      AttributeDescriptor.newAttribute("attributes",
          "attribute", AttributeDescriptor.DataType.STRING,
          "The names of the attributes to remove from the Resource during a " +
              "PATCH operation",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          AttributeDescriptor.createSubAttribute("value",
            AttributeDescriptor.DataType.STRING,
            "The attribute's significant value",
            SCIMConstants.SCHEMA_URI_CORE, false, true, false));
  private static final AttributeDescriptor META =
      AttributeDescriptor.createAttribute("meta",
          AttributeDescriptor.DataType.COMPLEX,
          "A complex type containing metadata about the resource",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          META_CREATED, META_LAST_MODIFIED, META_LOCATION, META_VERSION,
          META_ATTRIBUTES);

  //// 6.  SCIM User Schema ////
  //// 6.1.  Singular Attributes ////

  private static final AttributeDescriptor USER_NAME =
      AttributeDescriptor.createAttribute("userName",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the User, typically used by the user to " +
              "directly authenticate to the Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, false, true, false);

  private static final AttributeDescriptor NAME_FORMATTED =
      AttributeDescriptor.createSubAttribute("formatted",
          AttributeDescriptor.DataType.STRING,
          "The full name, including all middle names, titles, and suffixes " +
              "as appropriate, formatted for display (e.g. Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_FAMILY_NAME =
      AttributeDescriptor.createSubAttribute("familyName",
          AttributeDescriptor.DataType.STRING,
          "The family name of the User, or \"Last Name\" in most Western " +
              "languages (e.g. Jensen given the full name Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_GIVEN_NAME =
      AttributeDescriptor.createSubAttribute("givenName",
          AttributeDescriptor.DataType.STRING,
          "The given name of the User, or \"First Name\" in most Western " +
              "languages (e.g. Barbara given the full name Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_MIDDLE_NAME =
      AttributeDescriptor.createSubAttribute("middleName",
          AttributeDescriptor.DataType.STRING,
          "The middle name(s) of the User (e.g. Jane given the full name Ms. " +
              "Barbara Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_HONORIFIC_PREFIX =
      AttributeDescriptor.createSubAttribute("honorificPrefix",
          AttributeDescriptor.DataType.STRING,
          "The honorific prefix(es) of the User, or \"Title\" in most " +
              "Western languages (e.g. Ms. given the full name Ms. Barbara " +
              "Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_HONORIFIC_SUFFIX =
      AttributeDescriptor.createSubAttribute("honorificSuffix",
          AttributeDescriptor.DataType.STRING,
          "The honorific suffix(es) of the User, or \"Suffix\" in most " +
              "Western languages (e.g. III. given the full name Ms. Barbara " +
              "Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME =
      AttributeDescriptor.createAttribute("name",
          AttributeDescriptor.DataType.COMPLEX,
          "The components of the User's real name",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          NAME_FORMATTED, NAME_FAMILY_NAME, NAME_GIVEN_NAME, NAME_MIDDLE_NAME,
          NAME_HONORIFIC_PREFIX, NAME_HONORIFIC_SUFFIX);

  private static final AttributeDescriptor DISPLAY_NAME =
      AttributeDescriptor.createAttribute("displayName",
          AttributeDescriptor.DataType.STRING,
          "The name of the User, suitable for display to end-users",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NICK_NAME =
      AttributeDescriptor.createAttribute("nickName",
          AttributeDescriptor.DataType.STRING,
          "The casual way to address the user in real life, e.g. \"Bob\" or " +
              "\"Bobby\" instead of \"Robert\"",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PROFILE_URL =
      AttributeDescriptor.createAttribute("profileUrl",
          AttributeDescriptor.DataType.STRING,
          "URL to a page representing the User's online profile",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor TITLE =
      AttributeDescriptor.createAttribute("title",
          AttributeDescriptor.DataType.STRING,
          "The User's title, such as \"Vice President\"",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor USER_TYPE =
      AttributeDescriptor.createAttribute("userType",
          AttributeDescriptor.DataType.STRING,
          "The organization-to-user relationship",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PREFERRED_LANGUAGE =
      AttributeDescriptor.createAttribute("preferredLanguage",
          AttributeDescriptor.DataType.STRING,
          "The User's preferred written or spoken language. Generally used " +
              "for selecting a localized User interface.  Valid values are " +
              "concatenation of the ISO 639-1 two-letter language code, an " +
              "underscore, and the ISO 3166-1 two-letter country code; e.g., " +
              "specifies the language English and country US",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor LOCALE =
      AttributeDescriptor.createAttribute("locale",
          AttributeDescriptor.DataType.STRING,
          "Used to indicate the User's default location for purposes of " +
              "localizing items such as currency, date time format, " +
              "ISO 639-1 two-letter language code an underscore, and the " +
              "ISO 3166-1 two-letter country code; e.g., 'en_US' specifies " +
              "the language English and country US",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor TIMEZONE =
      AttributeDescriptor.createAttribute("timezone",
          AttributeDescriptor.DataType.STRING,
          "The User's time zone in the \"Olson\" timezone database format; " +
              "e.g.,'America/Los_Angeles'",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ACTIVE =
      AttributeDescriptor.createAttribute("active",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value indicating the User's administrative status",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PASSWORD =
      AttributeDescriptor.createAttribute("password",
          AttributeDescriptor.DataType.STRING,
          "The User's clear text password. This attribute is intended to be " +
              "used as a means to specify an initial password when creating " +
              "a new User or to reset an existing User's password. This " +
              "value will never be returned by a Service Provider in any form",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  //// 6.2. Multi-valued Attributes ////

  private static final AttributeDescriptor EMAILS =
      AttributeDescriptor.createMultiValuedAttribute("emails",
          "email", "E-mail addresses for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING),
          createMultiValuedDisplayDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedPrimaryDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedTypeDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              "work", "home", "other"));
  private static final AttributeDescriptor PHONE_NUMBERS =
      AttributeDescriptor.createMultiValuedAttribute("phoneNumbers",
          "phoneNumber", "Phone numbers for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING),
          createMultiValuedDisplayDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedPrimaryDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedTypeDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              "fax", "pager", "other"));
  private static final AttributeDescriptor IMS =
      AttributeDescriptor.createMultiValuedAttribute("ims",
          "im", "Instant messaging address for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING),
          createMultiValuedDisplayDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedPrimaryDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedTypeDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              "aim", "gtalk", "icq", "xmpp", "msn", "skype", "qq", "yahoo"));
  private static final AttributeDescriptor PHOTOS =
      AttributeDescriptor.createMultiValuedAttribute("photos",
          "photo", "URL of photos of the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING),
          createMultiValuedDisplayDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedPrimaryDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedTypeDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              "photo", "thumbnail"));

  private static final AttributeDescriptor ADDRESS_FORMATTED =
      AttributeDescriptor.createSubAttribute("formatted",
          AttributeDescriptor.DataType.STRING,
          "The full mailing address, formatted for display or use with a " +
              "mailing label",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_STREET_ADDRESS =
      AttributeDescriptor.createSubAttribute("streetAddress",
          AttributeDescriptor.DataType.STRING,
          "The full street address component, which may include house " +
              "number, street name, P.O. box, and multi-line extended street " +
              "address information",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_LOCALITY =
      AttributeDescriptor.createSubAttribute("locality",
          AttributeDescriptor.DataType.STRING,
          "The city or locality component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_REGION =
      AttributeDescriptor.createSubAttribute("region",
          AttributeDescriptor.DataType.STRING,
          "The state or region component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_POSTAL_CODE =
      AttributeDescriptor.createSubAttribute("postalCode",
          AttributeDescriptor.DataType.STRING,
          "The zipcode or postal code component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_COUNTRY =
      AttributeDescriptor.createSubAttribute("country",
          AttributeDescriptor.DataType.STRING,
          "The country name component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESSES =
      AttributeDescriptor.createMultiValuedAttribute("addresses",
          "address", "A physical mailing address for this User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedPrimaryDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedTypeDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              "work", "home", "other"),
          ADDRESS_FORMATTED, ADDRESS_STREET_ADDRESS, ADDRESS_LOCALITY,
          ADDRESS_REGION, ADDRESS_POSTAL_CODE, ADDRESS_COUNTRY);

  private static final AttributeDescriptor GROUPS =
      AttributeDescriptor.createMultiValuedAttribute("groups",
          "group", "A list of groups that the user belongs to",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING),
          createMultiValuedDisplayDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          createMultiValuedTypeDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              "direct", "indirect"));
  private static final AttributeDescriptor ENTITLEMENTS =
      AttributeDescriptor.createMultiValuedAttribute("entitlements",
          "entitlement",
          "A list of entitlements for the User that represent a thing the " +
              "User has. That is, an entitlement is an additional right to a " +
              "thing, object or service",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING));
  private static final AttributeDescriptor ROLES =
      AttributeDescriptor.createMultiValuedAttribute("roles",
          "role",
          "A list of roles for the User that collectively represent who the " +
              "User is",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING));
  private static final AttributeDescriptor X509CERTIFICATES =
      AttributeDescriptor.createMultiValuedAttribute("x509Certificates",
          "x509Certificate",
          "A list of certificates issued to the User. Values are DER " +
              "encoded x509.",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.BINARY));

  //// 7.  SCIM Enterprise User Schema Extension ////

  private static final AttributeDescriptor EMPLOYEE_NUMBER =
      AttributeDescriptor.createAttribute("employeeNumber",
          AttributeDescriptor.DataType.STRING,
          "Numeric or alphanumeric identifier assigned to a person, " +
              "typically based on order of hire or association with an " +
              "organization",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false,
          false, false);
  private static final AttributeDescriptor COST_CENTER =
      AttributeDescriptor.createAttribute("costCenter",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of a cost center",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false,
          false, false);
  private static final AttributeDescriptor ORGANIZATION =
      AttributeDescriptor.createAttribute("organization",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of an organization",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false,
          false, false);
  private static final AttributeDescriptor DIVISION =
      AttributeDescriptor.createAttribute("division",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of a division",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false,
          false, false);
  private static final AttributeDescriptor DEPARTMENT =
      AttributeDescriptor.createAttribute("department",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of a department",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false,
          false, false);

  private static final AttributeDescriptor MANAGER_ID =
      AttributeDescriptor.createSubAttribute("managerId",
          AttributeDescriptor.DataType.STRING,
          "The id of the SCIM resource representing the User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, true, false);
  private static final AttributeDescriptor MANAGER_DISPLAY_NAME =
      AttributeDescriptor.createSubAttribute("displayName",
          AttributeDescriptor.DataType.STRING,
          "The displayName of the User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor MANAGER =
      AttributeDescriptor.createAttribute("manager",
          AttributeDescriptor.DataType.COMPLEX, "The User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false,
          false, false, MANAGER_ID, MANAGER_DISPLAY_NAME);

  //// 8.  SCIM Group Schema ////

  private static final AttributeDescriptor GROUP_DISPLAY_NAME =
      AttributeDescriptor.createAttribute("displayName",
          AttributeDescriptor.DataType.STRING,
          "A human readable name for the Group",
          SCIMConstants.SCHEMA_URI_CORE, false, true, false);
  private static final AttributeDescriptor MEMBERS =
      AttributeDescriptor.createMultiValuedAttribute("members",
          "member", "A list of members of the Group",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          createMultiValuedTypeDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              "User", "Group"),
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING),
          createMultiValuedDisplayDescriptor(SCIMConstants.SCHEMA_URI_CORE));

  //// 9.  Service Provider Configuration Schema ////

  private static final AttributeDescriptor CONFIG_DOCUMENTATION_URL =
      AttributeDescriptor.createAttribute("documentationUrl",
          AttributeDescriptor.DataType.STRING,
          "An HTTP addressable URL pointing to the Service Provider's human " +
              "consumable help documentation",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor PATCH_SUPPORTED =
      AttributeDescriptor.createSubAttribute("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the PATCH operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_SUPPORTED =
      AttributeDescriptor.createSubAttribute("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the BULK operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_MAX_OPERATIONS =
      AttributeDescriptor.createSubAttribute("maxOperations",
          AttributeDescriptor.DataType.INTEGER,
          "An integer value specifying the maximum number of resource " +
              "operations in a BULK operation",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_MAX_PAYLOAD_SIZE =
      AttributeDescriptor.createSubAttribute("maxPayloadSize",
          AttributeDescriptor.DataType.INTEGER,
          "An integer value specifying the maximum payload size in bytes " +
              "of a BULK operation",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor FILTER_SUPPORTED =
      AttributeDescriptor.createSubAttribute("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the BULK operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor FILTER_MAX_RESULTS =
      AttributeDescriptor.createSubAttribute("maxResults",
          AttributeDescriptor.DataType.INTEGER,
          "Integer value specifying the maximum number of Resources returned " +
              "in a response",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor CHANGE_PASSWORD_SUPPORTED =
      AttributeDescriptor.createSubAttribute("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the Change Password operation " +
              "is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor SORT_SUPPORTED =
      AttributeDescriptor.createSubAttribute("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether sorting is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ETAG_SUPPORTED =
      AttributeDescriptor.createSubAttribute("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether Etag resource versions are " +
              "supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_NAME =
      AttributeDescriptor.createSubAttribute("name",
          AttributeDescriptor.DataType.STRING,
          "The common authentication scheme name.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_DESCRIPTION =
      AttributeDescriptor.createSubAttribute("description",
          AttributeDescriptor.DataType.STRING,
          "A description of the Authentication Scheme.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_SPEC_URL =
      AttributeDescriptor.createSubAttribute("specUrl",
          AttributeDescriptor.DataType.STRING,
          "A HTTP addressable URL pointing to the Authentication Scheme's " +
              "specification.",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor AUTH_SCHEME_DOCUMENTATION_URL =
      AttributeDescriptor.createSubAttribute("documentationUrl",
          AttributeDescriptor.DataType.STRING,
          "A HTTP addressable URL pointing to the Authentication Scheme's " +
              "usage documentation.",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor XML_DATA_TYPE_SUPPORTED =
      AttributeDescriptor.createSubAttribute("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the XML data format is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor PATCH_CONFIG =
      AttributeDescriptor.createAttribute("patch",
          AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies PATCH configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          PATCH_SUPPORTED);
  private static final AttributeDescriptor BULK_CONFIG =
      AttributeDescriptor.createAttribute("bulk",
          AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies BULK configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          BULK_SUPPORTED, BULK_MAX_OPERATIONS, BULK_MAX_PAYLOAD_SIZE);
  private static final AttributeDescriptor FILTER_CONFIG =
      AttributeDescriptor.createAttribute("filter",
          AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies Filter configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          FILTER_SUPPORTED, FILTER_MAX_RESULTS);
  private static final AttributeDescriptor CHANGE_PASSWORD_CONFIG =
      AttributeDescriptor.createAttribute("changePassword",
          AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies Change Password configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          CHANGE_PASSWORD_SUPPORTED);
  private static final AttributeDescriptor SORT_CONFIG =
      AttributeDescriptor.createAttribute("sort",
          AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies Sort configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          SORT_SUPPORTED);
  private static final AttributeDescriptor ETAG_CONFIG =
      AttributeDescriptor.createAttribute("etag",
          AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies Etag configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          ETAG_SUPPORTED);
  private static final AttributeDescriptor AUTH_SCHEMES =
      AttributeDescriptor.createMultiValuedAttribute("authenticationSchemes",
          "authenticationScheme",
          "A complex type that specifies supported Authentication Scheme " +
              "properties.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          createMultiValuedTypeDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              "OAuth", "OAuth2", "HttpBasic", "httpDigest"),
          createMultiValuedPrimaryDescriptor(SCIMConstants.SCHEMA_URI_CORE),
          AUTH_SCHEME_NAME, AUTH_SCHEME_DESCRIPTION, AUTH_SCHEME_SPEC_URL,
          AUTH_SCHEME_DOCUMENTATION_URL);
  private static final AttributeDescriptor XML_DATA_TYPE_CONFIG =
      AttributeDescriptor.createAttribute(
          "xmlDataFormat",
          AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies whether the XML data format is " +
          "supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          XML_DATA_TYPE_SUPPORTED);

  //// 10.  Resource Schema ////

  private static final AttributeDescriptor RESOURCE_NAME =
      AttributeDescriptor.createAttribute("name",
          AttributeDescriptor.DataType.STRING,
          "The addressable Resource endpoint name",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_DESCRIPTION =
      AttributeDescriptor.createAttribute("description",
          AttributeDescriptor.DataType.STRING,
          "The Resource's human readable description",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_SCHEMA =
      AttributeDescriptor.createAttribute("schema",
          AttributeDescriptor.DataType.STRING,
          "The Resource's associated schema URN",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_ENDPOINT =
      AttributeDescriptor.createAttribute("endpoint",
          AttributeDescriptor.DataType.STRING,
          "The Resource's HTTP addressable query endpoint relative to the " +
              "Base URL",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);

  private static final AttributeDescriptor ATTRIBUTES_NAME =
      AttributeDescriptor.createSubAttribute("name",
          AttributeDescriptor.DataType.STRING,
          "The attribute's name", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_TYPE =
      AttributeDescriptor.createSubAttribute("type",
          AttributeDescriptor.DataType.STRING,
          "The attribute's data type", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_MULTIVALUED =
      AttributeDescriptor.createSubAttribute("multiValued",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value indicating the attribute's plurality",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_MULTIVALUED_CHILD_NAME =
      AttributeDescriptor.createSubAttribute("multiValuedAttributeChildName",
          AttributeDescriptor.DataType.STRING,
          "String value specifying the child XML element name; e.g., the " +
              "'emails' attribute value is 'email', 'phoneNumbers' is " +
              "'phoneNumber'.",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor ATTRIBUTES_DESCRIPTION =
      AttributeDescriptor.createSubAttribute("description",
          AttributeDescriptor.DataType.STRING,
          "The attribute's human readable description",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_SCHEMA =
      AttributeDescriptor.createSubAttribute("schema",
          AttributeDescriptor.DataType.STRING,
          "The attribute's associated schema", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_READ_ONLY =
      AttributeDescriptor.createSubAttribute("readOnly",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the attribute is mutable",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_REQUIRED =
      AttributeDescriptor.createSubAttribute("required",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the attribute is required",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_CASE_EXACT =
      AttributeDescriptor.createSubAttribute("caseExact",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the string attribute is case " +
              "sensitive",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_CANONICAL_VALUES =
      AttributeDescriptor.createMultiValuedAttribute("canonicalValues",
          "canonicalValue", "A collection of canonical values",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false,
          createMultiValuedValueDescriptor(SCIMConstants.SCHEMA_URI_CORE,
              AttributeDescriptor.DataType.STRING));

  private static final AttributeDescriptor RESOURCE_SUB_ATTRIBUTES =
      AttributeDescriptor.newAttribute("subAttributes",
          "subAttribute", AttributeDescriptor.DataType.COMPLEX,
          "A list specifying the contained attributes",
          SCIMConstants.SCHEMA_URI_CORE, true, false,
          false, ATTRIBUTES_NAME, ATTRIBUTES_TYPE, ATTRIBUTES_MULTIVALUED,
          ATTRIBUTES_MULTIVALUED_CHILD_NAME, ATTRIBUTES_DESCRIPTION,
          ATTRIBUTES_READ_ONLY, ATTRIBUTES_REQUIRED, ATTRIBUTES_CASE_EXACT,
          ATTRIBUTES_CANONICAL_VALUES);

  private static final AttributeDescriptor RESOURCE_ATTRIBUTES =
      AttributeDescriptor.newAttribute("attributes",
          "attribute", AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies the set of associated " +
              "Resource attributes", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false, ATTRIBUTES_NAME, ATTRIBUTES_TYPE,
          ATTRIBUTES_MULTIVALUED, ATTRIBUTES_MULTIVALUED_CHILD_NAME,
          ATTRIBUTES_DESCRIPTION, ATTRIBUTES_SCHEMA,
          ATTRIBUTES_READ_ONLY, ATTRIBUTES_REQUIRED, ATTRIBUTES_CASE_EXACT,
          RESOURCE_SUB_ATTRIBUTES);

  private static final AttributeDescriptor.AttributeDescriptorResolver
        NESTING_ATTRIBUTES_RESOLVER =
        new AttributeDescriptor.AttributeDescriptorResolver(true);

  /**
   * The SCIM Resource Schema.
   */
  public static final ResourceDescriptor RESOURCE_SCHEMA_DESCRIPTOR;

  static
  {
    SCIMObject scimObject = new SCIMObject();
    scimObject.setAttribute(SCIMAttribute.create(
        RESOURCE_NAME, SCIMAttributeValue.createStringValue(
        SCIMConstants.RESOURCE_NAME_SCHEMA)));
    scimObject.setAttribute(SCIMAttribute.create(
        RESOURCE_DESCRIPTION, SCIMAttributeValue.createStringValue(
        "The Resource schema specifies the Attribute(s) and meta-data that " +
            "constitute a Resource")));
    scimObject.setAttribute(SCIMAttribute.create(
        RESOURCE_SCHEMA, SCIMAttributeValue.createStringValue(
        SCIMConstants.SCHEMA_URI_CORE)));
    scimObject.setAttribute(SCIMAttribute.create(
        RESOURCE_ENDPOINT, SCIMAttributeValue.createStringValue(
        SCIMConstants.RESOURCE_ENDPOINT_SCHEMAS)));

    SCIMAttributeValue[] entries = new SCIMAttributeValue[8];

    try
    {
      entries[0] = AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, ID);
      entries[1] = NESTING_ATTRIBUTES_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, META);
      entries[2] = AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, EXTERNAL_ID);
      entries[3] = AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, RESOURCE_NAME);
      entries[4] = AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, RESOURCE_DESCRIPTION);
      entries[5] = AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, RESOURCE_SCHEMA);
      entries[6] = AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, RESOURCE_ENDPOINT);
      entries[7] = NESTING_ATTRIBUTES_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, RESOURCE_ATTRIBUTES);
    }
    catch(InvalidResourceException e)
    {
      // This should not occur as these are all defined here...
      throw new RuntimeException(e);
    }

    scimObject.setAttribute(
        SCIMAttribute.create(RESOURCE_ATTRIBUTES, entries));


    RESOURCE_SCHEMA_DESCRIPTOR = new ResourceDescriptor(null, scimObject)
    {
      @Override
      public ResourceDescriptor getResourceDescriptor() {
        return this;
      }

      @Override
      public Collection<AttributeDescriptor> getAttributes() {
        return getAttributeValues(SCIMConstants.SCHEMA_URI_CORE,
            "attributes", NESTING_ATTRIBUTES_RESOLVER);
      }
    };
  }

  /**
   * The SCIM Service Provider Configuration Schema.
   */
  public static final ResourceDescriptor
      SERVICE_PROVIDER_CONFIG_SCHEMA_DESCRIPTOR =
      ResourceDescriptor.create(
          SCIMConstants.RESOURCE_NAME_SERVICE_PROVIDER_CONFIG,
          "The Service Provider Configuration Resource enables a Service " +
          "Provider to expose its compliance with the SCIM specification in " +
          "a standardized form as well as provide additional implementation " +
          "details to Consumers",
          SCIMConstants.SCHEMA_URI_CORE,
          SCIMConstants.RESOURCE_ENDPOINT_SERVICE_PROVIDER_CONFIG,
          ID, META, CONFIG_DOCUMENTATION_URL, PATCH_CONFIG, BULK_CONFIG,
          FILTER_CONFIG, CHANGE_PASSWORD_CONFIG, SORT_CONFIG, ETAG_CONFIG,
          AUTH_SCHEMES, XML_DATA_TYPE_CONFIG);

  /**
   * The SCIM User Schema.
   */
  public static final ResourceDescriptor USER_DESCRIPTOR =
      ResourceDescriptor.create(SCIMConstants.RESOURCE_NAME_USER,
          "SCIM core resource for representing users",
          SCIMConstants.SCHEMA_URI_CORE, SCIMConstants.RESOURCE_ENDPOINT_USERS,
          ID, META, EXTERNAL_ID, USER_NAME, NAME, DISPLAY_NAME, NICK_NAME,
          PROFILE_URL, TITLE, USER_TYPE, PREFERRED_LANGUAGE, LOCALE, TIMEZONE,
          ACTIVE, PASSWORD, EMAILS, PHONE_NUMBERS, IMS, PHOTOS, ADDRESSES,
          GROUPS, ENTITLEMENTS, ROLES, X509CERTIFICATES, EMPLOYEE_NUMBER,
          COST_CENTER, ORGANIZATION, DIVISION, DEPARTMENT, MANAGER);

  /**
   * The SCIM Group Schema.
   */
  public static final ResourceDescriptor GROUP_DESCRIPTOR =
      ResourceDescriptor.create(SCIMConstants.RESOURCE_NAME_GROUP,
          "SCIM core resource for representing groups",
          SCIMConstants.SCHEMA_URI_CORE, SCIMConstants.RESOURCE_ENDPOINT_GROUPS,
          ID, META, EXTERNAL_ID, GROUP_DISPLAY_NAME, MEMBERS);

  /**
   * The SCIM AttributeDescriptor for the meta attribute.
   */
  public static final AttributeDescriptor META_DESCRIPTOR = META;

  /**
   * The SCIM AttributeDescriptor for the id attribute.
   */
  public static final AttributeDescriptor ID_DESCRIPTOR = ID;

  /**
   * The SCIM AttributeDescriptor for the externalId attribute.
   */
  public static final AttributeDescriptor EXTERNAL_ID_DESCRIPTOR = EXTERNAL_ID;

  //// 3.2 Multi-valued Attributes ////
  /**
   * Convenience method to create an attribute descriptor for the type normative
   * sub-attribute of multi-valued attributes.
   *
   * @param schema The attribute's associated schema.
   * @param canonicalValues The canonical values to include in the descriptor.
   *
   * @return The attribute descriptor for the type normative sub-attribute of
   * multi-valued attributes.
   */
  public static AttributeDescriptor createMultiValuedTypeDescriptor(
      final String schema, final String... canonicalValues)
  {
    return  AttributeDescriptor.createSubAttribute("type",
        AttributeDescriptor.DataType.STRING,
        "A label indicating the attribute's function; " +
            "e.g., \"work\" or " + "\"home\"",
        schema, false, false, false, canonicalValues);
  }

  /**
   * Convenience method to create an attribute descriptor for the primary
   * normative sub-attribute of multi-valued attributes.
   *
   * @param schema The attribute's associated schema.
   *
   * @return The attribute descriptor for the primary normative sub-attribute of
   * multi-valued attributes.
   */
  public static AttributeDescriptor createMultiValuedPrimaryDescriptor(
      final String schema)
  {
    return AttributeDescriptor.createSubAttribute("primary",
        AttributeDescriptor.DataType.BOOLEAN,
        "A Boolean value indicating the 'primary' or preferred attribute " +
            "value for this attribute",
        schema, false, false, false);
  }

  /**
   * Convenience method to create an attribute descriptor for the display
   * normative sub-attribute of multi-valued attributes.
   *
   * @param schema The attribute's associated schema.
   *
   * @return The attribute descriptor for the display normative sub-attribute of
   * multi-valued attributes.
   */
  public static AttributeDescriptor createMultiValuedDisplayDescriptor(
      final String schema)
  {
    return AttributeDescriptor.createSubAttribute("display",
        AttributeDescriptor.DataType.STRING,
        "A human readable name, primarily used for display purposes",
        schema, true, false, false);
  }

  /**
   * Convenience method to create an attribute descriptor for the operation
   * normative sub-attribute of multi-valued attributes.
   *
   * @param schema The attribute's associated schema.
   *
   * @return The attribute descriptor for the display operation sub-attribute of
   * multi-valued attributes.
   */
  public static AttributeDescriptor createMultiValuedOperationDescriptor(
      final String schema)
  {
    return AttributeDescriptor.createSubAttribute("operation",
        AttributeDescriptor.DataType.STRING,
        "The operation to perform on the multi-valued attribute during a " +
            "PATCH request",
        schema, false, false, false);
  }

  /**
   * Convenience method to create an attribute descriptor for the value
   * normative sub-attribute of multi-valued attributes.
   *
   * @param schema The attribute's associated schema.
   * @param dataType The data type of the value sub-attribute.
   *
   * @return The attribute descriptor for the value normative sub-attribute of
   * multi-valued attributes.
   */
  public static AttributeDescriptor createMultiValuedValueDescriptor(
      final String schema, final AttributeDescriptor.DataType dataType)
  {
    return AttributeDescriptor.createSubAttribute("value", dataType,
        "The attribute's significant value", schema, false, true, false);
  }


  /**
   * Add any missing core schema attributes described in section 5.1.
   *
   * @param attributes The currently defined attributes.
   *
   * @return The attributes with any missing core schema attributes added.
   */
  static Collection<AttributeDescriptor> addCommonAttributes(
      final Collection<AttributeDescriptor> attributes)
  {
    boolean idMissing = true;
    boolean externalIdMissing = true;
    boolean metaMissing = true;
    for(AttributeDescriptor attribute : attributes)
    {
      if(attribute.equals(ID))
      {
        idMissing = false;
      }
      if(attribute.equals(EXTERNAL_ID))
      {
        externalIdMissing = false;
      }
      if(attribute.equals(META))
      {
        metaMissing = false;
      }
    }

    if(!idMissing && !externalIdMissing && !metaMissing)
    {
      return attributes;
    }

    ArrayList<AttributeDescriptor> missingAttributes =
        new ArrayList<AttributeDescriptor>(attributes.size() + 3);
    if(idMissing)
    {
      missingAttributes.add(ID);
    }
    if(externalIdMissing)
    {
      missingAttributes.add(EXTERNAL_ID);
    }
    if(metaMissing)
    {
      missingAttributes.add(META);
    }
    missingAttributes.addAll(attributes);
    return missingAttributes;
  }

  /**
   * Add any missing core schema sub-attributes described in section 5.1
   * and 3.2.
   *
   * @param attributeDescriptor The attribute descriptor.
   * @param subAttributes The map of sub-attributes currently defined.
   *
   * @return The map of sub-attributes with any missing normative sub-attributes
   * added or {@code null} if there are no sub-attributes defined.
   */
  static Map<String, AttributeDescriptor> addNormativeSubAttributes(
      final AttributeDescriptor attributeDescriptor,
      final Map<String, AttributeDescriptor> subAttributes)
  {
    Map<String, AttributeDescriptor> missingSubAttributes =
        new LinkedHashMap<String, AttributeDescriptor>();
    if(attributeDescriptor.equals(META))
    {
      if(subAttributes == null ||
          !subAttributes.containsKey(META_CREATED.getName()))
      {
        missingSubAttributes.put(META_CREATED.getName(), META_CREATED);
      }
      if(subAttributes == null ||
          !subAttributes.containsKey(toLowerCase(META_LAST_MODIFIED.getName())))
      {
        missingSubAttributes.put(toLowerCase(META_LAST_MODIFIED.getName()),
            META_LAST_MODIFIED);
      }
      if(subAttributes == null ||
          !subAttributes.containsKey(META_LOCATION.getName()))
      {
        missingSubAttributes.put(META_LOCATION.getName(), META_LOCATION);
      }
      if(subAttributes == null ||
          !subAttributes.containsKey(META_VERSION.getName()))
      {
        missingSubAttributes.put(META_VERSION.getName(), META_VERSION);
      }
      if(subAttributes == null ||
          !subAttributes.containsKey(META_ATTRIBUTES.getName()))
      {
        missingSubAttributes.put(META_ATTRIBUTES.getName(), META_ATTRIBUTES);
      }
    }
    else if(attributeDescriptor.isMultiValued())
    {
      if(subAttributes == null ||
          !subAttributes.containsKey("type"))
      {
        missingSubAttributes.put("type", createMultiValuedTypeDescriptor(
            attributeDescriptor.getSchema()));
      }
      if(subAttributes == null ||
          !subAttributes.containsKey("primary"))
      {
        missingSubAttributes.put("primary", createMultiValuedPrimaryDescriptor(
            attributeDescriptor.getSchema()));
      }
      if(subAttributes == null ||
          !subAttributes.containsKey("display"))
      {
        missingSubAttributes.put("display", createMultiValuedDisplayDescriptor(
            attributeDescriptor.getSchema()));
      }
      if(subAttributes == null ||
          !subAttributes.containsKey("operation"))
      {
        missingSubAttributes.put("operation",
            createMultiValuedOperationDescriptor(
                attributeDescriptor.getSchema()));
      }
      if(attributeDescriptor.getDataType() !=
                        AttributeDescriptor.DataType.COMPLEX &&
          (subAttributes == null ||
              !subAttributes.containsKey("value")))
      {
        missingSubAttributes.put("value", createMultiValuedValueDescriptor(
            attributeDescriptor.getSchema(),
            attributeDescriptor.getDataType()));
      }
    }

    if(missingSubAttributes.isEmpty())
    {
      return subAttributes;
    }
    if(subAttributes != null)
    {
      missingSubAttributes.putAll(subAttributes);
    }
    return missingSubAttributes;
  }
}
