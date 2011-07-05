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



import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.unboundid.directory.sdk.common.api.MonitorProvider;
import com.unboundid.directory.sdk.common.config.MonitorProviderConfig;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.FileArgument;



/**
 * This class provides a simple example of a monitor provider that will report
 * information about a specified file.  Monitor attributes will provide the
 * name of the file, the size in bytes, the time that it was last modified, and
 * the contents of that file.  It takes a single configuration argument:
 * <UL>
 *   <LI>data-file -- The path to the file containing the data to use for this
 *       monitor provider.</LI>
 * </UL>
 */
public final class ExampleMonitorProvider
       extends MonitorProvider
{
  /**
   * The name of the argument that will be used for the argument used to specify
   * the path to the data file.
   */
  private static final String ARG_NAME_DATA_FILE = "data-file";



  // The path to the data file to be read.
  private volatile File dataFile;

  // General configuration for this monitor provider.
  private volatile MonitorProviderConfig config;

  // The server context for the server in which this extension is running.
  private ServerContext serverContext;



  /**
   * Creates a new instance of this monitor provider.  All monitor provider
   * implementations must include a default constructor, but any initialization
   * should generally be done in the {@code initializeMonitorProvider} method.
   */
  public ExampleMonitorProvider()
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
    return "Example Monitor Provider";
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
      "This monitor provider serves an example that may be used to " +
           "demonstrate the process for creating a third-party monitor " +
           "provider.  It will provide information about a specified file on " +
           "filesystem, including its size, last-modified time, and content."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this monitor provider.  The argument parser may also
   * be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this monitor provider.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the path to the data file.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_DATA_FILE;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{path}";
    String    description     = "The path to the data file to be read.  " +
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
   * Initializes this monitor provider.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this monitor provider.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this monitor provider.
   *
   * @throws  LDAPException  If a problem occurs while initializing this monitor
   *                         provider.
   */
  @Override()
  public void initializeMonitorProvider(final ServerContext serverContext,
                                        final MonitorProviderConfig config,
                                        final ArgumentParser parser)
         throws LDAPException
  {
    this.serverContext = serverContext;
    this.config = config;

    // Get the data file path.
    final FileArgument arg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_DATA_FILE);
    dataFile = arg.getValue();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this monitor
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
  public boolean isConfigurationAcceptable(final MonitorProviderConfig config,
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
   * @param  config                The general configuration for this monitor
   *                               provider.
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
  public ResultCode applyConfiguration(final MonitorProviderConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the path to the data file from the new config.
    final FileArgument arg =
         (FileArgument) parser.getNamedArgument(ARG_NAME_DATA_FILE);
    dataFile = arg.getValue();

    this.config = config;

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this monitor provider is
   * to be taken out of service.
   */
  @Override()
  public void finalizeMonitorProvider()
  {
    // No implementation required.
  }



  /**
   * Retrieves the name that identifies this monitor provider instance.  It
   * will be used as the value of the naming attribute for monitor entries.
   * Each monitor provider instance must have a unique name.
   *
   * @return  The name that identifies this monitor provider instance.
   */
  @Override()
  public String getMonitorInstanceName()
  {
    // The monitor instance name will be based on the name of the config entry.
    return "Example Monitor Provider " + config.getConfigObjectName();
  }



  /**
   * Retrieves the name of the object class that will be used for monitor
   * entries created from this monitor provider.  It may be {@code null} if
   * a default object class should be used.
   *
   * @return  The name of the object class that will be used for monitor entries
   *          created from this monitor provider.
   */
  @Override()
  public String getMonitorObjectClass()
  {
    return "example-monitor-entry";
  }



  /**
   * Retrieves the update interval in milliseconds that should be used for this
   * monitor provider.  A value that is greater than zero will cause the
   * {@link #updateMonitorData} method to be repeatedly invoked at that
   * interval.  A value less than or equal to zero indicates that the monitor
   * provider should not be periodically updated.
   *
   * @return  The update interval in milliseconds that should be used for this
   *          monitor provider.
   */
  @Override()
  public long getUpdateIntervalMillis()
  {
    // This monitor provider will not be periodically updated by default.
    return 0L;
  }



  /**
   * Updates the information collected by this monitor provider.  This method
   * will be periodically invoked if the {@link #getUpdateIntervalMillis} method
   * returns a positive value.
   */
  @Override()
  public void updateMonitorData()
  {
    // No implementation provided by default.
  }



  /**
   * Retrieves a list of attributes containing the data to include in the
   * monitor entry generated from this monitor provider.
   *
   * @return  A list of attributes containing the data to include in the monitor
   *          entry generated from this monitor provider.
   */
  @Override()
  public List<Attribute> getMonitorAttributes()
  {
    // Get a local reference to the data file in case the configuration changes
    // while we're in the middle of the method.
    final File f = dataFile;

    final ArrayList<Attribute> attrList = new ArrayList<Attribute>(4);
    attrList.add(new Attribute("data-file", f.getAbsolutePath()));

    if (f.exists())
    {
      final int length = (int) f.length();
      attrList.add(new Attribute("file-size", String.valueOf(length)));
      attrList.add(new Attribute("last-modified-time",
           new Date(f.lastModified()).toString()));

      BufferedInputStream inputStream = null;
      try
      {
        inputStream = new BufferedInputStream(new FileInputStream(f));

        final ByteArrayOutputStream outputStream =
             new ByteArrayOutputStream(length);
        final byte[] buffer = new byte[8192];
        while (true)
        {
          final int bytesRead = inputStream.read(buffer);
          if (bytesRead < 0)
          {
            break;
          }
          else
          {
            outputStream.write(buffer, 0, bytesRead);
          }
        }

        attrList.add(new Attribute("file-data", outputStream.toByteArray()));
      }
      catch (final Exception e)
      {
        serverContext.debugCaught(e);
        attrList.add(new Attribute("message",
             "Unable to read the file contents:  " +
                  StaticUtils.getExceptionMessage(e)));
      }
      finally
      {
        if (inputStream != null)
        {
          try
          {
            inputStream.close();
          }
          catch (final Exception e)
          {
            serverContext.debugCaught(e);
          }
        }
      }
    }
    else
    {
      attrList.add(new Attribute("message", "The file does not exist"));
    }

    return attrList;
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
         Arrays.asList(ARG_NAME_DATA_FILE + "=config/monitor-data.txt"),
         "Provide a monitor file with access to information contained in " +
              "the config/monitor-data.txt file below the server root.");

    return exampleMap;
  }
}
