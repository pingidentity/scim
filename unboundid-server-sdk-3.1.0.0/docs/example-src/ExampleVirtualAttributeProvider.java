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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import com.unboundid.directory.sdk.common.types.Entry;
import com.unboundid.directory.sdk.common.types.OperationContext;
import com.unboundid.directory.sdk.ds.api.VirtualAttributeProvider;
import com.unboundid.directory.sdk.ds.config.VirtualAttributeProviderConfig;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a virtual attribute provider that
 * will generate a virtual attribute whose value will be the reverse of another
 * attribute in the same entry.  It takes a single configuration argument:
 * <UL>
 *   <LI>source-attribute -- The name of the attribute in the entry whose value
 *       will be reversed to obtain the values for the virtual attribute.</LI>
 * </UL>
 */
public final class ExampleVirtualAttributeProvider
       extends VirtualAttributeProvider
{
  /**
   * The name of the argument that will be used for the argument used to specify
   * the attribute whose values should be reversed.
   */
  private static final String ARG_NAME_ATTR = "source-attribute";



  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;

  // The source attribute from which to obtain the data for the virtual
  // attribute.
  private volatile String sourceAttribute;



  /**
   * Creates a new instance of this virtual attribute provider.  All virtual
   * attribute provider implementations must include a default constructor, but
   * any initialization should generally be done in the
   * {@code initializeVirtualAttributeProvider} method.
   */
  public ExampleVirtualAttributeProvider()
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
    return "Example Virtual Attribute Provider";
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
      "This virtual attribute provider serves as an example that may be used " +
           "to demonstrate the process for creating a third-party virtual " +
           "attribute provider.  It will generate an attribute whose values " +
           "are the same as the values of another attribute in the same " +
           "entry but with the characters of those values in reverse order."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this virtual attribute provider.  The argument parser
   * may also be updated to define relationships between arguments (e.g., to
   * specify required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this virtual attribute
   *                 provider.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the source attribute name.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_ATTR;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{attr}";
    String    description     = "The name of the attribute whose values " +
         "should be reversed to generate the values for the virtual attribute.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));
  }



  /**
   * Initializes this virtual attribute provider.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this virtual attribute
   *                        provider.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this virtual attribute
   *                        provider.
   *
   * @throws  LDAPException  If a problem occurs while initializing this virtual
   *                         attribute provider.
   */
  @Override()
  public void initializeVirtualAttributeProvider(
                   final DirectoryServerContext serverContext,
                   final VirtualAttributeProviderConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    this.serverContext = serverContext;

    // Get the source attribute name.
    final StringArgument arg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_ATTR);
    sourceAttribute = arg.getValue();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this virtual
   *                              attribute provider.
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
                      final VirtualAttributeProviderConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    // The argument parser will handle all of the necessary validation, so
    // we don't need to do anything here.
    return true;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this virtual
   *                               attribute provider.
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
                         final VirtualAttributeProviderConfig config,
                         final ArgumentParser parser,
                         final List<String> adminActionsRequired,
                         final List<String> messages)
  {
    // Get the new source attribute name.
    final StringArgument arg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_ATTR);
    sourceAttribute = arg.getValue();

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this virtual attribute
   * provider is to be taken out of service.
   */
  @Override()
  public void finalizeVirtualAttributeProvider()
  {
    // No implementation required.
  }



  /**
   * Indicates whether the server may cache values generated by this virtual
   * attribute provider for reuse against the same entry in the course of
   * processing the same operation.
   *
   * @return  {@code true} if the server may cache the value generated by this
   *          virtual attribute provider for reuse with the same entry in the
   *          same operation, or {@code false} if not.
   */
  @Override()
  public boolean mayCacheInOperation()
  {
    // The values of this virtual attribute are safe to cache.
    return true;
  }



  /**
   * Indicates whether this virtual attribute provider may generate attributes
   * with multiple values.
   *
   * @return  {@code true} if this virtual attribute provider may generate
   *          attributes with multiple values, or {@code false} if it will only
   *          generate single-valued attributes.
   */
  @Override()
  public boolean isMultiValued()
  {
    // If the source attribute is multi-valued, then the virtual attribute will
    // also be multi-valued.
    return true;
  }



  /**
   * Generates an attribute for inclusion in the provided entry.
   *
   * @param  operationContext  The operation context for the operation in
   *                           progress, if any.  It may be {@code null} if no
   *                           operation is available.
   * @param  entry             The entry for which the attribute is to be
   *                           generated.
   * @param  attributeName     The name of the attribute to be generated.
   *
   * @return  The generated attribute, or {@code null} if no attribute should be
   *          generated.
   */
  @Override()
  public Attribute generateAttribute(final OperationContext operationContext,
                                     final Entry entry,
                                     final String attributeName)
  {
    final List<Attribute> attrList = entry.getAttribute(sourceAttribute);
    if ((attrList == null) || attrList.isEmpty())
    {
      // The source attribute doesn't exist, so we can't generate a virtual
      // attribute.
      if (serverContext.debugEnabled())
      {
        serverContext.debugInfo("Returning null because attribute " +
             sourceAttribute + " does not exist in entry " + entry.getDN());
      }
      return null;
    }

    final LinkedHashSet<String> values = new LinkedHashSet<String>();
    for (final Attribute a : attrList)
    {
      for (final String s : a.getValues())
      {
        values.add(new StringBuilder(s).reverse().toString());
      }
    }

    if (values.isEmpty())
    {
      if (serverContext.debugEnabled())
      {
        serverContext.debugInfo("Returning null because attribute " +
             sourceAttribute + " does not have any values in entry " +
             entry.getDN());
      }
      return null;
    }
    else
    {
      final Attribute va = new Attribute(attributeName, values);
      if (serverContext.debugEnabled())
      {
        serverContext.debugInfo("Generated virtual attribute " + va);
      }

      return va;
    }
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
         Arrays.asList(ARG_NAME_ATTR + "=cn"),
         "Generate a virtual attribute whose values will be the reverse of " +
              "the values of the cn attribute in the same entry.");

    return exampleMap;
  }
}
