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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.unboundid.directory.sdk.common.api.DiskSpaceConsumer;
import com.unboundid.directory.sdk.common.operation.AddRequest;
import com.unboundid.directory.sdk.common.operation.AddResult;
import com.unboundid.directory.sdk.common.operation.DeleteRequest;
import com.unboundid.directory.sdk.common.operation.DeleteResult;
import com.unboundid.directory.sdk.common.operation.ModifyRequest;
import com.unboundid.directory.sdk.common.operation.ModifyResult;
import com.unboundid.directory.sdk.common.operation.ModifyDNRequest;
import com.unboundid.directory.sdk.common.operation.ModifyDNResult;
import com.unboundid.directory.sdk.common.types.AlertSeverity;
import com.unboundid.directory.sdk.common.types.CompletedOperationContext;
import com.unboundid.directory.sdk.common.types.Entry;
import com.unboundid.directory.sdk.common.types.RegisteredDiskSpaceConsumer;
import com.unboundid.directory.sdk.ds.api.ChangeSubscriptionHandler;
import com.unboundid.directory.sdk.ds.config.ChangeSubscriptionHandlerConfig;
import com.unboundid.directory.sdk.ds.types.ChangeSubscription;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.FileArgument;



/**
 * This class provides a simple example of a change subscription handler which
 * will write information about each matching change to a file in a specified
 * directory.  The file will be named with the change sequence number. It has
 * one configuration argument:
 * <UL>
 *   <LI>log-directory -- The path to the directory in which files will be
 *       written about matching changes.</LI>
 * </UL>
 */
public final class ExampleChangeSubscriptionHandler
       extends ChangeSubscriptionHandler
       implements DiskSpaceConsumer
{
  /**
   * The name of the argument that will be used to specify the path to the
   * directory in which to write log files.
   */
  private static final String ARG_NAME_LOG_DIR = "log-directory";



  // The general configuration for this change subscription handler.
  private volatile ChangeSubscriptionHandlerConfig config;

  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;

  // The directory in which to write log files.
  private volatile File logDirectory;

  // The registered disk space consumer for this change subscription handler.
  private volatile RegisteredDiskSpaceConsumer registeredConsumer;



  /**
   * Creates a new instance of this change subscription handler.  All change
   * subscription handler implementations must include a default constructor,
   * but any initialization should generally be done in the
   * {@code initializeChangeSubscriptionHandler} method.
   */
  public ExampleChangeSubscriptionHandler()
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
    return "Example Change Subscription Handler";
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
      "This change subscription handler serves an example that may be used " +
           "to demonstrate the process for creating a third-party change " +
           "subscription handler.  For each change processed in the server, " +
           "it will create a file with an LDIF representation of the change, " +
           "including comments providing additional information about the " +
           "operation that was processed.",

      "Because this change subscription handler writes to disk, it also " +
           "serves as an example of a disk space consumer so that the server " +
           "may track available space on the disk containing the log files " +
           "and potentially warn administrators if usable space becomes too " +
           "low."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this change subscription handler.  The argument parser
   * may also be updated to define relationships between arguments (e.g., to
   * specify required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this change subscription
   *                 handler.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the log directory.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_LOG_DIR;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{path}";
    String    description     = "The path to the directory in which log " +
         "files should be written with information about matching changes.  " +
         "Relative paths will be relative to the server root.";
    boolean   fileMustExist   = true;
    boolean   parentMustExist = true;
    boolean   mustBeFile      = false;
    boolean   mustBeDirectory = true;

    parser.addArgument(new FileArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, fileMustExist,
         parentMustExist, mustBeFile, mustBeDirectory));
  }



  /**
   * Initializes this change subscription handler.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this change
   *                        subscription handler.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this change subscription
   *                        handler.
   *
   * @throws  LDAPException  If a problem occurs while initializing this change
   *                         subscription handler.
   */
  @Override()
  public void initializeChangeSubscriptionHandler(
                   final DirectoryServerContext serverContext,
                   final ChangeSubscriptionHandlerConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo(
         "Beginning change subscription handler initialization");

    this.serverContext = serverContext;
    this.config        = config;

    // Get the path to the directory in which the log files should be written.
    final FileArgument logDirArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_LOG_DIR);
    logDirectory = logDirArg.getValue();

    // Register as a disk space consumer since we will be writing to the log
    // directory.
    registeredConsumer = serverContext.registerDiskSpaceConsumer(this);
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this change
   *                              subscription handler.
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
                      final ChangeSubscriptionHandlerConfig config,
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
   * @param  config                The general configuration for this change
   *                               subscription handler.
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
  public ResultCode applyConfiguration(
                         final ChangeSubscriptionHandlerConfig config,
                         final ArgumentParser parser,
                         final List<String> adminActionsRequired,
                         final List<String> messages)
  {
    this.config = config;

    // Get the new path to the log directory.  We don't really care if it's the
    // the same as the directory that was already in use.
    final FileArgument logDirArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_LOG_DIR);
    logDirectory = logDirArg.getValue();

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this change subscription
   * handler is to be taken out of service.
   */
  @Override()
  public void finalizeChangeSubscriptionHandler()
  {
    // Deregister as a disk space consumer.
    if (registeredConsumer != null)
    {
      serverContext.deregisterDiskSpaceConsumer(registeredConsumer);
    }
  }



  /**
   * Performs any processing necessary for an add operation matching the
   * subscription criteria.
   *
   * @param  operationContext     The context for the add operation.
   * @param  sequenceNumber       The sequence number for the change
   *                              subscription notification.
   * @param  changeSubscriptions  The set of change subscriptions whose criteria
   *                              matched the add operation.
   * @param  addRequest           Information about the request for the add
   *                              operation that was processed.
   * @param  addResult            Information about the result for the add
   *                              operation that was processed.
   * @param  entry                The entry that was added to the server.
   */
  @Override()
  public void addOperationProcessed(
                   final CompletedOperationContext operationContext,
                   final BigInteger sequenceNumber,
                   final Set<ChangeSubscription> changeSubscriptions,
                   final AddRequest addRequest,
                   final AddResult addResult, final Entry entry)
  {
    writeChange(operationContext, sequenceNumber, changeSubscriptions,
         addRequest.toLDIFChangeRecord());
  }



  /**
   * Performs any processing necessary for a delete operation matching the
   * subscription criteria.
   *
   * @param  operationContext     The context for the delete operation.
   * @param  sequenceNumber       The sequence number for the change
   *                              subscription notification.
   * @param  changeSubscriptions  The set of change subscriptions whose criteria
   *                              matched the delete operation.
   * @param  deleteRequest        Information about the request for the delete
   *                              operation that was processed.
   * @param  deleteResult         Information about the result for the delete
   *                              operation that was processed.
   * @param  entry                The entry that was removed from the server.
   */
  @Override()
  public void deleteOperationProcessed(
                   final CompletedOperationContext operationContext,
                   final BigInteger sequenceNumber,
                   final Set<ChangeSubscription> changeSubscriptions,
                   final DeleteRequest deleteRequest,
                   final DeleteResult deleteResult, final Entry entry)
  {
    writeChange(operationContext, sequenceNumber, changeSubscriptions,
         deleteRequest.toLDIFChangeRecord());
  }



  /**
   * Performs any processing necessary for a modify operation matching the
   * subscription criteria.
   *
   * @param  operationContext     The context for the modify operation.
   * @param  sequenceNumber       The sequence number for the change
   *                              subscription notification.
   * @param  changeSubscriptions  The set of change subscriptions whose criteria
   *                              matched the modify operation.
   * @param  modifyRequest        Information about the request for the modify
   *                              operation that was processed.
   * @param  modifyResult         Information about the result for the modify
   *                              operation that was processed.
   * @param  oldEntry             The entry as it appeared before the changes
   *                              were applied.
   * @param  newEntry             The entry as it appeared immediately after the
   *                              changes were applied.
   */
  @Override()
  public void modifyOperationProcessed(
                  final CompletedOperationContext operationContext,
                  final BigInteger sequenceNumber,
                  final Set<ChangeSubscription> changeSubscriptions,
                  final ModifyRequest modifyRequest,
                  final ModifyResult modifyResult,
                  final Entry oldEntry, final Entry newEntry)
  {
    writeChange(operationContext, sequenceNumber, changeSubscriptions,
         modifyRequest.toLDIFChangeRecord());
  }



  /**
   * Performs any processing necessary for a modify DN operation matching the
   * subscription criteria.
   *
   * @param  operationContext     The context for the modify DN operation.
   * @param  sequenceNumber       The sequence number for the change
   *                              subscription notification.
   * @param  changeSubscriptions  The set of change subscriptions whose criteria
   *                              matched the modify DN operation.
   * @param  modifyDNRequest      Information about the request for the modify
   *                              DN operation that was processed.
   * @param  modifyDNResult       Information about the result for the modify DN
   *                              operation that was processed.
   * @param  oldEntry             The entry as it appeared before being renamed.
   * @param  newEntry             The entry as it appeared immediately after
   *                              being renamed.
   */
  @Override()
  public void modifyDNOperationProcessed(
                   final CompletedOperationContext operationContext,
                   final BigInteger sequenceNumber,
                   final Set<ChangeSubscription> changeSubscriptions,
                   final ModifyDNRequest modifyDNRequest,
                   final ModifyDNResult modifyDNResult,
                   final Entry oldEntry, final Entry newEntry)
  {
    writeChange(operationContext, sequenceNumber, changeSubscriptions,
         modifyDNRequest.toLDIFChangeRecord());
  }



  /**
   * Writes information about the change to a file in the configured log
   * directory.
   *
   * @param  operationContext     The context for the associated operation.
   * @param  sequenceNumber       The sequence number for the change
   *                              subscription notification.
   * @param  changeSubscriptions  The set of change subscriptions whose criteria
   *                              matched the associated operation.
   * @param  changeRecord         The change record that was processed.
   */
  private void writeChange(final CompletedOperationContext operationContext,
                           final BigInteger sequenceNumber,
                           final Set<ChangeSubscription> changeSubscriptions,
                           final LDIFChangeRecord changeRecord)
  {
    // Determine the file that should be used for the change.  It will be named
    // based on the sequence number, which is guaranteed to be unique (even
    // across restarts).
    final File ldifFile =
         new File(logDirectory, sequenceNumber.toString() + ".ldif");


    // Open the file for writing.
    final PrintWriter w;
    try
    {
      w = new PrintWriter(ldifFile);
    }
    catch (final Exception e)
    {
      // About the only thing we can do here is to generate an alert to notify
      // administrators of the problem.
      serverContext.debugCaught(e);
      serverContext.sendAlert(AlertSeverity.ERROR,
           "Unable to write a record of change " + changeRecord.toString() +
                " to file " + ldifFile.getAbsolutePath() + ":  " +
                StaticUtils.getExceptionMessage(e));
      return;
    }


    // Write the header to the file.  Write it using LDIF comments so that the
    // change can be parsed using an LDIF reader.
    w.println("# Connection ID: " + operationContext.getConnectionID());
    w.println("# Operation ID: " + operationContext.getOperationID());
    w.println("# Requester DN: " + operationContext.getAuthorizationDN());
    w.println("# Request Time: " +
         new Date(operationContext.getProcessingStartTime()));
    w.println("# Processing Time (ns): " +
         new Date(operationContext.getProcessingTimeNanos()));
    for (final ChangeSubscription s : changeSubscriptions)
    {
      w.println("# Matches change subscription: " + s.getName());
    }


    // Write an LDIF representation of the change and close the file.
    w.println(changeRecord.toLDIFString());
    w.close();
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
         Arrays.asList(ARG_NAME_LOG_DIR + "=logs/change-notifications"),
         "Write information about any matching changes into the " +
              "logs/change-notifications directory below the server root.");

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
    return "Example Change Subscription Handler " +
         config.getConfigObjectName();
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
    return Arrays.asList(logDirectory);
  }
}
