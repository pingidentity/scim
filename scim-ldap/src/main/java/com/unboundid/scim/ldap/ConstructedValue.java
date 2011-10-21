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

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.util.StaticUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 * This class is used to construct DN and attribute values from a template value
 * that allows portions of the DN and entry attributes to be included in the
 * constructed attribute value / DN.
 * <p/>
 * This class is based on the UnboundID Sync Server implementation.
 */
class ConstructedValue
{

  // The provided template is broken up into chunks.  Each chunk is either
  // fixed text; an attribute replacement, e.g. {uid}, that is replaced with
  // the value from the source entry; or a DN component replacement that
  // substitutes a piece of the DN, e.g. {1}.
  private final List<Chunk> chunks;

  // These are the DN replacement components that appear in the value template.
  // For example, [1, 3] for a template that includes {1} and {3}.
  private final SortedSet<Integer> dnIndexReplacements;

  // These are the source attributes that appear in the value template.
  // For example, ["givenname", "sn"] for the cn={givnenname} {sn} template.
  private final SortedSet<String> entryAttrReplacements;



  /**
   * Constructs a value out of the specified template.
   *
   * @param template  The template for the constructed value.
   */
  ConstructedValue(final String template)
  {
    List<Chunk> chunkList = new ArrayList<Chunk>();

    TreeSet<String> entryAttrs = new TreeSet<String>(
        String.CASE_INSENSITIVE_ORDER);
    TreeSet<Integer> dnComponentIndexes = new TreeSet<Integer>();

    int pos = 0;
    while (pos < template.length())
    {

      int nextReplacementStart = template.indexOf('{', pos);

      int fixedTextEnd;

      if (nextReplacementStart == -1)
      {
        int nextReplacementEnd = template.indexOf('}', pos);
        if (nextReplacementEnd != -1)
        {
          throw new IllegalArgumentException(
              String.format(
                  "'%s' has mismatched or nested {} around a replacement value",
                  template));
        }

        fixedTextEnd = template.length();
      }
      else
      {
        fixedTextEnd = nextReplacementStart;
      }

      if (fixedTextEnd > pos)
      {
        chunkList.add(new FixedTextChunk(
            template.substring(pos, fixedTextEnd)));
        pos = fixedTextEnd;
      }

      if (nextReplacementStart != -1)
      {
        int nextReplacementEnd = template.indexOf('}', nextReplacementStart);
        if (nextReplacementEnd == -1)
        {
          throw new IllegalArgumentException(
              String.format(
                  "'%s' has mismatched or nested {} around a replacement value",
                  template));
        }

        String replacementStr =
            template.substring(nextReplacementStart + 1, nextReplacementEnd);

        AttributeValueChunk attrValueChunk;
        if (replacementStr.contains(":"))
        {
          attrValueChunk = new RegExAttributeValueChunk(replacementStr);
        }
        else
        {
          attrValueChunk = new AttributeValueChunk(replacementStr);
        }
        chunkList.add(attrValueChunk);

        if (attrValueChunk.isDnComponent())
        {
          dnComponentIndexes.add(attrValueChunk.getDnComponentIndex());
        }
        else
        {
          entryAttrs.add(attrValueChunk.getAttributeName());
        }

        pos = nextReplacementEnd + 1;
      }
    }

    this.entryAttrReplacements =
        Collections.unmodifiableSortedSet(entryAttrs);
    this.dnIndexReplacements =
        Collections.unmodifiableSortedSet(dnComponentIndexes);
    this.chunks = Collections.unmodifiableList(chunkList);
  }



  /**
   * Return the DN index replacements that appear in the value, e.g. [1, 3].
   *
   * @return  The DN index replacements that appear in the value.
   */
  SortedSet<Integer> getDnIndexReplacements()
  {
    return dnIndexReplacements;
  }



  /**
   * Return the source entry attribute replacements that appear in the value,
   * e.g. ["givenname", "sn"].
   *
   * @return  The source entry attribute replacements that appear in the value.
   */
  SortedSet<String> getEntryAttrReplacements()
  {
    return entryAttrReplacements;
  }



  /**
   * Construct a value from the provided DN components and entry.
   *
   * @param dnComponents  The DN components.
   * @param entry         The entry.
   *
   * @return The constructed value.
   */
  String constructValue(final List<DN> dnComponents, final Entry entry)
  {
    StringBuilder value = new StringBuilder();

    for (Chunk chunk : chunks)
    {
      value.append(chunk.getValue(dnComponents, entry));
    }

    return value.toString();
  }



  /**
   * Construct a value from the provided entry.
   *
   * @param entry         The entry.
   *
   * @return The constructed value.
   */
  String constructValue(final Entry entry)
  {
    List<DN> noDnComponents = Collections.emptyList();
    return constructValue(noDnComponents, entry);
  }



  /**
   * Represents a single "chunk" of a constructed value which can either be
   * fixed text, a replacement DN comonent, or a replacment attribute value. The
   * final two options can use regular expression replacement values.
   */
  private abstract static class Chunk
  {
    /**
     * Return the value constructed out the specified components.
     *
     * @param dnComponents  The DN components.
     * @param entry         The entry.
     *
     * @return  The value constructed out the specified components.
     */
    abstract String getValue(List<DN> dnComponents, Entry entry);
  }



  /**
   * A "chunk" in the replacement value that is fixed text.
   */
  private static final class FixedTextChunk extends Chunk
  {
    private final String fixedText;



    /**
     * Constructor for the fixed test chunk.
     *
     * @param fixedText  The fixed text.
     */
    private FixedTextChunk(final String fixedText)
    {
      this.fixedText = fixedText;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    String getValue(final List<DN> dnComponents, final Entry entry)
    {
      return fixedText;
    }
  }



  private static String ATTRIBUTE_ONLY_REGEX =
      "[a-zA-Z0-9_\\.;\\-]+(?:,[a-zA-Z0-9_\\.;\\-]+)*";

  private static Pattern ATTRIBUTE_REGEX =
      Pattern.compile("^" + ATTRIBUTE_ONLY_REGEX + "$");



  /**
   * A "chunk" in the replacement value that is a value from the source entry,
   * e.g. {givenname}.
   */
  private static class AttributeValueChunk extends Chunk
  {
    private final boolean isDnComponent;

    private final String attributeName;

    private final int dnComponentIndex;



    /**
     * Constructor.
     *
     * @param attributeName  The name of the attribute from the source entry.
     */
    AttributeValueChunk(final String attributeName)
    {
      this.attributeName = attributeName;

      Matcher matcher = ATTRIBUTE_REGEX.matcher(attributeName);

      if (!matcher.matches())
      {
        throw new IllegalArgumentException(
            String.format("'%s' is not a valid attribute name", attributeName));
      }

      if (attributeName.matches("\\d+"))
      {
        dnComponentIndex = Integer.parseInt(attributeName);
        if (dnComponentIndex == 0)
        {
          throw new IllegalArgumentException(
              String.format("'%s' is not a valid DN component replacement " +
                            "value.  To use the value of the first matching " +
                            "component, use '{1}'", attributeName));
        }
        isDnComponent = true;
      }
      else
      {
        isDnComponent = false;
        dnComponentIndex = 0;
      }
    }



    /**
     * {@inheritDoc}
     */
    @Override
    String getValue(final List<DN> dnComponents, final Entry entry)
    {
      if (isDnComponent)
      {
        if (dnComponentIndex <= dnComponents.size())
        {
          // convert from 1-based to 0-based
          return dnComponents.get(dnComponentIndex - 1).toString();
        }
        else
        {
          throw new RuntimeException(
              String.format("'%d' is not a valid DN replacement index",
                            dnComponentIndex));
        }
      }

      Attribute attribute = entry.getAttribute(attributeName);
      if (attribute == null)
      {
        throw new RuntimeException(
            String.format("Attribute '%s' does not exist in the entry so " +
                          "the value cannot be constructed", attributeName));
      }

      String[] values = attribute.getValues();
      if (values.length != 1)
      {
        throw new RuntimeException(
            String.format("Attribute '%s' has %d values in the entry so the " +
                          "value cannot be constructed since a single value " +
                          "is required", attributeName, values.length));
      }

      return values[0];
    }



    /**
     * Return true iff this value is a DN component as opposed to an attribute.
     *
     * @return  true iff this value is a DN component.
     */
    boolean isDnComponent()
    {
      return isDnComponent;
    }



    /**
     * Return the DN component index if this value is a DN component,
     * e.g. 1 for {1}.
     *
     * @return  The DN component index.
     */
    int getDnComponentIndex()
    {
      return dnComponentIndex;
    }



    /**
     * Return the attribute name if this value is not a DN component,
     * e.g. "givenname" for {givenname}.
     *
     * @return  The attribute name.
     */
    String getAttributeName()
    {
      return attributeName;
    }
  }



  // $1 matches the attribute name
  // $2 matches the regular expression to replace
  // $3 matches the replacement value of the regular expression
  // $4 matches the regex modifiers as follows
  //   g : replace globally instead of just the first one
  //   i : CASE_INSENSITIVE
  //   x : COMMENTS
  //   s : DOTALL
  //   m : MULTILINE
  //   u : UNICODE_CASE
  //   d : UNIX_LINES
  private static Pattern ATTRIBUTE_RE_AND_REPLACEMENT_REGEX =
      Pattern.compile("^(" + ATTRIBUTE_ONLY_REGEX +
                      ")\\:/([^/]+)/([^/]*)/([a-zA-Z]*)" + "$");



  /**
   * A replacement value (either DN component or source attribute) that is run
   * through a regular expression with a replacement string to construct the
   * value.
   */
  private static final class RegExAttributeValueChunk
      extends AttributeValueChunk
  {
    private final String rawAttrRegexReplacement;

    private final String rawRegexPattern;

    private final Pattern matchingPattern;

    private final String replacementString;

    private final boolean isReplaceAll;



    /**
     * Constructor for the regex replacement value.
     *
     * @param attrRegexReplacement  The regular expression replacement.
     */
    private RegExAttributeValueChunk(final String attrRegexReplacement)
    {
      super(attrRegexReplacement.split(":")[0]);

      this.rawAttrRegexReplacement = attrRegexReplacement;

      Matcher matcher = ATTRIBUTE_RE_AND_REPLACEMENT_REGEX.matcher(
          attrRegexReplacement);

      if (!matcher.matches())
      {
        throw new IllegalArgumentException(
            String.format("'%s' is an invalid constructed value template",
                          attrRegexReplacement));
      }

      String attribute = matcher.group(1);
      String regex = matcher.group(2);
      String replacement = matcher.group(3);
      String flags = matcher.group(4);

      boolean replaceAll = false;
      int patternFlags = 0;

      for (char flag : flags.toCharArray())
      {
        int flagMask = 0;
        switch (flag)
        {
          case 'i':
            flagMask = Pattern.CASE_INSENSITIVE;
            break;
          case 'x':
            flagMask = Pattern.COMMENTS;
            break;
          case 's':
            flagMask = Pattern.DOTALL;
            break;
          case 'm':
            flagMask = Pattern.MULTILINE;
            break;
          case 'u':
            flagMask = Pattern.UNICODE_CASE;
            break;
          case 'd':
            flagMask = Pattern.UNIX_LINES;
            break;
          case 'g':
            if (replaceAll)
            {
              throw new IllegalArgumentException(
                  String.format(
                      "Flag '%c' should only be specified once in '%s'",
                      flag, attrRegexReplacement));
            }
            replaceAll = true;
            break;
          default:

            throw new IllegalArgumentException(
                String.format("'%c' is not a valid flag in '%s'",
                              flag, attrRegexReplacement));
        }
        if ((patternFlags & flagMask) != 0)
        {
          throw new IllegalArgumentException(
              String.format(
                  "Flag '%c' should only be specified once in '%s'",
                  flag, attrRegexReplacement));
        }
        patternFlags |= flagMask;
      }

      try
      {
        this.matchingPattern = Pattern.compile(regex, patternFlags);
      }
      catch (Exception e)
      {
        Debug.debugException(e);
        throw new IllegalArgumentException(
            String.format("'%s' is not a valid regular expression: '%s'",
                          regex, StaticUtils.getExceptionMessage(e)), e);
      }

      this.rawRegexPattern = regex;

      this.replacementString = replacement;

      this.isReplaceAll = replaceAll;
    }



    /**
     * {@inheritDoc}
     */
    @Override
    String getValue(final List<DN> dnComponents, final Entry entry)
    {
      String baseValue = super.getValue(dnComponents, entry);

      Matcher matcher = matchingPattern.matcher(baseValue);

      try
      {
        if (isReplaceAll)
        {
          return matcher.replaceAll(replacementString);
        }
        else
        {
          return matcher.replaceFirst(replacementString);
        }
      }
      catch (Exception e)
      {
        Debug.debugException(e);
        // We might get
        throw new RuntimeException(
            String.format(
                "Could not construct a value using '%s' (%s='%s') because: %s",
                rawAttrRegexReplacement, getAttributeName(), baseValue,
                StaticUtils.getExceptionMessage(e)));
      }
    }
  }

  // TODO: it might be nice for the Chunks to be aware of the attribute that
  // we're constructing a value for.  Maybe they could be non-static inner
  // classes.
}
