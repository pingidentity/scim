/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshall.json;

import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.marshall.Marshaller;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This class provides a SCIM object marshaller implementation to write SCIM
 * objects to their Json representation.
 */
public class JsonMarshaller implements Marshaller {
  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final OutputStream outputStream)
    throws Exception {
    this.marshal(o, new JSONWriter(new OutputStreamWriter(outputStream)));
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final File file)
    throws Exception {
  }

  /**
   * {@inheritDoc}
   */
  public void marshal(final SCIMObject o, final Writer writer)
    throws Exception {
    this.marshal(o, new JSONWriter(writer));
  }

  /**
   * Write a SCIM object to an XML stream.
   *
   * @param o          The SCIM Object to be written.
   * @param jsonWriter Output to write the Object to.
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void marshal(final SCIMObject o,
    final JSONWriter jsonWriter) throws JSONException {
    jsonWriter.object();

    // write out the schemas for this object
    Collection<String> schemas = o.getSchemas();
    jsonWriter.key(SCIMObject.SCHEMAS_ATTRIBUTE_NAME);
    jsonWriter.array();
    for (String schema : schemas) {
      jsonWriter.object().key(SCIMObject.SCHEMAS_ATTRIBUTE_URI_NAME)
        .value(schema);
      jsonWriter.endObject();
    }
    jsonWriter.endArray();

    // first write out core schema, then if any extensions write them
    // out in their own json object keyed by the schema name

    for (SCIMAttribute attribute : o
      .getAttributes(SCIMConstants.SCHEMA_URI_CORE)) {
      if (attribute.isPlural()) {
        this.writePluralAttribute(attribute, jsonWriter);
      } else {
        this.writeSingularAttribute(attribute, jsonWriter);
      }
    }

    // write out any custom schemas
    for (String schema : schemas) {
      if (!schema.equalsIgnoreCase(SCIMConstants.SCHEMA_URI_CORE)) {
        jsonWriter.key(schema);
        jsonWriter.object();
        for (SCIMAttribute attribute : o.getAttributes(schema)) {
          if (attribute.isPlural()) {
            this.writePluralAttribute(attribute, jsonWriter);
          } else {
            this.writeSingularAttribute(attribute, jsonWriter);
          }
        }
        jsonWriter.endObject();
      }
    }
    jsonWriter.endObject();
  }

  /**
   * Write a plural attribute to an XML stream.
   *
   * @param scimAttribute The attribute to be written.
   * @param jsonWriter    Output to write the attribute to.
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void writePluralAttribute(final SCIMAttribute scimAttribute,
    final JSONWriter jsonWriter)
    throws JSONException {

    SCIMAttributeValue[] pluralValues = scimAttribute.getPluralValues();
    jsonWriter.key(scimAttribute.getName());
    jsonWriter.array();
    List<AttributeDescriptor> mappedAttributeDescriptors =
      scimAttribute.getAttributeDescriptor().getComplexAttributeDescriptors();
    for (SCIMAttributeValue pluralValue : pluralValues) {
      for (AttributeDescriptor attributeDescriptor :
        mappedAttributeDescriptors) {
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
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void writeSingularAttribute(final SCIMAttribute scimAttribute,
    final JSONWriter jsonWriter)
    throws JSONException {
    jsonWriter.key(scimAttribute.getName());
    SCIMAttributeValue val = scimAttribute.getSingularValue();
    if (val.isComplex()) {
      jsonWriter.object();
      for (SCIMAttribute a : val.getAttributes().values()) {
        this.writeSingularAttribute(a, jsonWriter);
      }
      jsonWriter.endObject();
    } else {
      String stringValue = scimAttribute.getSingularValue().getStringValue();
      jsonWriter.value(stringValue);
    }
  }

  /**
   * Write a complex attribute to an XML stream.
   *
   * @param scimAttribute The attribute to be written.
   * @param jsonWriter    Output to write the attribute to.
   * @throws org.json.JSONException Thrown if error writing to output.
   */
  private void writeComplexAttribute(final SCIMAttribute scimAttribute,
    final JSONWriter jsonWriter)
    throws JSONException {
    SCIMAttributeValue value = scimAttribute.getSingularValue();
    Map<String, SCIMAttribute> attributes = value.getAttributes();
    jsonWriter.object();
    for (SCIMAttribute attribute : attributes.values()) {
      writeSingularAttribute(attribute, jsonWriter);
    }
    jsonWriter.endObject();
  }
}
