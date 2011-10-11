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
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.AttributePath;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;



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
   * The query endpoint for resources handled by this resource mapper.
   */
  private String queryEndpoint;

  /**
   * The LDAP Search base DN to be used for querying.
   */
  private String searchBaseDN;

  /**
   * The LDAP filter to match all resources handled by this resource manager.
   */
  private Filter searchFilter;

  /**
   * The LDAP Add parameters.
   */
  private LDAPAddParameters addParameters;

  /**
   * A DN constructed value for the DN template.
   */
  private ConstructedValue dnConstructor;

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
   * @param resourceName   The name of the SCIM resource handled by this
   *                       resource mapper.
   * @param queryEndpoint  The  query endpoint for resources handled by this
   *                       resource mapper.
   * @param searchBaseDN   The LDAP Search base DN.
   * @param searchFilter   The LDAP Search filter.
   * @param addParameters  The LDAP Add parameters.
   * @param mappers        The attribute mappers for this resource mapper.
   */
  public ConfigurableResourceMapper(final String resourceName,
                                    final String queryEndpoint,
                                    final String searchBaseDN,
                                    final String searchFilter,
                                    final LDAPAddParameters addParameters,
                                    final Collection<AttributeMapper> mappers)
  {
    this.resourceName      = resourceName;
    this.queryEndpoint     = queryEndpoint;
    this.addParameters     = addParameters;
    this.searchBaseDN      = searchBaseDN;

    try
    {
      this.searchFilter = Filter.create(searchFilter);
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      throw new IllegalArgumentException(e.getExceptionMessage());
    }

    if (addParameters != null)
    {
      this.dnConstructor =
          new ConstructedValue(addParameters.getDNTemplate().trim());
    }

    attributeMappers =
        new HashMap<SCIMAttributeType, AttributeMapper>(mappers.size());
    ldapAttributeTypes = new HashSet<String>();

    for (final AttributeMapper m : mappers)
    {
      attributeMappers.put(m.getSCIMAttributeType(), m);
      ldapAttributeTypes.addAll(m.getLDAPAttributeTypes());
    }
  }



  /**
   * Parse an XML file defining a set of resource mappings.
   *
   * @param file  An XML file defining a set of resource mappings.
   *
   * @return  A list of resource mappers.
   *
   * @throws JAXBException  If an error occurs during the parsing.
   * @throws SAXException   If the XML schema cannot be instantiated.
   */
  public static List<ResourceMapper> parse(final File file)
      throws JAXBException, SAXException
  {
    final ObjectFactory factory = new ObjectFactory();
    final String packageName = factory.getClass().getPackage().getName();
    final JAXBContext context = JAXBContext.newInstance(packageName);

    final Unmarshaller unmarshaller = context.createUnmarshaller();
    final URL url = ResourcesDefinition.class.getResource("resources.xsd");
    if (url != null) {
      final SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI);
      final Schema schema = sf.newSchema(url);
      unmarshaller.setSchema(schema);
    }

    final JAXBElement jaxbElement = (JAXBElement) unmarshaller.unmarshal(file);
    final ResourcesDefinition resources =
        (ResourcesDefinition)jaxbElement.getValue();

    final List<ResourceMapper> resourceMappers =
        new ArrayList<ResourceMapper>();
    for (final ResourceDefinition resource : resources.getResource())
    {
      if (resource.getLDAPSearch() == null)
      {
        continue;
      }

      final List<AttributeMapper> attributeMappers =
          new ArrayList<AttributeMapper>();
      for (final AttributeDefinition attributeDefinition :
          resource.getAttribute())
      {
        final AttributeMapper m =
            AttributeMapper.create(attributeDefinition, resource.getSchema());
        if (m != null)
        {
          attributeMappers.add(m);
        }
      }

      String searchBaseDN = null;
      String searchFilter = null;
      if (resource.getLDAPSearch() != null)
      {
        searchBaseDN = resource.getLDAPSearch().getBaseDN().trim();
        searchFilter = resource.getLDAPSearch().getFilter().trim();
      }

      resourceMappers.add(
          new ConfigurableResourceMapper(resource.getName(),
                                         resource.getQueryEndpoint(),
                                         searchBaseDN,
                                         searchFilter,
                                         resource.getLDAPAdd(),
                                         attributeMappers));
    }

    return resourceMappers;
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
  public String getResourceName()
  {
    return resourceName;
  }



  @Override
  public String getQueryEndpoint()
  {
    return queryEndpoint;
  }



  @Override
  public boolean supportsQuery()
  {
    return searchBaseDN != null;
  }



  @Override
  public boolean supportsCreate()
  {
    return addParameters != null;
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
  public Entry toLDAPEntry(final SCIMObject scimObject)
      throws LDAPException
  {
    final Entry entry = new Entry("");

    if (addParameters == null)
    {
      throw new RuntimeException(
          "No LDAP Add Parameters were specified for the " + resourceName +
          " Resource Mapper");
    }

    for (final FixedAttribute fixedAttribute :
        addParameters.getFixedAttribute())
    {
      boolean preserveExisting = false;
      final String attributeName = fixedAttribute.getLdapAttribute();
      if (entry.hasAttribute(attributeName))
      {
        switch (fixedAttribute.getOnConflict())
        {
          case MERGE:
            break;
          case OVERWRITE:
            entry.removeAttribute(attributeName);
            break;
          case PRESERVE:
            preserveExisting = true;
            break;
        }
      }

      if (!preserveExisting)
      {
        entry.addAttribute(
            new Attribute(attributeName, fixedAttribute.getFixedValue()));
      }
    }

    for (final Attribute a : toLDAPAttributes(scimObject))
    {
      entry.addAttribute(a);
    }

    // TODO allow SCIM object values to be referenced
    entry.setDN(dnConstructor.constructValue(entry));

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
    if (searchFilter == null)
    {
      return null;
    }

    if (filter == null)
    {
      return searchFilter;
    }

    final Filter filterComponent = toLDAPFilterComponent(filter);

    if (filterComponent == null)
    {
      return searchFilter;
    }
    else
    {
      return Filter.createANDFilter(filterComponent, searchFilter);
    }
  }



  @Override
  public String getSearchBaseDN()
  {
    return searchBaseDN;
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
    if (searchFilter != null)
    {
      try
      {
        if (!searchFilter.matchesEntry(entry))
        {
          return null;
        }
      }
      catch (LDAPException e)
      {
        Debug.debugException(e);
        throw new RuntimeException(e.getExceptionMessage());
      }
    }

    final List<SCIMAttribute> attributes =
        toSCIMAttributes(resourceName, entry,
                         queryAttributes);

    final SCIMObject scimObject = new SCIMObject();
    for (final SCIMAttribute a : attributes)
    {
      scimObject.addAttribute(a);
    }

    return scimObject;
  }
}
