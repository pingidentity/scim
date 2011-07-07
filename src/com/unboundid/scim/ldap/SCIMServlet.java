/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import com.unboundid.scim.marshal.Context;
import com.unboundid.scim.marshal.Marshaller;
import com.unboundid.scim.marshal.Unmarshaller;
import com.unboundid.scim.schema.Resource;
import com.unboundid.scim.sdk.SCIMAttributeType;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;
import com.unboundid.scim.sdk.ScimURI;
import com.unboundid.util.StaticUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;

import static com.unboundid.scim.sdk.SCIMConstants.HEADER_NAME_ACCEPT;
import static com.unboundid.scim.sdk.SCIMConstants.HEADER_NAME_LOCATION;
import static com.unboundid.scim.sdk.SCIMConstants.HEADER_NAME_METHOD_OVERRIDE;
import static com.unboundid.scim.sdk.SCIMConstants.MEDIA_TYPE_JSON;
import static com.unboundid.scim.sdk.SCIMConstants.MEDIA_TYPE_XML;



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
   * Create a new instance of the SCIM servlet.
   *
   * @param backend       The backend providing the resource storage repository.
   */
  public SCIMServlet(final SCIMBackend backend)
  {
    this.backend = backend;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected void doGet(final HttpServletRequest request,
                       final HttpServletResponse response)
      throws ServletException, IOException
  {
    try
    {
      final ScimURI uri = ScimURI.parseURI(request.getContextPath(),
                                           request.getPathInfo(),
                                           request.getQueryString());

      final SCIMAttributeType resourceAttribute = uri.getResourceAttribute();
      if (resourceAttribute != null)
      {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                           "Operations on user attributes are not implemented");
        return;
      }

      // Determine the media type to return.
      final String mediaType = getMediaType(uri, request);
      if (mediaType == null)
      {
        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,
                           "Only JSON and XML content types are supported");
        return;
      }

      // Get the authenticated user ID.
      final String userID;
      final Principal userPrincipal = request.getUserPrincipal();
      if (userPrincipal != null)
      {
        userID = userPrincipal.getName();
      }
      else
      {
        userID = null;
      }

      // Process the request.
      final SCIMResponse scimResponse;
      if (uri.getResourceID() == null)
      {
        final GetResourcesRequest getResourcesRequest =
            new GetResourcesRequest(userID,
                                    uri.getResourceEndPoint(),
                                    uri.getFilter(),
                                    uri.getSortParameters(),
                                    uri.getPageParameters(),
                                    uri.getQueryAttributes());
        scimResponse = backend.getResources(getResourcesRequest);
      }
      else
      {
        final GetResourceRequest getResourceRequest =
            new GetResourceRequest(userID,
                                   uri.getResourceEndPoint(),
                                   uri.getResourceID(),
                                   uri.getQueryAttributes());
        scimResponse = backend.getResource(getResourceRequest);
      }

      // Return the response.
      response.setStatus(scimResponse.getStatusCode());

      final Marshaller marshaller;
      if (mediaType.equalsIgnoreCase(MEDIA_TYPE_JSON))
      {
        response.setContentType(MEDIA_TYPE_JSON);
        response.setCharacterEncoding("UTF-8");
        marshaller = Context.instance().marshaller(Context.Format.Json);
      }
      else
      {
        response.setContentType(MEDIA_TYPE_XML);
        response.setCharacterEncoding("UTF-8");
        marshaller = Context.instance().marshaller(Context.Format.Xml);
      }

      marshaller.marshal(scimResponse.getResponse(),
                         response.getOutputStream());
    }
    catch (Exception e)
    {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                         StaticUtils.getExceptionMessage(e));
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected void doPost(final HttpServletRequest request,
                        final HttpServletResponse response)
      throws ServletException, IOException
  {
    try
    {
      // Handle any method override.
      final String methodOverride =
          request.getHeader(HEADER_NAME_METHOD_OVERRIDE);
      if (methodOverride != null)
      {
        if (methodOverride.equalsIgnoreCase("PUT"))
        {
          doPut(request, response);
          return;
        }
        else if (methodOverride.equalsIgnoreCase("DELETE"))
        {
          doDelete(request, response);
          return;
        }
      }

      final ScimURI uri = ScimURI.parseURI(request.getContextPath(),
                                           request.getPathInfo(),
                                           request.getQueryString());

      final SCIMAttributeType resourceAttribute = uri.getResourceAttribute();
      if (resourceAttribute != null)
      {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                           "Operations on user attributes are not implemented");
        return;
      }

      // Determine the media type to return.
      final String mediaType = getMediaType(uri, request);
      if (mediaType == null)
      {
        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,
                           "Only JSON and XML content types are supported");
        return;
      }

      // Get the authenticated user ID.
      final String userID;
      final Principal userPrincipal = request.getUserPrincipal();
      if (userPrincipal != null)
      {
        userID = userPrincipal.getName();
      }
      else
      {
        userID = null;
      }

      // Parse the resource.
      final Unmarshaller unmarshaller;
      final String contentType = request.getContentType();
      if (contentType != null && contentType.equalsIgnoreCase(MEDIA_TYPE_XML))
      {
        unmarshaller = Context.instance().unmarshaller(Context.Format.Xml);
      }
      else
      {
        unmarshaller = Context.instance().unmarshaller(Context.Format.Json);
      }

      final SCIMObject requestObject =
          unmarshaller.unmarshal(request.getInputStream());

      // Process the request.
      final PostResourceRequest postResourceRequest =
          new PostResourceRequest(userID, uri.getResourceEndPoint(),
                                  requestObject, uri.getQueryAttributes());
      final SCIMResponse scimResponse =
          backend.postResource(postResourceRequest);

      // Return the response.
      response.setStatus(scimResponse.getStatusCode());

      final Resource resource = scimResponse.getResponse().getResource();
      if (resource != null)
      {
        final String location =
            getLocationURL(request, uri.getResourceEndPoint(),
                           resource.getId());
        response.addHeader(HEADER_NAME_LOCATION, location);
      }

      final Marshaller marshaller;
      if (mediaType.equalsIgnoreCase(MEDIA_TYPE_JSON))
      {
        response.setContentType(MEDIA_TYPE_JSON);
        response.setCharacterEncoding("UTF-8");
        marshaller = Context.instance().marshaller(Context.Format.Json);
      }
      else
      {
        response.setContentType(MEDIA_TYPE_XML);
        response.setCharacterEncoding("UTF-8");
        marshaller = Context.instance().marshaller(Context.Format.Xml);
      }

      marshaller.marshal(scimResponse.getResponse(),
                         response.getOutputStream());
    }
    catch (Exception e)
    {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                         StaticUtils.getExceptionMessage(e));
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected void doDelete(final HttpServletRequest request,
                          final HttpServletResponse response)
      throws ServletException, IOException
  {
    try
    {
      final ScimURI uri = ScimURI.parseURI(request.getContextPath(),
                                           request.getPathInfo(),
                                           null);
      if (uri.getResourceID() == null)
      {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "The operation does not specify a resource ID");
        return;
      }

      final SCIMAttributeType resourceAttribute = uri.getResourceAttribute();
      if (resourceAttribute != null)
      {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                           "Operations on user attributes are not implemented");
        return;
      }

      // Determine the media type to return.
      final String mediaType = getMediaType(uri, request);
      if (mediaType == null)
      {
        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,
                           "Only JSON and XML content types are supported");
        return;
      }

      // Get the authenticated user ID.
      final String userID;
      final Principal userPrincipal = request.getUserPrincipal();
      if (userPrincipal != null)
      {
        userID = userPrincipal.getName();
      }
      else
      {
        userID = null;
      }

      // Process the request.
      final DeleteResourceRequest deleteResourceRequest =
          new DeleteResourceRequest(userID, uri.getResourceEndPoint(),
                                    uri.getResourceID());
      final SCIMResponse scimResponse =
          backend.deleteResource(deleteResourceRequest);

      // Return the response.
      response.setStatus(scimResponse.getStatusCode());
      if (scimResponse.getStatusCode() != HttpServletResponse.SC_OK)
      {
        final Marshaller marshaller;
        if (mediaType.equalsIgnoreCase(MEDIA_TYPE_JSON))
        {
          response.setContentType(MEDIA_TYPE_JSON);
          response.setCharacterEncoding("UTF-8");
          marshaller = Context.instance().marshaller(Context.Format.Json);
        }
        else
        {
          response.setContentType(MEDIA_TYPE_XML);
          response.setCharacterEncoding("UTF-8");
          marshaller = Context.instance().marshaller(Context.Format.Xml);
        }

        marshaller.marshal(scimResponse.getResponse(),
                           response.getOutputStream());
      }
    }
    catch (Exception e)
    {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                         StaticUtils.getExceptionMessage(e));
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected void doPut(final HttpServletRequest request,
                       final HttpServletResponse response)
      throws ServletException, IOException
  {
    try
    {
      final ScimURI uri = ScimURI.parseURI(request.getContextPath(),
                                           request.getPathInfo(),
                                           request.getQueryString());

      final SCIMAttributeType resourceAttribute = uri.getResourceAttribute();
      if (resourceAttribute != null)
      {
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                           "Operations on user attributes are not implemented");
        return;
      }

      // Determine the media type to return.
      final String mediaType = getMediaType(uri, request);
      if (mediaType == null)
      {
        response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE,
                           "Only JSON and XML content types are supported");
        return;
      }

      // Get the authenticated user ID.
      final String userID;
      final Principal userPrincipal = request.getUserPrincipal();
      if (userPrincipal != null)
      {
        userID = userPrincipal.getName();
      }
      else
      {
        userID = null;
      }

      // Parse the resource.
      final Unmarshaller unmarshaller;
      final String contentType = request.getContentType();
      if (contentType != null && contentType.equalsIgnoreCase(MEDIA_TYPE_XML))
      {
        unmarshaller = Context.instance().unmarshaller(Context.Format.Xml);
      }
      else
      {
        unmarshaller = Context.instance().unmarshaller(Context.Format.Json);
      }

      final SCIMObject requestObject =
          unmarshaller.unmarshal(request.getInputStream());

      // Process the request.
      final PutResourceRequest putResourceRequest =
          new PutResourceRequest(userID, uri.getResourceEndPoint(),
                                 uri.getResourceID(), requestObject,
                                 uri.getQueryAttributes());
      final SCIMResponse scimResponse = backend.putResource(putResourceRequest);

      response.setStatus(scimResponse.getStatusCode());

      final Marshaller marshaller;
      if (mediaType.equalsIgnoreCase(MEDIA_TYPE_JSON))
      {
        response.setContentType(MEDIA_TYPE_JSON);
        response.setCharacterEncoding("UTF-8");
        marshaller = Context.instance().marshaller(Context.Format.Json);
      }
      else
      {
        response.setContentType(MEDIA_TYPE_XML);
        response.setCharacterEncoding("UTF-8");
        marshaller = Context.instance().marshaller(Context.Format.Xml);
      }

      marshaller.marshal(scimResponse.getResponse(),
                         response.getOutputStream());
    }
    catch (Exception e)
    {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                         StaticUtils.getExceptionMessage(e));
    }
  }



  /**
   * Determine the media type that should be used in a response.
   *
   * @param uri      The request URI.
   * @param request  The servlet request.
   *
   * @return  The media type (e.g. application/xml) or {@code null} if an
   *          acceptable media type cannot be determined.
   */
  private static String getMediaType(final ScimURI uri,
                                     final HttpServletRequest request)
  {
    String mediaType = null;

    final String mediaTypeSuffix = uri.getMediaTypeSuffix();
    if (mediaTypeSuffix != null)
    {
      if (mediaTypeSuffix.equalsIgnoreCase("xml"))
      {
        mediaType = MEDIA_TYPE_XML;
      }
      else if (mediaTypeSuffix.equalsIgnoreCase("json"))
      {
        mediaType = MEDIA_TYPE_JSON;
      }
    }

    if (mediaType == null)
    {
      final HttpAcceptHeader acceptHeader =
          HttpAcceptHeader.parse(request.getHeader(HEADER_NAME_ACCEPT));
      mediaType = acceptHeader.findBestMatch(MEDIA_TYPE_JSON, MEDIA_TYPE_XML);
    }

    return mediaType;
  }



  /**
   * Construct an absolute URL string representing the location of a specific
   * resource.
   *
   * @param request       The HTTP servlet request.
   * @param resourceName  The name of the resource.
   * @param resourceID    The resource ID.
   *
   * @return  The URL string.
   *
   * @throws MalformedURLException  If an error occurred while constructing the
   *                                URL.
   */
  private static String getLocationURL(final HttpServletRequest request,
                                       final String resourceName,
                                       final String resourceID)
      throws MalformedURLException
  {
    final ScimURI relativeURI =
        new ScimURI(request.getContextPath(), resourceName, resourceID,
                    null, null, new SCIMQueryAttributes());

    final URL url = new URL(request.getRequestURL().toString());
    final URL locationURL = new URL(url.getProtocol(),
                                    url.getHost(),
                                    url.getPort(),
                                    relativeURI.toString());

    return locationURL.toString();
  }
}
