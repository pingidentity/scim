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

package com.unboundid.scim.sdk;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;



/**
 * This class represents a SCIM query filter.
 */
public class SCIMFilter
{
  /**
   * The filter type.
   */
  private final SCIMFilterType filterType;

  /**
   * The attribute or sub-attribute to filter by, or {@code null} if not
   * applicable.
   */
  private final AttributePath filterAttribute;

  /**
   * The filter attribute value, or {@code null} if not applicable.
   */
  private final String filterValue;

  /**
   * Specifies whether the filter value is quoted in the string representation
   * of the filter. String and DateTime values are quoted. Integer and Boolean
   * values are not quoted.
   */
  private final boolean quoteFilterValue;

  /**
   * The filter components for 'or' and 'and' filter types, or {@code null}
   * if not applicable.
   */
  private final List<SCIMFilter> filterComponents;



  /**
   * Create a new SCIM filter from the provided information.
   *
   * @param filterType        The filter type.
   * @param filterAttribute   The attribute or sub-attribute to filter by, or
   *                          {@code null} if not applicable.
   * @param filterValue       The filter attribute value, or {@code null} if not
   *                          applicable.
   * @param quoteFilterValue  Specifies whether the filter value is quoted in
   *                          the string representation of the filter.
   * @param filterComponents  The filter components for 'or' and 'and' filter
   *                          types, or {@code null} if not applicable.
   */
  public SCIMFilter(final SCIMFilterType filterType,
                    final AttributePath filterAttribute,
                    final String filterValue,
                    final boolean quoteFilterValue,
                    final List<SCIMFilter> filterComponents)
  {
    this.filterType        = filterType;
    this.filterAttribute   = filterAttribute;
    this.filterValue       = filterValue;
    this.quoteFilterValue  = quoteFilterValue;
    this.filterComponents  = filterComponents;
  }



  /**
   * Parse a filter from its string representation.
   *
   * @param filterString  The string representation of the filter expression.
   *
   * @return  The parsed filter.
   *
   * @throws  SCIMException  If the filter string could not be parsed.
   */
  public static SCIMFilter parse(final String filterString)
      throws SCIMException
  {
    final FilterParser parser = new FilterParser(filterString,
                                                 SCIMConstants.SCHEMA_URI_CORE);
    return parser.parse();
  }



  /**
   * Parse a filter from its string representation.
   *
   * @param filterString  The string representation of the filter expression.
   * @param defaultSchema The default schema that should be assumed when parsing
   *                      attributes without the schema explicitly defined in
   *                      the URN.
   *
   * @return  The parsed filter.
   *
   * @throws  SCIMException  If the filter string could not be parsed.
   */
  public static SCIMFilter parse(final String filterString,
                                 final String defaultSchema)
          throws SCIMException
  {
    final FilterParser parser = new FilterParser(filterString, defaultSchema);
    return parser.parse();
  }



  /**
   * Create a new and filter.
   *
   * @param filterComponents  The filter components.
   *
   * @return  A new and filter.
   */
  public static SCIMFilter createAndFilter(
      final List<SCIMFilter> filterComponents)
  {
    return new SCIMFilter(SCIMFilterType.AND, null, null, false,
                          new ArrayList<SCIMFilter>(filterComponents));
  }



  /**
   * Create a new or filter.
   *
   * @param filterComponents  The filter components.
   *
   * @return  A new or filter.
   */
  public static SCIMFilter createOrFilter(
      final List<SCIMFilter> filterComponents)
  {
    return new SCIMFilter(SCIMFilterType.OR, null, null, false,
                          new ArrayList<SCIMFilter>(filterComponents));
  }



  /**
   * Create a new equality filter.
   *
   * @param filterAttribute  The attribute or sub-attribute to filter by.
   * @param filterValue      The filter attribute value.
   *
   * @return  A new equality filter.
   */
  public static SCIMFilter createEqualityFilter(
      final AttributePath filterAttribute, final String filterValue)
  {
    return new SCIMFilter(SCIMFilterType.EQUALITY, filterAttribute,
                          filterValue, true, null);
  }



  /**
   * Create a new contains filter.
   *
   * @param filterAttribute  The attribute or sub-attribute to filter by.
   * @param filterValue      The filter attribute value.
   *
   * @return  A new contains filter.
   */
  public static SCIMFilter createContainsFilter(
      final AttributePath filterAttribute, final String filterValue)
  {
    return new SCIMFilter(SCIMFilterType.CONTAINS, filterAttribute,
                          filterValue, true, null);
  }



  /**
   * Create a new starts with filter.
   *
   * @param filterAttribute  The attribute or sub-attribute to filter by.
   * @param filterValue      The filter attribute value.
   *
   * @return  A new starts with filter.
   */
  public static SCIMFilter createStartsWithFilter(
      final AttributePath filterAttribute, final String filterValue)
  {
    return new SCIMFilter(SCIMFilterType.STARTS_WITH, filterAttribute,
                          filterValue, true, null);
  }



  /**
   * Create a new presence filter.
   *
   * @param filterAttribute  The attribute or sub-attribute to filter by.
   *
   * @return  A new presence filter.
   */
  public static SCIMFilter createPresenceFilter(
      final AttributePath filterAttribute)
  {
    return new SCIMFilter(SCIMFilterType.PRESENCE, filterAttribute,
                          null, true, null);
  }



  /**
   * Create a new greater than filter.
   *
   * @param filterAttribute  The attribute or sub-attribute to filter by.
   * @param filterValue      The filter attribute value.
   *
   * @return  A new greater than filter.
   */
  public static SCIMFilter createGreaterThanFilter(
      final AttributePath filterAttribute, final String filterValue)
  {
    return new SCIMFilter(SCIMFilterType.GREATER_THAN, filterAttribute,
                          filterValue, true, null);
  }



  /**
   * Create a new greater or equal filter.
   *
   * @param filterAttribute  The attribute or sub-attribute to filter by.
   * @param filterValue      The filter attribute value.
   *
   * @return  A new greater or equal filter.
   */
  public static SCIMFilter createGreaterOrEqualFilter(
      final AttributePath filterAttribute, final String filterValue)
  {
    return new SCIMFilter(SCIMFilterType.GREATER_OR_EQUAL, filterAttribute,
                          filterValue, true, null);
  }



  /**
   * Create a new less than filter.
   *
   * @param filterAttribute  The attribute or sub-attribute to filter by.
   * @param filterValue      The filter attribute value.
   *
   * @return  A new less than filter.
   */
  public static SCIMFilter createLessThanFilter(
      final AttributePath filterAttribute, final String filterValue)
  {
    return new SCIMFilter(SCIMFilterType.LESS_THAN, filterAttribute,
                          filterValue, true, null);
  }



  /**
   * Create a new less or equal filter.
   *
   * @param filterAttribute  The attribute or sub-attribute to filter by.
   * @param filterValue      The filter attribute value.
   *
   * @return  A new less or equal filter.
   */
  public static SCIMFilter createLessOrEqualFilter(
      final AttributePath filterAttribute, final String filterValue)
  {
    return new SCIMFilter(SCIMFilterType.LESS_OR_EQUAL, filterAttribute,
                          filterValue, true, null);
  }



  /**
   * Retrieve the filter type.
   *
   * @return  The filter type.
   */
  public SCIMFilterType getFilterType()
  {
    return filterType;
  }



  /**
   * Retrieve the attribute or sub-attribute to filter by, or {@code null} if
   * not applicable for this filter type.
   *
   * @return  The attribute or sub-attribute to filter by
   */
  public AttributePath getFilterAttribute()
  {
    return filterAttribute;
  }



  /**
   * Retrieve the filter attribute value.
   *
   * @return  The filter attribute value, or {@code null} if not applicable
   *          for this filter type.
   */
  public String getFilterValue()
  {
    return filterValue;
  }



  /**
   * Determine whether the filter attribute value is quoted in the string
   * representation of the filter.
   *
   * @return {@code true} if the filter attribute value is quoted in the string
   *         representation of the filter.
   */
  public boolean isQuoteFilterValue()
  {
    return quoteFilterValue;
  }



  /**
   * Retrieve the filter components for an 'and' or 'or' filter.
   *
   * @return  The filter components for an 'and' or 'or' filter.
   */
  public List<SCIMFilter> getFilterComponents()
  {
    return filterComponents;
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
   * Append the string representation of the filter to the provided buffer.
   *
   * @param builder  The buffer to which the string representation of the
   *                 filter is to be appended.
   */
  public void toString(final StringBuilder builder)
  {
    switch (filterType)
    {
      case AND:
      case OR:
        builder.append('(');

        for (int i = 0; i < filterComponents.size(); i++)
        {
          if (i != 0)
          {
            builder.append(' ');
            builder.append(filterType);
            builder.append(' ');
          }

          builder.append(filterComponents.get(i));
        }

        builder.append(')');
        break;

      case EQUALITY:
      case CONTAINS:
      case STARTS_WITH:
      case GREATER_THAN:
      case GREATER_OR_EQUAL:
      case LESS_THAN:
      case LESS_OR_EQUAL:
        builder.append(filterAttribute);
        builder.append(' ');
        builder.append(filterType);
        builder.append(' ');

        if (quoteFilterValue)
        {
          try
          {
            builder.append(JSONObject.valueToString(filterValue));
          }
          catch (JSONException e)
          {
            Debug.debugException(e);
          }
        }
        else
        {
          builder.append(filterValue);
        }
        break;

      case PRESENCE:
        builder.append(filterAttribute);
        builder.append(' ');
        builder.append(filterType);
        break;
    }
  }
}
