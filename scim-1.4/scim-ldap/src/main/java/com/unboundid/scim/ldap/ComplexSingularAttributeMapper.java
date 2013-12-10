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
 * This class provides an attribute mapper that maps a singular complex SCIM
 * attribute to several single-valued LDAP attributes, where each LDAP
 * attribute holds one of the SCIM sub-attributes.
 */
public class ComplexSingularAttributeMapper extends AttributeMapper
{
  /**
   * The set of sub-attribute transformations indexed by the name of the
   * sub-attribute.
   */
  private final Map<String, SubAttributeTransformation> map;

  /**
   * The set of LDAP attributes that are mapped by this attribute mapper.
   */
  private final Set<String> ldapAttributeTypes;



  /**
   * Create a new instance of a complex singular attribute mapper.
   *
   * @param attributeDescriptor  The SCIM attribute type that is mapped by this
   *                             attribute mapper.
   * @param transformations    The set of sub-attribute transformations for
   *                           this attribute mapper.
   */
  public ComplexSingularAttributeMapper(
      final AttributeDescriptor attributeDescriptor,
      final List<SubAttributeTransformation> transformations)
  {
    super(attributeDescriptor);

    map = new HashMap<String, SubAttributeTransformation>();
    ldapAttributeTypes = new HashSet<String>();
    for (final SubAttributeTransformation t : transformations)
    {
      map.put(t.getSubAttribute(), t);
      ldapAttributeTypes.add(t.getAttributeTransformation().getLdapAttribute());
    }
  }



  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return ldapAttributeTypes;
  }



  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
      throws InvalidResourceException
  {
    final SCIMFilterType type = filter.getFilterType();

    String ldapAttributeType = null;
    String ldapFilterValue = null;

    if(type != SCIMFilterType.AND && type != SCIMFilterType.OR)
    {
      final String subAttributeName =
             filter.getFilterAttribute().getSubAttributeName();
      if(subAttributeName == null)
      {
        throw new InvalidResourceException(filter.getFilterAttribute() +
            " must be a path to a sub-attribute");
      }

      final SubAttributeTransformation subAttributeTransformation =
                                              map.get(subAttributeName);
      if(subAttributeTransformation == null)
      {
       // Make sure the sub-attribute is defined.
        getAttributeDescriptor().getSubAttribute(subAttributeName);
        return null; //match nothing
      }

      final AttributeTransformation attributeTransformation =
                subAttributeTransformation.getAttributeTransformation();

      ldapAttributeType = attributeTransformation.getLdapAttribute();

      if(filter.getFilterValue() != null)
      {
        final Transformation t = attributeTransformation.getTransformation();
        ldapFilterValue = t.toLDAPFilterValue(filter.getFilterValue());
      }
    }
    else
    {
      // We don't have to worry about AND and OR filter types since they are
      // handled earlier by the resource mapper.
      throw new RuntimeException("Invalid filter type: " + type);
    }

    switch(type)
    {
      case EQUALITY:
        return Filter.createEqualityFilter(ldapAttributeType, ldapFilterValue);
      case CONTAINS:
        return Filter.createSubstringFilter(ldapAttributeType, null,
                                    new String[] { ldapFilterValue }, null);
      case PRESENCE:
        return Filter.createPresenceFilter(ldapAttributeType);
      case STARTS_WITH:
        return Filter.createSubstringFilter(ldapAttributeType, ldapFilterValue,
                                              null, null);
      case GREATER_THAN:
        return Filter.createANDFilter(
          Filter.createGreaterOrEqualFilter(ldapAttributeType, ldapFilterValue),
          Filter.createNOTFilter(
              Filter.createEqualityFilter(ldapAttributeType, ldapFilterValue)));
      case GREATER_OR_EQUAL:
        return Filter.createGreaterOrEqualFilter(ldapAttributeType,
                ldapFilterValue);
      case LESS_THAN:
        return Filter.createANDFilter(
          Filter.createLessOrEqualFilter(ldapAttributeType, ldapFilterValue),
          Filter.createNOTFilter(
              Filter.createEqualityFilter(ldapAttributeType, ldapFilterValue)));
      case LESS_OR_EQUAL:
        return Filter.createLessOrEqualFilter(ldapAttributeType,
                ldapFilterValue);
      default:
        throw new RuntimeException(
                "Filter type " + type + " is not supported");
    }
  }



  @Override
  public ServerSideSortRequestControl toLDAPSortControl(
      final SortParameters sortParameters)
      throws InvalidResourceException
  {
    final String subAttributeName =
        sortParameters.getSortBy().getSubAttributeName();
    if(subAttributeName == null)
    {
      throw new InvalidResourceException("Cannot sort by attribute because " +
          sortParameters.getSortBy() + " must be a path to a sub-attribute");
    }

    final SubAttributeTransformation subAttributeTransformation =
        map.get(subAttributeName);
    if(subAttributeTransformation == null)
    {
      // Make sure the sub-attribute is defined.
      getAttributeDescriptor().getSubAttribute(subAttributeName);
      return null; //sort by nothing
    }

    final AttributeTransformation attributeTransformation =
        subAttributeTransformation.getAttributeTransformation();
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



  @Override
  public void toLDAPAttributes(final SCIMObject scimObject,
                               final Collection<Attribute> attributes)
      throws InvalidResourceException {
    final SCIMAttribute scimAttribute =
        scimObject.getAttribute(getAttributeDescriptor().getSchema(),
                                getAttributeDescriptor().getName());
    if (scimAttribute != null)
    {
      final SCIMAttributeValue value = scimAttribute.getValue();

      for (final SubAttributeTransformation sat : map.values())
      {
        final AttributeTransformation at = sat.getAttributeTransformation();
        final String scimType = sat.getSubAttribute();
        final String ldapType = at.getLdapAttribute();

        final SCIMAttribute subAttribute = value.getAttribute(scimType);
        if (subAttribute != null)
        {
          final AttributeDescriptor subDescriptor =
              getAttributeDescriptor().getSubAttribute(scimType);
          final ASN1OctetString v = at.getTransformation().toLDAPValue(
              subDescriptor, subAttribute.getValue().getValue());
          attributes.add(new Attribute(ldapType, v));
        }
      }
    }
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry)
      throws InvalidResourceException {
    final List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>();

    for (final SubAttributeTransformation sat : map.values())
    {
      final String subAttributeName = sat.getSubAttribute();
      final AttributeTransformation at = sat.getAttributeTransformation();

      final AttributeDescriptor subDescriptor =
          getAttributeDescriptor().getSubAttribute(subAttributeName);
      final Attribute a = entry.getAttribute(at.getLdapAttribute());
      if (a != null)
      {
        final ASN1OctetString[] rawValues = a.getRawValues();
        if (rawValues.length > 0)
        {
          final SimpleValue simpleValue =
              at.getTransformation().toSCIMValue(subDescriptor, rawValues[0]);
          subAttributes.add(
              SCIMAttribute.create(
                  subDescriptor, new SCIMAttributeValue(simpleValue)));
        }
      }
    }

    if (subAttributes.isEmpty())
    {
      return null;
    }

    final SCIMAttributeValue complexValue =
        SCIMAttributeValue.createComplexValue(subAttributes);
    return SCIMAttribute.create(getAttributeDescriptor(),
        complexValue);
  }
}
