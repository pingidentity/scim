/*
 * Copyright 2011-2013 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import com.unboundid.directory.sdk.common.types.LogSeverity;
import com.unboundid.directory.sdk.common.types.RegisteredMonitorProvider;
import com.unboundid.directory.sdk.http.api.HTTPServletExtension;
import com.unboundid.directory.sdk.http.config.HTTPServletExtensionConfig;
import com.unboundid.directory.sdk.http.types.HTTPServerContext;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.scim.ldap.LDAPBackend;
import com.unboundid.scim.ldap.ResourceMapper;
import com.unboundid.scim.wink.SCIMApplication;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.DebugType;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.BooleanArgument;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;
import org.apache.wink.server.internal.DeploymentConfiguration;
import org.apache.wink.server.internal.servlet.RestServlet;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;

import static com.unboundid.scim.sdk.Debug.debugException;



/**
 * This class provides a HTTP Servlet Extension that presents a Simple Cloud
 * Identity Management (SCIM) protocol interface to the Directory Server.
 */
public final class SCIMServletExtension
       extends HTTPServletExtension
{
  /**
   * The name of the argument that will be used to define the resources
   * supported by the SCIM protocol interface.
   */
  private static final String ARG_NAME_RESOURCES_FILE = "resourceMappingFile";

  /**
   * The name of the argument that will be used to enable debug logging in the
   * SCIM servlet.
   */
  private static final String ARG_NAME_DEBUG_ENABLED = "debugEnabled";

  /**
   * The name of the argument that will be used to specify the level of debug
   * logging in the SCIM servlet.
   */
  private static final String ARG_NAME_DEBUG_LEVEL = "debugLevel";

  /**
   * The name of the argument that will be used to specify the types of debug
   * logging in the SCIM servlet.
   */
  private static final String ARG_NAME_DEBUG_TYPE = "debugType";

  /**
   * The name of the argument that will be used to specify the maximum number
   * of resources returned in a response.
   */
  private static final String ARG_NAME_MAX_RESULTS = "maxResults";

  /**
   * The name of the argument that will be used to specify the path that will be
   * used to access the servlet.
   */
  private static final String ARG_NAME_CONTEXT_PATH = "contextPath";

  /**
   * The name of the argument that will be used to specify the maximum number
   * of operations permitted in a bulk request.
   */
  private static final String ARG_NAME_BULK_MAX_OPERATIONS =
      "bulkMaxOperations";

  /**
   * The name of the argument that will be used to specify the maximum payload
   * size in bytes of a bulk request.
   */
  private static final String ARG_NAME_BULK_MAX_PAYLOAD_SIZE =
      "bulkMaxPayloadSize";

  /**
   * The name of the argument that will be used to specify the maximum number
   * of concurrent bulk requests.
   */
  private static final String ARG_NAME_BULK_MAX_CONCURRENT_REQUESTS =
      "bulkMaxConcurrentRequests";

  /**
   * The name of the argument that will be used to define the path to a
   * directory that may be used by the SCIM extension to create temporary
   * files containing SCIM request data.
   */
  private static final String ARG_NAME_TMP_DATA_DIR = "tmpDataDir";

  /**
   * The default value for tmpDataDir.
   */
  private static final String DEFAULT_TMP_DATA_DIR =
      "extensions/com.unboundid.scim-extension/tmp-data";

  /**
   * The name of the argument that will be used to define the file permissions
   * to be set on the tmpDataDir directory.
   */
  private static final String ARG_NAME_TMP_DATA_DIR_PERMISSIONS =
      "tmpDataDirPermissions";

  /**
   * The servlet that has been created.
   */
  private volatile RestServlet servlet;

  /**
   * The context path that will be used for the servlet.
   */
  private volatile String contextPath;

  /**
   * The unique name that will be used to reference this servlet instance.
   */
  private volatile String name;

  /**
   * The backend that will handle the SCIM requests.
   */
  private LDAPBackend backend;

  /**
   * The JAX-RS application that will handle the SCIM requests.
   */
  private volatile SCIMApplication application;

  /**
   * The server context for the server in which this extension is running.
   */
  private HTTPServerContext serverContext;

  /**
   * The registered monitor provider for this extension.
   */
  private RegisteredMonitorProvider monitorProvider;



  /**
   * Creates a new instance of this HTTP servlet extension.  All HTTP servlet
   * extension implementations must include a default constructor, but any
   * initialization should generally be done in the {@code createServlet}
   * method.
   */
  public SCIMServletExtension()
  {
    servlet         = null;
    contextPath     = null;
    backend         = null;
    application     = null;
    serverContext   = null;
    monitorProvider = null;
  }



  /**
   * Retrieves a human-readable name for this extension.
   *
   * @return  A human-readable name for this extension.
   */
  @Override()
  public String getExtensionName()
  {
    return "SCIM HTTP Servlet Extension";
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
      "This HTTP servlet extension provides a Simple Cloud Identity " +
      "Management (SCIM) protocol interface to the Directory Server " +
      "and the Directory Proxy Server."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this HTTP servlet extension.  The argument parser may
   * also be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this HTTP servlet extension.
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
        new FileArgument(null, ARG_NAME_RESOURCES_FILE,
                         true, 1, "{path}",
                         "The path to an XML file defining the resources " +
                         "supported by the SCIM interface.",
                         true, true, true, false));

    // This argument has a default.
    parser.addArgument(
        new StringArgument(null, ARG_NAME_CONTEXT_PATH,
                           true, 1, "{path}",
                           "The context path to use to access the SCIM " +
                           "interface. If no path is specified then the " +
                           "default value'/' is used. Note that changes to " +
                           "this argument will only take effect if the " +
                           "associated HTTP connection handler (or the " +
                           "entire server) is stopped and re-started.", "/"));

    // This argument has a default.
    parser.addArgument(
        new FileArgument(null, ARG_NAME_TMP_DATA_DIR,
                         false, 1, "{path}",
                         "The path to a directory that will be used to " +
                         "create temporary files containing SCIM request " +
                         "data. Non-absolute paths are relative to the " +
                         "server root directory. The default value is " +
                         DEFAULT_TMP_DATA_DIR,
                         false, true, false, true));

    // This argument has a default.
    parser.addArgument(
        new StringArgument(null, ARG_NAME_TMP_DATA_DIR_PERMISSIONS,
                           true, 1, "{3-octal-digits}",
                           "Specifies the permissions that should be applied " +
                           "to the tmpDataDir directory. The permissions " +
                           "are expressed as a three-digit octal value, " +
                           "which is the traditional representation for " +
                           "UNIX file permissions. The default value is 700 " +
                           "which allows access only by the owner of the " +
                           "directory.",
                           "700"));

    // Debug log arguments.
    parser.addArgument(
        new BooleanArgument(null, ARG_NAME_DEBUG_ENABLED,
                           "Enables debug logging in the SCIM servlet"));

    parser.addArgument(
        new StringArgument(null, ARG_NAME_DEBUG_LEVEL,
                           false, 1,
                           "(SEVERE|WARN|INFO|CONFIG|FINE|FINER|FINEST)",
                           "Specifies the level of debug logging in the SCIM" +
                           " servlet"));

    parser.addArgument(
        new StringArgument(null, ARG_NAME_DEBUG_TYPE,
                           false, 1,
                           "{debug-type,...}",
                           "Specifies the types of debug logging in the SCIM" +
                           " servlet"));

    // This argument has a default.
    parser.addArgument(
        new IntegerArgument(null, ARG_NAME_MAX_RESULTS,
                            true, 1, "{integer}",
                            "The maximum number of resources that are " +
                            "returned in a response. The default value is 100",
                            1, Integer.MAX_VALUE, 100));

    // This argument has a default.
    parser.addArgument(
        new IntegerArgument(null, ARG_NAME_BULK_MAX_OPERATIONS,
                            true, 1, "{integer}",
                            "The maximum number of operations that are " +
                            "permitted in a bulk request. The default value " +
                            "is 10000.",
                            1, Integer.MAX_VALUE, 10000));

    // This argument has a default.
    parser.addArgument(
        new IntegerArgument(null, ARG_NAME_BULK_MAX_PAYLOAD_SIZE,
                            true, 1, "{integer}",
                            "The maximum payload size in bytes of a bulk " +
                            "request. The default value is 10000000 bytes.",
                            0, Integer.MAX_VALUE, 10000000));

    // This argument has a default.
    parser.addArgument(
        new IntegerArgument(null, ARG_NAME_BULK_MAX_CONCURRENT_REQUESTS,
                            true, 1, "{integer}",
                            "The maximum number of bulk requests that may be " +
                            "processed concurrently by the server. " +
                            "The default value is 10. Any bulk " +
                            "request that would cause this limit to be " +
                            "exceeded is rejected with HTTP status code 503.",
                            1, Integer.MAX_VALUE, 10));
  }



  /**
   * Retrieves a map of initialization parameters that should be provided to the
   * servlet when it is initialized.
   *
   * @return  A map of initialization parameters that should be provided to the
   *          servlet when it is initialized, or an empty map if no
   *          initialization parameters are needed.
   */
  @Override
  public Map<String,String> getServletInitParameters()
  {
    Map<String, String> params = new HashMap<String, String>(2);
    params.put("requestProcessorAttribute", name);
    params.put("propertiesLocation",
        "com/unboundid/scim/wink/wink-scim.properties");
    return params;
  }



  /**
   * Retrieves the order in which the servlet should be started.  A value
   * greater than or equal to zero guarantees that the servlet will be started
   * as soon as the servlet engine has been started, in order of ascending
   * servlet init order values, before the {@code doPostRegistrationProcessing}
   * method has been called.  If the value is less than zero, the servlet may
   * not be started until a request is received for one of its registered paths.
   *
   * @return  The order in which the servlet should be started, or a negative
   *          value if startup order does not matter.
   */
  @Override
  public int getServletInitOrder()
  {
    return 0;
  }



  /**
   * Creates an HTTP servlet extension using the provided information.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this HTTP servlet
   *                        extension.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this HTTP servlet extension.
   *
   * @return  The HTTP servlet that has been created.
   *
   * @throws  LDAPException  If a problem is encountered while attempting to
   *                         create the HTTP servlet.
   */
  @Override()
  public HttpServlet createServlet(
      final HTTPServerContext serverContext,
      final HTTPServletExtensionConfig config,
      final ArgumentParser parser)
      throws LDAPException
  {
    this.serverContext = serverContext;

    this.name = config.getConfigObjectName();

    Debug.getLogger().addHandler(new DebugLogHandler(serverContext));
    Debug.getLogger().setUseParentHandlers(false);

    final BooleanArgument debugEnabledArg =
        (BooleanArgument) parser.getNamedArgument(ARG_NAME_DEBUG_ENABLED);
    final StringArgument debugLevelArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_LEVEL);
    final StringArgument debugTypeArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_TYPE);
    final StringArgument contextPathArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_CONTEXT_PATH);
    final IntegerArgument maxResultsArg =
         (IntegerArgument) parser.getNamedArgument(ARG_NAME_MAX_RESULTS);
    final IntegerArgument bulkMaxOperationsArg =
         (IntegerArgument) parser.getNamedArgument(
             ARG_NAME_BULK_MAX_OPERATIONS);
    final IntegerArgument bulkMaxPayloadSizeArg =
         (IntegerArgument) parser.getNamedArgument(
             ARG_NAME_BULK_MAX_PAYLOAD_SIZE);
    final IntegerArgument bulkMaxConcurrentRequestsArg =
         (IntegerArgument) parser.getNamedArgument(
             ARG_NAME_BULK_MAX_CONCURRENT_REQUESTS);
    final FileArgument resourcesFileArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_RESOURCES_FILE);
    final FileArgument tmpDataDirArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_TMP_DATA_DIR);
    final StringArgument tmpDataDirPermissionsArg =
         (StringArgument) parser.getNamedArgument(
             ARG_NAME_TMP_DATA_DIR_PERMISSIONS);

    contextPath = contextPathArg.getValue();

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
                "Beginning SCIM Servlet Extension initialization");

    final Map<ResourceDescriptor, ResourceMapper> resourceMappers =
        new HashMap<ResourceDescriptor, ResourceMapper>();
    try
    {
      final List<ResourceMapper> mappers =
          ResourceMapper.parse(
              resourcesFileArg.getValue(),
              serverContext.getInternalRootConnection().getSchema());
      for (final ResourceMapper resourceMapper : mappers)
      {
        resourceMappers.put(resourceMapper.getResourceDescriptor(),
                            resourceMapper);
      }
    }
    catch (Exception e)
    {
      debugException(e);
      throw new LDAPException(
          ResultCode.OTHER,
          "An error occurred while initializing the resources file.", e);
    }

    backend = new ServerContextBackend(resourceMappers, serverContext);
    backend.getConfig().setMaxResults(maxResultsArg.getValue());

    final FilePermission tmpDataDirPermission;
    try
    {
      tmpDataDirPermission =
          FilePermission.decodeUNIXMode(tmpDataDirPermissionsArg.getValue());
    }
    catch(Exception e)
    {
      debugException(e);
      final String message = String.format(
          "The tmpDataDirPermissions %s is not a valid UNIX permission " +
          "mode in three-digit octal notation",
          tmpDataDirPermissionsArg.getValue());
      throw new LDAPException(ResultCode.OTHER, message, e);
    }

    // Create the tmpDataDir directory if it doesn't exist.
    File tmpDataDir = tmpDataDirArg.getValue();
    if (tmpDataDir == null)
    {
      tmpDataDir =
          new File(serverContext.getServerRoot(), DEFAULT_TMP_DATA_DIR);
    }
    if (!tmpDataDir.exists())
    {
      if(!tmpDataDir.mkdirs())
      {
        final String message = String.format(
            "The tmpDataDir directory %s could not be created",
            tmpDataDir);
        throw new LDAPException(ResultCode.OTHER, message);
      }
    }

    // Set permissions on the tmpDataDir directory.
    setPermissions(tmpDataDir, tmpDataDirPermission);

    // Create the Wink JAX-RS application.
    application = new SCIMApplication(resourceMappers.keySet(), backend, null);
    application.setBulkMaxOperations(
        bulkMaxOperationsArg.getValue().longValue());
    application.setBulkMaxPayloadSize(
        bulkMaxPayloadSizeArg.getValue().longValue());
    application.setBulkMaxConcurrentRequests(
        bulkMaxConcurrentRequestsArg.getValue());
    application.setTmpDataDir(tmpDataDir);

    // Create the Wink JAX-RS servlet.
    servlet = new RestServlet()
    {
      private static final long serialVersionUID = 7010592173510938139L;



      @Override
      protected Application getApplication(
          final DeploymentConfiguration configuration)
          throws ClassNotFoundException, InstantiationException,
          IllegalAccessException
      {
        return application;
      }
    };

    // Register a custom monitor provider.
    final String monitorInstanceName = getMonitorInstanceName(config);
    monitorProvider = serverContext.registerMonitorProvider(
        new SCIMServletMonitorProvider(monitorInstanceName, this), config);

    Debug.debug(Level.INFO, DebugType.OTHER,
                "Finished SCIM Servlet Extension initialization");

    return servlet;
  }



  /**
   * Attempt to set permissions on the provided file.
   *
   * @param file        The file whose permissions are to be set.
   * @param permission  The file permissions to apply.
   */
  private void setPermissions(final File file, final FilePermission permission)
  {
    if (FilePermission.canSetPermissions())
    {
      try
      {
        if(!FilePermission.setPermissions(file, permission))
        {
          final String message = String.format(
              "This platform does not support setting file permissions %s " +
              "on %s",
              permission.toString(), file.toString());
          serverContext.logMessage(LogSeverity.SEVERE_WARNING, message);
        }
      }
      catch(Exception e)
      {
        debugException(e);
        // Log a warning that the permissions were not set.
        final String message = String.format(
            "An error occurred while setting file permissions on %s: %s",
            file.toString(), e.getMessage());
        serverContext.logMessage(LogSeverity.SEVERE_WARNING, message);
      }
    }
  }



  /**
   * Return a list of javax.servlet.Filter instances that are used by the
   * HTTPConnectionHandler. Currently this provides a mechanism for SCIM to
   * inject Basic Authentication handling.
   * @return a list of javax.servlet.Filter instances
   */
  public List<Filter> getServletFilters() {
    return Collections.<Filter>singletonList(
        new BasicAuthenticationFilter(backend));
  }



  /**
   * Retrieves a list of the request paths for which the associated servlet
   * should be invoked.  This method will be called after the
   * {@link #createServlet} method has been used to create the servlet instance.
   *
   * @return  A list of the request paths for which the associated servlet
   *          should be invoked.
   */
  @Override()
  public List<String> getServletPaths()
  {
    final String normalizedPath = getNormalizedPath();

    return Arrays.asList(normalizedPath + "*", normalizedPath + "v1/*");
  }



  /**
   * Gets the path with a trailing '/'.
   *
   * @return  The path with a trailing '/'.
   */
  private String getNormalizedPath()
  {
    final String normalizedPath;
    if (!contextPath.endsWith("/"))
    {
      normalizedPath = contextPath + "/";
    }
    else
    {
      normalizedPath = contextPath;
    }

    return normalizedPath;
  }



  @Override
  public void doPostShutdownProcessing()
  {
    serverContext.deregisterMonitorProvider(monitorProvider);

    servlet         = null;
    contextPath     = null;
    backend         = null;
    application     = null;
    serverContext   = null;
    monitorProvider = null;
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this HTTP
   *                              servlet extension.
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
                      final HTTPServletExtensionConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    final StringArgument debugLevelArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_LEVEL);
    final StringArgument debugTypeArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_DEBUG_TYPE);
    final FileArgument useResourcesFileArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_RESOURCES_FILE);
    final StringArgument tmpDataDirPermissionsArg =
         (StringArgument) parser.getNamedArgument(
             ARG_NAME_TMP_DATA_DIR_PERMISSIONS);

    try
    {
      FilePermission.decodeUNIXMode(tmpDataDirPermissionsArg.getValue());
    }
    catch(Exception e)
    {
      debugException(e);
      final String message = String.format(
          "The tmpDataDirPermissions %s is not a valid UNIX permission " +
          "mode in three-digit octal notation",
          tmpDataDirPermissionsArg.getValue());
      unacceptableReasons.add(message);
      acceptable = false;
    }

    if (debugLevelArg.isPresent())
    {
      try
      {
        Level.parse(debugLevelArg.getValue());
      }
      catch (IllegalArgumentException e)
      {
        debugException(e);
        unacceptableReasons.add("Invalid value '" + debugLevelArg.getValue() +
                                "' for the SCIM servlet debug level");
        acceptable = false;
      }
    }

    if (debugTypeArg.isPresent())
    {
      final StringTokenizer t =
          new StringTokenizer(debugTypeArg.getValue(), ", ");
      while (t.hasMoreTokens())
      {
        final String debugTypeName = t.nextToken();
        if (DebugType.forName(debugTypeName) == null)
        {
          unacceptableReasons.add(
              "Invalid value '" + debugTypeName +
              "' for a SCIM servlet debug type.  Allowed values include:  " +
              DebugType.getTypeNameList() + '.');
          acceptable = false;
        }
      }
    }

    try
    {
      // The server context is null if the extension is not yet enabled.
      final Schema schema;
      if (serverContext != null)
      {
        schema = serverContext.getInternalRootConnection().getSchema();
      }
      else
      {
        schema = null;
      }
      ResourceMapper.parse(useResourcesFileArg.getValue(), schema);
    }
    catch (Exception e)
    {
      debugException(e);
      unacceptableReasons.add(
          "The resources file '" + useResourcesFileArg.getValue() +
          "' cannot be parsed: " + StaticUtils.getExceptionMessage(e));
      acceptable = false;
    }

    return acceptable;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this HTTP
   *                               servlet extension.
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
  public ResultCode applyConfiguration(final HTTPServletExtensionConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    ResultCode rc = ResultCode.SUCCESS;

    if (servlet == null)
    {
      // The servlet is not enabled. It is not clear how to register
      // our servlet at this point.
      adminActionsRequired.add(
          "The changes to the HTTP Servlet Extension will not take effect " +
          "until the HTTP Connection Handler (or the entire server) is " +
          "restarted");
      // We would prefer to return SUCCESS here but unfortunately dsconfig
      // would be silent and would not report the action required.
      // This way, it will report a communication error together with the
      // action required.
      return ResultCode.OTHER;
    }

    // The path will not change dynamically.  If a different path was given,
    // then report that as a required administrative action.
    final StringArgument contextPathArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_CONTEXT_PATH);
    if (! contextPath.equals(contextPathArg.getValue()))
    {
      adminActionsRequired.add("Changes to the servlet context path will not" +
           "take effect until the HTTP Connection Handler (or entire " +
           "server) is restarted.");
    }

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

    final IntegerArgument maxResultsArg =
         (IntegerArgument) parser.getNamedArgument(ARG_NAME_MAX_RESULTS);
    final IntegerArgument bulkMaxOperationsArg =
         (IntegerArgument) parser.getNamedArgument(
             ARG_NAME_BULK_MAX_OPERATIONS);
    final IntegerArgument bulkMaxPayloadSizeArg =
         (IntegerArgument) parser.getNamedArgument(
             ARG_NAME_BULK_MAX_PAYLOAD_SIZE);
    final IntegerArgument bulkMaxConcurrentRequestsArg =
         (IntegerArgument) parser.getNamedArgument(
             ARG_NAME_BULK_MAX_CONCURRENT_REQUESTS);
    final FileArgument tmpDataDirArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_TMP_DATA_DIR);
    final StringArgument tmpDataDirPermissionsArg =
         (StringArgument) parser.getNamedArgument(
             ARG_NAME_TMP_DATA_DIR_PERMISSIONS);

    final FilePermission tmpDataDirPermission;
    try
    {
      tmpDataDirPermission =
          FilePermission.decodeUNIXMode(tmpDataDirPermissionsArg.getValue());
    }
    catch(Exception e)
    {
      debugException(e);
      final String message = String.format(
          "The tmpDataDirPermissions %s is not a valid UNIX permission " +
          "mode in three-digit octal notation",
          tmpDataDirPermissionsArg.getValue());
      messages.add(message);
      return ResultCode.OTHER;
    }

    // Create the tmpDataDir directory if it doesn't exist.
    File tmpDataDir = tmpDataDirArg.getValue();
    if (tmpDataDir == null)
    {
      tmpDataDir =
          new File(serverContext.getServerRoot(), DEFAULT_TMP_DATA_DIR);
    }
    if (!tmpDataDir.exists())
    {
      if(!tmpDataDir.mkdirs())
      {
        final String message = String.format(
            "The tmpDataDir directory %s could not be created",
            tmpDataDir);
        messages.add(message);
        return ResultCode.OTHER;
      }
    }

    // Set permissions on the tmpDataDir directory.
    setPermissions(tmpDataDir, tmpDataDirPermission);

    backend.getConfig().setMaxResults(maxResultsArg.getValue());
    application.setBulkMaxOperations(
        bulkMaxOperationsArg.getValue().longValue());
    application.setBulkMaxPayloadSize(
        bulkMaxPayloadSizeArg.getValue().longValue());
    application.setBulkMaxConcurrentRequests(
        bulkMaxConcurrentRequestsArg.getValue());
    application.setTmpDataDir(tmpDataDir);

    final FileArgument useResourcesFileArg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_RESOURCES_FILE);
    try
    {
      final Map<ResourceDescriptor, ResourceMapper> resourceMappers =
          new HashMap<ResourceDescriptor, ResourceMapper>();
      final List<ResourceMapper> mappers =
          ResourceMapper.parse(useResourcesFileArg.getValue());
      for (final ResourceMapper resourceMapper : mappers)
      {
        resourceMappers.put(resourceMapper.getResourceDescriptor(),
                            resourceMapper);
      }

      backend.setResourceMappers(resourceMappers);
    }
    catch (Exception e)
    {
      debugException(e);
      messages.add("An error occurred while initializing the resources " +
                   "file: " + StaticUtils.getExceptionMessage(e));
      rc = ResultCode.OTHER;
    }

    // Re-register the monitor provider with the new config.
    try
    {
      final String monitorInstanceName = getMonitorInstanceName(config);
      serverContext.deregisterMonitorProvider(monitorProvider);
      monitorProvider = serverContext.registerMonitorProvider(
          new SCIMServletMonitorProvider(monitorInstanceName, this), config);
    }
    catch (LDAPException e)
    {
      debugException(e);
      messages.add("An error occurred while registering the monitor " +
                   "provider: " + StaticUtils.getExceptionMessage(e));
      rc = ResultCode.OTHER;
    }

    return rc;
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
             ARG_NAME_RESOURCES_FILE +
             "=extensions/com.unboundid.scim-extension/config/resources.xml",
             ARG_NAME_TMP_DATA_DIR +
             "=extensions/com.unboundid.scim-extension/tmp-data"),
         "Create a SCIM servlet that handles resources defined in " +
         "the specified XML file, and writes temporary request " +
         "data files to the specified directory.");

    return exampleMap;
  }



  /**
   * Retrieves the SCIM JAX-RS application instance used by this servlet.
   *
   * @return The SCIM JAX-RS application instance used by this servlet.
   */
  SCIMApplication getSCIMApplication()
  {
    return application;
  }



  /**
   * Get the name that identifies the monitor provider instance for this
   * extension. The returned name will include the name of the HTTP connection
   * handler that created this servlet extension.
   *
   * @param config  The general configuration for this HTTP servlet extension.
   *
   * @return  The name that identifies the monitor provider instance for this
   *          extension.
   *
   * @throws LDAPException  If the monitor instance name could not be
   *                        constructed.
   */
  private String getMonitorInstanceName(final HTTPServletExtensionConfig config)
      throws LDAPException
  {
    final DN dn = new DN(config.getHTTPConnectionHandlerConfigDN());
    return "SCIM Servlet (" + dn.getRDN().getAttributeValues()[0] + ")";
  }
}
