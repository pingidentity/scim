/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import java.util.ArrayList;
import java.util.List;



/**
 * This class may be used to parse an HTTP 1.1 Accept Header Field, and to
 * find the media type that is the best match.
 */
public final class HttpAcceptHeader
{
  /**
   * The list of media ranges in the accept header.
   */
  private final List<HttpMediaRange> ranges;



  /**
   * Create a new accept header instance.
   *
   * @param ranges  The list of media ranges in the accept header.
   */
  public HttpAcceptHeader(final List<HttpMediaRange> ranges)
  {
    this.ranges = ranges;
  }



  /**
   * Parse an Accept Header Field as specified in RFC 2616 Section 14.1.
   *
   * @param s  The accept header field to be parsed. A {@code null} string
   *           will be considered equivalent to accepting any media type.
   *
   * @return  A new accept header instance.
   */
  public static HttpAcceptHeader parse(final String s)
  {
    final List<HttpMediaRange> myRanges = new ArrayList<HttpMediaRange>();

    if (s == null)
    {
      myRanges.add(HttpMediaRange.parse("*/*"));
    }
    else
    {
      final String[] splitRanges = s.split(",");
      for (final String splitRange : splitRanges)
      {
        myRanges.add(HttpMediaRange.parse(splitRange.trim()));
      }
    }

    return new HttpAcceptHeader(myRanges);
  }



  /**
   * Find the best match for the specified media types.
   *
   * @param mediaTypes  The set of media types that could be returned to the
   *                    client.
   *
   * @return  The media type that provides the best match, or {@code null} if
   *          there is no match.
   */
  public String findBestMatch(final String ... mediaTypes)
  {
    String bestMatch = null;
    double bestQualityFactor = 0;

    for (final String mediaType : mediaTypes)
    {
      final String[] typeAndSubtype = mediaType.split("/");

      HttpMediaRange bestRange = null;
      for (final HttpMediaRange range : ranges)
      {
        if (range.matches(typeAndSubtype[0], typeAndSubtype[1]))
        {
          if (bestRange == null || range.overrides(bestRange))
          {
            bestRange = range;
          }
        }
      }

      if (bestRange != null)
      {
        if (bestRange.getQualityFactor() > bestQualityFactor)
        {
          bestMatch = mediaType;
          bestQualityFactor = bestRange.getQualityFactor();
        }
      }
    }

    return bestMatch;
  }
}
