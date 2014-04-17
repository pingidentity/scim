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

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PLAINBindRequest;
import com.unboundid.ldap.sdk.ResultCode;
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
import java.util.logging.Level;

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
   * SCIMBackend to bind users.
   *
   * @param scimBackend The backend to use to bind users.
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
    BindResult result = null;
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
            if(credentials.length == 2 &&
                ((credentials[0].isEmpty() && credentials[1].isEmpty()) ||
                    (!credentials[0].isEmpty() && !credentials[1].isEmpty())))
            {
              // Either username and password are all empty, or they are all
              // non-empty.
              authID = getSASLAuthenticationID(credentials[0]);
              String password = credentials[1];

              final BindRequest bindRequest =
                  new PLAINBindRequest(authID, password);
              if(scimBackend instanceof InMemoryLDAPBackend)
              {
                result =
                    ((InMemoryLDAPBackend)scimBackend).bind(bindRequest);
              }
              else if(scimBackend instanceof ExternalLDAPBackend)
              {
                result =
                    ((ExternalLDAPBackend)scimBackend).bind(bindRequest);
              }
            }
          }
        }
        catch(Exception pe)
        {
          Debug.debugException(pe);
        }
      }
    }

    if(result != null && result.getResultCode().equals(ResultCode.SUCCESS))
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

  /**
   * Retrieve a SASL Authentication ID from a HTTP Basic Authentication user ID.
   * We need this because the HTTP Authentication user ID can not include the
   * ':' character.
   *
   * @param userID  The HTTP user ID for which a SASL Authentication ID is
   *                required. It may be {@code null} if the request was not
   *                authenticated.
   *
   * @return  A SASL Authentication ID.
   */
  static String getSASLAuthenticationID(final String userID)
  {
    if (userID == null || userID.isEmpty())
    {
      return "dn:";
    }

    if(userID.startsWith("dn:") || userID.startsWith("u:"))
    {
      // If the user ID is already prefixed, just return.
      return userID;
    }

    // If the user ID can be parsed as a DN then prefix it with "dn:", otherwise
    // prefix it with "u:".
    try
    {
      final DN dn = new DN(userID);

      return "dn:" + dn.toString();
    }
    catch (LDAPException e)
    {
      Debug.debugException(Level.FINE, e);
      return "u:" + userID;
    }
  }
}
