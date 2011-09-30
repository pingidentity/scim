/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.SimpleValue;

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
   * Create a new instance of a complex attribute mapper.
   *
   * @param scimAttributeType  The SCIM attribute type that is mapped by this
   *                           attribute mapper.
   * @param pluralMappers      The set of complex value mappers for this
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
      for (final SubAttributeTransformation sat :
          pluralMapper.getTransformations())
      {
        ldapAttributeTypes.add(
            sat.getAttributeTransformation().getLdapAttribute());
      }
    }
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

    String subAttributeName =
        filter.getFilterAttribute().getSubAttributeName();
    if (subAttributeName == null)
    {
      subAttributeName = "value";
    }

    final List<SubAttributeTransformation> selectedTransformations =
        new ArrayList<SubAttributeTransformation>();
    for (final PluralValueMapper pluralValueMapper : valueMappers.values())
    {
      for (final SubAttributeTransformation sat :
          pluralValueMapper.getTransformations())
      {
        if (sat.getSubAttribute().equalsIgnoreCase(subAttributeName))
        {
          selectedTransformations.add(sat);
        }
      }
    }

    if (selectedTransformations.isEmpty())
    {
      return Filter.createORFilter();
    }

    final List<String> ldapFilterTypes =
        new ArrayList<String>(selectedTransformations.size());
    final List<String> ldapFilterValues =
        new ArrayList<String>(selectedTransformations.size());

    for (final SubAttributeTransformation sat : selectedTransformations)
    {
      final AttributeTransformation at = sat.getAttributeTransformation();
      final String filterValue;
      if (filter.getFilterValue() != null)
      {
        filterValue = at.getTransformation().toLDAPFilterValue(
            filter.getFilterValue());
      }
      else
      {
        filterValue = null;
      }

      ldapFilterTypes.add(at.getLdapAttribute());
      ldapFilterValues.add(filterValue);
    }

    final List<Filter> filterComponents =
        new ArrayList<Filter>(ldapFilterTypes.size());
    for (int i = 0; i < ldapFilterTypes.size(); i++)
    {
      final String ldapAttributeType = ldapFilterTypes.get(i);
      final String ldapFilterValue = ldapFilterValues.get(i);

      switch (filterType)
      {
        case EQUALITY:
        {
          filterComponents.add(
              Filter.createEqualityFilter(ldapAttributeType,
                                          ldapFilterValue));
        }
        break;

        case CONTAINS:
        {
          filterComponents.add(
              Filter.createSubstringFilter(ldapAttributeType,
                                           null,
                                           new String[] { ldapFilterValue },
                                           null));
        }
        break;

        case STARTS_WITH:
        {
          filterComponents.add(
              Filter.createSubstringFilter(ldapAttributeType,
                                           ldapFilterValue,
                                           null,
                                           null));
        }
        break;

        case PRESENCE:
        {
          filterComponents.add(Filter.createPresenceFilter(ldapAttributeType));
        }
        break;

        case GREATER_THAN:
        case GREATER_OR_EQUAL:
        {
          return Filter.createGreaterOrEqualFilter(ldapAttributeType,
                                                   ldapFilterValue);
        }

        case LESS_THAN:
        case LESS_OR_EQUAL:
        {
          return Filter.createLessOrEqualFilter(ldapAttributeType,
                                                ldapFilterValue);
        }

        default:
          throw new RuntimeException(
              "Filter type " + filterType + " is not supported");
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
          getSchema().getAttribute(getSCIMAttributeType().getName());

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
        PluralValueMapper pluralValueMapper = null;
        if (typeAttr != null)
        {
          final String type = typeAttr.getSingularValue().getStringValue();

          pluralValueMapper = valueMappers.get(type);
        }

        // Check for a default mapper.
        if (pluralValueMapper == null)
        {
          pluralValueMapper = valueMappers.get(null);
        }

        if (pluralValueMapper != null)
        {
          for (final SubAttributeTransformation sat :
              pluralValueMapper.getTransformations())
          {
            final AttributeTransformation at = sat.getAttributeTransformation();
            final String scimType = sat.getSubAttribute();
            final String ldapType = at.getLdapAttribute();

            final SCIMAttribute subAttribute = v.getAttribute(scimType);
            if (subAttribute != null)
            {
              final AttributeDescriptor subDescriptor =
                  complexDescriptor.getAttribute(scimType);
              final ASN1OctetString value = at.getTransformation().toLDAPValue(
                  subDescriptor, subAttribute.getSingularValue().getValue());
              attributes.add(new Attribute(ldapType, value));
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
        getSchema().getAttribute(getSCIMAttributeType().getName());
    final AttributeDescriptor complexDescriptor =
        descriptor.getComplexAttributeDescriptors().get(0);

    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();
    for (final PluralValueMapper pluralValueMapper : valueMappers.values())
    {
      if (pluralValueMapper.getTypeValue() != null)
      {
        final List<SCIMAttribute> subAttributes =
            new ArrayList<SCIMAttribute>();

        for (final SubAttributeTransformation sat :
            pluralValueMapper.getTransformations())
        {
          final AttributeTransformation at = sat.getAttributeTransformation();
          final String scimType = sat.getSubAttribute();
          final String ldapType = at.getLdapAttribute();

          final AttributeDescriptor subDescriptor =
              complexDescriptor.getAttribute(scimType);
          final Attribute a = entry.getAttribute(ldapType);
          if (a != null)
          {
            final ASN1OctetString[] rawValues = a.getRawValues();
            if (rawValues.length > 0)
            {
              final SimpleValue simpleValue =
                  at.getTransformation().toSCIMValue(subDescriptor,
                                                     rawValues[0]);
              subAttributes.add(
                  SCIMAttribute.createSingularAttribute(
                      subDescriptor, new SCIMAttributeValue(simpleValue)));
            }
          }
        }

        if (!subAttributes.isEmpty())
        {
          if (pluralValueMapper.getTypeValue() != null)
          {
            subAttributes.add(SCIMAttribute.createSingularAttribute(
                complexDescriptor.getAttribute("type"),
                SCIMAttributeValue.createStringValue(
                    pluralValueMapper.getTypeValue())));
          }

          final SCIMAttributeValue complexValue =
              SCIMAttributeValue.createComplexValue(subAttributes);
          final SCIMAttribute attribute =
              SCIMAttribute.createSingularAttribute(complexDescriptor,
                                                    complexValue);

          values.add(SCIMAttributeValue.createComplexValue(attribute));
        }
      }
      else
      {
        if (pluralValueMapper.getTransformations().size() > 0)
        {
          final SubAttributeTransformation sat =
              pluralValueMapper.getTransformations().iterator().next();

          final AttributeTransformation at = sat.getAttributeTransformation();
          final String scimType = sat.getSubAttribute();
          final String ldapType = at.getLdapAttribute();
          final Attribute a = entry.getAttribute(ldapType);
          if (a != null)
          {
            for (final ASN1OctetString v : a.getRawValues())
            {
              final List<SCIMAttribute> subAttributes =
                  new ArrayList<SCIMAttribute>();

              final AttributeDescriptor subDescriptor =
                  complexDescriptor.getAttribute(scimType);
              final SimpleValue simpleValue =
                  at.getTransformation().toSCIMValue(subDescriptor, v);

              final SCIMAttributeValue scimValue =
                  new SCIMAttributeValue(simpleValue);
              subAttributes.add(SCIMAttribute.createSingularAttribute(
                  subDescriptor, scimValue));

              final SCIMAttributeValue complexValue =
                  SCIMAttributeValue.createComplexValue(subAttributes);
              final SCIMAttribute attribute =
                  SCIMAttribute.createSingularAttribute(complexDescriptor,
                                                        complexValue);

              values.add(SCIMAttributeValue.createComplexValue(attribute));
            }
          }
        }
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
