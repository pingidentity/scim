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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;



/**
 * This class provides a mapping between a Group in the SCIM core schema and
 * an LDAP static group. Nested groups are allowed but this mapping does not
 * preserve the SCIM member type ("user" or "group").
 */
public class GroupResourceMapper extends ResourceMapper
{
  private ConfigurableResourceMapper resourceMapper;



  @Override
  public void initializeMapper()
  {
    final String[] objectClassValues =
        new String[]{"top", "groupOfUniqueNames" };

    final List<ValueMapper> memberValueMappers =
        new ArrayList<ValueMapper>();
    memberValueMappers.add(new ValueMapper("value", "uniqueMember"));

    final List<PluralValueMapper> memberPluralValueMappers =
        new ArrayList<PluralValueMapper>();
    memberPluralValueMappers.add(
        new PluralValueMapper(null, memberValueMappers));

    final List<AttributeMapper> attributeMappers =
        new ArrayList<AttributeMapper>();

    attributeMappers.add(
        new PluralAttributeMapper(
            new SCIMAttributeType("members"), memberPluralValueMappers));
    attributeMappers.add(
        new SimpleAttributeMapper(
            new SCIMAttributeType("displayName"), "cn"));

    resourceMapper =
        new ConfigurableResourceMapper("Group",
                                       "groupOfUniqueNames",
                                       objectClassValues,
                                       "cn",
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
