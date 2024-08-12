/*
 * Copyright 2011-2024 Ping Identity Corporation
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
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.AttributePath;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DebugType;
import com.unboundid.scim.sdk.ForbiddenException;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.PreconditionFailedException;
import com.unboundid.scim.sdk.ResourceConflictException;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMFilter;
import com.unboundid.scim.sdk.SCIMFilterType;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.ServerErrorException;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.StaticUtils;
import com.unboundid.scim.sdk.UnauthorizedException;
import com.unboundid.util.Validator;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

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
  protected ResourceDescriptor resourceDescriptor;

  /**
   * The LDAPSearchResolver to resolve resources handled by this resource
   * manager.
   */
  protected LDAPSearchResolver searchResolver;

  /**
   * The LDAP Add parameters.
   */
  protected LDAPAddParameters addParameters;

  /**
   * A DN constructed value for the DN template.
   */
  protected ConstructedValue dnConstructor;

  /**
   * The attribute mappers for this resource mapper.
   */
  protected Map<AttributeDescriptor, AttributeMapper> attributeMappers;

  /**
   * The derived attributes for this resource mapper.
   */
  protected Map<AttributeDescriptor,DerivedAttribute> derivedAttributes;

  /**
   * The internal AttributeMapper for the Meta object. This allows the
   * toLDAPFilter() code to generate LDAP filters which are based on the meta
   * information, even though there are no explicit mappings for Meta set up in
   * resources.xml.
   */
  protected AttributeMapper metaAttributeMapper;

  /**
   * The internal AttributeMapper for the id attribute. This allows the
   * toLDAPFilter() code to generate LDAP filters which are based on the id
   * information, even though there are no explicit mappings for id set up in
   * resources.xml.
   */
  protected AttributeMapper idAttributeMapper;

  /**
   * The internal AttributeMapper for the password attribute. This allows a
   * caller to determine which LDAP attribute is mapped to the SCIM password
   * attribute, if any.
   */
  protected AttributeMapper passwordAttributeMapper;

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
   * Parse an XML file defining a set of resource mappings. The LDAP attribute
   * mappings are not validated against an LDAP schema.
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
    return parse(file, null);
  }



  /**
   * Parse an XML file defining a set of resource mappings.
   *
   * @param file        An XML file defining a set of resource mappings.
   * @param ldapSchema  An LDAP schema to validate the LDAP attribute mappings
   *                    against. This parameter may be {@code null} if LDAP
   *                    schema validation is not required.
   *
   * @return  A list of resource mappers.
   *
   * @throws JAXBException  If an error occurs during the parsing.
   * @throws SAXException   If the XML schema cannot be instantiated.
   * @throws SCIMException  If some other error occurs.
   */
  public static List<ResourceMapper> parse(
      final File file,
      final com.unboundid.ldap.sdk.schema.Schema ldapSchema)
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

    final Map<String,LDAPSearchResolver> ldapSearchResolvers =
        new HashMap<String, LDAPSearchResolver>();
    for (final LDAPSearchParameters p : resources.getLDAPSearch())
    {
      if (p.getResourceIDMapping() != null)
      {
        if (ldapSchema != null)
        {
          final String ldapAttribute =
              p.getResourceIDMapping().getLdapAttribute();
          if (ldapSchema.getAttributeType(ldapAttribute) == null)
          {
            throw new ServerErrorException(
                "The LDAP attribute '" + ldapAttribute + "' referenced in " +
                "LDAP Search Parameters '" + p.getId() + "' does not exist " +
                "in the LDAP schema");
          }
        }
      }

      try
      {
        ldapSearchResolvers.put(StaticUtils.toLowerCase(p.getId()),
                    new LDAPSearchResolver(p, Collections.<DN>emptySet()));
      }
      catch (LDAPException e)
      {
        Debug.debugException(e);
        throw new ServerErrorException(e.getMessage());
      }
    }

    final List<ResourceMapper> resourceMappers =
        new ArrayList<ResourceMapper>();

    for (final ResourceDefinition resource : resources.getResource())
    {
      final LDAPSearchParameters ldapSearch;
      Object o = resource.getLDAPSearchRef().getIdref();
      if(o != null)
      {
        ldapSearch = (LDAPSearchParameters) o;
      }
      else
      {
        throw new JAXBException(
                "Cannot find <LDAPSearch> element referenced by " +
                "the \"" + resource.getName() + "\" resource.");
      }

      final List<AttributeMapper> attributeMappers =
          new ArrayList<AttributeMapper>();
      final List<DerivedAttribute> derivedAttributes =
          new ArrayList<DerivedAttribute>();
      final List<AttributeDescriptor> attributeDescriptors =
         new ArrayList<AttributeDescriptor>(resource.getAttribute().size() + 2);
      attributeDescriptors.add(CoreSchema.ID_DESCRIPTOR);
      attributeDescriptors.add(CoreSchema.META_DESCRIPTOR);
      for (final AttributeDefinition attributeDefinition :
          resource.getAttribute())
      {
        final AttributeDescriptor attributeDescriptor =
            createAttributeDescriptor(attributeDefinition);

        if (attributeDescriptor.equals(CoreSchema.ID_DESCRIPTOR))
        {
          throw new ServerErrorException(
              "An attribute definition is not permitted for the 'id' " +
              "attribute. The mapping for this attribute may only be " +
              "specified in a LDAPSearch element.");
        }

        final AttributeMapper m =
            AttributeMapper.create(attributeDefinition, attributeDescriptor,
                                   ldapSchema);
        if (m != null)
        {
          attributeMappers.add(m);
        }

        if (attributeDefinition.getDerivation() != null)
        {
          final DerivedAttribute derivedAttribute =
              DerivedAttribute.create(
                  attributeDefinition.getDerivation().getJavaClass(),
                  attributeDefinition.getDerivation().getAny());
          if(derivedAttribute instanceof MembersDerivedAttribute ||
                  derivedAttribute instanceof GroupsDerivedAttribute)
          {
            if(derivedAttribute.getArguments().containsKey(
                                DerivedAttribute.LDAP_SEARCH_REF))
            {
              String id = derivedAttribute.getArguments().get(
                            DerivedAttribute.LDAP_SEARCH_REF).toString();
              final LDAPSearchResolver resolver =
                  ldapSearchResolvers.get(StaticUtils.toLowerCase(id));
              if (resolver != null)
              {
                //Replace the String in the arguments map with a
                //LDAPSearchResolver instance. The DerivedAttribute
                //initialize() method will take advantage of this.
                derivedAttribute.getArguments().put(
                    DerivedAttribute.LDAP_SEARCH_REF, resolver);
              }
              else
              {
                throw new JAXBException(
                        "Cannot find <LDAPSearch> element with id=\"" + id +
                        "\"," + "which is referenced by a Derived Attribute (" +
                        derivedAttribute.getClass().getName() + ")");
              }

            }
          }
          derivedAttribute.initialize(attributeDescriptor);
          derivedAttributes.add(derivedAttribute);
        }

        // Only include the attribute descriptor if there is a mapping or it is
        // derived.
        if(m != null || attributeDefinition.getDerivation() != null)
        {
          attributeDescriptors.add(attributeDescriptor);
        }
        else
        {
          Debug.debug(Level.WARNING, DebugType.OTHER,
              "Definition for attribute " + new AttributePath(
                  attributeDefinition.getSchema(),
                  attributeDefinition.getName(), null).toString() +
                  " skipped because it does not have a mapping or " +
                  "derivation defined.");
        }
      }

      final ResourceDescriptor resourceDescriptor =
          ResourceDescriptor.create(resource.getName(),
              resource.getDescription(), resource.getSchema(),
              resource.getEndpoint(), attributeDescriptors.toArray(
              new AttributeDescriptor[attributeDescriptors.size()]));

      final ResourceMapper resourceMapper = create(resource.getMapping());
      final LDAPSearchResolver ldapSearchResolver =
          ldapSearchResolvers.get(StaticUtils.toLowerCase(ldapSearch.getId()));

      if (resource.getLDAPAdd() != null)
      {
        if (ldapSchema != null)
        {
          for (final FixedAttribute fixedAttribute :
              resource.getLDAPAdd().getFixedAttribute())
          {
            final String ldapAttribute = fixedAttribute.getLdapAttribute();
            if (ldapSchema.getAttributeType(ldapAttribute) == null)
            {
              throw new ServerErrorException(
                  "The LDAP attribute '" + ldapAttribute + "' referenced by " +
                  "a fixedAttribute element is not defined in the LDAP schema");
            }
          }
        }
      }

      resourceMapper.initializeMapper(
          resourceDescriptor,
          ldapSearchResolver,
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
   *
   * @return  A new attribute descriptor.
   *
   * @throws com.unboundid.scim.sdk.SCIMException  If an error occurs.
   */
  private static AttributeDescriptor createAttributeDescriptor(
      final AttributeDefinition attributeDefinition)
      throws SCIMException
  {
    final String schema = attributeDefinition.getSchema();

    if (attributeDefinition.getSimple() != null)
    {
      final SimpleAttributeDefinition simpleDefinition =
          attributeDefinition.getSimple();

      return AttributeDescriptor.createAttribute(
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
          subAttributes[i++] = AttributeDescriptor.createSubAttribute(
              subAttributeDefinition.getName(),
              AttributeDescriptor.DataType.parse(
                  subAttributeDefinition.getDataType().value()),
              subAttributeDefinition.getDescription(),
              schema,
              subAttributeDefinition.isReadOnly(),
              subAttributeDefinition.isRequired(),
              subAttributeDefinition.isCaseExact());
      }

      return AttributeDescriptor.createAttribute(
          attributeDefinition.getName(),
          AttributeDescriptor.DataType.COMPLEX,
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          false,
          subAttributes);
    }
    else if (attributeDefinition.getSimpleMultiValued() != null)
    {
      final SimpleMultiValuedAttributeDefinition simpleMultiValuedDefinition =
          attributeDefinition.getSimpleMultiValued();

      final String[] canonicalValues =
          new String[simpleMultiValuedDefinition.getCanonicalValue().size()];

      int i = 0;
      for (final CanonicalValue CanonicalValue :
          simpleMultiValuedDefinition.getCanonicalValue())
      {
        canonicalValues[i++] = CanonicalValue.getName();
      }

      AttributeDescriptor.DataType dataType =
          AttributeDescriptor.DataType.parse(
              simpleMultiValuedDefinition.getDataType().value());

      if(canonicalValues.length > 0)
      {
        return AttributeDescriptor.createMultiValuedAttribute(
            attributeDefinition.getName(),
            simpleMultiValuedDefinition.getChildName(),
            attributeDefinition.getDescription(),
            schema,
            attributeDefinition.isReadOnly(),
            attributeDefinition.isRequired(),
            simpleMultiValuedDefinition.isCaseExact(),
            CoreSchema.createMultiValuedValueDescriptor(schema, dataType),
            CoreSchema.createMultiValuedTypeDescriptor(schema,
                canonicalValues));
      }
      else
      {
        return AttributeDescriptor.createMultiValuedAttribute(
            attributeDefinition.getName(),
            simpleMultiValuedDefinition.getChildName(),
            attributeDefinition.getDescription(),
            schema,
            attributeDefinition.isReadOnly(),
            attributeDefinition.isRequired(),
            simpleMultiValuedDefinition.isCaseExact(),
            CoreSchema.createMultiValuedValueDescriptor(schema, dataType));
      }
    }
    else if (attributeDefinition.getComplexMultiValued() != null)
    {
      final ComplexMultiValuedAttributeDefinition complexMultiValuedDefinition =
          attributeDefinition.getComplexMultiValued();

      final String[] canonicalValues =
          new String[complexMultiValuedDefinition.getCanonicalValue().size()];

      int i = 0;
      for (final CanonicalValue CanonicalValue :
          complexMultiValuedDefinition.getCanonicalValue())
      {
        canonicalValues[i++] = CanonicalValue.getName();
      }

      AttributeDescriptor[] subAttributes = new AttributeDescriptor[
          complexMultiValuedDefinition.getSubAttribute().size()];
      i = 0;
      if(canonicalValues.length > 0)
      {
        subAttributes = new AttributeDescriptor[
            complexMultiValuedDefinition.getSubAttribute().size() + 1];
        subAttributes[
            complexMultiValuedDefinition.getSubAttribute().size()] =
            CoreSchema.createMultiValuedTypeDescriptor(schema, canonicalValues);
      }
      for (final SubAttributeDefinition subAttributeDefinition :
          complexMultiValuedDefinition.getSubAttribute())
      {
          subAttributes[i++] = AttributeDescriptor.createSubAttribute(
              subAttributeDefinition.getName(),
              AttributeDescriptor.DataType.parse(
                  subAttributeDefinition.getDataType().value()),
              subAttributeDefinition.getDescription(),
              schema,
              subAttributeDefinition.isReadOnly(),
              subAttributeDefinition.isRequired(),
              subAttributeDefinition.isCaseExact());
      }
      return AttributeDescriptor.createMultiValuedAttribute(
          attributeDefinition.getName(),
          complexMultiValuedDefinition.getChildName(),
          attributeDefinition.getDescription(),
          schema,
          attributeDefinition.isReadOnly(),
          attributeDefinition.isRequired(),
          false, subAttributes);
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
   * @param searchResolver     The LDAP Search resolver.
   * @param addParameters      The LDAP Add parameters, or {@code null} if there
   *                           are none defined.
   * @param mappers            The attribute mappers for this resource mapper.
   * @param derivedAttributes  The derived attributes for this resource mapper.
   */
  public void initializeMapper(
      final ResourceDescriptor resourceDescriptor,
      final LDAPSearchResolver searchResolver,
      final LDAPAddParameters addParameters,
      final Collection<AttributeMapper> mappers,
      final Collection<DerivedAttribute> derivedAttributes)
  {
    this.resourceDescriptor  = resourceDescriptor;
    this.addParameters       = addParameters;
    this.searchResolver      = searchResolver;

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
    idAttributeMapper = searchResolver.getIdAttributeMapper();

    attributeMappers =
        new HashMap<AttributeDescriptor, AttributeMapper>(mappers.size());

    for (final AttributeMapper m : mappers)
    {
      attributeMappers.put(m.getAttributeDescriptor(), m);
      if (m instanceof PasswordAttributeMapper)
      {
        passwordAttributeMapper = m;
      }
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
   * Gets the default schema URI that is assumed by this ResourceMapper.
   * Typically this should be the SCIM Core Schema, but subclasses may choose
   * to override this.
   *
   * @return a SCIM schema URN.
   */
  public String getDefaultSchemaURI()
  {
    return SCIMConstants.SCHEMA_URI_CORE;
  }



  /**
   * Indicates whether this mapper supports querying of resources.
   *
   * @return  {@code true} if this mapper supports resource query.
   */
  public boolean supportsQuery()
  {
    return searchResolver != null;
  }



  /**
   * Indicates whether this mapper supports creation of new resources through
   * {@code toLDAPEntry}.
   *
   * @return  {@code true} if this mapper supports resource creation.
   */
  public boolean supportsCreate()
  {
    return addParameters != null;
  }



  /**
   * Retrieve the set of LDAP attribute types that should be requested in order
   * to return the specified query attributes. Any LDAP attribute types mapped
   * from the password attribute will not be included.
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
      if (queryAttributes.isAttributeRequested(m.getAttributeDescriptor()) &&
          !(m instanceof PasswordAttributeMapper))
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

    searchResolver.addIdAttribute(ldapAttributes);

    if (queryAttributes.isDebugSearchIndex())
    {
      ldapAttributes.add("debugsearchindex");
    }

    return ldapAttributes;
  }



  /**
   * Retrieve the set of LDAP attribute types that are mapped from the given
   * set of SCIM attributes.
   *
   * @param scimAttributes  The SCIM attributes to map.
   *
   * @return  The set of LDAP attribute types that are derived from the given
   *          SCIM attributes.
   * @throws InvalidResourceException if the SCIM attributes contains an
   *                                  undefined attribute.
   */
  protected Set<String> toLDAPAttributeTypes(
      final Set<AttributePath> scimAttributes)
      throws InvalidResourceException
  {
    final Set<String> ldapAttributes = new LinkedHashSet<String>();
    for(AttributePath scimAttribute : scimAttributes)
    {
      final AttributeDescriptor attributeDescriptor =
          resourceDescriptor.getAttribute(
              scimAttribute.getAttributeSchema(),
              scimAttribute.getAttributeName());

      AttributeMapper attributeMapper =
          attributeMappers.get(attributeDescriptor);

      if (attributeMapper != null)
      {
        ldapAttributes.addAll(
            attributeMapper.toLDAPAttributeTypes(scimAttribute));
      }

      DerivedAttribute derivedAttribute =
          derivedAttributes.get(attributeDescriptor);
      if (derivedAttribute != null)
      {
        ldapAttributes.addAll(
            derivedAttribute.toLDAPAttributeTypes(scimAttribute));
      }
    }

    return ldapAttributes;
  }



  /**
   * Construct an LDAP entry from the provided SCIM object. This method, which
   * does not need an LDAP interface argument, is provided for the Sync Server
   * SCIM Sync Destination.
   *
   * @param scimObject       The SCIM object to form the contents of the entry.
   *
   * @return  An LDAP entry.
   *
   * @throws SCIMException  If the entry could not be constructed.
   */
  public Entry toLDAPEntry(final SCIMObject scimObject)
      throws SCIMException
  {
    return toLDAPEntry(scimObject, null);
  }



  /**
   * Construct an LDAP entry from the provided SCIM object.
   *
   * @param scimObject       The SCIM object to form the contents of the entry.
   * @param ldapInterface    An optional LDAP interface that can be used to
   *                         derive attributes from other entries.
   *
   * @return  An LDAP entry.
   *
   * @throws SCIMException  If the entry could not be constructed.
   */
  public Entry toLDAPEntry(final SCIMObject scimObject,
                           final LDAPRequestInterface ldapInterface)
      throws SCIMException
  {
    Entry entry = new Entry("");

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

    for (final Attribute a : toLDAPAttributes(scimObject, ldapInterface))
    {
      entry.addAttribute(a);
    }

    // TODO allow SCIM object values to be referenced
    entry.setDN(constructEntryDN(entry));

    entry = searchResolver.preProcessAddEntry(entry);
    return entry;
  }



  /**
   * Map the attributes in a SCIM object to LDAP attributes.
   *
   * @param scimObject       The object containing attributes to be mapped.
   * @param ldapInterface    An optional LDAP interface that can be used to
   *                         derive attributes from other entries.
   *
   * @return  A list of LDAP attributes mapped from the SCIM object. This should
   *          never be {@code null} but may be empty.
   *
   * @throws SCIMException  If the attributes could not be mapped.
   */
  public List<Attribute> toLDAPAttributes(
      final SCIMObject scimObject,
      final LDAPRequestInterface ldapInterface)
      throws SCIMException
  {
    final List<Attribute> attributes = new ArrayList<Attribute>();

    if (ldapInterface != null)
    {
      for (final Map.Entry<AttributeDescriptor,DerivedAttribute> e :
          derivedAttributes.entrySet())
      {
        final DerivedAttribute derivedAttribute = e.getValue();
        derivedAttribute.toLDAPAttributes(scimObject, attributes,
                                          ldapInterface, searchResolver);
      }
    }

    for (final AttributeMapper attributeMapper : attributeMappers.values())
    {
      attributeMapper.toLDAPAttributes(scimObject, attributes);
    }

    return attributes;
  }



  /**
   * Retrieve the set of all modifiable LDAP attribute types that should be
   * requested in order to return the current LDAP entry representing the SCIM
   * object. This entry may then be used with {@code toLDAPModifications} to
   * map the replacement attributes to LDAP modifications.
   *
   * @param scimObject The object containing attributes to be mapped.
   * @return The set of LDAP attribute types that should be requested.
   * @throws InvalidResourceException if the SCIM attributes contains an
   *                                  undefined attribute.
   */
  public Set<String> getModifiableLDAPAttributeTypes(
      final SCIMObject scimObject)
      throws InvalidResourceException
  {
    final Set<String> ldapAttributeTypes =
            new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
    final boolean hasPasswordAttribute =
        scimObject.hasAttribute(SCIMConstants.SCHEMA_URI_CORE, "password");

    // Retrieve all LDAP attributes that are mapped from SCIM attributes.
    for (final AttributeMapper m : attributeMappers.values())
    {
      // Exclude read only SCIM attributes, and passwords in some cases.
      // The password attribute is a special case as it should not be retrieved
      // unless it is included in the updated SCIM object to be mapped.
      if(!m.getAttributeDescriptor().isReadOnly() &&
         (hasPasswordAttribute || !(m instanceof PasswordAttributeMapper)))
      {
        ldapAttributeTypes.addAll(m.getLDAPAttributeTypes());
      }
    }

    for (final Map.Entry<AttributeDescriptor,DerivedAttribute> e :
            derivedAttributes.entrySet())
    {
      if (!e.getKey().isReadOnly())
      {
        final DerivedAttribute derivedAttribute = e.getValue();
        ldapAttributeTypes.addAll(derivedAttribute.getLDAPAttributeTypes());
      }
    }

    SCIMAttribute meta = scimObject.getAttribute(
            SCIMConstants.SCHEMA_URI_CORE, "meta");
    if (meta != null)
    {
      //The "attributes" sub-attribute is specifically for deleting attribute
      //values when performing a PATCH operation.
      if(meta.getValue().hasAttribute("attributes"))
      {
        Set<AttributePath> scimAttributes =
                new LinkedHashSet<AttributePath>();
        SCIMAttribute attrToDelete = meta.getValue().getAttribute("attributes");
        for(SCIMAttributeValue attr : attrToDelete.getValues())
        {
          String rawAttributeName;
          if(attr.isComplex())
          {
            rawAttributeName = attr.getSubAttributeValue("value",
                    AttributeValueResolver.STRING_RESOLVER);
          }
          else
          {
            rawAttributeName = attr.getStringValue();
          }

          AttributePath path = AttributePath.parse(rawAttributeName,
                  getDefaultSchemaURI());
          scimAttributes.add(path);
        }

        ldapAttributeTypes.addAll(toLDAPAttributeTypes(scimAttributes));
      }
    }

    return ldapAttributeTypes;
  }



  /**
   * Map the replacement attributes in a SCIM object to LDAP modifications.
   *
   * @param currentEntry   The current LDAP entry representing the SCIM object.
   * @param scimObject     The object containing attributes to be mapped.
   * @param mappedAttributeNames The names of the modifiable attributes.
   * @param ldapInterface  An optional LDAP interface that can be used to
   *                       derive attributes from other entries.
   *
   * @return  A list of LDAP modifications mapped from the SCIM object. This
   *          should never be {@code null} but may be empty.
   *
   * @throws SCIMException If the modifications could not be mapped.
   */
  public List<Modification> toLDAPModificationsForPut(
      final Entry currentEntry,
      final SCIMObject scimObject,
      final String[] mappedAttributeNames,
      final LDAPRequestInterface ldapInterface)
          throws SCIMException
  {
    final List<Attribute> attributes =
        toLDAPAttributes(scimObject, ldapInterface);
    final Entry entry = new Entry(currentEntry.getDN(), attributes);

    return Entry.diff(currentEntry, entry, false, false, mappedAttributeNames);
  }



  /**
   * Map the replacement attributes in a SCIM object to LDAP modifications
   * according to the PATCH specification.
   *
   * @param currentEntry   The current LDAP entry representing the SCIM object.
   * @param scimObject     The object containing attributes to be mapped.
   * @param ldapInterface  An optional LDAP interface that can be used to
   *                       derive attributes from other entries.
   *
   * @return  A list of LDAP modifications mapped from the SCIM object. This
   *          should never be {@code null} but may be empty.
   *
   * @throws SCIMException If the modifications could not be mapped.
   */
  public List<Modification> toLDAPModificationsForPatch(
          final Entry currentEntry,
          final SCIMObject scimObject,
          final LDAPRequestInterface ldapInterface)
              throws SCIMException
  {
    SCIMAttribute meta = scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                                        CoreSchema.META_DESCRIPTOR.getName());

    HashMap<String, Modification> deletedAttrs =
        new HashMap<String, Modification>();
    LinkedList<Modification> mods = new LinkedList<Modification>();

    if (meta != null)
    {
      SCIMAttributeValue value = meta.getValue();
      SCIMAttribute attributesAttr = value.getAttribute("attributes");
      if (attributesAttr != null)
      {
        Set<AttributePath> scimAttributes =
                new LinkedHashSet<AttributePath>();
        for (SCIMAttributeValue val : attributesAttr.getValues())
        {
          String rawAttributeName;
          if (val.isComplex())
          {
            rawAttributeName = val.getSubAttributeValue("value",
                    AttributeValueResolver.STRING_RESOLVER);
          }
          else
          {
            rawAttributeName = val.getStringValue();
          }

          AttributePath path = AttributePath.parse(rawAttributeName,
              getDefaultSchemaURI());
          scimAttributes.add(path);
        }

        Set<String> ldapAttributes = toLDAPAttributeTypes(scimAttributes);

        if (Debug.debugEnabled())
        {
          Debug.debug(Level.FINE, DebugType.OTHER,
                  "LDAP attributes to remove are: " + ldapAttributes);
        }

        for (String attr : ldapAttributes)
        {
          deletedAttrs.put(attr.toLowerCase(),
              new Modification(ModificationType.DELETE, attr));
        }
      }
    }

    Set<String> schemas = scimObject.getSchemas();
    for (String schema : schemas)
    {
      Collection<SCIMAttribute> attributes = scimObject.getAttributes(schema);
      for (SCIMAttribute attr : attributes)
      {
        // Skip the meta attribute.
        if (schema.equals(SCIMConstants.SCHEMA_URI_CORE) &&
            attr.getName().equalsIgnoreCase(
                CoreSchema.META_DESCRIPTOR.getName()))
        {
          continue;
        }

        if(attr.getAttributeDescriptor().isMultiValued())
        {
          //The attr is multi-valued, so merge it into the current value
          for (SCIMAttributeValue value : attr.getValues())
          {
            if(value.isComplex())
            {
              String operation = value.getSubAttributeValue(
                      "operation", AttributeValueResolver.STRING_RESOLVER);
              if ("delete".equalsIgnoreCase(operation))
              {
                //delete this value from the set of values for this attribute
                SCIMObject tempObject = new SCIMObject();
                SCIMAttribute attrToDelete =
                     SCIMAttribute.create(attr.getAttributeDescriptor(), value);
                tempObject.setAttribute(attrToDelete);
                for(Attribute attribute :
                    toLDAPAttributes(tempObject, ldapInterface))
                {
                  String attrName = attribute.getName();
                  if(!deletedAttrs.containsKey(attrName.toLowerCase()))
                  {
                    // All values may be deleted already, in which case we
                    // don't need to add another value specific delete mod.
                    mods.add(new Modification(ModificationType.DELETE,
                        attrName, attribute.getRawValues()));
                  }
                }
                continue;
              }
            }

            //add this value to the set of values for this attribute
            SCIMObject tempObject = new SCIMObject();
            SCIMAttribute attrToAdd =
                    SCIMAttribute.create(attr.getAttributeDescriptor(), value);
            tempObject.setAttribute(attrToAdd);
            for(Attribute attribute :
                toLDAPAttributes(tempObject, ldapInterface))
            {
              String attrName = attribute.getName();
              if(deletedAttrs.remove(attrName.toLowerCase()) == null)
              {
                mods.add(new Modification(ModificationType.ADD,
                    attrName, attribute.getRawValues()));
              }
              else
              {
                // Replace the delete/add with a single replace mod.
                mods.add(new Modification(ModificationType.REPLACE,
                    attrName, attribute.getRawValues()));
              }
            }
          }
        }
        else
        {
          //The attr is single-valued, so just replace it on the newObject.
          SCIMObject tempObject = new SCIMObject();
          tempObject.setAttribute(attr);
          for(Attribute attribute :
              toLDAPAttributes(tempObject, ldapInterface))
          {
            String attrName = attribute.getName();
            deletedAttrs.remove(attrName.toLowerCase());
            mods.add(new Modification(ModificationType.REPLACE,
                attrName, attribute.getRawValues()));

          }
        }
      }
    }

    mods.addAll(0, deletedAttrs.values());
    return mods;
  }



  /**
   * Map the provided SCIM filter to an LDAP filter.
   *
   * @param filter  The SCIM filter to be mapped, or {@code null} if no filter
   *                parameters were provided.
   * @param ldapInterface  An optional LDAP interface that can be used to
   *                       map filters using derived attributes.
   * @return  An LDAP filter or {@code null} if the SCIM filter could not be
   *          mapped and will not match anything.
   * @throws SCIMException  If an error occurs during the mapping.
   */
  public Filter toLDAPFilter(final SCIMFilter filter,
                             final LDAPRequestInterface ldapInterface)
      throws SCIMException
  {
    if (filter == null)
    {
      return searchResolver.getFilter();
    }

    final Filter filterComponent = toLDAPFilterComponent(filter, ldapInterface);

    if (filterComponent == null)
    {
      return null;
    }
    else if(searchResolver.getFilter() != null)
    {
      return Filter.createANDFilter(filterComponent,
                                    searchResolver.getFilter());
    }
    else
    {
      return filterComponent;
    }
  }



  /**
   * Get the search base DNs to be used for querying.
   *
   * @return  The search base DN to be used for querying.
   */
  public Set<DN> getSearchBaseDNs()
  {
    return searchResolver.getBaseDNs();
  }



  /**
   * Determine whether the provided DN could be a resource entry that
   * satisfies the criteria for this ResourceMapper.
   *
   * @param dn  The DN for which to make the determination.
   *
   * @return  {@code true} if the LDAP entry satisfies the criteria for this
   *          resolver.
   */
  final boolean isDnInScope(final String dn)
  {
    return searchResolver.isDnInScope(dn);
  }



  /**
   * Map the provided SCIM sort parameters to an LDAP sort control.
   *
   * @param sortParameters  The SCIM sort parameters to be mapped.
   *
   * @return  An LDAP sort control.
   * @throws SCIMException  If the sort parameters could not be mapped.
   */
  public Control toLDAPSortControl(final SortParameters sortParameters)
      throws SCIMException
  {
    final AttributePath attributePath = sortParameters.getSortBy();
    final AttributeDescriptor attributeDescriptor =
        resourceDescriptor.getAttribute(attributePath.getAttributeSchema(),
          attributePath.getAttributeName());
    AttributeMapper attributeMapper = attributeMappers.get(attributeDescriptor);

    //TODO: handle if scimAttributeType == meta here

    if (attributeMapper == null)
    {
      throw new InvalidResourceException("Cannot sort by attribute " +
          sortParameters.getSortBy());
    }

    final ServerSideSortRequestControl c =
        attributeMapper.toLDAPSortControl(sortParameters);
    if (c == null)
    {
      throw new InvalidResourceException("Cannot sort by attribute " +
          sortParameters.getSortBy());
    }

    return new ServerSideSortRequestControl(true, c.getSortKeys());
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
   * @throws SCIMException   If the attributes could not be mapped.
   */
  public List<SCIMAttribute> toSCIMAttributes(
      final Entry entry,
      final SCIMQueryAttributes queryAttributes,
      final LDAPRequestInterface ldapInterface) throws SCIMException
  {
    return toSCIMAttributes(new SearchResultEntry(entry), queryAttributes,
                            ldapInterface);
  }



  /**
   * Map the attributes in an LDAP search result entry to SCIM attributes.
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
   * @throws SCIMException   If the attributes could not be mapped.
   */
  public List<SCIMAttribute> toSCIMAttributes(
      final SearchResultEntry entry,
      final SCIMQueryAttributes queryAttributes,
      final LDAPRequestInterface ldapInterface) throws SCIMException
  {
    final List<SCIMAttribute> attributes = new ArrayList<SCIMAttribute>();

    //Keep a list of the derived attributes that we add to the result set
    final Set<AttributeDescriptor> derivedAttrs =
          new HashSet<AttributeDescriptor>(derivedAttributes.size());

    if (ldapInterface != null)
    {
      for (final Map.Entry<AttributeDescriptor,DerivedAttribute> e :
          derivedAttributes.entrySet())
      {
        if (queryAttributes.isAttributeRequested(e.getKey()))
        {
          derivedAttrs.add(e.getKey());
          final DerivedAttribute derivedAttribute = e.getValue();
          final SCIMAttribute attribute =
              derivedAttribute.searchEntryToSCIMAttribute(
                  entry, ldapInterface, searchResolver);
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

    for (final AttributeMapper attributeMapper : attributeMappers.values())
    {
      if(derivedAttrs.contains(attributeMapper.getAttributeDescriptor()))
      {
        //If this attribute has a derivation, then it was already added above.
        continue;
      }

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
   * @throws SCIMException   If the entry could not be mapped.
   */
  public SCIMObject toSCIMObject(final Entry entry,
                                 final SCIMQueryAttributes queryAttributes,
                                 final LDAPRequestInterface ldapInterface)
      throws SCIMException
  {
    return toSCIMObject(new SearchResultEntry(entry),
                        queryAttributes, ldapInterface);
  }



  /**
   * Map an LDAP search result entry to a SCIM resource.
   *
   * @param entry            The LDAP entry containing attributes to be
   *                         mapped, and optional controls as input to the
   *                         mapping.
   * @param queryAttributes  The set of SCIM attributes that are requested
   *                         to be returned.
   * @param ldapInterface    An optional LDAP interface that can be used to
   *                         derive attributes from other entries.
   *
   * @return  A SCIM object mapped from the LDAP entry, or {@code null} if this
   *          entry cannot be mapped to a SCIM object.
   * @throws SCIMException   If the entry could not be mapped.
   */
  public SCIMObject toSCIMObject(
      final SearchResultEntry entry,
      final SCIMQueryAttributes queryAttributes,
      final LDAPRequestInterface ldapInterface)
      throws SCIMException
  {
    if (searchResolver.getFilter() != null)
    {
      try
      {
        if (!searchResolver.getFilter().matchesEntry(entry))
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
      Validator.ensureTrue(scimObject.addAttribute(a));
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
   * @param ldapInterface  An optional LDAP interface that can be used to
   *                       map filters using derive attributes.
   * @return  The LDAP filter component, or {@code null} if the filter
   *          component could not be mapped and will not match anything.
   * @throws SCIMException  If an error occurs during the mapping.
   */
  protected Filter toLDAPFilterComponent(final SCIMFilter filter,
                                       final LDAPRequestInterface ldapInterface)
      throws SCIMException {
    final SCIMFilterType filterType = filter.getFilterType();

    switch (filterType)
    {
      case AND:
        final List<Filter> andFilterComponents = new ArrayList<Filter>();
        for (SCIMFilter f : filter.getFilterComponents())
        {
          final Filter filterComponent =
              toLDAPFilterComponent(f, ldapInterface);
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
          final Filter filterComponent =
              toLDAPFilterComponent(f, ldapInterface);
          if (filterComponent != null)
          {
            orFilterComponents.add(filterComponent);
          }
        }
        return Filter.createORFilter(orFilterComponents);

      default:
        final AttributePath filterAttribute = filter.getFilterAttribute();
        final AttributeDescriptor attributeDescriptor =
            resourceDescriptor.getAttribute(
                filterAttribute.getAttributeSchema(),
                filterAttribute.getAttributeName());

        AttributeMapper attributeMapper;

        if (attributeDescriptor.equals(
            metaAttributeMapper.getAttributeDescriptor()))
        {
          attributeMapper = metaAttributeMapper;
        }
        else if (idAttributeMapper != null &&
                 attributeDescriptor.equals(
                     idAttributeMapper.getAttributeDescriptor()))
        {
          attributeMapper = idAttributeMapper;
        }
        else
        {
          attributeMapper = attributeMappers.get(attributeDescriptor);
        }

        DerivedAttribute derivedAttribute =
            derivedAttributes.get(attributeDescriptor);

        final List<Filter> filters = new ArrayList<Filter>(2);
        if (attributeMapper != null)
        {
          filters.add(attributeMapper.toLDAPFilter(filter));
        }
        if(derivedAttribute != null && ldapInterface != null)
        {
          filters.add(derivedAttribute.toLDAPFilter(filter, ldapInterface,
              searchResolver));
        }

        if (filters.size() == 2)
        {
          return Filter.createORFilter(filters);
        }
        if (!filters.isEmpty())
        {
          return filters.get(0);
        }
        return null;
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
        Transformation.create(GeneralizedTimeTransformation.class.getName(),
            null);

    transformations.add(new SubAttributeTransformation("created",
           new AttributeTransformation("createTimestamp", dateTransformation)));

    transformations.add(new SubAttributeTransformation("lastModified",
           new AttributeTransformation("modifyTimestamp", dateTransformation)));

    return new ComplexSingularAttributeMapper(
                    CoreSchema.META_DESCRIPTOR, transformations);
  }



  /**
   * Determines whether the SCIM resource ID maps to the LDAP DN.
   *
   * @return  {@code true} if the SCIM resource ID maps to the LDAP DN or
   *          {@code false} if the ID maps to some other attribute.
   */
  public boolean idMapsToDn()
  {
    return searchResolver.idMapsToDn();
  }



  /**
   * Returns the LDAP attribute that the SCIM resource ID maps to, or
   * {@code null} if the SCIM ID maps to the LDAP DN.
   *
   * @return  The LDAP attribute that the SCIM resource ID maps to, or
   *          {@code null} if the SCIM ID maps to the LDAP DN.
   */
  public String getIdAttribute()
  {
    return searchResolver.getIdAttribute();
  }



  /**
   * Retrieve a resource ID from an LDAP entry.
   *
   * @param entry  The LDAP entry, which must contain a value for the
   *               resource ID attribute unless the resource ID maps to the
   *               LDAP DN.
   *
   * @return  The resource ID of the entry.
   *
   * @throws SCIMException  If the resource ID could not be determined.
   */
  public String getIdFromEntry(final Entry entry)
      throws SCIMException
  {
    return searchResolver.getIdFromEntry(entry);
  }



  /**
   * Construct the the entry DN from the entry attributes based on the
   * LDAP Add parameters.
   *
   * @param entry  The LDAP entry to construct the DN from.
   *
   * @return  The constructed DN for the entry or {@code null} if the LDAP
   *          ADD parameters are not defined.
   *
   * @throws SCIMException  If the resource ID could not be determined.
   */
  public String constructEntryDN(final Entry entry) throws SCIMException
  {
    if(dnConstructor != null)
    {
      try
      {
      return dnConstructor.constructValue(entry);
      }
      catch (Exception e)
      {
        throw new InvalidResourceException("An error occurred while " +
            "constructing the DN for a mapped entry: " +
            e.getLocalizedMessage(), e);
      }
    }
    throw new InvalidResourceException("A DNTemplate must be defined in the " +
        "LDAPAdd section of the resource mapping configuration to map User " +
        "resources to LDAP entry DNs.");
  }



  /**
   * Read the LDAP entry identified by the given resource ID. No attributes
   * are returned from the entry.
   *
   * @param ldapInterface  The LDAP interface to use to read the entry.
   * @param resourceID     The requested SCIM resource ID.
   *
   * @return  The LDAP entry for the given resource ID.
   *
   * @throws SCIMException  If the entry could not be read.
   */
  public SearchResultEntry getEntryWithoutAttrs(
      final LDAPRequestInterface ldapInterface,
      final String resourceID)
      throws SCIMException
  {
    return getEntry(ldapInterface, resourceID, "1.1");
  }



  /**
   * Read the LDAP entry identified by the given resource ID.
   *
   * @param ldapInterface  The LDAP interface to use to read the entry.
   * @param resourceID     The requested SCIM resource ID.
   * @param attributes     The requested LDAP attributes.
   *
   * @return  The LDAP entry for the given resource ID.
   *
   * @throws SCIMException  If the entry could not be read.
   */
  public SearchResultEntry getEntry(final LDAPRequestInterface ldapInterface,
                                    final String resourceID,
                                    final String... attributes)
      throws SCIMException
  {
    final List<Control> controls = new ArrayList<Control>();

    return searchResolver.getEntry(ldapInterface, resourceID, controls,
                                   attributes);
  }



  /**
   * Read the LDAP entry identified by the given resource ID. The LDAP entry
   * should be in a form suitable for mapping to a SCIM entry that will be
   * returned to the client.
   *
   * @param ldapInterface    The LDAP interface to use to read the entry.
   * @param resourceID       The requested SCIM resource ID.
   * @param queryAttributes  The set of requested SCIM attributes.
   * @param attributes       The requested LDAP attributes.
   *
   * @return  The LDAP entry for the given resource ID.
   *
   * @throws SCIMException  If the entry could not be read.
   */
  public SearchResultEntry getReturnEntry(
      final LDAPRequestInterface ldapInterface,
      final String resourceID,
      final SCIMQueryAttributes queryAttributes,
      final String... attributes)
      throws SCIMException
  {
    // Include any controls that are needed by derived attributes.
    final List<Control> controls = new ArrayList<Control>();
    addSearchControls(controls, queryAttributes);

    return searchResolver.getEntry(ldapInterface, resourceID, controls,
                                   attributes);
  }



  /**
   * Add any search controls that are needed by derived attributes when the
   * LDAP entry is fetched.
   *
   * @param controls         The list of controls that will be used to fetch
   *                         the LDAP entry.
   * @param queryAttributes  The set of requested SCIM attributes.
   */
  public void addSearchControls(final List<Control> controls,
                                final SCIMQueryAttributes queryAttributes)
  {
    for (final Map.Entry<AttributeDescriptor,DerivedAttribute> e :
        derivedAttributes.entrySet())
    {
      if (queryAttributes.isAttributeRequested(e.getKey()))
      {
        final DerivedAttribute derivedAttribute = e.getValue();
        derivedAttribute.addSearchControls(controls);
      }
    }
  }



  /**
   * Returns the LDAP attribute that the SCIM password attribute maps to,
   * or <code>null</code> if there is no mapping for the password attribute.
   *
   * @return The LDAP attribute name, or <code>null</code> if there is no
   *  mapping for the password attribute.
   */
  public String getPasswordAttribute()
  {
    if(passwordAttributeMapper != null)
    {
      Iterator<String> iter = passwordAttributeMapper
                                  .getLDAPAttributeTypes().iterator();
      if(iter.hasNext())
      {
        return iter.next();
      }
    }
    return null;
  }



  /**
   * Translate an LDAP exception to a SCIM exception.
   *
   * @param e  The LDAP exception to be translated.
   *
   * @return  The SCIM exception.
   */
  public static SCIMException toSCIMException(final LDAPException e)
  {
    return toSCIMException(
            com.unboundid.util.StaticUtils.getExceptionMessage(e), e);
  }



  /**
   * Translate an LDAP exception to a SCIM exception.
   *
   * @param errorMessage  The error message to use in the SCIM exception.
   * @param e             The LDAP exception to be translated.
   *
   * @return  The SCIM exception.
   */
  public static SCIMException toSCIMException(final String errorMessage,
                                              final LDAPException e)
  {
    switch (e.getResultCode().intValue())
    {
      case ResultCode.NO_SUCH_ATTRIBUTE_INT_VALUE:
      case ResultCode.INVALID_ATTRIBUTE_SYNTAX_INT_VALUE:
      case ResultCode.INVALID_DN_SYNTAX_INT_VALUE:
      case ResultCode.UNWILLING_TO_PERFORM_INT_VALUE:
      case ResultCode.OBJECT_CLASS_VIOLATION_INT_VALUE:
      case ResultCode.NOT_ALLOWED_ON_NONLEAF_INT_VALUE:
      case ResultCode.NOT_ALLOWED_ON_RDN_INT_VALUE:
      case ResultCode.OBJECT_CLASS_MODS_PROHIBITED_INT_VALUE:
        return new InvalidResourceException(errorMessage, e);

      case ResultCode.INVALID_CREDENTIALS_INT_VALUE:
        return new UnauthorizedException(errorMessage, e);

      case ResultCode.INSUFFICIENT_ACCESS_RIGHTS_INT_VALUE:
        return new ForbiddenException(errorMessage, e);

      case ResultCode.NO_SUCH_OBJECT_INT_VALUE:
        return new ResourceNotFoundException(errorMessage, e);

      case ResultCode.CONSTRAINT_VIOLATION_INT_VALUE:
      case ResultCode.ATTRIBUTE_OR_VALUE_EXISTS_INT_VALUE:
      case ResultCode.ENTRY_ALREADY_EXISTS_INT_VALUE:
        return new ResourceConflictException(errorMessage, e);
      case ResultCode.ASSERTION_FAILED_INT_VALUE:
        return new PreconditionFailedException(errorMessage, e);

      default:
        return new ServerErrorException(errorMessage, e);
    }
  }
}
