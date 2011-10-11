/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.plugin;

import com.unboundid.directory.sdk.common.api.MonitorProvider;
import com.unboundid.directory.sdk.common.config.MonitorProviderConfig;
import com.unboundid.directory.sdk.common.types.ServerContext;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.scim.ri.SCIMMonitorData;
import com.unboundid.scim.ri.SCIMServer;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.util.args.ArgumentParser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



/**
 * This class implements a monitor provider for the SCIM plugin.
 */
public class SCIMMonitorProvider extends MonitorProvider
{
  /**
   * General configuration for this monitor provider.
   */
  private volatile MonitorProviderConfig config;



  @Override
  public void initializeMonitorProvider(final ServerContext serverContext,
                                        final MonitorProviderConfig config,
                                        final ArgumentParser parser)
      throws LDAPException
  {
    this.config = config;
  }



  @Override
  public void finalizeMonitorProvider()
  {
    // No implementation required.
  }



  @Override
  public ResultCode applyConfiguration(final MonitorProviderConfig config,
                                       final ArgumentParser parser,
                                       final List<String> adminActionsRequired,
                                       final List<String> messages)
  {
    this.config = config;

    return ResultCode.SUCCESS;
  }



  @Override
  public String getExtensionName()
  {
    return "SCIM Monitor Provider";
  }



  @Override
  public String[] getExtensionDescription()
  {
    return new String[]
    {
      "This monitor provider supplies information about the SCIM plugin."
    };
  }



  @Override
  public String getMonitorInstanceName()
  {
    return "SCIM";
  }



  @Override
  public List<Attribute> getMonitorAttributes()
  {
    final ArrayList<Attribute> attrList = new ArrayList<Attribute>();

    final SCIMServer scimServer = SCIMServer.getInstance();
    final SCIMMonitorData monitorData = scimServer.getMonitorData();

    for (final Method m : monitorData.getClass().getMethods())
    {
      String name = m.getName();
      if (name.startsWith("get") && (m.getParameterTypes().length == 0))
      {
        if (name.equals("getClass"))
        {
          continue;
        }

        if (m.getReturnType().isArray())
        {
          continue;
        }

        try
        {
          final StringBuilder builder = new StringBuilder();
          builder.append(Character.toLowerCase(name.charAt(3)));
          if (name.length() > 4)
          {
            builder.append(name.substring(4));
          }

          final String statName = builder.toString();
          final Object value = m.invoke(monitorData);
          if (value == null)
          {
            continue;
          }

          if (value instanceof Collection)
          {
            final Collection<String> values = new ArrayList<String>();
            for (final Object v : (Collection)value)
            {
              values.add(String.valueOf(v));
            }
            final Attribute a = new Attribute(statName, values);
            attrList.add(a);
          }
          else
          {
            final Attribute a = new Attribute(statName, String.valueOf(value));
            attrList.add(a);
          }
        }
        catch (Exception e)
        {
          Debug.debugException(e);
        }
      }
    }

    return attrList;
  }
}
