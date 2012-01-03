/*
 * Copyright 2011-2012 UnboundID Corp.
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

import com.unboundid.ldap.sdk.AbstractConnectionPool;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.SingleServerSet;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;



/**
 * This class is used to interact with an external LDAP Directory Server.
 */
public class LDAPExternalServer
{
  /**
   * A reference to an LDAP connection pool that may be used to
   * interact with the LDAP server.
   */
  private final AtomicReference<AbstractConnectionPool> connPool;

  /**
   * The configuration of this LDAP external server.
   */
  private LDAPExternalServerConfig config;

  /**
   * The set of result codes that will cause an LDAP connection to be considered
   * defunct.
   */
  private static final Set<ResultCode> defunctResultCodes;
  static
  {
    defunctResultCodes = new HashSet<ResultCode>();
    defunctResultCodes.add(ResultCode.OPERATIONS_ERROR);
    defunctResultCodes.add(ResultCode.PROTOCOL_ERROR);
    defunctResultCodes.add(ResultCode.BUSY);
    defunctResultCodes.add(ResultCode.UNAVAILABLE);
    defunctResultCodes.add(ResultCode.UNWILLING_TO_PERFORM);
    defunctResultCodes.add(ResultCode.OTHER);
    defunctResultCodes.add(ResultCode.SERVER_DOWN);
    defunctResultCodes.add(ResultCode.LOCAL_ERROR);
    defunctResultCodes.add(ResultCode.ENCODING_ERROR);
    defunctResultCodes.add(ResultCode.DECODING_ERROR);
    defunctResultCodes.add(ResultCode.NO_MEMORY);
    defunctResultCodes.add(ResultCode.CONNECT_ERROR);
  }





  /**
   * Create a new instance of an LDAP external server from the provided
   * information.
   *
   * @param config  The configuration of this LDAP external server.
   */
  public LDAPExternalServer(final LDAPExternalServerConfig config)
  {
    this.config = config;
    this.connPool = new AtomicReference<AbstractConnectionPool>();
  }



  /**
   * Closes all connections to the LDAP server.
   */
  public void close()
  {
    final AbstractConnectionPool connectionPool = connPool.getAndSet(null);
    if (connectionPool != null)
    {
      connectionPool.close();
    }
  }



  /**
   * Retrieves the connection pool that may be used for LDAP operations.
   *
   * @return  The connection pool that may be used for LDAP operations.
   *
   * @throws LDAPException  If the pool is not already connected and a new pool
   *                        cannot be created.
   */
  public AbstractConnectionPool getLDAPConnectionPool()
      throws LDAPException
  {
    AbstractConnectionPool p = connPool.get();

    if ((p != null) && p.isClosed())
    {
      connPool.compareAndSet(p, null);
      p = null;
    }

    if (p == null)
    {
      p = createPool();

      if (! connPool.compareAndSet(null, p))
      {
        p.close();
        return connPool.get();
      }
    }

    return p;
  }



  /**
   * Create a connection to the LDAP server.
   *
   * @return  A connection to the LDAP server.
   *
   * @throws  LDAPException  If a problem occurs while creating the connection.
   */
  public LDAPConnection getLDAPConnection()
      throws LDAPException
  {
    final LDAPConnection connection =
        new LDAPConnection(config.getDsHost(),
                           config.getDsPort());

    final BindRequest bindRequest =
        new SimpleBindRequest(config.getDsBindDN(),
                              config.getDsBindPassword());
    connection.bind(bindRequest);

    return connection;
  }



  /**
   * Creates a new LDAP connection pool.
   *
   * @return  The created LDAP connection pool.
   *
   * @throws  LDAPException  If a problem occurs while creating the connection
   *                         pool.
   */
  private AbstractConnectionPool createPool()
      throws LDAPException
  {
    final SingleServerSet dsServerSet =
        new SingleServerSet(config.getDsHost(),
                            config.getDsPort());
    final BindRequest bindRequest =
        new SimpleBindRequest(config.getDsBindDN(),
                              config.getDsBindPassword());
    return new LDAPConnectionPool(dsServerSet, bindRequest,
                                  config.getNumConnections());
  }
}
