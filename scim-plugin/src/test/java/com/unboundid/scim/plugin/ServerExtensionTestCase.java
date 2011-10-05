/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.plugin;

import com.unboundid.directory.tests.standalone.BaseTestCase;
import com.unboundid.directory.tests.standalone.ExternalInstance;
import com.unboundid.directory.tests.standalone.util.ZipExtractor;

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
   * Configure a new instance of the scim-plugin.
   *
   * @param instance    The external instance where the plugin is to be created.
   * @param pluginName  The name of the plugin to be created.
   * @param listenPort  The HTTP listen port.
   */
  protected static void configurePlugin(final ExternalInstance instance,
                                        final String pluginName,
                                        final int listenPort)
  {
    instance.dsconfig(
        "create-plugin",
        "--plugin-name", pluginName,
        "--type", "third-party",
        "--set", "enabled:true",
        "--set", "plugin-type:startup",
        "--set", "extension-class:com.unboundid.scim.plugin.SCIMPlugin",
        "--set", "extension-argument:port=" + listenPort,
        "--set",
        "extension-argument:useResourcesFile=config/scim/resources.xml");
  }
}
