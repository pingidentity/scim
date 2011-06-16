/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import java.util.LinkedHashMap;



/**
 * This class may be used to parse an HTTP 1.1 media range, as used in the
 * Accept header field.
 */
public final class HttpMediaRange
{
  /**
   * The media type, or "*" for any.
   */
  private final String type;

  /**
   * The media subtype, or "*" for any.
   */
  private final String subtype;

  /**
   * The list of media type parameters applicable to the range.
   */
  private final LinkedHashMap<String,String> parameters;

  /**
   * The quality factor, a floating point value between 0 and 1.
   */
  private final double qualityFactor;

  /**
   * The list of accept-extensions.
   */
  private final LinkedHashMap<String,String> extensions;

  /**
   * Indicates whether this media range includes all media types.
   */
  private final boolean isAnyType;

  /**
   * Indicates whether this media range includes all subtypes of a media type.
   */
  private final boolean isAnySubtype;

  /**
   * An integer indicating how specific this range is. Lower numbers have
   * higher precedence.
   */
  private final int precedenceLevel;


  /**
   * Create a new media range instance form the provided information.
   *
   * @param type           The media type, or "*" for any.
   * @param subtype        The media subtype, or "*" for any.
   * @param parameters     The list of media type parameters applicable to the
   *                       range.
   * @param qualityFactor  The quality factor, a floating point value between 0
   *                       and 1.
   * @param extensions     The list of accept-extensions.
   */
  private HttpMediaRange(final String type, final String subtype,
                         final LinkedHashMap<String,String> parameters,
                         final double qualityFactor,
                         final LinkedHashMap<String,String> extensions)
  {
    this.type          = type;
    this.subtype       = subtype;
    this.parameters    = parameters;
    this.qualityFactor = qualityFactor;
    this.extensions    = extensions;

    if (subtype.equals("*"))
    {
      isAnySubtype = true;
      isAnyType    = type.equals("*");
    }
    else
    {
      isAnySubtype = false;
      isAnyType    = false;
    }

    if (isAnyType)
    {
      precedenceLevel = 4;
    }
    else if (isAnySubtype)
    {
      precedenceLevel = 3;
    }
    else if (parameters.isEmpty())
    {
      precedenceLevel = 2;
    }
    else
    {
      precedenceLevel = 1;
    }
  }



  /**
   * Parse a media-range as specified in RFC 2616 Section 14.1.
   *
   * @param s  The media range to be parsed.
   *
   * @return  A media range.
   */
  public static HttpMediaRange parse(final String s)
  {
    final String[] parameters = s.split(";");
    final String[] typeAndSubtype = parameters[0].trim().split("/");
    final String myType = typeAndSubtype[0];
    final String mySubtype;
    if (typeAndSubtype.length < 2)
    {
      mySubtype = "*";
    }
    else
    {
      mySubtype = typeAndSubtype[1];
    }

    double myQualityFactor = 1;

    final LinkedHashMap<String,String> myParameters =
        new LinkedHashMap<String, String>();
    int i = 1;
    while (i < parameters.length)
    {
      final String[] tokens = parameters[i].trim().split("=");
      final String attribute = tokens[0].trim();
      final String value;
      if (tokens.length > 1)
      {
        value = tokens[1].trim();
      }
      else
      {
        value = "";
      }

      i++;

      if (attribute.equalsIgnoreCase("q"))
      {
        myQualityFactor = Double.valueOf(value);
        break;
      }

      myParameters.put(attribute, value);
    }

    final LinkedHashMap<String,String> myExtensions =
        new LinkedHashMap<String, String>();

    while (i < parameters.length)
    {
      final String[] tokens = parameters[i].trim().split("=");
      final String attribute = tokens[0].trim();
      final String value;
      if (tokens.length > 1)
      {
        value = tokens[1].trim();
      }
      else
      {
        value = "";
      }

      i++;

      myExtensions.put(attribute, value);
    }

    return new HttpMediaRange(myType, mySubtype, myParameters,
                              myQualityFactor, myExtensions);
  }



  /**
   * Retrieve the quality factor.
   *
   * @return  The quality factor, a floating point value between 0 and 1.
   */
  public double getQualityFactor()
  {
    return qualityFactor;
  }



  /**
   * Determine whether the specified media type matches this media range.
   *
   * @param type     The media type.
   * @param subtype  The media subtype.
   *
   * @return {@code true} if the specified media type matches this media range,
   *         or {@code false} otherwise.
   */
  public boolean matches(final String type, final String subtype)
  {
    if (isAnyType)
    {
      return true;
    }

    if (!this.type.equalsIgnoreCase(type))
    {
      return false;
    }

    if (isAnySubtype)
    {
      return true;
    }

    return this.subtype.equalsIgnoreCase(subtype);
  }



  /**
   * Determine whether this media range overrides the specified media range
   * because this media range is more specific.
   *
   * @param range  The media range for which to make the determination.
   *
   * @return {@code true} if this media range overrides the specified media
   *         range, or {@code false} otherwise.
   */
  public boolean overrides(final HttpMediaRange range)
  {
    return precedenceLevel() < range.precedenceLevel();
  }



  /**
   * Returns an integer indicating how specific this range is. Lower numbers
   * have higher precedence.
   *
   * <ol>
   * <li>
   * Specific media type with parameters : text/html;level=1
   * </li>
   * <li>
   * Specific media type without parameters : text/html
   * </li>
   * <li>
   * All subtypes of a media type : text/*
   * </li>
   * <li>
   * All media types
   * </li>
   * </ol>
   *
   * @return  An integer indicating how specific this media range is. Lower
   *          numbers have higher precedence.
   */
  private int precedenceLevel()
  {
    return precedenceLevel;
  }
}
