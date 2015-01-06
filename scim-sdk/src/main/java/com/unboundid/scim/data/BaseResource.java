/*
 * Copyright 2011-2015 UnboundID Corp.
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
import com.unboundid.scim.sdk.ComplexValue;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMResponse;
import com.unboundid.scim.sdk.SimpleValue;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

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
      SCIMAttributeValue value = attribute.getValue();
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

    scimObject.setAttribute(SCIMAttribute.create(
        attributeDescriptor, resolver.fromInstance(attributeDescriptor,
        value)));
  }

  /**
   * Retrieves a multi-valued attribute value.
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
  public <T> Collection<T> getAttributeValues(
      final String schema, final String name,
      final AttributeValueResolver<T> resolver)
  {
    SCIMAttribute attribute = scimObject.getAttribute(schema, name);
    if(attribute != null)
    {
      SCIMAttributeValue[] values = attribute.getValues();
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
   * Sets a multi-valued attribute value.
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
  public <T> void setAttributeValues(
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
        SCIMAttribute.create(attributeDescriptor, entries));
  }



  /**
   * Set a simple attribute value with the specified name, using an
   * <code>AttributeValueResolver</code> appropriate for the attribute
   * data type.
   *
   * @param name the name of the attribute to be set, must be unique among all
   *             schemas in use by the Resource.
   * @param value the new attribute value.
   * @throws InvalidResourceException if no attribute with the specified name
   * is defined on the Resource, if the name is ambiguous, or if the
   * attribute is not of a simple data type.
   */
  public void setSimpleAttribute(
      final String name,
      final SimpleValue value) throws InvalidResourceException {

    setSimpleAttribute(findDescriptor(name), value);

  }


  /**
   * Set a simple attribute value with the specified schema and name,
   * using an <code>AttributeValueResolver</code> appropriate for
   * the attribute data type.
   *
   * @param schema Schema URI of the attribute to be set.
   * @param name the name of the attribute to be set.
   * @param value the new attribute value.
   * @throws InvalidResourceException if no attribute with the specified name
   * is defined by the schema, or if the attribute is not of a simple data
   * type.
   */
  public void setSimpleAttribute(
      final String schema,
      final String name,
      final SimpleValue value) throws InvalidResourceException {

    setSimpleAttribute(
        getResourceDescriptor().getAttribute(schema, name), value);

  }


  /**
   * Set a complex attribute value with the specified name.
   *
   * @param name the name of the attribute to be set, must be unique among all
   *             schemas in use by the Resource.
   * @param attributeValue ComplexValue object containing a map of
   *                       sub-attribute names and values
   * @throws InvalidResourceException if no attribute with the specified name
   * is defined on the Resource, if the name is ambiguous, or if the
   * attribute is not of a complex data type.
   */
  public void setComplexAttribute(
      final String name,
      final ComplexValue attributeValue)
      throws InvalidResourceException {

    setComplexAttribute(findDescriptor(name), attributeValue);
  }


  /**
   * Set a complex attribute value with the specified schema and name.
   *
   * @param schema Schema URI of the attribute to be set.
   * @param name The name of the attribute to be set.
   * @param attributeValue ComplexValue object containing a map of
   *                       sub-attribute names and values.
   * @throws InvalidResourceException if the name is not defined by the
   * specified schema or the attribute is not of a complex data type.
   */
  public void setComplexAttribute(
      final String schema,
      final String name,
      final ComplexValue attributeValue)
      throws InvalidResourceException {

    setComplexAttribute(
        getResourceDescriptor().getAttribute(schema, name),
        attributeValue);
  }


  /**
   * Set all values of a multi-valued attribute with the specified name.
   * If the attribute already exists, all values are overwritten.
   *
   * @param name The name of the attribute to be set, must be unique among all
   *             schemas in use by the Resource.
   * @param values A collection of complex attribute values.
   * @throws InvalidResourceException if no attribute with the specified name
   * is defined on the Resource, if the name is  ambiguous, or if the
   * attribute is not multi-valued.
   */
  public void setMultiValuedAttribute(
      final String name,
      final Collection<ComplexValue> values)
      throws InvalidResourceException {

    setMultiValuedAttribute(findDescriptor(name), values);
  }



  /**
   * Set all values of a multi-valued attribute with the specified schema and
   * name.  If the attribute already exists, all values are overwritten.
   *
   * @param schema Schema URI of the attribute to be set.
   * @param name The name of the attribute to be set.
   * @param values A collection of complex attribute values.
   * @throws InvalidResourceException if the name is not defined by the
   * specified schema or if the attribute is not multi-valued.
   */
  public void setMultiValuedAttribute(
      final String schema,
      final String name,
      final Collection<ComplexValue> values)
      throws InvalidResourceException {

    setMultiValuedAttribute(
        getResourceDescriptor().getAttribute(schema, name),
        values);
  }


  /**
   * Add or replace a value on a multi-valued attribute with the specified
   * name. If the attribute includes a canonical type sub-attribute
   * then a value matching the type will be overwritten.  Otherwise the new
   * value is added to the value list of the attribute.
   *
   * @param name The name of the attribute to be set, must be unique among all
   *             schemas in use by the Resource.
   * @param attributeValue the complex attribute value.
   * @throws InvalidResourceException if no attribute with the specified name
   * is defined on the Resource, if the name is  ambiguous, or if the
   * attribute is not multi-valued.
   */
  public void addOrReplaceMultiValuedValue(
      final String name,
      final ComplexValue attributeValue)
      throws InvalidResourceException {

    addOrReplaceMultiValuedValue(findDescriptor(name), attributeValue);
  }


  /**
   * Add or replace a value on a multi-valued attribute with the specified
   * schema and name. If the attribute includes a canonical type sub-attribute
   * then a value matching the type will be overwritten.  Otherwise the new
   * value is added to the value list of the attribute.
   *
   * @param schema Schema URI of the attribute to be set.
   * @param name  The name of the attribute to be set.
   * @param attributeValue the complex attribute value .
   * @throws InvalidResourceException if the attribute name is not
   * defined by the specified schema or if the attribute is not
   * multi-valued.
   */
  public void addOrReplaceMultiValuedValue(
      final String schema,
      final String name,
      final ComplexValue attributeValue)
      throws InvalidResourceException {

    addOrReplaceMultiValuedValue(
        getResourceDescriptor().getAttribute(schema, name),
        attributeValue);
  }



  /**
   * Get a simple attribute with the specified name.
   *
   * @param name The name of the attribute to retrieve, must be unique among
   *             all schemas in use by the Resource.
   * @return A SimpleValue, or null if the attribute is not present
   * on this Resource instance.
   * @throws InvalidResourceException if no attribute with the specified name
   * is defined on the Resource, or if the name is ambiguous.
   */
  public SimpleValue getSimpleAttributeValue(final String name)
      throws InvalidResourceException {

    return getSimpleAttributeValue(findDescriptor(name));
  }


  /**
   * Get a simple attribute with the specified name and schema.
   *
   * @param schema The schema URI of the attribute to retrieve.
   * @param name The name of the attribute to retrieve.
   * @return A SimpleValue, or null if the attribute is not present
   * on this Resource instance.
   * @throws InvalidResourceException if the attribute is not defined
   * by the resource schema or is not of a simple type.
   */
  public SimpleValue getSimpleAttributeValue(
      final String schema,
      final String name)
      throws InvalidResourceException {

    return getSimpleAttributeValue(getDescriptor(name, schema));
  }


  /**
   * Get a complex attribute with the specified name.
   *
   * @param name The name of the attribute to retrieve, must be unique among
   *             all schemas in use by the Resource.
   * @return A ComplexValue, or null if the attribute is not present
   * on this Resource instance.
   * @throws InvalidResourceException if no attribute with the specified name
   * is defined on the Resource, or if the name is ambiguous.
   */
  public ComplexValue getComplexAttributeValue(final String name)
      throws InvalidResourceException {

    return getComplexAttributeValue(findDescriptor(name));
  }


  /**
   * Get a complex attribute with the specified name and schema.
   *
   * @param schema The schema URI of the attribute to retrieve.
   * @param name The name of the attribute to retrieve.
   * @return A ComplexValue, or null if the attribute is not present
   * on this Resource instance.
   * @throws InvalidResourceException if the attribute is not defined
   * by the resource schema or is not complex.
   */
  public ComplexValue getComplexAttributeValue(
      final String schema,
      final String name)
      throws InvalidResourceException {

    return getComplexAttributeValue(getDescriptor(name, schema));
  }


  /**
   * Get the values of a multi-valued attribute with the specified name.
   *
   * @param name The name of the attribute to retrieve, must be unique among
   *             all schemas in use by the Resource.
   * @return A Collection of ComplexValue, one for each value instance,
   * or null if the attribute is not present on this Resource instance.
   * @throws InvalidResourceException if the attribute is not defined by
   * this SCIMResource or if the attribute is not multi-valued.
   */
  public Collection<ComplexValue> getMultiValuedAttribute(
      final String name) throws InvalidResourceException {

    return getAttributeValues(findDescriptor(name));
  }


  /**
   * Get the values of the multi-valued attribute specified by schema and
   * attribute name.
   *
   * @param schema The schema URI of the attribute to retrieve.
   * @param name The name of the attribute to retrieve.
   * @return A Collection of ComplexValue, one for each value instance,
   * or null if the attribute is not present on this Resource instance.
   * @throws InvalidResourceException if the attribute is not defined by
   * this SCIMResource or if the attribute is not multi-valued.
   */
  public Collection<ComplexValue> getMultiValuedAttribute(
      final String schema,
      final String name) throws InvalidResourceException {

    return getAttributeValues(getDescriptor(name, schema));
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



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("BaseResource");
    sb.append("{resource=").append(resourceDescriptor.getSchema());
    sb.append(SCIMConstants.SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE);
    sb.append(resourceDescriptor.getName());
    sb.append(", scimObject=").append(scimObject);
    sb.append('}');
    return sb.toString();
  }


  /**
   * Find the attribute with the given name but unspecified schema.
   * @param attributeName the name of the attribute to find
   * @return AttributeDescriptor for the found attribute
   * @throws InvalidResourceException if the specified attribute name
   * is not defined for this Resource, or if it is used by more than one
   * attribute schema.
   */
  private AttributeDescriptor findDescriptor(final String attributeName)
      throws InvalidResourceException {

    AttributeDescriptor attributeDescriptor = null;

    for (String schema : getResourceDescriptor().getAttributeSchemas()) {
      AttributeDescriptor d =
          getResourceDescriptor().findAttribute(schema, attributeName);
      if (d != null) {
        if (attributeDescriptor == null) {
          attributeDescriptor = d;
        }
        else {
          throw new InvalidResourceException(String.format(
              "Ambiguous attribute: %s is defined by more than one schema.",
              attributeName));
        }
      }
    }
    if (attributeDescriptor == null) {
      throw new InvalidResourceException(
          String.format("No attribute with name %s defined for this resource",
              attributeName));
    }
    return attributeDescriptor;
  }


  /**
   * Get the simple attribute defined by the specified AttributeDescriptor.
   * @param descriptor AttributeDescriptor object
   * @return A SimpleValue object, or null if the attribute is not
   * present on this SCIMResource instance.
   * @throws InvalidResourceException if the attribute is not of a simple
   * data type.
   */
  private SimpleValue getSimpleAttributeValue(
      final AttributeDescriptor descriptor) throws InvalidResourceException {

    validateNotComplex(descriptor);

    SCIMAttribute attribute = scimObject.getAttribute(descriptor.getSchema(),
        descriptor.getName());
    if(attribute != null) {
      SCIMAttributeValue value = attribute.getValue();
      if (value != null) {
        return value.getValue();
      }
    }
    return null;
  }


  /**
   * Get the complex attribute defined by the specified AttributeDescriptor.
   * @param descriptor AttributeDescriptor object
   * @return A Map of sub-attribute values, or null if the attribute is not
   * present on this SCIMResource instance
   * @throws InvalidResourceException if the attribute is not defined as
   * complex.
   */
  private ComplexValue getComplexAttributeValue(
      final AttributeDescriptor descriptor) throws InvalidResourceException {

    validateComplex(descriptor);
    validateSingular(descriptor);

    return getSingularAttributeValue(
        descriptor.getSchema(),
        descriptor.getName(),
        AttributeValueResolver.COMPLEX_RESOLVER);
  }


  /**
   * Get all values of a multi-valued attribute.
   * @param descriptor  AttributeDescriptor object
   * @return collection of complex attribute values
   * @throws InvalidResourceException if attribute is not multi-valued
   */
  private Collection<ComplexValue> getAttributeValues(
      final AttributeDescriptor descriptor) throws InvalidResourceException {

    validateMultiValued(descriptor);
    validateComplex(descriptor);   // is this needed - multi implies complex?

    return getAttributeValues(
        descriptor.getSchema(),
        descriptor.getName(),
        AttributeValueResolver.COMPLEX_RESOLVER);

  }


  /**
   * Set a simple attribute value.
   * @param descriptor AttributeDescriptor object
   * @param value the new attribute value
   * @throws InvalidResourceException if the attribute is not simple.
   */
  private void setSimpleAttribute(
      final AttributeDescriptor descriptor,
      final SimpleValue value) throws InvalidResourceException {

    validateNotComplex(descriptor);

    scimObject.setAttribute(SCIMAttribute.create(descriptor,
        SCIMAttributeValue.createSimpleValue(value)));
  }


  /**
   * Set a complex attribute value.
   * @param descriptor AttributeDescriptor object
   * @param attributeValue the ComplexAttributeValue to set
   * @throws InvalidResourceException if the attribute is not complex.
   */
  private void setComplexAttribute(
      final AttributeDescriptor descriptor,
      final ComplexValue attributeValue)
      throws InvalidResourceException {

    validateComplex(descriptor);
    validateSingular(descriptor);

    setSingularAttributeValue(descriptor.getSchema(),
        descriptor.getName(),
        AttributeValueResolver.COMPLEX_RESOLVER,
        attributeValue);
  }


  /**
   * Set all values of a multi-valued attribute.  If the attribute already
   * exists, all values are overwritten.  If the attribute does not exist
   * it is added.
   * @param descriptor AttributeDescriptor object
   * @param values A collection of complex attribute values.
   * @throws InvalidResourceException if the name is not defined by the
   * specified schema or if the attribute is not multi-valued.
   */
  private void setMultiValuedAttribute(
      final AttributeDescriptor descriptor,
      final Collection<ComplexValue> values)
      throws InvalidResourceException {

    validateMultiValued(descriptor);
    validateComplex(descriptor);   // is this needed?

    setAttributeValues(descriptor.getSchema(),
        descriptor.getName(),
        AttributeValueResolver.COMPLEX_RESOLVER,
        values);
  }


  /**
   * Add or replace a value on a multi-valued attribute.
   * If the attribute includes a canonical type sub-attribute then a value
   * matching the type will be overwritten.  Otherwise the new value is added
   * to the value list of the attribute.
   * @param descriptor AttributeDescriptor object
   * @param attributeValue the complex attribute vale, as a set of
   *                           sub-attribute values.
   * @throws InvalidResourceException if the attribute name is not
   * defined by the SCIMResource or if the attribute is not
   * multi-valued.
   */
  private void addOrReplaceMultiValuedValue(
      final AttributeDescriptor descriptor,
      final ComplexValue attributeValue)
      throws InvalidResourceException {

    Collection<ComplexValue> currentValues =
        getAttributeValues(descriptor);
    if (currentValues == null) {
      currentValues = Collections.singleton(attributeValue);
    }
    else {
      String canonicalType = attributeValue.getStringValue("type");
      if (canonicalType != null) {
        Iterator<ComplexValue> iterator = currentValues.iterator();
        while (iterator.hasNext()) {
          ComplexValue value = iterator.next();
          if (value.containsKey("type") &&
              value.getStringValue("type").equals(canonicalType)) {
            // replace this value with the input value
            iterator.remove();
            break;
          }
        }
      }
      currentValues.add(attributeValue);
    }
    setAttributeValues(descriptor.getSchema(),
        descriptor.getName(),
        AttributeValueResolver.COMPLEX_RESOLVER,
        currentValues);
  }



  /**
   * Utility method to get the AttributeDescriptor for the specified attribute
   * name and schema.
   * @param name attribute name
   * @param schema attribute schema URI
   * @return AttributeDescriptor object
   * @throws InvalidResourceException if the Resource does not define an
   * attribute with the specified name and schema.
   */
  private AttributeDescriptor getDescriptor(
      final String name,
      final String schema) throws InvalidResourceException {

    AttributeDescriptor descriptor =
        getResourceDescriptor().getAttribute(schema, name);
    if (descriptor == null) {
      throw new InvalidResourceException(
          String.format("No attribute with name %s defined with schema %s",
              name, schema)
      );
    }
    return descriptor;
  }


  /**
   * Validate that an attribute is a complex attribute.
   * @param descriptor AttributeDescriptor for the attribute
   * @throws InvalidResourceException if the attribute is not complex.
   */
  private void validateComplex(final AttributeDescriptor descriptor)
      throws InvalidResourceException {
    if (!descriptor.getDataType().equals(
        AttributeDescriptor.DataType.COMPLEX)) {
      throw new InvalidResourceException(
          String.format("Attribute %s is not defined as complex.",
              descriptor.getName()));
    }
  }

  /**
   * Validate that an attribute is not a complex attribute.
   * @param descriptor AttributeDescriptor for the attribute
   * @throws InvalidResourceException if the attribute is complex.
   */
  private void validateNotComplex(final AttributeDescriptor descriptor)
      throws InvalidResourceException {

    if (descriptor.getDataType().equals(AttributeDescriptor.DataType.COMPLEX)) {
      throw new InvalidResourceException(
          String.format("Attribute %s is defined as complex.",
              descriptor.getName()));
    }

  }


  /**
   * Validate that an attribute is multi-valued.
   * @param descriptor AttributeDescriptor for the attribute
   * @throws InvalidResourceException if the attribute is not multi-valued.
   */
  private void validateMultiValued(final AttributeDescriptor descriptor)
      throws InvalidResourceException {

    if (!descriptor.isMultiValued()) {
      throw new InvalidResourceException(String.format(
          "Attribute %s is not defined as multi-valued.",
          descriptor.getName()));
    }

  }


  /**
   * Validate that an attribute is singular.
   * @param descriptor AttributeDescriptor for the attribute
   * @throws InvalidResourceException if the attribute is multi-valued.
   */
  private void validateSingular(final AttributeDescriptor descriptor)
      throws InvalidResourceException {
    if (descriptor.isMultiValued()) {
      throw new InvalidResourceException(String.format(
          "Attribute %s is not a singular attribute.",
          descriptor.getName()));
    }
  }
}
