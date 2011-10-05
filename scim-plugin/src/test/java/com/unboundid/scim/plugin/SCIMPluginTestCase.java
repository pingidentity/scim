/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.plugin;

import com.unboundid.directory.tests.standalone.ExternalInstance;
import com.unboundid.directory.tests.standalone.ExternalInstanceId;
import com.unboundid.directory.tests.standalone.ExternalInstanceManager;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.Assert.*;



/**
 * Test coverage for the SCIM plugin.
 */
public class SCIMPluginTestCase extends ServerExtensionTestCase
{
  /**
   * Tests that the plugin can be installed and enabled.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void enablePlugin()
      throws Exception
  {
    final ExternalInstanceManager m = ExternalInstanceManager.singleton();
    final ExternalInstance instance =
        m.getExternalInstance(ExternalInstanceId.BasicServer);

    final File pluginZipFile = new File(System.getProperty("pluginZipFile"));
    installExtension(instance, pluginZipFile);

    instance.startInstance();
    configurePlugin(instance, "scim-plugin", 8181);
    instance.stopInstance();
  }
}
