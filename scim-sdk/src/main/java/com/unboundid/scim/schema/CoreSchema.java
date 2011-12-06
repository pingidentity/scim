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

package com.unboundid.scim.schema;

import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Contains the resources and their attributes defined in
 * Core Schema 1.0 - draft 2.
 */
public class CoreSchema
{
  //// 3.  SCIM Schema Structure ////
  private static final AttributeDescriptor MULTIVALUED_TYPE =
      AttributeDescriptor.simple("type",
          AttributeDescriptor.DataType.STRING,
          "A label indicating the attribute's function; e.g., \"work\" or " +
              "\"home\"",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor MULTIVALUED_PRIMARY =
      AttributeDescriptor.simple("primary",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value indicating the 'primary' or preferred attribute " +
              "value for this attribute",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor MULTIVALUED_DISPLAY =
      AttributeDescriptor.simple("display",
          AttributeDescriptor.DataType.STRING,
          "A human readable name, primarily used for display purposes",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor MULTIVALUED_OPERATION =
      AttributeDescriptor.simple("operation",
          AttributeDescriptor.DataType.STRING,
          "The operation to perform on the multi-valued attribute during a " +
              "PATCH request",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  /**
   * Creates the default sub-attributes for multi-valued attributes
   * of the given type. This will include the type, primary, display and
   * operation attributes.
   *
   * @param dataType The type of the attribute.
   * @return The default sub-attributes for multi-valued attributes.
   */
  static List<AttributeDescriptor> createCommonMultiValuedSubAttributes(
      final AttributeDescriptor.DataType dataType)
  {
     final AttributeDescriptor value =
      AttributeDescriptor.simple("value", dataType,
          "The attribute's significant value",
          SCIMConstants.SCHEMA_URI_CORE, false, true, false);
    List<AttributeDescriptor> subAttributes =
        new ArrayList<AttributeDescriptor>(5);
    subAttributes.add(value);
    subAttributes.add(MULTIVALUED_TYPE);
    subAttributes.add(MULTIVALUED_PRIMARY);
    subAttributes.add(MULTIVALUED_DISPLAY);
    subAttributes.add(MULTIVALUED_OPERATION);
    return subAttributes;
  }

  /**
   * Adds the default sub-attributes for multi-valued attributes. This
   * will include the type, primary, display and operation attributes.
   *
   * @param subAttributes  A list specifying the sub attributes of the complex
   *                       attribute.
   * @return The default sub-attributes for multi-valued attributes.
   */
  static List<AttributeDescriptor> addCommonMultiValuedSubAttributes(
      final AttributeDescriptor... subAttributes)
  {
    List<AttributeDescriptor> subAttributeList =
        new ArrayList<AttributeDescriptor>(subAttributes.length + 5);
    subAttributeList.addAll(Arrays.asList(subAttributes));
    subAttributeList.add(MULTIVALUED_TYPE);
    subAttributeList.add(MULTIVALUED_PRIMARY);
    subAttributeList.add(MULTIVALUED_DISPLAY);
    subAttributeList.add(MULTIVALUED_OPERATION);
    return subAttributeList;
  }

  /**
   * Adds the common resource attributes. This will include id, externalId,
   * and meta.
   *
   * @param attributes A list specifying the attributes of a resource.
   * @return The list of attributes including the common attributes.
   */
  static List<AttributeDescriptor> addCommonResourceAttributes(
      final AttributeDescriptor... attributes)
  {
    List<AttributeDescriptor> attributeList =
        new ArrayList<AttributeDescriptor>(attributes.length + 3);

    // These attributes need to be in the same order as defined in the SCIM XML
    // schema scim-core.xsd.
    attributeList.add(ID);
    attributeList.add(META);
    attributeList.add(EXTERNAL_ID);
    attributeList.addAll(Arrays.asList(attributes));
    return attributeList;
  }

  //// 5.  SCIM Core Schema ////

  //// 5.1.  Common Schema Attributes ////
  private static final AttributeDescriptor ID =
      AttributeDescriptor.simple("id",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the SCIM Resource as defined by the " +
              "Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor EXTERNAL_ID =
      AttributeDescriptor.simple("externalId",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the Resource as defined by the " +
              "Service Consumer",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);


  private static final AttributeDescriptor META_CREATED =
      AttributeDescriptor.simple("created",
          AttributeDescriptor.DataType.DATETIME,
          "The DateTime the Resource was added to the Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_LAST_MODIFIED =
      AttributeDescriptor.simple("lastModified",
          AttributeDescriptor.DataType.DATETIME,
          "The most recent DateTime the details of this Resource were " +
              "updated at the Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_LOCATION =
      AttributeDescriptor.simple("location",
          AttributeDescriptor.DataType.STRING,
          "The URI of the Resource being returned",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_VERSION =
      AttributeDescriptor.simple("version",
          AttributeDescriptor.DataType.STRING,
          "The version of the Resource being returned",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_ATTRIBUTES =
      AttributeDescriptor.simple("attributes",
          AttributeDescriptor.DataType.STRING,
          "The names of the attributes to remove from the Resource during a " +
              "PATCH operation",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META =
      AttributeDescriptor.complex("meta",
          "A complex type containing metadata about the resource",
          SCIMConstants.SCHEMA_URI_CORE, false, false, META_CREATED,
          META_LAST_MODIFIED, META_LOCATION, META_VERSION, META_ATTRIBUTES);

  //// 6.  SCIM User Schema ////
  //// 6.1.  Singular Attributes ////

  private static final AttributeDescriptor USER_NAME =
      AttributeDescriptor.simple("userName",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the User, typically used by the user to " +
              "directly authenticate to the Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, false, true, false);

  private static final AttributeDescriptor NAME_FORMATTED =
      AttributeDescriptor.simple("formatted",
          AttributeDescriptor.DataType.STRING,
          "The full name, including all middle names, titles, and suffixes " +
              "as appropriate, formatted for display (e.g. Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_FAMILY_NAME =
      AttributeDescriptor.simple("familyName",
          AttributeDescriptor.DataType.STRING,
          "The family name of the User, or \"Last Name\" in most Western " +
              "languages (e.g. Jensen given the full name Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_GIVEN_NAME =
      AttributeDescriptor.simple("givenName",
          AttributeDescriptor.DataType.STRING,
          "The given name of the User, or \"First Name\" in most Western " +
              "languages (e.g. Barbara given the full name Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_MIDDLE_NAME =
      AttributeDescriptor.simple("middleName",
          AttributeDescriptor.DataType.STRING,
          "The middle name(s) of the User (e.g. Jane given the full name Ms. " +
              "Barbara Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_HONORIFIC_PREFIX =
      AttributeDescriptor.simple("honorificPrefix",
          AttributeDescriptor.DataType.STRING,
          "The honorific prefix(es) of the User, or \"Title\" in most " +
              "Western languages (e.g. Ms. given the full name Ms. Barbara " +
              "Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_HONORIFIC_SUFFIX =
      AttributeDescriptor.simple("honorificSuffix",
          AttributeDescriptor.DataType.STRING,
          "The honorific suffix(es) of the User, or \"Suffix\" in most " +
              "Western languages (e.g. III. given the full name Ms. Barbara " +
              "Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME =
      AttributeDescriptor.complex("name",
          "The components of the User's real name",
          SCIMConstants.SCHEMA_URI_CORE, false, false, NAME_FORMATTED,
          NAME_FAMILY_NAME, NAME_GIVEN_NAME, NAME_MIDDLE_NAME,
          NAME_HONORIFIC_PREFIX, NAME_HONORIFIC_SUFFIX);

  private static final AttributeDescriptor DISPLAY_NAME =
      AttributeDescriptor.simple("displayName",
          AttributeDescriptor.DataType.STRING,
          "The name of the User, suitable for display to end-users",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NICK_NAME =
      AttributeDescriptor.simple("nickName",
          AttributeDescriptor.DataType.STRING,
          "The casual way to address the user in real life, e.g. \"Bob\" or " +
              "\"Bobby\" instead of \"Robert\"",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PROFILE_URL =
      AttributeDescriptor.simple("profileUrl",
          AttributeDescriptor.DataType.STRING,
          "URL to a page representing the User's online profile",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor TITLE =
      AttributeDescriptor.simple("title",
          AttributeDescriptor.DataType.STRING,
          "The User's title, such as \"Vice President\"",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor USER_TYPE =
      AttributeDescriptor.simple("userType",
          AttributeDescriptor.DataType.STRING,
          "The organization-to-user relationship",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PREFERRED_LANGUAGE =
      AttributeDescriptor.simple("preferredLanguage",
          AttributeDescriptor.DataType.STRING,
          "The User's preferred written or spoken language. Generally used " +
              "for selecting a localized User interface.  Valid values are " +
              "concatenation of the ISO 639-1 two letter language code, an " +
              "underscore, and the ISO 3166-1 2 letter country code; e.g., " +
              "specifies the language English and country US",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor LOCALE =
      AttributeDescriptor.simple("locale",
          AttributeDescriptor.DataType.STRING,
          "Used to indicate the User's default location for purposes of " +
              "localizing items such as currency, date time format, " +
              "ISO 639-1 two letter language code an underscore, and the " +
              "ISO 3166-1 2 letter country code; e.g., 'en_US' specifies the " +
              "language English and country US",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor TIMEZONE =
      AttributeDescriptor.simple("timezone",
          AttributeDescriptor.DataType.STRING,
          "The User's time zone in the \"Olson\" timezone database format; " +
              "e.g.,'America/Los_Angeles'",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ACTIVE =
      AttributeDescriptor.simple("active",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value indicating the User's administrative status",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PASSWORD =
      AttributeDescriptor.simple("password",
          AttributeDescriptor.DataType.STRING,
          "The User's clear text password. This attribute is intended to be " +
              "used as a means to specify an initial password when creating " +
              "a new User or to reset an existing User's password. This " +
              "value will never be returned by a Service Provider in any form",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  //// 6.2. Multi-valued Attributes ////

  private static final AttributeDescriptor EMAILS =
      AttributeDescriptor.simpleMultiValued("emails",
          AttributeDescriptor.DataType.STRING,
          "E-mail addresses for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PHONE_NUMBERS =
      AttributeDescriptor.simpleMultiValued("phoneNumbers",
          AttributeDescriptor.DataType.STRING,
          "Phone numbers for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor IMS =
      AttributeDescriptor.simpleMultiValued("ims",
          AttributeDescriptor.DataType.STRING,
          "Instant messaging address for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PHOTOS =
      AttributeDescriptor.simpleMultiValued("photos",
          AttributeDescriptor.DataType.STRING,
          "URL of photos of the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  private static final AttributeDescriptor ADDRESS_FORMATTED =
      AttributeDescriptor.simple("formatted",
          AttributeDescriptor.DataType.STRING,
          "The full mailing address, formatted for display or use with a " +
              "mailing label",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_STREET_ADDRESS =
      AttributeDescriptor.simple("streetAddress",
          AttributeDescriptor.DataType.STRING,
          "The full street address component, which may include house " +
              "number, street name, P.O. box, and multi-line extended street " +
              "address information",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_LOCALITY =
      AttributeDescriptor.simple("locality",
          AttributeDescriptor.DataType.STRING,
          "The city or locality component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_REGION =
      AttributeDescriptor.simple("region",
          AttributeDescriptor.DataType.STRING,
          "The state or region component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_POSTAL_CODE =
      AttributeDescriptor.simple("postalCode",
          AttributeDescriptor.DataType.STRING,
          "The zipcode or postal code component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_COUNTRY =
      AttributeDescriptor.simple("country",
          AttributeDescriptor.DataType.STRING,
          "The country name component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESSES =
      AttributeDescriptor.complexMultiValued("addresses",
          "A physical mailing address for this User",
          SCIMConstants.SCHEMA_URI_CORE, true, false,
          new String[]{"work", "home", "other"},
          ADDRESS_FORMATTED, ADDRESS_STREET_ADDRESS, ADDRESS_LOCALITY,
          ADDRESS_REGION, ADDRESS_POSTAL_CODE, ADDRESS_COUNTRY);

  private static final AttributeDescriptor GROUPS =
      AttributeDescriptor.simpleMultiValued("groups",
          AttributeDescriptor.DataType.STRING,
          "A list of groups that the user belongs to",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ENTITLEMENTS =
      AttributeDescriptor.simpleMultiValued("entitlements",
          AttributeDescriptor.DataType.STRING,
          "A list of entitlements for the User that represent a thing the " +
              "User has. That is, an entitlement is an additional right to a " +
              "thing, object or service",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ROLES =
      AttributeDescriptor.simpleMultiValued("roles",
          AttributeDescriptor.DataType.STRING,
          "A list of roles for the User that collectively represent who the " +
              "User is",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  //// 7.  SCIM Enterprise User Schema Extension ////

  private static final AttributeDescriptor EMPLOYEE_NUMBER =
      AttributeDescriptor.simple("employeeNumber",
          AttributeDescriptor.DataType.STRING,
          "Numeric or alphanumeric identifier assigned to a person, " +
              "typically based on order of hire or association with an " +
              "organization",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor COST_CENTER =
      AttributeDescriptor.simple("costCenter",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of a cost center",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor ORGANIZATION =
      AttributeDescriptor.simple("organization",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of an organization",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor DIVISION =
      AttributeDescriptor.simple("division",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of a division",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor DEPARTMENT =
      AttributeDescriptor.simple("department",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of a department",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);

  private static final AttributeDescriptor MANAGER_ID =
      AttributeDescriptor.simple("managerId",
          AttributeDescriptor.DataType.STRING,
          "The id of the SCIM resource representing the User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor MANAGER_DISPLAY_NAME =
      AttributeDescriptor.simple("displayName",
          AttributeDescriptor.DataType.STRING,
          "The displayName of the User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor MANAGER =
      AttributeDescriptor.complex("manager", "The User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false,
          MANAGER_ID, MANAGER_DISPLAY_NAME);

  //// 8.  SCIM Group Schema ////

  private static final AttributeDescriptor GROUP_DISPLAY_NAME =
      AttributeDescriptor.simple("displayName",
          AttributeDescriptor.DataType.STRING,
          "A human readable name for the Group",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor MEMBERS =
      AttributeDescriptor.simpleMultiValued("members",
          AttributeDescriptor.DataType.STRING,
          "A list of members of the Group",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false,
          "User", "Group");

  //// 9.  Service Provider Configuration Schema ////

  private static final AttributeDescriptor CONFIG_DOCUMENTATION_URL =
      AttributeDescriptor.simple("documentationUrl",
          AttributeDescriptor.DataType.STRING,
          "An HTTP addressable URL pointing to the Service Provider's human " +
              "consumable help documentation",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor PATCH_SUPPORTED =
      AttributeDescriptor.simple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the PATCH operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_SUPPORTED =
      AttributeDescriptor.simple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the BULK operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_MAX_OPERATIONS =
      AttributeDescriptor.simple("maxOperations",
          AttributeDescriptor.DataType.INTEGER,
          "An integer value specifying the maximum number of resource " +
              "operations in a BULK operation",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_MAX_PAYLOAD_SIZE =
      AttributeDescriptor.simple("maxPayloadSize",
          AttributeDescriptor.DataType.INTEGER,
          "An integer value specifying the maximum payload size in bytes " +
              "of a BULK operation",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor FILTER_SUPPORTED =
      AttributeDescriptor.simple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the BULK operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor FILTER_MAX_RESULTS =
      AttributeDescriptor.simple("maxResults",
          AttributeDescriptor.DataType.INTEGER,
          "Integer value specifying the maximum number of Resources returned " +
              "in a response",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor CHANGE_PASSWORD_SUPPORTED =
      AttributeDescriptor.simple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the Change Password operation " +
              "is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor SORT_SUPPORTED =
      AttributeDescriptor.simple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether sorting is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ETAG_SUPPORTED =
      AttributeDescriptor.simple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether Etag resource versions are " +
              "supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_NAME =
      AttributeDescriptor.simple("name",
          AttributeDescriptor.DataType.STRING,
          "The common authentication scheme name.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_DESCRIPTION =
      AttributeDescriptor.simple("description",
          AttributeDescriptor.DataType.STRING,
          "A description of the Authentication Scheme.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_SPEC_URL =
      AttributeDescriptor.simple("specUrl",
          AttributeDescriptor.DataType.STRING,
          "A HTTP addressable URL pointing to the Authentication Scheme's " +
              "specification.",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor AUTH_SCHEME_DOCUMENTATION_URL =
      AttributeDescriptor.simple("documentationUrl",
          AttributeDescriptor.DataType.STRING,
          "A HTTP addressable URL pointing to the Authentication Scheme's " +
              "usage documentation.",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor PATCH_CONFIG =
      AttributeDescriptor.complex(
          "patch",
          "A complex type that specifies PATCH configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          PATCH_SUPPORTED);
  private static final AttributeDescriptor BULK_CONFIG =
      AttributeDescriptor.complex(
          "bulk",
          "A complex type that specifies BULK configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          BULK_SUPPORTED, BULK_MAX_OPERATIONS, BULK_MAX_PAYLOAD_SIZE);
  private static final AttributeDescriptor FILTER_CONFIG =
      AttributeDescriptor.complex(
          "filter",
          "A complex type that specifies Filter configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          FILTER_SUPPORTED, FILTER_MAX_RESULTS);
  private static final AttributeDescriptor CHANGE_PASSWORD_CONFIG =
      AttributeDescriptor.complex("changePassword",
          "A complex type that specifies Change Password configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          CHANGE_PASSWORD_SUPPORTED);
  private static final AttributeDescriptor SORT_CONFIG =
      AttributeDescriptor.complex("sort",
          "A complex type that specifies Sort configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          SORT_SUPPORTED);
  private static final AttributeDescriptor ETAG_CONFIG =
      AttributeDescriptor.complex("etag",
          "A complex type that specifies Etag configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          ETAG_SUPPORTED);
  private static final AttributeDescriptor AUTH_SCHEMES =
      AttributeDescriptor.complexMultiValued("authenticationSchemes",
          "A complex type that specifies supported Authentication Scheme " +
              "properties.",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          new String[]{"OAuth", "OAuth2", "HttpBasic", "httpDigest"},
          AUTH_SCHEME_NAME, AUTH_SCHEME_DESCRIPTION, AUTH_SCHEME_SPEC_URL,
          AUTH_SCHEME_DOCUMENTATION_URL);

  //// 10.  Resource Schema ////

  private static final AttributeDescriptor RESOURCE_NAME =
      AttributeDescriptor.simple("name",
          AttributeDescriptor.DataType.STRING,
          "The addressable Resource endpoint name",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_DESCRIPTION =
      AttributeDescriptor.simple("description",
          AttributeDescriptor.DataType.STRING,
          "The Resource's human readable description",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_SCHEMA =
      AttributeDescriptor.simple("schema",
          AttributeDescriptor.DataType.STRING,
          "The Resource's associated schema URN",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_ENDPOINT =
      AttributeDescriptor.simple("endpoint",
          AttributeDescriptor.DataType.STRING,
          "The Resource's HTTP addressable query endpoint relative to the " +
              "Base URL",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);

  private static final AttributeDescriptor ATTRIBUTES_NAME =
      AttributeDescriptor.simple("name",
          AttributeDescriptor.DataType.STRING,
          "The attribute's name", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_TYPE =
      AttributeDescriptor.simple("type",
          AttributeDescriptor.DataType.STRING,
          "The attribute's data type", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_MULTIVALUED =
      AttributeDescriptor.simple("multiValued",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value indicating the attribute's plurality",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_DESCRIPTION =
      AttributeDescriptor.simple("description",
          AttributeDescriptor.DataType.STRING,
          "The attribute's human readable description",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_SCHEMA =
      AttributeDescriptor.simple("schema",
          AttributeDescriptor.DataType.STRING,
          "The attribute's associated schema", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_READ_ONLY =
      AttributeDescriptor.simple("readOnly",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the attribute is mutable",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_REQUIRED =
      AttributeDescriptor.simple("required",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the attribute is required",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_CASE_EXACT =
      AttributeDescriptor.simple("caseExact",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the string attribute is case " +
              "sensitive",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);

  private static final AttributeDescriptor RESOURCE_SUB_ATTRIBUTES =
      AttributeDescriptor.complexMultiValued("subAttributes",
          "A list specifying the contained attributes",
          SCIMConstants.SCHEMA_URI_CORE, true, false, (String[]) null,
          ATTRIBUTES_NAME, ATTRIBUTES_TYPE, ATTRIBUTES_DESCRIPTION,
          ATTRIBUTES_READ_ONLY, ATTRIBUTES_REQUIRED, ATTRIBUTES_CASE_EXACT);
  private static final AttributeDescriptor RESOURCE_CANONICAL_VALUES =
      AttributeDescriptor.simpleMultiValued("canonicalValues",
          AttributeDescriptor.DataType.STRING,
          "A collection of canonical values",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);

  private static final AttributeDescriptor RESOURCE_ATTRIBUTES =
      AttributeDescriptor.complexMultiValued("attributes",
          "A complex type that specifies the set of associated " +
              "Resource attributes", SCIMConstants.SCHEMA_URI_CORE, true,
          true, (String[]) null, ATTRIBUTES_NAME, ATTRIBUTES_TYPE,
          ATTRIBUTES_DESCRIPTION, ATTRIBUTES_SCHEMA,
          ATTRIBUTES_READ_ONLY, ATTRIBUTES_REQUIRED, ATTRIBUTES_CASE_EXACT,
          ATTRIBUTES_MULTIVALUED, RESOURCE_SUB_ATTRIBUTES,
          RESOURCE_CANONICAL_VALUES);

  private static final AttributeDescriptor.AttributeDescriptorResolver
        NESTING_ATTRIBUTES_RESOLVER =
        new AttributeDescriptor.AttributeDescriptorResolver(true);
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
   * The SCIM Resource Schema.
   */
  public static final ResourceDescriptor RESOURCE_SCHEMA_DESCRIPTOR;


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
          CONFIG_DOCUMENTATION_URL, PATCH_CONFIG, BULK_CONFIG, FILTER_CONFIG,
          CHANGE_PASSWORD_CONFIG, SORT_CONFIG, ETAG_CONFIG, AUTH_SCHEMES);


  /**
   * The SCIM User Schema.
   */
  public static final ResourceDescriptor USER_DESCRIPTOR =
      ResourceDescriptor.create(SCIMConstants.RESOURCE_NAME_USER,
          "SCIM core resource for representing users",
          SCIMConstants.SCHEMA_URI_CORE, SCIMConstants.RESOURCE_ENDPOINT_USERS,
          USER_NAME, NAME, DISPLAY_NAME, NICK_NAME, PROFILE_URL, TITLE,
          USER_TYPE, PREFERRED_LANGUAGE, LOCALE, TIMEZONE, ACTIVE, PASSWORD,
          EMAILS, PHONE_NUMBERS, IMS, PHOTOS, ADDRESSES, GROUPS, ENTITLEMENTS,
          ROLES, EMPLOYEE_NUMBER, COST_CENTER, ORGANIZATION, DIVISION,
          DEPARTMENT, MANAGER);

  /**
   * The SCIM Group Schema.
   */
  public static final ResourceDescriptor GROUP_DESCRIPTOR =
      ResourceDescriptor.create(SCIMConstants.RESOURCE_NAME_GROUP,
          "SCIM core resource for representing groups",
          SCIMConstants.SCHEMA_URI_CORE, SCIMConstants.RESOURCE_ENDPOINT_GROUPS,
          GROUP_DISPLAY_NAME, MEMBERS);

  /**
   * The SCIM AttributeDescriptor for the meta attribute.
   */
  public static final AttributeDescriptor META_DESCRIPTOR = META;
}
