/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import com.unboundid.scim.json.JSONContext;
import com.unboundid.scim.schema.User;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.marshall.Context;
import com.unboundid.scim.marshall.Marshaller;
import com.unboundid.scim.marshall.Unmarshaller;
import com.unboundid.scim.xml.XMLContext;
import org.eclipse.jetty.util.UrlEncoded;

import static com.unboundid.scim.sdk.SCIMConstants.ATTRIBUTES_QUERY_STRING;
import static com.unboundid.scim.sdk.SCIMConstants.HEADER_NAME_ACCEPT;
import static com.unboundid.scim.sdk.SCIMConstants.MEDIA_TYPE_JSON;
import static com.unboundid.scim.sdk.SCIMConstants.MEDIA_TYPE_XML;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;



/**
 * This is a base class for HTTP Servlets that implement the Simple Cloud
 * Identity Management (SCIM) REST interface. An LDAP server is used as the
 * resource storage repository.
 */
public class SCIMServlet
    extends HttpServlet
{
  /**
   * The serial version ID required for this serializable class.
   */
  private static final long serialVersionUID = 2718930275202254124L;

  /**
   * The backend providing the resource storage repository.
   */
  private SCIMBackend backend;

  /**
   * A JSON context to read and write JSON.
   */
  private JSONContext jsonContext;

  /**
   * An XML context to read and write XML.
   */
  private XMLContext xmlContext;



  /**
   * Create a new instance of the SCIM servlet.
   *
   * @param backend       The backend providing the resource storage repository.
   */
  public SCIMServlet(final SCIMBackend backend)
  {
    this.backend       = backend;
    this.jsonContext   = new JSONContext();
    this.xmlContext    = new XMLContext();
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected void doGet(final HttpServletRequest request,
                       final HttpServletResponse response)
      throws ServletException, IOException
  {
    // Get the part of the path that identifies the resource.
    final String pathInfo = request.getPathInfo();
    if (pathInfo == null)
    {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }

    // Split up the components of the resource identifier.
    final String[] split = pathInfo.split("/");
    if (split.length < 2 || split[1].isEmpty())
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                         "The operation does not specify a user ID");
      return;
    }

    if (split.length > 3 || !split[0].isEmpty())
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                         "The URI does not specify a valid user operation");
      return;
    }

    boolean xmlSuffix = false;
    boolean jsonSuffix = false;
    final String userID;
    final String resource = split[1];
    if (resource.endsWith(".xml"))
    {
      xmlSuffix = true;
      if (resource.length() > 4)
      {
        userID = resource.substring(0, resource.length() - 4);
      }
      else
      {
        userID = "";
      }
    }
    else if (resource.endsWith(".json"))
    {
      jsonSuffix = true;
      if (resource.length() > 5)
      {
        userID = resource.substring(0, resource.length() - 5);
      }
      else
      {
        userID = "";
      }
    }
    else
    {
      userID = resource;
    }

    final EnumSet<ContentTypeID> acceptTypes =
        acceptedContentTypes(request.getHeader(HEADER_NAME_ACCEPT));
    final boolean jsonAccepted = acceptTypes.contains(ContentTypeID.JSON);
    final boolean xmlAccepted = acceptTypes.contains(ContentTypeID.XML);

    if (xmlSuffix && !xmlAccepted)
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                         ".xml conflicts with accepted content types");
      return;
    }

    if (jsonSuffix && !jsonAccepted)
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                         ".json conflicts with accepted content types");
      return;
    }

    if (!xmlAccepted && !jsonAccepted)
    {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
                         "Only JSON and XML content types are supported");
      return;
    }

    boolean returnJSON = true;
    if (xmlSuffix)
    {
      returnJSON = false;
    }
    else if (!jsonAccepted)
    {
      returnJSON = false;
    }

    final String targetAttribute;
    if (split.length > 2)
    {
      targetAttribute = split[2];
    }
    else
    {
      targetAttribute = null;
    }

    if (targetAttribute != null && !targetAttribute.isEmpty())
    {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
                         "Operations on user attributes are not implemented");
      return;
    }

    // Parse the query string.
    final String[] attributes;
    final String queryString = request.getQueryString();
    if (queryString != null && !queryString.isEmpty())
    {
      final UrlEncoded queryMap = new UrlEncoded(queryString);
      for (final Object k : queryMap.keySet())
      {
        if (k instanceof String)
        {
          if (!k.equals(ATTRIBUTES_QUERY_STRING))
          {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                               "The query string is not valid");
          }
        }
      }
      if (queryMap.containsKey(ATTRIBUTES_QUERY_STRING))
      {
        final String commaList = queryMap.getString(ATTRIBUTES_QUERY_STRING);
        if (commaList != null && !commaList.isEmpty())
        {
          attributes = commaList.split(",");
        }
        else
        {
          attributes = new String[0];
        }
      }
      else
      {
        attributes = new String[0];
      }
    }
    else
    {
      attributes = new String[0];
    }

    final SCIMQueryAttributes queryAttributes =
        new SCIMQueryAttributes(attributes);
    final GetResourceRequest getResourceRequest =
        new GetResourceRequest("User", userID, queryAttributes);
    try
    {
      final User user = backend.getUser(getResourceRequest);
      if (user == null)
      {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
      else
      {
        if (returnJSON)
        {
          response.setContentType(MEDIA_TYPE_JSON);
          response.setCharacterEncoding("UTF-8");
          jsonContext.writeUser(response.getWriter(), user);
          response.setStatus(HttpServletResponse.SC_OK);
          response.flushBuffer();
        }
        else
        {
          response.setContentType(MEDIA_TYPE_XML);
          response.setCharacterEncoding("UTF-8");
          xmlContext.writeUser(response.getWriter(), user);
          response.setStatus(HttpServletResponse.SC_OK);
          response.flushBuffer();
        }
      }
    }
    catch (Exception e)
    {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                         e.getMessage());
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    try {
      // pull in a resource and echo it back
      Unmarshaller unmarshaller = Context.instance().unmarshaller();
      SCIMObject scimObject = unmarshaller.unmarshall(request.getInputStream());
      // dump it out
      Marshaller marshaller = Context.instance().marshaller();
      marshaller.marshall(scimObject, response.getWriter());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * An enumeration of possible content types.
   */
  protected enum ContentTypeID
  {
    /**
     * The application/json content type.
     */
    JSON,

    /**
     * The application/xml content type.
     */
    XML
  }



  /**
   * Determine which of our supported content types are accepted by the client.
   * Note that this method does not yet handle the full generality of RFC2616,
   * Section 14.1.
   *
   * @param acceptHeader  The value of the Accept header provided by the client
   *                      or {@code null} if there is no Accept header.
   *
   * @return  The set of content types accepted by the client.
   */
  protected EnumSet<ContentTypeID> acceptedContentTypes(
      final String acceptHeader)
  {
    if (acceptHeader == null || acceptHeader.equals("*/*"))
    {
      return EnumSet.allOf(ContentTypeID.class);
    }

    final String[] split = acceptHeader.split("/");
    if (split.length < 2)
    {
      return EnumSet.noneOf(ContentTypeID.class);
    }

    if (split[0].equalsIgnoreCase("application"))
    {
      if (split[1].equals("*"))
      {
        return EnumSet.of(ContentTypeID.JSON, ContentTypeID.XML);
      }
      else if (split[1].equalsIgnoreCase("json"))
      {
        return EnumSet.of(ContentTypeID.JSON);
      }
      else if (split[1].equalsIgnoreCase("xml"))
      {
        return EnumSet.of(ContentTypeID.XML);
      }

      return EnumSet.noneOf(ContentTypeID.class);
    }

    return EnumSet.noneOf(ContentTypeID.class);
  }
}
