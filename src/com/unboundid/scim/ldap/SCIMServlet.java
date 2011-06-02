/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.scim.json.JSONContext;
import com.unboundid.scim.schema.User;
import static com.unboundid.scim.sdk.SCIMConstants.ATTRIBUTES_QUERY_STRING;
import static com.unboundid.scim.sdk.SCIMConstants.HEADER_NAME_ACCEPT;
import static com.unboundid.scim.sdk.SCIMConstants.MEDIA_TYPE_JSON;

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
   * The name of the LDAP attribute that contains the SCIM User ID.
   */
  private static final String ATTR_ENTRYUUID = "entryUUID";

  /**
   * The SCIM server configuration.
   */
  private SCIMServerConfig serverConfig;

  /**
   * An LDAP external server to provide the resource storage repository.
   */
  private LDAPExternalServer ldapExternalServer;

  /**
   * A JSON context to read and write JSON.
   */
  private JSONContext jsonContext;



  /**
   * Create a new instance of the SCIM servlet.
   *
   * @param serverConfig        The configuration of the SCIM server in which
   *                            this servlet resides.
   * @param ldapExternalServer  The LDAP external server providing the resource
   *                            storage repository.
   */
  public SCIMServlet(final SCIMServerConfig serverConfig,
                     final LDAPExternalServer ldapExternalServer)
  {
    this.serverConfig       = serverConfig;
    this.ldapExternalServer = ldapExternalServer;

    this.jsonContext        = new JSONContext();
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

    boolean requestJSON = true;
    final String userID;
    final String resource = split[1];
    if (resource.endsWith(".xml"))
    {
      requestJSON = false;
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

    if (!requestJSON)
    {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
                         "XML content is not yet implemented");
      return;
    }

    if (!acceptedContentTypes(request.getHeader(HEADER_NAME_ACCEPT)).contains(
        ContentTypeID.JSON))
    {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
                         "Only JSON content is currently implemented");
      return;
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
    final String[] queryAttributes;
    final String queryString = request.getQueryString();
    if (queryString != null && !queryString.isEmpty())
    {
      final String[] querySplit = queryString.split("=", 2);
      if (!querySplit[0].equalsIgnoreCase(ATTRIBUTES_QUERY_STRING))
      {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "The query string is not valid");
      }

      if (querySplit.length < 2)
      {
        queryAttributes = new String[0];
      }
      else
      {
        queryAttributes = querySplit[1].split(",");
      }
    }
    else
    {
      queryAttributes = new String[0];
    }

    try
    {
      final Filter filter =
          Filter.createANDFilter(
              Filter.createEqualityFilter(ATTR_ENTRYUUID, userID),
              Filter.createEqualityFilter("objectclass", "inetorgperson"));
      final SearchRequest searchRequest =
          new SearchRequest(serverConfig.getDsBaseDN(), SearchScope.SUB,
                            filter, "*", ATTR_ENTRYUUID);
      final SearchResultEntry searchResultEntry =
          ldapExternalServer.searchForEntry(searchRequest);
      if (searchResultEntry == null)
      {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      }
      else
      {
        final User user =
            LDAPUtil.userFromInetOrgPersonEntry(searchResultEntry,
                                                queryAttributes);
        response.setContentType(MEDIA_TYPE_JSON);
        response.setCharacterEncoding("UTF-8");
        jsonContext.writeUser(response.getWriter(), user);
        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
      }
    }
    catch (Exception e)
    {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                         e.getMessage());
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
