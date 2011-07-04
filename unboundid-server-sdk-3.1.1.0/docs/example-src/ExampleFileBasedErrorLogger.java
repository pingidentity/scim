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
package com.unboundid.directory.sdk.examples;



import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.unboundid.directory.sdk.common.api.FileBasedErrorLogger;
import com.unboundid.directory.sdk.common.config.FileBasedErrorLoggerConfig;
import com.unboundid.directory.sdk.common.types.LogCategory;
import com.unboundid.directory.sdk.common.types.LogSeverity;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;



/**
 * This class provides a simple example of a file-based error logger that will
 * format a provided set of error log messages.  It does not require any
 * configuration arguments.
 */
public final class ExampleFileBasedErrorLogger
       extends FileBasedErrorLogger
{
  /**
   * Creates a new instance of this file-based error logger.  All file-based
   * error logger implementations must include a default constructor, but any
   * initialization should generally be done in the
   * {@code initializeErrorLogger} method.
   */
  public ExampleFileBasedErrorLogger()
  {
    // No implementation required.
  }



  /**
   * Retrieves a human-readable name for this extension.
   *
   * @return  A human-readable name for this extension.
   */
  @Override()
  public String getExtensionName()
  {
    return "Example File-Based Error Logger";
  }



  /**
   * Retrieves a human-readable description for this extension.  Each element
   * of the array that is returned will be considered a separate paragraph in
   * generated documentation.
   *
   * @return  A human-readable description for this extension, or {@code null}
   *          or an empty array if no description should be available.
   */
  @Override()
  public String[] getExtensionDescription()
  {
    return new String[]
    {
      "This error logger serves an example that may be used to demonstrate " +
           "the process for creating a third-party file-based error " +
           "logger.  It will generate a simple log message for each kind of " +
           "interaction with the client."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this error logger.  The argument parser may also
   * be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this error logger.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // This logger does not require any configuration arguments.
  }



  /**
   * Initializes this error logger.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this error logger.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this error logger.
   *
   * @throws  LDAPException  If a problem occurs while initializing this error
   *                         logger.
   */
  @Override()
  public void initializeErrorLogger(final ServerContext serverContext,
                                    final FileBasedErrorLoggerConfig config,
                                    final ArgumentParser parser)
         throws LDAPException
  {
    // No initialization is required.  All of the work of setting up the log
    // file writer and registering as a disk space consumer will be handled by
    // the server.
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this error
   *                              logger.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(
                      final FileBasedErrorLoggerConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    // No special validation is required, so we don't need to do anything here.
    return true;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this error
   *                               logger.
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
  public ResultCode applyConfiguration(final FileBasedErrorLoggerConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // This logger does not define any custom configuration arguments, so no
    // action is required.
    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this error logger is
   * to be taken out of service.
   */
  @Override()
  public void finalizeErrorLogger()
  {
    // All work required for shutting down the log writer will be handled by
    // the server, and no other finalization is required.
  }



  /**
   * Records information about the provided message, if appropriate.
   *
   * @param  category   The category for the message to be logged.
   * @param  severity   The severity for the message to be logged.
   * @param  messageID  The unique identifier with which the message text is
   *                    associated.
   * @param  message    The message to be logged.
   *
   * @return  The content of the message that should be logged.
   */
  @Override()
  public CharSequence logError(final LogCategory category,
                               final LogSeverity severity, final long messageID,
                               final String message)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date());
    buffer.append(" severity=");
    buffer.append(severity.name());
    buffer.append(" message=\"");
    buffer.append(message);
    buffer.append('"');

    return buffer;
  }



  /**
   * Retrieves a map containing examples of configurations that may be used for
   * this extension.  The map key should be a list of sample arguments, and the
   * corresponding value should be a description of the behavior that will be
   * exhibited by the extension when used with that configuration.
   *
   * @return  A map containing examples of configurations that may be used for
   *          this extension.  It may be {@code null} or empty if there should
   *          not be any example argument sets.
   */
  @Override()
  public Map<List<String>,String> getExamplesArgumentSets()
  {
    final LinkedHashMap<List<String>,String> exampleMap =
         new LinkedHashMap<List<String>,String>(1);

    exampleMap.put(
         Arrays.<String>asList(),
         "Write error log messages to the associated log file.");

    return exampleMap;
  }
}