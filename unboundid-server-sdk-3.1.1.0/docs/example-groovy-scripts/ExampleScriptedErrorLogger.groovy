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



import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

import com.unboundid.directory.sdk.common.config.ErrorLoggerConfig;
import com.unboundid.directory.sdk.common.scripting.ScriptedErrorLogger;
import com.unboundid.directory.sdk.common.types.LogCategory;
import com.unboundid.directory.sdk.common.types.LogSeverity;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.FileArgument;



/**
 * This class provides a simple example of a scripted error logger that will
 * append a message to a specified file about any errors, warnings, or events in
 * the server.  It takes a single configuration argument:
 * <UL>
 *   <LI>log-file -- The path to the log file that will be written.  This must
 *       be provided.</LI>
 * </UL>
 */
public final class ExampleScriptedErrorLogger
       extends ScriptedErrorLogger
{
  /**
   * The name of the argument that will be used for the argument used to specify
   * the path to the log file.
   */
  private static final String ARG_NAME_LOG_FILE = "log-file";



  // The path to the log file to be written.
  private volatile File logFile;

  // The lock that will be used to synchronize logging activity.
  private final Object loggerLock;

  // The print writer that will be used to actually write the log messages.
  private volatile PrintWriter writer;

  // The server context for the server in which this extension is running.
  private ServerContext serverContext;



  /**
   * Creates a new instance of this error logger.  All error logger
   * implementations must include a default constructor, but any initialization
   * should generally be done in the {@code initializeErrorLogger} method.
   */
  public ExampleScriptedErrorLogger()
  {
    loggerLock = new Object();
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
    // Add an argument that allows you to specify the path to the log file.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_LOG_FILE;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{path}";
    String    description     = "The path to the log file to be written.  " +
         "Non-absolute paths will be treated as relative to the server root.";
    boolean   fileMustExist   = false;
    boolean   parentMustExist = true;
    boolean   mustBeFile      = true;
    boolean   mustBeDirectory = false;

    parser.addArgument(new FileArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, fileMustExist,
         parentMustExist, mustBeFile, mustBeDirectory));
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
                                    final ErrorLoggerConfig config,
                                    final ArgumentParser parser)
         throws LDAPException
  {
    this.serverContext = serverContext;


    // Create the logger and open the log file.
    try
    {
      final FileArgument arg =
           (FileArgument) parser.getNamedArgument(ARG_NAME_LOG_FILE);
      logFile = arg.getValue();
      writer  = new PrintWriter(new FileWriter(logFile, true));
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);
      throw new LDAPException(ResultCode.OTHER,
           "Unable to open file " + logFile.getAbsolutePath() +
                " for writing:  " + StaticUtils.getExceptionMessage(e),
           e);
    }
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
  public boolean isConfigurationAcceptable(final ErrorLoggerConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    // The argument parser will handle all of the necessary validation, so
    // we don't need to do anything here.
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
  public ResultCode applyConfiguration(final ErrorLoggerConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the path to the log file from the new config.
    final FileArgument arg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_LOG_FILE);
    final File newFile = arg.getValue();

    // If the log file path hasn't changed, then we don't need to do anything.
    if (newFile.equals(logFile))
    {
      return ResultCode.SUCCESS;
    }

    // Create a print writer that can be used to write to the new log file.
    final PrintWriter newWriter;
    try
    {
      newWriter = new PrintWriter(new FileWriter(newFile, true));
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);
      messages.add("Unable to open new log file " + newFile.getAbsolutePath() +
           " for writing:  " + StaticUtils.getExceptionMessage(e));
      return ResultCode.OTHER;
    }

    // Swap the new logger into place.
    final PrintWriter oldWriter;
    synchronized (loggerLock)
    {
      oldWriter = writer;
      writer = newWriter;
      logFile = newFile;
    }

    // Close the old logger and return success.
    oldWriter.close();
    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this error logger is
   * to be taken out of service.
   */
  @Override()
  public void finalizeErrorLogger()
  {
    synchronized (loggerLock)
    {
      writer.close();
      writer = null;
      logFile = null;
    }
  }



  /**
   * Records information about the provided message, if appropriate.
   *
   * @param  category   The category for the message to be logged.
   * @param  severity   The severity for the message to be logged.
   * @param  messageID  The unique identifier with which the message text is
   *                    associated.
   * @param  message    The message to be logged.
   */
  @Override()
  public void logError(final LogCategory category, final LogSeverity severity,
                       final long messageID, final String message)
  {
    final StringBuilder buffer = new StringBuilder();
    buffer.append(new Date());
    buffer.append(" severity=");
    buffer.append(severity.name());
    buffer.append(" message=\"");
    buffer.append(message);
    buffer.append('"');

    synchronized (loggerLock)
    {
      if (writer == null)
      {
        // This should only happen if the logger has been shut down.
        return;
      }

      writer.println(buffer.toString());
    }
  }
}
