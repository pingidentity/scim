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



import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.unboundid.directory.sdk.common.operation.SearchRequest;
import com.unboundid.directory.sdk.common.operation.UpdatableAddRequest;
import com.unboundid.directory.sdk.common.operation.UpdatableAddResult;
import com.unboundid.directory.sdk.common.operation.UpdatableCompareRequest;
import com.unboundid.directory.sdk.common.operation.UpdatableCompareResult;
import com.unboundid.directory.sdk.common.operation.UpdatableModifyRequest;
import com.unboundid.directory.sdk.common.operation.UpdatableModifyResult;
import com.unboundid.directory.sdk.common.operation.UpdatableModifyDNRequest;
import com.unboundid.directory.sdk.common.operation.UpdatableModifyDNResult;
import com.unboundid.directory.sdk.common.operation.UpdatableSearchRequest;
import com.unboundid.directory.sdk.common.operation.UpdatableSearchResult;
import com.unboundid.directory.sdk.common.schema.AttributeType;
import com.unboundid.directory.sdk.common.schema.Schema;
import com.unboundid.directory.sdk.common.types.ActiveOperationContext;
import com.unboundid.directory.sdk.common.types.ActiveSearchOperationContext;
import com.unboundid.directory.sdk.common.types.UpdatableEntry;
import com.unboundid.directory.sdk.ds.api.Plugin;
import com.unboundid.directory.sdk.ds.config.PluginConfig;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.directory.sdk.ds.types.PreParsePluginResult;
import com.unboundid.directory.sdk.ds.types.SearchEntryPluginResult;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a plugin which will attempt to
 * prevent clients from interacting with a specified attribute.  Any add,
 * compare, modify, modify DN, or search request which references the specified
 * attribute will be rejected, and the attribute will be automatically removed
 * from any search result entries to be returned.  It has one configuration
 * argument:
 * <UL>
 *   <LI>attribute -- The name or OID of the attribute to attempt to prevent the
 *       user from accessing.</LI>
 * </UL>
 */
public final class ExamplePlugin
       extends Plugin
{
  /**
   * The name of the argument that will be used to specify the name of the
   * attribute to search for the provided identifier.
   */
  private static final String ARG_NAME_ATTR = "attribute";



  /**
   * A pre-parse plugin result that will be returned for requests that should be
   * rejected.
   */
  private static final PreParsePluginResult REJECT_REQUEST_RESULT =
       new PreParsePluginResult(false, // Connection terminated
            false,  // Continue pre-parse plugin processing
            true,   // Send response immediately
            true);  // Skip core processing



  // The attribute type definition for the target attribute.
  private volatile AttributeType attributeType;

  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;



  /**
   * Creates a new instance of this plugin.  All plugin implementations must
   * include a default constructor, but any initialization should generally be
   * done in the {@code initializePlugin} method.
   */
  public ExamplePlugin()
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
    return "Example Plugin";
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
      "This plugin serves an example that may be used to demonstrate the " +
           "process for creating a third-party plugin.  It will attempt to " +
           "prevent clients from interacting with a specified attribute by " +
           "rejecting requests which target that attribute, and by removing " +
           "it from search result entries to be returned.  Note that because " +
           "this plugin is primarily an example, it does not attempt to be " +
           "as thorough as might be necessary to absolutely prevent access " +
           "to the target attribute (e.g., it does not attempt to look " +
           "inside control or extended operation values)."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this plugin.  The argument parser may also be updated
   * to define relationships between arguments (e.g., to specify required,
   * exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this plugin.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the base DN for users.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_ATTR;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{attr}";
    String    description     = "The name or OID of the attribute type to " +
         "prevent clients from accessing.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));
  }



  /**
   * Initializes this plugin.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this plugin.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this plugin.
   *
   * @throws  LDAPException  If a problem occurs while initializing this plugin.
   */
  @Override()
  public void initializePlugin(final DirectoryServerContext serverContext,
                               final PluginConfig config,
                               final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning plugin initialization");

    this.serverContext = serverContext;

    // Get the attribute type to be excluded.
    attributeType = getAttributeType(serverContext, parser);
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this plugin.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(final PluginConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    // Make sure that the requested attribute type is defined in the schema.
    try
    {
      getAttributeType(config.getServerContext(), parser);
    }
    catch (final LDAPException le)
    {
      serverContext.debugCaught(le);
      unacceptableReasons.add(le.getMessage());
      acceptable = false;
    }

    return acceptable;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this plugin.
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
  public ResultCode applyConfiguration(final PluginConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    ResultCode rc = ResultCode.SUCCESS;

    // Get the attribute type to be excluded.
    try
    {
      attributeType = getAttributeType(config.getServerContext(), parser);
    }
    catch (final LDAPException le)
    {
      serverContext.debugCaught(le);
      messages.add(le.getMessage());
      rc = le.getResultCode();
    }

    return rc;
  }



  /**
   * Retrieves the definition for the attribute type to retrieve from the server
   * schema.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this plugin is running.
   * @param  parser         The argument parser with the configuration for this
   *                        plugin.
   *
   * @return  The configured attribute type definition.
   *
   * @throws  LDAPException  If the specified attribute type is not defined in
   *                         the server schema, or if a problem is encountered
   *                         while retrieving it.
   */
  private static AttributeType getAttributeType(
                      final DirectoryServerContext serverContext,
                      final ArgumentParser parser)
          throws LDAPException
  {
    final StringArgument arg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_ATTR);
    final String attrName = arg.getValue();

    final Schema schema = serverContext.getSchema();
    final AttributeType at = schema.getAttributeType(attrName, false);

    if (at == null)
    {
      throw new LDAPException(ResultCode.OTHER,
           "Attribute type " + attrName +
                " is not defined in the server schema.");
    }

    return at;
  }



  /**
   * Performs any cleanup which may be necessary when this plugin is to be taken
   * out of service.
   */
  @Override()
  public void finalizePlugin()
  {
    // No finalization is required.
  }



  /**
   * Performs any processing which may be necessary before the server starts
   * processing for an add request.
   *
   * @param  operationContext  The context for the add operation.
   * @param  request           The add request to be processed.  It may be
   *                           altered if desired.
   * @param  result            The result that will be returned to the client if
   *                           the plugin result indicates that processing on
   *                           the operation should be interrupted.  It may be
   *                           altered if desired.
   *
   * @return  Information about the result of the plugin processing.
   */
  @Override()
  public PreParsePluginResult doPreParse(
              final ActiveOperationContext operationContext,
              final UpdatableAddRequest request,
              final UpdatableAddResult result)
  {
    // Examine the add request and see if the entry includes the target
    // attribute type.  If so, then reject the request.
    final UpdatableEntry addEntry = request.getEntry();
    if (addEntry.hasAttribute(attributeType))
    {
      result.setResultCode(ResultCode.UNWILLING_TO_PERFORM);
      result.setDiagnosticMessage(
           "Unwilling to allow the client to add an entry containing the " +
                attributeType.getNameOrOID() + " attribute type.");
      return REJECT_REQUEST_RESULT;
    }
    else
    {
      return PreParsePluginResult.SUCCESS;
    }
  }



  /**
   * Performs any processing which may be necessary before the server starts
   * processing for a compare request.
   *
   * @param  operationContext  The context for the compare operation.
   * @param  request           The compare request to be processed.  It may be
   *                           altered if desired.
   * @param  result            The result that will be returned to the client if
   *                           the plugin result indicates that processing on
   *                           the operation should be interrupted.  It may be
   *                           altered if desired.
   *
   * @return  Information about the result of the plugin processing.
   */
  @Override()
  public PreParsePluginResult doPreParse(
              final ActiveOperationContext operationContext,
              final UpdatableCompareRequest request,
              final UpdatableCompareResult result)
  {
    // Examine the compare request and see if uses the target attribute type.
    // If so, then reject the request.
    final String targetType = request.getAttributeType();
    if (attributeType.hasNameOrOID(targetType))
    {
      result.setResultCode(ResultCode.UNWILLING_TO_PERFORM);
      result.setDiagnosticMessage("Unwilling to allow the client to perform " +
           "a compare operation targeting the '" + targetType +
           " attribute type.");
      return REJECT_REQUEST_RESULT;
    }
    else
    {
      return PreParsePluginResult.SUCCESS;
    }
  }



  /**
   * Performs any processing which may be necessary before the server starts
   * processing for a modify request.
   *
   * @param  operationContext  The context for the modify operation.
   * @param  request           The modify request to be processed.  It may be
   *                           altered if desired.
   * @param  result            The result that will be returned to the client if
   *                           the plugin result indicates that processing on
   *                           the operation should be interrupted.  It may be
   *                           altered if desired.
   *
   * @return  Information about the result of the plugin processing.
   */
  @Override()
  public PreParsePluginResult doPreParse(
              final ActiveOperationContext operationContext,
              final UpdatableModifyRequest request,
              final UpdatableModifyResult result)
  {
    // Look at the modifications and see if any of them references the target
    // attribute type.  If so, then reject the request.
    final List<Modification> mods = request.getModifications();
    for (final Modification m : mods)
    {
      final String attrName = m.getAttributeName();
      if (attributeType.hasNameOrOID(attrName))
      {
        result.setResultCode(ResultCode.UNWILLING_TO_PERFORM);
        result.setDiagnosticMessage("Unwilling to allow the client to " +
             "perform a modify operation targeting the '" + attrName +
             " attribute type.");
        return REJECT_REQUEST_RESULT;
      }
    }

    return PreParsePluginResult.SUCCESS;
  }



  /**
   * Performs any processing which may be necessary before the server starts
   * processing for a modify DN request.
   *
   * @param  operationContext  The context for the modify DN operation.
   * @param  request           The modify DN request to be processed.  It may be
   *                           altered if desired.
   * @param  result            The result that will be returned to the client if
   *                           the plugin result indicates that processing on
   *                           the operation should be interrupted.  It may be
   *                           altered if desired.
   *
   * @return  Information about the result of the plugin processing.
   */
  @Override()
  public PreParsePluginResult doPreParse(
              final ActiveOperationContext operationContext,
              final UpdatableModifyDNRequest request,
              final UpdatableModifyDNResult result)
  {
    // Look at the request and ensure that the newRDN does not reference the
    // target attribute.
    final String newRDN = request.getNewRDN();
    final RDN parsedNewRDN;
    try
    {
      parsedNewRDN = new RDN(newRDN);
    }
    catch (final LDAPException le)
    {
      serverContext.debugCaught(le);
      result.setResultCode(ResultCode.INVALID_DN_SYNTAX);
      result.setDiagnosticMessage("Unable to parse the provided new RDN:  " +
           le.getExceptionMessage());
      return REJECT_REQUEST_RESULT;
    }

    for (final String s : parsedNewRDN.getAttributeNames())
    {
      if (attributeType.hasNameOrOID(s))
      {
        result.setResultCode(ResultCode.UNWILLING_TO_PERFORM);
        result.setDiagnosticMessage("Unwilling to allow the client to " +
             "perform a modify DN operation in which the new RDN references " +
             "the " + s + " attribute type.");
        return REJECT_REQUEST_RESULT;
      }
    }

    return PreParsePluginResult.SUCCESS;
  }



  /**
   * Performs any processing which may be necessary before the server starts
   * processing for a search request.
   *
   * @param  operationContext  The context for the search operation.
   * @param  request           The search request to be processed.  It may be
   *                           altered if desired.
   * @param  result            The result that will be returned to the client if
   *                           the plugin result indicates that processing on
   *                           the operation should be interrupted.  It may be
   *                           altered if desired.
   *
   * @return  Information about the result of the plugin processing.
   */
  @Override()
  public PreParsePluginResult doPreParse(
              final ActiveSearchOperationContext operationContext,
              final UpdatableSearchRequest request,
              final UpdatableSearchResult result)
  {
    // Look at the filter and ensure that it does not reference the target
    // attribute type.
    if (filterContainsTargetAttribute(request.getFilter()))
    {
      result.setResultCode(ResultCode.UNWILLING_TO_PERFORM);
      result.setDiagnosticMessage("Unwilling to allow the client to " +
           "perform a search which references attribute " +
           attributeType.getNameOrOID() + " in the filter.");
      return REJECT_REQUEST_RESULT;
    }
    else
    {
      return PreParsePluginResult.SUCCESS;
    }
  }



  /**
   * Indicates whether the provided search filter references the target
   * attribute type.
   *
   * @param  f  The filter to examine.
   *
   * @return  {@code true} if the provided filter references the target
   *          attribute type, or {@code false} if not.
   */
  private boolean filterContainsTargetAttribute(final Filter f)
  {
    switch (f.getFilterType())
    {
      case Filter.FILTER_TYPE_AND:
      case Filter.FILTER_TYPE_OR:
        for (final Filter comp : f.getComponents())
        {
          if (filterContainsTargetAttribute(comp))
          {
            return true;
          }
        }
        return false;

      case Filter.FILTER_TYPE_NOT:
        return filterContainsTargetAttribute(f.getNOTComponent());

      default:
        final String attrName = f.getAttributeName();
        return ((attrName != null) && attributeType.hasNameOrOID(attrName));
    }
  }



  /**
   * Performs any processing which may be necessary before the server sends a
   * search result entry to the client.
   *
   * @param  operationContext  The context for the search operation.
   * @param  request           The search request being processed.
   * @param  result            The result that will be returned to the client if
   *                           the plugin result indicates that processing on
   *                           the operation should be interrupted.  It may be
   *                           altered if desired.
   * @param  entry             The entry to be returned to the client.  It may
   *                           be altered if desired.
   * @param  controls          The set of controls to be included with the
   *                           entry.  It may be altered if desired.
   *
   * @return  Information about the result of the plugin processing.
   */
  @Override()
  public SearchEntryPluginResult doSearchEntry(
              final ActiveSearchOperationContext operationContext,
              final SearchRequest request, final UpdatableSearchResult result,
              final UpdatableEntry entry, final List<Control> controls)
  {
    // Remove the target attribute from the entry to return.
    entry.removeAttribute(attributeType);

    return SearchEntryPluginResult.SUCCESS;
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
         Arrays.asList(
              ARG_NAME_ATTR + "=description"),
         "Prevent the 'description' attribute from being targeted by " +
              "client operations or returned in search result entries.");

    return exampleMap;
  }
}
