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
 *      Copyright 2011 UnboundID Corp.
 */
package com.unboundid.directory.sdk.examples;



import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.unboundid.directory.sdk.ds.api.CipherStreamProvider;
import com.unboundid.directory.sdk.ds.config.CipherStreamProviderConfig;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a cipher stream provider which will
 * use a password (provided as an argument to this extension) to generate the
 * key to use in order to perform encryption and decryption using a 128-bit AES
 * cipher.  It has a single configuration argument:
 * <UL>
 *   <LI>key-password -- The password to use to generate the encryption
 *       key.</LI>
 * </UL>
 */
public final class ExampleCipherStreamProvider
       extends CipherStreamProvider
{
  /**
   * The name of the argument that will be used to specify the password to use
   * when generating the encryption key.
   */
  private static final String ARG_NAME_KEY_PASSWORD = "key-password";



  // The characters that comprise the key password.
  private volatile char[] keyPassword;

  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;



  /**
   * Creates a new instance of this cipher stream provider.  All cipher stream
   * provider implementations must include a default constructor, but any
   * initialization should generally be done in the
   * {@code initializeCipherStreamProvider} method.
   */
  public ExampleCipherStreamProvider()
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
    return "Example Cipher Stream Provider";
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
      "This cipher stream provider uses a password provided in the " +
           "key-password argument in order to generate the encryption key " +
           "to use for encrypting and decrypting data using a 128-bit AES " +
           "cipher."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this cipher stream provider.  The argument parser may
   * also be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this cipher stream provider.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the subject attribute.
    final Character shortIdentifier = null;
    final String    longIdentifier  = ARG_NAME_KEY_PASSWORD;
    final boolean   required        = true;
    final int       maxOccurrences  = 1;
    final String    placeholder     = "{password}";
    final String    description     = "The password to use to generate the " +
         "key to use for performing 128-bit AES encryption and decryption.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));
  }



  /**
   * Initializes this cipher stream provider.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this cipher stream
   *                        provider.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this cipher stream provider.
   *
   * @throws  LDAPException  If a problem occurs while initializing this cipher
   *                         stream provider.
   */
  @Override()
  public void initializeCipherStreamProvider(
                   final DirectoryServerContext serverContext,
                   final CipherStreamProviderConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning cipher stream provider initialization");

    this.serverContext = serverContext;

    // Get the key password.
    final StringArgument pwArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_KEY_PASSWORD);
    keyPassword = pwArg.getValue().toCharArray();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this cipher
   *                              stream provider.
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
                      final CipherStreamProviderConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    // This is a special case, because we don't want the password to be changed
    // once it is set.  If the password is changed in the same configuration
    // entry (rather than defining a new configuration entry with the new
    // password), then it may no longer be possible to decrypt any data that was
    // previously encrypted with the old key.  As a result, we will only allow
    // the password change if there is no current configuration (which would be
    // the case when first initializing this cipher stream provider), or if the
    // new configuration has the same password as the previous one.
    if (keyPassword == null)
    {
      return true;
    }

    final StringArgument pwArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_KEY_PASSWORD);
    final String newPW = pwArg.getValue();
    if (newPW.equals(new String(keyPassword)))
    {
      return true;
    }
    else
    {
      unacceptableReasons.add("The password used for the cipher stream " +
           "provider must not be changed once it has been set, because it " +
           "can prevent previously-encrypted data from being decrypted.  " +
           "Instead, a new cipher stream provider instance should be " +
           "created and configured for use.");
      return false;
    }
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this cipher
   *                               stream provider.
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
  public ResultCode applyConfiguration(final CipherStreamProviderConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // We will not apply any configuration change, so just return success
    // without doing anything.
    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this cipher stream
   * provider is to be taken out of service.
   */
  @Override()
  public void finalizeCipherStreamProvider()
  {
    // No finalization is required.
  }



  /**
   * Wraps the provided input stream in a cipher input stream that can be used
   * to decrypt data read from the given stream.
   *
   * @param  source  The input stream to be wrapped with a cipher input stream.
   *
   * @return  The cipher input stream which wraps the provided input stream.
   *
   * @throws  LDAPException  If a problem occurs while creating the cipher input
   *                         stream.
   */
  @Override()
  public CipherInputStream createCipherInputStream(final InputStream source)
         throws LDAPException
  {
    final Cipher cipher;
    try
    {
      final SecretKeyFactory keyFactory =
           SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      final PBEKeySpec keySpec = new PBEKeySpec(keyPassword, new byte[8],
           1024, 128);
      final SecretKey encryptionKey =
           new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(),
                "AES");
      cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE, encryptionKey);
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);
      throw new LDAPException(ResultCode.OTHER,
           "An error occurred while attempting to create the encryption " +
                "cipher:  " + StaticUtils.getExceptionMessage(e),
           e);
    }

    return new CipherInputStream(source, cipher);
  }



  /**
   * Wraps the provided output stream in a cipher output stream that can be used
   * to encrypt data written to the given stream.
   *
   * @param  target  The output stream to be wrapped with a cipher output
   *                 stream.
   *
   * @return  The cipher output stream which wraps the provided output stream.
   *
   * @throws  LDAPException  If a problem occurs while creating the cipher
   *                         output stream.
   */
  @Override()
  public CipherOutputStream createCipherOutputStream(final OutputStream target)
         throws LDAPException
  {
    final Cipher cipher;
    try
    {
      final SecretKeyFactory keyFactory =
           SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      final PBEKeySpec keySpec = new PBEKeySpec(keyPassword, new byte[8],
           1024, 128);
      final SecretKey encryptionKey =
           new SecretKeySpec(keyFactory.generateSecret(keySpec).getEncoded(),
                "AES");
      cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);
      throw new LDAPException(ResultCode.OTHER,
           "An error occurred while attempting to create the decryption " +
                "cipher:  " + StaticUtils.getExceptionMessage(e),
           e);
    }

    return new CipherOutputStream(target, cipher);
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

    exampleMap.put(Arrays.asList(ARG_NAME_KEY_PASSWORD + "=secret123"),
         "Use the password 'secret123' to generate the key to use when " +
              "encrypting and decrypting data using a 128-bit AES cipher.");

    return exampleMap;
  }
}
