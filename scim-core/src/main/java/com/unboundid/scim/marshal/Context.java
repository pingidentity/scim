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

package com.unboundid.scim.marshal;

import com.unboundid.scim.marshal.json.JsonMarshaller;
import com.unboundid.scim.marshal.json.JsonUnmarshaller;
import com.unboundid.scim.marshal.xml.XmlMarshaller;
import com.unboundid.scim.marshal.xml.XmlUnmarshaller;



/**
 * This class provides a factory for SCIM object marshaller and un-marshaller
 * instances. Currently a singleton instance creates a marshaller and
 * un-marshaller for XML.
 */
public class Context {

  /**
   * The namespace label associated with the default schema.
   */
  public static final String DEFAULT_SCHEMA_PREFIX = "scim";

  /**
   * The singleton instance that can create a marshaller and un-marshaller for
   * XML.
   */
  private static final Context INSTANCE = new Context();

  /**
   * Set of supported un/marshaller formats.
   */
  public enum Format {
    /**
     * XML format.
     */
    Xml(),
    /**
     * JSON format.
     */
    Json(),;
  }

  /**
   * Retrieve a context instance that can create a marshaller and un-marshaller
   * for XML.
   *
   * @return A context instance that can create a marshaller and un-marshaller
   *         for XML.
   */
  public static Context instance() {
    return INSTANCE;
  }

  /**
   * Retrieve a SCIM object marshaller.
   *
   * @return A SCIM object marshaller.
   */
  public Marshaller marshaller() {
    return new XmlMarshaller();
  }

  /**
   * Retrieve a SCIM object marshaller capable of marshaling the specified
   * format.
   *
   * @param format The format to marshall.
   * @return The format specific marshaller.
   */
  public Marshaller marshaller(final Format format) {
    switch (format) {
      case Xml:
        return new XmlMarshaller();
      case Json:
        return new JsonMarshaller();
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * Retrieve a SCIM object un-marshaller capable of unmarshalling the
   * specified format.
   *
   * @param format The format to unmarshall.
   * @return A SCIM object un-marshaller.
   */
  public Unmarshaller unmarshaller(final Format format) {
    switch (format) {
      case Xml:
        return new XmlUnmarshaller();
      case Json:
        return new JsonUnmarshaller();
      default:
        throw new IllegalArgumentException();
    }
  }

  /**
   * Retrieve a SCIM object un-marshaller.
   *
   * @return A SCIM object un-marshaller.
   */
  public Unmarshaller unmarshaller() {
    return new XmlUnmarshaller();
  }

}
