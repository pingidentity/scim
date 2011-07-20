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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * This class provides an attribute mapper for plural attributes.
 * Each type of value (e.g. "work", "home") is mapped to one or more
 * single-valued LDAP attributes, where each LDAP attribute holds one
 * of the SCIM sub-attributes.
 */
public class PluralAttributeMapper extends AttributeMapper
{
  /**
   * A set of value mappers for each value of the "type" sub-attribute.
   */
  private final Map<String,PluralValueMapper> valueMappers;

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
   * Create a new instance of a complex attribute mapper.
   *
   * @param scimAttributeType  The SCIM attribute type that is mapped by this
   *                           attribute mapper.
   * @param pluralMappers     The set of complex value mappers for this
   *                           attribute mapper.
   */
  public PluralAttributeMapper(
      final SCIMAttributeType scimAttributeType,
      final Collection<PluralValueMapper> pluralMappers)
  {
    super(scimAttributeType);

    ldapAttributeTypes = new HashSet<String>();
    valueMappers = new HashMap<String, PluralValueMapper>();
    for (final PluralValueMapper pluralMapper : pluralMappers)
    {
      valueMappers.put(pluralMapper.getTypeValue(), pluralMapper);
      for (final ValueMapper m : pluralMapper.getValueMappers())
      {
        ldapAttributeTypes.add(m.getLdapAttributeType());
      }
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
    final String filterOp = filter.getFilterOp();

    String filterValue = filter.getFilterValue();

    final String[] attrPath = filter.getAttributePath();
    if (attrPath.length != 2)
    {
      return Filter.createORFilter();
    }

    final List<ValueMapper> selectedMappers = new ArrayList<ValueMapper>();
    final String subAttribute = attrPath[1];
    for (final PluralValueMapper pluralValueMapper : valueMappers.values())
    {
      for (final ValueMapper m : pluralValueMapper.getValueMappers())
      {
        if (m.getScimAttribute().equalsIgnoreCase(subAttribute))
        {
          selectedMappers.add(m);
        }
      }
    }

    if (selectedMappers.isEmpty())
    {
      return Filter.createORFilter();
    }

    final List<String> ldapFilterTypes =
        new ArrayList<String>(selectedMappers.size());
    final List<String> ldapFilterValues =
        new ArrayList<String>(selectedMappers.size());

    for (final ValueMapper m : selectedMappers)
    {
      String ldapFilterValue = null;
      if (filterValue != null)
      {
        ldapFilterValue = m.toLDAPValue(filterValue);
      }

      ldapFilterTypes.add(m.getLdapAttributeType());
      ldapFilterValues.add(ldapFilterValue);
    }

    final List<Filter> filterComponents =
        new ArrayList<Filter>(ldapFilterTypes.size());
    for (int i = 0; i < ldapFilterTypes.size(); i++)
    {
      final String ldapAttributeType = ldapFilterTypes.get(i);
      final String ldapFilterValue = ldapFilterValues.get(i);

      if (filterOp.equalsIgnoreCase("equals"))
      {
        // This will also return entries that do not match the exact case.
        filterComponents.add(
            Filter.createEqualityFilter(ldapAttributeType,
                                        ldapFilterValue));
      }
      else if (filterOp.equalsIgnoreCase("equalsIgnoreCase"))
      {
        // This only works if the mapped LDAP attribute is case-ignore.
        filterComponents.add(
            Filter.createEqualityFilter(ldapAttributeType,
                                        ldapFilterValue));
      }
      else if (filterOp.equalsIgnoreCase("contains"))
      {
        filterComponents.add(
            Filter.createSubstringFilter(ldapAttributeType,
                                         null,
                                         new String[] { ldapFilterValue },
                                         null));
      }
      else if (filterOp.equalsIgnoreCase("startswith"))
      {
        filterComponents.add(
            Filter.createSubstringFilter(ldapAttributeType,
                                         ldapFilterValue,
                                         null,
                                         null));
      }
      else if (filterOp.equalsIgnoreCase("present"))
      {
        filterComponents.add(Filter.createPresenceFilter(ldapAttributeType));
      }
      else
      {
        throw new RuntimeException(
            "Filter op " + filterOp + " is not supported");
      }
    }

    if (filterComponents.size() == 1)
    {
      return filterComponents.get(0);
    }
    else
    {
      return Filter.createORFilter(filterComponents);
    }
  }



  @Override
  public String toLDAPSortAttributeType()
  {
    return null;
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
      final AttributeDescriptor descriptor =
          schema.getAttribute(getSCIMAttributeType().getName());

      for (SCIMAttributeValue v : scimAttribute.getPluralValues())
      {
        final AttributeDescriptor complexDescriptor =
            descriptor.getComplexAttributeDescriptors().get(0);
        final SCIMAttribute attribute =
            v.getAttribute(complexDescriptor.getName());
        if (attribute != null)
        {
          v = attribute.getSingularValue();
        }

        final SCIMAttribute typeAttr = v.getAttribute("type");
        if (typeAttr != null)
        {
          final String type = typeAttr.getSingularValue().getStringValue();

          final PluralValueMapper pluralValueMapper = valueMappers.get(type);
          if (pluralValueMapper != null)
          {
            for (final ValueMapper valueMapper :
                pluralValueMapper.getValueMappers())
            {
              final String scimType = valueMapper.getScimAttribute();
              final String ldapType = valueMapper.getLdapAttributeType();

              final SCIMAttribute subAttribute = v.getAttribute(scimType);
              if (subAttribute != null)
              {
                final String stringValue =
                    valueMapper.toLDAPValue(
                        subAttribute.getSingularValue());
                attributes.add(new Attribute(ldapType, stringValue));
              }
            }
          }
        }
      }

    }
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry)
  {
    final AttributeDescriptor descriptor =
        schema.getAttribute(getSCIMAttributeType().getName());
    final AttributeDescriptor complexDescriptor =
        descriptor.getComplexAttributeDescriptors().get(0);

    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();
    for (final PluralValueMapper pluralValueMapper : valueMappers.values())
    {
      final List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>();

      for (final ValueMapper valueMapper : pluralValueMapper.getValueMappers())
      {
        final String scimType = valueMapper.getScimAttribute();
        final String ldapType = valueMapper.getLdapAttributeType();

        final String stringValue = entry.getAttributeValue(ldapType);
        if (stringValue != null)
        {
          final SCIMAttributeValue scimValue =
              valueMapper.toSCIMValue(stringValue);
          subAttributes.add(SCIMAttribute.createSingularAttribute(
              complexDescriptor.getAttribute(scimType), scimValue));
        }
      }

      if (!subAttributes.isEmpty())
      {
        subAttributes.add(SCIMAttribute.createSingularAttribute(
            complexDescriptor.getAttribute("type"),
            SCIMAttributeValue.createStringValue(
                pluralValueMapper.getTypeValue())));

        final SCIMAttributeValue complexValue =
            SCIMAttributeValue.createComplexValue(subAttributes);
        final SCIMAttribute attribute =
            SCIMAttribute.createSingularAttribute(complexDescriptor,
                                                  complexValue);

        values.add(SCIMAttributeValue.createComplexValue(attribute));
      }
    }

    if (values.isEmpty())
    {
      return null;
    }
    else
    {
      return SCIMAttribute.createPluralAttribute(
          descriptor, values.toArray(new SCIMAttributeValue[values.size()]));
    }
  }
}
