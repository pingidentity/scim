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

package com.unboundid.scim.sdk;

import java.util.Arrays;



/**
 * A parser for SCIM filter expressions.
 */
public class FilterParser
{
  /**
   * The filter to be parsed.
   */
  private final String filterString;

  /**
   * The position one higher than the last character.
   */
  private int endPos;

  /**
   * The current character position.
   */
  private int currentPos;

  /**
   * The position marking the first character of the previous word or value.
   */
  private int markPos;



  /**
   * Create a new instance of a filter parser.
   *
   * @param filterString  The filter to be parsed.
   */
  public FilterParser(final String filterString)
  {
    this.filterString = filterString;
    this.endPos = filterString.length();
    this.currentPos = 0;
    this.markPos = 0;
  }



  /**
   * Parse the filter provided in the constructor.
   *
   * @return  A parsed SCIM filter.
   */
  public SCIMFilter parse()
  {
    return readFilter();
  }



  /**
   * Read a filter component at the current position.
   *
   * @return  The filter component.
   */
  private SCIMFilter readFilter()
  {
    String word = readWord();
    if (word == null)
    {
      final String msg = String.format(
          "End of input at position %d but expected a filter expression",
          markPos);
      throw new IllegalArgumentException(msg);
    }

    if (word.equals("("))
    {
      final int openPos = markPos;
      final SCIMFilter filter = readFilter();

      final String closeParen = readWord();
      if (closeParen == null || !closeParen.equals(")"))
      {
        final String msg = String.format(
            "Expected closing parenthesis at position %d to match " +
            "opening parenthesis at position %d", currentPos, openPos);
        throw new IllegalArgumentException(msg);
      }

      if (!endOfInput())
      {
        final String msg = String.format(
            "Unexpected additional characters at position %d", currentPos);
        throw new IllegalArgumentException(msg);
      }

      return filter;
    }

    final AttributePath filterAttribute;
    try
    {
      filterAttribute = AttributePath.parse(word);
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      final String msg = String.format(
          "Expected an attribute reference at position %d: %s",
          markPos, e.getMessage());
      throw new IllegalArgumentException(msg);
    }

    final String operator = readWord();
    if (operator == null)
    {
      final String msg = String.format(
          "End of input at position %d but expected an attribute operator",
          markPos);
      throw new IllegalArgumentException(msg);
    }

    final SCIMFilterType filterType;
    if (operator.equalsIgnoreCase("eq"))
    {
      filterType = SCIMFilterType.EQUALITY;
    }
    else if (operator.equalsIgnoreCase("co"))
    {
      filterType = SCIMFilterType.CONTAINS;
    }
    else if (operator.equalsIgnoreCase("sw"))
    {
      filterType = SCIMFilterType.STARTS_WITH;
    }
    else if (operator.equalsIgnoreCase("pr"))
    {
      filterType = SCIMFilterType.PRESENCE;
    }
    else if (operator.equalsIgnoreCase("gt"))
    {
      filterType = SCIMFilterType.GREATER_THAN;
    }
    else if (operator.equalsIgnoreCase("ge"))
    {
      filterType = SCIMFilterType.GREATER_OR_EQUAL;
    }
    else if (operator.equalsIgnoreCase("lt"))
    {
      filterType = SCIMFilterType.LESS_THAN;
    }
    else if (operator.equalsIgnoreCase("le"))
    {
      filterType = SCIMFilterType.LESS_OR_EQUAL;
    }
    else
    {
      final String msg = String.format(
          "Unrecognized attribute operator '%s' at position %d. " +
          "Expected: eq,co,sw,pr,gt,ge,lt,le", operator, markPos);
      throw new IllegalArgumentException(msg);
    }

    final Object filterValue;
    if (!filterType.equals(SCIMFilterType.PRESENCE))
    {
      filterValue = readValue();
      if (filterValue == null)
      {
        final String msg = String.format(
            "End of input at position %d while expecting a value for " +
            "operator %s", markPos, operator);
        throw new IllegalArgumentException(msg);
      }
    }
    else
    {
      filterValue = null;
    }

    final SCIMFilter filter =
        new SCIMFilter(filterType, filterAttribute,
                       filterValue != null ? filterValue.toString() : null,
                       (filterValue != null) && (filterValue instanceof String),
                       null);

    final String nextWord = readWord();
    if (nextWord != null)
    {
      if (nextWord.equalsIgnoreCase("and"))
      {
        final SCIMFilter filter2 = readFilter();
        return SCIMFilter.createAndFilter(Arrays.asList(filter, filter2));
      }
      else if (nextWord.equalsIgnoreCase("or"))
      {
        final SCIMFilter filter2 = readFilter();
        return SCIMFilter.createOrFilter(Arrays.asList(filter, filter2));
      }
      else if (nextWord.equals(")"))
      {
        rewind();
      }
      else
      {
        final String msg = String.format(
            "Unexpected characters '%s' at position %d", nextWord, markPos);
        throw new IllegalArgumentException(msg);
      }
    }

    return filter;
  }



  /**
   * Read a word at the current position. A word is a consecutive sequence of
   * non-space characters, or an opening or closing parenthesis. Whitespace
   * before and after the word is consumed. The start of the word is saved
   * in {@code markPos}.
   *
   * @return The word at the current position, or {@code null} if the end of
   *         the input has been reached.
   */
  private String readWord()
  {
    skipWhitespace();
    markPos = currentPos;

    loop:
    while (currentPos < endPos)
    {
      final char c = filterString.charAt(currentPos);
      switch (c)
      {
        case '(':
        case ')':
          currentPos++;
          break loop;

        case ' ':
          break loop;

        default:
          currentPos++;
          break;
      }
    }

    if (currentPos - markPos == 0)
    {
      return null;
    }

    final String word = filterString.substring(markPos, currentPos);

    skipWhitespace();
    return word;
  }



  /**
   * Rewind the current position to the start of the previous word or value.
   */
  private void rewind()
  {
    currentPos = markPos;
  }



  /**
   * Read a value at the current position. A value can be a number, or a
   * boolean value (the words true or false), or a string value in single
   * quotes. Whitespace before and after the value is consumed. The start of
   * the value is saved in {@code markPos}.
   *
   * @return The value at the current position, or {@code null} if the end of
   *         the input has been reached.
   */
  private Object readValue()
  {
    skipWhitespace();
    markPos = currentPos;

    if (currentPos == endPos)
    {
      return null;
    }

    if (filterString.charAt(currentPos) == '\'')
    {
      currentPos++;

      final StringBuilder builder = new StringBuilder();
      while (currentPos < endPos)
      {
        final char c = filterString.charAt(currentPos);
        switch (c)
        {
          case '\\':
            currentPos++;
            if (endOfInput())
            {
              final String msg = String.format(
                  "End of input in a string value that began at " +
                  "position %d", markPos);
              throw new IllegalArgumentException(msg);
            }
            final char escapeChar = filterString.charAt(currentPos);
            currentPos++;
            switch (escapeChar)
            {
              case '\'':
              case '\\':
                builder.append(escapeChar);
                break;
              case 'n':
                builder.append('\n');
                break;
              case 't':
                builder.append('\t');
                break;
              default:
                final String msg = String.format(
                    "Unrecognized escape sequence '\\%c' in a string value " +
                    "at position %d", escapeChar, currentPos - 2);
                throw new IllegalArgumentException(msg);
            }
            break;

          case '\'':
            currentPos++;
            skipWhitespace();
            return builder.toString();

          default:
            builder.append(c);
            currentPos++;
            break;
        }
      }

      final String msg = String.format(
          "End of input in a string value that began at " +
          "position %d", markPos);
      throw new IllegalArgumentException(msg);
    }
    else
    {
      loop:
      while (currentPos < endPos)
      {
        final char c = filterString.charAt(currentPos);
        switch (c)
        {
          case ' ':
            break loop;

          case '-':
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
          case 'A':
          case 'B':
          case 'C':
          case 'D':
          case 'E':
          case 'F':
          case 'G':
          case 'H':
          case 'I':
          case 'J':
          case 'K':
          case 'L':
          case 'M':
          case 'N':
          case 'O':
          case 'P':
          case 'Q':
          case 'R':
          case 'S':
          case 'T':
          case 'U':
          case 'V':
          case 'W':
          case 'X':
          case 'Y':
          case 'Z':
          case 'a':
          case 'b':
          case 'c':
          case 'd':
          case 'e':
          case 'f':
          case 'g':
          case 'h':
          case 'i':
          case 'j':
          case 'k':
          case 'l':
          case 'm':
          case 'n':
          case 'o':
          case 'p':
          case 'q':
          case 'r':
          case 's':
          case 't':
          case 'u':
          case 'v':
          case 'w':
          case 'x':
          case 'y':
          case 'z':
            // These are all OK.
            currentPos++;
            break;

          case '.':
          case '/':
          case ':':
          case ';':
          case '<':
          case '=':
          case '>':
          case '?':
          case '@':
          case '[':
          case '\\':
          case ']':
          case '^':
          case '_':
          case '`':
            // These are not allowed, but they are explicitly called out because
            // they are included in the range of values between '-' and 'z', and
            // making sure all possible characters are included can help make
            // the switch statement more efficient.  We'll fall through to the
            // default clause to reject them.
          default:
            final String msg = String.format(
                "Invalid character '%c' in a number or boolean value at " +
                "position %d",
                c, currentPos);
            throw new IllegalArgumentException(msg);
        }
      }

      final String value = filterString.substring(markPos, currentPos);

      skipWhitespace();

      if (value.equalsIgnoreCase("true"))
      {
        return Boolean.TRUE;
      }
      else if (value.equalsIgnoreCase("false"))
      {
        return Boolean.FALSE;
      }
      else
      {
        try
        {
          return Long.valueOf(value);
        }
        catch (NumberFormatException e)
        {
          Debug.debugException(e);
          final String msg = String.format(
              "Invalid filter value '%s' at position %d. Expected a string " +
              "in quotes, a number or a boolean value (true,false)",
              value, markPos);
          throw new IllegalArgumentException(msg);
        }
      }
    }
  }



  /**
   * Determine if the end of the input has been reached.
   *
   * @return  {@code true} if the end of the input has been reached.
   */
  private boolean endOfInput()
  {
    return currentPos == endPos;
  }



  /**
   * Skip over any whitespace at the current position.
   */
  private void skipWhitespace()
  {
    while (currentPos < endPos && filterString.charAt(currentPos) == ' ')
    {
      currentPos++;
    }
  }
}
