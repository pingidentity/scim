/*
 * Copyright 2011-2013 UnboundID Corp.
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



import javax.net.ssl.SSLContext;
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
   * The SSL context that should be used for secure client connections, or
   * {@code null} if SSL should not be used.
   */
  private SSLContext sslContext = null;

  /**
   * The maximum number of threads available for incoming connections to the
   * server.
   */
  private int maxThreads = 16;

  /**
   * The XML file defining the SCIM resources.
   */
  private File resourcesFile;

  /**
   * The path to the HTTP access log file.
   */
  private String accessLogFile;

  /**
   * The maximum number of operations permitted in a bulk request.
   */
  private long bulkMaxOperations = Long.MAX_VALUE;

  /**
   * The maximum payload size in bytes of a bulk request.
   */
  private long bulkMaxPayloadSize = Long.MAX_VALUE;

  /**
   * The maximum number of concurrent bulk requests.
   */
  private int bulkMaxConcurrentRequests = Integer.MAX_VALUE;

  /**
   * The maximum number of resources that are returned in a response.
   */
  private int maxResults = Integer.MAX_VALUE;


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



  /**
   * Retrieve the SSL context that should be used for secure client connections.
   * @return  The SSL context that should be used for secure client connections,
   *          or {@code null} if SSL should not be used.
   */
  public SSLContext getSslContext()
  {
    return sslContext;
  }



  /**
   * Specifies the SSL context that should be used for secure client
   * connections.
   *
   * @param sslContext  The SSL context that should be used for secure client
   *                    connections, or {@code null} if SSL should not be used.
   */
  public void setSslContext(final SSLContext sslContext)
  {
    this.sslContext = sslContext;
  }



  /**
   * Retrieve the maximum number of concurrent bulk requests.
   *
   * @return The maximum number of concurrent bulk requests.
   */
  public int getBulkMaxConcurrentRequests()
  {
    return bulkMaxConcurrentRequests;
  }



  /**
   * Specify the maximum number of concurrent bulk requests.
   * @param bulkMaxConcurrentRequests  The maximum number of concurrent bulk
   *                                   requests.
   */
  public void setBulkMaxConcurrentRequests(final int bulkMaxConcurrentRequests)
  {
    this.bulkMaxConcurrentRequests = bulkMaxConcurrentRequests;
  }



  /**
   * Retrieve the maximum number of operations permitted in a bulk request.
   * @return The maximum number of operations permitted in a bulk request.
   */
  public long getBulkMaxOperations()
  {
    return bulkMaxOperations;
  }



  /**
   * Specify the maximum number of operations permitted in a bulk request.
   * @param bulkMaxOperations  The maximum number of operations permitted in a
   *                           bulk request.
   */
  public void setBulkMaxOperations(final long bulkMaxOperations)
  {
    this.bulkMaxOperations = bulkMaxOperations;
  }



  /**
   * Retrieve the maximum payload size in bytes of a bulk request.
   *
   * @return The maximum payload size in bytes of a bulk request.
   */
  public long getBulkMaxPayloadSize()
  {
    return bulkMaxPayloadSize;
  }



  /**
   * Specify the maximum payload size in bytes of a bulk request.
   * @param bulkMaxPayloadSize  The maximum payload size in bytes of a bulk
   *                            request.
   */
  public void setBulkMaxPayloadSize(final long bulkMaxPayloadSize)
  {
    this.bulkMaxPayloadSize = bulkMaxPayloadSize;
  }



  /**
   * Retrieve the maximum number of resources that are returned in a response.
   * @return  The maximum number of resources that are returned in a response.
   */
  public int getMaxResults()
  {
    return maxResults;
  }



  /**
   * Specify the maximum number of resources that are returned in a response.
   * @param maxResults  The maximum number of resources that are returned in
   *                    a response.
   */
  public void setMaxResults(final int maxResults)
  {
    this.maxResults = maxResults;
  }
}
