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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import com.unboundid.directory.sdk.sync.scripting.ScriptedSyncDestination;
import com.unboundid.directory.sdk.sync.config.SyncDestinationConfig;
import com.unboundid.directory.sdk.sync.types.EndpointException;
import com.unboundid.directory.sdk.sync.types.SyncOperation;
import com.unboundid.directory.sdk.sync.types.SyncServerContext;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.DNArgument;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;
import com.unboundid.ldap.sdk.ChangeLogEntry;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPThreadLocalConnectionPool;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ResultCode;


/**
 * This example provides an implementation of SyncDestination which can be used
 * as a one-way notification destination. Its configuration arguments require
 * the host and port of a target LDAP directory, to which changes are pushed
 * straight through. For reference, the fetchEntry() method is implemented
 * as well, although it is not used when a Sync Pipe is in <i>notification</i>
 * mode.
 */
public class ExampleScriptedSyncDestination extends ScriptedSyncDestination
{

  // The general configuration for this Sync Destination
  private volatile SyncDestinationConfig config;

  // The server context for the server in which this extension is running
  private SyncServerContext serverContext;

  // A pool of connections to the destination LDAP server
  private LDAPThreadLocalConnectionPool connectionPool;


  /**
   * {@inheritDoc}
   */
  @Override
  public void defineConfigArguments(final ArgumentParser parser)
                  throws ArgumentException
  {
    StringArgument hostArg = new StringArgument(
                                 null, "host", true, 1, "{hostname}",
                                 "The hostname of the target directory " +
                                 "server to which notifications should be " +
                                 "sent.");

    IntegerArgument portArg = new IntegerArgument(
                                 null, "port", true, 1, "{port}",
                                 "The port number of the target directory " +
                                 "server to which notifications should be " +
                                 "sent.");

    DNArgument bindDNArg = new DNArgument(
                                 null, "bind-dn", true, 1, "{bind-DN}",
                                 "The bind DN to use for requests to the " +
                                 "target directory server.");

    StringArgument bindPasswordArg = new StringArgument(
                                 null, "bind-password", true, 1,
                                 "{bind-password}", "The bind password to " +
                                 "use for requests to the target directory " +
                                 "server.");

    parser.addArgument(hostArg);
    parser.addArgument(portArg);
    parser.addArgument(bindDNArg);
    parser.addArgument(bindPasswordArg);
  }


  /**
   * Initializes this sync destination. This hook is called when a Sync Pipe
   * first starts up, or when the <i>resync</i> process first starts up. Any
   * initialization should be performed here. This method should generally store
   * the {@link SyncServerContext} in a class
   * member so that it can be used elsewhere in the implementation.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running. Extensions should
   *                        typically store this in a class member.
   * @param  config         The general configuration for this object.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this sync destination.
   * @throws  EndpointException
   *                        if a problem occurs while initializing this
   *                        sync destination.
   */
  @Override()
  public void initializeSyncDestination(
                                    final SyncServerContext serverContext,
                                    final SyncDestinationConfig config,
                                    final ArgumentParser parser)
                                           throws EndpointException
  {
    this.serverContext = serverContext;
    this.config        = config;

    StringArgument hostArg = parser.getNamedArgument("host");

    IntegerArgument portArg = parser.getNamedArgument("port");

    DNArgument bindDNArg = parser.getNamedArgument("bind-dn");

    StringArgument passwordArg = parser.getNamedArgument("bind-password");

    String host = hostArg.getValue();
    int port = portArg.getValue();
    DN bindDN = bindDNArg.getValue();
    String bindPassword = passwordArg.getValue();

    try
    {
      LDAPConnection conn = new LDAPConnection(host,
                                               port,
                                               bindDN.toString(),
                                               bindPassword);
      connectionPool = new LDAPThreadLocalConnectionPool(conn);
    }
    catch(LDAPException e)
    {
      throw new EndpointException(e);
    }
  }


  /**
   * This hook is called when a Sync Pipe shuts down, or when the <i>resync</i>
   * process shuts down. Any clean-up of this sync destination should be
   * performed here.
   */
  @Override
  public void finalizeSyncDestination()
  {
    if(connectionPool != null)
    {
      connectionPool.close();
    }
  }



  /**
   * Return the URL or path identifying the destination endpoint
   * to which this extension is transmitting data. This is used for logging
   * purposes only, so it could just be a server name or hostname and port, etc.
   *
   * @return the path to the destination endpoint
   */
  @Override
  public String getCurrentEndpointURL()
  {
    if(connectionPool == null)
    {
      return "not connected";
    }

    LDAPConnection conn = null;
    try
    {
      conn = connectionPool.getConnection();
      return conn.getConnectedAddress() + ":" + conn.getConnectedPort();
    }
    catch (LDAPException e)
    {
      return "not connected";
    }
    finally
    {
      connectionPool.releaseConnection(conn);
    }
  }



  /**
   * Return a full destination entry (in LDAP form) from the destination
   * endpoint, corresponding to the the source {@link Entry} that is passed in.
   * This method should perform any queries necessary to gather the latest
   * values for all the attributes to be synchronized and return them in an
   * Entry.
   * <p>
   * This method only needs to be implemented if the 'synchronization-mode' on
   * the Sync Pipe is set to 'standard'. If it is set to 'notification', this
   * method will never be called, and the pipe will pass changes straight
   * through to one of {@link #createEntry}, {@link #modifyEntry}, or
   * {@link #deleteEntry}.
   * <p>
   * Note that the if the source entry was renamed (see
   * {@link SyncOperation#isModifyDN}), the
   * <code>destEntryMappedFromSrc</code> will have the new DN; the old DN can
   * be obtained by calling
   * {@link SyncOperation#getDestinationEntryBeforeChange()} and getting the DN
   * from there. This method should return the entry in its existing form
   * (i.e. with the old DN, before it is changed).
   * <p>
   * This method <b>must be thread safe</b>, as it will be called repeatedly and
   * concurrently by each of the Sync Pipe worker threads as they process
   * entries.
   * @param destEntryMappedFromSrc
   *          the LDAP entry which corresponds to the destination "entry" to
   *          fetch
   * @param  operation
   *          the sync operation for this change
   * @return a list containing the full LDAP entries that matched this search
   *          (there may be more than one), or an empty list if no such entry
   *          exists
   * @throws EndpointException
   *           if there is an error fetching the entry
   */
  @Override
  public List<Entry> fetchEntry(final Entry destEntryMappedFromSrc,
                                final SyncOperation operation)
                                   throws EndpointException
  {
    List<Entry> entries = new ArrayList<Entry>();
    try
    {
      Entry e = connectionPool.getEntry(destEntryMappedFromSrc.getDN());
      if(e != null)
      {
        entries.add(e);
      }
      return entries;
    }
    catch(LDAPException e)
    {
      if(e.getResultCode().equals(ResultCode.NO_SUCH_OBJECT))
      {
        return Collections.emptyList();
      }
      throw new EndpointException(e);
    }
  }



  /**
   * Creates a full destination "entry", corresponding to the the LDAP
   * {@link Entry} that is passed in. This method is responsible for
   * transforming the contents of the entry into the desired format and
   * transmitting it to the target destination. It should perform any inserts or
   * updates necessary to make sure the entry is fully created on the
   * destination endpoint.
   * <p>
   * This method <b>must be thread safe</b>, as it will be called repeatedly and
   * concurrently by the Sync Pipe worker threads as they process CREATE
   * operations.
   * @param entryToCreate
   *          the LDAP entry which corresponds to the destination
   *          "entry" to create
   * @param  operation
   *          the sync operation for this change
   * @throws EndpointException
   *           if there is an error creating the entry
   */
  @Override
  public void createEntry(final Entry entryToCreate,
                                       final SyncOperation operation)
                                           throws EndpointException
  {
    try
    {
      connectionPool.add(entryToCreate);
      operation.logInfo("Created entry: " + entryToCreate.getDN());
    }
    catch(LDAPException e)
    {
      throw new EndpointException(e);
    }
  }



  /**
   * Modify an "entry" on the destination, corresponding to the the LDAP
   * {@link Entry} that is passed in. This method is responsible for
   * transforming the contents of the entry into the desired format and
   * transmitting it to the target destination. It may perform multiple updates
   * (including inserting or deleting other attributes) in order to fully
   * synchronize the entire entry on the destination endpoint.
   * <p>
   * Note that the if the source entry was renamed (see
   * {@link SyncOperation#isModifyDN}), the
   * <code>fetchedDestEntry</code> will have the old DN; the new DN can
   * be obtained by calling
   * {@link SyncOperation#getDestinationEntryAfterChange()} and getting the DN
   * from there.
   * <p>
   * This method <b>must be thread safe</b>, as it will be called repeatedly and
   * concurrently by the Sync Pipe worker threads as they process MODIFY
   * operations.
   * @param entryToModify
   *          the LDAP entry which corresponds to the destination
   *          "entry" to modify. If the synchronization mode is 'standard',
   *          this will be the entry that was returned by {@link #fetchEntry};
   *          otherwise if the synchronization mode is 'notification', this
   *          will be the destination entry mapped from the source entry, before
   *          changes are applied.
   * @param modsToApply
   *          a list of Modification objects which should be applied; these will
   *          have any configured attribute mappings already applied
   * @param  operation
   *          the sync operation for this change
   * @throws EndpointException
   *           if there is an error modifying the entry
   */
  @Override
  public void modifyEntry(final Entry entryToModify,
                                       final List<Modification> modsToApply,
                                       final SyncOperation operation)
                                                 throws EndpointException
  {
    if(operation.isModifyDN())
    {
      Entry destEntryAfterChange = operation.getDestinationEntryAfterChange();
      try
      {
        String newRDN = destEntryAfterChange.getRDN().toString();
        String newSuperior = destEntryAfterChange.getParentDNString();
        boolean deleteOldRdn = operation.getChangeLogEntry()
                .getAttributeValueAsBoolean(ChangeLogEntry.ATTR_DELETE_OLD_RDN);

        connectionPool.modifyDN(entryToModify.getDN(),
                                newRDN, deleteOldRdn, newSuperior);
        operation.logInfo("Modified DN " + entryToModify.getDN() +
                          " to " + destEntryAfterChange.getDN());
      }
      catch(LDAPException e)
      {
        throw new EndpointException(e);
      }
    }
    else if(!modsToApply.isEmpty())
    {
      try
      {
        connectionPool.modify(entryToModify.getDN(), modsToApply);
        operation.logInfo("Modified entry: " + entryToModify.getDN());
      }
      catch(LDAPException e)
      {
        throw new EndpointException(e);
      }
    }
  }



  /**
   * Delete a full "entry" from the destination, corresponding to the the LDAP
   * {@link Entry} that is passed in. This method may perform multiple deletes
   * or updates if necessary to fully delete the entry from the destination
   * endpoint.
   * <p>
   * This method <b>must be thread safe</b>, as it will be called repeatedly and
   * concurrently by the Sync Pipe worker threads as they process DELETE
   * operations.
   * @param entryToDelete
   *          the LDAP entry which corresponds to the destination
   *          "entry" to delete. If the synchronization mode is 'standard',
   *          this will be the entry that was returned by {@link #fetchEntry};
   *          otherwise if the synchronization mode is 'notification', this
   *          will be the mapped destination entry.
   * @param  operation
   *          the sync operation for this change
   * @throws EndpointException
   *           if there is an error deleting the entry
   */
  @Override
  public void deleteEntry(final Entry entryToDelete,
                                       final SyncOperation operation)
                                            throws EndpointException
  {
    try
    {
      connectionPool.delete(entryToDelete.getDN());
      operation.logInfo("Deleted entry: " + entryToDelete.getDN());
    }
    catch(LDAPException e)
    {
      throw new EndpointException(e);
    }
  }
}
