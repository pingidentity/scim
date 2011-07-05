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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.TrustManager;

import com.unboundid.directory.sdk.common.api.TrustManagerProvider;
import com.unboundid.directory.sdk.common.config.TrustManagerProviderConfig;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.StringArgument;
import com.unboundid.util.ssl.TrustStoreTrustManager;



/**
 * This class provides a simple example of a trust manager provider which will
 * obtain a trust manager from a standard Java keystore file.  It has three
 * configuration arguments:
 * <UL>
 *   <LI>trust-store-file -- The path to the Java keystore file.</LI>
 *   <LI>trust-store-pin-file -- The path to a text file containing the PIN to
 *       use to access the contents of the Java keystore.</LI>
 *   <LI>trust-store-format -- The format for the Java keystore file.</LI>
 * </UL>
 */
public final class ExampleTrustManagerProvider
       extends TrustManagerProvider
{
  /**
   * The name of the argument that will be used to specify the path to the
   * keystore PIN file.
   */
  private static final String ARG_NAME_PIN_PATH = "trust-store-pin-file";



  /**
   * The name of the argument that will be used to specify the path to the
   * keystore file.
   */
  private static final String ARG_NAME_STORE_PATH = "trust-store-file";



  /**
   * The name of the argument that will be used to specify the keystore format.
   */
  private static final String ARG_NAME_STORE_FORMAT = "trust-store-format";



  // The path to the keystore file.
  private volatile File trustStorePath;

  // The path to the keystore PIN file.
  private volatile FileArgument trustStorePINPathArg;

  // The server context for the server in which this extension is running.
  private ServerContext serverContext;

  // The format of the Java keystore file.
  private volatile String trustStoreFormat;



  /**
   * Creates a new instance of this trust manager provider.  All trust manager
   * provider implementations must include a default constructor, but any
   * initialization should generally be done in the
   * {@code initializeTrustManagerProvider} method.
   */
  public ExampleTrustManagerProvider()
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
    return "Example Trust Manager Provider";
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
      "This trust manager provider serves as an example that may be used to " +
           "demonstrate the process for creating a third-party trust manager " +
           "provider.  It will use a standard Java key store file in order " +
           "to make the determination about whether to trust a presented " +
           "certificate."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this trust manager provider.  The argument parser may
   * also be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this trust manager provider.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the trust store path.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_STORE_PATH;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{path}";
    String    description     = "The path to the Java trust store file.  " +
         "Relative paths will be relative to the server root.";
    boolean   fileMustExist   = true;
    boolean   parentMustExist = true;
    boolean   mustBeFile      = true;
    boolean   mustBeDirectory = false;

    parser.addArgument(new FileArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, fileMustExist,
         parentMustExist, mustBeFile, mustBeDirectory));


    // Add an argument that allows you to specify the trust store PIN file path.
    shortIdentifier = null;
    longIdentifier  = ARG_NAME_PIN_PATH;
    required        = false;
    maxOccurrences  = 1;
    placeholder     = "{path}";
    description     = "The path to the file containing the PIN used to " +
         "access the trust store contents.  Relative paths will be relative " +
         "to the server root.";
    fileMustExist   = true;
    parentMustExist = true;
    mustBeFile      = true;
    mustBeDirectory = false;

    parser.addArgument(new FileArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, fileMustExist,
         parentMustExist, mustBeFile, mustBeDirectory));


    // Add an argument that allows you to specify the trust store format.
    shortIdentifier    = null;
    longIdentifier      = ARG_NAME_STORE_FORMAT;
    required            = true;
    maxOccurrences      = 1;
    placeholder         = "{format}";
    description         = "The format for the Java trust store file.  If no " +
         "value is specified, then a default format of JKS will be used.";
    String defaultValue = "JKS";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, defaultValue));
  }



  /**
   * Initializes this trust manager provider.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this trust manager
   *                        provider.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this trust manager provider.
   *
   * @throws  LDAPException  If a problem occurs while initializing this trust
   *                         manager provider.
   */
  @Override()
  public void initializeTrustManagerProvider(final ServerContext serverContext,
                   final TrustManagerProviderConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning trust manager provider initialization");

    this.serverContext = serverContext;

     // Get the path to the trust store file.
    final FileArgument storePathArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_STORE_PATH);
    trustStorePath = storePathArg.getValue();

     // Get the path to the PIN file.
    trustStorePINPathArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_PIN_PATH);

    // Get the trust store format.
    final StringArgument formatArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_STORE_FORMAT);
    trustStoreFormat = formatArg.getValue();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this trust
   *                              manager provider.
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
                      final TrustManagerProviderConfig config,
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
   * @param  config                The general configuration for this trust
   *                               manager provider.
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
  public ResultCode applyConfiguration(final TrustManagerProviderConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the new path to the trust store file.
   final FileArgument storePathArg =
        (FileArgument) parser.getNamedArgument(ARG_NAME_STORE_PATH);
   final File newTrustStorePath = storePathArg.getValue();

    // Get the new path to the PIN file.
   final FileArgument newPINPathArg =
        (FileArgument) parser.getNamedArgument(ARG_NAME_PIN_PATH);

   // Get the new trust store format.
   final StringArgument formatArg =
        (StringArgument) parser.getNamedArgument(ARG_NAME_STORE_FORMAT);
   final String newFormat = formatArg.getValue();


    // Apply the new configuration.
    trustStorePath       = newTrustStorePath;
    trustStorePINPathArg = newPINPathArg;
    trustStoreFormat     = newFormat;

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this trust manager
   * provider is to be taken out of service.
   */
  @Override()
  public void finalizeTrustManagerProvider()
  {
    // No finalization is required.
  }



  /**
   * Retrieves a set of trust managers that may be used for operations within
   * the server which may require access to certificates.
   *
   * @return  The set of trust managers that may be used for operations within
   *          the server which may require access to certificates.
   *
   * @throws  LDAPException  If a problem occurs while attempting to retrieve
   *                         the trust managers.
   */
  @Override()
  public TrustManager[] getTrustManagers()
         throws LDAPException
  {
    final String path    = trustStorePath.getAbsolutePath();
    final String format  = trustStoreFormat;

    final char[] pin;
    if (trustStorePINPathArg.isPresent())
    {
      final String pinPath = trustStorePINPathArg.getValue().getAbsolutePath();

      final List<String> pinLines;
      try
      {
        pinLines = trustStorePINPathArg.getNonBlankFileLines();
      }
      catch (final Exception e)
      {
        serverContext.debugCaught(e);
        throw new LDAPException(ResultCode.OTHER,
             "Unable to read the contents of the PIN file " + pinPath + ":  " +
                  StaticUtils.getExceptionMessage(e), e);
      }

      if (pinLines.size() != 1)
      {
        throw new LDAPException(ResultCode.OTHER,
             "The key store PIN file " + pinPath +
                  " does not have exactly one line.");
      }

      pin = pinLines.get(0).toCharArray();
    }
    else
    {
      pin = null;
    }

    try
    {
      return new TrustManager[]
      {
        new TrustStoreTrustManager(path, pin, format, true)
      };
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);
      throw new LDAPException(ResultCode.OTHER,
           "An error occurred while attempting to create a trust manager " +
                "from file " + path + ":  " +
                StaticUtils.getExceptionMessage(e), e);
    }
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
         Arrays.asList(ARG_NAME_STORE_PATH + "=config/trust-store"),
         "Obtain a trust manager using the contents of the Java keystore in " +
              "the config/trust-store file below the server root.");

    return exampleMap;
  }
}
