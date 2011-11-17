/*
 * Copyright 2011 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */

package com.unboundid.scim.ri;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMBackend;
import com.unboundid.util.CommandLineTool;
import com.unboundid.util.MinimalLogFormatter;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.BooleanArgument;
import com.unboundid.util.args.DNArgument;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;

import static com.unboundid.scim.ri.RIMessages.*;



/**
 * This class provides a command-line tool that can be used to run an instance
 * of the UnboundID SCIM Reference Implementation (RI) server using an
 * in-memory LDAP Directory Server as the resource repository.  Instances of
 * the SCIM RI server may also be created and controlled programmatically using
 * the {@link SCIMServer} class.
 * <BR><BR>
 * The following command-line arguments may be used with this class:
 * <UL>
 *   <LI>"-p {port}" or "--port {port}" -- specifies the port on which the
 *       server should listen for client HTTP connections.  If this is not
 *       provided, then a free port will be automatically chosen for use by the
 *       server.</LI>
 *   <LI>"--useResourcesFile {path}" -- specifies the path to an XML file
 *       containing resource definitions to use for the SCIM server.</LI>
 *   <LI>"-u {baseURI}" or "--baseURI {baseURI}" -- specifies a base URI to use
 *       for the SCIM server.  If no base URI is specified, then the default
 *       value '/' is used.</LI>
 *   <LI>"-b {baseDN}" or "--baseDN {baseDN}" -- specifies a base DN to use for
 *       the LDAP server.  If no base DN is specified, then the default value
 *       'dc=example,dc=com' is used.</LI>
 *   <LI>"-l {path}" or "--ldifFile {path}" -- specifies the path to an LDIF
 *       file to use to initially populate the server.  If this is not provided,
 *       then the server will initially be empty.  The LDIF file will not be
 *       updated as operations are processed in the server.</LI>
 *   <LI>"-a {path}" or "--accessLogFile {path}" -- specifies the path to a file
 *       that should be used as a server access log.  If this is not provided,
 *       then no access logging will be performed.</LI>
 *   <LI>"--ldapAccessLogFile {path}" -- specifies the path to a
 *       file that should be used as an LDAP server access log.  If this is not
 *       provided, then no LDAP access logging will be performed.</LI>
 *   <LI>"--useLdapSchemaFile {path}" -- specifies the path to a file
 *       or directory containing schema definitions to use for the LDAP
 *       repository.  If the specified path represents a file, then it must be
 *       an LDIF file containing a valid LDAP subschema subentry.  If the path
 *       is a directory, then its files will be processed in lexicographic
 *       order by name.</LI>
 *   <LI>"--maxResults {integer}" -- specifies the maximum number of resources
 *       that are returned in a response.  If no value is specified, then the
 *       server will use a default value of 100.</LI>
 * </UL>
 */
public class InMemoryServerTool
    extends CommandLineTool
{
  // The argument used to specify the base URIs to use for the SCIM server.
  private StringArgument baseURIArgument;

  // The argument used to specify the base DNs to use for the LDAP server.
  private DNArgument baseDNArgument;

  // The argument used to specify the path to an access log file to which
  // information should be written about operations processed by the server.
  private FileArgument accessLogFileArgument;

  // The argument used to specify the path to an access log file to which
  // information should be written about operations processed by the LDAP
  // server.
  private FileArgument ldapAccessLogFileArgument;

  // The argument used to specify the path to an LDIF file with data to use to
  // initially populate the server.
  private FileArgument ldifFileArgument;

  // The argument used to specify the path to a directory containing LDAP
  // schema definitions.
  private FileArgument useLdapSchemaFileArgument;

  // The argument used to specify the path to a directory containing XML
  // resource definitions.
  private FileArgument resourceMappingFileArgument;

  // The argument used to specify the maximum number of resources returned in
  // a response.
  private IntegerArgument maxResultsArgument;

  // The argument used to specify the port on which the server should listen.
  private IntegerArgument portArgument;

  // The argument used to specify the LDAP port.
  private IntegerArgument ldapPortArgument;

  // The argument used to indicate that the server should use SSL.
  private BooleanArgument useSSLArgument;

  // The argument used to specify the path to the SSL key store file.
  private FileArgument keyStorePathArgument;

  // The argument used to specify the path to the SSL trust store file.
  private FileArgument trustStorePathArgument;

  // The argument used to specify the password to use to access the contents of
  // the SSL key store
  private StringArgument keyStorePasswordArgument;

  // The argument used to specify the password to use to access the contents of
  // the SSL trust store
  private StringArgument trustStorePasswordArgument;

  // The in-memory directory server instance that has been created by this tool.
  private InMemoryDirectoryServer directoryServer;

  // The SCIM server instance that has been created by this tool.
  private SCIMServer scimServer;



  /**
   * Parse the provided command line arguments and uses them to start the
   * server.
   *
   * @param  args  The command line arguments provided to this program.
   */
  public static void main(final String... args)
  {
    final ResultCode resultCode = main(args, System.out, System.err);
    if (resultCode != ResultCode.SUCCESS)
    {
      System.exit(resultCode.intValue());
    }
  }



  /**
   * Parse the provided command line arguments and uses them to start the
   * server.
   *
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   * @param  args       The command line arguments provided to this program.
   *
   * @return  A result code indicating whether the processing was successful.
   */
  public static ResultCode main(final String[] args,
                                final OutputStream outStream,
                                final OutputStream errStream)
  {
    final InMemoryServerTool tool =
         new InMemoryServerTool(outStream, errStream);
    return tool.runTool(args);
  }



  /**
   * Creates a new instance of this tool that use the provided output streams
   * for standard output and standard error.
   *
   * @param  outStream  The output stream to use for standard output.  It may be
   *                    {@code System.out} for the JVM's default standard output
   *                    stream, {@code null} if no output should be generated,
   *                    or a custom output stream if the output should be sent
   *                    to an alternate location.
   * @param  errStream  The output stream to use for standard error.  It may be
   *                    {@code System.err} for the JVM's default standard error
   *                    stream, {@code null} if no output should be generated,
   *                    or a custom output stream if the output should be sent
   *                    to an alternate location.
   */
  public InMemoryServerTool(final OutputStream outStream,
                            final OutputStream errStream)
  {
    super(outStream, errStream);

    scimServer                     = null;
    directoryServer                = null;
    baseURIArgument                = null;
    baseDNArgument                 = null;
    accessLogFileArgument          = null;
    ldapAccessLogFileArgument      = null;
    ldifFileArgument               = null;
    resourceMappingFileArgument    = null;
    useLdapSchemaFileArgument      = null;
    portArgument                   = null;
    maxResultsArgument             = null;
    ldapPortArgument               = null;
    useSSLArgument                 = null;
    keyStorePathArgument           = null;
    trustStorePathArgument         = null;
    keyStorePasswordArgument       = null;
    trustStorePasswordArgument     = null;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getToolName()
  {
    return "in-memory-scim-server";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public String getToolDescription()
  {
    return INFO_MEM_SERVER_TOOL_DESC.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void addToolArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    final DN baseDN;
    try
    {
      baseDN = new DN("dc=example,dc=com");
    }
    catch (LDAPException e)
    {
      Debug.debugException(e);
      throw new ArgumentException(e.getExceptionMessage());
    }

    resourceMappingFileArgument = new FileArgument(
        null, "resourceMappingFile", true, 1,
        INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PATH.get(),
        INFO_MEM_SERVER_TOOL_ARG_DESC_USE_RESOURCES_FILE.get(), true, true,
        false, false);
    parser.addArgument(resourceMappingFileArgument);

    portArgument = new IntegerArgument('p', "port", false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PORT.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_PORT.get(), 0, 65535);
    parser.addArgument(portArgument);

    maxResultsArgument =
        new IntegerArgument(
            null, "maxResults",
            true, 1,
            INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_MAX_RESULTS.get(),
            INFO_MEM_SERVER_TOOL_ARG_DESC_MAX_RESULTS.get(),
            1, Integer.MAX_VALUE, 100);

    baseURIArgument = new StringArgument('u', "baseURI", false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_BASE_URI.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_BASE_URI.get(),
         Arrays.asList("/"));
    parser.addArgument(baseURIArgument);

    ldapPortArgument = new IntegerArgument(null, "ldapPort", false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_LDAP_PORT.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_LDAP_PORT.get(), 0, 65535, 1389);
    parser.addArgument(ldapPortArgument);

    baseDNArgument = new DNArgument('b', "baseDN", false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_BASE_DN.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_BASE_DN.get(),
         Arrays.asList(baseDN));
    parser.addArgument(baseDNArgument);

    ldifFileArgument = new FileArgument('l', "ldifFile", false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PATH.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_LDIF_FILE.get(), true, true, true,
         false);
    parser.addArgument(ldifFileArgument);

    accessLogFileArgument = new FileArgument('a', "accessLogFile", false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PATH.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_ACCESS_LOG_FILE.get(), false, true, true,
         false);
    parser.addArgument(accessLogFileArgument);

    ldapAccessLogFileArgument = new FileArgument(null, "ldapAccessLogFile",
         false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PATH.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_LDAP_ACCESS_LOG_FILE.get(), false, true,
         true, false);
    parser.addArgument(ldapAccessLogFileArgument);

    useLdapSchemaFileArgument = new FileArgument(null, "useLdapSchemaFile",
         false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PATH.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_USE_LDAP_SCHEMA_FILE.get(), true, true,
         false, false);
    parser.addArgument(useLdapSchemaFileArgument);

    useSSLArgument = new BooleanArgument('Z', "useSSL",
         INFO_MEM_SERVER_TOOL_ARG_DESC_USE_SSL.get());
    parser.addArgument(useSSLArgument);

    keyStorePathArgument = new FileArgument('K', "keyStorePath", false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PATH.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_KEY_STORE_PATH.get(), true, true, true,
         false);
    parser.addArgument(keyStorePathArgument);

    keyStorePasswordArgument = new StringArgument('W', "keyStorePassword",
         false, 1, INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PASSWORD.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_KEY_STORE_PW.get());
    parser.addArgument(keyStorePasswordArgument);

    trustStorePathArgument = new FileArgument('P', "trustStorePath", false, 1,
         INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PATH.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_TRUST_STORE_PATH.get(), true, true, true,
         false);
    parser.addArgument(trustStorePathArgument);

    trustStorePasswordArgument = new StringArgument('T', "trustStorePassword",
         false, 1, INFO_MEM_SERVER_TOOL_ARG_PLACEHOLDER_PASSWORD.get(),
         INFO_MEM_SERVER_TOOL_ARG_DESC_TRUST_STORE_PW.get());
    parser.addArgument(trustStorePasswordArgument);

    parser.addDependentArgumentSet(useSSLArgument, keyStorePathArgument);
    parser.addDependentArgumentSet(useSSLArgument, keyStorePasswordArgument);
    parser.addDependentArgumentSet(keyStorePathArgument, useSSLArgument);
    parser.addDependentArgumentSet(keyStorePasswordArgument, useSSLArgument);
    parser.addDependentArgumentSet(trustStorePathArgument, useSSLArgument);
    parser.addDependentArgumentSet(trustStorePasswordArgument,
         trustStorePathArgument);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public ResultCode doToolProcessing()
  {
    // Create a base configuration for the LDAP server.
    final InMemoryDirectoryServerConfig ldapConfig;
    try
    {
      ldapConfig = getLDAPConfig();
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      err(ERR_MEM_SERVER_TOOL_ERROR_INITIALIZING_CONFIG.get(
          StaticUtils.getExceptionMessage(le)));
      return le.getResultCode();
    }


    // Create the LDAP server instance using the provided configuration, but
    // don't start it yet.
    try
    {
      directoryServer = new InMemoryDirectoryServer(ldapConfig);
    }
    catch (final LDAPException le)
    {
      Debug.debugException(le);
      err(ERR_MEM_SERVER_TOOL_ERROR_CREATING_SERVER_INSTANCE.get(
          StaticUtils.getExceptionMessage(le)));
      return le.getResultCode();
    }


    // If an LDIF file was provided, then use it to populate the LDAP server.
    if (ldifFileArgument.isPresent())
    {
      final File ldifFile = ldifFileArgument.getValue();
      try
      {
        final int numEntries = directoryServer.importFromLDIF(true,
             ldifFile.getAbsolutePath());
        out(INFO_MEM_SERVER_TOOL_ADDED_ENTRIES_FROM_LDIF.get(numEntries,
             ldifFile.getAbsolutePath()));
      }
      catch (final LDAPException le)
      {
        Debug.debugException(le);
        err(ERR_MEM_SERVER_TOOL_ERROR_POPULATING_SERVER_INSTANCE.get(
             ldifFile.getAbsolutePath(), StaticUtils.getExceptionMessage(le)));
        return le.getResultCode();
      }
    }


    // Create a base configuration for the SCIM server.
    final SCIMServerConfig serverConfig = getSCIMConfig();

    // If SSL is to be used, then create the corresponding socket factories.
    if (useSSLArgument.isPresent())
    {
      try
      {
        final KeyManager keyManager = new KeyStoreKeyManager(
             keyStorePathArgument.getValue(),
             keyStorePasswordArgument.getValue().toCharArray());

        final TrustManager trustManager;
        if (trustStorePathArgument.isPresent())
        {
          final char[] password;
          if (trustStorePasswordArgument.isPresent())
          {
            password = trustStorePasswordArgument.getValue().toCharArray();
          }
          else
          {
            password = null;
          }

          trustManager = new TrustStoreTrustManager(
               trustStorePathArgument.getValue(), password, "JKS", true);
        }
        else
        {
          trustManager = new TrustAllTrustManager();
        }

        final SSLUtil serverSSLUtil = new SSLUtil(keyManager, trustManager);

        serverConfig.setSslContext(serverSSLUtil.createSSLContext());
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
        err(ERR_MEM_SERVER_TOOL_ERROR_INITIALIZING_SSL.get(
            StaticUtils.getExceptionMessage(e)));
        return ResultCode.LOCAL_ERROR;
      }
    }

    // Create the SCIM server instance using the provided configuration, but
    // don't start it yet.
    scimServer = SCIMServer.getInstance();
    try
    {
      scimServer.initializeServer(serverConfig);
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      err(ERR_MEM_SERVER_TOOL_ERROR_INITIALIZING_SERVER.get(
          StaticUtils.getExceptionMessage(e)));
      return ResultCode.LOCAL_ERROR;
    }

    final String baseURI = baseURIArgument.getValue();
    final SCIMBackend backend =
        new InMemoryLDAPBackend(scimServer.getResourceMappers(),
                                directoryServer);
    backend.getConfig().setMaxResults(maxResultsArgument.getValue());

    scimServer.registerBackend(baseURI, backend);

    // Start the server.
    try
    {
      directoryServer.startListening();
      scimServer.startListening();
      out(INFO_MEM_SERVER_TOOL_LISTENING.get(
          useSSLArgument.isPresent() ? "HTTPS" : "HTTP",
          scimServer.getListenPort(),
          directoryServer.getListenPort()));
    }
    catch (final Exception e)
    {
      Debug.debugException(e);
      err(ERR_MEM_SERVER_TOOL_ERROR_STARTING_SERVER.get(
          StaticUtils.getExceptionMessage(e)));
      return ResultCode.LOCAL_ERROR;
    }

    return ResultCode.SUCCESS;
  }



  /**
   * Creates a SCIM server configuration based on information provided with
   * command line arguments.
   *
   * @return  The configuration that was created.
   *
   */
  private SCIMServerConfig getSCIMConfig()
  {
    final SCIMServerConfig serverConfig = new SCIMServerConfig();

    final int listenPort;
    if (portArgument.isPresent())
    {
      listenPort = portArgument.getValue();
    }
    else if (useSSLArgument.isPresent())
    {
      listenPort = 8443;
    }
    else
    {
      listenPort = 8080;
    }
    serverConfig.setListenPort(listenPort);

    serverConfig.setResourcesFile(resourceMappingFileArgument.getValue());

    // If an access log file was specified, then create the appropriate log
    // handler.
    if (accessLogFileArgument.isPresent())
    {
      serverConfig.setAccessLogFile(
          accessLogFileArgument.getValue().getAbsolutePath());
    }
    return serverConfig;
  }



  /**
   * Creates an LDAP server configuration based on information provided with
   * command line arguments.
   *
   * @return  The configuration that was created.
   *
   * @throws  LDAPException  If a problem is encountered while creating the
   *                         configuration.
   */
  private InMemoryDirectoryServerConfig getLDAPConfig()
          throws LDAPException
  {
    final List<DN> dnList = baseDNArgument.getValues();
    final DN[] baseDNs = new DN[dnList.size()];
    dnList.toArray(baseDNs);

    final InMemoryDirectoryServerConfig serverConfig =
         new InMemoryDirectoryServerConfig(baseDNs);

    if (ldapPortArgument.isPresent())
    {
      serverConfig.setListenerConfigs(
          InMemoryListenerConfig.createLDAPConfig("default",
                                                  ldapPortArgument.getValue()));
    }


    // TODO: config argument for manager password
    serverConfig.addAdditionalBindCredentials("cn=Directory Manager",
                                              "password");
    serverConfig.addAdditionalBindCredentials("cn=Manager",
                                              "password");

    // If schema should be used, then get it.
    if (!useLdapSchemaFileArgument.isPresent())
    {
      serverConfig.setSchema(Schema.getDefaultStandardSchema());
    }
    else if (useLdapSchemaFileArgument.isPresent())
    {
      final File f = useLdapSchemaFileArgument.getValue();
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
          try
          {
            serverConfig.setSchema(Schema.getSchema(schemaFiles));
          }
          catch (final Exception e)
          {
            Debug.debugException(e);
            throw new LDAPException(ResultCode.LOCAL_ERROR,
                 ERR_MEM_SERVER_TOOL_ERROR_READING_SCHEMA.get(
                      f.getAbsolutePath(), StaticUtils.getExceptionMessage(e)),
                 e);
          }
        }
      }
    }
    else
    {
      serverConfig.setSchema(null);
    }


    // If an access log file was specified, then create the appropriate log
    // handler.
    if (ldapAccessLogFileArgument.isPresent())
    {
      final File logFile = ldapAccessLogFileArgument.getValue();
      try
      {
        final FileHandler handler =
             new FileHandler(logFile.getAbsolutePath(), true);
        handler.setLevel(Level.INFO);
        handler.setFormatter(new MinimalLogFormatter(null, false, false,
             true));
        serverConfig.setAccessLogHandler(handler);
      }
      catch (final Exception e)
      {
        Debug.debugException(e);
        throw new LDAPException(ResultCode.LOCAL_ERROR,
             ERR_MEM_SERVER_TOOL_ERROR_CREATING_LOG_HANDLER.get(
                  logFile.getAbsolutePath(),
                  StaticUtils.getExceptionMessage(e)),
             e);
      }
    }

    return serverConfig;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public LinkedHashMap<String[],String> getExampleUsages()
  {
    final LinkedHashMap<String[],String> exampleUsages =
         new LinkedHashMap<String[],String>(2);

    final String[] example1Args =
    {
        "--useResourcesFile", "config/resources.xml"
    };
    exampleUsages.put(example1Args, INFO_MEM_SERVER_TOOL_EXAMPLE_1.get());

    final String[] example2Args =
    {
      "--useResourcesFile", "config/resources.xml",
      "--baseURI", "scim",
      "--port", "8181",
      "--ldifFile", "test.ldif"
    };
    exampleUsages.put(example2Args, INFO_MEM_SERVER_TOOL_EXAMPLE_2.get());

    return exampleUsages;
  }



}
