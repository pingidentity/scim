/*
 * Copyright 2011-2013 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import com.unboundid.directory.sdk.common.types.ServerContext;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;



/**
 * This class implements a Java log handler for SCIM SDK debug logging.
 * It forwards the log messages to the server logging.
 */
public class DebugLogHandler extends Handler
{
  /**
   * The server context used to log debug messages.
   */
  private final ServerContext serverContext;



  /**
   * Create a new instance of the debug log handler.
   *
   * @param serverContext  The server context used to log debug messages.
   */
  public DebugLogHandler(final ServerContext serverContext)
  {
    this.serverContext = serverContext;
  }




  @Override
  public void publish(final LogRecord record)
  {
    if (record.getLevel().equals(Level.SEVERE))
    {
      serverContext.debugError(record.getMessage());
    }
    else if (record.getLevel().equals(Level.WARNING))
    {
      serverContext.debugWarning(record.getMessage());
    }
    else if (record.getLevel().equals(Level.INFO) ||
             record.getLevel().equals(Level.CONFIG))
    {
      serverContext.debugInfo(record.getMessage());
    }
    else
    {
      serverContext.debugVerbose(record.getMessage());
    }
  }



  @Override
  public void flush()
  {
    // No action required.
  }



  @Override
  public void close() throws SecurityException
  {
    // No action required.
  }
}
