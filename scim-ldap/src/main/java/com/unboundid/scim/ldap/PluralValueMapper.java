/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import java.util.List;



/**
 * This class maps the sub-attributes of a SCIM plural value for a specified
 * type of value (e.g. "work" or "home").
 */
public class PluralValueMapper
{
  /**
   * The value of the "type" sub-attribute for SCIM attribute values that are
   * mapped by this plural value mapper.
   */
  private final String typeValue;

  /**
   * The value mappers for each sub-attribute.
   */
  private final List<ValueMapper> valueMappers;



  /**
   * Create a new instance of this plural value mapper.
   *
   * @param typeValue          The value of the "type" sub-attribute for SCIM
   *                           attribute values that are mapped by this
   *                           plural value mapper.
   * @param valueMappers       The value mappers for each sub-attribute.
   */
  public PluralValueMapper(final String typeValue,
                           final List<ValueMapper> valueMappers)
  {
    this.typeValue    = typeValue;
    this.valueMappers = valueMappers;
  }



  /**
   * Retrieve the value of the "type" sub-attribute for SCIM attribute values
   * that are mapped by this plural value mapper.
   *
   * @return  The value of the "type" sub-attribute for SCIM attribute values
   *          that are mapped by this plural value mapper.
   */
  public String getTypeValue()
  {
    return typeValue;
  }



  /**
   * Retrieve the value mappers for each sub-attribute.
   *
   * @return  The value mappers for each sub-attribute.
   */
  public List<ValueMapper> getValueMappers()
  {
    return valueMappers;
  }
}
