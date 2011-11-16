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

package com.unboundid.scim.data;

import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMResponse;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * This class represents a SCIM resource. It could also be sub-typed for
 * specific resource types (ie. Users or Groups) that provide convenience
 * methods for accessing specific attribute values.
 */
public class BaseResource implements SCIMResponse
{
  /**
   * A <code>ResourceFactory</code> for creating <code>BaseResource</code>
   * instances.
   */
  public static final ResourceFactory<BaseResource> BASE_RESOURCE_FACTORY =
      new ResourceFactory<BaseResource>() {
        /**
         * {@inheritDoc}
         */
        public BaseResource createResource(
            final ResourceDescriptor resourceDescriptor,
            final SCIMObject scimObject) {
          return new BaseResource(resourceDescriptor, scimObject);
        }
      };

  private final ResourceDescriptor resourceDescriptor;
  private final SCIMObject scimObject;

  /**
   * Construct a <code>BaseResource</code> with the specified
   * <code>ResourceDescriptor</code> and backed by the given
   * <code>SCIMObject</code>.
   *
   * @param resourceDescriptor The resource descriptor for this SCIM resource.
   * @param scimObject         The <code>SCIMObject</code> containing all the
   *                           SCIM attributes and their values.
   */
  public BaseResource(final ResourceDescriptor resourceDescriptor,
                      final SCIMObject scimObject)
  {
    this.resourceDescriptor = resourceDescriptor;
    this.scimObject = scimObject;
  }

  /**
   * Construct an empty <code>BaseResource</code> with the specified
   * <code>ResourceDescriptor</code>.
   *
   * @param resourceDescriptor The resource descriptor for this SCIM resource.
   */
  public BaseResource(final ResourceDescriptor resourceDescriptor)
  {
    this.resourceDescriptor = resourceDescriptor;
    this.scimObject = new SCIMObject();
  }

  /**
   * Retrieves the <code>ResourceDescriptor</code> for this resource.
   *
   * @return The <code>ResourceDescriptor</code> for this resource.
   */
  public ResourceDescriptor getResourceDescriptor() {
    return resourceDescriptor;
  }

  /**
   * Retrieves the <code>SCIMObject</code> wrapped by this resource.
   *
   * @return The <code>SCIMObject</code> wrapped by this resource.
   */
  public SCIMObject getScimObject() {
    return scimObject;
  }

  /**
   * Retrieves the unique identifier for the SCIM Resource as defined by
   * the Service Provider.
   *
   * @return The unique identifier for the SCIM Resource as defined by
   * the Service Provider.
   */
  public String getId()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "id",
        AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the unique identifier for the SCIM Resource.
   *
   * @param id The unique identifier for the SCIM Resource.
   */
  public void setId(final String id)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "id",
          AttributeValueResolver.STRING_RESOLVER, id);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves the unique identifier for the Resource as defined by the
   * Service Consumer.
   *
   * @return The unique identifier for the Resource as defined by the Service
   * Consumer.
   */
  public String getExternalId()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "externalId", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the unique identifier for the Resource as defined by the Service
   * Consumer.
   *
   * @param externalId The unique identifier for the Resource as defined by the
   * Service Consumer.
   */
  public void setExternalId(final String externalId)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "externalId",
          AttributeValueResolver.STRING_RESOLVER, externalId);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
  }


  /**
   * Retrieves the metadata about the resource.
   *
   * @return The metadata about the resource.
   */
  public Meta getMeta()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "meta",
        Meta.META_RESOLVER);
  }

  /**
   * Sets the metadata about the resource.
   * @param meta The metadata about the resource.
   */
  public void setMeta(final Meta meta)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE, "meta",
          Meta.META_RESOLVER, meta);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves a singular attribute value.
   *
   * @param <T>    The type of the resolved instance representing the value of
   *               sub-attribute.
   * @param schema The schema URI of the attribute value to retrieve.
   * @param name The name of the attribute value to retrieve.
   * @param resolver The <code>AttributeValueResolver</code> the should be used
   *                 to resolve the value to an instance.
   * @return The resolved value instance or <code>null</code> if the specified
   *         attribute does not exist.
   */
  public <T> T getSingularAttributeValue(
      final String schema, final String name,
      final AttributeValueResolver<T> resolver)
  {
    SCIMAttribute attribute = scimObject.getAttribute(schema, name);
    if(attribute != null)
    {
      SCIMAttributeValue value = attribute.getSingularValue();
      if(value != null)
      {
        return resolver.toInstance(value);
      }
    }
    return null;
  }

  /**
   * Sets a singular attribute value.
   *
   * @param <T>    The type of the resolved instance representing the value of
   *               sub-attribute.
   * @param schema The schema URI of the attribute value to retrieve.
   * @param name The name of the attribute value to retrieve.
   * @param resolver The <code>AttributeValueResolver</code> the should be used
   *                 to resolve the instance to attribute value.
   * @param value The value instance.
   * @throws InvalidResourceException if the attribute is not defined by the
   *                                  resource.
   */
  public <T> void setSingularAttributeValue(
      final String schema, final String name,
      final AttributeValueResolver<T> resolver, final T value)
      throws InvalidResourceException
  {
    if(value == null)
    {
      scimObject.removeAttribute(schema, name);
      return;
    }

    AttributeDescriptor attributeDescriptor =
        getResourceDescriptor().getAttribute(schema, name);

    scimObject.setAttribute(SCIMAttribute.createSingularAttribute(
        attributeDescriptor, resolver.fromInstance(attributeDescriptor,
        value)));
  }

  /**
   * Retrieves a plural attribute value.
   *
   * @param <T>    The type of the resolved instance representing the value of
   *               sub-attribute.
   * @param schema The schema URI of the attribute value to retrieve.
   * @param name The name of the attribute value to retrieve.
   * @param resolver The <code>AttributeValueResolver</code> the should be used
   *                 to resolve the value to an instance.
   * @return The collection of resolved value instances or <code>null</code> if
   *         the specified attribute does not exist.
   */
  public <T> Collection<T> getPluralAttributeValue(
      final String schema, final String name,
      final AttributeValueResolver<T> resolver)
  {
    SCIMAttribute attribute = scimObject.getAttribute(schema, name);
    if(attribute != null)
    {
      SCIMAttributeValue[] values = attribute.getPluralValues();
      if(values != null)
      {
        Collection<T> entries = new ArrayList<T>(values.length);
        for(SCIMAttributeValue v : values)
        {
          entries.add(resolver.toInstance(v));
        }
        return entries;
      }
    }
    return null;
  }

  /**
   * Sets a plural attribute value.
   *
   * @param <T>    The type of the resolved instance representing the value of
   *               sub-attribute.
   * @param schema The schema URI of the attribute value to retrieve.
   * @param name The name of the attribute value to retrieve.
   * @param resolver The <code>AttributeValueResolver</code> the should be used
   *                 to resolve the instance to attribute value.
   * @param values The value instances.
   * @throws InvalidResourceException if the attribute is not defined by the
   *                                  resource.
   */
  public <T> void setPluralAttributeValue(
      final String schema, final String name,
      final AttributeValueResolver<T> resolver, final Collection<T> values)
      throws InvalidResourceException
  {
    if(values == null)
    {
      scimObject.removeAttribute(schema, name);
      return;
    }

    AttributeDescriptor attributeDescriptor =
        getResourceDescriptor().getAttribute(schema, name);

    SCIMAttributeValue[] entries = new SCIMAttributeValue[values.size()];

    int i = 0;
    for(T value : values)
    {
      entries[i++] = resolver.fromInstance(attributeDescriptor, value);
    }

    scimObject.setAttribute(
        SCIMAttribute.createPluralAttribute(attributeDescriptor, entries));
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final Marshaller marshaller,
                      final OutputStream outputStream)
      throws Exception {
    marshaller.marshal(this, outputStream);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseResource)) {
      return false;
    }

    BaseResource that = (BaseResource) o;

    if (!resourceDescriptor.equals(that.resourceDescriptor)) {
      return false;
    }
    if (!scimObject.equals(that.scimObject)) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = resourceDescriptor.hashCode();
    result = 31 * result + scimObject.hashCode();
    return result;
  }
}
