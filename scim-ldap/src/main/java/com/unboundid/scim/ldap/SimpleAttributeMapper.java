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
   * The attribute transformation to be applied by this attribute mapper.
   */
  private final AttributeTransformation attributeTransformation;



  /**
   * Create a new instance of a simple attribute mapper.
   *
   * @param attributeDescriptor  The SCIM attribute type that is mapped by this
   *                             attribute mapper.
   * @param transformation     The attribute transformation to be applied
   *                           by this attribute mapper.
   */
  public SimpleAttributeMapper(final AttributeDescriptor attributeDescriptor,
                               final AttributeTransformation transformation)
  {
    super(attributeDescriptor);
    this.attributeTransformation = transformation;
  }



  @Override
  public Set<String> getLDAPAttributeTypes()
  {
    return Collections.singleton(attributeTransformation.getLdapAttribute());
  }



  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
  {
    final String ldapAttributeType = attributeTransformation.getLdapAttribute();
    final SCIMFilterType filterType = filter.getFilterType();
    final String filterValue;
    if (filter.getFilterValue() != null)
    {
      filterValue =
          attributeTransformation.getTransformation().toLDAPFilterValue(
              filter.getFilterValue());
    }
    else
    {
      filterValue = null;
    }

    switch (filterType)
    {
      // We don't have to worry about AND and OR filter types since they are
      // handled earlier by the resource mapper.
      case EQUALITY:
      {
        return Filter.createEqualityFilter(ldapAttributeType, filterValue);
      }

      case CONTAINS:
      {
        return Filter.createSubstringFilter(ldapAttributeType,
                                            null,
                                            new String[] { filterValue },
                                            null);
      }

      case STARTS_WITH:
      {
        return Filter.createSubstringFilter(ldapAttributeType,
                                            filterValue,
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
        return Filter.createGreaterOrEqualFilter(ldapAttributeType,
                                                 filterValue);
      }

      case LESS_THAN:
      case LESS_OR_EQUAL:
      {
        return Filter.createLessOrEqualFilter(ldapAttributeType,
                                              filterValue);
      }

      default:
        throw new RuntimeException(
            "Filter type " + filterType + " is not supported");
    }
  }



  @Override
  public ServerSideSortRequestControl toLDAPSortAttributeType(
      final SortParameters sortParameters)
      throws InvalidResourceException
  {
    if(attributeTransformation.getTransformation()
        instanceof DefaultTransformation)
    {
      final boolean reverseOrder = !sortParameters.isAscendingOrder();
      if(getAttributeDescriptor().getDataType() ==
          AttributeDescriptor.DataType.STRING)
      {
        return new ServerSideSortRequestControl(
            new SortKey(attributeTransformation.getLdapAttribute(),
                getAttributeDescriptor().isCaseExact() ?
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
  {
    final String ldapAttributeType = attributeTransformation.getLdapAttribute();
    final SCIMAttribute scimAttribute =
        scimObject.getAttribute(getAttributeDescriptor().getSchema(),
                                getAttributeDescriptor().getName());
    if (scimAttribute != null)
    {
      final ASN1OctetString ldapValue =
          attributeTransformation.getTransformation().toLDAPValue(
              getAttributeDescriptor(),
              scimAttribute.getValue().getValue());
      attributes.add(new Attribute(ldapAttributeType, ldapValue));
    }
  }



  @Override
  public SCIMAttribute toSCIMAttribute(final Entry entry)
  {
    final String ldapAttributeType = attributeTransformation.getLdapAttribute();
    final Attribute a = entry.getAttribute(ldapAttributeType);
    if (a != null)
    {
      final ASN1OctetString[] rawValues = a.getRawValues();
      if (rawValues.length > 0)
      {
        final SimpleValue simpleValue =
            attributeTransformation.getTransformation().toSCIMValue(
                getAttributeDescriptor(), rawValues[0]);
        return SCIMAttribute.create(
            getAttributeDescriptor(), new SCIMAttributeValue(simpleValue));
      }
    }

    return null;
  }
}
