/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;

import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Principal;

/**
 * Authenticates an end user via an UnboundID authentication scheme.
 */
public class LDAPLoginService extends MappedLoginService {

  /**
   * Authenticates the requesting user via UnboundID authentication scheme.
   *
   * @param username    The end user's principal name; i.e., username or uid.
   * @param credentials The end user's password.
   * @return A Jetty holder for the authenticated user.
   */
  @Override
  public UserIdentity login(final String username, final Object credentials) {
    UserIdentity login = super.login(username, credentials);
    if (login == null) {
      // user is not authenticated - authenticate them via ldap, then hand back
      // a userIdentity
      Principal userPrincipal = new UnboundIdPrincipal(username);
      Subject subject = new Subject();
      subject.getPrincipals().add(userPrincipal);
      UserIdentity userIdentity =
        this.getIdentityService().newUserIdentity(subject, userPrincipal,
          new String[]{});
      // todo: the super's load user handling is bizarre. probably should load
      // the user manually
      return userIdentity;
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  protected UserIdentity loadUser(final String username) {
    // no-op for the moment
    return null;
  }

  /**
   * {@inheritDoc}
   */
  protected void loadUsers() throws IOException {
    // no-op
  }

  /**
   * Web container holder for the authenticated user.
   */
  class UnboundIdPrincipal implements Principal {
    private String name;

    /**
     * Created a web container holder for the authenticated user.
     *
     * @param name the end user's principal name; i.e., username.
     */
    public UnboundIdPrincipal(final String name) {
      this.name = name;
    }

    /**
     * Returns The authenticated user's principal name.
     *
     * @return The principal name.
     */
    public String getName() {
      return name;
    }
  }
}
