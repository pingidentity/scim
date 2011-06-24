/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.scim.json.JSONContext;
import com.unboundid.scim.schema.User;
import com.unboundid.scim.xml.XMLContext;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import static com.unboundid.scim.sdk.SCIMConstants.HEADER_NAME_ACCEPT;
import static com.unboundid.scim.sdk.SCIMConstants.HEADER_NAME_LOCATION;
import static com.unboundid.scim.sdk.SCIMConstants.HEADER_NAME_METHOD_OVERRIDE;
import static com.unboundid.scim.sdk.SCIMConstants.MEDIA_TYPE_XML;
import static com.unboundid.scim.sdk.SCIMConstants.RESOURCE_NAME_USER;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;



/**
 * This class may be used to interact with a server that provides a Simple
 * Cloud Identity Management (SCIM) REST interface.
 */
public class SCIMClient
{
  /**
   * A Jetty HTTP client instance to make HTTP requests.
   */
  private HttpClient httpClient;

  /**
   * The address of the SCIM server.
   */
  private Address address;

  /**
   * The base URI of the SCIM interface.
   */
  private String baseURI;

  /**
   * A JSON context to read and write JSON.
   */
  private JSONContext jsonContext;

  /**
   * An XML context to read and write XML.
   */
  private XMLContext xmlContext;

  /**
   * Indicates whether the PUT operation is invoked using POST with
   * a method override.
   */
  private boolean putUsesMethodOverride = false;

  /**
   * Indicates whether the PATCH operation is invoked using POST with
   * a method override.
   */
  private boolean patchUsesMethodOverride = false;

  /**
   * Indicates whether the DELETE operation is invoked using POST with
   * a method override.
   */
  private boolean deleteUsesMethodOverride = false;



  /**
   * Create a new SCIM client from the provided information.
   *
   * @param host     The server host name.
   * @param port     The server port number.
   * @param baseURI  The base URI of the SCIM interface.
   */
  public SCIMClient(final String host, final int port, final String baseURI)
  {
    final HttpClient client = new HttpClient();
    client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
    client.setMaxConnectionsPerAddress(4);
    client.setThreadPool(new QueuedThreadPool(16));
    client.setTimeout(30000);

    jsonContext = new JSONContext();
    xmlContext = new XMLContext();
    address = new Address(host, port);
    httpClient = client;
    this.baseURI = baseURI;
  }



  /**
   * Attempts to start the client.
   *
   * @throws Exception If an error occurs during startup.
   */
  public void startClient()
      throws Exception
  {
    httpClient.start();
  }



  /**
   * Stops this client.
   *
   * @throws Exception If an error occurs during shutdown.
   */
  public void stopClient()
      throws Exception
  {
    if (httpClient != null)
    {
      httpClient.stop();

      httpClient = null;
    }
  }



  /**
   * Indicates whether the PUT operation is invoked using POST with
   * a method override.
   *
   * @return  {@code true} if the PUT operation is invoked using POST with
   *          a method override, or {@code false} if PUT is not invoked
   *          through POST.
   */
  public boolean isPutUsesMethodOverride()
  {
    return putUsesMethodOverride;
  }



  /**
   * Specifies whether the PUT operation is invoked using POST with
   * a method override.
   *
   * @param putUsesMethodOverride  {@code true} if the PUT operation is invoked
   *                                using POST with a method override, or
   *                                {@code false} if PUT is not invoked through
   *                                POST.
   */
  public void setPutUsesMethodOverride(final boolean putUsesMethodOverride)
  {
    this.putUsesMethodOverride = putUsesMethodOverride;
  }



  /**
   * Indicates whether the PATCH operation is invoked using POST with
   * a method override.
   *
   * @return  {@code true} if the PATCH operation is invoked using POST with
   *          a method override, or {@code false} if PATCH is not invoked
   *          through POST.
   */
  public boolean isPatchUsesMethodOverride()
  {
    return patchUsesMethodOverride;
  }



  /**
   * Specifies whether the PATCH operation is invoked using POST with
   * a method override.
   *
   * @param patchUsesMethodOverride  {@code true} if the PATCH operation is
   *                                  invoked using POST with a method override,
   *                                  or {@code false} if PATCH is not invoked
   *                                  through POST.
   */
  public void setPatchUsesMethodOverride(
      final boolean patchUsesMethodOverride)
  {
    this.patchUsesMethodOverride = patchUsesMethodOverride;
  }



  /**
   * Indicates whether the DELETE operation is invoked using POST with
   * a method override.
   *
   * @return  {@code true} if the DELETE operation is invoked using POST with
   *          a method override, or {@code false} if DELETE is not invoked
   *          through POST.
   */
  public boolean isDeleteUsesMethodOverride()
  {
    return deleteUsesMethodOverride;
  }



  /**
   * Specifies whether the DELETE operation is invoked using POST with
   * a method override.
   *
   * @param deleteUsesMethodOverride  {@code true} if the DELETE operation is
   *                                   invoked using POST with a method
   *                                   override, or {@code false} if DELETE is
   *                                   not invoked through POST.
   */
  public void setDeleteUsesMethodOverride(
      final boolean deleteUsesMethodOverride)
  {
    this.deleteUsesMethodOverride = deleteUsesMethodOverride;
  }



  /**
   * Retrieve the user with the given ID. A GET operation is invoked on the
   * User resource endpoint. The content type (JSON or XML) used for the
   * operation is not specified by the caller.
   *
   * @param userID      The ID of the user to be retrieved.
   * @param attributes  The set of attributes to be retrieved. If empty, then
   *                    the server returns all attributes.
   *
   * @return  The requested user or {@code null} if the user does not exist.
   *          The user may be incomplete if not all attributes were returned.
   *
   * @throws IOException  If an error occurred while retrieving the user.
   */
  public User getUser(final String userID, final String ... attributes)
      throws IOException
  {
    final ScimURI uri =
        new ScimURI(baseURI, RESOURCE_NAME_USER, userID, null, null,
                    new SCIMQueryAttributes(attributes));
    final ExceptionContentExchange exchange = new ExceptionContentExchange();
    exchange.setAddress(address);
    exchange.setMethod("GET");
    exchange.setURI(uri.toString());
    exchange.setRequestHeader(HEADER_NAME_ACCEPT, MEDIA_TYPE_XML);

    httpClient.send(exchange);
    final int exchangeState;
    try
    {
      exchangeState = exchange.waitForDone();
    }
    catch (InterruptedException e)
    {
      throw new IOException("HTTP exchange interrupted", e);
    }

    switch (exchangeState)
    {
      case HttpExchange.STATUS_COMPLETED:
        switch (exchange.getResponseStatus())
        {
          case HttpStatus.OK_200:
            // The user was found.
            return xmlContext.readUser(exchange.getResponseContent());

          case HttpStatus.NOT_FOUND_404:
            // The user was not found.
            return null;

          case HttpStatus.BAD_REQUEST_400:
          case HttpStatus.UNAUTHORIZED_401:
          case HttpStatus.FORBIDDEN_403:
          case HttpStatus.CONFLICT_409:
          case HttpStatus.PRECONDITION_FAILED_412:
          case HttpStatus.INTERNAL_SERVER_ERROR_500:
          case HttpStatus.NOT_IMPLEMENTED_501:
          default:
            final String statusMessage =
                HttpStatus.getMessage(exchange.getResponseStatus());
            if (exchange.getResponseContent() != null)
            {
              throw new IOException(statusMessage + ": " +
                                    exchange.getResponseContent());
            }
            else
            {
              throw new IOException(statusMessage);
            }
        }

      case HttpExchange.STATUS_EXCEPTED:
        throw new IOException("Exception during HTTP exchange",
                              exchange.getException());

      case HttpExchange.STATUS_EXPIRED:
        throw new IOException("HTTP request expired");

      default:
        // This should not happen.
        throw new IOException(
            "Unexpected HTTP exchange state: " + exchangeState);
    }
  }



  /**
   * Retrieve a user using a resource URI.
   *
   * @param resourceURI  The resource URI of a user.
   *
   * @return  The requested user or {@code null} if the user does not exist.
   *          The user may be incomplete if not all attributes were returned.
   *
   * @throws IOException  If an error occurred while retrieving the user.
   */
  public User getUserByURI(final String resourceURI)
      throws IOException
  {
    final ExceptionContentExchange exchange = new ExceptionContentExchange();
    exchange.setURL(resourceURI);
    exchange.setMethod("GET");
    exchange.setRequestHeader(HEADER_NAME_ACCEPT, MEDIA_TYPE_XML);

    httpClient.send(exchange);
    final int exchangeState;
    try
    {
      exchangeState = exchange.waitForDone();
    }
    catch (InterruptedException e)
    {
      throw new IOException("HTTP exchange interrupted", e);
    }

    switch (exchangeState)
    {
      case HttpExchange.STATUS_COMPLETED:
        switch (exchange.getResponseStatus())
        {
          case HttpStatus.OK_200:
            // The user was found.
            return xmlContext.readUser(exchange.getResponseContent());

          case HttpStatus.NOT_FOUND_404:
            // The user was not found.
            return null;

          default:
            final String statusMessage =
                HttpStatus.getMessage(exchange.getResponseStatus());
            if (exchange.getResponseContent() != null)
            {
              throw new IOException(statusMessage + ": " +
                                    exchange.getResponseContent());
            }
            else
            {
              throw new IOException(statusMessage);
            }
        }

      case HttpExchange.STATUS_EXCEPTED:
        throw new IOException("Exception during HTTP exchange",
                              exchange.getException());

      case HttpExchange.STATUS_EXPIRED:
        throw new IOException("HTTP request expired");

      default:
        // This should not happen.
        throw new IOException(
            "Unexpected HTTP exchange state: " + exchangeState);
    }
  }



  /**
   * Create a new user. A POST operation is invoked on the User resource
   * endpoint. The content type (JSON or XML) used for the operation is not
   * specified by the caller.
   *
   * @param user        The contents of the user to be created.
   * @param attributes  The set of attributes to be retrieved. If empty, then
   *                    the server returns all attributes.
   *
   * @return  The response from the request.
   *
   * @throws IOException  If an error occurred while creating the user.
   */
  public PostUserResponse postUser(final User user, final String ... attributes)
      throws IOException
  {
    final ScimURI uri =
        new ScimURI(baseURI, RESOURCE_NAME_USER, null, null, null,
                    new SCIMQueryAttributes(attributes));

    final ExceptionContentExchange exchange = new ExceptionContentExchange();
    exchange.setAddress(address);
    exchange.setMethod("POST");
    exchange.setURI(uri.toString());
    exchange.setRequestContentType(MEDIA_TYPE_XML);
    exchange.setRequestHeader(HEADER_NAME_ACCEPT, MEDIA_TYPE_XML);
    // TODO set character encoding utf-8

    // TODO we should re-use the buffer
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Writer writer = new OutputStreamWriter(out, "UTF-8");
    try
    {
      xmlContext.writeUser(writer, user);
    }
    catch (IOException e)
    {
      writer.close();
    }

    final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    exchange.setRequestContentSource(in);

    httpClient.send(exchange);
    final int exchangeState;
    try
    {
      exchangeState = exchange.waitForDone();
    }
    catch (InterruptedException e)
    {
      throw new IOException("HTTP exchange interrupted", e);
    }

    switch (exchangeState)
    {
      case HttpExchange.STATUS_COMPLETED:
        switch (exchange.getResponseStatus())
        {
          case HttpStatus.OK_200:
            // The user was created.
            final String resourceURI =
                exchange.getResponseFields().getStringField(
                    HEADER_NAME_LOCATION);
            final User returnUser =
                xmlContext.readUser(exchange.getResponseContent());

            return new PostUserResponse(resourceURI, returnUser);

          default:
            final String statusMessage =
                HttpStatus.getMessage(exchange.getResponseStatus());
            if (exchange.getResponseContent() != null)
            {
              throw new IOException(statusMessage + ": " +
                                    exchange.getResponseContent());
            }
            else
            {
              throw new IOException(statusMessage);
            }
        }

      case HttpExchange.STATUS_EXCEPTED:
        throw new IOException("Exception during HTTP exchange",
                              exchange.getException());

      case HttpExchange.STATUS_EXPIRED:
        throw new IOException("HTTP request expired");

      default:
        // This should not happen.
        throw new IOException(
            "Unexpected HTTP exchange state: " + exchangeState);
    }
  }



  /**
   * Delete a specified user.
   *
   * @param userID    The ID of the user to be deleted.
   *
   * @return  {@code true} if the resource was deleted, or {@code false} if
   *          the resource did not exist.
   *
   * @throws IOException  If an error occurred while deleting the user.
   */
  public boolean deleteUser(final String userID)
      throws IOException
  {
    return deleteResource(RESOURCE_NAME_USER, userID);
  }



  /**
   * Delete a specified resource.
   *
   * @param resourceName  The name of the resource (e.g. User).
   * @param resourceID    The ID of the resource to be deleted.
   *
   * @return  {@code true} if the resource was deleted, or {@code false} if
   *          the resource did not exist.
   *
   * @throws IOException  If an error occurred while deleting the resource.
   */
  public boolean deleteResource(final String resourceName,
                                final String resourceID)
      throws IOException
  {
    final ScimURI uri =
        new ScimURI(baseURI, resourceName, resourceID, null, null,
                    new SCIMQueryAttributes());
    final ExceptionContentExchange exchange = new ExceptionContentExchange();
    exchange.setAddress(address);
    if (deleteUsesMethodOverride)
    {
      exchange.setMethod("POST");
      exchange.setRequestHeader(HEADER_NAME_METHOD_OVERRIDE, "DELETE");
    }
    else
    {
      exchange.setMethod("DELETE");
    }
    exchange.setURI(uri.toString());

    httpClient.send(exchange);
    final int exchangeState;
    try
    {
      exchangeState = exchange.waitForDone();
    }
    catch (InterruptedException e)
    {
      throw new IOException("HTTP exchange interrupted", e);
    }

    switch (exchangeState)
    {
      case HttpExchange.STATUS_COMPLETED:
        switch (exchange.getResponseStatus())
        {
          case HttpStatus.OK_200:
            // The resource was deleted.
            return true;

          case HttpStatus.NOT_FOUND_404:
            // The resource was not found.
            return false;

          default:
            final String statusMessage =
                HttpStatus.getMessage(exchange.getResponseStatus());
            if (exchange.getResponseContent() != null)
            {
              throw new IOException(statusMessage + ": " +
                                    exchange.getResponseContent());
            }
            else
            {
              throw new IOException(statusMessage);
            }
        }

      case HttpExchange.STATUS_EXCEPTED:
        throw new IOException("Exception during HTTP exchange",
                              exchange.getException());

      case HttpExchange.STATUS_EXPIRED:
        throw new IOException("HTTP request expired");

      default:
        // This should not happen.
        throw new IOException(
            "Unexpected HTTP exchange state: " + exchangeState);
    }
  }



  /**
   * Delete a resource identified by the specified URI.
   *
   * @param resourceURI  The URI of the resource to be deleted.
   *
   * @return  {@code true} if the resource was deleted, or {@code false} if
   *          the resource did not exist.
   *
   * @throws IOException  If an error occurred while deleting the resource.
   */
  public boolean deleteResourceByURI(final String resourceURI)
      throws IOException
  {
    final ExceptionContentExchange exchange = new ExceptionContentExchange();
    exchange.setURL(resourceURI);
    if (deleteUsesMethodOverride)
    {
      exchange.setMethod("POST");
      exchange.setRequestHeader(HEADER_NAME_METHOD_OVERRIDE, "DELETE");
    }
    else
    {
      exchange.setMethod("DELETE");
    }

    httpClient.send(exchange);
    final int exchangeState;
    try
    {
      exchangeState = exchange.waitForDone();
    }
    catch (InterruptedException e)
    {
      throw new IOException("HTTP exchange interrupted", e);
    }

    switch (exchangeState)
    {
      case HttpExchange.STATUS_COMPLETED:
        switch (exchange.getResponseStatus())
        {
          case HttpStatus.OK_200:
            // The user was deleted.
            return true;

          case HttpStatus.NOT_FOUND_404:
            // The user was not found.
            return false;

          default:
            final String statusMessage =
                HttpStatus.getMessage(exchange.getResponseStatus());
            if (exchange.getResponseContent() != null)
            {
              throw new IOException(statusMessage + ": " +
                                    exchange.getResponseContent());
            }
            else
            {
              throw new IOException(statusMessage);
            }
        }

      case HttpExchange.STATUS_EXCEPTED:
        throw new IOException("Exception during HTTP exchange",
                              exchange.getException());

      case HttpExchange.STATUS_EXPIRED:
        throw new IOException("HTTP request expired");

      default:
        // This should not happen.
        throw new IOException(
            "Unexpected HTTP exchange state: " + exchangeState);
    }
  }



  /**
   * Replace the contents of an existing user. A PUT operation is invoked on
   * the User resource endpoint. The content type (JSON or XML) used for the
   * operation is not specified by the caller.
   *
   * @param userID      The ID of the user to be replaced.
   * @param user        The new contents of the user.
   * @param attributes  The set of attributes to be returned. If empty, then
   *                    the server returns all attributes.
   *
   * @return  The updated contents of the user, or {@code null} if the user
   *          did not exist.
   *
   * @throws IOException  If an error occurred while replacing the user.
   */
  public User putUser(final String userID, final User user,
                      final String ... attributes)
      throws IOException
  {
    final ScimURI uri =
        new ScimURI(baseURI, RESOURCE_NAME_USER, userID, null, null,
                    new SCIMQueryAttributes(attributes));

    final ExceptionContentExchange exchange = new ExceptionContentExchange();
    exchange.setAddress(address);
    if (putUsesMethodOverride)
    {
      exchange.setMethod("POST");
      exchange.setRequestHeader(HEADER_NAME_METHOD_OVERRIDE, "PUT");
    }
    else
    {
      exchange.setMethod("PUT");
    }
    exchange.setURI(uri.toString());
    exchange.setRequestContentType(MEDIA_TYPE_XML);
    exchange.setRequestHeader(HEADER_NAME_ACCEPT, MEDIA_TYPE_XML);
    // TODO set character encoding utf-8

    // TODO we should re-use the buffer
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final Writer writer = new OutputStreamWriter(out, "UTF-8");
    try
    {
      xmlContext.writeUser(writer, user);
    }
    catch (IOException e)
    {
      writer.close();
    }

    final ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
    exchange.setRequestContentSource(in);

    httpClient.send(exchange);
    final int exchangeState;
    try
    {
      exchangeState = exchange.waitForDone();
    }
    catch (InterruptedException e)
    {
      throw new IOException("HTTP exchange interrupted", e);
    }

    switch (exchangeState)
    {
      case HttpExchange.STATUS_COMPLETED:
        switch (exchange.getResponseStatus())
        {
          case HttpStatus.OK_200:
            // The user was replaced.
            return xmlContext.readUser(exchange.getResponseContent());

          case HttpStatus.NOT_FOUND_404:
            // The user was not found.
            return null;

          default:
            final String statusMessage =
                HttpStatus.getMessage(exchange.getResponseStatus());
            if (exchange.getResponseContent() != null)
            {
              throw new IOException(statusMessage + ": " +
                                    exchange.getResponseContent());
            }
            else
            {
              throw new IOException(statusMessage);
            }
        }

      case HttpExchange.STATUS_EXCEPTED:
        throw new IOException("Exception during HTTP exchange",
                              exchange.getException());

      case HttpExchange.STATUS_EXPIRED:
        throw new IOException("HTTP request expired");

      default:
        // This should not happen.
        throw new IOException(
            "Unexpected HTTP exchange state: " + exchangeState);
    }
  }



}
