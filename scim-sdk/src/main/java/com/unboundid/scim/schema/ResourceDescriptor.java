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

package com.unboundid.scim.schema;

import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.unboundid.scim.sdk.StaticUtils.toLowerCase;



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
      ResourceDescriptor rd =
              new ResourceDescriptor(resourceDescriptor, scimObject);

      if (scimObject.getSchemas().contains(
              "urn:unboundid:schemas:scim:ldap:1.0"))
      {
        //This is a convenience for when we're talking to the UnboundID
        //Directory REST API; clients could set this themselves, but we'll do
        //it for them in this case.
        rd.setStrictMode(false);
      }
      return rd;
    }
  };

  /**
   * A schema -> name -> AttributeDescriptor map to quickly look up
   * attributes. The attribute descriptors are keyed by the lower case
   * attribute name because attribute names are case-insensitive. Likewise,
   * the schema key is lower case because schema URNs are case-insensitive.
   */
  private Map<String, Map<String, AttributeDescriptor>> attributesCache;

  /**
   * Whether to use "strict mode" when looking up an attribute
   * that doesn't exist in the attributesCache.
   */
  private boolean strictMode = true;

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
    initAttributesCache();
    AttributeDescriptor attributeDescriptor = null;
    Map<String, AttributeDescriptor> map =
        attributesCache.get(toLowerCase(schema));
    if(map != null)
    {
      attributeDescriptor = map.get(toLowerCase(name));
    }
    if(attributeDescriptor == null)
    {
      if (strictMode || SCIMConstants.SCHEMA_URI_CORE.equalsIgnoreCase(schema))
      {
        throw new InvalidResourceException("Attribute " + schema +
            SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE + name +
            " is not defined for resource " + getName());
      }
      else
      {
        attributeDescriptor = AttributeDescriptor.createMultiValuedAttribute(
            name, "value", null, schema, false, false, false,
            CoreSchema.createMultiValuedValueDescriptor(
                schema, AttributeDescriptor.DataType.STRING));
      }
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
    Map<String, AttributeDescriptor> map =
        attributesCache.get(toLowerCase(schema));
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
   * Retrieves the schema for a specified attribute name.
   *
   * @param name The name of the attribute whose schema is to be found.
   * @param preferredSchemas an ordered list of schemas to prefer when matching
   *
   * @return The schema for the specified attribute or {@code null} if there is
   *         no such attribute name in any schema.
   */
  public String findAttributeSchema(final String name,
                                    final String... preferredSchemas)
  {
    Set<String> matchingSchemas = new HashSet<String>();
    for (String schema : getAttributeSchemas())
    {
      Map<String, AttributeDescriptor> map =
              attributesCache.get(toLowerCase(schema));
      if (map!= null && map.keySet().contains(name))
      {
        matchingSchemas.add(schema);
      }
    }
    if (matchingSchemas.isEmpty())
    {
      // Element does not belong to any known schema
      return null;
    }
    List<String> preferredSchemaList;
    if (preferredSchemas == null || preferredSchemas.length < 1)
    {
      preferredSchemaList = new ArrayList<String>(2);
      preferredSchemaList.add(getSchema());
      preferredSchemaList.add(SCIMConstants.SCHEMA_URI_CORE);
    }
    else
    {
      preferredSchemaList = Arrays.asList(preferredSchemas);
    }
    for (String preferredSchema : preferredSchemaList)
    {
      if (matchingSchemas.contains(preferredSchema))
      {
        return preferredSchema;
      }
    }
    // If no preferred schema was found then just return the first from the list
    return matchingSchemas.iterator().next();
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
   * Sets the "strict mode" for this ResourceDescriptor. If strict mode is off,
   * then a call to {@link #getAttribute(String, String)} where the requested
   * attribute does not exist in the attributesCache will result in the method
   * generating an AttributeDescriptor on the fly. If strict mode were on in
   * this case, it would throw an exception because that attribute was not
   * defined.
   *
   * @param strictMode a boolean indicating whether to use strict mode or not.
   */
  public void setStrictMode(final boolean strictMode)
  {
    this.strictMode = strictMode;
  }

  /**
   * Gets the "strict mode" setting for this ResourceDescriptor. If strict mode
   * is off, then a call to {@link #getAttribute(String, String)} where the
   * requested attribute does not exist in the attributesCache will result in
   * the method generating an AttributeDescriptor on the fly. If strict mode
   * were on in this case, it would throw an exception because that attribute
   * was not defined.
   *
   * @return boolean indicating whether strict mode is enabled.
   */
  public boolean isStrictMode()
  {
    return this.strictMode;
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
    Collection<AttributeDescriptor> attributes =
        getAttributeValues(SCIMConstants.SCHEMA_URI_CORE,
            "attributes", AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER);
    return CoreSchema.addCommonAttributes(attributes);
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
      setAttributeValues(SCIMConstants.SCHEMA_URI_CORE,
          "attributes", AttributeDescriptor.ATTRIBUTE_DESCRIPTOR_RESOLVER,
          attributes);
      synchronized (this) {
        attributesCache = null;
      }
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
    return toLowerCase(
            getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "schema",
            AttributeValueResolver.STRING_RESOLVER));
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
  public String getEndpoint()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "endpoint", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the Resource's HTTP addressable endpoint relative to the
   * Base URL.
   *
   * @param endpoint The Resource's HTTP addressable endpoint relative to
   *                 the Base URL.
   * @return this ResourceDescriptor.
   */
  private ResourceDescriptor setEndpoint(final String endpoint)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "endpoint", AttributeValueResolver.STRING_RESOLVER, endpoint);
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
          final String lowerCaseSchema =
              toLowerCase(attributeDescriptor.getSchema());
          Map<String, AttributeDescriptor> map =
              attributesCache.get(lowerCaseSchema);
          if(map == null)
          {
            map = new HashMap<String, AttributeDescriptor>();
            attributesCache.put(lowerCaseSchema, map);
          }
          map.put(toLowerCase(attributeDescriptor.getName()),
                  attributeDescriptor);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode()
  {
    int hashCode = 31;
    hashCode += hashCode * toLowerCase(getSchema()).hashCode();
    hashCode += hashCode * toLowerCase(getName()).hashCode();
    return hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object obj)
  {
    if (this == obj)
    {
      return true;
    }

    if (!(obj instanceof ResourceDescriptor))
    {
      return false;
    }

    final ResourceDescriptor that = (ResourceDescriptor)obj;
    final String thisSchema = getSchema();
    final String thisName = getName();
    final String thatSchema = that.getSchema();
    final String thatName = that.getName();
    if (thisSchema == null && thatSchema == null)
    {
      return thisName.equalsIgnoreCase(thatName);
    }
    else
    {
      return thisSchema != null && thatSchema != null &&
          thisSchema.equalsIgnoreCase(thatSchema) &&
          thisName.equalsIgnoreCase(thatName);
    }
  }

  @Override
  public String toString()
  {
    return "ResourceDescriptor{" +
        "name='" + getName() + '\'' +
        ", description='" + getDescription() +
        ", schema='" + getSchema() + '\'' +
        ", endpoint='" + getEndpoint() + '\'' +
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
   * @param endpoint The Resource's HTTP addressable endpoint relative
   *                 to the Base URL.
   * @param attributes Specifies the set of associated Resource attributes.
   * @return The newly constructed resource descriptor.
   */
  public static ResourceDescriptor create(
      final String name, final String description, final String schema,
      final String endpoint, final AttributeDescriptor... attributes)
  {
    ResourceDescriptor resourceDescriptor =
      new ResourceDescriptor(CoreSchema.RESOURCE_SCHEMA_DESCRIPTOR);
    resourceDescriptor.setName(name);
    resourceDescriptor.setDescription(description);
    resourceDescriptor.setSchema(schema);
    resourceDescriptor.setEndpoint(endpoint);
    resourceDescriptor.setAttributes(Arrays.asList(attributes));

    return resourceDescriptor;
  }
}
