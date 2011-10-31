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

package ${build.version.package.name};



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//import com.unboundid.util.ThreadSafety;
//import com.unboundid.util.ThreadSafetyLevel;



/**
 * This class provides information about the current version of the UnboundID
 * SCIM SDK for Java.
 */
//@ThreadSafety(level=ThreadSafetyLevel.COMPLETELY_THREADSAFE)
public final class Version
{
  //
  // NOTE -- This file is dynamically generated.  Do not edit it.  If you need
  //         to add something to it, then add it to the
  //         resource/Version.java.stub file below the SCIM SDK build root.
  //



  /**
   * The official full product name for the SCIM SDK.  For this build, the
   * value is "${build.product.name}".
   */
  public static final String PRODUCT_NAME =
       "${build.product.name}";



  /**
   * The short product name for the SCIM SDK.  This will not have any spaces.
   * For this build, the value is "${build.product.short.name}".
   */
  public static final String SHORT_NAME =
       "${build.product.short.name}";



  /**
   * The version string for the SCIM SDK.
   * For this build, the value is "${build.version}".
   */
  public static final String VERSION =
       "${build.version}";



  /**
   * A timestamp that indicates when this build of the SCIM SDK was generated.
   * For this build, the value is "${scim.build.timestamp}".
   */
  public static final String BUILD_TIMESTAMP = "${scim.build.timestamp}";



  /**
   * The Subversion path associated with the build root directory from which
   * this build of the SCIM SDK was generated.  It may be an absolute local
   * filesystem path if the Subversion path isn't available at build time.
   * For this build, the value is "${build.svn.path}".
   */
  public static final String REPOSITORY_PATH =
       "${build.svn.path}";



  /**
   * The source revision number from which this build of the SCIM SDK was
   * generated.  It may be -1 if the Subversion revision isn't available at
   * build time.  For this build, the value is ${build.svn.version}.
   */
  public static final long REVISION_NUMBER = ${build.svn.revision};



  /**
   * The full version string for the SCIM SDK.  For this build, the value is
   * "${build.product.name} ${build.version}".
   */
  public static final String FULL_VERSION_STRING =
       PRODUCT_NAME + ' ' + VERSION;



  /**
   * The short version string for the SCIM SDK.  This will not have any spaces.
   * For this build, the value is
   * "${build.product.short.name}-${build.version}".
   */
  public static final String SHORT_VERSION_STRING =
       SHORT_NAME + '-' + VERSION;



  /**
   * Prevent this class from being instantiated.
   */
  private Version()
  {
    // No implementation is required.
  }



  /**
   * Prints version information from this class to standard output.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(final String... args)
  {
    for (final String line : getVersionLines())
    {
      System.out.println(line);
    }
  }



  /**
   * Retrieves a list of lines containing information about the SCIM SDK
   * version.
   *
   * @return  A list of lines containing information about the SCIM SDK
   *          version.
   */
  public static List<String> getVersionLines()
  {
    final ArrayList<String> versionLines = new ArrayList<String>(11);

    versionLines.add("Full Version String:   " + FULL_VERSION_STRING);
    versionLines.add("Short Version String:  " + SHORT_VERSION_STRING);
    versionLines.add("Product Name:          " + PRODUCT_NAME);
    versionLines.add("Short Name:            " + SHORT_NAME);
    versionLines.add("Version:               " + VERSION);
    versionLines.add("Build Timestamp:       " + BUILD_TIMESTAMP);
    versionLines.add("Repository Path:       " + REPOSITORY_PATH);
    versionLines.add("Revision Number:       " + REVISION_NUMBER);

    return Collections.unmodifiableList(versionLines);
  }
}
