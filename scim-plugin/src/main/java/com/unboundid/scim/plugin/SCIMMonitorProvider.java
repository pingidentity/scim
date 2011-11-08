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
import com.unboundid.scim.ri.wink.ResourceStats;
import com.unboundid.scim.ri.wink.SCIMApplication;
import com.unboundid.scim.sdk.Version;
import com.unboundid.util.args.ArgumentParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
      "This monitor provider supplies information about the SCIM HTTP " +
      "Servlet Extension."
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

    attrList.add(new Attribute("version", Version.VERSION));
    attrList.add(new Attribute("build", Version.BUILD_TIMESTAMP));
    attrList.add(new Attribute("revision",
        String.valueOf(Version.REVISION_NUMBER)));

    if(SCIMServletExtension.getInstance() != null)
    {
      final SCIMApplication application =
          SCIMServletExtension.getInstance().getSCIMApplication();

      for(ResourceStats stats : application.getResourceStats())
      {
        for(Map.Entry<String, Long> stat : stats.getStats().entrySet())
        {
          attrList.add(
              new Attribute(
                  stats.getName().toLowerCase() + "-resource-" + stat.getKey(),
                  String.valueOf(stat.getValue())));
        }
      }
    }

    return attrList;
  }
}
