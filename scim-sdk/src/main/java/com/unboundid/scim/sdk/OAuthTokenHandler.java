/*
 * Copyright 2012-2014 UnboundID Corp.
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

package com.unboundid.scim.sdk;

import java.security.GeneralSecurityException;


/**
 * This class defines an API that must be implemented by extensions which will
 * handle incoming SCIM requests with OAuth 2.0 bearer token authentication.
 * The OAuthTokenHandler is responsible for decoding the bearer token and
 * checking it for authenticity and validity.
 * <BR><BR>
 * OAuth provides a method for clients to access a protected resource on
 * behalf of a resource owner. In the general case, before a client can
 * access a protected resource, it must first obtain an authorization
 * grant from the resource owner and then exchange the authorization
 * grant for an access token. The access token represents the grant's
 * scope, duration, and other attributes specified by the authorization
 * grant. The client accesses the protected resource by presenting the
 * access token to the resource server (i.e. the Identity Data Store or Identity
 * Proxy with the SCIM HTTP Servlet enabled).
 * <BR><BR>
 * The access token provides an abstraction, replacing different
 * authorization constructs (e.g., username and password, assertion) for
 * a single token understood by the resource server. This abstraction
 * enables issuing access tokens valid for a short time period, as well
 * as removing the resource server's need to understand a wide range of
 * authentication schemes. See "OAuth 2.0 Authorization Framework: Bearer
 * Token Usage" (<i>RFC 6750</i>) for the full
 * specification and details.
 * <BR><BR>
 * TLS security is required to use OAuth 2.0 bearer tokens, as specified in
 * <i>RFC 6750</i>. A bearer token may be used by any party
 * in possession of that token (the "bearer"), and thus needs to be protected
 * when transmitted across the network. Implementations of this API should take
 * special care to verify that the token came from a trusted source (using a
 * secret key or some other signing mechanism to prove that the token is
 * authentic). Please read "OAuth 2.0 Threat Model and Security Considerations"
 * (<i>RFC 6819</i>) for a comprehensive list of
 * security threats to consider when working with OAuth bearer tokens.
 * <BR><BR>
 * The OAuthTokenHandler is also responsible for extracting an authorization DN
 * from the bearer token (or otherwise providing one), which will be used to
 * apply access controls before returning the protected resource. There are also
 * methods to extract the expiration date of the token as well as verify that
 * the intended audience is this server (to deal with token redirect).
 * <BR><BR>
 * The order these methods are called by the SCIM HTTP Servlet Extension is as
 * follows:
 * <ol>
 *   <li><i>decodeOAuthToken()</i></li>
 *   <li><i>isTokenAuthentic()</i></li>
 *   <li><i>isTokenForThisServer()</i></li>
 *   <li><i>isTokenExpired()</i></li>
 *   <li><i>validateToken()</i></li>
 *   <li><i>getAuthzDN()</i></li>
 * </ol>
 * If any of the methods fail or return an error result, the server will return
 * an appropriate "unauthorized" response to the client.
 */
public interface OAuthTokenHandler
{
  /**
   * Creates an {@link OAuthToken} instance from the incoming token value.
   * <p>
   * Implementers may choose to return a subclass of {@link OAuthToken} in
   * order to provide convenience methods for interacting with the token. This
   * can be helpful because the returned {@link OAuthToken} is passed to all of
   * the other methods in this class.
   *
   * @param rawTokenValue the b64token token value. Note that b64token is just
   *                      an ABNF syntax definition and does not imply any
   *                      base64-encoding of the token value.
   * @return a {@link OAuthToken} instance. This must not be {@code null}.
   * @throws GeneralSecurityException if there is an error decoding the token
   */
  OAuthToken decodeOAuthToken(final String rawTokenValue)
              throws GeneralSecurityException;



  /**
   * Determines whether the given token is expired.
   *
   * @param token the OAuth 2.0 bearer token.
   * @return {@code true} if the token is already expired, {@code false} if not.
   * @throws GeneralSecurityException if there is an error determining the
   *         token's expiration date
   */
   boolean isTokenExpired(final OAuthToken token)
              throws GeneralSecurityException;



  /**
   * Determines whether the incoming token is authentic (i.e. that it came from
   * a trusted authorization server and not an attacker). Implementers are
   * encouraged to use signed tokens and use this method to verify the
   * signature, but other methods such as symmetric key encryption (using a
   * shared secret) can be used as well.
   *
   * @param token the OAuth 2.0 bearer token.
   * @return {@code true} if the bearer token can be verified as authentic and
   *         originating from a trusted source, {@code false} if not.
   * @throws GeneralSecurityException if there is an error determining whether
   *         the token is authentic
   */
  boolean isTokenAuthentic(final OAuthToken token)
              throws GeneralSecurityException;



  /**
   * Determines whether the incoming token is targeted for this server. This
   * allows the implementation to reject the token early in the validation
   * process if it can see that the intended recipient was not this server.
   *
   * @param token the OAuth 2.0 bearer token.
   * @return {@code true} if the bearer token identifies this server as the
   *         intended recipient, {@code false} if not.
   * @throws GeneralSecurityException if there is an error determining whether
   *         the token is for this server
   */
  boolean isTokenForThisServer(final OAuthToken token)
              throws GeneralSecurityException;



  /**
   * Determines whether the incoming token is valid for the given request. This
   * method should verify that the token is legitimate and grants access to the
   * requested resource specified in the {@link SCIMRequest}. This typically
   * involves checking the token scope and any other attributes granted by the
   * authorization grant. Implementations may need to call back to the
   * authorization server to verify that the token is still valid and has not
   * been revoked.
   *
   * @param token the OAuth 2.0 bearer token.
   * @param scimRequest the {@link SCIMRequest} that we are validating.
   * @return an {@link OAuthTokenStatus} object which indicates whether the
   *         bearer token is valid and grants access to the target resource.
   *         This must not be {@code null}.
   * @throws GeneralSecurityException if there is an error determining whether
   *         the token is valid
   */
  OAuthTokenStatus validateToken(final OAuthToken token,
                                 final SCIMRequest scimRequest)
              throws GeneralSecurityException;



  /**
   * Extracts the DN of the authorization entry (for which to apply access
   * controls) from the incoming token.
   * <p>
   * This may require performing an LDAP search in order to find the DN that
   * matches a certain attribute value contained in the token.
   *
   * @param token the OAuth 2.0 bearer token.
   * @return the authorization DN to use. This must not return {@code null}.
   * @throws GeneralSecurityException if there is an error determining the
   *         authorization user DN
   */
  String getAuthzDN(final OAuthToken token) throws GeneralSecurityException;
}
