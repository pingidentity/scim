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

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.marshal.StreamMarshaller;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.ServerErrorException;
import org.json.JSONException;
import org.json.JSONWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;



/**
 * This class provides a SCIM object marshaller implementation to write a
 * stream of SCIM objects to their JSON representation.
 */
public class JsonStreamMarshaller implements StreamMarshaller
{
  private final OutputStreamWriter outputStreamWriter;
  private final JSONWriter jsonWriter;



  /**
   * Create a JSON marshaller that writes to the given output stream.
   * The resulting marshaller must be closed after use.
   *
   * @param outputStream  The ouput stream to write to.
   *
   * @throws SCIMException  If the marshaller could not be created.
   */
  public JsonStreamMarshaller(final OutputStream outputStream)
      throws SCIMException
  {
    try
    {
      outputStreamWriter = new OutputStreamWriter(outputStream);
      jsonWriter = new JSONWriter(outputStreamWriter);
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot create JSON marshaller: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void close()
      throws SCIMException
  {
    try
    {
      outputStreamWriter.close();
    }
    catch (IOException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot close marshaller: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final BaseResource resource)
      throws SCIMException
  {
    try
    {
      marshal(resource, true);
    }
    catch (JSONException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write resource: " + e.getMessage());
    }
  }



  /**
   * Write a SCIM resource to a JSON writer.
   *
   * @param resource   The SCIM resource to be written.
   * @param includeSchemas  Indicates whether the schemas should be written
   *                        at the start of the object.
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void marshal(final BaseResource resource,
                       final boolean includeSchemas)
      throws JSONException
  {
    jsonWriter.object();

    final Set<String> schemas = new HashSet<String>(
        resource.getResourceDescriptor().getAttributeSchemas());
    if (includeSchemas)
    {
      // Write out the schemas for this object.
      jsonWriter.key(SCIMConstants.SCHEMAS_ATTRIBUTE_NAME);
      jsonWriter.array();
      for (final String schema : schemas)
      {
        jsonWriter.value(schema);
      }
      jsonWriter.endArray();
    }

    // first write out core schema, then if any extensions write them
    // out in their own json object keyed by the schema name

    for (final SCIMAttribute attribute : resource.getScimObject()
        .getAttributes(SCIMConstants.SCHEMA_URI_CORE))
    {
      if (attribute.getAttributeDescriptor().isMultiValued())
      {
        this.writeMultiValuedAttribute(attribute, jsonWriter);
      }
      else
      {
        this.writeSingularAttribute(attribute, jsonWriter);
      }
    }

    // write out any custom schemas
    for (final String schema : schemas)
    {
      if (!schema.equalsIgnoreCase(SCIMConstants.SCHEMA_URI_CORE))
      {
        Collection<SCIMAttribute> attributes =
            resource.getScimObject().getAttributes(schema);
        if(!attributes.isEmpty())
        {
          jsonWriter.key(schema);
          jsonWriter.object();
          for (SCIMAttribute attribute : attributes)
          {
            if (attribute.getAttributeDescriptor().isMultiValued())
            {
              this.writeMultiValuedAttribute(attribute, jsonWriter);
            }
            else
            {
              this.writeSingularAttribute(attribute, jsonWriter);
            }
          }
          jsonWriter.endObject();
        }
      }
    }
    jsonWriter.endObject();
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final Resources<? extends BaseResource> response)
      throws SCIMException
  {
    try
    {
      jsonWriter.object();
      jsonWriter.key("totalResults");
      jsonWriter.value(response.getTotalResults());

      jsonWriter.key("itemsPerPage");
      jsonWriter.value(response.getItemsPerPage());

      jsonWriter.key("startIndex");
      jsonWriter.value(response.getStartIndex());

      // Figure out what schemas are referenced by the resources.
      final Set<String> schemaURIs = new HashSet<String>();
      for (final BaseResource resource : response)
      {
        schemaURIs.addAll(
            resource.getResourceDescriptor().getAttributeSchemas());
      }

      // Write the schemas.
      jsonWriter.key(SCIMConstants.SCHEMAS_ATTRIBUTE_NAME);
      jsonWriter.array();
      for (final String schemaURI : schemaURIs)
      {
        jsonWriter.value(schemaURI);
      }
      jsonWriter.endArray();

      // Write the resources.
      jsonWriter.key("Resources");
      jsonWriter.array();
      for (final BaseResource resource : response)
      {
        marshal(resource, false);
      }
      jsonWriter.endArray();

      jsonWriter.endObject();
    }
    catch (JSONException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write resources response: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMException response)
      throws SCIMException
  {
    try
    {
      jsonWriter.object();
      jsonWriter.key("Errors");
      jsonWriter.array();

      jsonWriter.object();

      jsonWriter.key("code");
      jsonWriter.value(String.valueOf(response.getStatusCode()));

      final String description = response.getMessage();
      if (description != null)
      {
        jsonWriter.key("description");
        jsonWriter.value(description);
      }

      jsonWriter.endObject();

      jsonWriter.endArray();

      jsonWriter.endObject();
    }
    catch (JSONException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write error response: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void writeBulkStart(final int failOnErrors,
                             final Set<String> schemaURIs)
      throws SCIMException
  {
    try
    {
      jsonWriter.object();

      if (failOnErrors >= 0)
      {
        jsonWriter.key("failOnErrors");
        jsonWriter.value(failOnErrors);
      }

      // Write the schemas.
      jsonWriter.key(SCIMConstants.SCHEMAS_ATTRIBUTE_NAME);
      jsonWriter.array();
      for (final String schemaURI : schemaURIs)
      {
        jsonWriter.value(schemaURI);
      }
      jsonWriter.endArray();

      // Write the operations.
      jsonWriter.key("Operations");
      jsonWriter.array();
    }
    catch (JSONException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write start of bulk operations: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void writeBulkOperation(final BulkOperation o)
      throws SCIMException
  {
    try
    {
      jsonWriter.object();
      if (o.getMethod() != null)
      {
        jsonWriter.key("method");
        jsonWriter.value(o.getMethod().toString());
      }
      if (o.getBulkId() != null)
      {
        jsonWriter.key("bulkId");
        jsonWriter.value(o.getBulkId());
      }
      if (o.getVersion() != null)
      {
        jsonWriter.key("version");
        jsonWriter.value(o.getVersion());
      }
      if (o.getPath() != null)
      {
        jsonWriter.key("path");
        jsonWriter.value(o.getPath());
      }
      if (o.getLocation() != null)
      {
        jsonWriter.key("location");
        jsonWriter.value(o.getLocation());
      }
      if (o.getData() != null)
      {
        jsonWriter.key("data");
        marshal(o.getData(), true);
      }
      if (o.getStatus() != null)
      {
        jsonWriter.key("status");
        jsonWriter.object();
        jsonWriter.key("code");
        jsonWriter.value(o.getStatus().getCode());
        if (o.getStatus().getDescription() != null)
        {
          jsonWriter.key("description");
          jsonWriter.value(o.getStatus().getDescription());
        }
        jsonWriter.endObject();
      }
      jsonWriter.endObject();
    }
    catch (JSONException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write bulk operation: " + e.getMessage());
    }
  }


  /**
   * {@inheritDoc}
   */
  public void writeBulkFinish()
      throws SCIMException
  {
    try
    {
      jsonWriter.endArray();
      jsonWriter.endObject();
    }
    catch (JSONException e)
    {
      Debug.debugException(e);
      throw new ServerErrorException(
          "Cannot write end of bulk operations: " + e.getMessage());
    }
  }



  /**
   * {@inheritDoc}
   */
  public void bulkMarshal(final int failOnErrors,
                          final List<BulkOperation> operations)
      throws SCIMException
  {
    // Figure out what schemas are referenced by the resources.
    final Set<String> schemaURIs = new HashSet<String>();
    for (final BulkOperation o : operations)
    {
      final BaseResource resource = o.getData();
      if (resource != null)
      {
        schemaURIs.addAll(
            o.getData().getResourceDescriptor().getAttributeSchemas());
      }
    }

    writeBulkStart(failOnErrors, schemaURIs);
    for (final BulkOperation o : operations)
    {
      writeBulkOperation(o);
    }
    writeBulkFinish();
  }



  /**
   * Write a multi-valued attribute to an XML stream.
   *
   * @param scimAttribute The attribute to be written.
   * @param jsonWriter    Output to write the attribute to.
   *
   * @throws JSONException Thrown if error writing to output.
   */
  private void writeMultiValuedAttribute(final SCIMAttribute scimAttribute,
                                         final JSONWriter jsonWriter)
      throws JSONException
  {

    SCIMAttributeValue[] values = scimAttribute.getValues();
    jsonWriter.key(scimAttribute.getName());
    jsonWriter.array();
    for (SCIMAttributeValue value : values)
    {
      jsonWriter.object();
      for (SCIMAttribute attribute : value.getAttributes().values())
      {
        if (attribute.getAttributeDescriptor().isMultiValued())
        {
          this.writeMultiValuedAttribute(attribute, jsonWriter);
        }
        else
        {
          this.writeSingularAttribute(attribute, jsonWriter);
        }
      }
      jsonWriter.endObject();
    }
    jsonWriter.endArray();
  }



  /**
   * Write a singular attribute to an XML stream.
   *
   * @param scimAttribute The attribute to be written.
   * @param jsonWriter    Output to write the attribute to.
   *
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void writeSingularAttribute(final SCIMAttribute scimAttribute,
                                      final JSONWriter jsonWriter)
      throws JSONException
  {
    jsonWriter.key(scimAttribute.getName());
    SCIMAttributeValue val = scimAttribute.getValue();
    if (val.isComplex())
    {
      jsonWriter.object();
      for (SCIMAttribute a : val.getAttributes().values())
      {
        this.writeSingularAttribute(a, jsonWriter);
      }
      jsonWriter.endObject();
    }
    else
    {
      if (scimAttribute.getAttributeDescriptor().getDataType() != null)
      {
        switch (scimAttribute.getAttributeDescriptor().getDataType())
        {
          case BOOLEAN:
            jsonWriter.value(val.getBooleanValue());
            break;

          case DECIMAL:
            jsonWriter.value(val.getDecimalValue());
            break;

          case INTEGER:
            jsonWriter.value(val.getIntegerValue());
            break;

          case BINARY:
          case DATETIME:
          case STRING:
          default:
            jsonWriter.value(val.getStringValue());
            break;
        }
      }
      else
      {
        jsonWriter.value(val.getStringValue());
      }
    }
  }
}
