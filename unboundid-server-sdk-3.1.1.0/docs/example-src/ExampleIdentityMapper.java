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

import com.unboundid.directory.sdk.common.types.InternalConnection;
import com.unboundid.directory.sdk.ds.api.IdentityMapper;
import com.unboundid.directory.sdk.ds.config.IdentityMapperConfig;
import com.unboundid.directory.sdk.ds.types.DirectoryServerContext;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.DNArgument;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of an identity mapper which will expect
 * expect to find an attribute in a user's entry whose value exactly matches
 * the provided identifier string.  It has two configuration arguments:
 * <UL>
 *   <LI>base-dn -- The base DN below which to search for user entries.  At
 *       least one base DN must be configured.</LI>
 *   <LI>map-attribute -- The name of the attribute in which to search for the
 *       provided identifier string.</LI>
 * </UL>
 */
public final class ExampleIdentityMapper
       extends IdentityMapper
{
  /**
   * The name of the argument that will be used to specify the name of the
   * attribute to search for the provided identifier.
   */
  private static final String ARG_NAME_BASE_DN = "base-dn";



  /**
   * The name of the argument that will be used to specify the name of the
   * attribute to search for the provided identifier.
   */
  private static final String ARG_NAME_MAP_ATTR = "map-attribute";



  // The server context for the server in which this extension is running.
  private DirectoryServerContext serverContext;

  // The list of base DNs to use for the searches.
  private volatile List<DN> baseDNs;

  // The name of the map attribute.
  private volatile String mapAttribute;



  /**
   * Creates a new instance of this identity mapper.  All identity mapper
   * implementations must include a default constructor, but any initialization
   * should generally be done in the {@code initializeIdentityMapper} method.
   */
  public ExampleIdentityMapper()
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
    return "Example Identity Mapper";
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
      "This identity mapper serves an example that may be used to " +
           "demonstrate the process for creating a third-party identity " +
           "mapper.  In order to establish a mapping, exactly one entry must " +
           "contain a specified attribute whose value equals the presented " +
           "identifier.  The mapping will fail if no matching entries are " +
           "found, or if multiple matches are found."
    };
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this identity mapper.  The argument parser may also
   * be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this identity mapper.
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
    String    longIdentifier  = ARG_NAME_BASE_DN;
    boolean   required        = true;
    int       maxOccurrences  = 0; // No maximum.
    String    placeholder     = "{dn}";
    String    description     = "The base DN below which to search for user " +
         "entries.";

    parser.addArgument(new DNArgument(shortIdentifier, longIdentifier, required,
         maxOccurrences, placeholder, description));


    // Add an argument that allows you to specify the map attribute.
    shortIdentifier = null;
    longIdentifier  = ARG_NAME_MAP_ATTR;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{attribute}";
    description     = "The name of the attribute in user entries which holds " +
         "the user ID value that will be provided.  It should be indexed for " +
         "equality below all configured base DNs.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));
  }



  /**
   * Initializes this identity mapper.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this identity mapper.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this identity mapper.
   *
   * @throws  LDAPException  If a problem occurs while initializing this
   *                         identity mapper.
   */
  @Override()
  public void initializeIdentityMapper(
                   final DirectoryServerContext serverContext,
                   final IdentityMapperConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning identity mapper initialization");

    this.serverContext = serverContext;

    // Get the set of base DNs.
    final DNArgument dnArg =
         (DNArgument) parser.getNamedArgument(ARG_NAME_BASE_DN);
    baseDNs = dnArg.getValues();

    // Get the map attribute.
    final StringArgument attrArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_MAP_ATTR);
    mapAttribute = attrArg.getValue();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this identity
   *                              mapper.
   * @param  parser               The argument parser which has been initialized
   *                              with the proposed configuration.
   * @param  unacceptableReasons  A list that can be updated with reasons that
   *                              the proposed configuration is not acceptable.
   *
   * @return  {@code true} if the proposed configuration is acceptable, or
   *          {@code false} if not.
   */
  @Override()
  public boolean isConfigurationAcceptable(final IdentityMapperConfig config,
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
   * @param  config                The general configuration for this identity
   *                               mapper.
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
  public ResultCode applyConfiguration(final IdentityMapperConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the new set of base DNs.
    final DNArgument dnArg =
         (DNArgument) parser.getNamedArgument(ARG_NAME_BASE_DN);
    final List<DN> newBaseDNs = dnArg.getValues();

    // Get the new map attribute.
    final StringArgument attrArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_MAP_ATTR);
    final String newMapAttr = attrArg.getValue();

    // Activate the new configuration.
    baseDNs      = newBaseDNs;
    mapAttribute = newMapAttr;

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this identity mapper is
   * to be taken out of service.
   */
  @Override()
  public void finalizeIdentityMapper()
  {
    // No finalization is required.
  }



  /**
   * Performs any processing which may be necessary to map the provided username
   * to a user within the server.
   *
   * @param  username  The username to be mapped to a user within the server.
   *
   * @return  The DN of the user within the server to which the provided
   *          username corresponds.
   *
   * @throws  LDAPException  If the provided username cannot be mapped to
   *                         exactly one user in the server.
   */
  @Override()
  public String mapUsername(final String username)
         throws LDAPException
  {
    final boolean debugEnabled = serverContext.debugEnabled();
    if (debugEnabled)
    {
      serverContext.debugInfo("Attempting to map username " + username);
    }

    // Construct the search request to issue.  We'll swap out the base DN later.
    final Filter f = Filter.createEqualityFilter(mapAttribute, username);
    final SearchRequest r = new SearchRequest("", SearchScope.SUB, f,
         SearchRequest.NO_ATTRIBUTES);


    // Get an internal connection and use it to perform the searches.
    String userDN = null;
    final InternalConnection conn = serverContext.getInternalRootConnection();
    for (final DN baseDN : baseDNs)
    {
      r.setBaseDN(baseDN);

      try
      {
        // Use the searchForEntry method, which will fail if multiple entries
        // are returned.  However, we still need to check to see if a match was
        // already found under a different base DN.
        final SearchResultEntry searchEntry = conn.searchForEntry(r);
        if (searchEntry == null)
        {
          if (debugEnabled)
          {
            serverContext.debugInfo("No match was found below base DN " +
                 baseDN);
          }

          continue;
        }
        else
        {
          if (debugEnabled)
          {
            serverContext.debugInfo("Found matching entry " + userDN);
          }
        }

        if (userDN == null)
        {
          userDN = searchEntry. getDN();
        }
        else
        {
          throw new LDAPException(ResultCode.SIZE_LIMIT_EXCEEDED,
               "Multiple user entries were found matching filter " +
                    f.toString());
        }
      }
      catch (final LDAPSearchException lse)
      {
        serverContext.debugCaught(lse);

        // We should examine the result code to determine how to proceed.
        switch (lse.getResultCode().intValue())
        {
          case ResultCode.NO_SUCH_OBJECT_INT_VALUE:
            // This means that the base entry doesn't exist.  We can ignore
            // this.
            break;

          case ResultCode.SIZE_LIMIT_EXCEEDED_INT_VALUE:
            // This means that multiple entries were found matching the filter.
            throw new LDAPException(ResultCode.SIZE_LIMIT_EXCEEDED,
                 "Multiple user entries were found matching filter" +
                      f.toString(), lse);

          default:
            // We'll just re-throw the exception as-is.
            throw lse;
        }
      }
    }


    // Return the DN of the matching user, or null if none was found.
    return userDN;
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
              ARG_NAME_BASE_DN + "=dc=example,dc=com",
              ARG_NAME_MAP_ATTR + "=uid"),
         "Attempt to map the provided identifier string to user entries by " +
              "searching below dc=example,dc=com for user entries containing " +
              "a uid value equal to the provided identifier.");

    return exampleMap;
  }
}
