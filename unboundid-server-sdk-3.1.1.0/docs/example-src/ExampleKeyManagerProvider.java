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
import javax.net.ssl.KeyManager;

import com.unboundid.directory.sdk.common.api.KeyManagerProvider;
import com.unboundid.directory.sdk.common.config.KeyManagerProviderConfig;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.StringArgument;
import com.unboundid.util.ssl.KeyStoreKeyManager;



/**
 * This class provides a simple example of a key manager provider which will
 * obtain a key manager from a standard Java keystore file.  It has three
 * configuration arguments:
 * <UL>
 *   <LI>key-store-file -- The path to the Java keystore file.</LI>
 *   <LI>key-store-pin-file -- The path to a text file containing the PIN to use
 *       to access the contents of the Java keystore.</LI>
 *   <LI>key-store-format -- The format for the Java keystore file.</LI>
 * </UL>
 */
public final class ExampleKeyManagerProvider
       extends KeyManagerProvider
{
  /**
   * The name of the argument that will be used to specify the path to the
   * keystore PIN file.
   */
  private static final String ARG_NAME_PIN_PATH = "key-store-pin-file";



  /**
   * The name of the argument that will be used to specify the path to the
   * keystore file.
   */
  private static final String ARG_NAME_STORE_PATH = "key-store-file";



  /**
   * The name of the argument that will be used to specify the keystore format.
   */
  private static final String ARG_NAME_STORE_FORMAT = "key-store-format";



  // The path to the keystore file.
  private volatile File keyStorePath;

  // The path to the keystore PIN file.
  private volatile FileArgument keyStorePINPathArg;

  // The server context for the server in which this extension is running.
  private ServerContext serverContext;

  // The format of the Java keystore file.
  private volatile String keyStoreFormat;



  /**
   * Creates a new instance of this key manager provider.  All key manager
   * provider implementations must include a default constructor, but any
   * initialization should generally be done in the
   * {@code initializeKeyManagerProvider} method.
   */
  public ExampleKeyManagerProvider()
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
    return "Example Key Manager Provider";
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
      "This key manager provider serves as an example that may be used to " +
           "demonstrate the process for creating a third-party key manager " +
           "provider.  It will use a standard Java key store file in order " +
           "to obtain the key material."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this key manager provider.  The argument parser may
   * also be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this key manager provider.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the key store path.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_STORE_PATH;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{path}";
    String    description     = "The path to the Java key store file.  " +
         "Relative paths will be relative to the server root.";
    boolean   fileMustExist   = true;
    boolean   parentMustExist = true;
    boolean   mustBeFile      = true;
    boolean   mustBeDirectory = false;

    parser.addArgument(new FileArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, fileMustExist,
         parentMustExist, mustBeFile, mustBeDirectory));


    // Add an argument that allows you to specify the key store PIN file path.
    shortIdentifier = null;
    longIdentifier  = ARG_NAME_PIN_PATH;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{path}";
    description     = "The path to the file containing the PIN used to " +
         "access the key store contents.  Relative paths will be relative to " +
         "the server root.";
    fileMustExist   = true;
    parentMustExist = true;
    mustBeFile      = true;
    mustBeDirectory = false;

    parser.addArgument(new FileArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, fileMustExist,
         parentMustExist, mustBeFile, mustBeDirectory));


    // Add an argument that allows you to specify the key store format.
    shortIdentifier    = null;
    longIdentifier      = ARG_NAME_STORE_FORMAT;
    required            = true;
    maxOccurrences      = 1;
    placeholder         = "{format}";
    description         = "The format for the Java key store file.  If no " +
         "value is specified, then a default format of JKS will be used.";
    String defaultValue = "JKS";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description, defaultValue));
  }



  /**
   * Initializes this key manager provider.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this key manager
   *                        provider.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this key manager provider.
   *
   * @throws  LDAPException  If a problem occurs while initializing this key
   *                         manager provider.
   */
  @Override()
  public void initializeKeyManagerProvider(final ServerContext serverContext,
                   final KeyManagerProviderConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning key manager provider initialization");

    this.serverContext = serverContext;

     // Get the path to the key store file.
    final FileArgument storePathArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_STORE_PATH);
    keyStorePath = storePathArg.getValue();

     // Get the path to the PIN file.
    keyStorePINPathArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_PIN_PATH);

    // Get the key store format.
    final StringArgument formatArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_STORE_FORMAT);
    keyStoreFormat = formatArg.getValue();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this key manager
   *                              provider.
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
                      final KeyManagerProviderConfig config,
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
   * @param  config                The general configuration for this key
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
  public ResultCode applyConfiguration(final KeyManagerProviderConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the new path to the key store file.
   final FileArgument storePathArg =
        (FileArgument) parser.getNamedArgument(ARG_NAME_STORE_PATH);
   final File newKeyStorePath = storePathArg.getValue();

    // Get the new path to the PIN file.
   final FileArgument newPINPathArg =
        (FileArgument) parser.getNamedArgument(ARG_NAME_PIN_PATH);

   // Get the new key store format.
   final StringArgument formatArg =
        (StringArgument) parser.getNamedArgument(ARG_NAME_STORE_FORMAT);
   final String newFormat = formatArg.getValue();


    // Apply the new configuration.
    keyStorePath       = newKeyStorePath;
    keyStorePINPathArg = newPINPathArg;
    keyStoreFormat     = newFormat;

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this key manager provider
   * is to be taken out of service.
   */
  @Override()
  public void finalizeKeyManagerProvider()
  {
    // No finalization is required.
  }



  /**
   * Retrieves a set of key managers that may be used for operations within
   * the server which may require access to certificates.
   *
   * @return  The set of key managers that may be used for operations within the
   *          server which may require access to certificates.
   *
   * @throws  LDAPException  If a problem occurs while attempting to retrieve
   *                         the key managers.
   */
  @Override()
  public KeyManager[] getKeyManagers()
         throws LDAPException
  {
    final String path    = keyStorePath.getAbsolutePath();
    final String format  = keyStoreFormat;
    final String pinPath = keyStorePINPathArg.getValue().getAbsolutePath();

    final List<String> pinLines;
    try
    {
      pinLines = keyStorePINPathArg.getNonBlankFileLines();
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

    final char[] pin = pinLines.get(0).toCharArray();

    try
    {
      return new KeyManager[]
      {
        new KeyStoreKeyManager(path, pin, format, null)
      };
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);
      throw new LDAPException(ResultCode.OTHER,
           "An error occurred while attempting to create a key manager from " +
                "file " + path + ":  " + StaticUtils.getExceptionMessage(e), e);
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
         Arrays.asList(ARG_NAME_STORE_PATH + "=config/key-store",
                       ARG_NAME_PIN_PATH + "=config/keystore.pin"),
         "Obtain a key manager using the contents of the Java keystore in " +
              "the config/key-store file below the server root.");

    return exampleMap;
  }
}
