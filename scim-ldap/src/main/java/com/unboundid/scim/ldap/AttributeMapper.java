/*
 * Copyright 2011-2012 UnboundID Corp.
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

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SortParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;



/**
 * This abstract class defines an interface that may be implemented to perform
 * a mapping between a SCIM attribute and LDAP attributes. There may be several
 * attribute mapper instances defined for a single SCIM attribute type, in order
 * to support, for example, different mappings for work addresses and home
 * addresses.
 */
public abstract class AttributeMapper
{
  /**
   * OID of caseExactOrderingMatch.
   */
  protected static final String CASE_EXACT_OMR_OID = "2.5.13.6";

  /**
   * OID for caseIgnoreOrderingMatch.
   */
  protected static final String CASE_IGNORE_OMR_OID = "2.5.13.3";

  /**
   * The SCIM attribute type that is mapped by this attribute mapper.
   */
  private final AttributeDescriptor attributeDescriptor;



  /**
   * Create a new instance of an attribute mapper.
   *
   * @param attributeDescriptor  The SCIM attribute type that is mapped by this
   *                             attribute mapper.
   */
  protected AttributeMapper(final AttributeDescriptor attributeDescriptor)
  {
    this.attributeDescriptor = attributeDescriptor;
  }



  /**
   * Retrieve the SCIM attribute type that is mapped by this attribute mapper.
   *
   * @return  The SCIM attribute type that is mapped by this attribute mapper.
   */
  public AttributeDescriptor getAttributeDescriptor()
  {
    return attributeDescriptor;
  }



  /**
   * Retrieve the set of LDAP attribute types used by this attribute mapper.
   *
   * @return  The set of LDAP attribute types used by this attribute mapper.
   */
  public abstract Set<String> getLDAPAttributeTypes();



  /**
   * Map the provided SCIM filter to an LDAP filter.
   *
   * @param filter  The SCIM filter to be mapped. The filter identifies the
   *                SCIM attribute that is mapped by this attribute mapper,
   *                or one of its sub-attributes.
   *
   * @return  An LDAP filter or <code>null</code>if the SCIM filter could not
   *          be mapped and will not match anything.
   * @throws InvalidResourceException if the SCIM filter contains an undefined
   *                                  attribute.
   */
  public abstract Filter toLDAPFilter(final SCIMFilter filter)
      throws InvalidResourceException;



  /**
   * Maps this SCIM attribute type to an LDAP attribute type that should be
   * used as a sort key when the SCIM attribute type has been specified in
   * sort parameters.
   *
   * @return  The mapped server side sort request control or {@code null} if
   *          the SCIM attribute type cannot be mapped.
   * @param sortParameters The SCIM sort parameters to be mapped.
   * @throws InvalidResourceException if the SCIM attribute cannot be used in
   *                                  sort parameters.
   */
  public abstract ServerSideSortRequestControl toLDAPSortControl(
      SortParameters sortParameters) throws InvalidResourceException;



  /**
   * Map the SCIM attribute in the provided SCIM object to LDAP attributes.
   *
   * @param scimObject  The SCIM object containing the attribute to be mapped.
   * @param attributes  A collection of LDAP attributes to hold any attributes
   *                    created by this mapping.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public abstract void toLDAPAttributes(
      final SCIMObject scimObject,
      final Collection<Attribute> attributes) throws InvalidResourceException;



  /**
   * Map the LDAP attributes in the provided LDAP entry to a SCIM attribute.
   *
   * @param entry  The LDAP entry whose attributes are to be mapped.
   *
   * @return  A SCIM attribute, or {@code null} if no attribute was created.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public abstract SCIMAttribute toSCIMAttribute(final Entry entry)
      throws InvalidResourceException;



  /**
   * Create an attribute mapper from an attribute definition.
   *
   * @param attributeDefinition  The attribute definition.
   * @param attributeDescriptor  The attribute descriptor.
   *
   * @return  An attribute mapper, or {@code null} if the attribute definition
   *          contains no mappings.
   */
  public static AttributeMapper create(
      final AttributeDefinition attributeDefinition,
      final AttributeDescriptor attributeDescriptor)
  {
    if (attributeDefinition.getSimple() != null)
    {
      final SimpleAttributeDefinition simpleDefinition =
          attributeDefinition.getSimple();

      if (simpleDefinition.getMapping() == null)
      {
        return null;
      }

      final AttributeTransformation t =
          AttributeTransformation.create(simpleDefinition.getMapping());

      if(attributeDescriptor.getSchema().equalsIgnoreCase(
          SCIMConstants.SCHEMA_URI_CORE) &&
          attributeDescriptor.getName().equalsIgnoreCase("password"))
      {
        return new PasswordAttributeMapper(attributeDescriptor, t);
      }
      else
      {
        return new SimpleAttributeMapper(attributeDescriptor, t);
      }
    }
    else if (attributeDefinition.getComplex() != null)
    {
      final ComplexAttributeDefinition complexDefinition =
          attributeDefinition.getComplex();

      final List<SubAttributeTransformation> transformations =
          new ArrayList<SubAttributeTransformation>();

      for (final SubAttributeDefinition subAttributeDefinition :
          complexDefinition.getSubAttribute())
      {
        if (subAttributeDefinition.getMapping() != null)
        {
          transformations.add(
              SubAttributeTransformation.create(subAttributeDefinition));
        }
      }

      if (transformations.isEmpty())
      {
        return null;
      }

      return new ComplexSingularAttributeMapper(attributeDescriptor,
                                                transformations);
    }
    else if (attributeDefinition.getSimpleMultiValued() != null)
    {
      final SimpleMultiValuedAttributeDefinition simpleMultiValuedDefinition =
          attributeDefinition.getSimpleMultiValued();

      final List<CanonicalValueMapper> canonicalValueMappers =
          new ArrayList<CanonicalValueMapper>();
      for (final CanonicalValue CanonicalValue :
          simpleMultiValuedDefinition.getCanonicalValue())
      {
        final CanonicalValueMapper m =
            CanonicalValueMapper.create(CanonicalValue);
        if (m != null)
        {
          canonicalValueMappers.add(m);
        }
      }

      if (simpleMultiValuedDefinition.getMapping() != null)
      {
        canonicalValueMappers.add(CanonicalValueMapper.create(
            simpleMultiValuedDefinition.getMapping()));
      }

      if (canonicalValueMappers.isEmpty())
      {
        return null;
      }

      return new MultiValuedAttributeMapper(attributeDescriptor,
          canonicalValueMappers);
    }
    else if (attributeDefinition.getComplexMultiValued() != null)
    {
      final ComplexMultiValuedAttributeDefinition complexMultiValuedDefinition =
          attributeDefinition.getComplexMultiValued();

      final List<CanonicalValueMapper> canonicalValueMappers =
          new ArrayList<CanonicalValueMapper>();

      for (final CanonicalValue CanonicalValue :
          complexMultiValuedDefinition.getCanonicalValue())
      {
        final CanonicalValueMapper m =
            CanonicalValueMapper.create(CanonicalValue);
        if (m != null)
        {
          canonicalValueMappers.add(m);
        }
      }

      if (canonicalValueMappers.isEmpty())
      {
        return null;
      }

      return new MultiValuedAttributeMapper(attributeDescriptor,
          canonicalValueMappers);
    }
    else
    {
      return null;
    }
  }
}
