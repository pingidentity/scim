/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * docs/licenses/cddl.txt
 * or http://www.opensource.org/licenses/cddl1.php.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * docs/licenses/cddl.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2010-2011 UnboundID Corp.
 */
package com.unboundid.directory.sdk.examples.groovy;



import java.util.List;

import com.unboundid.directory.sdk.proxy.config.LDAPHealthCheckConfig;
import com.unboundid.directory.sdk.proxy.scripting.ScriptedLDAPHealthCheck;
import com.unboundid.directory.sdk.proxy.types.BackendServer;
import com.unboundid.directory.sdk.proxy.types.CompletedProxyOperationContext;
import com.unboundid.directory.sdk.proxy.types.HealthCheckResult;
import com.unboundid.directory.sdk.proxy.types.HealthCheckState;
import com.unboundid.directory.sdk.proxy.types.ProxyServerContext;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.DNArgument;
import com.unboundid.util.args.IntegerArgument;



/**
 * This class provides a simple example of an LDAP health check which simply
 * attempts to retrieve a specified entry from the backend server.  The length
 * of time required to retrieve the entry will be used to help determine the
 * health check score.  It has the following configuration arguments:
 * <UL>
 *   <LI>entry-dn -- The DN of the entry to retrieve.</LI>
 *   <LI>max-available-response-time -- The maximum search duration to consider
 *       a server available.  Any duration longer than this will cause the
 *       server to be considered either degraded or unavailable.</LI>
 *   <LI>max-degraded-response-time -- The maximum search duration to consider a
 *       server degraded.  Any duration longer than this will cause the server
 *       to be considered unavailable.</LI>
 * </UL>
 */
public final class ExampleScriptedLDAPHealthCheck
       extends ScriptedLDAPHealthCheck
{
  /**
   * The name of the argument that will be used to specify the DN of the entry
   * to retrieve.
   */
  private static final String ARG_NAME_ENTRY_DN = "entry-dn";



  /**
   * The name of the argument that will be used to specify the maximum available
   * response time.
   */
  private static final String ARG_NAME_MAX_AVAILABLE_DURATION =
       "max-available-response-time-millis";



  /**
   * The name of the argument that will be used to specify the maximum degraded
   * response time.
   */
  private static final String ARG_NAME_MAX_DEGRADED_DURATION =
       "max-degraded-response-time-millis";



  // The maximum available duration.
  private volatile long maxAvailableDuration;

  // The maximum degraded duration.
  private volatile long maxDegradedDuration;

  // The server context for the server in which this extension is running.
  private ProxyServerContext serverContext;

  // The DN of the entry to retrieve.
  private volatile String entryDN;



  /**
   * Creates a new instance of this LDAP health check.  All LDAP health check
   * implementations must include a default constructor, but any initialization
   * should generally be done in the {@code initializeLDAPHealthCheck} method.
   */
  public ExampleScriptedLDAPHealthCheck()
  {
    // No implementation required.
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this LDAP health check.  The argument parser may also
   * be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this LDAP health check.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the target entry.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_ENTRY_DN;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{dn}";
    String    description     = "The DN of the entry to retrieve during the " +
         "course of health check processing.";

    parser.addArgument(new DNArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));


    // Add an argument that allows you to specify the maximum available
    // duration.
    shortIdentifier = null;
    longIdentifier  = ARG_NAME_MAX_AVAILABLE_DURATION;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{duration}";
    description     = "The maximum length of time that a health check search " +
         "may take for a server to be considered available.  The value " +
         "should consist of an integer followed by a time unit (e.g., " +
         "'10 ms').";

    int     lowerBound   = 1;
    int     upperBound   = Integer.MAX_VALUE;
    Integer defaultValue = null;

    parser.addArgument(new IntegerArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, lowerBound,
         upperBound, defaultValue));


    // Add an argument that allows you to specify the maximum degraded
    // duration.
    shortIdentifier = null;
    longIdentifier  = ARG_NAME_MAX_DEGRADED_DURATION;
    required        = true;
    placeholder     = "{duration}";
    description     = "The maximum length of time that a health check search " +
         "may take for a server to be considered degraded.  The value " +
         "should consist of an integer followed by a time unit (e.g., " +
         "'10 ms').";
    lowerBound      = 1;
    upperBound      = Integer.MAX_VALUE;
    defaultValue    = null;

    parser.addArgument(new IntegerArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, lowerBound,
         upperBound, defaultValue));
  }



  /**
   * Initializes this LDAP health check.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this LDAP health
   *                        check.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this LDAP health check.
   *
   * @throws  LDAPException  If a problem occurs while initializing this LDAP
   *                         health check.
   */
  @Override()
  public void initializeLDAPHealthCheck(final ProxyServerContext serverContext,
                                        final LDAPHealthCheckConfig config,
                                        final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning LDAP health check initialization");

    this.serverContext = serverContext;

    // Get the target entry DN.
    entryDN = ((DNArgument)
         parser.getNamedArgument(ARG_NAME_ENTRY_DN)).getValue().toString();

    // Get the maximum available response time.
    maxAvailableDuration = ((IntegerArgument)
         parser.getNamedArgument(ARG_NAME_MAX_AVAILABLE_DURATION)).getValue();

    // Get the maximum degraded response time.
    maxDegradedDuration = ((IntegerArgument)
         parser.getNamedArgument(ARG_NAME_MAX_DEGRADED_DURATION)).getValue();


    // The maximum available response time must be less than or equal to the
    // maximum degraded response time.
    if (maxAvailableDuration > maxDegradedDuration)
    {
      throw new LDAPException(ResultCode.PARAM_ERROR,
           "The maximum available duration must be less than or equal to the " +
                "maximum degraded duration.");
    }
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this LDAP health
   *                              check.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(final LDAPHealthCheckConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    // The maximum available response time must be less than or equal to the
    // maximum degraded response time.
    // Get the target entry DN.
    final int maxAvailable = ((IntegerArgument)
         parser.getNamedArgument(ARG_NAME_MAX_AVAILABLE_DURATION)).getValue();

    final int maxDegraded = ((IntegerArgument)
         parser.getNamedArgument(ARG_NAME_MAX_DEGRADED_DURATION)).getValue();

    if (maxAvailable > maxDegraded)
    {
      unacceptableReasons.add("The maximum available duration must be less " +
           "than or equal to the maximum degraded duration.");
      acceptable = false;
    }


    return acceptable;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this LDAP
   *                               health check.
   * @param  parser                The argument parser which has been
   *                               initialized with the new configuration.
   * @param  adminActionsRequired  A list that can be updated with information
   *                               about any administrative actions that may be
   *                               required before one or more of the
   *                               configuration changes will be applied.
   * @param  messages              A list that can be updated with information
   *                               about the result of applying the new
   *                               configuration.
   *
   * @return  A result code that provides information about the result of
   *          attempting to apply the configuration change.
   */
  @Override()
  public ResultCode applyConfiguration(final LDAPHealthCheckConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the target entry DN.
    final String newDN = ((DNArgument)
         parser.getNamedArgument(ARG_NAME_ENTRY_DN)).getValue().toString();

    // Get the maximum available response time.
    final int newAvailable = ((IntegerArgument)
         parser.getNamedArgument(ARG_NAME_MAX_AVAILABLE_DURATION)).getValue();

    // Get the maximum degraded response time.
    final int newDegraded = ((IntegerArgument)
         parser.getNamedArgument(ARG_NAME_MAX_DEGRADED_DURATION)).getValue();


    entryDN              = newDN;
    maxAvailableDuration = newAvailable;
    maxDegradedDuration  = newDegraded;

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this LDAP health check is
   * to be taken out of service.
   */
  @Override()
  public void finalizeLDAPHealthCheck()
  {
    // No finalization is required.
  }



  /**
   * Attempts to determine the health of the provided LDAP external server whose
   * last health check result indicated that the server had a state of
   * AVAILABLE.  This method may be periodically invoked for AVAILABLE servers
   * to determine whether their state has changed.
   *
   * @param  backendServer  A handle to the LDAP external server whose health is
   *                        to be assessed.
   * @param  connection     A connection that may be used to communicate with
   *                        the server in the course of performing the
   *                        evaluation.  The health check should not do anything
   *                        which may alter the state of this connection.
   *
   * @return  Information about the result of the health check.
   */
  @Override()
  public HealthCheckResult checkAvailableServer(
                                final BackendServer backendServer,
                                final LDAPConnection connection)
  {
    return checkServer(connection);
  }



  /**
   * Attempts to determine the health of the provided LDAP external server whose
   * last health check result indicated that the server had a state of DEGRADED.
   * This method may be periodically invoked for DEGRADED servers to determine
   * whether their state has changed.
   *
   * @param  backendServer  A handle to the LDAP external server whose health is
   *                        to be assessed.
   * @param  connection     A connection that may be used to communicate with
   *                        the server in the course of performing the
   *                        evaluation.  The health check should not do anything
   *                        which may alter the state of this connection.
   *
   * @return  Information about the result of the health check.
   */
  @Override()
  public HealthCheckResult checkDegradedServer(
                                final BackendServer backendServer,
                                final LDAPConnection connection)
  {
    return checkServer(connection);
  }



  /**
   * Attempts to determine the health of the provided LDAP external server whose
   * last health check result indicated that the server had a state of
   * UNAVAILABLE.  This method may be periodically invoked for UNAVAILABLE
   * servers to determine whether their state has changed.
   *
   * @param  backendServer  A handle to the LDAP external server whose health is
   *                        to be assessed.
   * @param  connection     A connection that may be used to communicate with
   *                        the server in the course of performing the
   *                        evaluation.  The health check should not do anything
   *                        which may alter the state of this connection.
   *
   * @return  Information about the result of the health check.
   */
  @Override()
  public HealthCheckResult checkUnavailableServer(
                                final BackendServer backendServer,
                                final LDAPConnection connection)
  {
    return checkServer(connection);
  }



  /**
   * Attempts to determine the health of the provided LDAP external server in
   * which an attempted operation did not complete successfully.
   *
   * @param  operationContext  A handle to the operation context for the
   *                           operation that failed.
   * @param  exception         The exception caught when attempting to process
   *                           the associated operation in the backend server.
   * @param  backendServer     A handle to the backend server in which the
   *                           operation was processed.
   *
   * @return  Information about the result of the health check.
   */
  @Override()
  public HealthCheckResult checkFailedOperation(
              final CompletedProxyOperationContext operationContext,
              final LDAPException exception,
              final BackendServer backendServer)
  {
    // Look at the result code to see if it indicates that the server might not
    // be available.
    if (exception.getResultCode().isConnectionUsable())
    {
      // The result code indicates that the connection is probably usable, so
      // we'll just return whatever the last known result was.
      return backendServer.getHealthCheckResult();
    }


    // The server might not be usable.  See if we can establish a connection to
    // it.
    final LDAPConnection connection;
    try
    {
      connection = backendServer.createNewConnection(null,
           "Example Health Check for failed operation " +
                operationContext.toString());
    }
    catch (final Exception e)
    {
      // We can't establish a connection, so we have to consider the server
      // unavailable.
      serverContext.debugCaught(e);
      return serverContext.createHealthCheckResult(
           HealthCheckState.UNAVAILABLE, 0,
           "Unable to establish a connection to the server:  " +
                StaticUtils.getExceptionMessage(e));
    }


    // Use the connection to perform the health check.
    try
    {
      return checkServer(connection);
    }
    finally
    {
      connection.close();
    }
  }



  /**
   * Performs a search to assess the health of the backend server using the
   * given connection.
   *
   * @param  connection  The connection to use to communicate with the server.
   *
   * @return  The health check result representing the assessed health of the
   *          server.
   */
  private HealthCheckResult checkServer(final LDAPConnection connection)
  {
    // Create local copies of the config variables.
    final String dn = entryDN;
    final long maxA = maxAvailableDuration;
    final long maxD = maxDegradedDuration;


    // Construct a search request to use for the health check.
    final SearchRequest searchRequest = new SearchRequest(dn, SearchScope.BASE,
         Filter.createPresenceFilter("objectClass"),
         SearchRequest.NO_ATTRIBUTES);
    searchRequest.setResponseTimeoutMillis(maxDegradedDuration);


    // Get the start time.
    final long startTime = System.currentTimeMillis();


    // Perform a search to retrieve the target entry
    SearchResult searchResult;
    try
    {
      searchResult = connection.search(searchRequest);
    }
    catch (final LDAPSearchException lse)
    {
      serverContext.debugCaught(lse);
      searchResult = lse.getSearchResult();
    }


    // Get the stop time.
    final long stopTime = System.currentTimeMillis();


    // If the result code is anything other than success, then we'll consider
    // the server unavailable.
    if (searchResult.getResultCode() != ResultCode.SUCCESS)
    {
      return serverContext.createHealthCheckResult(
           HealthCheckState.UNAVAILABLE, 0,
           "Example health check search returned a non-success result of " +
                searchResult.toString());
    }


    // Figure out how long the search took and use that to determine the state
    // and score to use for the server.
    final long elapsedTime = stopTime - startTime;
    if (elapsedTime <= maxA)
    {
      final int score = (int) Math.round(10.0d - (10.0d * elapsedTime / maxA));
      return serverContext.createHealthCheckResult(
           HealthCheckState.AVAILABLE, score);
    }
    else if (elapsedTime <= maxD)
    {
      final int score = (int) Math.round(10.0d - (10.0d * elapsedTime / maxD));
      return serverContext.createHealthCheckResult(
           HealthCheckState.DEGRADED, score,
           "Example health check duration exceeded the maximum available " +
                "response time of " + maxA + "ms");
    }
    else
    {
      return serverContext.createHealthCheckResult(
           HealthCheckState.UNAVAILABLE, 0,
           "Example health check duration exceeded the maximum degraded " +
                "response time of " + maxD + "ms");
    }
  }
}
