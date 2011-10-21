/*
 * Copyright 2011 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
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
