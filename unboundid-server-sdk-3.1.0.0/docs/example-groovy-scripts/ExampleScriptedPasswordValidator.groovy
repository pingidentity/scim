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



import java.util.List;
import java.util.Set;

import com.unboundid.directory.sdk.common.types.Entry;
import com.unboundid.directory.sdk.common.types.OperationContext;
import com.unboundid.directory.sdk.ds.config.PasswordValidatorConfig;
import com.unboundid.directory.sdk.ds.scripting.ScriptedPasswordValidator;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.ByteString;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a scripted password validator that
 * may be used to ensure that the proposed password does not match the value of
 * a specified set of attributes in the user's entry.  It has one configuration
 * argument:
 * <UL>
 *   <LI>attribute -- The name(s) of the attributes that should be checked.
 *       If multiple attributes should be checked, then this argument should be
 *       provided multiple times with different attribute names.  If no
 *       attribute names are provided, then all user attributes in the entry
 *       will be checked.</LI>
 * </UL>
 */
public final class ExampleScriptedPasswordValidator
       extends ScriptedPasswordValidator
{
  /**
   * The name of the argument that will be used to specify the attribute(s) that
   * will be checked.
   */
  private static final String ARG_NAME_ATTRIBUTE = "attribute";



  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;

  // The set of attributes to be checked.
  private volatile List<String> attributes;



  /**
   * Creates a new instance of this password validator.  All password validator
   * implementations must include a default constructor, but any initialization
   * should generally be done in the {@code initializePasswordValidator} method.
   */
  public ExampleScriptedPasswordValidator()
  {
    // No implementation required.
  }



    /**
     * Updates the provided argument parser to define any configuration arguments
     * which may be used by this password generator.  The argument parser may also
     * be updated to define relationships between arguments (e.g., to specify
     * required, exclusive, or dependent argument sets).
     *
     * @param  parser  The argument parser to be updated with the configuration
     *                 arguments which may be used by this password generator.
     *
     * @throws  ArgumentException  If a problem is encountered while updating the
     *                             provided argument parser.
     */
    @Override()
    public void defineConfigArguments(final ArgumentParser parser)
           throws ArgumentException
    {
      // Add an argument that allows you to specify the set of target attributes.
      Character shortIdentifier = null;
      String    longIdentifier  = ARG_NAME_ATTRIBUTE;
      boolean   required        = false;
      int       maxOccurrences  = 0; // Unlimited.
      String    placeholder     = "{attr}";
      String    description     = "The name or OID of an attribute to check.";

      parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
           required, maxOccurrences, placeholder, description));
    }



    /**
     * Initializes this password validator.
     *
     * @param  serverContext  A handle to the server context for the server in
     *                        which this extension is running.
     * @param  config         The general configuration for this password
     *                        validator.
     * @param  parser         The argument parser which has been initialized from
     *                        the configuration for this password validator.
     *
     * @throws  LDAPException  If a problem occurs while initializing this
     *                         password validator.
     */
    @Override()
    public void initializePasswordValidator(
                     final DirectoryServerContext serverContext,
                     final PasswordValidatorConfig config,
                     final ArgumentParser parser)
           throws LDAPException
    {
      serverContext.debugInfo("Beginning password validator initialization");

      this.serverContext = serverContext;

      // The work we need to do is the same for the initial configuration as for
      // a configuration change, so we'll just call the same method in both cases.
      applyConfig(parser);
    }



    /**
     * Indicates whether the configuration contained in the provided argument
     * parser represents a valid configuration for this extension.
     *
     * @param  config               The general configuration for this password
     *                              validator.
     * @param  parser               The argument parser which has been initialized
     *                              with the proposed configuration.
     * @param  unacceptableReasons  A list that can be updated with reasons that
     *                              the proposed configuration is not acceptable.
     *
     * @return  {@code true} if the proposed configuration is acceptable, or
     *          {@code false} if not.
     */
    @Override()
    public boolean isConfigurationAcceptable(final PasswordValidatorConfig config,
                        final ArgumentParser parser,
                        final List<String> unacceptableReasons)
    {
      // No special validation is required.
      return true;
    }



    /**
     * Attempts to apply the configuration contained in the provided argument
     * parser.
     *
     * @param  config                The general configuration for this password
     *                               validator.
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
    public ResultCode applyConfiguration(final PasswordValidatorConfig config,
                                         final ArgumentParser parser,
                                         final List<String> adminActionsRequired,
                                         final List<String> messages)
    {
      // The work we need to do is the same for the initial configuration as for
      // a configuration change, so we'll just call the same method in both cases.
      applyConfig(parser);

      return ResultCode.SUCCESS;
    }



    /**
     * Applies the configuration contained in the provided argument parser.
     *
     * @param  parser  The argument parser with the configuration to apply.
     */
    private void applyConfig(final ArgumentParser parser)
    {
      List<String> attrs = null;

      final StringArgument attrArg =
           (StringArgument) parser.getNamedArgument(ARG_NAME_ATTRIBUTE);
      if (attrArg != null)
      {
        attrs = attrArg.getValues();
      }

      if ((attrs == null) || attrs.isEmpty())
      {
        attributes = null;
      }
      else
      {
        attributes = attrs;
      }

      serverContext.debugInfo("Set the target attribute set to " + attributes);
    }



  /**
   * Performs any cleanup which may be necessary when this password validator is
   * to be taken out of service.
   */
  @Override()
  public void finalizePasswordValidator()
  {
    // No finalization is required.
  }



  /**
   * Indicates whether the proposed password is acceptable for the specified
   * user.
   *
   * @param  operationContext  The operation context for the associated request.
   *                           It may be associated with an add, modify, or
   *                           password modify operation.
   * @param  newPassword       The proposed new password for the user that
   *                           should be validated.  It will not be encoded or
   *                           obscured in any way.
   * @param  currentPasswords  The current set of passwords for the user, if
   *                           available.  It may be {@code null} if this is
   *                           not available.  Note that even if one or more
   *                           current passwords are available, it may not
   *                           constitute the complete set of passwords for the
   *                           user.
   * @param  userEntry         The entry for the user whose password is being
   *                           changed.
   * @param  invalidReason     A buffer to which a message may be appended to
   *                           indicate why the proposed password is not
   *                           acceptable.
   *
   * @return  {@code true} if the proposed new password is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isPasswordAcceptable(final OperationContext operationContext,
                                      final ByteString newPassword,
                                      final Set<ByteString> currentPasswords,
                                      final Entry userEntry,
                                      final StringBuilder invalidReason)
  {
    // Create a local copy for the attribute set to protect against
    // configuration changes while performing the validation.
    final List<String> attrs = attributes;

    final byte[] newPW = newPassword.getValue();
    if (attrs == null)
    {
      // We should check all attributes in the user entry.
      for (final Attribute a : userEntry.getAttributes())
      {
        if (a.hasValue(newPW))
        {
          invalidReason.append("The password matches the value of another " +
               "attribute in the user entry.");
          return false;
        }
      }
    }
    else
    {
      // We should check only the specified set of attributes.
      for (final String attrName : attrs)
      {
        if (userEntry.hasAttributeValue(attrName, newPW))
        {
          invalidReason.append("The password matches the value of another " +
               "attribute in the user entry.");
          return false;
        }
      }
    }

    return true;
  }
}
