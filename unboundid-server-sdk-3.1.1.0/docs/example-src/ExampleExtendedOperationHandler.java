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



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.unboundid.asn1.ASN1Element;
import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.asn1.ASN1Sequence;
import com.unboundid.asn1.ASN1Set;
import com.unboundid.directory.sdk.common.operation.ExtendedRequest;
import com.unboundid.directory.sdk.common.operation.UpdatableExtendedResult;
import com.unboundid.directory.sdk.common.types.InternalConnection;
import com.unboundid.directory.sdk.common.types.OperationContext;
import com.unboundid.directory.sdk.ds.api.ExtendedOperationHandler;
import com.unboundid.directory.sdk.ds.config.ExtendedOperationHandlerConfig;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of an extended operation handler which
 * may be used to retrieve a specified entry from the directory.  The extended
 * request must have a value, which will be encoded as follows:
 * <PRE>
 *   GetEntryRequest ::= SEQUENCE {
 *        dn             OCTET STRING
 *        attributes     SEQUENCE OF OCTET STRING OPTIONAL }
 * </PRE>
 * That is, the request value must include the DN of the entry to request, and
 * may also include a list of the attributes to return from that entry.  If the
 * entry exists, then the extended result will have a value with the following
 * encoding:
 * <PRE>
 *   GetEntryResult ::= SEQUENCE OF SEQUENCE {
 *        attributeDescription     OCTET STRING
 *        values                   SET OF OCTET STRING }
 * </PRE>
 * That is, the value will be a sequence of the attributes contained in that
 * entry.
 * <BR><BR>
 * This extended operation handler takes one configuration argument:
 * <UL>
 *   <LI>request-oid -- The OID that clients can use to request this extended
 *       operation.</LI>
 * </UL>
 */
public final class ExampleExtendedOperationHandler
       extends ExtendedOperationHandler
{
  /**
   * The name of the argument that will be used to specify the request OID for
   * the extended request.
   */
  private static final String ARG_NAME_REQUEST_OID = "request-oid";



  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;

  // The request OID for the extended request.
  private String requestOID;



  /**
   * Creates a new instance of this extended operation handler.  All extended
   * operation handler implementations must include a default constructor, but
   * any initialization should generally be done in the
   * {@code initializeExtendedOperationHandler} method.
   */
  public ExampleExtendedOperationHandler()
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
    return "Example Extended Operation Handler";
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
      "This extended operation handler serves as an example that may be used " +
           "to demonstrate the process for creating a third-party " +
           "extended operation handler.  It may be used to retrieve all or " +
           "a select set of attributes from a specified entry in the server."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this extended operation handler.  The argument parser
   * may also be updated to define relationships between arguments (e.g., to
   * specify required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this extended operation
   *                 handler.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the request OID.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_REQUEST_OID;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{oid}";
    String    description     = "The OID that clients should use to request " +
         "this extended operation.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));
  }



  /**
   * Initializes this extended operation handler.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this extended
   *                        operation handler.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this extended operation
   *                        handler.
   *
   * @throws  LDAPException  If a problem occurs while initializing this
   *                         extended operation handler.
   */
  @Override()
  public void initializeExtendedOperationHandler(
                   final DirectoryServerContext serverContext,
                   final ExtendedOperationHandlerConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo(
         "Beginning extended operation handler initialization");

    this.serverContext = serverContext;

    // Get the request OID that should be used for this extended request.
    final StringArgument arg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_REQUEST_OID);
    requestOID = arg.getValue();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this extended
   *                              operation handler.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(
                      final ExtendedOperationHandlerConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    // Make sure that the request OID is a valid numeric OID.
    final StringArgument arg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_REQUEST_OID);
    final String oid = arg.getValue();


    if (! StaticUtils.isNumericOID(oid))
    {
      unacceptableReasons.add("The provided value '" + oid +
           "' is not a valid numeric OID.");
      acceptable = false;
    }

    return acceptable;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this extended
   *                               operation handler.
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
                         final ExtendedOperationHandlerConfig config,
                         final ArgumentParser parser,
                         final List<String> adminActionsRequired,
                         final List<String> messages)
  {
    // The only option that we support is the numeric OID.  However, that can't
    // be changed on the fly.  See if a new OID was configured, and if so then
    // indicate that admin action is required.
    final StringArgument arg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_REQUEST_OID);
    final String newOID = arg.getValue();
    if (! requestOID.equals(newOID))
    {
      adminActionsRequired.add("The extended operation handler must be " +
           "disabled and re-enabled for changes to the request OID to take " +
           "effect.");
    }

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this extended operation
   * handler is to be taken out of service.
   */
  @Override()
  public void finalizeExtendedOperationHandler()
  {
    // No finalization is required.
  }



  /**
   * Retrieves the name of the extended operation with the provided OID.
   *
   * @param  oid  The OID of the extended operation for which to retrieve the
   *              corresponding name.
   *
   * @return  The name of the extended operation with the specified OID, or
   *          {@code null} if the specified OID is not recognized by this
   *          extended operation handler.
   */
  @Override()
  public String getExtendedOperationName(final String oid)
  {
    if (oid.equals(requestOID))
    {
      return "Example Get Entry Extended Operation";
    }
    else
    {
      return null;
    }
  }



  /**
   * Retrieves the OIDs of the extended operation types supported by this
   * extended operation handler.
   *
   * @return  The OIDs of the extended operation types supported by this
   *          extended operation handler.  It must not be {@code null} or
   *          empty, and the contents of the set returned must not change over
   *          the life of this extended operation handler.
   */
  @Override()
  public Set<String> getSupportedExtensions()
  {
    final HashSet<String> supportedExtensions = new HashSet<String>(1);
    supportedExtensions.add(requestOID);
    return supportedExtensions;
  }



  /**
   * Retrieves the OIDs of any controls supported by this extended operation
   * handler.
   *
   * @return  The OIDs of any controls supported by this extended operation
   *          handler.  It may be {@code null} or empty if this extended
   *          operation handler does not support any controls.
   */
  @Override()
  public Set<String> getSupportedControls()
  {
    return Collections.emptySet();
  }



  /**
   * Retrieves the OIDs of any features supported by this extended operation
   * handler that should be advertised in the server root DSE.
   *
   * @return  The OIDs of any features supported by this extended operation
   *          handler.  It may be {@code null} or empty if this extended
   *          operation handler does not support any controls.
   */
  @Override()
  public Set<String> getSupportedFeatures()
  {
    return Collections.emptySet();
  }



  /**
   * Performs any processing appropriate for the provided extended request.
   *
   * @param  operationContext  The operation context for the extended operation.
   * @param  request           The extended request to be processed.
   * @param  result            The extended result to be updated with the result
   *                           of processing.
   */
  @Override()
  public void processExtendedOperation(final OperationContext operationContext,
                                       final ExtendedRequest request,
                                       final UpdatableExtendedResult result)
  {
    final boolean debugEnabled = serverContext.debugEnabled();
    if (debugEnabled)
    {
      serverContext.debugInfo("Starting processing for extended request " +
           request.toString());
    }


    // First, make sure that the request has a value and that we can decode it
    // as a sequence containing the target entry DN and optional set of
    // requested attributes.
    final ASN1OctetString requestValue = request.getRequestValue();
    if (requestValue == null)
    {
      if (debugEnabled)
      {
        serverContext.debugWarning(
             "The extended request does not have a value.");
      }

      result.setResultCode(ResultCode.PROTOCOL_ERROR);
      result.setDiagnosticMessage(
           "The provided request did not include a value.");
      return;
    }

    final String dn;
    final String[] attributes;
    try
    {
      final ASN1Sequence requestSequence =
           ASN1Sequence.decodeAsSequence(requestValue.getValue());
      final ASN1Element[] requestElements = requestSequence.elements();
      dn = requestElements[0].decodeAsOctetString().stringValue();
      if (requestElements.length == 2)
      {
        final ASN1Sequence attrSequence = requestElements[1].decodeAsSequence();
        final ASN1Element[] attrElements = attrSequence.elements();
        final ArrayList<String> attrList =
             new ArrayList<String>(attrElements.length);
        for (final ASN1Element e : attrElements)
        {
          attrList.add(e.decodeAsOctetString().stringValue());
        }
        attributes = new String[attrList.size()];
        attrList.toArray(attributes);
      }
      else
      {
        attributes = new String[0];
      }
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);

      result.setResultCode(ResultCode.PROTOCOL_ERROR);
      result.setDiagnosticMessage("Unable to decode the request value:  " +
           StaticUtils.getExceptionMessage(e));
      return;
    }

    if (debugEnabled)
    {
      serverContext.debugInfo("Requested entry DN:  " + dn);
      serverContext.debugInfo("Requested attributes:  " +
           Arrays.toString(attributes));
    }


    // Get an internal connection authenticated as the user issuing the request
    // and use it to perform a search to get the target entry.
    final Entry entry;
    try
    {
      final InternalConnection conn =
           operationContext.getInternalUserConnection();
      entry = conn.getEntry(dn, attributes);
    }
    catch (final LDAPException le)
    {
      serverContext.debugCaught(le);
      result.setResultData(le);
      return;
    }

    if (entry == null)
    {
      result.setResultCode(ResultCode.NO_SUCH_OBJECT);
      result.setDiagnosticMessage("Requested entry " + dn +
           " does not exist or is not readable.");
      return;
    }


    // Encode the value for the result.
    final Collection<Attribute> entryAttributes = entry.getAttributes();
    final ArrayList<ASN1Element> attrElements =
         new ArrayList<ASN1Element>(entryAttributes.size());
    for (final Attribute a : entryAttributes)
    {
      final ArrayList<ASN1Element> valueElements =
           new ArrayList<ASN1Element>(a.size());
      for (final ASN1OctetString s : a.getRawValues())
      {
        valueElements.add(s);
      }

      attrElements.add(new ASN1Sequence(
           new ASN1OctetString(a.getName()),
           new ASN1Set(valueElements)));
    }

    final ASN1Sequence resultSequence = new ASN1Sequence(attrElements);


    // Prepare the result to return to the client.
    result.setResultCode(ResultCode.SUCCESS);
    result.setResultOID(requestOID);
    result.setResultValue(new ASN1OctetString(resultSequence.encode()));
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
         Arrays.asList(ARG_NAME_REQUEST_OID + "=1.3.6.1.4.1.32473.1"),
         "Serve extended requests with a request OID of 1.3.6.1.4.1.32473.1 " +
              "by trying to decode the value as an entry DN and responding " +
              "with the corresponding entry in LDIF form.  Note that this " +
              "OID is in a range specifically reserved by IANA for " +
              "documentation examples and should not actually be used.");

    return exampleMap;
  }
}
