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
import java.util.List;

import com.unboundid.directory.sdk.proxy.config.ProxyTransformationConfig;
import com.unboundid.directory.sdk.proxy.scripting.ScriptedProxyTransformation;
import com.unboundid.directory.sdk.proxy.types.ProxyOperationContext;
import com.unboundid.directory.sdk.proxy.types.ProxyServerContext;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.CompareRequest;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ReadOnlySearchRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.StringArgument;



/**
 * This class provides a simple example of a scripted proxy transformation which
 * will attempt to replace references to one attribute with references to another.
 * References will be replaced in add, compare, modify, and search requests, and
 * also in search result entries.  It takes two configuration arguments:
 * <LI>
 *   <LI>client-attribute -- The client-side name for the target attribute.</LI>
 *   <LI>server-attribute -- The server-side name for the target attribute.</LI>
 * </LI>
 *
 * References to the client attribute will be replaced with references to the
 * server attribute in requests, and references to the server attribute will be
 * replaced with references to the client attribute in responses.
 */
public final class ExampleScriptedProxyTransformation
       extends ScriptedProxyTransformation
{
  /**
   * The name of the argument that will be used to specify the name of the
   * client attribute.
   */
  private static final String ARG_NAME_CLIENT_ATTR = "client-attribute";



  /**
   * The name of the argument that will be used to specify the name of the
   * server attribute.
   */
  private static final String ARG_NAME_SERVER_ATTR = "server-attribute";



  // The server context for the server in which this extension is running.
  private ProxyServerContext serverContext;

  // The name of the client attribute.
  private volatile String clientAttribute;

  // The name of the server attribute.
  private volatile String serverAttribute;



  /**
   * Creates a new instance of this proxy transformation.  All proxy
   * transformation implementations must include a default constructor, but any
   * initialization should generally be done in the
   * {@code initializeProxyTransformation} method.
   */
  public ExampleScriptedProxyTransformation()
  {
    // No implementation required.
  }



  /**
   * Updates the provided argument parser to define any configuration arguments
   * which may be used by this proxy transformation.  The argument parser may
   * also be updated to define relationships between arguments (e.g., to specify
   * required, exclusive, or dependent argument sets).
   *
   * @param  parser  The argument parser to be updated with the configuration
   *                 arguments which may be used by this proxy transformation.
   *
   * @throws  ArgumentException  If a problem is encountered while updating the
   *                             provided argument parser.
   */
  @Override()
  public void defineConfigArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    // Add an argument that allows you to specify the client attribute.
    Character shortIdentifier = null;
    String    longIdentifier  = ARG_NAME_CLIENT_ATTR;
    boolean   required        = true;
    int       maxOccurrences  = 1;
    String    placeholder     = "{attr}";
    String    description     = "The name that clients will use to reference " +
         "the target attribute.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));


    // Add an argument that allows you to specify the server attribute.
    shortIdentifier = null;
    longIdentifier  = ARG_NAME_SERVER_ATTR;
    required        = true;
    maxOccurrences  = 1;
    placeholder     = "{attr}";
    description     = "The name that the server will use to reference " +
         "the target attribute.";

    parser.addArgument(new StringArgument(shortIdentifier, longIdentifier,
         required, maxOccurrences, placeholder, description));
  }



  /**
   * Initializes this proxy transformation.
   *
   * @param  serverContext  A handle to the server context for the server in
   *                        which this extension is running.
   * @param  config         The general configuration for this proxy
   *                        transformation.
   * @param  parser         The argument parser which has been initialized from
   *                        the configuration for this proxy transformation.
   *
   * @throws  LDAPException  If a problem occurs while initializing this proxy
   *                         transformation.
   */
  @Override()
  public void initializeProxyTransformation(
                   final ProxyServerContext serverContext,
                   final ProxyTransformationConfig config,
                   final ArgumentParser parser)
         throws LDAPException
  {
    serverContext.debugInfo("Beginning proxy transformation initialization");

    this.serverContext = serverContext;


    // Get the client attribute.
    final StringArgument clientArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_CLIENT_ATTR);
    clientAttribute = clientArg.getValue();


    // Get the server attribute.
    final StringArgument serverArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_SERVER_ATTR);
    serverAttribute = serverArg.getValue();
  }



  /**
   * Indicates whether the configuration contained in the provided argument
   * parser represents a valid configuration for this extension.
   *
   * @param  config               The general configuration for this proxy
   *                              transformation.
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
                      final ProxyTransformationConfig config,
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
   * @param  config                The general configuration for this proxy
   *                               transformation.
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
  public ResultCode applyConfiguration(final ProxyTransformationConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    // Get the new client attribute.
    final StringArgument clientArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_CLIENT_ATTR);
    final String newClientAttr = clientArg.getValue();


    // Get the new server attribute.
    final StringArgument serverArg =
         (StringArgument) parser.getNamedArgument(ARG_NAME_SERVER_ATTR);
    final String newServerAttr = serverArg.getValue();


    clientAttribute = newClientAttr;
    serverAttribute = newServerAttr;

    return ResultCode.SUCCESS;
  }



  /**
   * Performs any cleanup which may be necessary when this proxy transformation
   * is to be taken out of service.
   */
  @Override()
  public void finalizeProxyTransformation()
  {
    // No finalization is required.
  }



  /**
   * Applies any necessary transformation to the provided add request.
   *
   * @param  operationContext  Information about the operation being processed
   *                           in the Directory Proxy Server.
   * @param  addRequest        The add request to be transformed.
   *
   * @return  A new add request which has been transformed as necessary, or the
   *          original request if no transformation is required or the provided
   *          add request has been updated in place.
   *
   * @throws  LDAPException  If a problem is encountered while processing the
   *                         transformation, or if the provided request should
   *                         not be forwarded to a backend server.
   */
  @Override()
  public AddRequest transformAddRequest(
                         final ProxyOperationContext operationContext,
                         final AddRequest addRequest)
         throws LDAPException
  {
    // Get the client-side attribute from the add request.  If the add request
    // doesn't have the target attribute then we'll just return the original
    // request.
    final Attribute a = addRequest.getAttribute(clientAttribute);
    if (a == null)
    {
      return addRequest;
    }


    // Duplicate the add request and replace the client-side attribute with the
    // server-side version.
    final AddRequest newRequest = addRequest.duplicate();
    newRequest.removeAttribute(clientAttribute);
    newRequest.addAttribute(serverAttribute, a.getValueByteArrays());

    return newRequest;
  }



  /**
   * Applies any necessary transformation to the provided compare request.
   *
   * @param  operationContext  Information about the operation being processed
   *                           in the Directory Proxy Server.
   * @param  compareRequest    The compare request to be transformed.
   *
   * @return  A new compare request which has been transformed as necessary, or
   *          the original request if no transformation is required or the
   *          provided compare request has been updated in place.
   *
   * @throws  LDAPException  If a problem is encountered while processing the
   *                         transformation, or if the provided request should
   *                         not be forwarded to a backend server.
   */
  @Override()
  public CompareRequest transformCompareRequest(
                             final ProxyOperationContext operationContext,
                             final CompareRequest compareRequest)
         throws LDAPException
  {
    // If the target attribute is the client attribute, then replace it.
    // Otherwise, return the original request.
    if (compareRequest.getAttributeName().equalsIgnoreCase(clientAttribute))
    {
      final CompareRequest newRequest = compareRequest.duplicate();
      newRequest.setAttributeName(serverAttribute);
      return newRequest;
    }
    else
    {
      return compareRequest;
    }
  }



  /**
   * Applies any necessary transformation to the provided modify request.
   *
   * @param  operationContext  Information about the operation being processed
   *                           in the Directory Proxy Server.
   * @param  modifyRequest     The modify request to be transformed.
   *
   * @return  A new modify request which has been transformed as necessary, or
   *          the original request if no transformation is required or the
   *          provided modify request has been updated in place.
   *
   * @throws  LDAPException  If a problem is encountered while processing the
   *                         transformation, or if the provided request should
   *                         not be forwarded to a backend server.
   */
  @Override()
  public ModifyRequest transformModifyRequest(
                            final ProxyOperationContext operationContext,
                            final ModifyRequest modifyRequest)
         throws LDAPException
  {
    // See if any of the modifications reference the client attribute.
    boolean found = false;
    final List<Modification> mods = modifyRequest.getModifications();
    for (final Modification m : mods)
    {
      if (m.getAttributeName().equalsIgnoreCase(clientAttribute))
      {
        found = true;
        break;
      }
    }


    // If the client attribute wasn't found, then return the original request.
    if (! found)
    {
      return modifyRequest;
    }


    // Duplicate the request and replace the modification set.
    final ModifyRequest newRequest = modifyRequest.duplicate();

    final ArrayList<Modification> newMods =
         new ArrayList<Modification>(mods.size());
    for (final Modification m : mods)
    {
      if (m.getAttributeName().equalsIgnoreCase(clientAttribute))
      {
        newMods.add(new Modification(m.getModificationType(), serverAttribute,
             m.getRawValues()));
      }
      else
      {
        newMods.add(m);
      }
    }

    newRequest.setModifications(newMods);

    return newRequest;
  }



  /**
   * Applies any necessary transformation to the provided search request.
   *
   * @param  operationContext  Information about the operation being processed
   *                           in the Directory Proxy Server.
   * @param  searchRequest     The search request to be transformed.
   *
   * @return  A new search request which has been transformed as necessary, or
   *          the original request if no transformation is required or the
   *          provided search request has been updated in place.
   *
   * @throws  LDAPException  If a problem is encountered while processing the
   *                         transformation, or if the provided request should
   *                         not be forwarded to a backend server.
   */
  @Override()
  public SearchRequest transformSearchRequest(
                            final ProxyOperationContext operationContext,
                            final SearchRequest searchRequest)
         throws LDAPException
  {
    // See if the filter or requested attribute list contains the client
    // attribute.
    boolean foundInAttrs = false;
    final String[] attrs = searchRequest.getAttributes();
    for (final String s : attrs)
    {
      if (s.equalsIgnoreCase(clientAttribute))
      {
        foundInAttrs = true;
        break;
      }
    }

    final Filter filter = searchRequest.getFilter();
    final boolean foundInFilter = filterReferencesClientAttr(filter);

    if (! (foundInAttrs || foundInFilter))
    {
      return searchRequest;
    }


    final SearchRequest newRequest = searchRequest.duplicate();

    if (foundInAttrs)
    {
      final String[] newAttrs = new String[attrs.length];
      for (int i=0; i < attrs.length; i++)
      {
        if (attrs[i].equalsIgnoreCase(clientAttribute))
        {
          newAttrs[i] = serverAttribute;
        }
        else
        {
          newAttrs[i] = attrs[i];
        }
      }

      newRequest.setAttributes(newAttrs);
    }

    if (foundInFilter)
    {
      newRequest.setFilter(updateFilter(filter));
    }

    return newRequest;
  }



  /**
   * Indicates whether the provided filter references the client attribute.
   *
   * @param  f  The filter for which to make the determination.
   *
   * @return  {@code true} if the provided filter references the client
   *          attribute, or {@code false} if not.
   */
  private boolean filterReferencesClientAttr(final Filter f)
  {
    switch (f.getFilterType())
    {
      case Filter.FILTER_TYPE_AND:
      case Filter.FILTER_TYPE_OR:
        for (final Filter comp : f.getComponents())
        {
          if (filterReferencesClientAttr(comp))
          {
            return true;
          }
        }
        return false;

      case Filter.FILTER_TYPE_NOT:
        return filterReferencesClientAttr(f.getNOTComponent());

      default:
        final String attrName = f.getAttributeName();
        return ((attrName != null) &&
                (attrName.equalsIgnoreCase(clientAttribute)));
    }
  }



  /**
   * Creates a new filter with all references to the client attribute replaced
   * by references to the server attribute.
   *
   * @param  f  The filter to be replaced.
   *
   * @return  The updated filter.
   */
  private Filter updateFilter(final Filter f)
  {
    switch (f.getFilterType())
    {
      case Filter.FILTER_TYPE_AND:
        Filter[] comps = f.getComponents();
        Filter[] newComps = new Filter[comps.length];
        for (int i=0; i < comps.length; i++)
        {
          newComps[i] = updateFilter(comps[i]);
        }
        return Filter.createANDFilter(newComps);

      case Filter.FILTER_TYPE_OR:
        comps = f.getComponents();
        newComps = new Filter[comps.length];
        for (int i=0; i < comps.length; i++)
        {
          newComps[i] = updateFilter(comps[i]);
        }
        return Filter.createORFilter(newComps);

      case Filter.FILTER_TYPE_NOT:
        return Filter.createNOTFilter(updateFilter(f.getNOTComponent()));

      case Filter.FILTER_TYPE_EQUALITY:
        String attrName = f.getAttributeName();
        if (attrName.equalsIgnoreCase(clientAttribute))
        {
          return Filter.createEqualityFilter(serverAttribute,
               f.getAssertionValueBytes());
        }
        else
        {
          return f;
        }

      case Filter.FILTER_TYPE_SUBSTRING:
        attrName = f.getAttributeName();
        if (attrName.equalsIgnoreCase(clientAttribute))
        {
          return Filter.createSubstringFilter(serverAttribute,
               f.getSubInitialBytes(), f.getSubAnyBytes(),
               f.getSubFinalBytes());
        }
        else
        {
          return f;
        }

      case Filter.FILTER_TYPE_GREATER_OR_EQUAL:
        attrName = f.getAttributeName();
        if (attrName.equalsIgnoreCase(clientAttribute))
        {
          return Filter.createGreaterOrEqualFilter(serverAttribute,
               f.getAssertionValueBytes());
        }
        else
        {
          return f;
        }

      case Filter.FILTER_TYPE_LESS_OR_EQUAL:
        attrName = f.getAttributeName();
        if (attrName.equalsIgnoreCase(clientAttribute))
        {
          return Filter.createLessOrEqualFilter(serverAttribute,
               f.getAssertionValueBytes());
        }
        else
        {
          return f;
        }

      case Filter.FILTER_TYPE_PRESENCE:
        attrName = f.getAttributeName();
        if (attrName.equalsIgnoreCase(clientAttribute))
        {
          return Filter.createPresenceFilter(serverAttribute);
        }
        else
        {
          return f;
        }

      case Filter.FILTER_TYPE_APPROXIMATE_MATCH:
        attrName = f.getAttributeName();
        if (attrName.equalsIgnoreCase(clientAttribute))
        {
          return Filter.createApproximateMatchFilter(serverAttribute,
               f.getAssertionValueBytes());
        }
        else
        {
          return f;
        }

      case Filter.FILTER_TYPE_EXTENSIBLE_MATCH:
        attrName = f.getAttributeName();
        if ((attrName != null) && attrName.equalsIgnoreCase(clientAttribute))
        {
          return Filter.createExtensibleMatchFilter(serverAttribute,
               f.getMatchingRuleID(), f.getDNAttributes(),
               f.getAssertionValueBytes());
        }
        else
        {
          return f;
        }

      default:
        // This should never happen.
        return f;
    }
  }



  /**
   * Applies any necessary transformation to the provided search result entry.
   *
   * @param  operationContext  Information about the operation being processed
   *                           in the Directory Proxy Server.
   * @param  searchRequest     The search request that is being processed.
   * @param  searchEntry       The search result entry to be transformed.
   *
   * @return  A new search result entry which has been transformed as necessary,
   *          the original search result entry if no transformation is required,
   *          or {@code null} if the entry should not be returned to the client.
   */
  @Override()
  public SearchResultEntry transformSearchResultEntry(
                                final ProxyOperationContext operationContext,
                                final ReadOnlySearchRequest searchRequest,
                                final SearchResultEntry searchEntry)
  {
    // Get the server-side attribute from the entry.  If the entry doesn't have
    // the target attribute then we'll just return the original version.
    final Attribute a = searchEntry.getAttribute(serverAttribute);
    if (a == null)
    {
      return searchEntry;
    }


    // Duplicate the entry and replace the server-side attribute with the
    // client-side version.
    final Entry newEntry = searchEntry.duplicate();
    newEntry.removeAttribute(serverAttribute);
    newEntry.addAttribute(clientAttribute, a.getValueByteArrays());

    return new SearchResultEntry(searchEntry.getMessageID(), newEntry,
         searchEntry.getControls());
  }
}
