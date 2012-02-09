/*
 * Copyright 2011-2012 UnboundID Corp.
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

import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;



/**
 * This class represents a list of query attributes taken from the attributes
 * query parameter. e.g. attributes=name.formatted,userName
 */
public class SCIMQueryAttributes
{
  /**
   * Indicates whether all attributes and sub-attributes are requested.
   */
  private final boolean allAttributesRequested;

  /**
   * The set of attributes and sub-attributes explicitly requested.
   */
  private final Map<AttributeDescriptor,Set<AttributeDescriptor>> descriptors;



  /**
   * Create a new instance of query attributes from their string representation.
   *
   * @param resourceDescriptor  The resource descriptor for the SCIM endpoint.
   * @param attributes     The attributes query parameter specifying the set of
   *                       attributes or sub-attributes requested, or null if
   *                       all attributes and sub-attributes are requested. The
   *                       attributes must be qualified by their
   *                       schema URI if they are not in the core schema.
   *
   * @throws InvalidResourceException  If one of the specified attributes does
   *                                   not exist.
   */
  public SCIMQueryAttributes(final ResourceDescriptor resourceDescriptor,
                             final String attributes)
      throws InvalidResourceException
  {
    descriptors =
        new HashMap<AttributeDescriptor, Set<AttributeDescriptor>>();

    if (attributes == null)
    {
      allAttributesRequested = true;
    }
    else
    {
      allAttributesRequested = false;
      if (!attributes.isEmpty())
      {
        final String[] paths = attributes.split(",");
        if (paths.length > 0)
        {
          for (final String a : paths)
          {
            final AttributePath path = AttributePath.parse(a);
            final AttributeDescriptor attributeDescriptor =
                resourceDescriptor.getAttribute(path.getAttributeSchema(),
                                                path.getAttributeName());

            Set<AttributeDescriptor> subAttributes =
                descriptors.get(attributeDescriptor);
            if (subAttributes == null)
            {
              subAttributes = new HashSet<AttributeDescriptor>();
              if (path.getSubAttributeName() != null)
              {
                subAttributes.add(
                    attributeDescriptor.getSubAttribute(
                        path.getSubAttributeName()));
              }
              descriptors.put(attributeDescriptor, subAttributes);
            }
            else
            {
              if (!subAttributes.isEmpty())
              {
                if (path.getSubAttributeName() != null)
                {
                  subAttributes.add(
                      attributeDescriptor.getSubAttribute(
                          path.getSubAttributeName()));
                }
                else
                {
                  subAttributes.clear();
                }
              }
            }
          }
        }
      }

      final AttributeDescriptor id =
          resourceDescriptor.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "id");
      if (!descriptors.containsKey(id))
      {
        descriptors.put(id, new HashSet<AttributeDescriptor>());
      }

      final AttributeDescriptor meta =
          resourceDescriptor.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                                          "meta");
      if (!descriptors.containsKey(meta))
      {
        descriptors.put(meta, new HashSet<AttributeDescriptor>());
      }
    }
  }



  /**
   * Create a new set of query attributes from the provided information.
   *
   * @param descriptors         The set of attributes and sub-attributes
   *                            explicitly requested, or {@code null} if all
   *                            attributes are requested.
   */
  private SCIMQueryAttributes(
      final Map<AttributeDescriptor,Set<AttributeDescriptor>> descriptors)
  {
    this.allAttributesRequested = (descriptors == null);
    this.descriptors = descriptors;
  }



  /**
   * Determine whether all attributes and sub-attributes are requested by
   * these query attributes.
   *
   * @return  {@code true} if all attributes and sub-attributes are requested,
   *          and {@code false} otherwise.
   */
  public boolean allAttributesRequested()
  {
    return allAttributesRequested;
  }



  /**
   * Determine whether the specified attribute is requested by these query
   * attributes.
   *
   * @param attributeDescriptor  The attribute for which to make the
   *                             determination.
   *
   * @return  {@code true} if the specified attribute is requested, or false
   *          otherwise.
   */
  public boolean isAttributeRequested(
      final AttributeDescriptor attributeDescriptor)
  {
    return allAttributesRequested() ||
           descriptors.containsKey(attributeDescriptor);
  }



  /**
   * Pare down a SCIM object to its requested attributes.
   *
   * @param scimObject  The SCIM object to be pared down.
   *
   * @return  The pared down SCIM object.
   */
  public SCIMObject pareObject(final SCIMObject scimObject)
  {
    if (allAttributesRequested())
    {
      return scimObject;
    }

    final SCIMObject paredObject = new SCIMObject();
    for (final Map.Entry<AttributeDescriptor,Set<AttributeDescriptor>> entry :
        descriptors.entrySet())
    {
      final AttributeDescriptor attributeDescriptor = entry.getKey();

      final SCIMAttribute a =
          scimObject.getAttribute(attributeDescriptor.getSchema(),
                                  attributeDescriptor.getName());
      if (a != null)
      {
        final SCIMAttribute paredAttribute = pareAttribute(a);
        if (paredAttribute != null)
        {
          paredObject.addAttribute(paredAttribute);
        }
      }
    }

    return paredObject;
  }



  /**
   * Pare down an attribute to its requested sub-attributes.
   *
   * @param attribute  The attribute to be pared down.
   *
   * @return  The pared down attribute, or {@code null} if the attribute
   *          should not be included at all.
   */
  public SCIMAttribute pareAttribute(final SCIMAttribute attribute)
  {
    final AttributeDescriptor descriptor = attribute.getAttributeDescriptor();

    if (allAttributesRequested() || descriptor.getSubAttributes() == null)
    {
      return attribute;
    }

    final Set<AttributeDescriptor> subDescriptors = descriptors.get(descriptor);
    if (subDescriptors == null)
    {
      return null;
    }

    if (subDescriptors.isEmpty())
    {
      return attribute;
    }

    if (attribute.getAttributeDescriptor().isMultiValued())
    {
      final ArrayList<SCIMAttributeValue> values =
          new ArrayList<SCIMAttributeValue>();

      for (final SCIMAttributeValue v : attribute.getValues())
      {
        final ArrayList<SCIMAttribute> subAttributes =
            new ArrayList<SCIMAttribute>();
        for (final AttributeDescriptor d : subDescriptors)
        {
          final SCIMAttribute subAttribute = v.getAttribute(d.getName());
          if (subAttribute != null)
          {
            subAttributes.add(subAttribute);
          }
        }
        values.add(SCIMAttributeValue.createComplexValue(subAttributes));
      }

      return SCIMAttribute.create(
          descriptor, values.toArray(new SCIMAttributeValue[values.size()]));
    }
    else
    {
      final ArrayList<SCIMAttribute> subAttributes =
          new ArrayList<SCIMAttribute>();
      for (final AttributeDescriptor d : subDescriptors)
      {
        final SCIMAttribute subAttribute =
            attribute.getValue().getAttribute(d.getName());
        if (subAttribute != null)
        {
          subAttributes.add(subAttribute);
        }
      }
      return SCIMAttribute.create(descriptor,
          SCIMAttributeValue.createComplexValue(subAttributes));
    }
  }



  /**
   * Return query attributes formed by merging these query attributes with the
   * provided query attributes.
   *
   * @param that  The query attributes to be merged with these query attributes
   *              to form new query attributes.
   *
   * @return  The merged query attributes.
   *
   * @throws InvalidResourceException  If the query attributes could not be
   *                                   merged.
   */
  public SCIMQueryAttributes merge(final SCIMQueryAttributes that)
      throws InvalidResourceException
  {
    if (this.allAttributesRequested || that.allAttributesRequested)
    {
      return new SCIMQueryAttributes(null);
    }

    final Map<AttributeDescriptor,Set<AttributeDescriptor>> merged =
        new HashMap<AttributeDescriptor, Set<AttributeDescriptor>>(
            this.descriptors);

    for (final Map.Entry<AttributeDescriptor,Set<AttributeDescriptor>> e :
        that.descriptors.entrySet())
    {
      final AttributeDescriptor attributeDescriptor = e.getKey();
      final Set<AttributeDescriptor> thatSet = e.getValue();

      Set<AttributeDescriptor> thisSet = merged.get(attributeDescriptor);
      if (thisSet == null)
      {
        merged.put(attributeDescriptor, thatSet);
      }
      else
      {
        if (!thisSet.isEmpty())
        {
          if (thatSet.isEmpty())
          {
            thisSet.clear();
          }
          else
          {
            thisSet.addAll(thatSet);
          }
        }
      }
    }

    return new SCIMQueryAttributes(merged);
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("SCIMQueryAttributes");
    sb.append("{allAttributesRequested=").append(allAttributesRequested);
    sb.append(", descriptors=").append(descriptors);
    sb.append('}');
    return sb.toString();
  }
}
