/*
 * Copyright 2012 UnboundID Corp.
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

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.marshal.BulkInputStreamWrapper;
import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.BulkContentHandler;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.Status;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;



/**
 * This class is a helper class to handle parsing of JSON bulk operations.
 */
public class JsonBulkParser extends JsonParser
{
  private final BulkInputStreamWrapper bulkInputStream;
  private final BulkConfig bulkConfig;
  private final BulkContentHandler handler;
  private int operationIndex = 0;
  private JSONTokener tokener;

  /**
   * Create a new instance of this bulk unmarshaller.
   *
   * @param inputStream  The input stream containing the bulk content to be
   *                     read.
   * @param bulkConfig   The bulk configuration settings to be enforced.
   * @param handler      A bulk operation listener to handle the content as it
   *                     is read.
   */
  public JsonBulkParser(final InputStream inputStream,
                        final BulkConfig bulkConfig,
                        final BulkContentHandler handler)
  {
    this.bulkInputStream = new BulkInputStreamWrapper(inputStream);
    this.bulkConfig      = bulkConfig;
    this.handler         = handler;
    this.operationIndex = 0;
  }



  /**
   * Reads a SCIM bulk request or response from the input stream.
   *
   * @throws SCIMException If the bulk content could not be read.
   */
  public void unmarshal()
      throws SCIMException
  {
    try
    {
      tokener = new JSONTokener(bulkInputStream);

      if (tokener.nextClean() != '{')
      {
          throw tokener.syntaxError("A JSONObject text must begin with '{'");
      }
      for (;;)
      {
        String key;
        char c = tokener.nextClean();
        switch (c)
        {
          case 0:
            throw tokener.syntaxError("A JSONObject text must end with '}'");
          case '}':
            return;
          default:
            tokener.back();
            key = tokener.nextValue().toString();
        }

        // The key is followed by ':'. We will also tolerate '=' or '=>'.

        c = tokener.nextClean();
        if (c == '=')
        {
          if (tokener.next() != '>')
          {
            tokener.back();
          }
        } else if (c != ':')
        {
          throw tokener.syntaxError("Expected a ':' after a key");
        }

        if (key.equals("failOnErrors"))
        {
          handler.handleFailOnErrors((Integer)tokener.nextValue());
        }
        else if (key.equals("Operations"))
        {
          parseOperations();
        }
        else
        {
          // Skip.
          tokener.nextValue();
        }

        // Pairs are separated by ','. We will also tolerate ';'.

        switch (tokener.nextClean())
        {
          case ';':
          case ',':
            if (tokener.nextClean() == '}')
            {
              return;
            }
            tokener.back();
            break;
          case '}':
            return;
          default:
            throw tokener.syntaxError("Expected a ',' or '}'");
        }
      }
    }
    catch (JSONException e)
    {
      throw new InvalidResourceException(
          "Error while reading JSON Bulk content: " + e.getMessage(), e);
    }
  }



  /**
   * Parse the Operations element.
   *
   * @throws JSONException  If the JSON could not be parsed.
   * @throws SCIMException  If some other error occurred.
   */
  private void parseOperations()
      throws JSONException, SCIMException
  {
    if (tokener.nextClean() != '[')
    {
        throw this.tokener.syntaxError("A JSONArray text must start with '['");
    }
    if (tokener.nextClean() != ']')
    {
      tokener.back();
      for (;;)
      {
        if (tokener.nextClean() != ',')
        {
          tokener.back();
          if (operationIndex >= bulkConfig.getMaxOperations())
          {
            throw SCIMException.createException(
                413,
                "The number of operations in the bulk operation exceeds " +
                "maxOperations (" + bulkConfig.getMaxOperations() + ")");
          }

          if (bulkInputStream.getBytesRead() > bulkConfig.getMaxPayloadSize())
          {
            throw SCIMException.createException(
                413,
                "The size of the bulk operation exceeds the maxPayloadSize " +
                "(" + bulkConfig.getMaxPayloadSize() + ")");
          }
          final BulkOperation bulkOperation =
              parseBulkOperation((JSONObject)tokener.nextValue());
          handler.handleOperation(operationIndex, bulkOperation);
          operationIndex++;
        }

        switch (tokener.nextClean())
        {
          case ';':
          case ',':
            if (tokener.nextClean() == ']')
            {
              return;
            }
            tokener.back();
            break;
          case ']':
            return;
          default:
            throw tokener.syntaxError("Expected a ',' or ']'");
        }
      }
    }
  }



  /**
   * Parse an individual operation in a bulk operation request or response.
   *
   * @param o            The JSON object representing the operation.
   *
   * @return  The parsed bulk operation.
   *
   * @throws JSONException  If the operation cannot be parsed due to a JSON
   *                        error.
   * @throws SCIMException  If the operation cannot be parsed for some other
   *                        reason.
   */
  private BulkOperation parseBulkOperation(
      final JSONObject o)
  throws JSONException, SCIMException
  {
    final String httpMethod = o.getString("method");
    final String bulkId = o.optString("bulkId", null);
    final String version = o.optString("version", null);
    final String path = o.optString("path", null);
    final String location = o.optString("location", null);
    final JSONObject data = o.optJSONObject("data");
    final JSONObject statusObject = o.optJSONObject("status");

    final Status status;
    if (statusObject != null)
    {
      final String code = statusObject.getString("code");
      final String description = statusObject.optString("description", null);
      status = new Status(code, description);
    }
    else
    {
      status = null;
    }

    final BulkOperation.Method method;
    try
    {
      method = BulkOperation.Method.valueOf(httpMethod);
    }
    catch (IllegalArgumentException e)
    {
      throw new InvalidResourceException(
          "Bulk operation " + operationIndex + " specifies an invalid HTTP " +
          "method '" + httpMethod);
    }

    String endpoint = null;
    if (path != null)
    {
      int startPos = 0;
      if (path.charAt(startPos) == '/')
      {
        startPos++;
      }

      int endPos = path.indexOf('/', startPos);
      if (endPos == -1)
      {
        endPos = path.length();
      }

      endpoint = path.substring(startPos, endPos);
    }

    BaseResource resource = null;
    if (data != null)
    {
      if (path == null)
      {
        throw new InvalidResourceException(
            "Bulk operation " + operationIndex + " has data but no path");
      }

      final ResourceDescriptor descriptor =
          handler.getResourceDescriptor(endpoint);
      if (descriptor == null)
      {
        throw new InvalidResourceException(
            "Bulk operation " + operationIndex + " specifies an unknown " +
            "resource endpoint '" + endpoint + "'");
      }

      resource = unmarshal(data, descriptor,
                           BaseResource.BASE_RESOURCE_FACTORY, null);
    }

    return new BulkOperation(method, bulkId, version, path, location, resource,
                             status);
  }



  /**
   * Parse a simple attribute from its representation as a JSON Object.
   *
   * @param jsonAttribute       The JSON object representing the attribute.
   * @param attributeDescriptor The attribute descriptor.
   *
   * @return The parsed attribute.
   */
  protected SCIMAttribute createSimpleAttribute(
      final Object jsonAttribute,
      final AttributeDescriptor attributeDescriptor)
  {
    final String v =
        handler.transformValue(operationIndex, jsonAttribute.toString());

    return SCIMAttribute.create(attributeDescriptor,
                                SCIMAttributeValue.createStringValue(v));
  }
}
