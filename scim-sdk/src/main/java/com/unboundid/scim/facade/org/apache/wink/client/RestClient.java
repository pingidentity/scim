/*
 * Copyright 2011-2024 Ping Identity Corporation
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

package com.unboundid.scim.facade.org.apache.wink.client;

import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.ws.rs.client.Client;
import java.net.URI;
import java.net.URISyntaxException;

/**
 *  Wink compatibility layer class - see Wink docs.
 */
public class RestClient
{
  private Client client;

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param clientConfig Wink compatibility layer class - see Wink docs.
   */
  public RestClient(final org.glassfish.jersey.client.ClientConfig clientConfig)
  {
    client = JerseyClientBuilder.newClient(clientConfig);
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param uriString Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   * @throws URISyntaxException Wink compatibility layer class - see Wink docs.
   */
  public Resource resource(final String uriString) throws URISyntaxException
  {
    URI uri = new URI(uriString);
    return new Resource(this, uri);
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param uri Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource resource(final URI uri)
  {
    return new Resource(this, uri);
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Client getClient()
  {
    return client;
  }
}
