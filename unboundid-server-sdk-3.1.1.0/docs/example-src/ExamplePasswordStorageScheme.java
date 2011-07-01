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

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.directory.sdk.ds.api.PasswordStorageScheme;
import com.unboundid.directory.sdk.ds.config.PasswordStorageSchemeConfig;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.util.Base64;
import com.unboundid.util.ByteString;
import com.unboundid.util.StaticUtils;
import com.unboundid.util.ByteStringBuffer;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a password storage scheme that will
 * use a simple XOR to obscure the provided password.  This implementation is
 * only intended to be used for example purposes, as it is not secure.  It has
 * one configuration argument:
 * <UL>
 *   <LI>key -- The key that will be XORed with the provided password in order
 *       to generate the encoded password.</LI>
 * </UL>
 */
public final class ExamplePasswordStorageScheme
       extends PasswordStorageScheme
{
  /**
   * The name of the argument that will be used as the key to XOR with the
   * provided password.
   */
  private static final String ARG_NAME_KEY = "key";



  // The key to XOR with the provided password.
  private volatile byte[] key;

  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;



  /**
   * Creates a new instance of this password storage scheme.  All password
   * storage scheme implementations must include a default constructor, but any
   * initialization should generally be done in the
   * {@code initializePasswordStorageScheme} method.
   */
  public ExamplePasswordStorageScheme()
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
    return "Example Password Storage Scheme";
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
      "This password storage scheme serves an example that may be used to " +
           "demonstrate the process for creating a third-party password " +
           "storage scheme.  It will essentially XOR the provided clear-text " +
           "password with a given key.  It is not secure and should not " +
           "actually be used in production environments."
    };
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
    // Add an argument that allows you to specify the key for the encoded
    // passwords.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_KEY;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{key}";
    String    description     = "The key to XOR with provided passwords in " +
         "order to generate the encoded representation.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));
  }



  /**
   * Initializes this password storage scheme.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this password storage
   *                        scheme.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this password storage scheme.
   *
   * @throws  LDAPException  If a problem occurs while initializing this
   *                         password storage scheme.
   */
  @Override()
  public void initializePasswordStorageScheme(
                   final DirectoryServerContext serverContext,
                   final PasswordStorageSchemeConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning password storage scheme initialization");

    this.serverContext = serverContext;

    final StringArgument keyArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_KEY);
    key = StaticUtils.getBytes(keyArg.getValue());
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this password
   *                              storage scheme.
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
                      final PasswordStorageSchemeConfig config,
                      final ArgumentParser parser,
                      final List<String> unacceptableReasons)
  {
    boolean acceptable = true;

    // The key must not be empty.
    final StringArgument keyArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_KEY);
    final String keyStr = keyArg.getValue();
    if ((keyStr == null) || (keyStr.length() == 0))
    {
      acceptable = false;
      unacceptableReasons.add("The XOR key must not be empty.");
    }

    return acceptable;
  }



  /**
   * Attempts to apply the configuration contained in the provided argument
   * parser.
   *
   * @param  config                The general configuration for this password
   *                               storage scheme.
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
  public ResultCode applyConfiguration(final PasswordStorageSchemeConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    final StringArgument keyArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_KEY);
    key = StaticUtils.getBytes(keyArg.getValue());

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this password storage
   * scheme is to be taken out of service.
   */
  @Override()
  public void finalizePasswordStorageScheme()
  {
    // No finalization is required.
  }



  /**
   * Retrieves the name for this password storage scheme.  This will be the
   * identifier which appears in curly braces at the beginning of the encoded
   * password.  The name should not include curly braces.
   *
   * @return  The name for this password storage scheme.
   */
  @Override()
  public String getStorageSchemeName()
  {
    return "XOR";
  }



  /**
   * Indicates whether this password storage scheme encodes passwords in a form
   * that allows the original plaintext value to be obtained from the encoded
   * representation.
   *
   * @return  {@code true} if the original plaintext password may be obtained
   *          from the encoded password, or {@code false} if not.
   */
  @Override()
  public boolean isReversible()
  {
    return true;
  }



  /**
   * Indicates whether this password storage scheme encodes passwords in a form
   * that may be considered secure.  A storage scheme should only be considered
   * secure if it is not possible to trivially determine a clear-text value
   * which may be used to generate a given encoded representation.
   *
   * @return  {@code true} if this password storage scheme may be considered
   *          secure, or {@code false} if not.
   */
  @Override()
  public boolean isSecure()
  {
    // This scheme is definitely not secure.
    return false;
  }



  /**
   * Encodes the provided plaintext password.  The encoded password should not
   * include the scheme name in curly braces.
   *
   * @param  plaintext  The plaintext password to be encoded.  It must not be
   *                    {@code null}.
   *
   * @return  The encoded representation of the provided password.
   *
   * @throws  LDAPException  If a problem occurs while attempting to encode the
   *                         password.
   */
  @Override()
  public ByteString encodePassword(final ByteString plaintext)
         throws LDAPException
  {
    // Cache the key locally in case the configuration changes during
    // processing.
    final byte[] k = key;

    // Create a buffer to use to hold the encoded password.
    final ByteStringBuffer buffer = new ByteStringBuffer();

    // First, append the number of bytes in the key followed by a null byte.
    buffer.append(k.length);
    buffer.append((byte) 0x00);

    // Next, append the key itself.  This is massively insecure, but it will
    // protect against existing encoded passwords becoming invalid if the key
    // changes.
    buffer.append(k);

    // Finally, iterate through the bytes of the provided password and XOR them
    // with the bytes in the key, looping through the key multiple times if
    // necessary.
    final byte[] plaintextBytes = plaintext.getValue();
    for (int i=0; i < plaintextBytes.length; i++)
    {
      final byte plaintextByte = plaintextBytes[i];
      final byte keyByte = k[i % k.length];
      buffer.append((byte) (plaintextByte ^ keyByte));
    }


    // Base64-encode the contents of the buffer before returning it to make
    // sure it's safe to represent in ASCII.
    return new ASN1OctetString(Base64.encode(buffer.toByteArray()));
  }



  /**
   * Indicates whether the provided plaintext password could have been used to
   * generate the given encoded password.
   *
   * @param  plaintext  The plaintext password for which to make the
   *                    determination.
   * @param  encoded    The encoded password for which to make the
   *                    determination.  It will not include the scheme name.
   *
   * @return  {@code true} if the provided clear-text password could have been
   *          used to generate the encoded password, or {@code false} if not.
   */
  @Override()
  public boolean passwordMatches(final ByteString plaintext,
                                 final ByteString encoded)
  {
    final ByteString decoded;
    try
    {
      decoded = getPlaintextValue(encoded);
    }
    catch (final Exception e)
    {
      // The provided encoded password was not valid.
      serverContext.debugCaught(e);
      return false;
    }

    // We don't want to use ByteString.equals in this case because they
    // might refer to ASN.1 elements with identical values but different types.
    return Arrays.equals(decoded.getValue(), plaintext.getValue());
  }



  /**
   * Attempts to determine the plaintext password used to generate the provided
   * encoded password.  This method should only be called if the
   * {@link #isReversible} method returns {@code true}.
   *
   * @param  encoded  The encoded password for which to obtain the original
   *                  plaintext password.  It must not be {@code null} and will
   *                  not be prefixed with the scheme name.
   *
   * @return  The plaintext password obtained from the given encoded password.
   *
   * @throws  LDAPException  If this password storage scheme is not reversible,
   *                         or if the provided value could not be decoded to
   *                         its plaintext representation.
   */
  @Override()
  public ByteString getPlaintextValue(final ByteString encoded)
         throws LDAPException
  {
    // First, base64-decode the provided value.
    final byte[] encodedBytes;
    try
    {
      encodedBytes = Base64.decode(encoded.stringValue());
    }
    catch (final Exception e)
    {
      throw new LDAPException(ResultCode.OTHER,
           "Unable to base64-encode the provided password:  " +
                StaticUtils.getExceptionMessage(e), e);
    }


    // Find the index of the first null byte.
    int nullPos = -1;
    for (int i=0; i < encodedBytes.length; i++)
    {
      if (encodedBytes[i] == (byte) 0x00)
      {
        nullPos = i;
        break;
      }
    }

    if (nullPos <= 0)
    {
      throw new LDAPException(ResultCode.OTHER,
           "The password value cannot be decoded.");
    }

    try
    {
      // Get the key length.
      final byte[] keyLengthBytes = new byte[nullPos];
      System.arraycopy(encodedBytes, 0, keyLengthBytes, 0, nullPos);
      final int keyLength =
           Integer.parseInt(StaticUtils.toUTF8String(keyLengthBytes));

      // Get the key data.
      final byte[] keyBytes = new byte[keyLength];
      System.arraycopy(encodedBytes, nullPos+1, keyBytes, 0, keyLength);

      // Get the password XORed with the key.
      final byte[] pwBytes =
           new byte[encodedBytes.length - (nullPos  + 1 + keyLength)];
      System.arraycopy(encodedBytes, nullPos+1+keyLength, pwBytes, 0,
           pwBytes.length);

      // Iterate through the encoded password bytes and XOR them with the same
      // key to get back the original value.
      final byte[] clearPWBytes = new byte[pwBytes.length];
      for (int i=0; i < pwBytes.length; i++)
      {
        final byte encodedPWByte = pwBytes[i];
        final byte keyByte = keyBytes[i % keyLength];
        clearPWBytes[i] = (byte) (encodedPWByte ^ keyByte);
      }

      return new ASN1OctetString(clearPWBytes);
    }
    catch (final Exception e)
    {
      serverContext.debugCaught(e);
      throw new LDAPException(ResultCode.OTHER,
           "An error occurred while attempting to decode the provided " +
                "password:  " + StaticUtils.getExceptionMessage(e), e);
    }
  }



  /**
   * Indicates whether this password storage scheme provides the ability to
   * encode passwords in the authentication password syntax as described in RFC
   * 3112.
   *
   * @return  {@code true} if this password storage scheme supports the
   *          authentication password syntax, or {@code false} if not.
   */
  @Override()
  public boolean supportsAuthPasswordSyntax()
  {
    return true;
  }



  /**
   * Retrieves the name that should be used to identify this password storage
   * scheme when encoding passwords using the authentication password syntax as
   * described in RFC 3112.  This should only be used if the
   * {@link #supportsAuthPasswordSyntax} method returns {@code true}.
   *
   * @return  The name that should be used to identify this password storage
   *          scheme when encoding passwords using the authentication password
   *          syntax.
   */
  @Override()
  public String getAuthPasswordSchemeName()
  {
    return "xor";
  }



  /**
   * Encodes the provided plaintext password using the authentication password
   * syntax as defined in RFC 3112.  This should only be used if the
   * {@link #supportsAuthPasswordSyntax} method returns {@code true}.
   *
   * @param  plaintext  The plaintext password to be encoded.
   *
   * @return  The encoded representation of the provided password.
   *
   * @throws  LDAPException  If a problem occurs while encoding the provided
   *                         password, or if this password storage scheme does
   *                         not support the authentication password syntax.
   */
  @Override()
  public ByteString encodeAuthPassword(final ByteString plaintext)
         throws LDAPException
  {
    final byte[] keyBytes = key;

    final ByteStringBuffer buffer = new ByteStringBuffer();
    buffer.append("xor$");
    Base64.encode(keyBytes, buffer);
    buffer.append('$');

    final byte[] plaintextBytes = plaintext.getValue();
    final byte[] encodedBytes   = new byte[plaintextBytes.length];
    for (int i=0; i < plaintextBytes.length; i++)
    {
      final byte plaintextByte = plaintextBytes[i];
      final byte keyByte = keyBytes[i % keyBytes.length];
      encodedBytes[i] = (byte) (plaintextByte ^ keyByte);
    }

    Base64.encode(encodedBytes, buffer);
    return buffer.toByteString();
  }



  /**
   * Indicates whether the provided plaintext password may be used to generate
   * an encoded password with the given authInfo and authValue elements when
   * using the authentication password syntax as defined in RFC 3112.  This
   * should only be used if the {@link #supportsAuthPasswordSyntax} method
   * returns {@code true}.
   *
   * @param  plaintext  The plaintext password for which to make the
   *                    determination.
   * @param  authInfo   The authInfo portion of the encoded password for which
   *                    to make the determination.
   * @param  authValue  The authValue portion of the encoded password for which
   *                    to make the determination.
   *
   * @return  {@code true} if the provided plaintext password could be used to
   *          generate an encoded password with the given authInfo and authValue
   *          portions, or {@code false} if not.
   */
  @Override()
  public boolean authPasswordMatches(final ByteString plaintext,
                                     final String authInfo,
                                     final String authValue)
  {
    final ByteString decoded;
    try
    {
      decoded = getAuthPasswordPlaintextValue(authInfo, authValue);
    }
    catch (final Exception e)
    {
      // The provided encoded password was not valid.
      serverContext.debugCaught(e);
      return false;
    }

    // We don't want to use ByteString.equals in this case because they
    // might refer to ASN.1 elements with identical values but different types.
    return Arrays.equals(decoded.getValue(), plaintext.getValue());
  }



  /**
   * Obtains the plaintext password that was used to generate an encoded
   * password with the given authInfo and authValue elements when using the
   * authentication password syntax as described in RFC 3112.  This should only
   * be used if both the {@link #supportsAuthPasswordSyntax} and
   * {@link #isReversible} methods return {@code true}.
   *
   * @param  authInfo   The authInfo portion of the encoded password for which
   *                    to retrieve the corresponding plaintext value.
   * @param  authValue  The authValue portion of the encoded password for which
   *                    to retrieve the corresponding plaintext value.
   *
   * @return  The plaintext password that was used to generate the encoded
   *          password.
   *
   * @throws  LDAPException  If this password storage scheme is not reversible,
   *                         if this password storage scheme does not support
   *                         the authentication password syntax, or if some
   *                         other problem is encountered while attempting to
   *                         determine the plaintext password.
   */
  @Override()
  public ByteString getAuthPasswordPlaintextValue(final String authInfo,
                                                  final String authValue)
         throws LDAPException
  {
    // The authInfo will be the base64-encoded key.
    final byte[] keyBytes;
    try
    {
      keyBytes = Base64.decode(authInfo);
    }
    catch (final Exception e)
    {
      throw new LDAPException(ResultCode.OTHER,
           "Unable to base64-decode the authInfo portion of the encoded " +
                "password:  " + StaticUtils.getExceptionMessage(e), e);
    }

    // The authValue will be a base64-encoded representation of the key XORed
    // with the original password.
    final byte[] encodedPWBytes;
    try
    {
      encodedPWBytes = Base64.decode(authValue);
    }
    catch (final Exception e)
    {
      throw new LDAPException(ResultCode.OTHER,
           "Unable to base64-decode the authValue portion of the encoded " +
                "password:  " + StaticUtils.getExceptionMessage(e), e);
    }

    final byte[] clearPWBytes = new byte[encodedPWBytes.length];
    for (int i=0; i < encodedPWBytes.length; i++)
    {
      final byte encodedPWByte = encodedPWBytes[i];
      final byte keyByte = keyBytes[i % keyBytes.length];
      clearPWBytes[i] = (byte) (encodedPWByte ^ keyByte);
    }

    return new ASN1OctetString(clearPWBytes);
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
         Arrays.asList(ARG_NAME_KEY + "=secret"),
         "Encode user passwords by XORing them with the key 'secret'.");

    return exampleMap;
  }
}
