/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.Schema;
import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * This class provides an attribute mapper that maps a singular complex SCIM
 * attribute to several single-valued LDAP attributes, where each LDAP
 * attribute holds one of the SCIM sub-attributes.
 */
public class ComplexSingularAttributeMapper extends AttributeMapper
{
  /**
   * The value mappers for this attribute mapper.
   */
  private final List<ValueMapper> valueMappers;

  /**
   * The LDAP attribute to be used as a sort key when a sort parameter
   * references the SCIM attribute type.
   */
  private final String sortKeyAttributeType;

  /**
   * The set of LDAP attributes that are mapped by this attribute mapper.
   */
  private final Set<String> ldapAttributeTypes;

  /**
   * The SCIM schema for the SCIM attribute type that is mapped by this
   * attribute mapper.
   */
  private final Schema schema;



  /**
   * Create a new instance of a complex singular attribute mapper.
   *
   * @param scimAttributeType  The SCIM attribute type that is mapped by this
   *                           attribute mapper.
   * @param sortAttribute      The SCIM sub-attribute that is implied when a
   *                           sort parameter references the SCIM attribute
   *                           type.
   * @param valueMappers       value mappers for this attribute mapper.
   */
  public ComplexSingularAttributeMapper(
      final SCIMAttributeType scimAttributeType,
      final String sortAttribute,
      final List<ValueMapper> valueMappers)
  {
    super(scimAttributeType);

    this.valueMappers = valueMappers;
    this.sortKeyAttributeType = sortAttribute;

    ldapAttributeTypes = new HashSet<String>();
    for (final ValueMapper m : valueMappers)
    {
      ldapAttributeTypes.add(m.getLdapAttributeType());
    }

    final SchemaManager schemaManager = SchemaManager.instance();
    schema = schemaManager.getSchema(scimAttributeType.getSchema());
  }



  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return ldapAttributeTypes;
  }



  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
  {
    final SCIMFilterType filterType = filter.getFilterType();

    String filterValue = filter.getFilterValue();

    final String subAttributeName =
        filter.getFilterAttribute().getSubAttributeName();
    if (subAttributeName == null)
    {
      return Filter.createORFilter();
    }

    ValueMapper valueMapper = null;
    for (final ValueMapper m : valueMappers)
    {
      if (m.getScimAttribute().equalsIgnoreCase(subAttributeName))
      {
        valueMapper = m;
      }
    }

    if (valueMapper == null)
    {
      return Filter.createORFilter();
    }

    final String ldapAttributeType = valueMapper.getLdapAttributeType();

    switch (filterType)
    {
      case EQUALITY:
      {
        final String ldapFilterValue = valueMapper.toLDAPValue(filterValue);
        return Filter.createEqualityFilter(ldapAttributeType,
                                           ldapFilterValue);
      }

      case CONTAINS:
      {
        final String ldapFilterValue = valueMapper.toLDAPValue(filterValue);
        return Filter.createSubstringFilter(ldapAttributeType,
                                            null,
                                            new String[] { ldapFilterValue },
                                            null);
      }

      case STARTS_WITH:
      {
        final String ldapFilterValue = valueMapper.toLDAPValue(filterValue);
        return Filter.createSubstringFilter(ldapAttributeType,
                                            ldapFilterValue,
                                            null,
                                            null);
      }

      case PRESENCE:
      {
        return Filter.createPresenceFilter(ldapAttributeType);
      }

      case GREATER_THAN:
      case GREATER_OR_EQUAL:
      {
        final String ldapFilterValue = valueMapper.toLDAPValue(filterValue);
        return Filter.createGreaterOrEqualFilter(ldapAttributeType,
                                                 ldapFilterValue);
      }

      case LESS_THAN:
      case LESS_OR_EQUAL:
      {
        final String ldapFilterValue = valueMapper.toLDAPValue(filterValue);
        return Filter.createLessOrEqualFilter(ldapAttributeType,
                                              ldapFilterValue);
      }

      default:
        throw new RuntimeException(
            "Filter type " + filterType + " is not supported");
    }
  }



  @Override
  public String toLDAPSortAttributeType()
  {
    return sortKeyAttributeType;
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
      final SCIMAttributeValue value = scimAttribute.getSingularValue();

      for (final ValueMapper valueMapper : valueMappers)
      {
        final String scimType = valueMapper.getScimAttribute();
        final String ldapType = valueMapper.getLdapAttributeType();

        final SCIMAttribute subAttribute = value.getAttribute(scimType);
        if (subAttribute != null)
        {
          final String s = subAttribute.getSingularValue().getStringValue();
          attributes.add(new Attribute(ldapType, s));
        }
      }
    }
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry)
  {
    final AttributeDescriptor descriptor =
        schema.getAttribute(getSCIMAttributeType().getName());

    final List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>();

    for (final ValueMapper valueMapper : valueMappers)
    {
      final String scimType = valueMapper.getScimAttribute();
      final String ldapType = valueMapper.getLdapAttributeType();

      final String value = entry.getAttributeValue(ldapType);
      if (value != null)
      {
        subAttributes.add(
            SCIMAttribute.createSingularAttribute(
                descriptor.getAttribute(scimType),
                SCIMAttributeValue.createStringValue(value)));
      }
    }

    if (subAttributes.isEmpty())
    {
      return null;
    }

    final SCIMAttributeValue complexValue =
        SCIMAttributeValue.createComplexValue(subAttributes);
    return SCIMAttribute.createSingularAttribute(descriptor, complexValue);
  }
}
