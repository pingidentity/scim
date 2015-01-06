/*
 * Copyright 2012-2015 UnboundID Corp.
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

import com.unboundid.scim.SCIMTestCase;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;



/**
 * This class provides test coverage for the {@code AdjustableSemaphore} class.
 */
@Test
public class AdjustableSemaphoreTestCase
    extends SCIMTestCase
{
  /**
   * This test ensures correct behavior when there are threads waiting to
   * acquire a permit and the maximum number of permits is reduced or
   * increased.
   *
   * @throws Exception  If the test fails.
   */
  @Test
  public void testAdjust()
      throws Exception
  {
    final AdjustableSemaphore semaphore = new AdjustableSemaphore(10);
    final AtomicInteger completedThreads = new AtomicInteger(0);
    final Runnable runnable = new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          semaphore.acquire();
          completedThreads.incrementAndGet();
        }
        catch (InterruptedException e)
        {
          // Give up.
        }
      }
    };

    // Take up all available permits.
    assertEquals(semaphore.getMaxPermits(), 10);
    assertEquals(semaphore.availablePermits(), 10);
    semaphore.acquire(5);
    assertEquals(semaphore.availablePermits(), 5);
    semaphore.acquire(5);
    assertEquals(semaphore.availablePermits(), 0);

    // Start some threads to acquire a permit.
    for (int i = 0; i < 4; i++)
    {
      final Thread t = new Thread(runnable);
      t.start();
    }

    // Reduce the maximum permits.
    semaphore.setMaxPermits(5);
    assertEquals(semaphore.getMaxPermits(), 5);

    // The threads should still be waiting.
    assertEquals(semaphore.availablePermits(), 0);
    assertEquals(completedThreads.get(), 0);

    // Release permits down to the maximum.
    semaphore.release(5);

    // The threads should still be waiting.
    assertEquals(semaphore.availablePermits(), 0);
    assertEquals(completedThreads.get(), 0);

    // Release one more permit.
    semaphore.release();

    // One of the threads should be able to acquire a permit.
    boolean passed;
    long start = System.currentTimeMillis();
    long end;
    do
    {
      passed = completedThreads.get() == 1;
      if (!passed)
      {
        Thread.sleep(10);
      }
      end = System.currentTimeMillis();
    }
    while (!passed && (end - start) <= 10000);

    assertEquals(semaphore.availablePermits(), 0);

    // Increase the maximum permits.
    semaphore.setMaxPermits(8);

    // All threads should be able to acquire a permit.
    start = System.currentTimeMillis();
    do
    {
      passed = completedThreads.get() == 5;
      if (!passed)
      {
        Thread.sleep(10);
      }
      end = System.currentTimeMillis();
    }
    while (!passed && (end - start) <= 10000);

    assertEquals(semaphore.availablePermits(), 0);
    assertEquals(completedThreads.get(), 4);
  }
}
