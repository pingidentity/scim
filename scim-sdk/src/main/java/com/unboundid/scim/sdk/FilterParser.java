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

import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;



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
   * Base class for expression stack nodes. The expression stack is needed to
   * employ the shunting-yard algorithm to parse the filter expression.
   */
  class Node
  {
    private final int pos;



    /**
     * Create a new node.
     *
     * @param pos  The position of the node in the filter string.
     */
    public Node(final int pos)
    {
      this.pos = pos;
    }



    /**
     * Retrieve the position of the node in the filter string.
     * @return  The position of the node in the filter string.
     */
    public int getPos()
    {
      return pos;
    }
  }



  /**
   * A node representing a filter component.
   */
  class FilterNode extends Node
  {
    private final SCIMFilter filterComponent;



    /**
     * Create a new filter component node.
     *
     * @param filterComponent  The filter component.
     * @param pos              The position of the node in the filter string.
     */
    public FilterNode(final SCIMFilter filterComponent,
                      final int pos)
    {
      super(pos);
      this.filterComponent = filterComponent;
    }



    /**
     * Retrieve the filter component.
     *
     * @return  The filter component.
     */
    public SCIMFilter getFilterComponent()
    {
      return filterComponent;
    }



    @Override
    public String toString()
    {
      return "FilterNode{" +
             "filterComponent=" + filterComponent +
             "} " + super.toString();
    }
  }



  /**
   * A node representing a logical operator.
   */
  class OperatorNode extends Node
  {
    private final SCIMFilterType filterType;

    /**
     * Create a new logical operator node.
     *
     * @param filterType   The type of operator, either SCIMFilterType.AND or
     *                     SCIMFilterType.OR.
     * @param pos          The position of the node in the filter string.
     */
    public OperatorNode(final SCIMFilterType filterType,
                        final int pos)
    {
      super(pos);
      this.filterType = filterType;
    }



    /**
     * Retrieve the type of operator.
     *
     * @return  The type of operator, either SCIMFilterType.AND or
     *          SCIMFilterType.OR.
     */
    public SCIMFilterType getFilterType()
    {
      return filterType;
    }



    /**
     * Retrieve the precedence of the operator.
     *
     * @return  The precedence of the operator.
     */
    public int getPrecedence()
    {
      switch (filterType)
      {
        case AND:
          return 2;

        case OR:
        default:
          return 1;
      }
    }



    @Override
    public String toString()
    {
      return "OperatorNode{" +
             "filterType=" + filterType +
             "} " + super.toString();
    }
  }



  /**
   * A node representing an opening parenthesis.
   */
  class LeftParenthesisNode extends Node
  {
    /**
     * Create a new opening parenthesis node.
     *
     * @param pos  The position of the parenthesis in the filter string.
     */
    public LeftParenthesisNode(final int pos)
    {
      super(pos);
    }
  }



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
   *
   * @throws  SCIMException  If the filter string could not be parsed.
   */
  public SCIMFilter parse()
      throws SCIMException
  {
    try
    {
      return readFilter();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw SCIMException.createException(
          400, MessageFormat.format("Invalid filter ''{0}'': {1}",
                                    filterString, e.getMessage()));
    }
  }



  /**
   * Read a filter component at the current position. A filter component is
   * <pre>
   * attribute attribute-operator [value]
   * </pre>
   * Most attribute operators require a value but 'pr' (presence) requires
   * no value.
   *
   * @return  The parsed filter component.
   */
  private SCIMFilter readFilterComponent()
  {
    String word = readWord();
    if (word == null)
    {
      final String msg = String.format(
          "End of input at position %d but expected a filter expression",
          markPos);
      throw new IllegalArgumentException(msg);
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

    return new SCIMFilter(
        filterType, filterAttribute,
        filterValue != null ? filterValue.toString() : null,
        (filterValue != null) && (filterValue instanceof String),
        null);
  }



  /**
   * Read a filter expression.
   *
   * @return  The SCIM filter.
   */
  private SCIMFilter readFilter()
  {
    final Stack<Node> expressionStack = new Stack<Node>();

    // Employ the shunting-yard algorithm to parse into reverse polish notation,
    // where the operands are filter components and the operators are the
    // logical AND and OR operators. This algorithm ensures that operator
    // precedence and parentheses are respected.
    final List<Node> reversePolish = new ArrayList<Node>();
    for (String word = readWord(); word != null; word = readWord())
    {
      if (word.equalsIgnoreCase("and") || word.equalsIgnoreCase("or"))
      {
        final OperatorNode currentOperator;
        if (word.equalsIgnoreCase("and"))
        {
          currentOperator = new OperatorNode(SCIMFilterType.AND, markPos);
        }
        else
        {
          currentOperator = new OperatorNode(SCIMFilterType.OR, markPos);
        }
        while (!expressionStack.empty() &&
               (expressionStack.peek() instanceof OperatorNode))
        {
          final OperatorNode previousOperator =
              (OperatorNode)expressionStack.peek();
          if (previousOperator.getPrecedence() <
              currentOperator.getPrecedence())
          {
            break;
          }
          reversePolish.add(expressionStack.pop());
        }
        expressionStack.push(currentOperator);
      }
      else if (word.equals("("))
      {
        expressionStack.push(new LeftParenthesisNode(markPos));
      }
      else if (word.equals(")"))
      {
        while (!expressionStack.empty() &&
               !(expressionStack.peek() instanceof LeftParenthesisNode))
        {
          reversePolish.add(expressionStack.pop());
        }
        if (expressionStack.empty())
        {
          final String msg =
              String.format("No opening parenthesis matching closing " +
                            "parenthesis at position %d", markPos);
          throw new IllegalArgumentException(msg);
        }
        expressionStack.pop();
      }
      else
      {
        rewind();
        final int pos = currentPos;
        final SCIMFilter filterComponent = readFilterComponent();
        reversePolish.add(new FilterNode(filterComponent, pos));
      }
    }

    while  (!expressionStack.empty())
    {
      final Node node = expressionStack.pop();
      if (node instanceof LeftParenthesisNode)
      {
        final String msg =
            String.format("No closing parenthesis matching opening " +
                          "parenthesis at position %d", node.getPos());
        throw new IllegalArgumentException(msg);
      }
      reversePolish.add(node);
    }

    // Evaluate the reverse polish notation to create a single complex filter.
    final Stack<FilterNode> filterStack = new Stack<FilterNode>();
    for (final Node node : reversePolish)
    {
      if (node instanceof OperatorNode)
      {
        final FilterNode rightOperand = filterStack.pop();
        final FilterNode leftOperand = filterStack.pop();

        final OperatorNode operatorNode = (OperatorNode)node;
        if (operatorNode.getFilterType().equals(SCIMFilterType.AND))
        {
          final SCIMFilter filter = SCIMFilter.createAndFilter(
              Arrays.asList(leftOperand.getFilterComponent(),
                            rightOperand.getFilterComponent()));
          filterStack.push(new FilterNode(filter, leftOperand.getPos()));
        }
        else
        {
          final SCIMFilter filter = SCIMFilter.createOrFilter(
              Arrays.asList(leftOperand.getFilterComponent(),
                            rightOperand.getFilterComponent()));
          filterStack.push(new FilterNode(filter, leftOperand.getPos()));
        }
      }
      else
      {
        filterStack.push((FilterNode)node);
      }
    }

    if (filterStack.size() == 0)
    {
      final String msg = String.format("Empty filter expression");
      throw new IllegalArgumentException(msg);
    }
    else if (filterStack.size() > 1)
    {
      final String msg = String.format(
          "Unexpected characters at position %d", expressionStack.get(1).pos);
      throw new IllegalArgumentException(msg);
    }

    return filterStack.get(0).filterComponent;
  }



  /**
   * Read a word at the current position. A word is a consecutive sequence of
   * characters terminated by whitespace or a parenthesis, or a single opening
   * or closing parenthesis. Whitespace before and after the word is consumed.
   * The start of the word is saved in {@code markPos}.
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
          if (currentPos == markPos)
          {
            currentPos++;
          }
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
   * boolean value (the words true or false), or a string value in double
   * quotes, using the same syntax as for JSON values. Whitespace before and
   * after the value is consumed. The start of the value is saved in
   * {@code markPos}.
   *
   * @return A Boolean, Double, Integer, Long or String representing the value
   *         at the current position, or {@code null} if the end of the input
   *         has already been reached.
   */
  public Object readValue()
  {
    skipWhitespace();
    markPos = currentPos;

    if (currentPos == endPos)
    {
      return null;
    }

    if (filterString.charAt(currentPos) == '"')
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
              case '"':
              case '/':
              case '\'':
              case '\\':
                builder.append(escapeChar);
                break;
              case 'b':
                builder.append('\b');
                break;
              case 'f':
                builder.append('\f');
                break;
              case 'n':
                builder.append('\n');
                break;
              case 'r':
                builder.append('\r');
                break;
              case 't':
                builder.append('\t');
                break;
              case 'u':
                if (currentPos + 4 > endPos)
                {
                  final String msg = String.format(
                      "End of input in a string value that began at " +
                      "position %d", markPos);
                  throw new IllegalArgumentException(msg);
                }
                final String hexChars =
                    filterString.substring(currentPos, currentPos + 4);
                builder.append((char)Integer.parseInt(hexChars, 16));
                currentPos += 4;
                break;
              default:
                final String msg = String.format(
                    "Unrecognized escape sequence '\\%c' in a string value " +
                    "at position %d", escapeChar, currentPos - 2);
                throw new IllegalArgumentException(msg);
            }
            break;

          case '"':
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
          case '(':
          case ')':
            break loop;

          case '+':
          case '-':
          case '.':
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

      final String s = filterString.substring(markPos, currentPos);
      skipWhitespace();
      final Object value = JSONObject.stringToValue(s);

      if (value.equals(JSONObject.NULL) || value instanceof String)
      {
        final String msg = String.format(
            "Invalid filter value beginning at position %d", markPos);
        throw new IllegalArgumentException(msg);
      }

      return value;
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
