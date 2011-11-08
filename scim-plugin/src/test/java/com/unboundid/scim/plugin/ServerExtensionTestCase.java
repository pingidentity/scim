/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.plugin;

import com.unboundid.directory.tests.standalone.BaseTestCase;
import com.unboundid.directory.tests.standalone.ExternalInstance;
import com.unboundid.directory.tests.standalone.util.ZipExtractor;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;

import java.io.File;



/**
 * Base class for all scim-plugin test cases.
 */
public class ServerExtensionTestCase extends BaseTestCase
{
  /**
   * Install a server extension in an external instance.
   *
   * @param instance  The external instance where the extension is to be
   *                  installed.
   * @param zipFile   The extension zip file.
   *
   * @throws Exception  If the extension could not be installed.
   */
  protected static void installExtension(final ExternalInstance instance,
                                         final File zipFile)
      throws Exception
  {
    final ZipExtractor extractor = new ZipExtractor(zipFile);
    extractor.extract(instance.getInstanceRoot());
  }



  /**
   * Configure the SCIM extension.
   *
   * @param instance    The external instance where the extension is to be
   *                    configured.
   * @param listenPort  The HTTP listen port.
   */
  protected static void configureExtension(final ExternalInstance instance,
                                           final int listenPort)
  {
    instance.dsconfig(
        "set-log-publisher-prop",
        "--publisher-name", "Server SDK Extension Debug Logger",
        "--set", "enabled:true");

    instance.dsconfig(
        "create-http-servlet-extension",
        "--extension-name", "SCIM",
        "--type", "third-party",
        "--set", "extension-class:" +
                 "com.unboundid.scim.plugin.SCIMServletExtension",
        "--set", "extension-argument:" +
                 "useResourcesFile=config/scim/resources.xml",
        "--set", "extension-argument:debugEnabled");

    instance.dsconfig(
        "create-log-publisher",
        "--publisher-name", "HTTP Common Access",
        "--type", "common-log-file-http-operation",
        "--set", "enabled:true",
        "--set", "log-file:logs/http-common-access",
        "--set", "rotation-policy:24 Hours Time Limit Rotation Policy",
        "--set", "rotation-policy:Size Limit Rotation Policy",
        "--set", "retention-policy:File Count Retention Policy",
        "--set", "retention-policy:Free Disk Space Retention Policy");

    instance.dsconfig(
        "create-connection-handler",
        "--handler-name", "HTTP",
        "--type", "http",
        "--set", "enabled:true",
        "--set", "http-servlet-extension:" + "SCIM",
        "--set", "http-operation-log-publisher:HTTP Common Access",
        "--set", "listen-port:" + listenPort);

//    final int secureListenPort = 8443;
//    instance.dsconfig(
//        "create-connection-handler",
//        "--handler-name", "HTTPS",
//        "--type", "http",
//        "--set", "enabled:true",
//        "--set", "http-servlet-extension:" + "SCIM",
//        "--set", "http-operation-log-publisher:HTTP Common Access",
//        "--set", "listen-port:" + secureListenPort,
//        "--set", "use-ssl:true",
//        "--set", "key-manager-provider:JKS",
//        "--set", "trust-manager-provider:JKS");

    instance.dsconfig(
        "create-monitor-provider",
        "--provider-name", "SCIM",
        "--type", "third-party",
        "--set", "enabled:true",
        "--set", "extension-class:" +
                 "com.unboundid.scim.plugin.SCIMMonitorProvider");

  }



  /**
   * Get a SCIM monitor attribute as a string.
   *
   * @param instance   The external instance containing the monitor data.
   * @param attribute  The desired monitor attribute.
   * @return  The value of the SCIM monitor attribute as a string.
   * @throws LDAPException  If the SCIM monitor entry could not be read.
   */
  protected String getMonitorAsString(final ExternalInstance instance,
                                      final String attribute)
      throws LDAPException
  {
    final Entry entry =
        instance.readEntry("cn=SCIM,cn=monitor");
    return entry.getAttributeValue(attribute);
  }
}
