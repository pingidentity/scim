/*
 * Copyright 2011-2013 UnboundID Corp.
 * All Rights Reserved.
 */

/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/ds/resource/legal-notices/cddl.txt
 * or http://www.opensource.org/licenses/cddl1.php.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/ds/resource/legal-notices/cddl.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2007-2013 UnboundID Corp.
 *      Portions Copyright 2006-2008 Sun Microsystems, Inc.
 */

package com.unboundid.scim.extension;

import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.ServerErrorException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * This class was originally copied from
 * com.unboundid.directory.server.types.FilePermission
 *
 * This class provides a mechanism for setting file permissions in a
 * more abstract manner than is provided by the underlying operating
 * system and/or filesystem.  It uses a traditional UNIX-style rwx/ugo
 * representation for the permissions and converts them as necessary
 * to the scheme used by the underlying platform.  It does not provide
 * any mechanism for getting file permissions, nor does it provide any
 * way of dealing with file ownership or ACLs.
 * <BR><BR>
 * Note that the mechanism used to perform this work on UNIX systems
 * is based on executing the <CODE>chmod</CODE> command on the
 * underlying system.  This should be a safe operation because the
 * Directory Server startup scripts should explicitly specify the PATH
 * that should be used.  Nevertheless, it is possible to prevent the
 * server from using the <CODE>Runtime.exec</CODE> method by setting
 * the <CODE>com.unboundid.directory.server.DisableExec</CODE> system property
 * with a value of "true".
 */
public class FilePermission
{
  /**
   * The name of the system property that can be used to indicate whether
   * components should be allowed to use the <CODE>Runtime.exec</CODE> method.
   * If this property is set and the value is anything other than "false",
   * "off", "no", or "0", then components should not allow the use of the
   * <CODE>exec</CODE> method.
   */
  public static final String PROPERTY_DISABLE_EXEC =
       "com.unboundid.directory.server.DisableExec";

  /**
   * The local operating system.
   */
  private static final OperatingSystem operatingSystem =
      OperatingSystem.local();

  /**
   * The bitmask that should be used for indicating whether a file is
   * readable by its owner.
   */
  public static final int OWNER_READABLE = 0x0100;



  /**
   * The bitmask that should be used for indicating whether a file is
   * writable by its owner.
   */
  public static final int OWNER_WRITABLE = 0x0080;



  /**
   * The bitmask that should be used for indicating whether a file is
   * executable by its owner.
   */
  public static final int OWNER_EXECUTABLE = 0x0040;



  /**
   * The bitmask that should be used for indicating whether a file is
   * readable by members of its group.
   */
  public static final int GROUP_READABLE = 0x0020;



  /**
   * The bitmask that should be used for indicating whether a file is
   * writable by members of its group.
   */
  public static final int GROUP_WRITABLE = 0x0010;



  /**
   * The bitmask that should be used for indicating whether a file is
   * executable by members of its group.
   */
  public static final int GROUP_EXECUTABLE = 0x0008;



  /**
   * The bitmask that should be used for indicating whether a file is
   * readable by users other than the owner or group members.
   */
  public static final int OTHER_READABLE = 0x0004;



  /**
   * The bitmask that should be used for indicating whether a file is
   * writable by users other than the owner or group members.
   */
  public static final int OTHER_WRITABLE = 0x0002;



  /**
   * The bitmask that should be used for indicating whether a file is
   * executable by users other than the owner or group members.
   */
  public static final int OTHER_EXECUTABLE = 0x0001;



  // Indicates whether to allow the use of exec for setting file
  // permissions.
  private static boolean allowExec;

  // The method that may be used to specify whether a file is
  // executable by its owner (and optionally others).
  private static Method setExecutableMethod;

  // The method that may be used to specify whether a file is readable
  // by its owner (and optionally others).
  private static Method setReadableMethod;

  // The method that may be used to specify whether a file is wriable
  // by its owner (and optionally others).
  private static Method setWritableMethod;

  // The encoded representation for this file permission.
  private int encodedPermission;



  static
  {
    // Iterate through the available methods and see if any of the
    // Java 6 methods for dealing with permissions are available.
    try
    {
      setExecutableMethod = null;
      setReadableMethod   = null;
      setWritableMethod   = null;

      for (Method m : File.class.getMethods())
      {
        String  name     = m.getName();
        Class[] argTypes = m.getParameterTypes();

        if (name.equals("setExecutable") && (argTypes.length == 2))
        {
          setExecutableMethod = m;
        }
        else if (name.equals("setReadable") && (argTypes.length == 2))
        {
          setReadableMethod = m;
        }
        else if (name.equals("setWritable") && (argTypes.length == 2))
        {
          setWritableMethod = m;
        }
      }
    }
    catch (Exception e)
    {
      Debug.debugException(e);
    }


    // Determine whether we should disable the ability to execute
    // commands on the underlying system even if it could provide more
    // control and capability.
    allowExec = mayUseExec();
  }



  /**
   * Creates a new file permission object with the provided encoded
   * representation.
   *
   * @param  encodedPermission  The encoded representation for this
   *                            file permission.
   */
  public FilePermission(final int encodedPermission)
  {
    this.encodedPermission = encodedPermission;
  }



  /**
   * Creates a new file permission with the specified rights for the
   * file owner.  Users other than the owner will not have any rights.
   *
   * @param  ownerReadable    Indicates whether the owner should have
   *                          the read permission.
   * @param  ownerWritable    Indicates whether the owner should have
   *                          the write permission.
   * @param  ownerExecutable  Indicates whether the owner should have
   *                          the execute permission.
   */
  public FilePermission(final boolean ownerReadable,
                        final boolean ownerWritable,
                        final boolean ownerExecutable)
  {
    encodedPermission = 0x0000;

    if (ownerReadable)
    {
      encodedPermission |= OWNER_READABLE;
    }

    if (ownerWritable)
    {
      encodedPermission |= OWNER_WRITABLE;
    }

    if (ownerExecutable)
    {
      encodedPermission |= OWNER_EXECUTABLE;
    }
  }



  /**
   * Creates a new file permission with the specified rights for the
   * file owner, group members, and other users.
   *
   * @param  ownerReadable    Indicates whether the owner should have
   *                          the read permission.
   * @param  ownerWritable    Indicates whether the owner should have
   *                          the write permission.
   * @param  ownerExecutable  Indicates whether the owner should have
   *                          the execute permission.
   * @param  groupReadable    Indicates whether members of the file's
   *                          group should have the read permission.
   * @param  groupWritable    Indicates whether members of the file's
   *                          group should have the write permission.
   * @param  groupExecutable  Indicates whether members of the file's
   *                          group should have the execute
   *                          permission.
   * @param  otherReadable    Indicates whether other users should
   *                          have the read permission.
   * @param  otherWritable    Indicates whether other users should
   *                          have the write permission.
   * @param  otherExecutable  Indicates whether other users should
   *                          have the execute permission.
   */
  public FilePermission(final boolean ownerReadable,
                        final boolean ownerWritable,
                        final boolean ownerExecutable,
                        final boolean groupReadable,
                        final boolean groupWritable,
                        final boolean groupExecutable,
                        final boolean otherReadable,
                        final boolean otherWritable,
                        final boolean otherExecutable)
  {
    encodedPermission = 0x0000;

    if (ownerReadable)
    {
      encodedPermission |= OWNER_READABLE;
    }

    if (ownerWritable)
    {
      encodedPermission |= OWNER_WRITABLE;
    }

    if (ownerExecutable)
    {
      encodedPermission |= OWNER_EXECUTABLE;
    }

    if (groupReadable)
    {
      encodedPermission |= GROUP_READABLE;
    }

    if (groupWritable)
    {
      encodedPermission |= GROUP_WRITABLE;
    }

    if (groupExecutable)
    {
      encodedPermission |= GROUP_EXECUTABLE;
    }

    if (otherReadable)
    {
      encodedPermission |= OTHER_READABLE;
    }

    if (otherWritable)
    {
      encodedPermission |= OTHER_WRITABLE;
    }

    if (otherExecutable)
    {
      encodedPermission |= OTHER_EXECUTABLE;
    }
  }



  /**
   * Indicates whether this file permission includes the owner read
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          owner read permission, or <CODE>false</CODE> if not.
   */
  public boolean isOwnerReadable()
  {
    return ((encodedPermission & OWNER_READABLE) == OWNER_READABLE);
  }



  /**
   * Indicates whether this file permission includes the owner write
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          owner write permission, or <CODE>false</CODE> if not.
   */
  public boolean isOwnerWritable()
  {
    return ((encodedPermission & OWNER_WRITABLE) == OWNER_WRITABLE);
  }



  /**
   * Indicates whether this file permission includes the owner execute
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          owner execute permission, or <CODE>false</CODE> if not.
   */
  public boolean isOwnerExecutable()
  {
    return ((encodedPermission & OWNER_EXECUTABLE) ==
            OWNER_EXECUTABLE);
  }



  /**
   * Indicates whether this file permission includes the group read
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          group read permission, or <CODE>false</CODE> if not.
   */
  public boolean isGroupReadable()
  {
    return ((encodedPermission & GROUP_READABLE) == GROUP_READABLE);
  }



  /**
   * Indicates whether this file permission includes the group write
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          group write permission, or <CODE>false</CODE> if not.
   */
  public boolean isGroupWritable()
  {
    return ((encodedPermission & GROUP_WRITABLE) == GROUP_WRITABLE);
  }



  /**
   * Indicates whether this file permission includes the group execute
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          group execute permission, or <CODE>false</CODE> if not.
   */
  public boolean isGroupExecutable()
  {
    return ((encodedPermission & GROUP_EXECUTABLE) ==
            GROUP_EXECUTABLE);
  }



  /**
   * Indicates whether this file permission includes the other read
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          other read permission, or <CODE>false</CODE> if not.
   */
  public boolean isOtherReadable()
  {
    return ((encodedPermission & OTHER_READABLE) == OTHER_READABLE);
  }



  /**
   * Indicates whether this file permission includes the other write
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          other write permission, or <CODE>false</CODE> if not.
   */
  public boolean isOtherWritable()
  {
    return ((encodedPermission & OTHER_WRITABLE) == OTHER_WRITABLE);
  }



  /**
   * Indicates whether this file permission includes the other execute
   * permission.
   *
   * @return  <CODE>true</CODE> if this file permission includes the
   *          other execute permission, or <CODE>false</CODE> if not.
   */
  public boolean isOtherExecutable()
  {
    return ((encodedPermission & OTHER_EXECUTABLE) ==
            OTHER_EXECUTABLE);
  }



  /**
   * Indicates whether the there is a mechanism available for setting
   * permissions in the underlying filesystem on the current platform.
   *
   * @return  <CODE>true</CODE> if there is a mechanism available for
   *          setting file permissions on the underlying system (e.g.,
   *          if the server is running in a Java 6 environment, or if
   *          this is a UNIX-based system and the use of exec is
   *          allowed), or <CODE>false</CODE> if no such mechanism is
   *          available.
   */
  public static boolean canSetPermissions()
  {
    if ((setReadableMethod != null) && (setWritableMethod != null) &&
        (setExecutableMethod != null))
    {
      // It's a Java 6 environment, so we can always use that
      // mechanism.
      return true;
    }

    OperatingSystem os = getOperatingSystem();
    if (allowExec && (os != null) && OperatingSystem.isUNIXBased(os))
    {
      // It's a UNIX-based system and we can exec the chmod utility.
      return true;
    }

    // We have no way to set file permissions on this system.
    return false;
  }



  /**
   * Attempts to set the given permissions on the specified file.  If
   * the underlying platform does not allow the full level of
   * granularity specified in the permissions, then an attempt will be
   * made to set them as closely as possible to the provided
   * permissions, erring on the side of security.
   *
   * @param  f  The file to which the permissions should be applied.
   * @param  p  The permissions to apply to the file.
   *
   * @return  <CODE>true</CODE> if the permissions (or the nearest
   *          equivalent) were successfully applied to the specified
   *          file, or <CODE>false</CODE> if was not possible to set
   *          the permissions on the current platform.
   *
   * @throws  FileNotFoundException  If the specified file does not
   *                                 exist.
   *
   * @throws  SCIMException  If a problem occurs while trying to
   *                              set the file permissions.
   */
  public static boolean setPermissions(final File f, final FilePermission p)
         throws FileNotFoundException, SCIMException
  {
    if (! f.exists())
    {
      String message = String.format(
          "Unable to set permissions for file %s because it does not exist",
          f.getAbsolutePath());
      throw new FileNotFoundException(message);
    }


    // If we're running Java 6, then we'll use the methods that Java
    // provides.  Even though it's potentially less fine-grained on a
    // UNIX-based system, it is more efficient and doesn't require an
    // external process.
    if ((setReadableMethod != null) && (setWritableMethod != null) &&
        (setExecutableMethod != null))
    {
      return setUsingJava(f, p);
    }


    // If it's a UNIX-based system, then try using the chmod command
    // to set the permissions.  Otherwise (or if that fails), then try
    // to use the Java 6 API.
    OperatingSystem os = getOperatingSystem();
    if (allowExec && (os != null) && OperatingSystem.isUNIXBased(os))
    {
      return setUsingUNIX(f, p);
    }

    // FIXME -- Consider using cacls on Windows.


    // We have no way to set file permissions on this system.
    return false;
  }



  /**
   * Attempts to set the specified permissions for the given file
   * using the UNIX chmod command.
   *
   * @param  f  The file to which the permissions should be applied.
   * @param  p  The permissions to apply to the file.
   *
   * @return  <CODE>true</CODE> if the permissions were successfully
   *          updated, or <CODE>false</CODE> if not.
   *
   * @throws  SCIMException  If an error occurs while trying to
   *                         execute the chmod command.
   */
  private static boolean setUsingUNIX(final File f, final FilePermission p)
          throws SCIMException
  {
    String[] arguments =
    {
      toUNIXMode(p),
      f.getAbsolutePath()
    };

    int exitCode;
    try
    {
      exitCode = exec("chmod", arguments, null, null, null);
    }
    catch (Exception e)
    {
      Debug.debugException(e);

      String message = String.format(
          "Unable to execute the chmod command to set file permissions on " +
          "%s:  %s",
          f.getAbsolutePath(), String.valueOf(e));
      throw new ServerErrorException(message);
    }

    return (exitCode == 0);
  }



  /**
   * Attempts to set the specified permissions for the given file
   * using the Java 6 <CODE>FILE</CODE> API.  Only the "owner" and
   * "other" permissions will be preserved, since Java doesn't provide
   * a way to set the group permissions directly.
   *
   * @param  f  The file to which the permissions should be applied.
   * @param  p  The permissions to apply to the file.
   *
   * @return  <CODE>true</CODE> if the permissions were successfully
   *          updated, or <CODE>false</CODE> if not.
   *
   * @throws  SCIMException  If a problem occurs while attempting
   *                         to update permissions.
   */
  private static boolean setUsingJava(final File f, final FilePermission p)
          throws SCIMException
  {
    // NOTE:  Due to a very nasty behavior of the Java 6 API, if you
    //        want to want to grant a permission for the owner but not
    //        for anyone else, then you *must* remove it for everyone
    //        first, and then add it only for the owner.  Otherwise,
    //        the other permissions will be left unchanged and if they
    //        had it before then they will still have it.

    boolean anySuccessful   = false;
    boolean anyFailed       = false;
    boolean exceptionThrown = false;

    // Take away read permission from everyone if necessary.
    if (p.isOwnerReadable() && (! p.isOtherReadable()))
    {
      try
      {
        Boolean b =
             (Boolean) setReadableMethod.invoke(f, false, false);
        if (b.booleanValue())
        {
          anySuccessful = true;
        }
        else
        {
          if(!getOperatingSystem().equals(
              OperatingSystem.WINDOWS))
          {
            // On Windows platforms, file readability permissions
            // cannot be set to false. Do not consider this case
            // a failure. http://java.sun.com/developer/
            // technicalArticles/J2SE/Desktop/javase6/enhancements/
            anyFailed = true;
          }
        }
      }
      catch (Exception e)
      {
        Debug.debugException(e);
        exceptionThrown = true;
      }
    }

    // Grant the appropriate read permission.
    try
    {
      boolean ownerOnly =
           (p.isOwnerReadable() != p.isOtherReadable());

      Boolean b = (Boolean)
                  setReadableMethod.invoke(f, p.isOwnerReadable(),
                                           ownerOnly);
      if (b.booleanValue())
      {
        anySuccessful = true;
      }
      else
      {
        if(!getOperatingSystem().equals(
            OperatingSystem.WINDOWS) || p.isOwnerReadable())
        {
          // On Windows platforms, file readabilitys permissions
          // cannot be set to false. Do not consider this case
          // a failure. http://java.sun.com/developer/
          // technicalArticles/J2SE/Desktop/javase6/enhancements/
          anyFailed = true;
        }
      }
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      exceptionThrown = true;
    }


    // Take away write permission from everyone if necessary.
    if (p.isOwnerWritable() && (! p.isOtherWritable()))
    {
      try
      {
        Boolean b =
             (Boolean) setWritableMethod.invoke(f, false, false);
        if (b.booleanValue())
        {
          anySuccessful = true;
        }
        else
        {
          anyFailed = true;
        }
      }
      catch (Exception e)
      {
        Debug.debugException(e);
        exceptionThrown = true;
      }
    }

    // Grant the appropriate write permission.
    try
    {
      boolean ownerOnly =
           (p.isOwnerWritable() != p.isOtherWritable());

      Boolean b = (Boolean)
                  setWritableMethod.invoke(f, p.isOwnerWritable(),
                                           ownerOnly);
      if (b.booleanValue())
      {
        anySuccessful = true;
      }
      else
      {
        anyFailed = true;
      }
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      exceptionThrown = true;
    }


    // Take away execute permission from everyone if necessary.
    if (p.isOwnerExecutable() && (! p.isOtherExecutable()))
    {
      try
      {
        Boolean b =
             (Boolean) setExecutableMethod.invoke(f, false, false);
        if (b.booleanValue())
        {
          anySuccessful = true;
        }
        else
        {
          if(!getOperatingSystem().equals(
              OperatingSystem.WINDOWS))
          {
            // On Windows platforms, file execute permissions
            // cannot be set to false. Do not consider this case
            // a failure. http://java.sun.com/developer/
            // technicalArticles/J2SE/Desktop/javase6/enhancements/
            anyFailed = true;
          }
        }
      }
      catch (Exception e)
      {
        Debug.debugException(e);
        exceptionThrown = true;
      }
    }

    // Grant the appropriate execute permission.
    try
    {
      boolean ownerOnly =
           (p.isOwnerExecutable() != p.isOtherExecutable());

      Boolean b = (Boolean)
                  setExecutableMethod.invoke(f, p.isOwnerExecutable(),
                                             ownerOnly);
      if (b.booleanValue())
      {
        anySuccessful = true;
      }
      else
      {
        if(!getOperatingSystem().equals(
            OperatingSystem.WINDOWS) || p.isOwnerExecutable())
        {
          // On Windows platforms, file execute permissions
          // cannot be set to false. Do not consider this case
          // a failure. http://java.sun.com/developer/
          // technicalArticles/J2SE/Desktop/javase6/enhancements/
          anyFailed = true;
        }
      }
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      exceptionThrown = true;
    }


    if (exceptionThrown)
    {
      // If an exception was thrown, we can't be sure whether or not
      // any permissions were updated.
      String message = String.format(
          "One or more exceptions were thrown in the process of updating " +
          "the file permissions for %s.  Some of the permissions for the " +
          "file may have been altered", f.getAbsolutePath());
      throw new ServerErrorException(message);
    }
    else if (anyFailed)
    {
      if (anySuccessful)
      {
        // Some of the file permissions may have been altered.
        String message = String.format(
            "One or more updates to the file permissions for %s failed, " +
            "but at least one update was successful.  Some of the " +
            "permissions for the file may have been altered",
            f.getAbsolutePath());
        throw new ServerErrorException(message);
      }
      else
      {
        // The file permissions should have been left intact.
        String message = String.format(
            "All of the attempts to update the file permissions for %s " +
            "failed.  The file should be left with its original permissions",
            f.getAbsolutePath());
        throw new ServerErrorException(message);
      }
    }
    else
    {
      return anySuccessful;
    }
  }



  /**
   * Retrieves a three-character string that is the UNIX mode for the
   * provided file permission.  Each character of the string will be a
   * numeric digit from zero through seven.
   *
   * @param  p  The permission to retrieve as a UNIX mode string.
   *
   * @return  The UNIX mode string for the provided permission.
   */
  public static String toUNIXMode(final FilePermission p)
  {
    StringBuilder buffer = new StringBuilder(3);
    toUNIXMode(buffer, p);
    return buffer.toString();
  }



  /**
   * Appends a three-character string that is the UNIX mode for the
   * provided file permission to the given buffer.  Each character of
   * the string will be anumeric digit from zero through seven.
   *
   * @param  buffer  The buffer to which the mode string should be
   *                 appended.
   * @param  p       The permission to retrieve as a UNIX mode string.
   */
  public static void toUNIXMode(final StringBuilder buffer,
                                final FilePermission p)
  {
    byte modeByte = 0x00;
    if (p.isOwnerReadable())
    {
      modeByte |= 0x04;
    }
    if (p.isOwnerWritable())
    {
      modeByte |= 0x02;
    }
    if (p.isOwnerExecutable())
    {
      modeByte |= 0x01;
    }
    buffer.append(String.valueOf(modeByte));

    modeByte = 0x00;
    if (p.isGroupReadable())
    {
      modeByte |= 0x04;
    }
    if (p.isGroupWritable())
    {
      modeByte |= 0x02;
    }
    if (p.isGroupExecutable())
    {
      modeByte |= 0x01;
    }
    buffer.append(String.valueOf(modeByte));

    modeByte = 0x00;
    if (p.isOtherReadable())
    {
      modeByte |= 0x04;
    }
    if (p.isOtherWritable())
    {
      modeByte |= 0x02;
    }
    if (p.isOtherExecutable())
    {
      modeByte |= 0x01;
    }
    buffer.append(String.valueOf(modeByte));
  }



  /**
   * Decodes the provided string as a UNIX mode and retrieves the
   * corresponding file permission.  The mode string must contain
   * three digits between zero and seven.
   *
   * @param  modeString  The string representation of the UNIX mode to
   *                     decode.
   *
   * @return  The file permission that is equivalent to the given UNIX
   *          mode.
   *
   * @throws  SCIMException  If the provided string is not a
   *                         valid three-digit UNIX mode.
   */
  public static FilePermission decodeUNIXMode(final String modeString)
         throws SCIMException
  {
    String invalidModeMessage = String.format(
        "The provided string %s does not represent a valid UNIX file " +
        "mode. UNIX file modes must be a three-character string in which " +
        "each character is a numeric digit between zero and seven",
        String.valueOf(modeString));

    if ((modeString == null) || (modeString.length() != 3))
    {
      throw new ServerErrorException(invalidModeMessage);
    }

    int encodedPermission = 0x0000;
    switch (modeString.charAt(0))
    {
      case '0':
        break;
      case '1':
        encodedPermission |= OWNER_EXECUTABLE;
        break;
      case '2':
        encodedPermission |= OWNER_WRITABLE;
        break;
      case '3':
        encodedPermission |= OWNER_WRITABLE | OWNER_EXECUTABLE;
        break;
      case '4':
        encodedPermission |= OWNER_READABLE;
        break;
      case '5':
         encodedPermission |= OWNER_READABLE | OWNER_EXECUTABLE;
        break;
      case '6':
        encodedPermission |= OWNER_READABLE | OWNER_WRITABLE;
        break;
      case '7':
        encodedPermission |= OWNER_READABLE | OWNER_WRITABLE |
                             OWNER_EXECUTABLE;
        break;
      default:
        throw new ServerErrorException(invalidModeMessage);
    }

    switch (modeString.charAt(1))
    {
      case '0':
        break;
      case '1':
        encodedPermission |= GROUP_EXECUTABLE;
        break;
      case '2':
        encodedPermission |= GROUP_WRITABLE;
        break;
      case '3':
        encodedPermission |= GROUP_WRITABLE | GROUP_EXECUTABLE;
        break;
      case '4':
        encodedPermission |= GROUP_READABLE;
        break;
      case '5':
         encodedPermission |= GROUP_READABLE | GROUP_EXECUTABLE;
        break;
      case '6':
        encodedPermission |= GROUP_READABLE | GROUP_WRITABLE;
        break;
      case '7':
        encodedPermission |= GROUP_READABLE | GROUP_WRITABLE |
                             GROUP_EXECUTABLE;
        break;
      default:
        throw new ServerErrorException(invalidModeMessage);
    }

    switch (modeString.charAt(2))
    {
      case '0':
        break;
      case '1':
        encodedPermission |= OTHER_EXECUTABLE;
        break;
      case '2':
        encodedPermission |= OTHER_WRITABLE;
        break;
      case '3':
        encodedPermission |= OTHER_WRITABLE | OTHER_EXECUTABLE;
        break;
      case '4':
        encodedPermission |= OTHER_READABLE;
        break;
      case '5':
         encodedPermission |= OTHER_READABLE | OTHER_EXECUTABLE;
        break;
      case '6':
        encodedPermission |= OTHER_READABLE | OTHER_WRITABLE;
        break;
      case '7':
        encodedPermission |= OTHER_READABLE | OTHER_WRITABLE |
                             OTHER_EXECUTABLE;
        break;
      default:
        throw new ServerErrorException(invalidModeMessage);
    }

    return new FilePermission(encodedPermission);
  }



  /**
   * Retrieves a string representation of this file permission.
   *
   * @return  A string representation of this file permission.
   */
  public String toString()
  {
    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this file permission to the
   * given buffer.
   *
   * @param  buffer  The buffer to which the data should be appended.
   */
  public void toString(final StringBuilder buffer)
  {
    buffer.append("Owner=");

    if (isOwnerReadable())
    {
      buffer.append("r");
    }
    if (isOwnerWritable())
    {
      buffer.append("w");
    }
    if (isOwnerExecutable())
    {
      buffer.append("x");
    }
    buffer.append(", Group=");

    if (isGroupReadable())
    {
      buffer.append("r");
    }
    if (isGroupWritable())
    {
      buffer.append("w");
    }
    if (isGroupExecutable())
    {
      buffer.append("x");
    }
    buffer.append(", Other=");

    if (isOtherReadable())
    {
      buffer.append("r");
    }
    if (isOtherWritable())
    {
      buffer.append("w");
    }
    if (isOtherExecutable())
    {
      buffer.append("x");
    }
  }



  /**
   * Indicates whether the use of the exec method will be allowed on this
   * system.  It will be allowed by default, but that capability will be removed
   * if the com.unboundid.directory.server.DisableExec system property is set
   * and has any value other than "false", "off", "no", or "0".
   *
   * @return  <CODE>true</CODE> if the use of the exec method should be allowed,
   *          or <CODE>false</CODE> if it should not be allowed.
   */
  public static boolean mayUseExec()
  {
    return (! disableExec());
  }



  /**
   * Indicates whether the Directory Server is disallowed from using
   * the {@code Runtime.exec()} method to launch external
   * commands on the underlying system.
   *
   * @return  {@code true} if the Directory Server is disallowed
   *          from using {@code Runtime.exec()}, or {@code false} otherwise.
   */
  public static boolean disableExec()
  {
    String disableStr = System.getProperty(PROPERTY_DISABLE_EXEC);
    if (disableStr == null)
    {
      return false;
    }
    else
    {
      return disableStr.equalsIgnoreCase("true");
    }
  }



  /**
   * Retrieve the local operating system.
   * @return  The local operating system.
   */
  private static OperatingSystem getOperatingSystem()
  {
    return operatingSystem;
  }



  /**
   * Executes the specified command on the system and captures its output.  This
   * will not return until the specified process has completed.
   *
   * @param  command           The command to execute.
   * @param  args              The set of arguments to provide to the command.
   * @param  workingDirectory  The working directory to use for the command, or
   *                           <CODE>null</CODE> if the default directory
   *                           should be used.
   * @param  environment       The set of environment variables that should be
   *                           set when executing the command, or
   *                           <CODE>null</CODE> if none are needed.
   * @param  output            The output generated by the command while it was
   *                           running.  This will include both standard
   *                           output and standard error.  It may be
   *                           <CODE>null</CODE> if the output does not need to
   *                           be captured.
   *
   * @return  The exit code for the command.
   *
   * @throws java.io.IOException  If an I/O problem occurs while trying to
   *                              execute the command.
   *
   * @throws  SecurityException  If the security policy will not allow the
   *                             command to be executed.
   *
   * @throws InterruptedException If the current thread is interrupted by
   *                              another thread while it is waiting, then
   *                              the wait is ended and an InterruptedException
   *                              is thrown.
   */
  static int exec(final String command, final String[] args,
                          final File workingDirectory,
                          final Map<String,String> environment,
                          final List<String> output)
          throws IOException, SecurityException, InterruptedException
  {
    ArrayList<String> commandAndArgs = new ArrayList<String>();
    commandAndArgs.add(command);
    if ((args != null) && (args.length > 0))
    {
      for (String arg : args)
      {
        commandAndArgs.add(arg);
      }
    }

    ProcessBuilder processBuilder = new ProcessBuilder(commandAndArgs);
    processBuilder.redirectErrorStream(true);

    if ((workingDirectory != null) && workingDirectory.isDirectory())
    {
      processBuilder.directory(workingDirectory);
    }

    if ((environment != null) && (! environment.isEmpty()))
    {
      processBuilder.environment().putAll(environment);
    }

    Process process = processBuilder.start();

    // We must exhaust stdout and stderr before calling waitfor. Since we
    // redirected the error stream, we just have to read from stdout.
    InputStream processStream =  process.getInputStream();
    BufferedReader reader =
            new BufferedReader(new InputStreamReader(processStream));
    String line = null;

    try
    {
      while((line = reader.readLine()) != null)
      {
        if(output != null)
        {
          output.add(line);
        }
      }
    }
    catch(IOException ioe)
    {
      // If this happens, then we have no choice but to forcefully terminate
      // the process.
      try
      {
        process.destroy();
      }
      catch (Exception e)
      {
        Debug.debugException(e);
      }

      throw ioe;
    }
    finally
    {
      try
      {
        reader.close();
      }
      catch(IOException e)
      {
        Debug.debugException(e);
      }
    }

    return process.waitFor();
  }
}
