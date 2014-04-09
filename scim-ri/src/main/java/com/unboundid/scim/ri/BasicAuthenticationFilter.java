/*
 * Copyright 2011-2014 UnboundID Corp.
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


package com.unboundid.scim.ri;

import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;

/**
 * Authenticates an end user via BASIC authentication scheme.
 */
public class BasicAuthenticationFilter implements Filter
{
  /**
   * Request wrapper to provide security info of the authenticated user.
   */
  private static final class SecurityRequestWrapper
      extends HttpServletRequestWrapper
  {
    private final Principal userPrincipal;

    /**
     * Construct a new security wrapper with the provided info.
     *
     * @param request The HttpServletRequest to wrap.
     * @param userName The name of the authenticated user.
     */
    private SecurityRequestWrapper(final HttpServletRequest request,
                                   final String userName)
    {
      super(request);
      userPrincipal = new UnboundIdPrincipal(userName);
    }

    @Override
    public String getAuthType() {
      return HttpServletRequest.BASIC_AUTH;
    }

    @Override
    public Principal getUserPrincipal() {
      return userPrincipal;
    }

    @Override
    public boolean isUserInRole(final String role) {
      return true;
    }
  }

  /**
   * Web container holder for the authenticated user.
   */
  private static final class UnboundIdPrincipal implements Principal
  {
    private String name;



    /**
     * Created a web container holder for the authenticated user.
     *
     * @param name the end user's principal name; i.e., username.
     */
    public UnboundIdPrincipal(final String name)
    {
      this.name = name;
    }



    /**
     * Returns The authenticated user's principal name.
     *
     * @return The principal name.
     */
    public String getName()
    {
      return name;
    }
  }

  /**
   * A SCIM backend through which authentication may be performed.
   */
  private final SCIMBackend scimBackend;

  /**
   * Construct a new BasicAuthenticationFilter that uses the provided
   * SCIMBackend to authenticate users.
   *
   * @param scimBackend The backend to use to authenticate users.
   */
  public BasicAuthenticationFilter(final SCIMBackend scimBackend)
  {
    this.scimBackend = scimBackend;
  }

  /**
   * {@inheritDoc}
   */
  public void destroy()
  {
    // Nothing to do.
  }

  /**
   * {@inheritDoc}
   */
  public void init(final FilterConfig filterConfig) throws ServletException
  {
    // Nothing to do.
  }

  /**
   * {@inheritDoc}
   */
  public void doFilter(final ServletRequest request,
                       final ServletResponse response,
                       final FilterChain chain)
      throws IOException, ServletException
  {
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    String header = httpRequest.getHeader("Authorization");
    String authID = null;
    if(header != null)
    {
      String[] authorization = header.split(" ");
      if(authorization.length == 2 &&
          authorization[0].equalsIgnoreCase("Basic") &&
          authorization[1].length() > 0)
      {
        try
        {
          String credentialsStr = Base64.decodeToString(authorization[1]);
          if(credentialsStr != null)
          {
            String[] credentials = credentialsStr.split(":", 2);
            if(credentials.length == 2)
            {
              if (scimBackend.authenticate(credentials[0], credentials[1]))
              {
                authID = credentials[0];
              }
            }
            else if(scimBackend.authenticate(credentials[0], ""))
            {
              authID = credentials[0];
            }
          }
        }
        catch(ParseException pe)
        {
          Debug.debugException(pe);
        }
      }
    }

    if(authID != null)
    {
      httpRequest = new SecurityRequestWrapper(httpRequest, authID);
    }
    else
    {
      ((HttpServletResponse)response).addHeader(
          "WWW-Authenticate", "Basic realm=SCIM");
    }
    chain.doFilter(httpRequest, response);
  }
}
