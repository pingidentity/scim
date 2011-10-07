/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.plugin;



import com.unboundid.directory.sdk.common.types.LogSeverity;
import com.unboundid.directory.sdk.ds.api.Plugin;
import com.unboundid.directory.sdk.ds.config.PluginConfig;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.directory.sdk.ds.types.StartupPluginResult;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.scim.ri.SCIMServer;
import com.unboundid.scim.ri.SCIMServerConfig;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DebugType;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.BooleanArgument;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;



/**
 * This class provides a plugin that presents a Simple Cloud Identity
 * Management (SCIM) protocol interface to the Directory Server.
 */
public final class SCIMPlugin
       extends Plugin
{
  /**
   * The DN of a SCIM root user that has proxied auth privilege.
   * TODO: it was too hard to get proxied auth to work
   */
//  public static final String SCIM_USER_DN =
//      "cn=SCIM User,cn=Root DNs,cn=config";

  /**
   * The name of the argument that will be used to define the resources
   * supported by the SCIM protocol interface.
   */
  private static final String ARG_NAME_USE_RESOURCES_FILE = "useResourcesFile";

  /**
   * The name of the argument that will be used to specify the XML schema
   * supported by the SCIM protocol interface.
   */
  private static final String ARG_NAME_USE_SCHEMA_FILE = "useSchemaFile";

  /**
   * The name of the argument that will be used to specify the port number of
   * the SCIM protocol interface.
   */
  private static final String ARG_NAME_PORT = "port";

  /**
   * The name of the argument that will be used to specify the base URI of
   * the SCIM protocol interface.
   */
  private static final String ARG_NAME_BASE_URI = "baseURI";

  /**
   * The name of the argument that will be used to enable debug logging in the
   * SCIM plugin.
   */
  private static final String ARG_NAME_DEBUG_ENABLED = "debugEnabled";

  /**
   * The name of the argument that will be used to specify the level of debug
   * logging in the SCIM plugin.
   */
  private static final String ARG_NAME_DEBUG_LEVEL = "debugLevel";

  /**
   * The name of the argument that will be used to specify the types of debug
   * logging in the SCIM plugin.
   */
  private static final String ARG_NAME_DEBUG_TYPE = "debugType";

  /**
   * The server context for the server in which this extension is running.
   */
  private DirectoryServerContext serverContext;



  /**
   * Creates a new instance of this plugin.  All plugin implementations must
   * include a default constructor, but any initialization should generally be
   * done in the {@code initializePlugin} method.
   */
  public SCIMPlugin()
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
    return "SCIM Plugin";
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
      "This plugin provides a Simple Cloud Identity Management (SCIM) " +
      "protocol interface to the Directory Server."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this plugin.  The argument parser may also be updated
   * to define relationships between arguments (e.g., to specify required,
   * exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this plugin.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // This is a required argument.
    parser.addArgument(
        new FileArgument(null, ARG_NAME_USE_RESOURCES_FILE,
                         true, 1, "{path}",
                         "The path to an XML file defining the resources " +
                         "supported by the SCIM interface.",
                         true, true, true, false));

    // This argument is not required because built-in defaults are available.
    parser.addArgument(
        new FileArgument(null, ARG_NAME_USE_SCHEMA_FILE,
                         false, 1, "{path}",
                         "The path to a file or directory containing XML " +
                         "schema definitions for resources supported by " +
                         "the SCIM interface.",
                         true, true, false, false));

    // This is a required argument.
    parser.addArgument(
        new IntegerArgument(null, ARG_NAME_PORT,
                            true, 1, "{port}",
                            "The port number of the SCIM interface.",
                            0, 65535));

    // This argument is required but has a default.
    parser.addArgument(
        new StringArgument(null, ARG_NAME_BASE_URI,
                           true, 1, "{URL-path}",
                           "The base URI of the SCIM interface. If no base " +
                           "URI is specified then the default value '/' is " +
                           "used", "/"));

    // Debug log arguments.
    parser.addArgument(
        new BooleanArgument(null, ARG_NAME_DEBUG_ENABLED,
                           "Enables debug logging in the SCIM plugin"));

    parser.addArgument(
        new StringArgument(null, ARG_NAME_DEBUG_LEVEL,
                           false, 1,
                           "(SEVERE|WARN|INFO|CONFIG|FINE|FINER|FINEST)",
                           "Specifies the level of debug logging in the SCIM" +
                           " plugin"));

    parser.addArgument(
        new StringArgument(null, ARG_NAME_DEBUG_TYPE,
                           false, 1,
                           "{debug-type,...}",
                           "Specifies the types of debug logging in the SCIM" +
                           " plugin"));
  }



  /**
   * Initializes this plugin.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this plugin.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this plugin.
   *
   * @throws  LDAPException  If a problem occurs while initializing this plugin.
   */
  @Override()
  public void initializePlugin(final DirectoryServerContext serverContext,
                               final PluginConfig config,
                               final ArgumentParser parser)
      throws LDAPException
  {
    Debug.getLogger().addHandler(new DebugLogHandler(serverContext));
    Debug.getLogger().setUseParentHandlers(false);

    final BooleanArgument debugEnabledArg =
        (BooleanArgument) parser.getNamedArgument(ARG_NAME_DEBUG_ENABLED);
    final StringArgument debugLevelArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_LEVEL);
    final StringArgument debugTypeArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_TYPE);

    final Properties properties = new Properties();
    properties.setProperty(Debug.PROPERTY_DEBUG_ENABLED,
                           Boolean.toString(debugEnabledArg.isPresent()));
    if (debugLevelArg.isPresent())
    {
      properties.setProperty(Debug.PROPERTY_DEBUG_LEVEL,
                             debugLevelArg.getValue());
    }
    if (debugTypeArg.isPresent())
    {
      properties.setProperty(Debug.PROPERTY_DEBUG_TYPE,
                             debugTypeArg.getValue());
    }
    Debug.initialize(properties);

    Debug.debug(Level.INFO, DebugType.OTHER,
                "Beginning SCIM plugin initialization");

    this.serverContext = serverContext;

    // It was too hard to get Proxied Auth to work.
//    final InternalConnection internalConnection =
//        serverContext.getInternalRootConnection();
//    if (internalConnection.getEntry(SCIM_USER_DN) == null)
//    {
//      try
//      {
//        internalConnection.add(
//            "dn: " + SCIM_USER_DN,
//            "objectClass: top",
//            "objectClass: person",
//            "objectClass: organizationalPerson",
//            "objectClass: inetOrgPerson",
//            "objectClass: ds-cfg-root-dn-user",
//            "cn: SCIM User",
//            "givenName: SCIM",
//            "sn: User",
//            "userPassword: password",
//            "ds-cfg-inherit-default-root-privileges: false",
//            "ds-privilege-name: proxied-auth");
//      }
//      catch (LDIFException e)
//      {
//        Debug.debugException(e);
//        throw new LDAPException(ResultCode.OTHER, e);
//      }
//    }

    final SCIMServerConfig scimServerConfig = getSCIMConfig(parser);

    final SCIMServer scimServer = SCIMServer.getInstance();
    try
    {
      scimServer.initializeServer(scimServerConfig);
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      throw new LDAPException(
          ResultCode.OTHER,
          "An error occurred while initializing the SCIM plugin.", e);
    }

    final StringArgument baseUriArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_BASE_URI);
    final String baseUri = baseUriArg.getValue();
    final SCIMBackend scimBackend =
        new ServerContextBackend(serverContext);
    scimServer.registerBackend(baseUri, scimBackend);

    if (serverContext.isRunning())
    {
      // This is needed to start listening if the plugin was dynamically added
      // after the server was started (e.g., via dsconfig).
      try
      {
        scimServer.startListening();
        serverContext.logMessage(
            LogSeverity.NOTICE,
            "The server is listening for SCIM requests on port " +
            scimServer.getListenPort());
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
        throw new LDAPException(
            ResultCode.OTHER,
            "An error occurred while attempting to start listening for " +
            "SCIM requests:" + StaticUtils.getExceptionMessage(e), e);
      }
    }

    Debug.debug(Level.INFO, DebugType.OTHER,
                "Finished SCIM plugin initialization");
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this plugin.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(final PluginConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    final StringArgument debugLevelArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_LEVEL);
    final StringArgument debugTypeArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_TYPE);

    final Properties properties = new Properties();
    if (debugLevelArg.isPresent())
    {
      try
      {
        Level.parse(debugLevelArg.getValue());
      }
      catch (IllegalArgumentException e)
      {
        Debug.debugException(e);
        unacceptableReasons.add("Invalid value '" + debugLevelArg.getValue() +
                                "' for the SCIM plugin debug level");
        acceptable = false;
      }
    }

    if (debugTypeArg.isPresent())
    {
      properties.setProperty(Debug.PROPERTY_DEBUG_TYPE,
                             debugTypeArg.getValue());
      final StringTokenizer t =
          new StringTokenizer(debugTypeArg.getValue(), ", ");
      while (t.hasMoreTokens())
      {
        final String debugTypeName = t.nextToken();
        if (DebugType.forName(debugTypeName) == null)
        {
          unacceptableReasons.add(
              "Invalid value '" + debugTypeName +
              "' for a SCIM plugin debug type.  Allowed values include:  " +
              DebugType.getTypeNameList() + '.');
          acceptable = false;
        }
      }
    }
    Debug.initialize(properties);

    final FileArgument useSchemaFileArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_USE_SCHEMA_FILE);

    if (useSchemaFileArg.isPresent())
    {
      final File f = useSchemaFileArg.getValue();
      if (f.exists())
      {
        final ArrayList<File> schemaFiles = new ArrayList<File>(1);
        if (f.isFile())
        {
          schemaFiles.add(f);
        }
        else
        {
          for (final File subFile : f.listFiles())
          {
            if (subFile.isFile())
            {
              schemaFiles.add(subFile);
            }
          }
        }

        if (schemaFiles.isEmpty())
        {
          unacceptableReasons.add("No schema files found at " +
                                  useSchemaFileArg.toString());
          acceptable = false;
        }
      }
      else
      {
        unacceptableReasons.add("Schema file or directory " +
                                useSchemaFileArg.toString() +
                                " does not exist");
        acceptable = false;
      }
    }

    return acceptable;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this plugin.
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
  public ResultCode applyConfiguration(final PluginConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    ResultCode rc = ResultCode.SUCCESS;

    final BooleanArgument debugEnabledArg =
        (BooleanArgument) parser.getNamedArgument(ARG_NAME_DEBUG_ENABLED);
    final StringArgument debugLevelArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_LEVEL);
    final StringArgument debugTypeArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_TYPE);

    final Properties properties = new Properties();
    properties.setProperty(Debug.PROPERTY_DEBUG_ENABLED,
                           Boolean.toString(debugEnabledArg.isPresent()));
    if (debugLevelArg.isPresent())
    {
      properties.setProperty(Debug.PROPERTY_DEBUG_LEVEL,
                             debugLevelArg.getValue());
    }
    if (debugTypeArg.isPresent())
    {
      properties.setProperty(Debug.PROPERTY_DEBUG_TYPE,
                             debugTypeArg.getValue());
    }
    Debug.initialize(properties);

    final SCIMServer scimServer = SCIMServer.getInstance();
    try
    {
      scimServer.shutdown();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
    }

    final SCIMServerConfig scimServerConfig = getSCIMConfig(parser);

    try
    {
      scimServer.initializeServer(scimServerConfig);
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      messages.add("An error occurred while initializing the SCIM plugin:" +
                   StaticUtils.getExceptionMessage(e));
      return ResultCode.OTHER;
    }

    final StringArgument baseUriArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_BASE_URI);
    final String baseUri = baseUriArg.getValue();
    final SCIMBackend scimBackend =
        new ServerContextBackend(serverContext);
    scimServer.registerBackend(baseUri, scimBackend);

    try
    {
      scimServer.startListening();
      serverContext.logMessage(
          LogSeverity.NOTICE,
          "The server is listening for SCIM requests on port " +
          scimServer.getListenPort());
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      messages.add("An error occurred while attempting to start listening " +
                   "for SCIM requests:" + StaticUtils.getExceptionMessage(e));
      return ResultCode.OTHER;
    }

    return rc;
  }



  /**
   * Performs any processing which may be necessary when the server is starting.
   *
   * @return  Information about the result of the plugin processing.
   */
  public StartupPluginResult doStartup()
  {
    // Start listening for SCIM requests.
    try
    {
      final SCIMServer scimServer = SCIMServer.getInstance();
      scimServer.startListening();
      serverContext.logMessage(
          LogSeverity.NOTICE,
          "The server is listening for SCIM requests on port " +
          scimServer.getListenPort());
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      return new StartupPluginResult(
          false, true,
          "An error occurred while attempting to start listening for " +
          "SCIM requests:" + StaticUtils.getExceptionMessage(e));
    }

    return StartupPluginResult.SUCCESS;
  }



  /**
   * Performs any processing which may be necessary when the server is shutting
   * down.
   *
   * @param  shutdownReason  A message which may provide information about the
   *                         reason the server is shutting down.
   */
  public void doShutdown(final String shutdownReason)
  {
    try
    {
      SCIMServer.getInstance().shutdown();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
    }
  }



  /**
   * Performs any cleanup which may be necessary when this plugin is to be taken
   * out of service.
   */
  @Override()
  public void finalizePlugin()
  {
    try
    {
      SCIMServer.getInstance().shutdown();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
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
         Arrays.asList(
              ARG_NAME_USE_RESOURCES_FILE + "=config/resources.xml",
              ARG_NAME_PORT + "=8080"),
         "Creates a SCIM protocol interface listening on port 8080. The " +
         "interface supports the resources defined in resources.xml.");

    return exampleMap;
  }



  /**
   * Creates a SCIM server configuration based on information provided in an
   * argument parser.
   *
   * @param parser  The argument parser containing the information needed to
   *                create the configuration.
   *
   * @return  The configuration that was created.
   *
   */
  private static SCIMServerConfig getSCIMConfig(final ArgumentParser parser)
  {
    final FileArgument useResourcesFileArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_USE_RESOURCES_FILE);

    final FileArgument useSchemaFileArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_USE_SCHEMA_FILE);

    final IntegerArgument portArg =
         (IntegerArgument) parser.getNamedArgument(ARG_NAME_PORT);

    final SCIMServerConfig scimServerConfig = new SCIMServerConfig();

    scimServerConfig.setListenPort(portArg.getValue());
    scimServerConfig.setResourcesFile(useResourcesFileArg.getValue());

    if (useSchemaFileArg.isPresent())
    {
      final File f = useSchemaFileArg.getValue();
      if (f.exists())
      {
        final ArrayList<File> schemaFiles = new ArrayList<File>(1);
        if (f.isFile())
        {
          schemaFiles.add(f);
        }
        else
        {
          for (final File subFile : f.listFiles())
          {
            if (subFile.isFile())
            {
              schemaFiles.add(subFile);
            }
          }
        }

        if (! schemaFiles.isEmpty())
        {
          final File[] files = new File[schemaFiles.size()];
          scimServerConfig.setSchemaFiles(schemaFiles.toArray(files));
        }
      }
    }

    return scimServerConfig;
  }
}
