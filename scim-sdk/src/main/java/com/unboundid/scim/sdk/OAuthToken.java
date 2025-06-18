/*
 * Copyright 2012-2025 Ping Identity Corporation
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


/**
 * This class represents an OAuth token, described by
 * <i>RFC 6750</i>. This class can be extended by clients
 * that need to add extra functionality, such as the ability to extract an
 * expiration date, scope, target audience, etc, from the token. These details
 * are implementation-specific.
 */
public class OAuthToken {
  /**
   * This enum defines the supported set of OAuth token types.
   */
  public enum Type {
    /**
     * OAuth 1.0 token type.
     */
    OAuth,
    /**
     * OAuth bearer token type.
     */
    Bearer
  }

  /**
   * The OAuth token type.
   */
  private final Type type;

  /**
   * The OAuth token value.
   */
  private final String tokenValue;

  /**
   * Constructs an OAuth 2.0 bearer token with the given b64token value. Note
   * that b64token is just an ABNF syntax definition and does not imply any
   * base64-encoding of the token value.
   *
   * @param tokenValue The bearer token value.
   */
  public OAuthToken(final String tokenValue)
  {
    this(Type.Bearer, tokenValue);
  }

  /**
   * Constructs an OAuthToken with the specified {@link Type} and token value.
   *
   * @param type The token Type.
   * @param tokenValue The token value.
   */
  public OAuthToken(final Type type, final String tokenValue)
  {
    this.type = type;
    this.tokenValue = tokenValue;
  }

  /**
   * Returns the token type.
   *
   * @return the token type.
   */
  public Type getType()
  {
    return type;
  }

  /**
   * Returns the token value.
   *
   * @return the token value.
   */
  public String getTokenValue()
  {
    return tokenValue;
  }

  /**
   * Returns a formatted representation of the token type and value for use as
   * an Authorization header value. For example, if this is a bearer token, this
   * method would return a String like "Bearer vF9dft4qmT".
   *
   * @return the token type and value in HTTP header value form.
   */
  public String getFormattedValue()
  {
    return getType().name() + " " + getTokenValue();
  }
}
