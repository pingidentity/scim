/*
 * Copyright 2012 UnboundID Corp.
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
 * This class defines the validation status of a OAuth 2.0 bearer token, and
 * allows a OAuthTokenHandler extension to insert an error description and
 * OAuth scope value to be returned to the client.
 */
public final class OAuthTokenStatus
{
  /**
   * Possible values for the error_code.
   */
  public static enum ErrorCode
  {
    /**
     * Indicates that the OAuth token is valid.
     */
    OK(200),

    /**
     * Indicates that the OAuth token is expired, revoked, malformed, or invalid
     * for other reasons.
     */
    INVALID_TOKEN(401),

    /**
     * Indicates that the request requires higher privileges than provided by
     * the OAuth token.
     */
    INSUFFICIENT_SCOPE(403);


    private final int code;

    /**
     * Creates an ErrorCode instance with the given HTTP status code.
     *
     * @param code The HTTP status code.
     */
    ErrorCode(final int code)
    {
      this.code = code;
    }

    /**
     * Get the HTTP status code associated with this ErrorCode.
     *
     * @return the integer HTTP status code for this ErrorCode.
     */
    public int getHttpStatusCode()
    {
      return code;
    }
  }

  private final ErrorCode code;
  private final String description;
  private final String scope;

  /**
   * Constructs an OAuthTokenStatus with the given ErrorCode.
   *
   * @param errorCode the {@link ErrorCode} to use. This must not be
   *                  {@code null}.
   */
  public OAuthTokenStatus(final ErrorCode errorCode)
  {
    this(errorCode, null, null);
  }

  /**
   * Constructs an OAuthTokenStatus with the given ErrorCode and error
   * description.
   *
   * @param errorCode the {@link ErrorCode} to use. This must not be
   *                  {@code null}.
   * @param errorDescription a human-readable description of the error. This may
   *                         be {@code null}.
   */
  public OAuthTokenStatus(final ErrorCode errorCode,
                          final String errorDescription)
  {
    this(errorCode, errorDescription, null);
  }

  /**
   * Constructs an OAuthTokenStatus with the given ErrorCode, error
   * description, and scope value.
   *
   * @param errorCode the {@link ErrorCode} to use. This must not be
   *                  {@code null}.
   * @param errorDescription a human-readable description of the error. This may
   *                         be {@code null}.
   * @param scope a space-delimited list of case-sensitive scope values
   *              indicating the required scope of the access token for
   *              accessing the requested resource. Scope values are
   *              implementation defined; there is no centralized registry for
   *              them; allowed values are defined by the authorization server.
   *              The order of scope values is not significant. This may be
   *              {@code null}.
   */
  public OAuthTokenStatus(final ErrorCode errorCode,
                          final String errorDescription,
                          final String scope)
  {
    if(errorCode == null)
    {
      throw new NullPointerException("The errorCode parameter may not be null");
    }
    this.code = errorCode;
    this.description = errorDescription;
    this.scope = scope;
  }

  /**
   * Gets the specified ErrorCode for this OAuthTokenStatus.
   *
   * @return an {@link ErrorCode} instance.
   */
  public ErrorCode getErrorCode()
  {
    return code;
  }

  /**
   * Gets the specified error description for this OAuthTokenStatus.
   *
   * @return a human-readable explanation of the error condition, or
   *         {@code null} if none was specified.
   */
  public String getErrorDescription()
  {
    return description;
  }

  /**
   * Gets the specified OAuth scope for this OAuthTokenStatus.
   *
   * @return a space-delimited list of case-sensitive scope values indicating
   *         the required scope of the access token for accessing the requested
   *         resource. Scope values are implementation defined; there is no
   *         centralized registry for them; allowed values are defined by the
   *         authorization server. The order of scope values is not significant.
   *         This may return {@code null} if no scope was specified.
   */
  public String getScope()
  {
    return scope;
  }
}
