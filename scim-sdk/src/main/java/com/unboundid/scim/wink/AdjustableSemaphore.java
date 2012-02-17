/*
 * Copyright 2012 UnboundID Corp.
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

package com.unboundid.scim.wink;



import java.util.concurrent.Semaphore;



/**
 * A semaphore that can be dynamically resized.
 */
public final class AdjustableSemaphore extends Semaphore
{
  private static final long serialVersionUID = -2211345506986838529L;



  /**
   * The maximum number of permits provided by this semaphore.
   */
  private int maxPermits;



  /**
   * Create a new instance of this semaphore.
   *
   * @param maxPermits The initial number of permits.
   */
  public AdjustableSemaphore(final int maxPermits)
  {
    super(maxPermits);
    this.maxPermits = maxPermits;
  }



  /**
   * Retrieves the maximum number of permits.
   * @return  The maximum number of permits.
   */
  public int getMaxPermits()
  {
    return maxPermits;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public int availablePermits()
  {
    // Do not return a negative number of permits. This could happen if
    // there are threads waiting to acquire a permit and the maximum number
    // of permits is reduced.
    final int availablePermits = super.availablePermits();
    return availablePermits >= 0 ? availablePermits : 0;
  }



  /**
   * Set the maximum number of permits.
   *
   * @param maxPermits The maximum number of permits. Must be greater than zero.
   */
  public synchronized void setMaxPermits(final int maxPermits)
  {
    if (maxPermits < 1)
    {
      throw new IllegalArgumentException();
    }

    int delta = maxPermits - this.maxPermits;
    if (delta == 0)
    {
      return;
    }
    else if (delta > 0)
    {
      release(delta);
    }
    else
    {
      reducePermits(-delta);
    }

    this.maxPermits = maxPermits;
  }
}
