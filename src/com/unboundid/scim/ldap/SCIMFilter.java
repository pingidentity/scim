/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import static com.unboundid.scim.sdk.SCIMConstants.*;



/**
 * This class represents the filter parameters of a SCIM request.
 */
public class SCIMFilter
{
  /**
   * The filter operation. e.g. equals, contains, startswith, or present.
   */
  private final String filterOp;

  /**
   * The filter attribute value, or {@code null} if a value is not required.
   */
  private final String filterValue;

  /**
   * The schema URI of the attribute to filter by.
   */
  private final String attributeSchema;

  /**
   * The path to the attribute to filter by. e.g. userName or name.lastName
   */
  private final String[] attributePath;



  /**
   * Create a new SCIM filter from the provided information.
   *
   * @param filterOp         The filter operation. e.g. equals, contains,
   *                         startswith, or present.
   * @param filterValue      The filter attribute value, or {@code null} if a
   *                         value is not required.
   * @param attributeSchema  The schema URI of the attribute to filter by.
   * @param attributePath    The path to the attribute to filter by.
   *                         e.g. userName or name.lastName
   */
  public SCIMFilter(final String filterOp, final String filterValue,
                    final String attributeSchema,
                    final String ... attributePath)
  {
    this.filterOp        = filterOp;
    this.filterValue     = filterValue;
    this.attributeSchema = attributeSchema;
    this.attributePath   = attributePath;
  }



  /**
   * Create a new SCIM filter from the provided information.
   *
   * @param filterBy         The full path to the attribute to filter by.
   * @param filterOp         The filter operation. e.g. equals, contains,
   *                         startswith, or present.
   * @param filterValue      The filter attribute value, or {@code null} if a
   *                         value is not required.
   */
  public SCIMFilter(final String filterBy,
                    final String filterOp,
                    final String filterValue)
  {
    this.filterOp        = filterOp;
    this.filterValue     = filterValue;

    final int lastColonPos =
        filterBy.lastIndexOf(SEPARATOR_CHAR_QUALIFIED_ATTRIBUTE);
    if (lastColonPos == -1)
    {
      attributeSchema = SCHEMA_URI_CORE;
      attributePath = filterBy.split("\\.");
    }
    else
    {
      attributeSchema = filterBy.substring(0, lastColonPos);
      attributePath = filterBy.substring(lastColonPos+1).split("\\.");
    }
  }



  /**
   * Create a new case-exact equality filter.
   *
   * @param filterBy     The full path to the attribute to filter by.
   * @param filterValue  The filter attribute value.
   *
   * @return  A new case-exact equality filter.
   */
  public static SCIMFilter createEqualityFilter(
      final String filterBy, final String filterValue)
  {
    return new SCIMFilter(filterBy, "equals", filterValue);
  }



  /**
   * Create a new case-ignore equality filter.
   *
   * @param filterBy     The full path to the attribute to filter by.
   * @param filterValue  The filter attribute value.
   *
   * @return  A new case-ignore equality filter.
   */
  public static SCIMFilter createIgnoresCaseEqualityFilter(
      final String filterBy, final String filterValue)
  {
    return new SCIMFilter(filterBy, "equalsIgnoreCase", filterValue);
  }



  /**
   * Create a new 'contains' filter.
   *
   * @param filterBy     The full path to the attribute to filter by.
   * @param filterValue  The filter attribute value.
   *
   * @return  A new 'contains' filter.
   */
  public static SCIMFilter createContainsFilter(
      final String filterBy, final String filterValue)
  {
    return new SCIMFilter(filterBy, "contains", filterValue);
  }



  /**
   * Create a new 'startsWith' filter.
   *
   * @param filterBy     The full path to the attribute to filter by.
   * @param filterValue  The filter attribute value.
   *
   * @return  A new 'startsWith' filter.
   */
  public static SCIMFilter createStartsWithFilter(
      final String filterBy, final String filterValue)
  {
    return new SCIMFilter(filterBy, "startsWith", filterValue);
  }



  /**
   * Create a new presence filter.
   *
   * @param filterBy     The full path to the attribute to filter by.
   *
   * @return  A new presence filter.
   */
  public static SCIMFilter createPresenceFilter(final String filterBy)
  {
    return new SCIMFilter(filterBy, "present", null);
  }



  /**
   * Retrieve the filter operation. e.g. equals, contains, startswith, or
   * present.
   *
   * @return  The filter operation.
   */
  public String getFilterOp()
  {
    return filterOp;
  }



  /**
   * Retrieve the filter attribute value.
   *
   * @return  The filter attribute value, or {@code null} if a value is not
   *          required.
   */
  public String getFilterValue()
  {
    return filterValue;
  }



  /**
   * Retrieve the schema URI of the attribute to filter by.
   *
   * @return  The schema URI of the attribute to filter by.
   */
  public String getAttributeSchema()
  {
    return attributeSchema;
  }



  /**
   * Retrieve the path to the attribute to filter by. e.g. userName or
   * name.lastName
   *
   * @return  The path to the attribute to filter by.
   */
  public String[] getAttributePath()
  {
    return attributePath;
  }
}
