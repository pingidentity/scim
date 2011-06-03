/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ldap;



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



}
