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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import com.unboundid.directory.sdk.sync.scripting.ScriptedLDAPSyncDestinationPlugin;
import com.unboundid.directory.sdk.sync.config.LDAPSyncDestinationPluginConfig;
import com.unboundid.directory.sdk.sync.types.PostStepResult;
import com.unboundid.directory.sdk.sync.types.PreStepResult;
import com.unboundid.directory.sdk.sync.types.SyncOperation;
import com.unboundid.directory.sdk.sync.types.SyncServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.DNArgument;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a scripted LDAP sync destination
 * plugin which manipulates an attribute that stores a reference to another
 * entry.  In the LDAP destination server, the reference is stored as a DN, but
 * the source instance (possible a relational database) stores the reference
 * using a key for the referenced entry (e.g. uid).
 * <UL>
 *   <LI>{@code postFetch} -- After an entry is returned from the source, the
 *       key value in the referenced attribute must be retrieved by doing
 *       a search for the referenced entry.
 *   </LI>
 *   <LI>{@code preCreate} -- When an entry is created at the destination,
 *       the key attribute values in the reference attribute must be replaced
 *       with DNs (i.e. the reverse of what happens in {@code postFetch} by
 *       retrieving the referenced entries from the destination server using
 *       the key attribute value.
 *   </LI>
 *   <LI>{@code preModify} -- If the reference attribute is modified,
 *       the key attribute values in the reference attribute must be replaced
 *       with DNs (i.e. the reverse of what happens in {@code postFetch} by
 *       retrieving the referenced entries from the destination server using
 *       the key attribute value.
 *   </LI>
 * </UL>
 * It takes the following arguments:
 * <UL>
 *   <LI>reference-attribute -- The destination attribute that stores the
 *                              DN of the referenced entry.</LI>
 *   <LI>referenced-entry-key-attribute -- The key attribute on the referenced
 *                              entry that is used to find the entry, e.g.
 *                              uid.</LI>
 *   <LI>search-base-dn -- The base DN to search for referenced entries using
 *                         the key.</LI>
 * </UL>
 */
public class ExampleScriptedLDAPSyncDestinationPlugin
     extends ScriptedLDAPSyncDestinationPlugin
{

  private static final String ARG_NAME_REFERENCE_ATTRIBUTE =
       "reference-attribute";
  private static final String ARG_NAME_REFERENCED_ENTRY_KEY_ATTRIBUTE =
       "referenced-entry-key-attribute";
  private static final String ARG_NAME_SEARCH_BASE_DN = "search-base-dn";

  // The server context for the server in which this extension is running.
  private SyncServerContext serverContext;

  // This lock ensures that the configuration is updated atomically and safely.
  private final ReadWriteLock configLock = new ReentrantReadWriteLock();
  private final Lock configReadLock = configLock.readLock();
  private final Lock configWriteLock = configLock.writeLock();

  private String referenceAttribute;

  private String referencedEntryKeyAttribute;

  private DN searchBaseDn;



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this sync pipe plugin.  The argument parser may
   * also be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this sync pipe plugin.
   *
   * @throws ArgumentException  If a problem is encountered while updating the
   *                            provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the source attribute.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_REFERENCE_ATTRIBUTE;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{attr}";
    String    description     = "The name of the destination attribute that " +
         "stores a reference to another entry as a DN.";

    StringArgument arg = new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description);
    arg.setValueRegex(Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\\\-]*\$"),
                      "A valid attribute name is required.");
    parser.addArgument(arg);

    shortIdentifier = null;
    longIdentifier  = ARG_NAME_REFERENCED_ENTRY_KEY_ATTRIBUTE;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{attr}";
    description     = "The name of the key attribute on the destination " +
         "entry that is referenced by the entry being synchronized.  For " +
         "instance, this could be 'uid' if the referring entry stored the " +
         "value of the uid attribute in the referenced entry.";

    arg = new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description);
    arg.setValueRegex(Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\\\-]*\$"),
                      "A valid attribute name is required.");
    parser.addArgument(arg);

    shortIdentifier = null;
    longIdentifier  = ARG_NAME_SEARCH_BASE_DN;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{base-dn}";
    description     = "The base DN to use when searching for referenced " +
         "entries.  A subtree search will be issued with a base of the DN " +
         "specified here to find the referenced entries.";

    parser.addArgument(new DNArgument(shortIdentifier, longIdentifier, required,
         maxOccurrences, placeholder, description));
  }



  /**
   * Initializes this LDAP sync destination plugin.  This method will be called
   * before any other methods in the class.
   *
   * @param  serverContext  A handle to the server context for the
   *                        Synchronization Server in which this extension is
   *                        running.  Extensions should typically store this
   *                        in a class member.
   * @param  config         The general configuration for this proxy
   *                        transformation.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this LDAP sync destination
   *                        plugin.
   *
   * @throws  LDAPException  If a problem occurs while initializing this ldap
   *                         sync destination plugin.
   */
  @Override
  public void initializeLDAPSyncDestinationPlugin(
       final SyncServerContext serverContext,
       final LDAPSyncDestinationPluginConfig config,
       final ArgumentParser parser)
       throws LDAPException
  {
    this.serverContext = serverContext;
    setConfig(config, parser);
  }



  /**
    * Indicates whether the configuration contained in the provided argument
    * parser represents a valid configuration for this extension.
    *
    * @param  config               The general configuration for this LDAP sync
    *                              destination plugin.
    * @param  parser               The argument parser which has been
    *                              initialized with the proposed configuration.
    * @param  unacceptableReasons  A list that can be updated with reasons that
    *                              the proposed configuration is not acceptable.
    *
    * @return  {@code true} if the proposed configuration is acceptable, or
    *          {@code false} if not.
    */
  @Override
  public boolean isConfigurationAcceptable(
       final LDAPSyncDestinationPluginConfig config,
       final ArgumentParser parser,
       final List<String> unacceptableReasons)
  {
    // The built-in ArgumentParser validation does all of the validation that
    // we need.
    return true;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this LDAP sync
   *                               destination.
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
  public ResultCode applyConfiguration(
       final LDAPSyncDestinationPluginConfig config,
       final ArgumentParser parser,
       final List<String> adminActionsRequired,
       final List<String> messages)
  {
    setConfig(config, parser);
    return ResultCode.SUCCESS;
  }



  /**
   * Sets the configuration for this plugin.  This is a centralized place
   * where the configuration is initialized or updated.
   *
   * @param  config         The general configuration for this LDAP sync
   *                        destination plugin.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this LDAP sync destination
   *                        plugin.
   */
  private void setConfig(
                   final LDAPSyncDestinationPluginConfig config,
                   final ArgumentParser parser)
  {
    configWriteLock.lock();
    try
    {
      this.referenceAttribute =((StringArgument)parser.getNamedArgument(
            ARG_NAME_REFERENCE_ATTRIBUTE)).getValue();
      this.referencedEntryKeyAttribute =
           ((StringArgument)parser.getNamedArgument(
             ARG_NAME_REFERENCED_ENTRY_KEY_ATTRIBUTE)).getValue();
      this.searchBaseDn = ((DNArgument)parser.getNamedArgument(
            ARG_NAME_SEARCH_BASE_DN)).getValue();
    }
    finally
    {
      configWriteLock.unlock();
    }
  }



  /**
   * This method is called after an attempt to fetch a destination entry.  An
   * connection to the destination server is provided along with the
   * {@code SearchRequest} that was sent to the server.  This method is
   * overridden by plugins that need to manipulate the search results that
   * are returned to the Sync Pipe.  This can include filtering out certain
   * entries, remove information from the entries, or adding additional
   * information, possibly by doing a followup LDAP search.
   * <p>
   * This method might be called multiple times for a single synchronization
   * operation, specifically when there are multiple search criteria or
   * multiple base DNs defined for the Sync Destination.
   * <p>
   * This method will not be called if the search fails, for instance, if
   * the base DN of the search does not exist.
   *
   * @param  destinationConnection  A connection to the destination server.
   * @param  searchRequest          The search request that the LDAP Sync
   *                                Destination used to fetch the entry.
   * @param  fetchedEntries         A list of entries that have been fetched.
   *                                When the search criteria matches multiple
   *                                entries, they will all be returned.  Entries
   *                                in this list can be edited directly, and the
   *                                list can be edited as well.
   * @param  operation              The synchronization operation for this
   *                                change.
   *
   * @return  The result of the plugin processing.
   *
   * @throws  LDAPException  In general subclasses should not catch
   *                         LDAPExceptions that are thrown when
   *                         using the LDAPInterface unless there
   *                         are specific exceptions that are
   *                         expected.  The Synchronization Server
   *                         will handle LDAPExceptions in an
   *                         appropriate way based on the specific
   *                         cause of the exception.  For example,
   *                         some errors will result in the
   *                         SyncOperation being retried, and others
   *                         will trigger fail over to a different
   *                         server.  Plugins should only throw
   *                         LDAPException for errors related to
   *                         communication with the LDAP server.
   *                         Use the return code to indicate other
   *                         types of errors, which might require
   *                         retry.
   */
  @Override
  public PostStepResult postFetch(final LDAPInterface destinationConnection,
                                  final SearchRequest searchRequest,
                                  final List<Entry> fetchedEntries,
                                  final SyncOperation operation)
       throws LDAPException
  {
    configReadLock.lock();

    try
    {
      for (Entry entry: fetchedEntries)
      {
        String[] referenceDns = entry.getAttributeValues(referenceAttribute);
        if (referenceDns == null)
        {
          serverContext.debugInfo("Entry " + entry.getDN() + " does not have " +
              "any values for the " + referencedEntryKeyAttribute +
              "attribute.");
          continue;
        }

        List<String> keyValues = new ArrayList<String>(referenceDns.length);
        for (int i = 0; i < referenceDns.length; i++)
        {
          String referenceDn = referenceDns[i];
          Entry referencedEntry = destinationConnection.getEntry(referenceDn,
             referencedEntryKeyAttribute);
          if (referencedEntry != null)
          {
            String keyValue =
                 referencedEntry.getAttributeValue(referencedEntryKeyAttribute);
            if (keyValue != null)
            {
              keyValues.add(keyValue);
            }
            else
            {
              operation.logError("For fetched entry '" + entry.getDN() + "', " +
                  "the example plugin could not find a corresponding " +
                  referencedEntryKeyAttribute + " value for referenced entry " +
                  referenceDn + " but the referenced entry does exist.");
            }
          }
          else
          {
            operation.logError("For fetched entry '" + entry.getDN() + "', " +
                "the example plugin could not find the entry, " +
                referenceDn + ", which is referenced by " + referenceAttribute +
                ".");
          }
        }

        if (keyValues.isEmpty())
        {
          operation.logInfo("Example plugin removed the " + referenceAttribute +
            " attribute because no entries could be found to match the list " +
            "DNs: " + Arrays.asList(referenceDns));
          entry.removeAttribute(referenceAttribute);
        }
        else
        {
          operation.logInfo("Example plugin replacing DN(s)=" +
              Arrays.asList(referenceDns) + " in attribute " +
              referenceAttribute + " with " + referencedEntryKeyAttribute +
              " value(s)=" + keyValues);
          entry.setAttribute(new Attribute(referenceAttribute, keyValues));
        }
      }

      return PostStepResult.CONTINUE;
    }
    finally
    {
      configReadLock.unlock();
    }
  }



  /**
   * This method is called before a destination entry is created.  A
   * connection to the destination server is provided along with the
   * {@code Entry} that will be sent to the server.  This method is
   * overridden by plugins that need to alter the entry before it is created
   * at the server.
   *
   * @param  destinationConnection  A connection to the destination server.
   * @param  entryToCreate          The entry that will be created at the
   *                                destination.  A plugin that wishes to
   *                                create the entry should be sure to return
   *                                {@code PreStepResult#SKIP_CURRENT_STEP}.
   * @param  operation              The synchronization operation for this
   *                                change.
   *
   * @return  The result of the plugin processing.
   *
   * @throws  LDAPException  In general subclasses should not catch
   *                         LDAPExceptions that are thrown when
   *                         using the LDAPInterface unless there
   *                         are specific exceptions that are
   *                         expected.  The Synchronization Server
   *                         will handle LDAPExceptions in an
   *                         appropriate way based on the specific
   *                         cause of the exception.  For example,
   *                         some errors will result in the
   *                         SyncOperation being retried, and others
   *                         will trigger fail over to a different
   *                         server.  Plugins should only throw
   *                         LDAPException for errors related to
   *                         communication with the LDAP server.
   *                         Use the return code to indicate other
   *                         types of errors, which might require
   *                         retry.
   */
  @Override
  public PreStepResult preCreate(final LDAPInterface destinationConnection,
                                 final Entry entryToCreate,
                                 final SyncOperation operation)
       throws LDAPException
  {
    try
    {
      configReadLock.lock();

      String[] referenceValues =
           entryToCreate.getAttributeValues(referenceAttribute);
      if (referenceValues == null)
      {
        return PreStepResult.CONTINUE;
      }

      List<String> dnValues = new ArrayList<String>(referenceValues.length);
      for (int i = 0; i < referenceValues.length; i++)
      {
        String referenceValue = referenceValues[i];

        String dnValue = lookupDnByKeyValue(destinationConnection,
             entryToCreate, operation, referenceValue);
        if (dnValue != null)
        {
          dnValues.add(dnValue);
        }
      }

      if (dnValues.isEmpty())
      {
        operation.logInfo("Example plugin removed the " + referenceAttribute +
            " attribute because no entries could be found to match the list " +
            "of values: " + Arrays.asList(referenceValues));
        entryToCreate.removeAttribute(referenceAttribute);
      }
      else
      {
        operation.logInfo("Example plugin replacing " +
            referencedEntryKeyAttribute + " reference values in the " +
            referenceAttribute + " attribute: " +
            Arrays.asList(referenceValues) +
            " with DN value(s)=" + dnValues);
        entryToCreate.setAttribute(new Attribute(referenceAttribute, dnValues));
      }

      return PreStepResult.CONTINUE;
    }
    finally
    {
      configReadLock.unlock();
    }
  }



  /**
   * This method is called before a destination entry is modified.  A
   * connection to the destination server is provided along with the
   * {@code Entry} that will be sent to the server.  This method is
   * overridden by plugins that need to perform some processing on an entry
   * before it is modified.
   *
   * @param  destinationConnection  A connection to the destination server.
   * @param  entryToModify          The entry that will be modified at the
   *                                destination.  A plugin that wishes to
   *                                modify the entry should be sure to return
   *                                {@code PreStepResult#SKIP_CURRENT_STEP}.
   * @param  modsToApply            A modifiable list of the modifications to
   *                                apply at the server.
   * @param  operation              The synchronization operation for this
   *                                change.
   *
   * @return  The result of the plugin processing.
   *
   * @throws  LDAPException  In general subclasses should not catch
   *                         LDAPExceptions that are thrown when
   *                         using the LDAPInterface unless there
   *                         are specific exceptions that are
   *                         expected.  The Synchronization Server
   *                         will handle LDAPExceptions in an
   *                         appropriate way based on the specific
   *                         cause of the exception.  For example,
   *                         some errors will result in the
   *                         SyncOperation being retried, and others
   *                         will trigger fail over to a different
   *                         server.  Plugins should only throw
   *                         LDAPException for errors related to
   *                         communication with the LDAP server.
   *                         Use the return code to indicate other
   *                         types of errors, which might require
   *                         retry.
   */
  @Override
  public PreStepResult preModify(final LDAPInterface destinationConnection,
                                 final Entry entryToModify,
                                 final List<Modification> modsToApply,
                                 final SyncOperation operation)
       throws LDAPException
  {
    try
    {
      configReadLock.lock();

      List<Modification> updatedModsToApply =
           new ArrayList<Modification>(modsToApply.size());

      for (Modification modification : modsToApply)
      {
        // Let any modification to non-referenceAttribute attributes pass
        // straight through.
        if (!referenceAttribute.equalsIgnoreCase(
        modification.getAttributeName()))
        {
          updatedModsToApply.add(modification);
          continue;
        }

        // For the referenceAttribute attribute, convert keys to DNs.
        String[] keyValues = modification.getValues();
        List<String> dnValues = new ArrayList<String>(keyValues.length);
        for (String keyValue : keyValues)
        {
          String dnValue = lookupDnByKeyValue(destinationConnection,
                                              entryToModify,
                                              operation,
                                              keyValue);
          if (dnValue != null)
          {
            dnValues.add(dnValue);
          }
        }

        // If we started off with no values, or ended up with at least one
        // DN, then let this mod pass through.
        if ((keyValues.length == 0) || !dnValues.isEmpty())
        {
          String[] dnValuesArr = dnValues.toArray(new String[0]);
          Modification updatedMod =
               new Modification(modification.getModificationType(),
                                modification.getAttributeName(), dnValuesArr);
          updatedModsToApply.add(updatedMod);
        }
      }

      // We can't edit the mods in place, so we just replace them with the
      // list that we built up.
      modsToApply.clear();
      modsToApply.addAll(updatedModsToApply);

      if (modsToApply.isEmpty())
      {
        // If there are no mods, then skip the current step.
        return PreStepResult.SKIP_CURRENT_STEP;
      }
      else
      {
        return PreStepResult.CONTINUE;
      }
    }
    finally
    {
      configReadLock.unlock();
    }
  }



  /**
   * Returns the DN of the entry with a {@code referencedEntryKeyAttribute}
   * value of {@code referenceValue}.
   *
   * @param destinationConnection  A connection to the destination server.
   * @param entry                  The referencing entry.
   * @param operation              The synchronization operation.
   * @param referenceValue         The value of the reference attribute.
   *
   * @return  The DN of the referenced entry or {@code null} if there is not
   *          exactly one referenced entry.
   *
   * @throws  LDAPException  If there is a problem retrieving the remote entry.
   */
  private String lookupDnByKeyValue(final LDAPInterface destinationConnection,
                                    final Entry entry,
                                    final SyncOperation operation,
                                    final String referenceValue)
       throws LDAPException
  {
    Filter filter = Filter.createEqualityFilter(referencedEntryKeyAttribute,
                                                referenceValue);

    SearchResult searchResult = destinationConnection.search(
         searchBaseDn.toString(),
         SearchScope.SUB,
         filter,
         "1.1"); // Don't return any attributes.

    List<SearchResultEntry> entries = searchResult.getSearchEntries();
    if (entries.isEmpty())
    {
      operation.logError("For entry '" +
          entry.getDN() + "', " +
          "the example plugin could not find any entry that matched " +
          filter.toString() + ".  This value of the " + referenceAttribute +
          " attribute will not be included in the entry.");
      return null;
    }
    else if (entries.size() > 1)
    {
      operation.logError("For entry '" +
          entry.getDN() + "', " +
          "the example plugin found " + entries.size() + " entries that " +
          "matched " + filter.toString() + " instead of a single one.  " +
          "This value of the " + referenceAttribute +
          " attribute will not be included in the entry.");
      return null;
    }
    else
    {
      String dnValue = entries.get(0).getDN();
      if (serverContext.debugEnabled())
      {
        serverContext.debugVerbose("When creating entry " +
            entry.getDN() + " mapped " + referenceValue + " to " +
            dnValue + " for the " + referenceAttribute + " attribute.");
      }
      return dnValue;
    }
  }
}
