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
  private static final AttributeDescriptor PLURAL_TYPE =
      AttributeDescriptor.singularSimple("type",
          AttributeDescriptor.DataType.STRING,
          "The type of attribute for this instance, usually used to label " +
              "the preferred function of this instance of the Plural Attribute",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PLURAL_PRIMARY =
      AttributeDescriptor.singularSimple("primary",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value indicating whether this instance of the Plural " +
              "Attribute is the primary or preferred value of this " +
              "attribute",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PLURAL_DISPLAY =
      AttributeDescriptor.singularSimple("display",
          AttributeDescriptor.DataType.STRING,
          "A human readable name, primarily used for display purposes where " +
              "the value is an opaque or complex type such as an id",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor PLURAL_OPERATION =
      AttributeDescriptor.singularSimple("operation",
          AttributeDescriptor.DataType.STRING,
          "The operation to perform on the plural attribute during a " +
              "PATCH request",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  /**
   * Creates the default sub-attributes for simple plural attributes
   * of the given type. This will include the type, primary, display and
   * operation attributes.
   *
   * @param dataType The type of the plural attribute.
   * @return The default sub-attributes for simple plural attributes.
   */
  static List<AttributeDescriptor> createCommonPluralSubAttributes(
      final AttributeDescriptor.DataType dataType)
  {
     final AttributeDescriptor value =
      AttributeDescriptor.singularSimple("value", dataType,
          "The attribute's significant value",
          SCIMConstants.SCHEMA_URI_CORE, false, true, false);
    List<AttributeDescriptor> subAttributes =
        new ArrayList<AttributeDescriptor>(5);
    subAttributes.add(value);
    subAttributes.add(PLURAL_TYPE);
    subAttributes.add(PLURAL_PRIMARY);
    subAttributes.add(PLURAL_DISPLAY);
    subAttributes.add(PLURAL_OPERATION);
    return subAttributes;
  }

  /**
   * Adds the default sub-attributes for complex plural attributes. This
   * will include the type, primary, display and operation attributes.
   *
   * @param subAttributes  A list specifying the sub attributes of the complex
   *                       attribute.
   * @return The default sub-attributes for complex plural attributes.
   */
  static List<AttributeDescriptor> addCommonPluralSubAttributes(
      final AttributeDescriptor... subAttributes)
  {
    List<AttributeDescriptor> subAttributeList =
        new ArrayList<AttributeDescriptor>(subAttributes.length + 5);
    subAttributeList.addAll(Arrays.asList(subAttributes));
    subAttributeList.add(PLURAL_TYPE);
    subAttributeList.add(PLURAL_PRIMARY);
    subAttributeList.add(PLURAL_DISPLAY);
    subAttributeList.add(PLURAL_OPERATION);
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
      AttributeDescriptor.singularSimple("id",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the SCIM Resource as defined by the " +
              "Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor EXTERNAL_ID =
      AttributeDescriptor.singularSimple("externalId",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the Resource as defined by the " +
              "Service Consumer",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);


  private static final AttributeDescriptor META_CREATED =
      AttributeDescriptor.singularSimple("created",
          AttributeDescriptor.DataType.DATETIME,
          "The DateTime the Resource was added to the Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_LAST_MODIFIED =
      AttributeDescriptor.singularSimple("lastModified",
          AttributeDescriptor.DataType.DATETIME,
          "The most recent DateTime the details of this Resource were " +
              "updated at the Service Provider",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_LOCATION =
      AttributeDescriptor.singularSimple("location",
          AttributeDescriptor.DataType.STRING,
          "The URI of the Resource being returned",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_VERSION =
      AttributeDescriptor.singularSimple("version",
          AttributeDescriptor.DataType.STRING,
          "The version of the Resource being returned",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META_ATTRIBUTES =
      AttributeDescriptor.singularSimple("attributes",
          AttributeDescriptor.DataType.STRING,
          "The names of the attributes to remove from the Resource during a " +
              "PATCH operation",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor META =
      AttributeDescriptor.singularComplex("meta",
          "A complex type containing metadata about the resource",
          SCIMConstants.SCHEMA_URI_CORE, false, false, META_CREATED,
          META_LAST_MODIFIED, META_LOCATION, META_VERSION, META_ATTRIBUTES);

  //// 6.  SCIM User Schema ////
  //// 6.1.  Singular Attributes ////

  private static final AttributeDescriptor USER_NAME =
      AttributeDescriptor.singularSimple("userName",
          AttributeDescriptor.DataType.STRING,
          "Unique identifier for the User, typically used by the user to " +
              "directly authenticate to the service provider",
          SCIMConstants.SCHEMA_URI_CORE, false, true, false);

  private static final AttributeDescriptor NAME_FORMATTED =
      AttributeDescriptor.singularSimple("formatted",
          AttributeDescriptor.DataType.STRING,
          "The full name, including all middle names, titles, and suffixes " +
              "as appropriate, formatted for display (e.g. Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_FAMILY_NAME =
      AttributeDescriptor.singularSimple("familyName",
          AttributeDescriptor.DataType.STRING,
          "The family name of the User, or \"Last Name\" in most Western " +
              "languages (e.g. Jensen given the full name Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_GIVEN_NAME =
      AttributeDescriptor.singularSimple("givenName",
          AttributeDescriptor.DataType.STRING,
          "The given name of the User, or \"First Name\" in most Western " +
              "languages (e.g. Barbara given the full name Ms. Barbara Jane " +
              "Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_MIDDLE_NAME =
      AttributeDescriptor.singularSimple("middleName",
          AttributeDescriptor.DataType.STRING,
          "The middle name(s) of the User (e.g. Jane given the full name Ms. " +
              "Barbara Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_HONORIFIC_PREFIX =
      AttributeDescriptor.singularSimple("honorificPrefix",
          AttributeDescriptor.DataType.STRING,
          "The honorific prefix(es) of the User, or \"Title\" in most " +
              "Western languages (e.g. Ms. given the full name Ms. Barbara " +
              "Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME_HONORIFIC_SUFFIX =
      AttributeDescriptor.singularSimple("honorificSuffix",
          AttributeDescriptor.DataType.STRING,
          "The honorific suffix(es) of the User, or \"Suffix\" in most " +
              "Western languages (e.g. III. given the full name Ms. Barbara " +
              "Jane Jensen, III.)",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NAME =
      AttributeDescriptor.singularComplex("name",
          "The components of the User's real name",
          SCIMConstants.SCHEMA_URI_CORE, false, false, NAME_FORMATTED,
          NAME_FAMILY_NAME, NAME_GIVEN_NAME, NAME_MIDDLE_NAME,
          NAME_HONORIFIC_PREFIX, NAME_HONORIFIC_SUFFIX);

  private static final AttributeDescriptor DISPLAY_NAME =
      AttributeDescriptor.singularSimple("displayName",
          AttributeDescriptor.DataType.STRING,
          "The name of the User, suitable for display to end-users",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor NICK_NAME =
      AttributeDescriptor.singularSimple("nickName",
          AttributeDescriptor.DataType.STRING,
          "The casual way to address the user in real life, e.g. \"Bob\" or " +
              "\"Bobby\" instead of \"Robert\"",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PROFILE_URL =
      AttributeDescriptor.singularSimple("profileUrl",
          AttributeDescriptor.DataType.STRING,
          "URL to a page representing the User's online profile",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor TITLE =
      AttributeDescriptor.singularSimple("title",
          AttributeDescriptor.DataType.STRING,
          "The user's title, such as \"Vice President\"",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor USER_TYPE =
      AttributeDescriptor.singularSimple("userType",
          AttributeDescriptor.DataType.STRING,
          "Used to identify the organization to user relationship",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PREFERRED_LANGUAGE =
      AttributeDescriptor.singularSimple("preferredLanguage",
          AttributeDescriptor.DataType.STRING,
          "Used to indicate a User's preferred written or spoken language",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor LOCALE =
      AttributeDescriptor.singularSimple("locale",
          AttributeDescriptor.DataType.STRING,
          "Used to indicate the User's default location for purposes of " +
              "localizing items such as currency, date time format, " +
              "numerical representations, etc",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor TIMEZONE =
      AttributeDescriptor.singularSimple("timezone",
          AttributeDescriptor.DataType.STRING,
          "The User's time zone in the public-domain time zone database format",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ACTIVE =
      AttributeDescriptor.singularSimple("active",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value indicating the User's administrative status",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  //// 6.2.  Plural Attributes ////

  private static final AttributeDescriptor EMAILS =
      AttributeDescriptor.pluralSimple("emails",
          AttributeDescriptor.DataType.STRING,
          "E-mail addresses for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PHONE_NUMBERS =
      AttributeDescriptor.pluralSimple("phoneNumbers",
          AttributeDescriptor.DataType.STRING,
          "Phone numbers for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor IMS =
      AttributeDescriptor.pluralSimple("ims",
          AttributeDescriptor.DataType.STRING,
          "Instant messaging address for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor PHOTOS =
      AttributeDescriptor.pluralSimple("photos",
          AttributeDescriptor.DataType.STRING,
          "URL of a photo of the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  private static final AttributeDescriptor ADDRESS_FORMATTED =
      AttributeDescriptor.singularSimple("formatted",
          AttributeDescriptor.DataType.STRING,
          "The full mailing address, formatted for display or use with a " +
              "mailing label.",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_STREET_ADDRESS =
      AttributeDescriptor.singularSimple("streetAddress",
          AttributeDescriptor.DataType.STRING,
          "The full street address component, which may include house " +
              "number, street name, P.O. box, and multi-line extended street " +
              "address information.",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_LOCALITY =
      AttributeDescriptor.singularSimple("locality",
          AttributeDescriptor.DataType.STRING,
          "The city or locality component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_REGION =
      AttributeDescriptor.singularSimple("region",
          AttributeDescriptor.DataType.STRING,
          "The state or region component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_POSTAL_CODE =
      AttributeDescriptor.singularSimple("postalCode",
          AttributeDescriptor.DataType.STRING,
          "The zipcode or postal code component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESS_COUNTRY =
      AttributeDescriptor.singularSimple("country",
          AttributeDescriptor.DataType.STRING,
          "The country name component",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ADDRESSES =
      AttributeDescriptor.pluralComplex("addresses",
          "A physical mailing address for this User",
          SCIMConstants.SCHEMA_URI_CORE, true, false, null,
          ADDRESS_FORMATTED, ADDRESS_STREET_ADDRESS, ADDRESS_LOCALITY,
          ADDRESS_REGION, ADDRESS_POSTAL_CODE, ADDRESS_COUNTRY);

  private static final AttributeDescriptor GROUPS =
      AttributeDescriptor.pluralSimple("groups",
          AttributeDescriptor.DataType.STRING,
          "A list of groups that the user belongs to",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ENTITLEMENTS =
      AttributeDescriptor.pluralSimple("entitlements",
          AttributeDescriptor.DataType.STRING,
          "A list of entitlements for the User that represent a thing the " +
              "User has",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor ROLES =
      AttributeDescriptor.pluralSimple("entitlements",
          AttributeDescriptor.DataType.STRING,
          "A list of roles for the User that collectively represent who the " +
              "User is",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  //// 7.  SCIM Enterprise User Schema Extension ////

  private static final AttributeDescriptor EMPLOYEE_NUMBER =
      AttributeDescriptor.singularSimple("employeeNumber",
          AttributeDescriptor.DataType.STRING,
          "Numeric or alphanumeric identifier assigned to a person, " +
              "typically based on order of hire or association with an " +
              "organization",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor COST_CENTER =
      AttributeDescriptor.singularSimple("costCenter",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of a cost center",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor ORGANIZATION =
      AttributeDescriptor.singularSimple("organization",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of an organization",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor DIVISION =
      AttributeDescriptor.singularSimple("division",
          AttributeDescriptor.DataType.STRING,
          "dentifies the name of a division",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor DEPARTMENT =
      AttributeDescriptor.singularSimple("department",
          AttributeDescriptor.DataType.STRING,
          "Identifies the name of a department",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);

  private static final AttributeDescriptor MANAGER_ID =
      AttributeDescriptor.singularSimple("managerId",
          AttributeDescriptor.DataType.STRING,
          "The id of the SCIM resource representing the User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor MANAGER_DISPLAY_NAME =
      AttributeDescriptor.singularSimple("displayName",
          AttributeDescriptor.DataType.STRING,
          "The displayName of the User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false, false);
  private static final AttributeDescriptor MANAGER =
      AttributeDescriptor.singularComplex("manager", "The User's manager",
          SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, false, false,
          MANAGER_ID, MANAGER_DISPLAY_NAME);

  //// 8.  SCIM Group Schema ////

  private static final AttributeDescriptor GROUP_DISPLAY_NAME =
      AttributeDescriptor.singularSimple("displayName",
          AttributeDescriptor.DataType.STRING,
          "A human readable name for the Group",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor MEMBERS =
      AttributeDescriptor.pluralSimple("members",
          AttributeDescriptor.DataType.STRING,
          "A list of members of the Group",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false,
          "User", "Group");

  //// 9.  Service Provider Configuration Schema ////

  private static final AttributeDescriptor CONFIG_DOCUMENTATION_URL =
      AttributeDescriptor.singularSimple("documentationUrl",
          AttributeDescriptor.DataType.STRING,
          "An HTTP addressable URL pointing to the Service Provider's human " +
          "consumable help documentation",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor PATCH_SUPPORTED =
      AttributeDescriptor.singularSimple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the PATCH operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_SUPPORTED =
      AttributeDescriptor.singularSimple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the BULK operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_MAX_OPERATIONS =
      AttributeDescriptor.singularSimple("maxOperations",
          AttributeDescriptor.DataType.INTEGER,
          "An integer value specifying the maximum number of resource " +
          "operations in a BULK operation",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor BULK_MAX_PAYLOAD_SIZE =
      AttributeDescriptor.singularSimple("maxPayloadSize",
          AttributeDescriptor.DataType.INTEGER,
          "An integer value specifying the maximum payload size in bytes " +
          "of a BULK operation",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor FILTER_SUPPORTED =
      AttributeDescriptor.singularSimple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the BULK operation is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor FILTER_MAX_RESULTS =
      AttributeDescriptor.singularSimple("maxResults",
          AttributeDescriptor.DataType.INTEGER,
          "Integer value specifying the maximum number of Resources returned " +
          "in a response",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor CHANGE_PASSWORD_SUPPORTED =
      AttributeDescriptor.singularSimple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether the Change Password operation " +
          "is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor SORT_SUPPORTED =
      AttributeDescriptor.singularSimple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether sorting is supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ETAG_SUPPORTED =
      AttributeDescriptor.singularSimple("supported",
          AttributeDescriptor.DataType.BOOLEAN,
          "Boolean value specifying whether Etag resource versions are " +
          "supported",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_NAME =
      AttributeDescriptor.singularSimple("name",
          AttributeDescriptor.DataType.STRING,
          "The common authentication scheme name.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_DESCRIPTION =
      AttributeDescriptor.singularSimple("description",
          AttributeDescriptor.DataType.STRING,
          "A description of the Authentication Scheme.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor AUTH_SCHEME_SPEC_URL =
      AttributeDescriptor.singularSimple("specUrl",
          AttributeDescriptor.DataType.STRING,
          "A HTTP addressable URL pointing to the Authentication Scheme's " +
          "specification.",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor AUTH_SCHEME_DOCUMENTATION_URL =
      AttributeDescriptor.singularSimple("documentationUrl",
          AttributeDescriptor.DataType.STRING,
          "A HTTP addressable URL pointing to the Authentication Scheme's " +
          "usage documentation.",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor PATCH_CONFIG =
      AttributeDescriptor.singularComplex(
          "patch",
          "A complex type that specifies PATCH configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          PATCH_SUPPORTED);
  private static final AttributeDescriptor BULK_CONFIG =
      AttributeDescriptor.singularComplex(
          "bulk",
          "A complex type that specifies BULK configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          BULK_SUPPORTED, BULK_MAX_OPERATIONS, BULK_MAX_PAYLOAD_SIZE);
  private static final AttributeDescriptor FILTER_CONFIG =
      AttributeDescriptor.singularComplex(
          "filter",
          "A complex type that specifies Filter configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          FILTER_SUPPORTED, FILTER_MAX_RESULTS);
  private static final AttributeDescriptor CHANGE_PASSWORD_CONFIG =
      AttributeDescriptor.singularComplex("changePassword",
          "A complex type that specifies Change Password configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          CHANGE_PASSWORD_SUPPORTED);
  private static final AttributeDescriptor SORT_CONFIG =
      AttributeDescriptor.singularComplex("sort",
          "A complex type that specifies Sort configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          SORT_SUPPORTED);
  private static final AttributeDescriptor ETAG_CONFIG =
      AttributeDescriptor.singularComplex("etag",
          "A complex type that specifies Etag configuration options",
          SCIMConstants.SCHEMA_URI_CORE, true, true,
          ETAG_SUPPORTED);
  private static final AttributeDescriptor AUTH_SCHEMES =
      AttributeDescriptor.pluralComplex("authenticationSchemes",
          "A complex type that specifies supported Authentication Scheme " +
          "properties.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, null,
          AUTH_SCHEME_NAME, AUTH_SCHEME_DESCRIPTION, AUTH_SCHEME_SPEC_URL,
          AUTH_SCHEME_DOCUMENTATION_URL);

  //// 10.  Resource Schema ////

  private static final AttributeDescriptor RESOURCE_NAME =
      AttributeDescriptor.singularSimple("name",
          AttributeDescriptor.DataType.STRING,
          "The addressable Resource endpoint name",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_DESCRIPTION =
      AttributeDescriptor.singularSimple("description",
          AttributeDescriptor.DataType.STRING,
          "The Resource's human readable description",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_SCHEMA =
      AttributeDescriptor.singularSimple("schema",
          AttributeDescriptor.DataType.STRING,
          "The Resource's associated schema URN",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor RESOURCE_QUERY_ENDPOINT =
      AttributeDescriptor.singularSimple("queryEndpoint",
          AttributeDescriptor.DataType.STRING,
          "The Resource's HTTP addressable query endpoint relative to the " +
              "Base URL",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);

  private static final AttributeDescriptor ATTRIBUTES_NAME =
      AttributeDescriptor.singularSimple("name",
          AttributeDescriptor.DataType.STRING,
          "The attribute's name", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_TYPE =
      AttributeDescriptor.singularSimple("type",
          AttributeDescriptor.DataType.STRING,
          "The attribute's data type", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_PLURAL =
      AttributeDescriptor.singularSimple("plural",
          AttributeDescriptor.DataType.BOOLEAN,
          "Specifies if the attribute is a plural",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_DESCRIPTION =
      AttributeDescriptor.singularSimple("description",
          AttributeDescriptor.DataType.STRING,
          "The attribute's human readable description",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_SCHEMA =
      AttributeDescriptor.singularSimple("schema",
          AttributeDescriptor.DataType.STRING,
          "The attribute's associated schema", SCIMConstants.SCHEMA_URI_CORE,
          true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_READ_ONLY =
      AttributeDescriptor.singularSimple("readOnly",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the attribute is mutable",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_REQUIRED =
      AttributeDescriptor.singularSimple("required",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the attribute is required",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);
  private static final AttributeDescriptor ATTRIBUTES_CASE_EXACT =
      AttributeDescriptor.singularSimple("caseExact",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value that specifies if the string attribute is case " +
              "sensitive",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false);

  private static final AttributeDescriptor RESOURCE_SUB_ATTRIBUTES =
      AttributeDescriptor.pluralComplex("subAttributes",
          "A list specifying the contained attributes",
          SCIMConstants.SCHEMA_URI_CORE, true, false, null,
          ATTRIBUTES_NAME, ATTRIBUTES_TYPE, ATTRIBUTES_DESCRIPTION,
          ATTRIBUTES_READ_ONLY, ATTRIBUTES_REQUIRED, ATTRIBUTES_CASE_EXACT);
  private static final AttributeDescriptor RESOURCE_PLURAL_TYPES =
      AttributeDescriptor.pluralSimple("pluralTypes",
          AttributeDescriptor.DataType.STRING,
          "A list of canonical type values",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);

  private static final AttributeDescriptor RESOURCE_ATTRIBUTES =
      AttributeDescriptor.pluralComplex("attributes",
          "A complex type that specifies the set of associated " +
              "Resource attributes", SCIMConstants.SCHEMA_URI_CORE, true,
          true, null, ATTRIBUTES_NAME, ATTRIBUTES_TYPE,
          ATTRIBUTES_DESCRIPTION, ATTRIBUTES_SCHEMA,
          ATTRIBUTES_READ_ONLY, ATTRIBUTES_REQUIRED, ATTRIBUTES_CASE_EXACT,
          ATTRIBUTES_PLURAL, RESOURCE_SUB_ATTRIBUTES, RESOURCE_PLURAL_TYPES);

  private static final AttributeDescriptor.AttributeDescriptorResolver
        NESTING_ATTRIBUTES_RESOLVER =
        new AttributeDescriptor.AttributeDescriptorResolver(true);
  static
  {
    SCIMObject scimObject = new SCIMObject();
    scimObject.setAttribute(SCIMAttribute.createSingularAttribute(
        RESOURCE_NAME, SCIMAttributeValue.createStringValue(
        SCIMConstants.RESOURCE_NAME_SCHEMA)));
    scimObject.setAttribute(SCIMAttribute.createSingularAttribute(
        RESOURCE_DESCRIPTION, SCIMAttributeValue.createStringValue(
        "The Resource schema specifies the Attribute(s) and meta-data that " +
            "constitute a Resource")));
    scimObject.setAttribute(SCIMAttribute.createSingularAttribute(
        RESOURCE_SCHEMA, SCIMAttributeValue.createStringValue(
        SCIMConstants.SCHEMA_URI_CORE)));
    scimObject.setAttribute(SCIMAttribute.createSingularAttribute(
        RESOURCE_QUERY_ENDPOINT, SCIMAttributeValue.createStringValue(
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
          fromInstance(RESOURCE_ATTRIBUTES, RESOURCE_QUERY_ENDPOINT);
      entries[7] = NESTING_ATTRIBUTES_RESOLVER.
          fromInstance(RESOURCE_ATTRIBUTES, RESOURCE_ATTRIBUTES);
    }
    catch(InvalidResourceException e)
    {
      // This should not occur as these are all defined here...
      throw new RuntimeException(e);
    }

    scimObject.setAttribute(
        SCIMAttribute.createPluralAttribute(RESOURCE_ATTRIBUTES, entries));


    RESOURCE_SCHEMA_DESCRIPTOR = new ResourceDescriptor(null, scimObject)
    {
      @Override
      public ResourceDescriptor getResourceDescriptor() {
        return this;
      }

      @Override
      public Collection<AttributeDescriptor> getAttributes() {
        return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
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
          "SCIM provides a schema for representing Users",
          SCIMConstants.SCHEMA_URI_CORE, SCIMConstants.RESOURCE_ENDPOINT_USERS,
          USER_NAME, NAME, DISPLAY_NAME, NICK_NAME, PROFILE_URL, TITLE,
          USER_TYPE, PREFERRED_LANGUAGE, LOCALE, TIMEZONE, ACTIVE, EMAILS,
          PHONE_NUMBERS, IMS, PHOTOS, ADDRESSES, GROUPS, ENTITLEMENTS, ROLES,
          EMPLOYEE_NUMBER, COST_CENTER, ORGANIZATION, DIVISION, DEPARTMENT,
          MANAGER);

  /**
   * The SCIM Group Schema.
   */
  public static final ResourceDescriptor GROUP_DESCRIPTOR =
      ResourceDescriptor.create(SCIMConstants.RESOURCE_NAME_GROUP,
          "SCIM provides a schema for representing groups",
          SCIMConstants.SCHEMA_URI_CORE, SCIMConstants.RESOURCE_ENDPOINT_GROUPS,
          GROUP_DISPLAY_NAME, MEMBERS);

  /**
   * The SCIM AttributeDescriptor for the meta attribute.
   */
  public static final AttributeDescriptor META_DESCRIPTOR = META;
}
