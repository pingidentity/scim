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



import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.unboundid.directory.sdk.common.api.AlertHandler;
import com.unboundid.directory.sdk.common.api.DiskSpaceConsumer;
import com.unboundid.directory.sdk.common.config.AlertHandlerConfig;
import com.unboundid.directory.sdk.common.types.AlertNotification;
import com.unboundid.directory.sdk.common.types.LogSeverity;
import com.unboundid.directory.sdk.common.types.RegisteredDiskSpaceConsumer;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.FileArgument;



/**
 * This class provides a simple example of an alert handler which will write
 * information about each alert notification to a file in a specified directory.
 * The file will be named with the alert ID. It has one configuration argument:
 * <UL>
 *   <LI>alert-directory -- The path to the directory in which files will be
 *       written about alerts that are generated.</LI>
 * </UL>
 */
public final class ExampleAlertHandler
       extends AlertHandler
       implements DiskSpaceConsumer
{
  /**
   * The name of the argument that will be used to specify the path to the
   * directory in which to write alert files.
   */
  private static final String ARG_NAME_ALERT_DIR = "alert-directory";



  // The current configuration for this alert handler.
  private volatile AlertHandlerConfig config;

  // The directory in which to write alert files.
  private volatile File alertDirectory;

  // The registered disk space consumer for this alert handler.
  private volatile RegisteredDiskSpaceConsumer registeredConsumer;

  // The server context for the server in which this extension is running.
  private ServerContext serverContext;



  /**
   * Creates a new instance of this alert handler.  All alert handler
   * implementations must include a default constructor, but any initialization
   * should generally be done in the {@code initializeAlertHandler} method.
   */
  public ExampleAlertHandler()
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
    return "Example Alert Handler";
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
      "This alert handler serves an example that may be used to demonstrate " +
           "the process for creating a third-party alert handler.  It will " +
           "create a file in a specified directory for each alert " +
           "generated within the server.  The file will be human-readable " +
           "but not particularly machine-parseable.",

      "Because this alert handler writes to disk, it also serves as an " +
           "example of a disk space consumer so that the server may track " +
           "available space on the disk containing the alert files and " +
           "potentially warn administrators if usable space becomes too low."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this alert handler.  The argument parser may also be
   * updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this alert handler.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the alert directory.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_ALERT_DIR;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{path}";
    String    description     = "The path to the directory in which alert " +
         "files should be written.  Relative paths will be relative to the " +
         "server root.";
    boolean   fileMustExist   = true;
    boolean   parentMustExist = true;
    boolean   mustBeFile      = false;
    boolean   mustBeDirectory = true;

    parser.addArgument(new FileArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, fileMustExist,
         parentMustExist, mustBeFile, mustBeDirectory));
  }



  /**
   * Initializes this alert handler.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this alert handler.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this alert handler.
   *
   * @throws  LDAPException  If a problem occurs while initializing this alert
   *                         handler.
   */
  @Override()
  public void initializeAlertHandler(final ServerContext serverContext,
                                     final AlertHandlerConfig config,
                                     final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning alert handler initialization");

    this.serverContext = serverContext;
    this.config        = config;

    // Get the path to the directory in which the alert files should be written.
    final FileArgument alertDirArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_ALERT_DIR);
    alertDirectory = alertDirArg.getValue();

    // Register as a disk space consumer since we will be writing to the
    // alert directory.
    registeredConsumer = serverContext.registerDiskSpaceConsumer(this);
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this alert
   *                              handler.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(final AlertHandlerConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    // No special validation is required.
    return true;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this alert
   *                               handler.
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
  public ResultCode applyConfiguration(final AlertHandlerConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the new path to the alert directory.  We don't really care if it's
    // the same as the directory that was already in use.
    final FileArgument alertDirArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_ALERT_DIR);
    alertDirectory = alertDirArg.getValue();

    // Cache an updated copy of the configuration.
    this.config = config;

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this alert handler is
   * to be taken out of service.
   */
  @Override()
  public void finalizeAlertHandler()
  {
    // Deregister the disk space consumer.
    if (registeredConsumer != null)
    {
      serverContext.deregisterDiskSpaceConsumer(registeredConsumer);
    }
  }



  /**
   * Performs any processing which may be necessary to handle the provided alert
   * notification.
   *
   * @param  alert  The alert notification generated within the server.
   */
  @Override()
  public void handleAlert(final AlertNotification alert)
  {
    // The filename for the alert will be the alert ID.  It will be unique, so
    // we can be certain there won't be any collisions.
    final File alertFile = new File(alertDirectory, alert.getAlertID());

    final PrintWriter writer;
    try
    {
      writer = new PrintWriter(alertFile);
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);

      // If we can't create the file, then we can't write anything about this
      // alert.  We don't want to generate an alert in response to this because
      // that could create an infinite loop.  The only thing we'll do is to log
      // an error message.
      serverContext.logMessage(LogSeverity.SEVERE_ERROR,
           "The example alert handler defined in configuration entry '" +
                config.getConfigObjectDN() + "' could not write file '" +
                alertFile.getAbsolutePath() + "' with information about " +
                "alert " + alert.toString() + ":  " +
                StaticUtils.getExceptionMessage(e));
      return;
    }

    // Write information about the alert to the file.
    writer.println("Alert ID:  " + alert.getAlertID());
    writer.println("Alert Type:  " + alert.getAlertTypeName());
    writer.println("Alert Severity:  " + alert.getAlertSeverity().toString());
    writer.println("Generated by Class:  " +
         alert.getAlertGeneratorClassName());
    writer.println("Alert Message:  " + alert.getAlertMessage());

    writer.close();
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
         Arrays.asList(ARG_NAME_ALERT_DIR + "=logs/alerts"),
         "Write information about any generated alerts into the logs/alerts " +
              "directory below the server root.");

    return exampleMap;
  }



  /**
   * Retrieves the name that should be used to identify this disk space
   * consumer.
   *
   * @return  The name that should be used to identify this disk space consumer.
   */
  public String getDiskSpaceConsumerName()
  {
    return "Example Alert Handler " + config.getConfigObjectName();
  }



  /**
   * Retrieves a list of filesystem paths in which this disk space consumer may
   * store files which may consume a significant amount of space.  It is
   * generally recommended that the paths be directories, but they may also be
   * individual files.
   *
   * @return  A list of filesystem paths in which this disk space consumer may
   *          store files which may consume a significant amount of space.
   */
  public List<File> getDiskSpaceConsumerPaths()
  {
    return Arrays.asList(alertDirectory);
  }
}
