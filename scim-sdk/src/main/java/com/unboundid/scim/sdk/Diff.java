/*
 * Copyright 2013-2014 UnboundID Corp.
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

package com.unboundid.scim.sdk;

import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.schema.ResourceDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.unboundid.scim.sdk.StaticUtils.toLowerCase;

/**
 * This utility class may be used to generate a set of attribute
 * modifications between two SCIM resources of the same type. This is
 * especially useful for performing a PATCH request to modify a resource so it
 * matches a target resource. For example:
 *
 * <pre>
 * UserResource target = ...
 * UserResource source = userEndpoint.getUser("someUser");
 *
 * Diff diff = Diff.generate(source, target);
 *
 * userEndpoint.update(source.getId(),
 *                     diff.getAttributesToUpdate(),
 *                     diff.getAttributesToDelete());
 * </pre>
 *
 * You can also create a Diff instance from a SCIM partial resource which
 * contains PATCH modifications. This can then be applied to a source resource
 * to produce the target resource. For example:
 *
 * <pre>
 * Diff diff = Diff.fromPartialResource(partialResource);
 * BaseResource targetResource = diff.apply(sourceResource);
 * </pre>
 *
 * @param <R> The type of resource instances the diff was generated from.
 */
public final class Diff<R extends BaseResource>

{
  private final List<SCIMAttribute> attributesToUpdate;
  private final List<String> attributesToDelete;
  private final ResourceDescriptor resourceDescriptor;

  /**
   * Construct a new Diff instance.
   *
   * @param resourceDescriptor The resource descriptor of resource the diff
   *                           was generated from.
   * @param attributesToDelete The list of attributes deleted from source
   *                           resource.
   * @param attributesToUpdate The list of attributes (and their new values) to
   *                           update on the source resource.
   */
  Diff(final ResourceDescriptor resourceDescriptor,
       final List<String> attributesToDelete,
       final List<SCIMAttribute> attributesToUpdate)
  {
    this.resourceDescriptor = resourceDescriptor;
    this.attributesToDelete = attributesToDelete;
    this.attributesToUpdate = attributesToUpdate;
  }

  /**
   * Retrieves the list of attributes deleted from the source resource. The
   * values here are SCIM attribute names which may or may not contain the
   * schema URN. These can be easily parsed using the
   * {@link AttributePath#parse} method.
   *
   * @return The list of attributes deleted from source resource.
   */
  public List<String> getAttributesToDelete()
  {
    return attributesToDelete;
  }

  /**
   * Retrieves the list of updated attributes (and their new values) to
   * update on the source resource. These attributes will conform to
   * Section 3.2.2 of the SCIM 1.1 specification (<i>draft-scim-api-01</i>),
   * "Modifying Resources with PATCH".
   *
   * @return The list of attributes (and their new values) to update on the
   *         source resource. Note that the attributes are in PATCH form (i.e.
   *         they contain the values to merge into the resource).
   */
  public List<SCIMAttribute> getAttributesToUpdate()
  {
    return attributesToUpdate;
  }

  /**
   * Applies the modifications from this {@link Diff} to the specified source
   * resource, and returns the resulting SCIM resource.
   *
   * @param sourceResource the source resource to which the modifications should
   *                       be applied.
   * @param resourceFactory The ResourceFactory that should be used to create
   *                        the new resource instance.
   * @return the target resource with the modifications applied
   */
  public R apply(final R sourceResource,
                 final ResourceFactory<R> resourceFactory)
  {
    final SCIMObject scimObject =
            new SCIMObject(sourceResource.getScimObject());

    if(attributesToDelete != null)
    {
      for(String attrPath : attributesToDelete)
      {
        AttributePath path = AttributePath.parse(attrPath);
        String schema = path.getAttributeSchema();
        String attrName = path.getAttributeName();
        String subAttrName = path.getSubAttributeName();

        if (subAttrName != null)
        {
          attrName = attrName + "." + subAttrName;
        }

        scimObject.removeAttribute(schema, attrName);
      }
    }

    if (attributesToUpdate != null)
    {
      for(SCIMAttribute attr : attributesToUpdate)
      {
        if(attr.getAttributeDescriptor().isMultiValued())
        {
          //Go through and process all deleted values first
          for(SCIMAttributeValue value : attr.getValues())
          {
            SCIMAttribute currentAttribute =
                    scimObject.getAttribute(attr.getSchema(), attr.getName());

            if(value.isComplex())
            {
              String operation = value.getSubAttributeValue("operation",
                      AttributeValueResolver.STRING_RESOLVER);

              if("delete".equalsIgnoreCase(operation))
              {
                //We are deleting a specific value from this
                //multi-valued attribute
                List<SCIMAttribute> subAttrs = new ArrayList<SCIMAttribute>();
                Map<String, SCIMAttribute> subAttrMap = value.getAttributes();

                for(String subAttrName : subAttrMap.keySet())
                {
                  if(!"operation".equalsIgnoreCase(subAttrName))
                  {
                    subAttrs.add(subAttrMap.get(subAttrName));
                  }
                }

                SCIMAttributeValue valueToDelete =
                        SCIMAttributeValue.createComplexValue(subAttrs);

                if(currentAttribute != null)
                {
                  Set<SCIMAttributeValue> newValues =
                          new HashSet<SCIMAttributeValue>();

                  for(SCIMAttributeValue currentValue :
                          currentAttribute.getValues())
                  {
                    if(!currentValue.equals(valueToDelete))
                    {
                      newValues.add(currentValue);
                    }
                  }

                  if (!newValues.isEmpty())
                  {
                    SCIMAttribute finalAttribute = SCIMAttribute.create(
                          attr.getAttributeDescriptor(), newValues.toArray(
                                  new SCIMAttributeValue[newValues.size()]));

                    scimObject.setAttribute(finalAttribute);
                  }
                  else
                  {
                    scimObject.removeAttribute(
                            attr.getSchema(), attr.getName());
                  }
                }
              }
            }
          }

          //Now go through and merge in any new values
          for (SCIMAttributeValue value : attr.getValues())
          {
            SCIMAttribute currentAttribute =
                    scimObject.getAttribute(attr.getSchema(), attr.getName());

            Set<SCIMAttributeValue> newValues =
                    new HashSet<SCIMAttributeValue>();

            if(value.isComplex())
            {
              String operation = value.getSubAttributeValue("operation",
                      AttributeValueResolver.STRING_RESOLVER);

              if("delete".equalsIgnoreCase(operation))
              {
                continue; //handled earlier
              }

              String type = value.getSubAttributeValue("type",
                      AttributeValueResolver.STRING_RESOLVER);

              //It's a complex multi-valued attribute. If a value with the same
              //canonical type already exists, merge in the sub-attributes to
              //that existing value. Otherwise, add a new complex value to the
              //set of values.
              if(currentAttribute != null)
              {
                SCIMAttributeValue valueToUpdate = null;
                List<SCIMAttributeValue> finalValues =
                        new LinkedList<SCIMAttributeValue>();

                for(SCIMAttributeValue currentValue :
                               currentAttribute.getValues())
                {
                  String currentType = currentValue.getSubAttributeValue(
                          "type", AttributeValueResolver.STRING_RESOLVER);

                  if (type != null && type.equalsIgnoreCase(currentType))
                  {
                    valueToUpdate = currentValue;
                  }
                  else if (!currentValue.equals(value))
                  {
                    finalValues.add(currentValue);
                  }
                }

                if (valueToUpdate != null)
                {
                  Map<String, SCIMAttribute> subAttrMap = value.getAttributes();
                  Map<String, SCIMAttribute> existingSubAttrMap =
                          valueToUpdate.getAttributes();
                  Map<String, SCIMAttribute> finalSubAttrs =
                          new HashMap<String, SCIMAttribute>();

                  for(String subAttrName : existingSubAttrMap.keySet())
                  {
                    if(subAttrMap.containsKey(subAttrName))
                    {
                      //Replace the subAttr with the incoming value
                      finalSubAttrs.put(subAttrName,
                              subAttrMap.get(subAttrName));
                    }
                    else
                    {
                      //Leave this subAttr as-is (it's not being modified)
                      finalSubAttrs.put(subAttrName,
                              existingSubAttrMap.get(subAttrName));
                    }
                  }

                  //Add in any new sub-attributes that weren't in the
                  //existing set
                  for(String subAttrName : subAttrMap.keySet())
                  {
                    if(!finalSubAttrs.containsKey(subAttrName))
                    {
                      finalSubAttrs.put(subAttrName,
                              subAttrMap.get(subAttrName));
                    }
                  }

                  SCIMAttributeValue updatedValue = SCIMAttributeValue
                          .createComplexValue(finalSubAttrs.values());
                  finalValues.add(updatedValue);
                }
                else
                {
                  SCIMAttributeValue updatedValue = SCIMAttributeValue
                          .createComplexValue(value.getAttributes().values());
                  finalValues.add(updatedValue);
                }

                attr = SCIMAttribute.create(attr.getAttributeDescriptor(),
                        finalValues.toArray(new SCIMAttributeValue[
                                finalValues.size()]));
              }

              scimObject.setAttribute(attr);
            }
            else
            {
              //It's a simple multi-valued attribute. Merge this value into the
              //existing values (if any) for the attribute
              if(currentAttribute != null)
              {
                for(SCIMAttributeValue currentValue :
                         currentAttribute.getValues())
                {
                  newValues.add(currentValue);
                }
              }
              newValues.add(value);

              SCIMAttribute finalAttribute = SCIMAttribute.create(
                      attr.getAttributeDescriptor(), newValues.toArray(
                      new SCIMAttributeValue[newValues.size()]));

              scimObject.setAttribute(finalAttribute);
            }
          }
        }
        else //It's a single-valued attribute
        {
          if (scimObject.hasAttribute(attr.getSchema(), attr.getName()))
          {
            SCIMAttributeValue value = attr.getValue();
            if (value.isComplex())
            {
              SCIMAttribute existingAttr =
                      scimObject.getAttribute(attr.getSchema(), attr.getName());
              SCIMAttributeValue existingValue = existingAttr.getValue();

              Map<String,SCIMAttribute> subAttrMap = value.getAttributes();
              Map<String,SCIMAttribute> existingSubAttrMap =
                      existingValue.getAttributes();
              Map<String,SCIMAttribute> finalSubAttrs =
                      new HashMap<String,SCIMAttribute>();

              for (String subAttrName : existingSubAttrMap.keySet())
              {
                if (subAttrMap.containsKey(subAttrName))
                {
                  finalSubAttrs.put(subAttrName,
                          subAttrMap.get(subAttrName));
                }
                else
                {
                  finalSubAttrs.put(subAttrName,
                          existingSubAttrMap.get(subAttrName));
                }
              }

              //Add in any new sub-attributes that weren't in the existing set
              for (String subAttrName : subAttrMap.keySet())
              {
                if (!finalSubAttrs.containsKey(subAttrName))
                {
                  finalSubAttrs.put(subAttrName, subAttrMap.get(subAttrName));
                }
              }

              SCIMAttributeValue finalValue = SCIMAttributeValue
                      .createComplexValue(finalSubAttrs.values());
              attr = SCIMAttribute.create(
                      attr.getAttributeDescriptor(), finalValue);
            }
          }
          scimObject.setAttribute(attr);
        }
      }
    }

    return resourceFactory.createResource(resourceDescriptor, scimObject);
  }

  /**
   * Retrieves the partial resource with the modifications that maybe sent in
   * a PATCH request.
   *
   * @param resourceFactory The ResourceFactory that should be used to create
   *                        the new resource instance.
   * @param includeReadOnlyAttributes whether read-only attributes should be
   *                                  included in the partial resource. If this
   *                                  is {@code false}, these attributes will be
   *                                  stripped out.
   * @return The partial resource with the modifications that maybe sent in
   *         a PATCH request.
   * @throws InvalidResourceException If an error occurs.
   */
  public R toPartialResource(final ResourceFactory<R> resourceFactory,
                             final boolean includeReadOnlyAttributes)
      throws InvalidResourceException
  {
    SCIMObject scimObject = new SCIMObject();
    if(attributesToDelete != null && !attributesToDelete.isEmpty())
    {
      SCIMAttributeValue[] values =
              new SCIMAttributeValue[attributesToDelete.size()];
      for(int i = 0; i < attributesToDelete.size(); i++)
      {
        values[i] = SCIMAttributeValue.createStringValue(
                            attributesToDelete.get(i));
      }

      AttributeDescriptor subDescriptor =
              CoreSchema.META_DESCRIPTOR.getSubAttribute("attributes");

      SCIMAttribute attributes = SCIMAttribute.create(subDescriptor, values);

      SCIMAttribute meta = SCIMAttribute.create(
              CoreSchema.META_DESCRIPTOR,
              SCIMAttributeValue.createComplexValue(attributes));

      scimObject.setAttribute(meta);
    }

    if(attributesToUpdate != null)
    {
      for(SCIMAttribute attr : attributesToUpdate)
      {
        if(!attr.getAttributeDescriptor().isReadOnly() ||
                includeReadOnlyAttributes)
        {
          scimObject.setAttribute(attr);
        }
      }
    }

    return resourceFactory.createResource(resourceDescriptor, scimObject);
  }

  /**
   * Generates a diff with modifications that can be applied to the source
   * resource in order to make it match the target resource.
   *
   * @param <R> The type of the source and target resource instances.
   * @param partialResource The partial resource containing the PATCH
   *                        modifications from which to generate the diff.
   * @param includeReadOnlyAttributes whether read-only attributes should be
   *                                  included in the Diff. If this is
   *                                  {@code false}, these attributes will be
   *                                  stripped out.
   * @return A diff with modifications that can be applied to the source
   *         resource in order to make it match the target resource.
   */
  public static <R extends BaseResource> Diff<R> fromPartialResource(
           final R partialResource, final boolean includeReadOnlyAttributes)
  {
    final SCIMObject scimObject =
            new SCIMObject(partialResource.getScimObject());
    final Set<String> attributesToDelete = new HashSet<String>();
    final List<SCIMAttribute> attributesToUpdate =
            new ArrayList<SCIMAttribute>(10);

    SCIMAttribute metaAttr = scimObject.getAttribute(
            SCIMConstants.SCHEMA_URI_CORE, "meta");

    if(metaAttr != null)
    {
      SCIMAttribute attributesAttr =
              metaAttr.getValue().getAttribute("attributes");

      if(attributesAttr != null)
      {
        for(SCIMAttributeValue attrPath : attributesAttr.getValues())
        {
          attributesToDelete.add(attrPath.getStringValue());
        }
      }
    }

    scimObject.removeAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta");

    for(String schema : scimObject.getSchemas())
    {
      for(SCIMAttribute attr : scimObject.getAttributes(schema))
      {
        if(!attr.getAttributeDescriptor().isReadOnly() ||
                includeReadOnlyAttributes)
        {
          attributesToUpdate.add(attr);
        }
      }
    }

    return new Diff<R>(partialResource.getResourceDescriptor(),
        Collections.unmodifiableList(new ArrayList<String>(attributesToDelete)),
        Collections.unmodifiableList(attributesToUpdate));
  }

  /**
   * Generates a diff with modifications that can be applied to the source
   * resource in order to make it match the target resource.
   *
   * @param <R>    The type of the source and target resource instances.
   * @param source The source resource for which the set of modifications should
   *               be generated.
   * @param target The target resource, which is what the source resource should
   *               look like if the returned modifications are applied.
   * @param attributes The set of attributes to be compared in standard
   *                   attribute notation (ie. name.givenName). If this is
   *                   {@code null} or empty, then all attributes will be
   *                   compared.
   * @return A diff with modifications that can be applied to the source
   *         resource in order to make it match the target resource.
   */
  public static <R extends BaseResource> Diff<R> generate(
      final R source, final R target, final String... attributes)
  {
    final SCIMObject sourceObject = source.getScimObject();
    final SCIMObject targetObject = target.getScimObject();

    HashMap<String, HashMap<String, HashSet<String>>> compareAttrs = null;
    if ((attributes != null) && (attributes.length > 0))
    {
      compareAttrs = new HashMap<String, HashMap<String, HashSet<String>>>();
      for (final String s : attributes)
      {
        final AttributePath path = AttributePath.parse(s);
        final String schema = toLowerCase(path.getAttributeSchema());
        final String attrName = toLowerCase(path.getAttributeName());
        final String subAttrName = path.getSubAttributeName() == null ? null :
            toLowerCase(path.getSubAttributeName());

        HashMap<String, HashSet<String>> schemaAttrs = compareAttrs.get(schema);
        if(schemaAttrs == null)
        {
          schemaAttrs = new HashMap<String, HashSet<String>>();
          compareAttrs.put(schema, schemaAttrs);
        }
        HashSet<String> subAttrs = schemaAttrs.get(attrName);
        if(subAttrs == null)
        {
          subAttrs = new HashSet<String>();
          schemaAttrs.put(attrName, subAttrs);
        }
        if(subAttrName != null)
        {
          subAttrs.add(subAttrName);
        }
      }
    }

    final SCIMObject sourceOnlyAttrs = new SCIMObject();
    final SCIMObject targetOnlyAttrs = new SCIMObject();
    final SCIMObject commonAttrs = new SCIMObject();

    for (final String schema : sourceObject.getSchemas())
    {
      for (final SCIMAttribute attribute : sourceObject.getAttributes(schema))
      {
        if (!shouldProcess(compareAttrs, attribute, null))
        {
          continue;
        }

        sourceOnlyAttrs.setAttribute(attribute);
        commonAttrs.setAttribute(attribute);
      }
    }

    for (final String schema : targetObject.getSchemas())
    {
      for (final SCIMAttribute attribute : targetObject.getAttributes(schema))
      {
        if (!shouldProcess(compareAttrs, attribute, null))
        {
          continue;
        }

        if (!sourceOnlyAttrs.removeAttribute(
            attribute.getSchema(), attribute.getName()))
        {
          // It wasn't in the set of source attributes, so it must be a
          // target-only attribute.
          targetOnlyAttrs.setAttribute(attribute);
        }
      }
    }

    for (final String schema : sourceOnlyAttrs.getSchemas())
    {
      for (final SCIMAttribute attribute :
          sourceOnlyAttrs.getAttributes(schema))
      {
        commonAttrs.removeAttribute(attribute.getSchema(), attribute.getName());
      }
    }

    final Set<String> attributesToDelete = new HashSet<String>();
    final List<SCIMAttribute> attributesToUpdate =
        new ArrayList<SCIMAttribute>(10);

    // Delete all attributes that are only in the source object
    for (final String schema : sourceOnlyAttrs.getSchemas())
    {
      for (final SCIMAttribute sourceAttribute :
          sourceOnlyAttrs.getAttributes(schema))
      {
        deleteAttribute(compareAttrs, attributesToDelete, sourceAttribute);
      }
    }

    // Add all attributes that are only in the target object
    for (final String schema : targetOnlyAttrs.getSchemas())
    {
      for (final SCIMAttribute targetAttribute :
          targetOnlyAttrs.getAttributes(schema))
      {
        if (targetAttribute.getAttributeDescriptor().isMultiValued())
        {
          ArrayList<SCIMAttributeValue> targetValues =
              new ArrayList<SCIMAttributeValue>(
                  targetAttribute.getValues().length);
          for (SCIMAttributeValue targetValue : targetAttribute.getValues())
          {
            Map<String, SCIMAttribute> subAttrs =
                filterSubAttributes(compareAttrs, targetAttribute,
                    targetValue);
            if(!subAttrs.isEmpty())
            {
              targetValues.add(
                  SCIMAttributeValue.createComplexValue(subAttrs.values()));
            }
          }
          if(!targetValues.isEmpty())
          {
            attributesToUpdate.add(SCIMAttribute.create(
                targetAttribute.getAttributeDescriptor(), targetValues.toArray(
                new SCIMAttributeValue[targetValues.size()])));
          }
        }
        else if(targetAttribute.getValue().isComplex())
        {
          Map<String, SCIMAttribute> subAttrs =
              filterSubAttributes(compareAttrs, targetAttribute,
                  targetAttribute.getValue());
          if(!subAttrs.isEmpty())
          {
            attributesToUpdate.add(
                SCIMAttribute.create(targetAttribute.getAttributeDescriptor(),
                    SCIMAttributeValue.createComplexValue(subAttrs.values())));
          }
        }
        else
        {
          attributesToUpdate.add(targetAttribute);
        }
      }
    }

    // Add all common attributes with different values
    for (final String schema : commonAttrs.getSchemas())
    {
      for (final SCIMAttribute sourceAttribute :
          commonAttrs.getAttributes(schema))
      {
        SCIMAttribute targetAttribute =
            targetObject.getAttribute(sourceAttribute.getSchema(),
                sourceAttribute.getName());
        if (sourceAttribute.equals(targetAttribute))
        {
          continue;
        }

        if(sourceAttribute.getAttributeDescriptor().isMultiValued())
        {
          Set<SCIMAttributeValue> sourceValues =
              new LinkedHashSet<SCIMAttributeValue>(
                  sourceAttribute.getValues().length);
          Set<SCIMAttributeValue> targetValues =
              new LinkedHashSet<SCIMAttributeValue>(
                  targetAttribute.getValues().length);
          Collections.addAll(sourceValues, sourceAttribute.getValues());

          for (SCIMAttributeValue v : targetAttribute.getValues())
          {
            if (!sourceValues.remove(v))
            {
              // This value could be an added or updated value
              // TODO: Support matching on value sub-attribute if possible?
              targetValues.add(v);
            }
          }

          if(sourceValues.size() == sourceAttribute.getValues().length)
          {
            // All source values seem to have been deleted. Just delete the
            // attribute instead of listing all delete values.
            deleteAttribute(compareAttrs, attributesToDelete, sourceAttribute);
            sourceValues = Collections.emptySet();
          }

          ArrayList<SCIMAttributeValue> patchValues =
              new ArrayList<SCIMAttributeValue>(
                  sourceValues.size() + targetValues.size());
          for (SCIMAttributeValue sourceValue : sourceValues)
          {
            Map<String, SCIMAttribute> subAttrs =
                filterSubAttributes(compareAttrs, sourceAttribute, sourceValue);
            if(!subAttrs.isEmpty())
            {
              SCIMAttribute operationAttr;
              try
              {
                operationAttr = SCIMAttribute.create(
                    sourceAttribute.getAttributeDescriptor().getSubAttribute(
                        "operation"),
                    SCIMAttributeValue.createStringValue("delete"));
              }
              catch (InvalidResourceException e)
              {
                // This should never happen
                throw new IllegalStateException(e);
              }
              subAttrs.put(toLowerCase(operationAttr.getName()), operationAttr);
              patchValues.add(SCIMAttributeValue.createComplexValue(
                  subAttrs.values()));
            }
          }
          for (SCIMAttributeValue targetValue : targetValues)
          {
            // Add any new or updated target sub-attributes
            Map<String, SCIMAttribute> subAttrs =
                filterSubAttributes(compareAttrs, targetAttribute, targetValue);
            if(!subAttrs.isEmpty())
            {
              patchValues.add(SCIMAttributeValue.createComplexValue(
                              subAttrs.values()));
            }
          }
          if(!patchValues.isEmpty())
          {
            attributesToUpdate.add(SCIMAttribute.create(
                sourceAttribute.getAttributeDescriptor(), patchValues.toArray(
                new SCIMAttributeValue[patchValues.size()])));
          }
        }
        else if(sourceAttribute.getValue().isComplex())
        {
          // Remove any source only sub-attributes
          SCIMAttributeValue sourceAttributeValue =
              sourceAttribute.getValue();
          SCIMAttributeValue targetAttributeValue =
              targetAttribute.getValue();
          for (final Map.Entry<String, SCIMAttribute> e :
              filterSubAttributes(compareAttrs, sourceAttribute,
                  sourceAttributeValue).entrySet())
          {
            if(!targetAttributeValue.hasAttribute(e.getKey()))
            {
              final AttributePath path =
                  new AttributePath(sourceAttribute.getSchema(),
                      sourceAttribute.getName(), e.getValue().getName());
              attributesToDelete.add(path.toString());
            }
          }

          // Add any new or updated target sub-attributes
          Map<String, SCIMAttribute> targetSubAttrs =
              filterSubAttributes(compareAttrs, targetAttribute,
                  targetAttributeValue);
          final Iterator<Map.Entry<String, SCIMAttribute>> targetIterator =
              targetSubAttrs.entrySet().iterator();
          while(targetIterator.hasNext())
          {
            Map.Entry<String, SCIMAttribute> e = targetIterator.next();
            SCIMAttribute sourceSubAttr =
                sourceAttributeValue.getAttribute(e.getKey());
            if(sourceSubAttr != null && sourceSubAttr.equals(e.getValue()))
            {
              // This sub-attribute is the same so do not include it in the
              // patch.
              targetIterator.remove();
            }
          }
          if(!targetSubAttrs.isEmpty())
          {
            attributesToUpdate.add(SCIMAttribute.create(
                targetAttribute.getAttributeDescriptor(),
                SCIMAttributeValue.createComplexValue(
                    targetSubAttrs.values())));
          }
        }
        else
        {
          attributesToUpdate.add(targetAttribute);
        }
      }
    }

    return new Diff<R>(source.getResourceDescriptor(),
        Collections.unmodifiableList(new ArrayList<String>(attributesToDelete)),
        Collections.unmodifiableList(attributesToUpdate));
  }

  /**
   * Utility method to determine if an attribute should be processed when
   * generating the modifications.
   *
   * @param compareAttrs The map of attributes to be compared.
   * @param attribute The attribute to consider.
   * @param subAttribute The sub-attribute to consider or {@code null} if
   *                     not available.
   * @return {@code true} if the attribute should be processed or
   *         {@code false} otherwise.
   */
  private static boolean shouldProcess(
      final HashMap<String, HashMap<String, HashSet<String>>> compareAttrs,
      final SCIMAttribute attribute, final SCIMAttribute subAttribute)
  {
    if(compareAttrs == null)
    {
      return true;
    }

    final HashMap<String, HashSet<String>> schemaAttrs =
        compareAttrs.get(toLowerCase(attribute.getSchema()));

    if(schemaAttrs == null)
    {
      return false;
    }

    final HashSet<String> subAttrs = schemaAttrs.get(toLowerCase(
        attribute.getName()));

    return subAttrs != null && (
        !(subAttribute != null && !subAttrs.isEmpty()) ||
            subAttrs.contains(toLowerCase(subAttribute.getName())));
  }

  /**
   * Utility method to filter sub-attributes down to only those that should
   * be processed when generating the modifications.
   *
   * @param compareAttrs The map of attributes to be compared.
   * @param attribute The attribute to consider.
   * @param value     The complex SCIMAttributeValue to filter
   * @return A map of sub-attributes that should be included in the diff.
   */
  private static Map<String, SCIMAttribute> filterSubAttributes(
      final HashMap<String, HashMap<String, HashSet<String>>> compareAttrs,
      final SCIMAttribute attribute, final SCIMAttributeValue value)
  {
    Map<String, SCIMAttribute> filteredSubAttributes =
        new LinkedHashMap<String, SCIMAttribute>(
            value.getAttributes());
    Iterator<Map.Entry<String, SCIMAttribute>> subAttrsIterator =
        filteredSubAttributes.entrySet().iterator();
    while(subAttrsIterator.hasNext())
    {
      Map.Entry<String, SCIMAttribute> e = subAttrsIterator.next();
      if(!shouldProcess(compareAttrs, attribute, e.getValue()))
      {
        subAttrsIterator.remove();
      }
    }

    return filteredSubAttributes;
  }

  /**
   * Utility method to add an attribute and all its sub-attributes if
   * applicable to the attributesToDelete set.
   *
   * @param compareAttrs The map of attributes to be compared.
   * @param attributesToDelete The list of attributes to delete to append.
   * @param attribute The attribute to delete.
   */
  private static void deleteAttribute(
      final HashMap<String, HashMap<String, HashSet<String>>> compareAttrs,
      final Set<String> attributesToDelete, final SCIMAttribute attribute)
  {
    if(attribute.getAttributeDescriptor().isMultiValued())
    {
      // Technically, all multi-valued attributes are complex since they may
      // have sub-attributes.
      Set<String> subAttributes = new HashSet<String>();
      for(SCIMAttributeValue sourceValue : attribute.getValues())
      {
        if(sourceValue.isComplex())
        {
          for(Map.Entry<String, SCIMAttribute> e :
              filterSubAttributes(compareAttrs, attribute,
                  sourceValue).entrySet())
          {
            // Skip non-significant normative sub-attributes
            if(e.getKey().equals("type") ||
                e.getKey().equals("primary") ||
                e.getKey().equals("operation") ||
                e.getKey().equals("display"))
            {
              continue;
            }

            final AttributePath path =
                new AttributePath(attribute.getSchema(),
                    attribute.getName(), e.getKey());
            subAttributes.add(path.toString());
          }
        }
        else
        {
          // There are no sub-attributes for this attribute, which is
          // technically not correct. Just delete the whole attribute
          final AttributePath path =
              new AttributePath(attribute.getSchema(),
                  attribute.getName(),
                  null);
          subAttributes.clear();
          attributesToDelete.add(path.toString());
          break;
        }
      }
      attributesToDelete.addAll(subAttributes);
    }
    else if(attribute.getAttributeDescriptor().getDataType() ==
        AttributeDescriptor.DataType.COMPLEX)
    {
      for(Map.Entry<String, SCIMAttribute> e :
          filterSubAttributes(compareAttrs, attribute,
              attribute.getValue()).entrySet())
      {
        final AttributePath path =
            new AttributePath(attribute.getSchema(),
                attribute.getName(), e.getKey());
        attributesToDelete.add(path.toString());
      }
    }
    else
    {
      final AttributePath path =
          new AttributePath(attribute.getSchema(),
              attribute.getName(),
              null);
      attributesToDelete.add(path.toString());
    }
  }

}
