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



import java.io.File;



/**
 * This class provides a simple data structure with information that is
 * used to control the behavior of a {@link SCIMServer} instance.
 */
public class SCIMServerConfig
{
  /**
   * The port on which the server should accept client connections.
   */
  private int listenPort = 0;

  /**
   * The maximum number of threads available for incoming connections to the
   * server.
   */
  private int maxThreads = 16;

  /**
   * The XML schema to be used by the server.
   */
  private File[] schemaFiles;

  /**
   * The XML file defining the SCIM resources.
   */
  private File resourcesFile;

  /**
   * The path to the HTTP access log file.
   */
  private String accessLogFile;



  /**
   * Create a new server config with default information.
   */
  public SCIMServerConfig()
  {
  }



  /**
   * Retrieve the port on which the server should accept client connections.
   *
   * @return  The port on which the server should accept client connections.
   */
  public int getListenPort()
  {
    return listenPort;
  }



  /**
   * Specifies the port on which the server should accept client connections.
   *
   * @param listenPort  The port on which the server should accept client
   *                    connections.
   */
  public void setListenPort(final int listenPort)
  {
    this.listenPort = listenPort;
  }



  /**
   * Retrieve the maximum number of threads available for incoming connections
   * to the server.
   *
   * @return  The maximum number of threads available for incoming connections
   *          to the server.
   */
  public int getMaxThreads()
  {
    return maxThreads;
  }



  /**
   * Specifies the maximum number of threads available for incoming connections
   * to the server.
   *
   * @param maxThreads  The maximum number of threads available for incoming
   *                    connections to the server.
   */
  public void setMaxThreads(final int maxThreads)
  {
    this.maxThreads = maxThreads;
  }



  /**
   * Retrieve the XML file defining the SCIM resources.
   *
   * @return  The XML file defining the SCIM resources.
   */
  public File getResourcesFile()
  {
    return resourcesFile;
  }



  /**
   * Specifies the XML file defining the SCIM resources.
   *
   * @param resourcesFile  The XML file defining the SCIM resources.
   */
  public void setResourcesFile(final File resourcesFile)
  {
    this.resourcesFile = resourcesFile;
  }



  /**
   * Retrieve the path of the HTTP access log file.
   *
   * @return The path of the HTTP access log file or <code>null</code> if
   *         not specified.
   */
  public String getAccessLogFile() {
    return accessLogFile;
  }



  /**
   * Specifies the path of the HTTP access log file.
   *
   * @param accessLogFile The path of the HTTP access log file or
   *                      <code>null</code> to disable access logging.
   */
  public void setAccessLogFile(final String accessLogFile) {
    this.accessLogFile = accessLogFile;
  }
}
