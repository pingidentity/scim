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

package com.unboundid.directory.sdk.examples.groovy;

import java.sql.*;

import com.unboundid.ldap.sdk.*;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.ArgumentException;

import com.unboundid.directory.sdk.sync.types.TransactionContext;
import com.unboundid.directory.sdk.sync.types.SyncServerContext;
import com.unboundid.directory.sdk.sync.types.SyncOperation;
import com.unboundid.directory.sdk.sync.scripting.ScriptedJDBCSyncDestination;
import com.unboundid.directory.sdk.sync.config.JDBCSyncDestinationConfig;
import com.unboundid.directory.sdk.sync.util.ScriptUtils;


/**
 * This class implements the necessary methods to synchronize data to a simple,
 * single-table database schema from its LDAP counterpart.
 * <p>
 * To use this script, place it under
 *  /lib/groovy-scripted-extensions/com/unboundid/directory/sdk/examples/groovy
 * and set the 'script-class' property on the Sync Destination to
 *  "com.unboundid.directory.sdk.examples.groovy.ExampleJDBCSyncDestination".
 */
public final class ExampleScriptedJDBCSyncDestination extends ScriptedJDBCSyncDestination
{

  //The server context which can be used for obtaining the server state, logging, etc.
  private SyncServerContext serverContext;

  //The name of the destination data table.
  private static final String DATA_TABLE = "DataTable";


  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this extension.  The argument parser may also be
   * updated to define relationships between arguments (e.g. to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this extension.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // No arguments will be allowed by default.
  }


  /**
   * This hook is called when a Sync Pipe first starts up, or when the
   * <i>resync</i> process first starts up. Any initialization of this sync
   * destination should be performed here. This method should generally store
   * the {@link SyncServerContext} in a class
   * member so that it can be used elsewhere in the implementation.
   *
   * @param ctx
   *          a TransactionContext which provides a valid JDBC connection to the
   *          database.
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this sync destination.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this JDBC sync destination.
   */
  @Override
  public void initializeJDBCSyncDestination(final TransactionContext ctx,
                                            final SyncServerContext serverContext,
                                            final JDBCSyncDestinationConfig config,
                                            final ArgumentParser parser)
  {
    this.serverContext = serverContext;
  }


  /**
   * This hook is called when a Sync Pipe shuts down, or when the Resync process
   * shuts down. Any clean-up should be performed here.
   * @param ctx
   *          a TransactionContext which provides a valid JDBC connection to the
   *          database.
   */
  public void finalizeJDBCSyncDestination(final TransactionContext ctx)
  {
    // No cleanup required.
  }


  /**
   * Return a full destination entry (in LDAP form) from the database,
   * corresponding to the the source {@link Entry} that is passed in.
   * This method should perform any queries necessary to gather the latest
   * values for all the attributes to be synchronized and return them in an
   * Entry.
   * <p>
   * Note that the if the source entry was renamed (see
   * {@link SyncOperation#isModifyDN}), the <code>destEntryMappedFromSrc</code>
   * will have the new DN; the old DN can be obtained by calling
   * {@link SyncOperation#getDestinationEntryBeforeChange()} and getting the DN
   * from there. This method should return the entry in its existing form
   * (i.e. with the old DN, before it is changed).
   * <p>
   * This method <b>must be thread safe</b>, as it will be called repeatedly and
   * concurrently by each of the Sync Pipe worker threads as they process
   * entries.
   * @param ctx
   *          a TransactionContext which provides a valid JDBC connection to the
   *          database.
   * @param destEntryMappedFromSrc
   *          the LDAP entry which corresponds to the database "entry" to fetch
   * @param  operation
   *          the sync operation for this change
   * @return a full LDAP Entry, or null if no such entry exists.
   * @throws SQLException
   *           if there is an error fetching the entry
   */
  @Override
  public Entry fetchEntry(final TransactionContext ctx,
                          final Entry destEntryMappedFromSrc,
                          final SyncOperation operation)
                            throws SQLException
  {
    Attribute oc = destEntryMappedFromSrc.getObjectClassAttribute();
    Entry entry;
    if(ScriptUtils.containsAnyValue(oc, "iNetOrgPerson"))
    {
      long uid = Long.valueOf(destEntryMappedFromSrc.getAttributeValue("uid"));
      String sql = "SELECT * FROM " + DATA_TABLE + " WHERE uid = ?";
      PreparedStatement stmt = ctx.prepareStatement(sql);
      try
      {
        stmt.setLong(1, uid);
        entry = ctx.searchToRawEntry(stmt, "uid");
      }
      finally
      {
        stmt.close();
      }
      //add an extra attribute that is not found in the database
      ScriptUtils.addNumericAttribute(entry, "employeeNumber", uid);
    }
    else
    {
      throw new IllegalArgumentException("Unknown entry type: " + oc);
    }
    return entry;
  }


  /**
   * Creates a full database "entry", corresponding to the the LDAP
   * {@link Entry} that is passed in. This method should perform any inserts and
   * updates necessary to make sure the entry is fully created on the database.
   * <p>
   * This method <b>must be thread safe</b>, as it will be called repeatedly and
   * concurrently by the Sync Pipe worker threads as they process CREATE
   * operations.
   * @param ctx
   *          a TransactionContext which provides a valid JDBC connection to the
   *          database.
   * @param entryToCreate
   *          the LDAP entry which corresponds to the database "entry" to create
   * @param  operation
   *          the sync operation for this change
   * @throws SQLException
   *           if there is an error creating the entry
   */
  @Override
  public void createEntry(final TransactionContext ctx,
                          final Entry entryToCreate,
                          final SyncOperation operation)
                             throws SQLException
  {
    Attribute oc = entryToCreate.getObjectClassAttribute();
    if(ScriptUtils.containsAnyValue(oc, "iNetOrgPerson"))
    {
      long uid = Long.valueOf(entryToCreate.getAttributeValue("uid"));
      String cn = entryToCreate.getAttributeValue("cn");
      String givenName = entryToCreate.getAttributeValue("givenname");
      String sn = entryToCreate.getAttributeValue("sn");
      String description = entryToCreate.getAttributeValue("description"); //may be null

      PreparedStatement stmt = ctx.prepareStatement(
              "INSERT INTO " + DATA_TABLE + " (uid, objectclass, cn, givenname, sn, description)" +
                " VALUES (?,?,?,?,?,?)");

      stmt.setLong(1, uid);
      stmt.setString(2, "iNetOrgPerson");
      stmt.setString(3, cn);
      stmt.setString(4, givenName);
      stmt.setString(5, sn);
      if(description != null)
      {
        stmt.setString(6, description);
      }
      else
      {
        stmt.setNull(6, Types.NULL);
      }
      stmt.executeUpdate();
      stmt.close();
    }
    else
    {
      throw new IllegalArgumentException("Unknown entry type: " + oc);
    }
  }


  /**
   * Modify an "entry" in the database, corresponding to the the LDAP
   * {@link Entry} that is passed in. This method may perform multiple updates
   * (including inserting or deleting rows) in order to fully synchronize the
   * entire entry on the database.
   * <p>
   * Note that the if the source entry was renamed (see
   * {@link SyncOperation#isModifyDN}), the <code>fetchedDestEntry</code> will
   * have the old DN; the new DN can be obtained by calling
   * {@link SyncOperation#getDestinationEntryAfterChange()} and getting the DN
   * from there.
   * <p>
   * This method <b>must be thread safe</b>, as it will be called repeatedly and
   * concurrently by the Sync Pipe worker threads as they process MODIFY
   * operations.
   * @param ctx
   *          a TransactionContext which provides a valid JDBC connection to the
   *          database.
   * @param fetchedDestEntry
   *          the LDAP entry which corresponds to the database "entry" to modify
   * @param modsToApply
   *          a list of Modification objects which should be applied
   * @param  operation
   *          the sync operation for this change
   * @throws SQLException
   *           if there is an error modifying the entry
   */
  @Override
  public void modifyEntry(final TransactionContext ctx,
                          final Entry fetchedDestEntry,
                          final List<Modification> modsToApply,
                          final SyncOperation operation)
                             throws SQLException
  {
    Attribute oc = fetchedDestEntry.getObjectClassAttribute();
    if(ScriptUtils.containsAnyValue(oc, "iNetOrgPerson"))
    {
      long uid = Long.valueOf(fetchedDestEntry.getAttributeValue("uid"));
      Set<String> allAttrs = new HashSet(["objectclass", "cn", "givenname", "sn", "description"]);
      //Compute the set of columns to update
      StringBuilder attrsToUpdate = new StringBuilder();
      for(Modification m : modsToApply)
      {
        String attrName = m.getAttributeName().toLowerCase();
        if(!allAttrs.contains(attrName))
        {
          continue;
        }
        else if(m.getValues().length == 0)
        {
          attrsToUpdate.append(attrName).append(" = NULL,");
        }
        else
        {
          attrsToUpdate.append(attrName).append(" = ?,");
        }
      }
      //Remove trailing comma
      if(attrsToUpdate.length() > 0)
      {
        attrsToUpdate = attrsToUpdate.deleteCharAt(attrsToUpdate.length()-1);
      }
      else
      {
        return;
      }

      //For a single table, a single update statement is all we need
      String sql = "UPDATE " + DATA_TABLE + " SET " + attrsToUpdate.toString() + " WHERE uid = ?";
      PreparedStatement stmt = ctx.prepareStatement(sql);

      //Bind the values
      int i = 1;
      for(Modification m : modsToApply)
      {
        String attrName = m.getAttributeName().toLowerCase();
        if(!allAttrs.contains(attrName) || m.getValues().length == 0)
        {
          continue;
        }
        stmt.setString(i, m.getAttribute().getValue());
        i++;
      }
      stmt.setLong(i, uid);
      stmt.executeUpdate();
      stmt.close();
    }
    else
    {
      throw new IllegalArgumentException("Unknown entry type: " + oc);
    }
  }


  /**
   * Delete a full "entry" from the database, corresponding to the the LDAP
   * {@link Entry} that is passed in. This method may perform multiple deletes
   * or updates if necessary to fully delete the entry from the database.
   * <p>
   * This method <b>must be thread safe</b>, as it will be called repeatedly and
   * concurrently by the Sync Pipe worker threads as they process DELETE
   * operations.
   * @param ctx
   *          a TransactionContext which provides a valid JDBC connection to the
   *          database.
   * @param fetchedDestEntry
   *          the LDAP entry which corresponds to the database "entry" to delete
   * @param  operation
   *          the sync operation for this change
   * @throws SQLException
   *           if there is an error deleting the entry
   */
  @Override
  public void deleteEntry(final TransactionContext ctx,
                          final Entry fetchedDestEntry,
                          final SyncOperation operation)
                            throws SQLException
  {
    Attribute oc = fetchedDestEntry.getObjectClassAttribute();
    if(ScriptUtils.containsAnyValue(oc, "iNetOrgPerson"))
    {
      long uid = Long.valueOf(fetchedDestEntry.getAttributeValue("uid"));
      PreparedStatement stmt = ctx.prepareStatement("DELETE FROM " + DATA_TABLE + " WHERE uid = ?");
      stmt.setLong(1, uid);
      stmt.executeUpdate();
      stmt.close();
    }
    else
    {
      throw new IllegalArgumentException("Unknown entry type: " + oc);
    }
  }
}
