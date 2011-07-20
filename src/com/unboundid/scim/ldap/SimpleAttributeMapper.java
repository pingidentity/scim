/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.scim.config.Schema;
import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;



/**
 * This class provides an attribute mapper that maps a singular
 * simple SCIM attribute to a single-valued LDAP attribute.
 */
public class SimpleAttributeMapper extends AttributeMapper
{
  /**
   * The LDAP attribute that is mapped by this attribute mapper.
   */
  private final String ldapAttributeType;



  /**
   * The SCIM schema for the SCIM attribute type that is mapped by this
   * attribute mapper.
   */
  private final Schema schema;



  /**
   * Create a new instance of a simple attribute mapper.
   *
   * @param scimAttributeType  The SCIM attribute type that is mapped by this
   *                           attribute mapper.
   * @param ldapAttributeType  The LDAP attribute that is mapped by this
   *                           attribute mapper.
   */
  public SimpleAttributeMapper(final SCIMAttributeType scimAttributeType,
                               final String ldapAttributeType)
  {
    super(scimAttributeType);
    this.ldapAttributeType = ldapAttributeType;

    final SchemaManager schemaManager = SchemaManager.instance();
    schema = schemaManager.getSchema(scimAttributeType.getSchema());
  }



  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return Collections.singleton(ldapAttributeType);
  }



  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
  {
    final String filterOp = filter.getFilterOp();
    final String filterValue = filter.getFilterValue();

    if (filterOp.equalsIgnoreCase("equals"))
    {
      // This will also return entries that do not match the exact case.
      return Filter.createEqualityFilter(ldapAttributeType, filterValue);
    }
    else if (filterOp.equalsIgnoreCase("equalsIgnoreCase"))
    {
      // This only works if the mapped LDAP attribute is case-ignore.
      return Filter.createEqualityFilter(ldapAttributeType, filterValue);
    }
    else if (filterOp.equalsIgnoreCase("contains"))
    {
      return Filter.createSubstringFilter(ldapAttributeType,
                                          null,
                                          new String[] { filterValue },
                                          null);
    }
    else if (filterOp.equalsIgnoreCase("startswith"))
    {
      return Filter.createSubstringFilter(ldapAttributeType,
                                          filterValue,
                                          null,
                                          null);
    }
    else if (filterOp.equalsIgnoreCase("present"))
    {
      return Filter.createPresenceFilter(ldapAttributeType);
    }
    else
    {
      throw new RuntimeException(
          "Filter op " + filterOp + " is not supported");
    }
  }



  @Override
  public String toLDAPSortAttributeType()
  {
    return ldapAttributeType;
  }



  @Override
  public void toLDAPAttributes(final SCIMObject scimObject,
                               final Collection<Attribute> attributes)
  {
    final SCIMAttribute scimAttribute =
        scimObject.getAttribute(getSCIMAttributeType().getSchema(),
                                getSCIMAttributeType().getName());
    if (scimAttribute != null)
    {
      attributes.add(
          new Attribute(ldapAttributeType,
                        scimAttribute.getSingularValue().getStringValue()));
    }
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry)
  {
    final String value = entry.getAttributeValue(ldapAttributeType);
    if (value != null)
    {
      return SCIMAttribute.createSingularAttribute(
          schema.getAttribute(getSCIMAttributeType().getName()),
          SCIMAttributeValue.createStringValue(value));
    }

    return null;
  }
}
