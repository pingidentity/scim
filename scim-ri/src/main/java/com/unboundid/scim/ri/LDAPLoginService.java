/*
 * Copyright 2011 UnboundID Corp.
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

import com.unboundid.scim.sdk.SCIMBackend;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Principal;



/**
 * Authenticates an end user via an UnboundID authentication scheme.
 */
public class LDAPLoginService extends MappedLoginService
{

  /**
   * A SCIM backend through which authentication may be performed.
   */
  private final SCIMBackend scimBackend;

  /**
   * Create a new login service that authenticates through a SCIM backend.
   *
   * @param scimBackend  A SCIM backend through which authentication may be
   *                     performed.
   */
  public LDAPLoginService(final SCIMBackend scimBackend)
  {
    this.scimBackend = scimBackend;
  }



  /**
   * Authenticates the requesting user via UnboundID authentication scheme.
   *
   * @param username    The end user's principal name; i.e., username or uid.
   * @param credentials The end user's password.
   * @return A Jetty holder for the authenticated user.
   */
  @Override
  public UserIdentity login(final String username, final Object credentials)
  {
    UserIdentity userIdentity = super.login(username, credentials);
    if (userIdentity == null)
    {
      // user is not authenticated - authenticate them, then hand back
      // a userIdentity

      String password;
      if (credentials instanceof char[])
      {
        password = new String((char[])credentials);
      }
      else
      {
        password = credentials.toString();
      }

      if (scimBackend.authenticate(username, password))
      {
        final Principal userPrincipal = new UnboundIdPrincipal(username);
        final Subject subject = new Subject();
        subject.getPrincipals().add(userPrincipal);
        userIdentity =
            this.getIdentityService().newUserIdentity(subject, userPrincipal,
                                                      new String[]{});
        // todo: the super's load user handling is bizarre. probably should load
        // the user manually
      }
    }

    return userIdentity;
  }



  /**
   * {@inheritDoc}
   */
  protected UserIdentity loadUser(final String username)
  {
    // no-op for the moment
    return null;
  }



  /**
   * {@inheritDoc}
   */
  protected void loadUsers() throws IOException
  {
    // no-op
  }



  /**
   * Web container holder for the authenticated user.
   */
  class UnboundIdPrincipal implements Principal
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
}
