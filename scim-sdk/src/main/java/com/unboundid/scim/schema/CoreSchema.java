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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Contains the resources and their attributes defined in
 * Core Schema 1.1.
 */
public class CoreSchema
{
  //// 3.  SCIM Schema Structure ////
  private static final AttributeDescriptor MULTIVALUED_PRIMARY =
      AttributeDescriptor.createSubAttribute("primary",
          AttributeDescriptor.DataType.BOOLEAN,
          "A Boolean value indicating the 'primary' or preferred attribute " +
              "value for this attribute",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);
  private static final AttributeDescriptor MULTIVALUED_DISPLAY =
      AttributeDescriptor.createSubAttribute("display",
          AttributeDescriptor.DataType.STRING,
          "A human readable name, primarily used for display purposes",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false);
  private static final AttributeDescriptor MULTIVALUED_OPERATION =
      AttributeDescriptor.createSubAttribute("operation",
          AttributeDescriptor.DataType.STRING,
          "The operation to perform on the multi-valued attribute during a " +
              "PATCH request",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false);

  /**
   * Adds the default sub-attributes for multi-valued attributes if they don't
   * already exist. This will include the type, primary, display and operation
   * attributes.
   *
   * @param schema          The schema of the multi-valued attribute.
   * @param dataType        The data type of the value sub-attribute.
   * @param canonicalValues The list of canonical values for the type attribute.
   * @param subAttributes  A list specifying the sub attributes of the complex
   *                       attribute.
   * @return The default sub-attributes for multi-valued attributes.
   */
  static AttributeDescriptor[] addCommonMultiValuedSubAttributes(
      final String schema,
      final AttributeDescriptor.DataType dataType,
      final String[] canonicalValues,
      final AttributeDescriptor... subAttributes)
  {
    final AttributeDescriptor type = AttributeDescriptor.createSubAttribute(
        "type", AttributeDescriptor.DataType.STRING, "A label indicating the " +
        "attribute's function; e.g., \"work\" or " + "\"home\"",
        schema, false, false, false, canonicalValues);

    final AttributeDescriptor value = AttributeDescriptor.createSubAttribute(
        "value", dataType, "The attribute's significant value",
        schema, false, true, false);

    int numSubAttributes = 0;
    boolean displayExists = false;
    boolean primaryExists = false;
    boolean typeExists = false;
    boolean operationExists = false;
    boolean valueExists = false;
    if (subAttributes != null)
    {
      numSubAttributes = subAttributes.length;

      for(AttributeDescriptor attribute : subAttributes)
      {
        if(attribute.equals(MULTIVALUED_DISPLAY))
        {
          displayExists = true;
        }
        else if(attribute.equals(MULTIVALUED_PRIMARY))
        {
          primaryExists = true;
        }
        else if(attribute.equals(type))
        {
          typeExists = true;
        }
        else if(attribute.equals(MULTIVALUED_OPERATION))
        {
          operationExists = true;
        }
        else if(attribute.equals(value))
        {
          valueExists = true;
        }
      }
    }

    final List<AttributeDescriptor> allSubAttributes =
        new ArrayList<AttributeDescriptor>(numSubAttributes + 5);
    if (dataType != AttributeDescriptor.DataType.COMPLEX)
    {
      if(!valueExists)
      {
        allSubAttributes.add(value);
      }
    }

    if(!displayExists)
    {
      allSubAttributes.add(MULTIVALUED_DISPLAY);
    }
    if(!primaryExists)
    {
      allSubAttributes.add(MULTIVALUED_PRIMARY);
    }
    if(!typeExists)
    {
      allSubAttributes.add(type);
    }
    if(!operationExists)
    {
      allSubAttributes.add(MULTIVALUED_OPERATION);
    }

    if (numSubAttributes > 0)
    {
      allSubAttributes.addAll(Arrays.asList(subAttributes));
    }

    return allSubAttributes.toArray(
        new AttributeDescriptor[allSubAttributes.size()]);
  }

  /**
   * Adds the common resource attributes if they don't already exist.
   * This will include id, externalId, and meta.
   *
   * @param attributes A list specifying the attributes of a resource.
   * @return The list of attributes including the common attributes.
   */
  static List<AttributeDescriptor> addCommonResourceAttributes(
      final AttributeDescriptor... attributes)
  {
    int numAttributes = 0;
    boolean idExists = false;
    boolean metaExists = false;
    boolean externalIdExists = false;
    if (attributes != null)
    {
      numAttributes = attributes.length;

      for(AttributeDescriptor attribute : attributes)
      {
        if(attribute.equals(ID))
        {
          idExists = true;
        }
        else if(attribute.equals(META))
        {
          metaExists = true;
        }
        else if(attribute.equals(EXTERNAL_ID))
        {
          externalIdExists = true;
        }
      }
    }

    final List<AttributeDescriptor> attributeList =
        new ArrayList<AttributeDescriptor>(numAttributes + 3);

    // These attributes need to be in the same order as defined in the SCIM XML
    // schema scim-core.xsd.
    if(!idExists)
    {
      attributeList.add(ID);
    }
    if(!metaExists)
    {
      attributeList.add(META);
    }
    if(!externalIdExists)
    {
      attributeList.add(EXTERNAL_ID);
    }
    if (numAttributes > 0)
    {
      attributeList.addAll(Arrays.asList(attributes));
    }
    return attributeList;
  }

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
          "email", AttributeDescriptor.DataType.STRING,
          "E-mail addresses for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          new String[] {"work", "home", "other"});
  private static final AttributeDescriptor PHONE_NUMBERS =
      AttributeDescriptor.createMultiValuedAttribute("phoneNumbers",
          "phoneNumber", AttributeDescriptor.DataType.STRING,
          "Phone numbers for the User",
          SCIMConstants.SCHEMA_URI_CORE,
          false, false, false, new String[] {"fax", "pager", "other"});
  private static final AttributeDescriptor IMS =
      AttributeDescriptor.createMultiValuedAttribute("ims",
          "im", AttributeDescriptor.DataType.STRING,
          "Instant messaging address for the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          new String[] {"aim", "gtalk", "icq", "xmpp", "msn", "skype", "qq",
              "yahoo"});
  private static final AttributeDescriptor PHOTOS =
      AttributeDescriptor.createMultiValuedAttribute("photos",
          "photo", AttributeDescriptor.DataType.STRING,
          "URL of photos of the User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          new String[] {"photo", "thumbnail"});

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
          "address", AttributeDescriptor.DataType.COMPLEX,
          "A physical mailing address for this User",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          new String[]{"work", "home", "other"},
          ADDRESS_FORMATTED, ADDRESS_STREET_ADDRESS, ADDRESS_LOCALITY,
          ADDRESS_REGION, ADDRESS_POSTAL_CODE, ADDRESS_COUNTRY);

  private static final AttributeDescriptor GROUPS =
      AttributeDescriptor.createMultiValuedAttribute("groups",
          "group", AttributeDescriptor.DataType.STRING,
          "A list of groups that the user belongs to",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          new String[] {"direct", "indirect"});
  private static final AttributeDescriptor ENTITLEMENTS =
      AttributeDescriptor.createMultiValuedAttribute("entitlements",
          "entitlement", AttributeDescriptor.DataType.STRING,
          "A list of entitlements for the User that represent a thing the " +
              "User has. That is, an entitlement is an additional right to a " +
              "thing, object or service",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false, null);
  private static final AttributeDescriptor ROLES =
      AttributeDescriptor.createMultiValuedAttribute("roles",
          "role", AttributeDescriptor.DataType.STRING,
          "A list of roles for the User that collectively represent who the " +
              "User is",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false, null);
  private static final AttributeDescriptor X509CERTIFICATES =
      AttributeDescriptor.createMultiValuedAttribute("x509Certificates",
          "x509Certificate", AttributeDescriptor.DataType.BINARY,
          "A list of certificates issued to the User. Values are DER " +
              "encoded x509.",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false, null);

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
          "member", AttributeDescriptor.DataType.STRING,
          "A list of members of the Group",
          SCIMConstants.SCHEMA_URI_CORE, false, false, false,
          new String[] {"User", "Group"});

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
          "authenticationScheme", AttributeDescriptor.DataType.COMPLEX,
          "A complex type that specifies supported Authentication Scheme " +
              "properties.",
          SCIMConstants.SCHEMA_URI_CORE, true, true, false,
          new String[]{"OAuth", "OAuth2", "HttpBasic", "httpDigest"},
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
          "canonicalValue", AttributeDescriptor.DataType.STRING,
          "A collection of canonical values",
          SCIMConstants.SCHEMA_URI_CORE, true, false, false, null);

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
          CONFIG_DOCUMENTATION_URL, PATCH_CONFIG, BULK_CONFIG, FILTER_CONFIG,
          CHANGE_PASSWORD_CONFIG, SORT_CONFIG, ETAG_CONFIG, AUTH_SCHEMES,
          XML_DATA_TYPE_CONFIG);

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
          ROLES, X509CERTIFICATES, EMPLOYEE_NUMBER, COST_CENTER, ORGANIZATION,
          DIVISION, DEPARTMENT, MANAGER);

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

  /**
   * The SCIM AttributeDescriptor for the id attribute.
   */
  public static final AttributeDescriptor ID_DESCRIPTOR = ID;

  /**
   * The SCIM AttributeDescriptor for the externalId attribute.
   */
  public static final AttributeDescriptor EXTERNAL_ID_DESCRIPTOR = EXTERNAL_ID;
}
