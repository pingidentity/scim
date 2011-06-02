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
   * The base URI for the server.
   */
  private String baseURI = "/";

  /**
   * The maximum number of threads available for incoming connections to the
   * server.
   */
  private int maxThreads = 16;

  /**
   * The host name of the LDAP Directory Server instance.
   */
  private String dsHost = "localhost";

  /**
   * The port number of the LDAP Directory Server instance.
   */
  private int dsPort = -1;

  /**
   * The base DN for the LDAP Directory Server.
   */
  private String dsBaseDN = "dc=example,dc=com";

  /**
   * The bind DN for the LDAP Directory Server.
   */
  private String dsBindDN = "cn=Directory Manager";

  /**
   * The bind password for the LDAP Directory Server.
   */
  private String dsBindPassword = "password";




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
   * Retrieve the base URI for the server.
   *
   * @return  The base URI for the server.
   */
  public String getBaseURI()
  {
    return baseURI;
  }



  /**
   * Specifies the base URI for the server.
   *
   * @param baseURI  The base URI for the server.
   */
  public void setBaseURI(final String baseURI)
  {
    this.baseURI = baseURI;
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
   * Retrieve the the host name of the LDAP Directory Server instance.
   *
   * @return  The the host name of the LDAP Directory Server instance.
   */
  public String getDsHost()
  {
    return dsHost;
  }



  /**
   * Specifies the host name of the LDAP Directory Server instance.
   *
   * @param dsHost  The host name of the LDAP Directory Server instance.
   */
  public void setDsHost(final String dsHost)
  {
    this.dsHost = dsHost;
  }



  /**
   * Retrieve the port number of the LDAP Directory Server instance.
   *
   * @return  The port number of the LDAP Directory Server instance.
   */
  public int getDsPort()
  {
    return dsPort;
  }



  /**
   * Specifies the port number of the LDAP Directory Server instance.
   *
   * @param dsPort  The port number of the LDAP Directory Server instance.
   */
  public void setDsPort(final int dsPort)
  {
    this.dsPort = dsPort;
  }



  /**
   * Retrieve the base DN for the LDAP Directory Server.
   *
   * @return  The base DN for the LDAP Directory Server.
   */
  public String getDsBaseDN()
  {
    return dsBaseDN;
  }



  /**
   * Specifies the base DN for the LDAP Directory Server.
   *
   * @param dsBaseDN  The base DN for the LDAP Directory Server.
   */
  public void setDsBaseDN(final String dsBaseDN)
  {
    this.dsBaseDN = dsBaseDN;
  }



  /**
   * Retrieve the bind DN for the LDAP Directory Server.
   *
   * @return  The bind DN for the LDAP Directory Server.
   */
  public String getDsBindDN()
  {
    return dsBindDN;
  }



  /**
   * Specifies the bind DN for the LDAP Directory Server.
   *
   * @param dsBindDN  The bind DN for the LDAP Directory Server.
   */
  public void setDsBindDN(final String dsBindDN)
  {
    this.dsBindDN = dsBindDN;
  }



  /**
   * Retrieve the bind password for the LDAP Directory Server.
   *
   * @return  The bind password for the LDAP Directory Server.
   */
  public String getDsBindPassword()
  {
    return dsBindPassword;
  }



  /**
   * Specifies the bind password for the LDAP Directory Server.
   *
   * @param dsBindPassword  The bind password for the LDAP Directory Server.
   */
  public void setDsBindPassword(final String dsBindPassword)
  {
    this.dsBindPassword = dsBindPassword;
  }
}
