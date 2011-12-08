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
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.ServerErrorException;
import com.unboundid.scim.sdk.SortParameters;
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
 * Resource mappers may be used to override the behaviour of any part of the
 * mapping process, which includes:
 * <ul>
 *   <li>Retrieve the set of LDAP attribute types that should be requested in
 *   order to return the specified query attributes.</li>
 *   <li>Mapping a SCIM filter to a LDAP filter</li>
 *   <li>Mapping a SCIM filter component to an LDAP filter component</li>
 *   <li>Mapping the attributes in a SCIM object to LDAP attributes</li>
 *   <li>Getting the search base DN to be used for querying</li>
 *   <li>Mapping a set of SCIM sort parameters to a LDAP sort control</li>
 *   <li>Mapping the attributes in a LDAP entry to SCIM attributes</li>
 *   <li>Mapping an LDAP entry to a SCIM resource</li>
 *   <li>Mapping the replacement attributes in a SCIM object to LDAP
 *   modifications</li>
 *   <li>Constructing an LDAP entry from the provided SCIM object.</li>
 * </ul>
 * <BR><BR>
 * Resource mappers should be used to handle situations that are too complex to
 * be expressed in the configuration file and can not be implemented using
 * custom attribute value transformations and/or attribute derivations.
 * <BR><BR>
 * To use a custom resource mapper class, use the <tt>mapper</tt> attribute to
 * specify the implementation class in any <tt>resource</tt> configuration
 * elements. For example:
 * <BR><BR>
 * <PRE>
 * &lt;resource name=&quot;User&quot;
 *  schema=&quot;urn:scim:schemas:core:1.0&quot;
 *  mapper=&quot;com.unboundid.example.ExampleResourceMapper&quot;
 * &#47;&gt;
 * </PRE>
 * <BR><BR>
 * This API is volatile and could change in future releases.
 */
public class ResourceMapper
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
   * Create a new instance of this resource mapper. All resource mappers must
   * provide a default constructor, but any initialization should be done
   * in the {@code initializeMapper()} method.
   */
  protected ResourceMapper()
  {
    // No implementation required.
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
        ResourceMapper.class.getClassLoader());

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
              resource.getEndpoint(), attributeDescriptors);

      final ResourceMapper resourceMapper =
          create(resource.getMapping());
      resourceMapper.initializeMapper(resourceDescriptor,
                                      resource.getLDAPSearch(),
                                      resource.getLDAPAdd(),
                                      attributeMappers,
                                      derivedAttributes);

      resourceMappers.add(resourceMapper);
    }

    return resourceMappers;
  }

  /**
   * Create an attribute descriptor from an attribute definition.
   *
   * @param attributeDefinition  The attribute definition.
   * @param resourceSchema       The resource schema URN.
   *
   * @return  A new attribute descriptor.
   *
   * @throws com.unboundid.scim.sdk.SCIMException  If an error occurs.
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

      return AttributeDescriptor.simple(
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
          subAttributes[i++] = AttributeDescriptor.simple(
              subAttributeDefinition.getName(),
              AttributeDescriptor.DataType.parse(
                  subAttributeDefinition.getDataType().value()),
              subAttributeDefinition.getDescription(),
              schema,
              subAttributeDefinition.isReadOnly(),
              subAttributeDefinition.isRequired(),
              subAttributeDefinition.isCaseExact());
      }

      return AttributeDescriptor.complex(
          attributeDefinition.getName(),
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          subAttributes);
    }
    else if (attributeDefinition.getSimpleMultiValued() != null)
    {
      final SimpleMultiValuedAttributeDefinition simpleMultiValuedDefinition =
          attributeDefinition.getSimpleMultiValued();

      final String[] CanonicalValues =
          new String[simpleMultiValuedDefinition.getCanonicalValue().size()];

      int i = 0;
      for (final CanonicalValue CanonicalValue :
          simpleMultiValuedDefinition.getCanonicalValue())
      {
        CanonicalValues[i++] = CanonicalValue.getName();
      }

      return AttributeDescriptor.simpleMultiValued(
          attributeDefinition.getName(),
          AttributeDescriptor.DataType.parse(
              simpleMultiValuedDefinition.getDataType().value()),
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          simpleMultiValuedDefinition.isCaseExact(),
          CanonicalValues);
    }
    else if (attributeDefinition.getComplexMultiValued() != null)
    {
      final ComplexMultiValuedAttributeDefinition complexMultiValuedDefinition =
          attributeDefinition.getComplexMultiValued();

      final String[] CanonicalValues =
          new String[complexMultiValuedDefinition.getCanonicalValue().size()];

      int i = 0;
      for (final CanonicalValue CanonicalValue :
          complexMultiValuedDefinition.getCanonicalValue())
      {
        CanonicalValues[i++] = CanonicalValue.getName();
      }

      final AttributeDescriptor[] subAttributes =
          new AttributeDescriptor[
              complexMultiValuedDefinition.getSubAttribute().size()];

      i = 0;
      for (final SubAttributeDefinition subAttributeDefinition :
          complexMultiValuedDefinition.getSubAttribute())
      {
          subAttributes[i++] = AttributeDescriptor.simple(
              subAttributeDefinition.getName(),
              AttributeDescriptor.DataType.parse(
                  subAttributeDefinition.getDataType().value()),
              subAttributeDefinition.getDescription(),
              schema,
              subAttributeDefinition.isReadOnly(),
              subAttributeDefinition.isRequired(),
              subAttributeDefinition.isCaseExact());
      }
      return AttributeDescriptor.complexMultiValued(
          attributeDefinition.getName(),
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          CanonicalValues, subAttributes);
    }
    else
    {
      final SCIMException e =
          new ServerErrorException(
              "Attribute definition '" + attributeDefinition.getName() +
              "' does not have a simple, complex, simpleMultiValued or " +
              "complexMultiValued element");
      Debug.debugCodingError(e);
      throw e;
    }
  }


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
   * Performs any cleanup which may be necessary when this resource mapper is
   * to be taken out of service.
   */
  public void finalizeMapper()
  {
    // No implementation required.
  }



  /**
   * Retrieve the ResourceDescriptor of the SCIM resource handled by this
   * resource mapper.
   *
   * @return  The ResourceDescriptor of the SCIM resource handled by this
   *          resource mapper.
   */
  public ResourceDescriptor getResourceDescriptor()
  {
    return resourceDescriptor;
  }



  /**
   * Indicates whether this mapper supports querying of resources.
   *
   * @return  {@code true} if this mapper supports resource query.
   */
  public boolean supportsQuery()
  {
    return searchBaseDN != null;
  }



  /**
   * Indicates whether this mapper supports creation of new resources through
   * {@link #toLDAPEntry(com.unboundid.scim.sdk.SCIMObject)}.
   *
   * @return  {@code true} if this mapper supports resource creation.
   */
  public boolean supportsCreate()
  {
    return addParameters != null;
  }



  /**
   * Retrieve the set of LDAP attribute types that should be requested in order
   * to return the specified query attributes.
   *
   * @param queryAttributes  The requested query attributes.
   *
   * @return  The set of LDAP attribute types that should be requested.
   */
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

  /**
   * Construct an LDAP entry from the provided SCIM object.
   *
   * @param scimObject  The SCIM object to form the contents of the entry.
   *
   * @return  An LDAP entry.
   *
   * @throws LDAPException  If the entry could not be constructed.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
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


  /**
   * Map the attributes in a SCIM object to LDAP attributes.
   *
   * @param scimObject  The object containing attributes to be mapped.
   *
   * @return  A list of LDAP attributes mapped from the SCIM object. This should
   *          never be {@code null} but may be empty.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public List<Attribute> toLDAPAttributes(final SCIMObject scimObject)
      throws InvalidResourceException {
    final List<Attribute> attributes = new ArrayList<Attribute>();

    for (final AttributeMapper attributeMapper : attributeMappers.values())
    {
      attributeMapper.toLDAPAttributes(scimObject, attributes);
    }

    return attributes;
  }

  /**
   * Map the replacement attributes in a SCIM object to LDAP modifications.
   *
   * @param currentEntry  The current LDAP entry representing the SCIM object.
   * @param scimObject  The object containing attributes to be mapped.
   *
   * @return  A list of LDAP modifications mapped from the SCIM object. This
   *          should never be {@code null} but may be empty.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
  public List<Modification> toLDAPModifications(final Entry currentEntry,
                                                final SCIMObject scimObject)
      throws InvalidResourceException {
    final List<Attribute> attributes = toLDAPAttributes(scimObject);
    final Entry entry = new Entry(currentEntry.getDN(), attributes);

    final String[] ldapAttributesArray =
        ldapAttributeTypes.toArray(new String[ldapAttributeTypes.size()]);

    return Entry.diff(currentEntry, entry, true, false, ldapAttributesArray);
  }

  /**
   * Map the provided SCIM filter to an LDAP filter.
   *
   * @param filter  The SCIM filter to be mapped, or {@code null} if no filter
   *                parameters were provided.
   *
   * @return  An LDAP filter or {@code null} if the SCIM filter could not be
   *          mapped and will not match anything.
   * @throws InvalidResourceException if the filter contains an undefined
   *                                  attribute.
   */
  public Filter toLDAPFilter(final SCIMFilter filter)
      throws InvalidResourceException
  {
    if (filter == null)
    {
      return searchFilter;
    }

    final Filter filterComponent = toLDAPFilterComponent(filter);

    if (filterComponent == null)
    {
      return null;
    }
    else if(searchFilter != null)
    {
      return Filter.createANDFilter(filterComponent, searchFilter);
    }
    else
    {
      return filterComponent;
    }
  }


  /**
   * Get the search base DN to be used for querying.
   *
   * @return  The search base DN to be used for querying.
   */
  public String getSearchBaseDN()
  {
    return searchBaseDN;
  }



  /**
   * Map the provided SCIM sort parameters to an LDAP sort control.
   *
   * @param sortParameters  The SCIM sort parameters to be mapped.
   *
   * @return  An LDAP sort control.
   * @throws InvalidResourceException if the sort parameters are invalid.
   */
  public Control toLDAPSortControl(final SortParameters sortParameters)
      throws InvalidResourceException
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
      resourceDescriptor.getAttribute(attributePath.getAttributeSchema(),
          attributePath.getAttributeName());
      return null;
    }

    return attributeMapper.toLDAPSortAttributeType(sortParameters);
  }



  /**
   * Map the attributes in an LDAP entry to SCIM attributes.
   *
   * @param entry            The LDAP entry containing attributes to be
   *                         mapped.
   * @param queryAttributes  The set of SCIM attributes that are requested
   *                         to be returned.
   * @param ldapInterface    An optional LDAP interface that can be used to
   *                         derive attributes from other entries.
   *
   * @return  A list of SCIM attributes mapped from the LDAP entry. This should
   *          never be {@code null} but may be empty.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
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



  /**
   * Map an LDAP entry to a SCIM resource.
   *
   * @param entry            The LDAP entry containing attributes to be
   *                         mapped.
   * @param queryAttributes  The set of SCIM attributes that are requested
   *                         to be returned.
   * @param ldapInterface    An optional LDAP interface that can be used to
   *                         derive attributes from other entries.
   *
   * @return  A SCIM object mapped from the LDAP entry, or {@code null} if this
   *          entry cannot be mapped to a SCIM object.
   * @throws InvalidResourceException if the mapping violates the schema.
   */
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
   * Create a resource mapper from the name of a class that extends the
   * {@code ResourceMapper} abstract class.
   *
   * @param className  The name of a class that extends {@code ResourceMapper}.
   *
   * @return  A new instance of the ResourceMapper class.
   */
  public static ResourceMapper create(final String className)
  {
    if (className == null)
    {
      return new ResourceMapper();
    }

    Class clazz;
    try
    {
      clazz = Class.forName(className, true,
          ResourceMapper.class.getClassLoader());
    }
    catch (ClassNotFoundException e)
    {
      Debug.debugException(e);
      throw new IllegalArgumentException(
          "Class '" + className + "' not found", e);
    }

    final Object object;
    try
    {
      object = clazz.newInstance();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new IllegalArgumentException(
          "Cannot create instance of class '" + className + "'", e);
    }

    if (!(object instanceof ResourceMapper))
    {
      throw new IllegalArgumentException(
          "Class '" + className + "' is not a ResourceMapper");
    }

    return (ResourceMapper)object;
  }

  /**
   * Map a SCIM filter component to an LDAP filter component.
   *
   * @param filter  The SCIM filter component to be mapped.
   *
   * @return  The LDAP filter component, or {@code null} if the filter
   *          component could not be mapped and will not match anything.
   * @throws InvalidResourceException if the filter contains an undefined
   *                                  attribute.
   */
  private Filter toLDAPFilterComponent(final SCIMFilter filter)
      throws InvalidResourceException {
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
          else
          {
            // AND nothing is still nothing
            return null;
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
        return Filter.createORFilter(orFilterComponents);

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
        else
        {
          // Throw an error if this is an undefined attribute.
          resourceDescriptor.getAttribute(filterAttribute.getAttributeSchema(),
              filterAttribute.getAttributeName());
        }
        break;
    }

    return null;
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
