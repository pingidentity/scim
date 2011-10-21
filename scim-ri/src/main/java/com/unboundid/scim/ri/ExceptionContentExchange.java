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
