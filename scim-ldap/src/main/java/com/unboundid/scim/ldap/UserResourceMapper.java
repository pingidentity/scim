/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SortParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.unboundid.scim.sdk.SCIMConstants.
    SCHEMA_URI_ENTERPRISE_EXTENSION;



/**
 * This class provides a mapping between a User in the SCIM core schema and
 * the LDAP inetOrgPerson object class. The specific attribute mappings are
 * fixed and can not be changed through configuration. This mapping is not able
 * to preserve all information from SCIM to LDAP, nor from LDAP to SCIM.
 *
 * The following SCIM core attributes are mapped:
 * userName, name, addresses, emails, phoneNumbers, displayName,
 * preferredLanguage, title.
 *
 * The following enterprise extension attributes are mapped:
 * employeeNumber, organization, division, department, manager.
 *
 * These SCIM attributes are not currently mapped: externalId, nickName,
 * profileUrl, userType, locale, utcOffset, ims, photos, memberOf.
 */
public class UserResourceMapper extends ResourceMapper
{
  private ConfigurableResourceMapper resourceMapper;



  @Override
  public void initializeMapper()
  {
    final String[] objectClassValues =
        new String[]
            { "top", "person", "organizationalPerson", "inetOrgPerson" };

    final List<ValueMapper> nameValueMappers = new ArrayList<ValueMapper>();
    nameValueMappers.add(new ValueMapper("formatted", "cn"));
    nameValueMappers.add(new ValueMapper("familyName", "sn"));
    nameValueMappers.add(new ValueMapper("givenName", "givenName"));

    final List<ValueMapper> workAddressValueMappers =
        new ArrayList<ValueMapper>();
    workAddressValueMappers.add(
        new FormattedAddressValueMapper("formatted", "postalAddress"));
    workAddressValueMappers.add(new ValueMapper("locality", "l"));
    workAddressValueMappers.add(new ValueMapper("postalCode", "postalCode"));
    workAddressValueMappers.add(new ValueMapper("region", "st"));
    workAddressValueMappers.add(new ValueMapper("streetAddress", "street"));

    final List<ValueMapper> homeAddressValueMappers =
        new ArrayList<ValueMapper>();
    homeAddressValueMappers.add(
        new FormattedAddressValueMapper("formatted", "homePostalAddress"));

    final List<PluralValueMapper> addressPluralValueMappers =
        new ArrayList<PluralValueMapper>();
    addressPluralValueMappers.add(
        new PluralValueMapper("work", workAddressValueMappers));
    addressPluralValueMappers.add(
        new PluralValueMapper("home", homeAddressValueMappers));

    final List<ValueMapper> workEmailValueMappers =
        new ArrayList<ValueMapper>();
    workEmailValueMappers.add(new ValueMapper("value", "mail"));

    final List<PluralValueMapper> emailPluralValueMappers =
        new ArrayList<PluralValueMapper>();
    emailPluralValueMappers.add(
        new PluralValueMapper("work", workEmailValueMappers));

    final List<ValueMapper> workPhoneValueMappers =
        new ArrayList<ValueMapper>();
    workPhoneValueMappers.add(new ValueMapper("value", "telephoneNumber"));

    final List<ValueMapper> homePhoneValueMappers =
        new ArrayList<ValueMapper>();
    homePhoneValueMappers.add(new ValueMapper("value", "homePhone"));

    final List<ValueMapper> faxPhoneValueMappers =
        new ArrayList<ValueMapper>();
    faxPhoneValueMappers.add(new ValueMapper("value",
                                             "facsimileTelephoneNumber"));

    final List<PluralValueMapper> phonePluralValueMappers =
        new ArrayList<PluralValueMapper>();
    phonePluralValueMappers.add(
        new PluralValueMapper("work", workPhoneValueMappers));
    phonePluralValueMappers.add(
        new PluralValueMapper("home", homePhoneValueMappers));
    phonePluralValueMappers.add(
        new PluralValueMapper("fax", faxPhoneValueMappers));

    final List<ValueMapper> managerValueMappers = new ArrayList<ValueMapper>();
    managerValueMappers.add(new ValueMapper("managerId", "manager"));

    final List<AttributeMapper> attributeMappers =
        new ArrayList<AttributeMapper>();

    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType("userName"), "uid"));
    attributeMappers.add(
        new ComplexSingularAttributeMapper(
            new SCIMAttributeType("name"), "familyName", nameValueMappers));
    attributeMappers.add(
        new PluralAttributeMapper(
            new SCIMAttributeType("addresses"), addressPluralValueMappers));
    attributeMappers.add(
        new PluralAttributeMapper(
            new SCIMAttributeType("emails"), emailPluralValueMappers));
    attributeMappers.add(
        new PluralAttributeMapper(
            new SCIMAttributeType("phoneNumbers"), phonePluralValueMappers));
    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType("displayName"), "displayName"));
    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType("preferredLanguage"), "preferredLanguage"));
    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType("title"), "title"));
    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType(SCHEMA_URI_ENTERPRISE_EXTENSION,
                                  "employeeNumber"), "employeeNumber"));
    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType(SCHEMA_URI_ENTERPRISE_EXTENSION,
                                  "organization"), "o"));
    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType(SCHEMA_URI_ENTERPRISE_EXTENSION,
                                  "division"), "ou"));
    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType(SCHEMA_URI_ENTERPRISE_EXTENSION,
                                  "department"), "departmentNumber"));
    attributeMappers.add(
        new ComplexSingularAttributeMapper(
            new SCIMAttributeType(SCHEMA_URI_ENTERPRISE_EXTENSION,
                                  "manager"), "displayName",
            managerValueMappers));

    resourceMapper =
        new ConfigurableResourceMapper("User",
                                       "inetOrgPerson",
                                       objectClassValues,
                                       "uid",
                                       attributeMappers);
  }



  @Override
  public void finalizeMapper()
  {
    // No implementation required.
  }



  @Override
  public boolean supportsCreate()
  {
    return resourceMapper.supportsCreate();
  }



  @Override
  public Set<String> toLDAPAttributeTypes(
      final SCIMQueryAttributes queryAttributes)
  {
    return resourceMapper.toLDAPAttributeTypes(queryAttributes);
  }



  @Override
  public Entry toLDAPEntry(final SCIMObject scimObject, final String baseDN)
      throws LDAPException
  {
    return resourceMapper.toLDAPEntry(scimObject, baseDN);
  }



  @Override
  public List<Attribute> toLDAPAttributes(final SCIMObject scimObject)
  {
    return resourceMapper.toLDAPAttributes(scimObject);
  }



  @Override
  public List<Modification> toLDAPModifications(final Entry currentEntry,
                                                final SCIMObject scimObject)
  {
    return resourceMapper.toLDAPModifications(currentEntry, scimObject);
  }



  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
  {
    return resourceMapper.toLDAPFilter(filter);
  }



  @Override
  public Control toLDAPSortControl(final SortParameters sortParameters)
  {
    return resourceMapper.toLDAPSortControl(sortParameters);
  }



  @Override
  public List<SCIMAttribute> toSCIMAttributes(
      final String resourceName,
      final Entry entry,
      final SCIMQueryAttributes queryAttributes)
  {
    return resourceMapper.toSCIMAttributes(resourceName, entry,
                                           queryAttributes);
  }



  @Override
  public SCIMObject toSCIMObject(final Entry entry,
                                 final SCIMQueryAttributes queryAttributes)
  {
    return resourceMapper.toSCIMObject(entry, queryAttributes);
  }
}
