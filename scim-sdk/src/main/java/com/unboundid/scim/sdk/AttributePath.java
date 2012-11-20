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

import com.unboundid.scim.schema.CoreSchema;

import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * This class represents a path to an attribute or sub-attribute. The path is
 * a string comprising an schema URI (the SCIM core schema is assumed
 * if absent), an attribute name, and an optional sub-attribute name.
 */
public class AttributePath
{
  /**
   * A regular expression to match the components of an attribute path.
   * Given the path "urn:scim:schemas:core:user:1.0:name.familyName" then
   * group 2 will match "urn:scim:schemas:core:user:1.0", group 3 matches
   * "name" and group 5 matches "familyName".
   */
  private static final Pattern pattern =
      Pattern.compile("^((.+):)?([^.]+)(\\.(.+))?$");

  /**
   * The URI of the attribute schema.
   */
  private final String attributeSchema;

  /**
   * The name of the attribute.
   */
  private final String attributeName;

  /**
   * The name of the sub-attribute, or {@code null} if absent.
   */
  private final String subAttributeName;



  /**
   * Create a new attribute path.
   *
   * @param attributeSchema   The URI of the attribute schema.
   * @param attributeName     The name of the attribute.
   * @param subAttributeName  The name of the sub-attribute, or {@code null} if
   *                          absent.
   */
  public AttributePath(final String attributeSchema,
                       final String attributeName,
                       final String subAttributeName)
  {
    this.attributeSchema  = attributeSchema;
    this.attributeName    = attributeName;
    this.subAttributeName = subAttributeName;
  }



  /**
   * Parse an attribute path.
   *
   * @param path  The attribute path.
   *
   * @return The parsed attribute path.
   */
  public static AttributePath parse(final String path)
  {
    final Matcher matcher = pattern.matcher(path);

    if (!matcher.matches() || matcher.groupCount() != 5)
    {
      throw new IllegalArgumentException(
          String.format(
              "'%s' does not match '[schema:]attr[.sub-attr]' format", path));
    }

    final String attributeSchema = matcher.group(2);
    final String attributeName = matcher.group(3);
    final String subAttributeName = matcher.group(5);

    if (attributeSchema != null)
    {
      return new AttributePath(attributeSchema, attributeName,
                               subAttributeName);
    }
    else
    {
      return new AttributePath(SCIMConstants.SCHEMA_URI_CORE, attributeName,
                               subAttributeName);
    }
  }



  /**
   * Parse an attribute path.
   *
   * @param path  The attribute path.
   * @param defaultSchema The default schema to assume for attributes that do
   *                      not have the schema part of the urn specified. The
   *                      'id', 'externalId', and 'meta' attributes will always
   *                      assume the SCIM Core schema.
   *
   * @return The parsed attribute path.
   */
  public static AttributePath parse(final String path,
                                    final String defaultSchema)
  {
    final Matcher matcher = pattern.matcher(path);

    if (!matcher.matches() || matcher.groupCount() != 5)
    {
      throw new IllegalArgumentException(
              String.format(
                "'%s' does not match '[schema:]attr[.sub-attr]' format", path));
    }

    final String attributeSchema = matcher.group(2);
    final String attributeName = matcher.group(3);
    final String subAttributeName = matcher.group(5);

    if (attributeSchema != null)
    {
      return new AttributePath(attributeSchema, attributeName,
              subAttributeName);
    }
    else
    {
      if (attributeName.equalsIgnoreCase(
                  CoreSchema.ID_DESCRIPTOR.getName()) ||
          attributeName.equalsIgnoreCase(
                  CoreSchema.EXTERNAL_ID_DESCRIPTOR.getName()) ||
          attributeName.equalsIgnoreCase(
                  CoreSchema.META_DESCRIPTOR.getName()))
      {
        return new AttributePath(SCIMConstants.SCHEMA_URI_CORE, attributeName,
                subAttributeName);
      }
      else
      {
        return new AttributePath(defaultSchema, attributeName,
                subAttributeName);
      }
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder builder = new StringBuilder();
    toString(builder);
    return builder.toString();
  }



  /**
   * Append the string representation of the attribute path to the provided
   * buffer.
   *
   * @param builder  The buffer to which the string representation of the
   *                 attribute path is to be appended.
   */
  public void toString(final StringBuilder builder)
  {
    if (!attributeSchema.equalsIgnoreCase(SCIMConstants.SCHEMA_URI_CORE))
    {
      builder.append(attributeSchema);
      builder.append(':');
    }

    builder.append(attributeName);
    if (subAttributeName != null)
    {
      builder.append('.');
      builder.append(subAttributeName);
    }
  }



  /**
   * Retrieve the URI of the attribute schema.
   * @return The URI of the attribute schema.
   */
  public String getAttributeSchema()
  {
    return attributeSchema;
  }



  /**
   * Retrieve the name of the attribute.
   * @return The name of the attribute.
   */
  public String getAttributeName()
  {
    return attributeName;
  }



  /**
   * Retrieve the name of the sub-attribute, or {@code null} if absent.
   * @return The name of the sub-attribute, or {@code null} if absent.
   */
  public String getSubAttributeName()
  {
    return subAttributeName;
  }
}
