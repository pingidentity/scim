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

package com.unboundid.scim.ldap;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.SimpleValue;
import com.unboundid.scim.sdk.SortParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * This class provides an attribute mapper for multi-valued attributes.
 * Each type of value (e.g. "work", "home") is mapped to one or more
 * single-valued LDAP attributes, where each LDAP attribute holds one
 * of the SCIM sub-attributes.
 */
public class MultiValuedAttributeMapper extends AttributeMapper
{
  /**
   * A set of value mappers for each value of the "type" sub-attribute.
   */
  private final Map<String,CanonicalValueMapper> valueMappers;

  /**
   * The set of LDAP attributes that are mapped by this attribute mapper.
   */
  private final Set<String> ldapAttributeTypes;



  /**
   * Create a new instance of a complex attribute mapper.
   *
   * @param attributeDescriptor   The SCIM attribute type that is mapped by this
   *                              attribute mapper.
   * @param canonicalValueMappers The set of complex value mappers for this
   *                              attribute mapper.
   */
  public MultiValuedAttributeMapper(
      final AttributeDescriptor attributeDescriptor,
      final Collection<CanonicalValueMapper> canonicalValueMappers)
  {
    super(attributeDescriptor);

    ldapAttributeTypes = new HashSet<String>();
    valueMappers = new HashMap<String, CanonicalValueMapper>();
    for (final CanonicalValueMapper canonicalValueMapper :
        canonicalValueMappers)
    {
      valueMappers.put(canonicalValueMapper.getTypeValue(),
          canonicalValueMapper);
      for (final SubAttributeTransformation sat :
          canonicalValueMapper.getTransformations())
      {
        ldapAttributeTypes.add(
            sat.getAttributeTransformation().getLdapAttribute());
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return ldapAttributeTypes;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
      throws InvalidResourceException
  {
    final SCIMFilterType type = filter.getFilterType();

    List<String> ldapFilterTypes = null;
    List<String> ldapFilterValues = null;

    if(type != SCIMFilterType.AND && type != SCIMFilterType.OR)
    {
      String subAttributeName =
        filter.getFilterAttribute().getSubAttributeName();
      if (subAttributeName == null)
      {
        subAttributeName = "value";
      }

      final List<SubAttributeTransformation> selectedTransformations =
          new ArrayList<SubAttributeTransformation>();
      for (final CanonicalValueMapper canonicalValueMapper :
          valueMappers.values())
      {
        for (final SubAttributeTransformation sat :
            canonicalValueMapper.getTransformations())
        {
          if (sat.getSubAttribute().equalsIgnoreCase(subAttributeName))
          {
            selectedTransformations.add(sat);
          }
        }
      }

      if (selectedTransformations.isEmpty())
      {
        getAttributeDescriptor().getSubAttribute(subAttributeName);
        return null; //match nothing
      }

      ldapFilterTypes = new ArrayList<String>(selectedTransformations.size());
      ldapFilterValues = new ArrayList<String>(selectedTransformations.size());

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
    }
    else
    {
      // We don't have to worry about AND and OR filter types since they are
      // handled earlier by the resource mapper.
      throw new RuntimeException("Invalid filter type: " + type);
    }

    final List<Filter> filterComponents =
        new ArrayList<Filter>(ldapFilterTypes.size());
    for (int i = 0; i < ldapFilterTypes.size(); i++)
    {
      final String ldapAttributeType = ldapFilterTypes.get(i);
      final String ldapFilterValue = ldapFilterValues.get(i);

      switch (type)
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
              "Filter type " + type + " is not supported");
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



  /**
   * {@inheritDoc}
   */
  @Override
  public ServerSideSortRequestControl toLDAPSortControl(
      final SortParameters sortParameters)
      throws InvalidResourceException
  {
    String subAttributeName = sortParameters.getSortBy().getSubAttributeName();
    if (subAttributeName == null)
    {
      subAttributeName = "value";
    }

    SubAttributeTransformation selectedTransformation = null;
    for (final CanonicalValueMapper canonicalValueMapper :
        valueMappers.values())
    {
      for (final SubAttributeTransformation sat :
          canonicalValueMapper.getTransformations())
      {
        if (sat.getSubAttribute().equalsIgnoreCase(subAttributeName))
        {
          selectedTransformation = sat;
          break;
        }
      }
      if(selectedTransformation != null)
      {
        break;
      }
    }

    if (selectedTransformation == null)
    {
      // Make sure the sub-attribute is defined
      getAttributeDescriptor().getSubAttribute(subAttributeName);
      return null; //sort by nothing
    }

    final AttributeTransformation attributeTransformation =
        selectedTransformation.getAttributeTransformation();
    if(attributeTransformation.getTransformation()
        instanceof DefaultTransformation)
    {
      final AttributeDescriptor subAttributeDescriptor =
          getAttributeDescriptor().getSubAttribute(subAttributeName);
      final boolean reverseOrder = !sortParameters.isAscendingOrder();
      if(subAttributeDescriptor.getDataType() ==
          AttributeDescriptor.DataType.STRING)
      {
        return new ServerSideSortRequestControl(
            new SortKey(attributeTransformation.getLdapAttribute(),
                subAttributeDescriptor.isCaseExact() ?
                    CASE_EXACT_OMR_OID : CASE_IGNORE_OMR_OID, reverseOrder));
      }
      return new ServerSideSortRequestControl(
          new SortKey(attributeTransformation.getLdapAttribute(),
              reverseOrder));
    }
    else
    {
      throw new InvalidResourceException("Cannot sort by attribute " +
          sortParameters.getSortBy() + " because it is mapped with custom " +
          "transformations");
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void toLDAPAttributes(final SCIMObject scimObject,
                               final Collection<Attribute> attributes)
      throws InvalidResourceException {
    final SCIMAttribute scimAttribute =
        scimObject.getAttribute(getAttributeDescriptor().getSchema(),
                                getAttributeDescriptor().getName());
    if (scimAttribute != null)
    {
      for (SCIMAttributeValue v : scimAttribute.getValues())
      {
        final SCIMAttribute typeAttr = v.getAttribute("type");
        CanonicalValueMapper canonicalValueMapper = null;
        if (typeAttr != null)
        {
          final String type = typeAttr.getValue().getStringValue();

          canonicalValueMapper = valueMappers.get(type);
        }

        // Check for a default mapper.
        if (canonicalValueMapper == null)
        {
          canonicalValueMapper = valueMappers.get(null);
        }

        if (canonicalValueMapper != null)
        {
          for (final SubAttributeTransformation sat :
              canonicalValueMapper.getTransformations())
          {
            final AttributeTransformation at = sat.getAttributeTransformation();
            final String scimType = sat.getSubAttribute();
            final String ldapType = at.getLdapAttribute();

            final SCIMAttribute subAttribute = v.getAttribute(scimType);
            if (subAttribute != null)
            {
              final AttributeDescriptor subDescriptor =
                  getAttributeDescriptor().getSubAttribute(scimType);
              final ASN1OctetString value = at.getTransformation().toLDAPValue(
                  subDescriptor, subAttribute.getValue().getValue());
              attributes.add(new Attribute(ldapType, value));
            }
          }
        }
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry)
      throws InvalidResourceException {
    final List<SCIMAttributeValue> values = new ArrayList<SCIMAttributeValue>();
    for (final CanonicalValueMapper canonicalValueMapper :
        valueMappers.values())
    {
      if (canonicalValueMapper.getTypeValue() != null)
      {
        final List<SCIMAttribute> subAttributes =
            new ArrayList<SCIMAttribute>();

        for (final SubAttributeTransformation sat :
            canonicalValueMapper.getTransformations())
        {
          final AttributeTransformation at = sat.getAttributeTransformation();
          final String scimType = sat.getSubAttribute();
          final String ldapType = at.getLdapAttribute();

          final AttributeDescriptor subDescriptor =
              getAttributeDescriptor().getSubAttribute(scimType);
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
                  SCIMAttribute.create(
                      subDescriptor, new SCIMAttributeValue(simpleValue)));
            }
          }
        }

        if (!subAttributes.isEmpty())
        {
          if (canonicalValueMapper.getTypeValue() != null)
          {
            subAttributes.add(SCIMAttribute.create(
                getAttributeDescriptor().getSubAttribute("type"),
                SCIMAttributeValue.createStringValue(
                    canonicalValueMapper.getTypeValue())));
          }

          final SCIMAttributeValue complexValue =
              SCIMAttributeValue.createComplexValue(subAttributes);

          values.add(complexValue);
        }
      }
      else
      {
        if (canonicalValueMapper.getTransformations().size() > 0)
        {
          final SubAttributeTransformation sat =
              canonicalValueMapper.getTransformations().iterator().next();

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
                  getAttributeDescriptor().getSubAttribute(scimType);
              final SimpleValue simpleValue =
                  at.getTransformation().toSCIMValue(subDescriptor, v);

              final SCIMAttributeValue scimValue =
                  new SCIMAttributeValue(simpleValue);
              subAttributes.add(SCIMAttribute.create(
                  subDescriptor, scimValue));

              final SCIMAttributeValue complexValue =
                  SCIMAttributeValue.createComplexValue(subAttributes);

              values.add(complexValue);
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
      return SCIMAttribute.create(
          getAttributeDescriptor(),
          values.toArray(new SCIMAttributeValue[values.size()]));
    }
  }
}
