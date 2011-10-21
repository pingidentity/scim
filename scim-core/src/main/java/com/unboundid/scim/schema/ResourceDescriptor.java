/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.schema;

import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * This class provides methods that describe the schema for a SCIM resource. It
 * may be used to help read and write SCIM objects in their external XML and
 * JSON representation, and to convert SCIM objects to and from LDAP entries.
 */
public class ResourceDescriptor extends BaseResource
{
  /**
   * A <code>ResourceFactory</code> for creating <code>ResourceDescriptor</code>
   * instances.
   */
  public static final ResourceFactory<ResourceDescriptor>
      RESOURCE_DESCRIPTOR_FACTORY = new ResourceFactory<ResourceDescriptor>() {
    /**
     * {@inheritDoc}
     */
    public ResourceDescriptor createResource(
        final ResourceDescriptor resourceDescriptor,
        final SCIMObject scimObject) {
      return new ResourceDescriptor(resourceDescriptor, scimObject);
    }
  };

  /**
   * A schema -> name -> AttributeDescriptor map to quickly look up
   * attributes.
   */
  private Map<String, Map<String, AttributeDescriptor>> attributesCache;

  /**
   * Constructs a new ResourceDescriptor from a existing SCIMObject.
   *
   * @param resourceDescriptor The Resource Schema descriptor.
   * @param scimObject The SCIMObject containing the schema.
   */
  ResourceDescriptor(final ResourceDescriptor resourceDescriptor,
                     final SCIMObject scimObject) {
    super(resourceDescriptor, scimObject);
  }

  /**
   * Constructs a new empty ResourceDescriptor.
   *
   * @param resourceDescriptor The Resource Schema descriptor.
   */
  private ResourceDescriptor(final ResourceDescriptor resourceDescriptor) {
    super(resourceDescriptor);
  }

  /**
   * Retrieves the attribute descriptor for a specified attribute.
   *
   * @param schema The attribute descriptor's associated schema URN.
   * @param name The name of the attribute whose descriptor is to be retrieved.
   *
   * @return The attribute descriptor for the specified attribute.
   * @throws InvalidResourceException if there is no such attribute.
   */
  public AttributeDescriptor getAttribute(final String schema,
                                          final String name)
      throws InvalidResourceException
  {
    // TODO: Should we implement a strict and non strict mode?
    initAttributesCache();
    AttributeDescriptor attributeDescriptor = null;
    Map<String, AttributeDescriptor> map = attributesCache.get(schema);
    if(map != null)
    {
      attributeDescriptor = map.get(name);
    }
    if(attributeDescriptor == null)
    {
      throw new InvalidResourceException("Attribute " + schema +
          SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE + name +
          " is not defined for resource " + getName());
    }
    return attributeDescriptor;
  }

  /**
   * Retrieves all the attribute descriptors of the provided schema defined
   * in the resource.
   *
   * @param schema The name of the schema.
   * @return All the attribute descriptors of the provided schema defined
   * for this resource.
   */
  public Collection<AttributeDescriptor> getAttributes(final String schema)
  {
    initAttributesCache();
    Map<String, AttributeDescriptor> map = attributesCache.get(schema);
    if(map != null)
    {
      return map.values();
    }
    return null;
  }

  /**
   * Retrieves the set of unique schemas for the attribute descriptors defined
   * in the resource.
   *
   * @return The set of unique schemas for the attribute descriptors defined
   * in the resource.
   */
  public Set<String> getAttributeSchemas()
  {
    initAttributesCache();
    return attributesCache.keySet();
  }

  /**
   * Retrieve the name of the resource to be used in any external representation
   * of the resource.
   *
   * @return Retrieve the name of the resource to be used in any external
   *         representation of the resource. It is never {@code null}.
   */
  public String getName()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "name",
        AttributeValueResolver.STRING_RESOLVER);
  }



  /**
   * Sets the name of the resource to be used in any external representation
   * of the resource.
   *
   * @param name The name of the resource to be used in any external
   *             representation of the resource.
   * @return this ResourceDescriptor.
   */
  private ResourceDescriptor setName(final String name)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "name", AttributeValueResolver.STRING_RESOLVER, name);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Retrieves the list of all attribute descriptors defined in the resource.
   *
   * @return The list of attribute descriptors for the resource. It is never
   *         {@code null}.
   */
  public Collection<AttributeDescriptor> getAttributes()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "attributes", AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER);
  }



  /**
   * Sets the list of attribute descriptors for the resource.
   *
   * @param attributes The list of attribute descriptors for the resource.
   * @return this ResourceDescriptor.
   */
  private ResourceDescriptor setAttributes(
      final Collection<AttributeDescriptor> attributes)
  {
    try {
      setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "attributes", AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER,
          attributes);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Returns the resource's XML schema (namespace) name.
   *
   * @return The XML namespace name.
   */
  public String getSchema()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "schema",
        AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the resource's XML schema (namespace) name.
   *
   * @param schema The XML namespace name.
   * @return this ResourceDescriptor.
   */
  private ResourceDescriptor setSchema(final String schema)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "schema", AttributeValueResolver.STRING_RESOLVER, schema);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Retrieves the resource's human readable description.
   *
   * @return The resource's human readable description.
   */
  public String getDescription()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "description", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the resource's human readable description.
   *
   * @param description The resource's human readable description.
   * @return this ResourceDescriptor.
   */
  private ResourceDescriptor setDescription(final String description)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "description", AttributeValueResolver.STRING_RESOLVER, description);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Retrieves the Resource's HTTP addressable endpoint relative to the
   * Base URL.
   *
   * @return The Resource's HTTP addressable endpoint relative to the Base URL.
   */
  public String getQueryEndpoint()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "queryEndpoint", AttributeValueResolver.STRING_RESOLVER);
  }


  /**
   * Sets the Resource's HTTP addressable endpoint relative to the
   * Base URL.
   *
   * @param queryEndpoint The Resource's HTTP addressable endpoint relative to
   *                      the Base URL.
   * @return this ResourceDescriptor.
   */
  private ResourceDescriptor setQueryEndpoint(final String queryEndpoint)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "queryEndpoint", AttributeValueResolver.STRING_RESOLVER,
          queryEndpoint);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }

  /**
   * Initializes the attributesCache if needed.
   */
  private void initAttributesCache()
  {
    synchronized(this)
    {
      if(attributesCache == null)
      {
        attributesCache = new HashMap<String,
            Map<String, AttributeDescriptor>>();
        for(AttributeDescriptor attributeDescriptor : getAttributes())
        {
          Map<String, AttributeDescriptor> map =
              attributesCache.get(attributeDescriptor.getSchema());
          if(map == null)
          {
            map = new HashMap<String, AttributeDescriptor>();
            attributesCache.put(attributeDescriptor.getSchema(), map);
          }
          map.put(attributeDescriptor.getName(), attributeDescriptor);
        }
      }
    }
  }

  @Override
  public String toString()
  {
    return "ResourceDescriptor{" +
        "name='" + getName() + '\'' +
        ", description='" + getDescription() +
        ", schema='" + getSchema() + '\'' +
        ", queryEndpoint='" + getQueryEndpoint() + '\'' +
        ", attributes=" + getAttributes() +
        '}';
  }

  /**
   * Construct a new resource descriptor with the provided information.
   * The resource attributes specified here should not include common core
   * attributes (ie. id, externalId, meta) as these will be added automatically.
   *
   * @param name The addressable Resource endpoint name.
   * @param description The Resource's human readable description.
   * @param schema The Resource's associated schema URN
   * @param queryEndpoint The Resource's HTTP addressable endpoint relative
   *                      to the Base URL.
   * @param attributes Specifies the set of associated Resource attributes.
   * @return The newly constructed resource descriptor.
   */
  public static ResourceDescriptor create(
      final String name, final String description, final String schema,
      final String queryEndpoint, final AttributeDescriptor... attributes)
  {
    ResourceDescriptor resourceDescriptor =
      new ResourceDescriptor(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR);
    resourceDescriptor.setName(name);
    resourceDescriptor.setDescription(description);
    resourceDescriptor.setSchema(schema);
    resourceDescriptor.setQueryEndpoint(queryEndpoint);
    resourceDescriptor.setAttributes(
        CoreSchema.addCommonResourceAttributes(attributes));

    return resourceDescriptor;
  }
}
