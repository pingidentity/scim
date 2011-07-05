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
import java.net.InetAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.security.auth.x500.X500Principal;

import com.unboundid.directory.sdk.common.config.AccessLoggerConfig;
import com.unboundid.directory.sdk.common.operation.AbandonRequest;
import com.unboundid.directory.sdk.common.operation.AddRequest;
import com.unboundid.directory.sdk.common.operation.AddResult;
import com.unboundid.directory.sdk.common.operation.BindResult;
import com.unboundid.directory.sdk.common.operation.CompareRequest;
import com.unboundid.directory.sdk.common.operation.CompareResult;
import com.unboundid.directory.sdk.common.operation.DeleteRequest;
import com.unboundid.directory.sdk.common.operation.DeleteResult;
import com.unboundid.directory.sdk.common.operation.ExtendedRequest;
import com.unboundid.directory.sdk.common.operation.ExtendedResult;
import com.unboundid.directory.sdk.common.operation.GenericResult;
import com.unboundid.directory.sdk.common.operation.ModifyRequest;
import com.unboundid.directory.sdk.common.operation.ModifyResult;
import com.unboundid.directory.sdk.common.operation.ModifyDNRequest;
import com.unboundid.directory.sdk.common.operation.ModifyDNResult;
import com.unboundid.directory.sdk.common.operation.SASLBindRequest;
import com.unboundid.directory.sdk.common.operation.SearchRequest;
import com.unboundid.directory.sdk.common.operation.SearchResult;
import com.unboundid.directory.sdk.common.operation.SimpleBindRequest;
import com.unboundid.directory.sdk.common.operation.UnbindRequest;
import com.unboundid.directory.sdk.common.scripting.ScriptedAccessLogger;
import com.unboundid.directory.sdk.common.types.ClientContext;
import com.unboundid.directory.sdk.common.types.CompletedOperationContext;
import com.unboundid.directory.sdk.common.types.CompletedSearchOperationContext;
import com.unboundid.directory.sdk.common.types.DisconnectReason;
import com.unboundid.directory.sdk.common.types.Entry;
import com.unboundid.directory.sdk.common.types.ForwardTarget;
import com.unboundid.directory.sdk.common.types.OperationContext;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.IntermediateResponse;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.FileArgument;



/**
 * This class provides a simple example of a scripted access logger that will
 * append a message to a specified file for any type of client communication.
 * It takes a single configuration argument:
 * <UL>
 *   <LI>log-file -- The path to the log file that will be written.  This must
 *       be provided.</LI>
 * </UL>
 */
public final class ExampleScriptedAccessLogger
       extends ScriptedAccessLogger
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
   * Creates a new instance of this access logger.  All access logger
   * implementations must include a default constructor, but any initialization
   * should generally be done in the {@code initializeAccessLogger} method.
   */
  public ExampleScriptedAccessLogger()
  {
    loggerLock = new Object();
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this access logger.  The argument parser may also
   * be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this access logger.
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
   * Initializes this access logger.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this access logger.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this access logger.
   *
   * @throws  LDAPException  If a problem occurs while initializing this access
   *                         logger.
   */
  @Override()
  public void initializeAccessLogger(final ServerContext serverContext,
                                     final AccessLoggerConfig config,
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
   * @param  config               The general configuration for this access
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
  public boolean isConfigurationAcceptable(final AccessLoggerConfig config,
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
   * @param  config                The general configuration for this access
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
  public ResultCode applyConfiguration(final AccessLoggerConfig config,
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
   * Performs any cleanup which may be necessary when this access logger is
   * to be taken out of service.
   */
  @Override()
  public void finalizeAccessLogger()
  {
    synchronized (loggerLock)
    {
      writer.close();
      writer = null;
      logFile = null;
    }
  }



  /**
   * Logs a message indicating that a new connection has been established.
   *
   * @param  clientContext  Information about the client connection that has
   *                        been accepted.
   */
  @Override()
  public void logConnect(final ClientContext clientContext)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" CONNECT conn=");
    buffer.append(clientContext.getConnectionID());

    final InetAddress clientAddress = clientContext.getClientInetAddress();
    if (clientAddress != null)
    {
      buffer.append(" from=\"");
      buffer.append(clientAddress.getHostAddress());
      buffer.append('"');
    }

    final InetAddress serverAddress = clientContext.getServerInetAddress();
    if (serverAddress != null)
    {
      buffer.append(" to=");
      buffer.append(serverAddress.getHostAddress());
      buffer.append('"');
    }

    buffer.append(" protocol=\"");
    buffer.append(clientContext.getProtocol());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message indicating that a connection has been closed.
   *
   * @param  clientContext     Information about the client connection that has
   *                           been closed.
   * @param  disconnectReason  A general reason that the connection has been
   *                           closed.
   * @param  message           A message with additional information about the
   *                           closure.  It may be {@code null} if none is
   *                           available.
   */
  @Override()
  public void logDisconnect(final ClientContext clientContext,
                            final DisconnectReason disconnectReason,
                            final String message)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" DISCONNECT conn=");
    buffer.append(clientContext.getConnectionID());

    buffer.append(" reason=\"");
    buffer.append(disconnectReason.getClosureMessage());
    buffer.append('"');

    if (message != null)
    {
      buffer.append(" message=\"");
      buffer.append(message);
      buffer.append('"');
    }

    write(buffer);
  }



  /**
   * Logs a message about a certificate chain presented by a client.
   *
   * @param  clientContext  Information about the client that presented the
   *                        certificate chain.
   * @param  certChain      The certificate chain presented by the client.
   * @param  authDN         The DN of the user as whom the client was
   *                        automatically authenticated, or {@code null} if the
   *                        client was not automatically authenticated.
   */
  @Override()
  public void logClientCertificateChain(final ClientContext clientContext,
                                        final Certificate[] certChain,
                                        final String authDN)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" CERTIFICATE conn=");
    buffer.append(clientContext.getConnectionID());

    if (certChain.length > 0)
    {
      final Certificate cert = certChain[0];
      if (cert instanceof X509Certificate)
      {
        final X509Certificate c = (X509Certificate) cert;
        final String subject =
             c.getSubjectX500Principal().getName(X500Principal.RFC2253);
        buffer.append(" subject=\"");
        buffer.append(subject);
        buffer.append('"');
      }
    }

    if (authDN != null)
    {
      buffer.append(" authDN=\"");
      buffer.append(authDN);
      buffer.append('"');
    }

    write(buffer);
  }



  /**
   * Logs a message about an abandon request received from a client.
   *
   * @param  opContext  The operation context for the abandon operation.
   * @param  request    The abandon request that was received.
   */
  @Override()
  public void logAbandonRequest(final OperationContext opContext,
                                final AbandonRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" ABANDON REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" idToAbandon=");
    buffer.append(request.getIDToAbandon());

    write(buffer);
  }



  /**
   * Logs a message about an abandon request that will be forwarded to another
   * server.
   *
   * @param  opContext  The operation context for the abandon operation.
   * @param  request    The abandon request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logAbandonForward(final OperationContext opContext,
                                final AbandonRequest request,
                                final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about the result of processing an abandon request.
   *
   * @param  opContext  The operation context for the abandon operation.
   * @param  request    The abandon request that was received.
   * @param  result     The result of processing the abandon request.
   */
  @Override()
  public void logAbandonResult(final CompletedOperationContext opContext,
                               final AbandonRequest request,
                               final GenericResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about an add request received from a client.
   *
   * @param  opContext  The operation context for the add operation.
   * @param  request    The add request that was received.
   */
  @Override()
  public void logAddRequest(final OperationContext opContext,
                            final AddRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" ADD REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" dn=\"");
    buffer.append(request.getEntry().getDN());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about an add request that will be forwarded to another
   * server.
   *
   * @param  opContext  The operation context for the add operation.
   * @param  request    The add request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logAddForward(final OperationContext opContext,
                            final AddRequest request,
                            final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward an
   * add request to another server.
   *
   * @param  opContext  The operation context for the add operation.
   * @param  request    The add request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logAddForwardFailure(final OperationContext opContext,
                                   final AddRequest request,
                                   final ForwardTarget target,
                                   final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about the result of processing an add request.
   *
   * @param  opContext  The operation context for the add operation.
   * @param  request    The add request that was received.
   * @param  result     The result of processing the add request.
   */
  @Override()
  public void logAddResponse(final CompletedOperationContext opContext,
                             final AddRequest request,
                             final AddResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about a simple bind request received from a client.
   *
   * @param  opContext  The operation context for the bind operation.
   * @param  request    The bind request that was received.
   */
  @Override()
  public void logBindRequest(final OperationContext opContext,
                             final SimpleBindRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" BIND REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" authType=SIMPLE dn=\"");
    buffer.append(" dn=\"");
    buffer.append(request.getDN());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about a simple bind request that will be forwarded to
   * another server.
   *
   * @param  opContext  The operation context for the bind operation.
   * @param  request    The bind request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logBindForward(final OperationContext opContext,
                                final SimpleBindRequest request,
                                final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward a
   * simple bind request to another server.
   *
   * @param  opContext  The operation context for the bind operation.
   * @param  request    The bind request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logBindForwardFailure(final OperationContext opContext,
                                    final SimpleBindRequest request,
                                    final ForwardTarget target,
                                    final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about the result of processing a simple bind request.
   *
   * @param  opContext  The operation context for the bind operation.
   * @param  request    The bind request that was received.
   * @param  result     The result of processing the bind request.
   */
  @Override()
  public void logBindResponse(final CompletedOperationContext opContext,
                              final SimpleBindRequest request,
                              final BindResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about a SASL bind request received from a client.
   *
   * @param  opContext  The operation context for the bind operation.
   * @param  request    The bind request that was received.
   */
  @Override()
  public void logBindRequest(final OperationContext opContext,
                             final SASLBindRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" BIND REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" authType=SASL mechanism=\"");
    buffer.append(request.getSASLMechanism());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about a SASL bind request that will be forwarded to
   * another server.
   *
   * @param  opContext  The operation context for the bind operation.
   * @param  request    The bind request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logBindForward(final OperationContext opContext,
                                final SASLBindRequest request,
                                final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward a
   * SASL bind request to another server.
   *
   * @param  opContext  The operation context for the bind operation.
   * @param  request    The bind request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logBindForwardFailure(final OperationContext opContext,
                                    final SASLBindRequest request,
                                    final ForwardTarget target,
                                    final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about the result of processing a SASL bind request.
   *
   * @param  opContext  The operation context for the bind operation.
   * @param  request    The bind request that was received.
   * @param  result     The result of processing the bind request.
   */
  @Override()
  public void logBindResponse(final CompletedOperationContext opContext,
                              final SASLBindRequest request,
                              final BindResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about a compare request received from a client.
   *
   * @param  opContext  The operation context for the compare operation.
   * @param  request    The compare request that was received.
   */
  @Override()
  public void logCompareRequest(final OperationContext opContext,
                                final CompareRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" COMPARE REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" dn=\"");
    buffer.append(request.getDN());
    buffer.append('"');

    buffer.append(" dn=\"");
    buffer.append(request.getDN());
    buffer.append("\" attribute=\"");
    buffer.append(request.getAttributeType());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about a compare request that will be forwarded to another
   * server.
   *
   * @param  opContext  The operation context for the compare operation.
   * @param  request    The compare request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logCompareForward(final OperationContext opContext,
                                final CompareRequest request,
                                final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward a
   * compare request to another server.
   *
   * @param  opContext  The operation context for the compare operation.
   * @param  request    The compare request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logCompareForwardFailure(final OperationContext opContext,
                                       final CompareRequest request,
                                       final ForwardTarget target,
                                       final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about the result of processing a compare request.
   *
   * @param  opContext  The operation context for the compare operation.
   * @param  request    The compare request that was received.
   * @param  result     The result of processing the compare request.
   */
  @Override()
  public void logCompareResponse(final CompletedOperationContext opContext,
                                 final CompareRequest request,
                                 final CompareResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about a delete request received from a client.
   *
   * @param  opContext  The operation context for the delete operation.
   * @param  request    The delete request that was received.
   */
  @Override()
  public void logDeleteRequest(final OperationContext opContext,
                               final DeleteRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" DELETE REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" dn=\"");
    buffer.append(request.getDN());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about a delete request that will be forwarded to another
   * server.
   *
   * @param  opContext  The operation context for the delete operation.
   * @param  request    The delete request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logDeleteForward(final OperationContext opContext,
                               final DeleteRequest request,
                               final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward a
   * delete request to another server.
   *
   * @param  opContext  The operation context for the delete operation.
   * @param  request    The delete request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logDeleteForwardFailure(final OperationContext opContext,
                                      final DeleteRequest request,
                                      final ForwardTarget target,
                                      final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about the result of processing a delete request.
   *
   * @param  opContext  The operation context for the delete operation.
   * @param  request    The delete request that was received.
   * @param  result     The result of processing the delete request.
   */
  @Override()
  public void logDeleteResponse(final CompletedOperationContext opContext,
                                final DeleteRequest request,
                                final DeleteResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about an extended request received from a client.
   *
   * @param  opContext  The operation context for the extended operation.
   * @param  request    The extended request that was received.
   */
  @Override()
  public void logExtendedRequest(final OperationContext opContext,
                                 final ExtendedRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" EXTENDED REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" oid=\"");
    buffer.append(request.getRequestOID());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about an extended request that will be forwarded to another
   * server.
   *
   * @param  opContext  The operation context for the extended operation.
   * @param  request    The extended request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logExtendedForward(final OperationContext opContext,
                                 final ExtendedRequest request,
                                 final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward an
   * extended request to another server.
   *
   * @param  opContext  The operation context for the extended operation.
   * @param  request    The extended request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logExtendedForwardFailure(final OperationContext opContext,
                                        final ExtendedRequest request,
                                        final ForwardTarget target,
                                        final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about the result of processing an extended request.
   *
   * @param  opContext  The operation context for the extended operation.
   * @param  request    The extended request that was received.
   * @param  result     The result of processing the extended request.
   */
  @Override()
  public void logExtendedResponse(final CompletedOperationContext opContext,
                                  final ExtendedRequest request,
                                  final ExtendedResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about a modify request received from a client.
   *
   * @param  opContext  The operation context for the modify operation.
   * @param  request    The modify request that was received.
   */
  @Override()
  public void logModifyRequest(final OperationContext opContext,
                               final ModifyRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" MODIFY REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" dn=\"");
    buffer.append(request.getDN());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about a modify request that will be forwarded to another
   * server.
   *
   * @param  opContext  The operation context for the modify operation.
   * @param  request    The modify request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logModifyForward(final OperationContext opContext,
                               final ModifyRequest request,
                               final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward a
   * modify request to another server.
   *
   * @param  opContext  The operation context for the modify operation.
   * @param  request    The modify request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logModifyForwardFailure(final OperationContext opContext,
                                      final ModifyRequest request,
                                      final ForwardTarget target,
                                      final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about the result of processing a modify request.
   *
   * @param  opContext  The operation context for the modify operation.
   * @param  request    The modify request that was received.
   * @param  result     The result of processing the modify request.
   */
  @Override()
  public void logModifyResponse(final CompletedOperationContext opContext,
                                final ModifyRequest request,
                                final ModifyResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about a modify DN request received from a client.
   *
   * @param  opContext  The operation context for the modify DN operation.
   * @param  request    The modify DN request that was received.
   */
  @Override()
  public void logModifyDNRequest(final OperationContext opContext,
                                 final ModifyDNRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" MODIFY_DN REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" dn=\"");
    buffer.append(request.getDN());
    buffer.append("\" newRDN=\"");
    buffer.append(request.getNewRDN());
    buffer.append("\" deleteOldRDN=");
    buffer.append(request.deleteOldRDN());

    final String newSuperior = request.getNewSuperiorDN();
    if (newSuperior != null)
    {
      buffer.append(" newSuperior=\"");
      buffer.append(newSuperior);
      buffer.append('"');
    }

    write(buffer);
  }



  /**
   * Logs a message about a modify DN request that will be forwarded to another
   * server.
   *
   * @param  opContext  The operation context for the modify DN operation.
   * @param  request    The modify DN request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logModifyDNForward(final OperationContext opContext,
                                 final ModifyDNRequest request,
                                 final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward a
   * modify DN request to another server.
   *
   * @param  opContext  The operation context for the modify DN operation.
   * @param  request    The modify DN request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logModifyDNForwardFailure(final OperationContext opContext,
                                        final ModifyDNRequest request,
                                        final ForwardTarget target,
                                        final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about the result of processing a modify DN request.
   *
   * @param  opContext  The operation context for the modify DN operation.
   * @param  request    The modify DN request that was received.
   * @param  result     The result of processing the modify DN request.
   */
  @Override()
  public void logModifyDNResponse(final CompletedOperationContext opContext,
                                  final ModifyDNRequest request,
                                  final ModifyDNResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about a search request received from a client.
   *
   * @param  opContext  The operation context for the search operation.
   * @param  request    The search request that was received.
   */
  @Override()
  public void logSearchRequest(final OperationContext opContext,
                               final SearchRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" SEARCH REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" base=\"");
    buffer.append(request.getBaseDN());
    buffer.append("\" scope=");
    buffer.append(request.getScope().intValue());
    buffer.append(" filter=\"");
    buffer.append(request.getFilter().toString());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about a search request that will be forwarded to another
   * server.
   *
   * @param  opContext  The operation context for the search operation.
   * @param  request    The search request that was received.
   * @param  target     Information about the server to which the request will
   *                    be forwarded.
   */
  @Override()
  public void logSearchForward(final OperationContext opContext,
                               final SearchRequest request,
                               final ForwardTarget target)
  {
    writeForward(opContext, target);
  }



  /**
   * Logs a message about a failure encountered while attempting to forward a
   * search request to another server.
   *
   * @param  opContext  The operation context for the search operation.
   * @param  request    The search request that was received.
   * @param  target     Information about the server to which the request was
   *                    forwarded.
   * @param  failure    The exception that was received when attempting to
   *                    forward the request.
   */
  @Override()
  public void logSearchForwardFailure(final OperationContext opContext,
                                      final SearchRequest request,
                                      final ForwardTarget target,
                                      final LDAPException failure)
  {
    writeForwardFailure(opContext, target, failure);
  }



  /**
   * Logs a message about a search result entry that was returned to the client.
   *
   * @param  opContext  The operation context for the search operation.
   * @param  request    The search request that was received.
   * @param  entry      The entry that was returned.
   * @param  controls   The set of controls included with the entry, or an empty
   *                    list if there were none.
   */
  @Override()
  public void logSearchResultEntry(final OperationContext opContext,
                                   final SearchRequest request,
                                   final Entry entry,
                                   final List<Control> controls)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" SEARCH ENTRY conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" dn=\"");
    buffer.append(entry.getDN());
    buffer.append('"');

    write(buffer);
  }



  /**
   * Logs a message about a search result reference that was returned to the
   * client.
   *
   * @param  opContext     The operation context for the search operation.
   * @param  request       The search request that was received.
   * @param  referralURLs  The referral URLs for the reference that was
   *                       returned.
   * @param  controls      The set of controls included with the reference, or
   *                       an empty list if there were none.
   */
  @Override()
  public void logSearchResultReference(final OperationContext opContext,
                                       final SearchRequest request,
                                       final List<String> referralURLs,
                                       final List<Control> controls)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" SEARCH REFERENCE conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" referralURLs={");
    final Iterator<String> iterator = referralURLs.iterator();
    while (! iterator.hasNext())
    {
      buffer.append('"');
      buffer.append(iterator.next());
      buffer.append('"');

      if (iterator.hasNext())
      {
        buffer.append(", ");
      }
    }
    buffer.append('}');

    write(buffer);
  }



  /**
   * Logs a message about the result of processing a search request.
   *
   * @param  opContext  The operation context for the search operation.
   * @param  request    The search request that was received.
   * @param  result     The result of processing the search request.
   */
  @Override()
  public void logSearchResultDone(
                   final CompletedSearchOperationContext opContext,
                   final SearchRequest request, final SearchResult result)
  {
    writeResult(opContext, result);
  }



  /**
   * Logs a message about an unbind request received from a client.
   *
   * @param  opContext  The operation context for the unbind operation.
   * @param  request    The unbind request that was received.
   */
  @Override()
  public void logUnbindRequest(final OperationContext opContext,
                               final UnbindRequest request)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" UNBIND REQUEST conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    write(buffer);
  }



  /**
   * Logs a message about an intermediate response that was returned to the
   * client.
   *
   * @param  opContext             The operation context for the associated
   *                               operation.
   * @param  intermediateResponse  The intermediate response that was returned.
   */
  @Override()
  public void logIntermediateResponse(final OperationContext opContext,
                   final IntermediateResponse intermediateResponse)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(" INTERMEDIATE RESPONSE conn=");
    buffer.append(opContext.getConnectionID());

    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    final String oid = intermediateResponse.getOID();
    if (oid != null)
    {
      buffer.append(" oid=\"");
      buffer.append(oid);
      buffer.append('"');
    }

    final String name = intermediateResponse.getIntermediateResponseName();
    if ((name != null) && (! name.equals(oid)))
    {
      buffer.append(" name=\"");
      buffer.append(name);
      buffer.append('"');
    }

    final String valueStr = intermediateResponse.valueToString();
    if (valueStr != null)
    {
      buffer.append(" value=\"");
      buffer.append(valueStr);
      buffer.append('"');
    }

    write(buffer);
  }



  /**
   * Logs information about an operation to be forwarded.
   *
   * @param  opContext  The operation context for the operation.
   * @param  target     The forward target for the operation.
   */
  private void writeForward(final OperationContext opContext,
                            final ForwardTarget target)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(' ');
    buffer.append(opContext.getOperationType().getOperationName());
    buffer.append(" FORWARD conn=");
    buffer.append(opContext.getConnectionID());
    buffer.append(" op=");
    buffer.append(opContext.getOperationID());
    buffer.append(" targetAddress=");
    buffer.append(target.getForwardTargetAddress());
    buffer.append(" targetPort=");
    buffer.append(target.getForwardTargetPort());

    write(buffer);
  }



  /**
   * Logs information about a failed forward attempt.
   *
   * @param  opContext  The operation context for the operation.
   * @param  target     The forward target for the operation.
   * @param  failure    The exception caught when trying to forward the
   *                    operation.
   */
  private void writeForwardFailure(final OperationContext opContext,
                                   final ForwardTarget target,
                                   final LDAPException failure)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(' ');
    buffer.append(opContext.getOperationType().getOperationName());
    buffer.append(" FORWARD FAILURE conn=");
    buffer.append(opContext.getConnectionID());
    buffer.append(" op=");
    buffer.append(opContext.getOperationID());
    buffer.append(" targetAddress=");
    buffer.append(target.getForwardTargetAddress());
    buffer.append(" targetPort=");
    buffer.append(target.getForwardTargetPort());
    buffer.append(" resultCode=");
    buffer.append(failure.getResultCode());

    final String diagnosticMessage = failure.getDiagnosticMessage();
    if (diagnosticMessage != null)
    {
      buffer.append(" diagnosticMessage=\"");
      buffer.append(diagnosticMessage);
      buffer.append('"');
    }

    final String matchedDN = failure.getMatchedDN();
    if (matchedDN != null)
    {
      buffer.append(" matchedDN=\"");
      buffer.append(matchedDN);
      buffer.append('"');
    }

    final String[] referralURLs = failure.getReferralURLs();
    if ((referralURLs != null) && (referralURLs.length > 0))
    {
      buffer.append(" referralURLs={");
      for (int i=0; i < referralURLs.length; i++)
      {
        if (i > 0)
        {
          buffer.append(", ");
        }

        buffer.append('"');
        buffer.append(referralURLs[i]);
        buffer.append('"');
      }
      buffer.append('}');
    }

    write(buffer);
  }



  /**
   * Logs information about the result of the provided operation.
   *
   * @param  opContext  The operation context for the operation.
   * @param  result     The result to be logged.
   */
  private void writeResult(final CompletedOperationContext opContext,
                           final GenericResult result)
  {
    final StringBuilder buffer = new StringBuilder();

    buffer.append(new Date().toString());

    buffer.append(' ');
    buffer.append(opContext.getOperationType().getOperationName());
    buffer.append(" RESULT conn=");
    buffer.append(opContext.getConnectionID());
    buffer.append(" op=");
    buffer.append(opContext.getOperationID());

    buffer.append(" resultCode=");
    buffer.append(result.getResultCode());

    final String diagnosticMessage = result.getDiagnosticMessage();
    if (diagnosticMessage != null)
    {
      buffer.append(" diagnosticMessage=\"");
      buffer.append(diagnosticMessage);
      buffer.append('"');
    }

    final String matchedDN = result.getMatchedDN();
    if (matchedDN != null)
    {
      buffer.append(" matchedDN=\"");
      buffer.append(matchedDN);
      buffer.append('"');
    }

    final List<String> referralURLs = result.getReferralURLs();
    if ((referralURLs != null) && (! referralURLs.isEmpty()))
    {
      buffer.append(" referralURLs={");
      final Iterator<String> iterator = referralURLs.iterator();
      while (! iterator.hasNext())
      {
        buffer.append('"');
        buffer.append(iterator.next());
        buffer.append('"');

        if (iterator.hasNext())
        {
          buffer.append(", ");
        }
      }
      buffer.append('}');
    }

    final String additionalMessage = result.getAdditionalLogMessage();
    if (additionalMessage != null)
    {
      buffer.append(" additionalLogMessage=\"");
      buffer.append(additionalMessage);
      buffer.append('"');
    }

    buffer.append(" elapsedTimeMillis=");
    buffer.append(opContext.getProcessingTimeMillis());

    if (opContext instanceof CompletedSearchOperationContext)
    {
      final CompletedSearchOperationContext c =
           (CompletedSearchOperationContext) opContext;
      buffer.append(" entriesReturned=");
      buffer.append(c.getEntryCount());
      buffer.append(" referencesReturned=");
      buffer.append(c.getReferenceCount());
    }

    if (result instanceof ExtendedResult)
    {
      final ExtendedResult r = (ExtendedResult) result;
      final String resultOID = r.getResultOID();
      if (resultOID != null)
      {
        buffer.append(" resultOID=\"");
        buffer.append(resultOID);
        buffer.append('"');
      }
    }

    write(buffer);
  }



  /**
   * Writes the contents of the provided buffer to the logger.
   *
   * @param  buffer  The buffer containing the data to write.
   */
  private void write(final StringBuilder buffer)
  {
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
