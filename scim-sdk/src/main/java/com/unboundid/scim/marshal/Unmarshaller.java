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

package com.unboundid.scim.marshal;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.BulkConfig;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.BulkContentHandler;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMException;

import java.io.File;
import java.io.InputStream;



/**
 * This interface provides methods that may be used to read SCIM objects
 * from their external representation. There are un-marshaller implementations
 * for XML and JSON. Un-marshaller implementations are required to be
 * thread-safe.
 */
public interface Unmarshaller {

  /**
   * Reads a SCIM resource from an input stream.
   *
   * @param <R> The type of resource instance.
   * @param inputStream  The input stream containing the SCIM object to be read.
   * @param resourceDescriptor The descriptor of the SCIM resource to be read.
   * @param resourceFactory The resource factory to use to create the resource
   *                        instance.
   *
   * @return  The SCIM resource that was read.
   *
   * @throws InvalidResourceException If an error occurred.
   */
  <R extends BaseResource> R unmarshal(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory)
      throws InvalidResourceException;

  /**
   * Reads a SCIM query response from an input stream.
   *
   * @param <R> The type of resource instance.
   * @param inputStream  The input stream containing the SCIM object to be read.
   * @param resourceDescriptor The descriptor of the SCIM resource to be read.
   * @param resourceFactory The resource factory to use to create the resource
   *                        instance.
   *
   * @return The SCIM query response that was read.
   *
   * @throws InvalidResourceException If an error occurred.
   */
  <R extends BaseResource> Resources<R> unmarshalResources(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory)
      throws InvalidResourceException;

  /**
   * Reads a SCIM error response from an input stream.
   *
   * @param inputStream  The input stream containing the SCIM object to be read.
   * @return The SCIM error response that was read.
   *
   * @throws InvalidResourceException If an error occurred.
   */
  SCIMException unmarshalError(final InputStream inputStream)
      throws InvalidResourceException;

  /**
   * Reads a SCIM bulk request or response from an input stream.
   *
   * @param inputStream  The input stream containing the bulk content to be
   *                     read.
   * @param bulkConfig   The bulk configuration settings to be enforced.
   * @param handler      A bulk operation listener to handle the content as it
   *                     is read.
   *
   * @throws SCIMException If the bulk content could not be read.
   */
  void bulkUnmarshal(final InputStream inputStream,
                     final BulkConfig bulkConfig,
                     final BulkContentHandler handler)
      throws SCIMException;



  /**
   * Reads a SCIM bulk request or response from a file.
   *
   * @param file         The file containing the bulk content to be read.
   * @param bulkConfig   The bulk configuration settings to be enforced.
   * @param handler      A bulk operation listener to handle the content as it
   *                     is read.
   *
   * @throws SCIMException If the bulk content could not be read.
   */
  void bulkUnmarshal(final File file,
                     final BulkConfig bulkConfig,
                     final BulkContentHandler handler)
      throws SCIMException;
}
