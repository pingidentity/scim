/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.AttributePath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;




/**
 * This class provides a resource mapper whose behavior can be specified in
 * configuration.
 */
public class ConfigurableResourceMapper extends ResourceMapper
{
  /**
   * The name of the SCIM resource handled by this resource mapper.
   */
  private String resourceName;

  /**
   * The LDAP structural object class representing resources handled by this
   * resource mapper.
   */
  private String structuralObjectClass;

  /**
   * The LDAP object class values to be used in entries created for new
   * resources.
   */
  private Attribute objectClassAttribute;

  /**
   * The LDAP attribute whose value should be used to form the RDN of entries
   * created for new resources.
   */
  private String rdnAttributeType;

  /**
   * The attribute mappers for this resource mapper.
   */
  private Map<SCIMAttributeType,AttributeMapper> attributeMappers;

  /**
   * The set of LDAP attributes used by this resource mapper.
   */
  private Set<String> ldapAttributeTypes;



  /**
   * Create a new instance of the resource mapper.
   *
   * @param resourceName           The name of the SCIM resource handled by
   *                               this resource mapper.
   * @param structuralObjectClass  The LDAP structural object class representing
   *                               resources handled by this resource mapper.
   * @param objectClassValues      The LDAP object class values to be used in
   *                               entries created for new resources.
   * @param rdnAttributeType       The LDAP attribute whose value should be used
   *                               to form the RDN of entries created for new
   *                               resources.
   * @param mappers                The attribute mappers for this resource
   *                               mapper.
   */
  public ConfigurableResourceMapper(final String resourceName,
                                    final String structuralObjectClass,
                                    final String[] objectClassValues,
                                    final String rdnAttributeType,
                                    final Collection<AttributeMapper> mappers)
  {
    this.resourceName          = resourceName;
    this.structuralObjectClass = structuralObjectClass;
    this.rdnAttributeType      = rdnAttributeType;

    objectClassAttribute = new Attribute("objectClass", objectClassValues);

    attributeMappers =
        new HashMap<SCIMAttributeType, AttributeMapper>(mappers.size());
    ldapAttributeTypes = new HashSet<String>();

    for (final AttributeMapper m : mappers)
    {
      attributeMappers.put(m.getSCIMAttributeType(), m);
      ldapAttributeTypes.addAll(m.getLDAPAttributeTypes());
    }
  }



  @Override
  public void initializeMapper()
  {
    // No implementation required.
  }



  @Override
  public void finalizeMapper()
  {
    // No implementation required.
  }



  @Override
  public boolean supportsCreate()
  {
    return true;
  }



  @Override
  public Set<String> toLDAPAttributeTypes(
      final SCIMQueryAttributes queryAttributes)
  {
    final Set<String> ldapAttributes = new HashSet<String>();
    for (final AttributeMapper m : attributeMappers.values())
    {
      if (queryAttributes.isAttributeRequested(m.getSCIMAttributeType()))
      {
        ldapAttributes.addAll(m.getLDAPAttributeTypes());
      }
    }

    return ldapAttributes;
  }



  @Override
  public Entry toLDAPEntry(final SCIMObject scimObject, final String baseDN)
      throws LDAPException
  {
    final Entry entry = new Entry("");
    entry.addAttribute(objectClassAttribute);
    for (final Attribute a : toLDAPAttributes(scimObject))
    {
      entry.addAttribute(a);
    }

    RDN rdn = null;
    if (entry.hasAttribute(rdnAttributeType))
    {
      rdn = new RDN(rdnAttributeType,
                    entry.getAttributeValue(rdnAttributeType));
    }

    if (rdn != null)
    {
      entry.setDN(new DN(rdn, new DN(baseDN)));
    }

    return entry;
  }



  @Override
  public List<Attribute> toLDAPAttributes(final SCIMObject scimObject)
  {
    final List<Attribute> attributes = new ArrayList<Attribute>();

    for (final AttributeMapper attributeMapper : attributeMappers.values())
    {
      attributeMapper.toLDAPAttributes(scimObject, attributes);
    }

    return attributes;
  }



  @Override
  public List<Modification> toLDAPModifications(final Entry currentEntry,
                                                final SCIMObject scimObject)
  {
    final List<Attribute> attributes = toLDAPAttributes(scimObject);
    final Entry entry = new Entry(currentEntry.getDN(), attributes);

    final String[] ldapAttributesArray =
        ldapAttributeTypes.toArray(new String[ldapAttributeTypes.size()]);

    return Entry.diff(currentEntry, entry, true, false, ldapAttributesArray);
  }



  @Override
  public Filter toLDAPFilter(final SCIMFilter filter)
  {
    final Filter objectClassFilter =
        Filter.createEqualityFilter("objectclass", structuralObjectClass);

    if (filter == null)
    {
      return objectClassFilter;
    }

    final Filter filterComponent = toLDAPFilterComponent(filter);

    if (filterComponent == null)
    {
      return objectClassFilter;
    }
    else
    {
      return Filter.createANDFilter(filterComponent, objectClassFilter);
    }
  }



  /**
   * Map a SCIM filter component to an LDAP filter component.
   *
   * @param filter  The SCIM filter component to be mapped.
   *
   * @return  The LDAP filter component, or {@code null} if the filter
   *          component could not be mapped.
   */
  private Filter toLDAPFilterComponent(final SCIMFilter filter)
  {
    final SCIMFilterType filterType = filter.getFilterType();

    switch (filterType)
    {
      case AND:
        final List<Filter> andFilterComponents = new ArrayList<Filter>();
        for (SCIMFilter f : filter.getFilterComponents())
        {
          final Filter filterComponent = toLDAPFilterComponent(f);
          if (filterComponent != null)
          {
            andFilterComponents.add(filterComponent);
          }
        }
        return Filter.createANDFilter(andFilterComponents);

      case OR:
        final List<Filter> orFilterComponents = new ArrayList<Filter>();
        for (SCIMFilter f : filter.getFilterComponents())
        {
          final Filter filterComponent = toLDAPFilterComponent(f);
          if (filterComponent != null)
          {
            orFilterComponents.add(filterComponent);
          }
        }
        return Filter.createANDFilter(orFilterComponents);

      default:
        final AttributePath filterAttribute = filter.getFilterAttribute();
        final SCIMAttributeType filterAttributeType =
            new SCIMAttributeType(filterAttribute.getAttributeSchema(),
                                  filterAttribute.getAttributeName());
        final AttributeMapper attributeMapper =
            attributeMappers.get(filterAttributeType);
        if (attributeMapper != null)
        {
          return attributeMapper.toLDAPFilter(filter);
        }
        break;
    }

    return null;
  }



  @Override
  public Control toLDAPSortControl(final SortParameters sortParameters)
  {
    final SCIMAttributeType scimAttributeType = sortParameters.getSortBy();

    final AttributeMapper attributeMapper =
        attributeMappers.get(scimAttributeType);
    if (attributeMapper == null)
    {
      throw new RuntimeException("Cannot sort by attribute " +
                                 scimAttributeType);
    }

    final String ldapAttribute = attributeMapper.toLDAPSortAttributeType();

    final boolean reverseOrder = !sortParameters.isAscendingOrder();
    return new ServerSideSortRequestControl(
        new SortKey(ldapAttribute, reverseOrder));
  }



  @Override
  public List<SCIMAttribute> toSCIMAttributes(
      final String resourceName,
      final Entry entry,
      final SCIMQueryAttributes queryAttributes)
  {
    final List<SCIMAttribute> attributes =
        new ArrayList<SCIMAttribute>();

    for (final AttributeMapper attributeMapper : attributeMappers.values())
    {
      if (queryAttributes.isAttributeRequested(
          attributeMapper.getSCIMAttributeType()))
      {
        final SCIMAttribute attribute = attributeMapper.toSCIMAttribute(entry);
        if (attribute != null)
        {
          attributes.add(attribute);
        }
      }
    }

    return attributes;
  }



  @Override
  public SCIMObject toSCIMObject(final Entry entry,
                                 final SCIMQueryAttributes queryAttributes)
  {
    if (!entry.hasObjectClass(structuralObjectClass))
    {
      return null;
    }

    final List<SCIMAttribute> attributes =
        toSCIMAttributes(resourceName, entry,
                         queryAttributes);

    final SCIMObject scimObject = new SCIMObject(resourceName);
    for (final SCIMAttribute a : attributes)
    {
      scimObject.addAttribute(a);
    }

    return scimObject;
  }
}
