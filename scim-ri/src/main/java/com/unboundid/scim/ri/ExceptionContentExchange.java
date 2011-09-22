/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri;

import org.eclipse.jetty.client.ContentExchange;



/**
 * This class extends the Jetty {@code ContentExchange} class so that any
 * exception during the exchange can be captured.
 */
class ExceptionContentExchange
    extends ContentExchange
{
  /**
   * The last exception that occurred during the exchange.
   */
  private Throwable throwable;



  /**
   * Create a new content exchange to capture exceptions.
   */
  public ExceptionContentExchange()
  {
    super(true);
  }



  /**
   * Capture any exception that has been thrown during the exchange.
   *
   * @param throwable  The exception that occurred.
   */
  @Override
  protected void onException(final Throwable throwable)
  {
    this.throwable = throwable;
    super.onException(throwable);
  }



  /**
   * Retrieves the last exception that occurred during the exchange, if any.
   *
   * @return  The last exception that occurred during the exchange, or
   *          {@code null} if no exception was thrown.
   */
  public Throwable getException()
  {
    return throwable;
  }



}
