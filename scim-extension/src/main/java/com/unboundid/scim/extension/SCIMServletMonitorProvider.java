/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import com.unboundid.directory.sdk.common.api.MonitorProvider;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.scim.wink.ResourceStats;
import com.unboundid.scim.wink.SCIMApplication;
import com.unboundid.scim.sdk.Version;
import com.unboundid.util.InternalUseOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



/**
 * This class implements a monitor provider for the SCIM plugin.
 */
@InternalUseOnly
public class SCIMServletMonitorProvider extends MonitorProvider
{
  /**
   * The name that identifies this monitor provider instance.
   */
  private final String monitorInstanceName;

  /**
   * The SCIM servlet extension that is associated with this monitor provider.
   */
  private final SCIMServletExtension extension;



  /**
   * Default constructor. This is present so that the
   * GenerateExtensionDocumenatation build tool can call Class.newInstance() on
   * this class.
   */
  public SCIMServletMonitorProvider()
  {
    this(null, null);
  }



  /**
   * Construct a new instance of the SCIM monitor provider.
   *
   * @param monitorInstanceName  The name that identifies this monitor provider
   *                             instance.
   * @param extension            The SCIM servlet extension that is associated
   *                             with this monitor provider.
   */
  SCIMServletMonitorProvider(final String monitorInstanceName,
                             final SCIMServletExtension extension)
  {
    this.monitorInstanceName = monitorInstanceName;
    this.extension = extension;
  }



  /**
   * Retrieves a human-readable name for this extension. This is not used
   * since we are registering this MonitorProvider through the ServerContext
   * object (e.g. it is not a stand-alone extension).
   *
   * @return  A human-readable name for this extension.
   */
  @Override
  public String getExtensionName()
  {
    return "SCIM Servlet Monitor Provider";
  }



  /**
   * Retrieves a human-readable description for this extension.  Each element
   * of the array that is returned will be considered a separate paragraph in
   * generated documentation. This is not used since we are registering this
   * MonitorProvider through the ServerContext object (e.g. it is not a
   * stand-alone extension).
   *
   * @return  A human-readable description for this extension, or {@code null}
   *          or an empty array if no description should be available.
   */
  @Override
  public String[] getExtensionDescription()
  {
    return new String[]
    {
      "This monitor provider supplies information about the SCIM HTTP " +
      "Servlet Extension."
    };
  }



  /**
   * Retrieves the name that identifies this monitor provider instance.  It
   * will be used as the value of the naming attribute for monitor entries.
   * Each monitor provider instance must have a unique name.
   *
   * @return  The name that identifies this monitor provider instance.
   */
  @Override
  public String getMonitorInstanceName()
  {
    return monitorInstanceName;
  }



  /**
   * Retrieves the name of the object class that will be used for monitor
   * entries created from this monitor provider.  This does not need to be
   * defined in the server schema.  It may be {@code null} if a default object
   * class should be used.
   *
   * @return  The name of the object class that will be used for monitor
   *          entries created from this monitor provider.
   */
  @Override
  public String getMonitorObjectClass()
  {
    return "scim-servlet-monitor-entry";
  }



  /**
   * Retrieves a list of attributes containing the data to include in the
   * monitor entry generated from this monitor provider.
   *
   * @return  A list of attributes containing the data to include in the
   *          monitor entry generated from this monitor provider.
   */
  @Override
  public List<Attribute> getMonitorAttributes()
  {
    final ArrayList<Attribute> attrList = new ArrayList<Attribute>();

    attrList.add(new Attribute("version", Version.VERSION));
    attrList.add(new Attribute("build", Version.BUILD_TIMESTAMP));
    attrList.add(new Attribute("revision",
        String.valueOf(Version.REVISION_NUMBER)));

    final SCIMApplication application = extension.getSCIMApplication();

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

    return attrList;
  }
}
