/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.marshal.json;

import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.ldap.GenericResource;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.schema.Resource;
import com.unboundid.scim.schema.Response;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import org.json.JSONException;
import org.json.JSONWriter;

import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



/**
 * This class provides a SCIM object marshaller implementation to write SCIM
 * objects to their Json representation.
 */
public class JsonMarshaller implements Marshaller
{
  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final OutputStream outputStream)
      throws Exception
  {
    final OutputStreamWriter outputStreamWriter =
        new OutputStreamWriter(outputStream);
    try
    {
      marshal(o, new JSONWriter(outputStreamWriter), true);
    }
    finally
    {
      outputStreamWriter.close();
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final File file)
      throws Exception
  {
    throw new UnsupportedOperationException("marshal to file not supported");
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final Writer writer)
      throws Exception
  {
    marshal(o, new JSONWriter(writer), true);
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final Response response, final OutputStream outputStream)
      throws Exception
  {
    final OutputStreamWriter outputStreamWriter =
        new OutputStreamWriter(outputStream);
    try
    {
      this.marshal(response, new JSONWriter(outputStreamWriter));
    }
    finally
    {
      outputStreamWriter.close();
    }
  }



  /**
   * {@inheritDoc}
   */
  public void marshal(final Response response, final Writer writer)
      throws Exception
  {
    this.marshal(response, new JSONWriter(writer));
  }



  /**
   * Write a SCIM object to a JSON writer.
   *
   * @param o          The SCIM Object to be written.
   * @param jsonWriter Output to write the Object to.
   * @param includeSchemas  Indicates whether the schemas should be written
   *                        at the start of the object.
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void marshal(final SCIMObject o,
    final JSONWriter jsonWriter, final boolean includeSchemas)
      throws JSONException
  {
    jsonWriter.object();

    final Set<String> schemas = new HashSet<String>(o.getSchemas());
    if (includeSchemas)
    {
      // Write out the schemas for this object.
      jsonWriter.key(SCIMObject.SCHEMAS_ATTRIBUTE_NAME);
      jsonWriter.array();
      for (final String schema : schemas)
      {
        jsonWriter.value(schema);
      }
      jsonWriter.endArray();
    }

    // first write out core schema, then if any extensions write them
    // out in their own json object keyed by the schema name

    for (final SCIMAttribute attribute : o
        .getAttributes(SCIMConstants.SCHEMA_URI_CORE))
    {
      if (attribute.isPlural())
      {
        this.writePluralAttribute(attribute, jsonWriter);
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
        jsonWriter.key(schema);
        jsonWriter.object();
        for (SCIMAttribute attribute : o.getAttributes(schema))
        {
          if (attribute.isPlural())
          {
            this.writePluralAttribute(attribute, jsonWriter);
          }
          else
          {
            this.writeSingularAttribute(attribute, jsonWriter);
          }
        }
        jsonWriter.endObject();
      }
    }
    jsonWriter.endObject();
  }

  /**
   * Write a SCIM response to a JSON writer.
   *
   * @param response    The SCIM resource to be written.
   * @param jsonWriter  Output to write the resource to.
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void marshal(final Response response,
                       final JSONWriter jsonWriter)
    throws JSONException
  {
    final Resource resource = response.getResource();
    final Response.Resources resources = response.getResources();
    final Response.Errors errors = response.getErrors();

    // If the response is a single resource then we omit the response object.
    if (resource != null)
    {
      final GenericResource genericResource = (GenericResource) resource;
      marshal(genericResource.getScimObject(), jsonWriter, true);
    }
    else
    {
      jsonWriter.object();

      if (errors != null)
      {
        jsonWriter.key("Errors");
        jsonWriter.array();

        for (final com.unboundid.scim.schema.Error error : errors.getError())
        {
          jsonWriter.object();

          final String description = error.getDescription();
          if (description != null)
          {
            jsonWriter.key("description");
            jsonWriter.value(description);
          }

          final String code = error.getCode();
          if (code != null)
          {
            jsonWriter.key("code");
            jsonWriter.value(code);
          }

          final String uri = error.getUri();
          if (uri != null)
          {
            jsonWriter.key("uri");
            jsonWriter.value(uri);
          }

          jsonWriter.endObject();
        }

        jsonWriter.endArray();
      }
      else
      {
        if (response.getTotalResults() != null)
        {
          jsonWriter.key("totalResults");
          jsonWriter.value(response.getTotalResults());
        }

        if (response.getItemsPerPage() != null)
        {
          jsonWriter.key("itemsPerPage");
          jsonWriter.value(response.getItemsPerPage());
        }

        if (response.getStartIndex() != null)
        {
          jsonWriter.key("startIndex");
          jsonWriter.value(response.getStartIndex());
        }

        if (resources != null && !resources.getResource().isEmpty())
        {
          // Figure out what schemas are referenced by the resources.
          final Set<String> schemaURIs = new HashSet<String>();
          final List<SCIMObject> scimObjects =
              new ArrayList<SCIMObject>(resources.getResource().size());
          for (final Resource r : resources.getResource())
          {
            // Each resource is carried as a SCIMObject wrapped into a Resource
            // instance.
            if (r instanceof GenericResource)
            {
              final GenericResource genericResource = (GenericResource) r;
              final SCIMObject scimObject = genericResource.getScimObject();
              schemaURIs.addAll(scimObject.getSchemas());
              scimObjects.add(scimObject);
            }
          }

          // Write the schemas.
          jsonWriter.key(SCIMObject.SCHEMAS_ATTRIBUTE_NAME);
          jsonWriter.array();
          for (final String schemaURI : schemaURIs)
          {
            jsonWriter.value(schemaURI);
          }
          jsonWriter.endArray();

          // Write the resources.
          jsonWriter.key("Resources");
          jsonWriter.array();
          for (final SCIMObject scimObject : scimObjects)
          {
            marshal(scimObject, jsonWriter, false);
          }
          jsonWriter.endArray();
        }
      }
      jsonWriter.endObject();
    }
  }



  /**
   * Write a plural attribute to an XML stream.
   *
   * @param scimAttribute The attribute to be written.
   * @param jsonWriter    Output to write the attribute to.
   *
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void writePluralAttribute(final SCIMAttribute scimAttribute,
                                    final JSONWriter jsonWriter)
      throws JSONException
  {

    SCIMAttributeValue[] pluralValues = scimAttribute.getPluralValues();
    jsonWriter.key(scimAttribute.getName());
    jsonWriter.array();
    List<AttributeDescriptor> mappedAttributeDescriptors =
        scimAttribute.getAttributeDescriptor().getComplexAttributeDescriptors();
    for (SCIMAttributeValue pluralValue : pluralValues)
    {
      for (AttributeDescriptor attributeDescriptor :
          mappedAttributeDescriptors)
      {
        SCIMAttribute attribute =
            pluralValue.getAttribute(
                attributeDescriptor.getName());
        this.writeComplexAttribute(attribute, jsonWriter);
      }
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
    SCIMAttributeValue val = scimAttribute.getSingularValue();
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

          case INTEGER:
            jsonWriter.value(val.getLongValue());
            break;

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



  /**
   * Write a complex attribute to an XML stream.
   *
   * @param scimAttribute The attribute to be written.
   * @param jsonWriter    Output to write the attribute to.
   *
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void writeComplexAttribute(final SCIMAttribute scimAttribute,
                                     final JSONWriter jsonWriter)
      throws JSONException
  {
    SCIMAttributeValue value = scimAttribute.getSingularValue();
    Map<String, SCIMAttribute> attributes = value.getAttributes();
    jsonWriter.object();
    for (SCIMAttribute attribute : attributes.values())
    {
      writeSingularAttribute(attribute, jsonWriter);
    }
    jsonWriter.endObject();
  }
}
