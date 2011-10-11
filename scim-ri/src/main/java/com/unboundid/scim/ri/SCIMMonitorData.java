/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri;

import com.unboundid.scim.sdk.Version;



/**
 * This class provides access to SCIM monitoring data.
 */
public class SCIMMonitorData
{
  /**
   * Retrieve the SCIM version.
   *
   * @return  The SCIM version.
   */
  public String getVersion()
  {
    return Version.VERSION;
  }



  /**
   * Retrieve the SCIM build timestamp.
   *
   * @return  The SCIM build timestamp.
   */
  public String getBuildTimestamp()
  {
    return Version.BUILD_TIMESTAMP;
  }



  /**
   * Retrieve the source revision number from which this build was generated.
   *
   * @return  The source revision number from which this build was generated.
   */
  public long getRevisionNumber()
  {
    return Version.REVISION_NUMBER;
  }
}
