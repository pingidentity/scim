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

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.ServerErrorException;
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
public final class ConfigurableResourceMapper extends ResourceMapper
{
  /**
   * The ResourceDescriptor of the SCIM resource handled by this resource
   * mapper.
   */
  private ResourceDescriptor resourceDescriptor;

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
  private Map<AttributeDescriptor, AttributeMapper> attributeMappers;

  /**
   * The set of LDAP attributes used by this resource mapper.
   */
  private Set<String> ldapAttributeTypes;

  /**
   * The derived attributes for this resource mapper.
   */
  private Map<AttributeDescriptor,DerivedAttribute> derivedAttributes;

  /**
   * The internal AttributeMapper for the Meta object. This allows the
   * toLDAPFilter() code to generate LDAP filters which are based on the meta
   * information, even though there are no explicit mappings for Meta set up in
   * resources.xml.
   */
  private AttributeMapper metaAttributeMapper;



  /**
   * Initialize this resource mapper from the provided information.
   *
   * @param resourceDescriptor The ResourceDescriptor of the SCIM resource
   *                           handled by this resource mapper.
   * @param searchParameters   The LDAP Search parameters, or {@code null} if
   *                           there are none defined.
   * @param addParameters      The LDAP Add parameters, or {@code null} if there
   *                           are none defined.
   * @param mappers            The attribute mappers for this resource mapper.
   * @param derivedAttributes  The derived attributes for this resource mapper.
   */
  @Override
  public void initializeMapper(
      final ResourceDescriptor resourceDescriptor,
      final LDAPSearchParameters searchParameters,
      final LDAPAddParameters addParameters,
      final Collection<AttributeMapper> mappers,
      final Collection<DerivedAttribute> derivedAttributes)
  {
    this.resourceDescriptor  = resourceDescriptor;
    this.addParameters       = addParameters;

    if (searchParameters != null)
    {
      this.searchBaseDN = searchParameters.getBaseDN().trim();
      try
      {
        this.searchFilter = Filter.create(searchParameters.getFilter().trim());
      }
      catch (LDAPException e)
      {
        Debug.debugException(e);
        throw new IllegalArgumentException(e.getExceptionMessage());
      }
    }
    else
    {
      this.searchBaseDN = null;
      this.searchFilter = null;
    }

    if (addParameters != null)
    {
      this.dnConstructor =
          new ConstructedValue(addParameters.getDNTemplate().trim());
    }
    else
    {
      this.dnConstructor = null;
    }

    metaAttributeMapper = createMetaAttributeMapper();

    attributeMappers =
        new HashMap<AttributeDescriptor, AttributeMapper>(mappers.size());
    ldapAttributeTypes = new HashSet<String>();

    for (final AttributeMapper m : mappers)
    {
      attributeMappers.put(m.getAttributeDescriptor(), m);
      ldapAttributeTypes.addAll(m.getLDAPAttributeTypes());
    }

    this.derivedAttributes =
        new HashMap<AttributeDescriptor, DerivedAttribute>(
            derivedAttributes.size());
    for (final DerivedAttribute derivedAttribute : derivedAttributes)
    {
      this.derivedAttributes.put(derivedAttribute.getAttributeDescriptor(),
                                 derivedAttribute);
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
   * @throws SCIMException  If some other error occurs.
   */
  public static List<ResourceMapper> parse(final File file)
      throws JAXBException, SAXException, SCIMException
  {
    final ObjectFactory factory = new ObjectFactory();
    final String packageName = factory.getClass().getPackage().getName();
    // Use the class loader that loaded this class to find the JAXB classes.
    final JAXBContext context = JAXBContext.newInstance(packageName,
        ConfigurableResourceMapper.class.getClassLoader());

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
      final List<DerivedAttribute> derivedAttributes =
          new ArrayList<DerivedAttribute>();
      final AttributeDescriptor[] attributeDescriptors =
          new AttributeDescriptor[resource.getAttribute().size()];
      int i = 0;
      for (final AttributeDefinition attributeDefinition :
          resource.getAttribute())
      {
        final AttributeDescriptor attributeDescriptor =
            createAttributeDescriptor(attributeDefinition,
                                      resource.getSchema());
        attributeDescriptors[i++] = attributeDescriptor;
        final AttributeMapper m =
            AttributeMapper.create(attributeDefinition, attributeDescriptor);
        if (m != null)
        {
          attributeMappers.add(m);
        }

        if (attributeDefinition.getDerivation() != null)
        {
          final DerivedAttribute derivedAttribute =
              DerivedAttribute.create(
                  attributeDefinition.getDerivation().getJavaClass());
          derivedAttribute.initialize(attributeDescriptor);
          derivedAttributes.add(derivedAttribute);
        }
      }

      final ResourceDescriptor resourceDescriptor =
          ResourceDescriptor.create(resource.getName(),
              resource.getDescription(), resource.getSchema(),
              resource.getQueryEndpoint(), attributeDescriptors);

      final ResourceMapper resourceMapper =
          ResourceMapper.create(resource.getMapping());
      resourceMapper.initializeMapper(resourceDescriptor,
                                      resource.getLDAPSearch(),
                                      resource.getLDAPAdd(),
                                      attributeMappers,
                                      derivedAttributes);

      resourceMappers.add(resourceMapper);
    }

    return resourceMappers;
  }



  @Override
  public void finalizeMapper()
  {
    // No implementation required.
  }



  @Override
  public ResourceDescriptor getResourceDescriptor()
  {
    return resourceDescriptor;
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
      if (queryAttributes.isAttributeRequested(m.getAttributeDescriptor()))
      {
        ldapAttributes.addAll(m.getLDAPAttributeTypes());
      }
    }

    for (final Map.Entry<AttributeDescriptor,DerivedAttribute> e :
        derivedAttributes.entrySet())
    {
      if (queryAttributes.isAttributeRequested(e.getKey()))
      {
        final DerivedAttribute derivedAttribute = e.getValue();
        ldapAttributes.addAll(derivedAttribute.getLDAPAttributeTypes());
      }
    }

    return ldapAttributes;
  }



  @Override
  public Entry toLDAPEntry(final SCIMObject scimObject)
      throws LDAPException, InvalidResourceException {
    final Entry entry = new Entry("");

    if (addParameters == null)
    {
      throw new RuntimeException(
          "No LDAP Add Parameters were specified for the " +
              resourceDescriptor.getName() + " Resource Mapper");
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
      throws InvalidResourceException {
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
      throws InvalidResourceException {
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
        // TODO: This should have a reference to the resource descriptor
        AttributeMapper attributeMapper = null;

        if(metaAttributeMapper.getAttributeDescriptor().getName()
                .equalsIgnoreCase(filterAttribute.getAttributeName()))
        {
          attributeMapper = metaAttributeMapper;
        }
        else
        {
          for(AttributeDescriptor attrDescriptor : attributeMappers.keySet())
          {
            if(attrDescriptor.getSchema().equals(
                filterAttribute.getAttributeSchema()) &&
                  attrDescriptor.getName().equalsIgnoreCase(
                    filterAttribute.getAttributeName()))
            {
              attributeMapper = attributeMappers.get(attrDescriptor);
              break;
            }
          }
        }
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
    final AttributePath attributePath = sortParameters.getSortBy();
    AttributeMapper attributeMapper = null;

    //TODO: handle if scimAttributeType == meta here

    for(AttributeDescriptor attributeDescriptor : attributeMappers.keySet())
    {
      if(attributeDescriptor.getSchema().equals(
          attributePath.getAttributeSchema()) &&
          attributeDescriptor.getName().equalsIgnoreCase(
              attributePath.getAttributeName()))
      {
        attributeMapper = attributeMappers.get(attributeDescriptor);
      }
    }
    if (attributeMapper == null)
    {
      throw new RuntimeException("Cannot sort by attribute " +
                                 attributePath);
    }

    final String ldapAttribute = attributeMapper.toLDAPSortAttributeType();

    final boolean reverseOrder = !sortParameters.isAscendingOrder();
    return new ServerSideSortRequestControl(
        new SortKey(ldapAttribute, reverseOrder));
  }



  @Override
  public List<SCIMAttribute> toSCIMAttributes(
      final Entry entry,
      final SCIMQueryAttributes queryAttributes,
      final LDAPInterface ldapInterface) throws InvalidResourceException {
    final List<SCIMAttribute> attributes =
        new ArrayList<SCIMAttribute>();

    for (final AttributeMapper attributeMapper : attributeMappers.values())
    {
      if (queryAttributes.isAttributeRequested(
          attributeMapper.getAttributeDescriptor()))
      {
        final SCIMAttribute attribute = attributeMapper.toSCIMAttribute(entry);
        if (attribute != null)
        {
          final SCIMAttribute paredAttribute =
              queryAttributes.pareAttribute(attribute);
          if (paredAttribute != null)
          {
            attributes.add(paredAttribute);
          }
        }
      }
    }

    if (ldapInterface != null)
    {
      for (final Map.Entry<AttributeDescriptor,DerivedAttribute> e :
          derivedAttributes.entrySet())
      {
        if (queryAttributes.isAttributeRequested(e.getKey()))
        {
          final DerivedAttribute derivedAttribute = e.getValue();
          final SCIMAttribute attribute =
              derivedAttribute.toSCIMAttribute(entry, ldapInterface,
                                               searchBaseDN);
          if (attribute != null)
          {
            final SCIMAttribute paredAttribute =
                queryAttributes.pareAttribute(attribute);
            if (paredAttribute != null)
            {
              attributes.add(paredAttribute);
            }
          }
        }
      }
    }

    return attributes;
  }



  @Override
  public SCIMObject toSCIMObject(final Entry entry,
                                 final SCIMQueryAttributes queryAttributes,
                                 final LDAPInterface ldapInterface)
      throws InvalidResourceException {
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
        toSCIMAttributes(entry, queryAttributes, ldapInterface);

    final SCIMObject scimObject = new SCIMObject();
    for (final SCIMAttribute a : attributes)
    {
      scimObject.addAttribute(a);
    }

    return scimObject;
  }



  /**
   * Create an attribute descriptor from an attribute definition.
   *
   * @param attributeDefinition  The attribute definition.
   * @param resourceSchema       The resource schema URN.
   *
   * @return  A new attribute descriptor.
   *
   * @throws SCIMException  If an error occurs.
   */
  private static AttributeDescriptor createAttributeDescriptor(
      final AttributeDefinition attributeDefinition,
      final String resourceSchema)
      throws SCIMException
  {
    final String schema;
    if (attributeDefinition.getSchema() == null)
    {
      schema = resourceSchema;
    }
    else
    {
      schema = attributeDefinition.getSchema();
    }

    if (attributeDefinition.getSimple() != null)
    {
      final SimpleAttributeDefinition simpleDefinition =
          attributeDefinition.getSimple();

      return AttributeDescriptor.singularSimple(
          attributeDefinition.getName(),
          AttributeDescriptor.DataType.parse(
              simpleDefinition.getDataType().value()),
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          simpleDefinition.isCaseExact());
    }
    else if (attributeDefinition.getComplex() != null)
    {
      final ComplexAttributeDefinition complexDefinition =
          attributeDefinition.getComplex();

      final AttributeDescriptor[] subAttributes =
          new AttributeDescriptor[complexDefinition.getSubAttribute().size()];

      int i = 0;
      for (final SubAttributeDefinition subAttributeDefinition :
          complexDefinition.getSubAttribute())
      {
          subAttributes[i++] = AttributeDescriptor.singularSimple(
                  subAttributeDefinition.getName(),
                  AttributeDescriptor.DataType.parse(
                      subAttributeDefinition.getDataType().value()),
                  subAttributeDefinition.getDescription(),
                  schema,
                  subAttributeDefinition.isReadOnly(),
                  subAttributeDefinition.isRequired(),
                  subAttributeDefinition.isCaseExact());
      }

      return AttributeDescriptor.singularComplex(
          attributeDefinition.getName(),
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          subAttributes);
    }
    else if (attributeDefinition.getSimplePlural() != null)
    {
      final SimplePluralAttributeDefinition simplePluralDefinition =
          attributeDefinition.getSimplePlural();

      final String[] pluralTypes =
          new String[simplePluralDefinition.getPluralType().size()];

      int i = 0;
      for (final PluralType pluralType : simplePluralDefinition.getPluralType())
      {
        pluralTypes[i++] = pluralType.getName();
      }

      return AttributeDescriptor.pluralSimple(
          attributeDefinition.getName(),
          AttributeDescriptor.DataType.parse(
              simplePluralDefinition.getDataType().value()),
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          simplePluralDefinition.isCaseExact(),
          pluralTypes);
    }
    else if (attributeDefinition.getComplexPlural() != null)
    {
      final ComplexPluralAttributeDefinition complexPluralDefinition =
          attributeDefinition.getComplexPlural();

      final String[] pluralTypes =
          new String[complexPluralDefinition.getPluralType().size()];

      int i = 0;
      for (final PluralType pluralType :
          complexPluralDefinition.getPluralType())
      {
        pluralTypes[i++] = pluralType.getName();
      }

      final AttributeDescriptor[] subAttributes =
          new AttributeDescriptor[
              complexPluralDefinition.getSubAttribute().size()];

      i = 0;
      for (final SubAttributeDefinition subAttributeDefinition :
          complexPluralDefinition.getSubAttribute())
      {
          subAttributes[i++] = AttributeDescriptor.singularSimple(
                  subAttributeDefinition.getName(),
                  AttributeDescriptor.DataType.parse(
                      subAttributeDefinition.getDataType().value()),
                  subAttributeDefinition.getDescription(),
                  schema,
                  subAttributeDefinition.isReadOnly(),
                  subAttributeDefinition.isRequired(),
                  subAttributeDefinition.isCaseExact());
      }
      return AttributeDescriptor.pluralComplex(
          attributeDefinition.getName(),
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          pluralTypes, subAttributes);
    }
    else
    {
      final SCIMException e =
          new ServerErrorException(
              "Attribute definition '" + attributeDefinition.getName() +
              "' does not have a simple, complex, simplePlural or " +
              "complexPlural element");
      Debug.debugCodingError(e);
      throw e;
    }
  }


  /**
   * Gets an AttributeMapper for the SCIM Meta object (part of the core schema).
   *
   * @return an AttributeMapper
   */
  private AttributeMapper createMetaAttributeMapper()
  {
    List<SubAttributeTransformation> transformations =
              new ArrayList<SubAttributeTransformation>();

    final Transformation dateTransformation =
        Transformation.create(GeneralizedTimeTransformation.class.getName());

    transformations.add(new SubAttributeTransformation("created",
           new AttributeTransformation("createTimestamp", dateTransformation)));

    transformations.add(new SubAttributeTransformation("lastModified",
           new AttributeTransformation("modifyTimestamp", dateTransformation)));

    return new ComplexSingularAttributeMapper(
                    CoreSchema.META_DESCRIPTOR, transformations);
  }
}
