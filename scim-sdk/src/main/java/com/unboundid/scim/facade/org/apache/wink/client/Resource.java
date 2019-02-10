/*
 * Copyright 2011-2019 Ping Identity Corporation
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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 *  Wink compatibility layer class - see Wink docs.
 */
public class Resource
{
  private WebTarget webTarget;
  private MediaType contentType;
  private MediaType acceptType;
  private MultivaluedMap<String, Object> headers;
  private Cookie cookie;

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param restClient Wink compatibility layer class - see Wink docs.
   * @param uri Wink compatibility layer class - see Wink docs.
   */
  Resource(final RestClient restClient, final URI uri)
  {
    Client client = restClient.getClient();
    webTarget = client.target(uri);
    headers = new MultivaluedHashMap<String, Object>();
    contentType = MediaType.APPLICATION_JSON_TYPE;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param acceptType Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource accept(final MediaType acceptType)
  {
    this.acceptType = acceptType;
    return this;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param acceptTypeString Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource accept(final String acceptTypeString)
  {
    this.acceptType = MediaType.valueOf(acceptTypeString);
    return this;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param contentType Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource contentType(final MediaType contentType)
  {
    this.contentType = contentType;
    return this;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param cookie Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource cookie(final String cookie)
  {
    this.cookie = Cookie.valueOf(cookie);
    return this;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param cookie Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource cookie(final Cookie cookie)
  {
    this.cookie = cookie;
    return this;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param contentTypeString Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource contentType(final String contentTypeString)
  {
    this.contentType = MediaType.valueOf(contentTypeString);
    return this;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param name Wink compatibility layer class - see Wink docs.
   * @param values Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource header(final String name, final String ... values)
  {
    this.headers.addAll(name, values);
    return this;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param name Wink compatibility layer class - see Wink docs.
   * @param values Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public Resource queryParam(final String name, final Object ... values)
  {
    webTarget = webTarget.queryParam(name, values);
    return this;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public ClientResponse options()
  {
    Invocation.Builder builder = webTarget.request();
    builder.headers(headers);
    if(acceptType != null)
    {
      builder.accept(acceptType);
    }
    if(cookie != null)
    {
      builder.cookie(cookie);
    }
    Response response = builder.options();

    ClientResponse clientResponse = new ClientResponse(response);
    return clientResponse;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public ClientResponse get()
  {
    Invocation.Builder builder = webTarget.request();
    builder.headers(headers);
    if(acceptType != null)
    {
      builder.accept(acceptType);
    }
    if(cookie != null)
    {
      builder.cookie(cookie);
    }
    Response response = builder.get();

    ClientResponse clientResponse = new ClientResponse(response);
    return clientResponse;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param entity Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   * @throws ClientWebException Wink compatibility layer class - see Wink docs.
   */
  public ClientResponse post(final Object entity) throws ClientWebException
  {
    Invocation.Builder builder = webTarget.request();
    builder.headers(headers);
    if(acceptType != null)
    {
      builder.accept(acceptType);
    }
    if(cookie != null)
    {
      builder.cookie(cookie);
    }
    Response response = builder.post(Entity.entity(entity, contentType));

    ClientResponse clientResponse = new ClientResponse(response);
    return clientResponse;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param entity Wink compatibility layer class - see Wink docs.
   * @param <T> Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public <T> ClientResponse put(final T entity)
  {
    Invocation.Builder builder = webTarget.request();
    builder.headers(headers);

    if(acceptType != null)
    {
      builder.accept(acceptType);
    }
    if(cookie != null)
    {
      builder.cookie(cookie);
    }

    Response response = builder.put(Entity.entity(entity, contentType));

    ClientResponse clientResponse = new ClientResponse(response);
    return clientResponse;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   */
  public ClientResponse delete()
  {
    Invocation.Builder builder = webTarget.request();
    builder.headers(headers);
    if(acceptType != null)
    {
      builder.accept(acceptType);
    }
    if(cookie != null)
    {
      builder.cookie(cookie);
    }

    Response response = builder.delete();

    ClientResponse clientResponse = new ClientResponse(response);
    return clientResponse;
  }

  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param operation Wink compatibility layer class - see Wink docs.
   * @param responseClass Wink compatibility layer class - see Wink docs.
   * @param entity Wink compatibility layer class - see Wink docs.
   * @param <T> Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   * @throws ClientWebException Wink compatibility layer class - see Wink docs.
   */
  public <T> ClientResponse invoke(final String operation,
      final Class<ClientResponse> responseClass, final T entity)
      throws ClientWebException
  {
    Invocation.Builder builder = webTarget.request();
    builder.headers(headers);
    if(acceptType != null)
    {
      builder.accept(acceptType);
    }
    if(cookie != null)
    {
      builder.cookie(cookie);
    }

    Response response = builder.method(operation,
        Entity.entity(entity, contentType));
        ClientResponse clientResponse = new ClientResponse(response);
    return clientResponse;
  }
}
