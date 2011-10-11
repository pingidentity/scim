/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.marshal;

import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.MarshalException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMException;

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
   * @throws MarshalException If an error occurred.
   */
  <R extends BaseResource> R unmarshal(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory)
      throws MarshalException;

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
   * @throws MarshalException If an error occurred.
   */
  <R extends BaseResource> Resources<R> unmarshalResources(
      final InputStream inputStream,
      final ResourceDescriptor resourceDescriptor,
      final ResourceFactory<R> resourceFactory)
      throws MarshalException;

  /**
   * Reads a SCIM error response from an input stream.
   *
   * @param inputStream  The input stream containing the SCIM object to be read.
   * @return The SCIM error response that was read.
   *
   * @throws MarshalException If an error occurred.
   */
  SCIMException unmarshalError(final InputStream inputStream)
      throws MarshalException;
}
