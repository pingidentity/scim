/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.sdk;

import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.scim.ldap.SCIMFilter;
import com.unboundid.scim.schema.Response;
import com.unboundid.util.Debug;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ValuePattern;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;



/**
 * This class provides a thread that may be used to repeatedly perform queries.
 */
public class QueryRateThread
    extends Thread
{
  // Indicates whether a request has been made to stop running.
  private final AtomicBoolean stopRequested;

  // The counter used to track the number of resources returned.
  private final AtomicLong resourceCounter;

  // The counter used to track the number of errors encountered while querying.
  private final AtomicLong errorCounter;

  // The counter used to track the number of queries performed.
  private final AtomicLong queryCounter;

  // The value that will be updated with total duration of the queries.
  private final AtomicLong queryDurations;

  // The thread that is actually performing the queries.
  private final AtomicReference<Thread> queryThread;

  // The client to use for the queries.
  private SCIMClient client;

  // The result code for this thread.
  private final AtomicReference<ResultCode> resultCode;

  // The barrier that will be used to coordinate starting among all the threads.
  private final CyclicBarrier startBarrier;

  // The set of requested attributes for query requests.
  private final String[] attributes;

  // The endpoint URI to use for queries.
  private final String endpointURI;

  // The value pattern to use for the filters.
  private final ValuePattern filterPattern;

  // The barrier to use for controlling the rate of queries.  null if no
  // rate-limiting should be used.
  private final FixedRateBarrier fixedRateBarrier;



  /**
   * Creates a new search rate thread with the provided information.
   *
   * @param  threadNumber     The thread number for this thread.
   * @param  client           The client to use for the queries.
   * @param  endpointURI      The endpointURI to use for the queries.
   * @param  filterPattern    The value pattern for the filters.
   * @param  attributes       The set of attributes to return.
   * @param  startBarrier     A barrier used to coordinate starting between all
   *                          of the threads.
   * @param  queryCounter     A value that will be used to keep track of the
   *                          total number of queries performed.
   * @param  resourceCounter  A value that will be used to keep track of the
   *                          total number of resources returned.
   * @param  queryDurations   A value that will be used to keep track of the
   *                          total duration for all queries.
   * @param  errorCounter     A value that will be used to keep track of the
   *                          number of errors encountered while querying.
   * @param  rateBarrier      The barrier to use for controlling the rate of
   *                          queries.  {@code null} if no rate-limiting
   *                          should be used.
   */
  QueryRateThread(final int threadNumber,
                  final SCIMClient client,
                  final String endpointURI,
                  final ValuePattern filterPattern,
                  final String[] attributes,
                  final CyclicBarrier startBarrier,
                  final AtomicLong queryCounter,
                  final AtomicLong resourceCounter,
                  final AtomicLong queryDurations,
                  final AtomicLong errorCounter,
                  final FixedRateBarrier rateBarrier)
  {
    setName("QueryRate Thread " + threadNumber);
    setDaemon(true);

    this.client          = client;
    this.endpointURI     = endpointURI;
    this.filterPattern   = filterPattern;
    this.attributes      = attributes;
    this.queryCounter    = queryCounter;
    this.resourceCounter = resourceCounter;
    this.queryDurations  = queryDurations;
    this.errorCounter    = errorCounter;
    this.startBarrier    = startBarrier;
    fixedRateBarrier     = rateBarrier;

    resultCode    = new AtomicReference<ResultCode>(null);
    queryThread   = new AtomicReference<Thread>(null);
    stopRequested = new AtomicBoolean(false);
  }



  /**
   * Performs all search processing for this thread.
   */
  @Override()
  public void run()
  {
    queryThread.set(currentThread());

    try
    {
      startBarrier.await();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
    }

    while (! stopRequested.get())
    {
      // If we're trying for a specific target rate, then we might need to
      // wait until issuing the next search.
      if (fixedRateBarrier != null)
      {
        fixedRateBarrier.await();
      }

      final String filterValue = filterPattern.nextValue();
      final long startTime = System.nanoTime();

      try
      {
        final SCIMFilter filter = parseFilter(filterValue);
        final Response response =
            client.getResources(endpointURI, filter, null, null, attributes);
        final Response.Resources resources = response.getResources();
        if (resources != null)
        {
          resourceCounter.addAndGet(resources.getResource().size());
        }

        final Response.Errors errors = response.getErrors();
        if (errors != null)
        {
          errorCounter.incrementAndGet();
        }
      }
      catch (Exception e)
      {
        Debug.debugException(e);
        errorCounter.incrementAndGet();

        final ResultCode rc = ResultCode.OTHER;
        resultCode.compareAndSet(null, rc);
      }

      queryCounter.incrementAndGet();
      queryDurations.addAndGet(System.nanoTime() - startTime);
    }

    queryThread.set(null);
  }



  /**
   * Indicates that this thread should stop running.  It will not wait for the
   * thread to complete before returning.
   */
  void signalShutdown()
  {
    stopRequested.set(true);

    if (fixedRateBarrier != null)
    {
      fixedRateBarrier.shutdownRequested();
    }
  }



  /**
   * Waits for this thread to stop running.
   *
   * @return  A result code that provides information about whether any errors
   *          were encountered during processing.
   */
  ResultCode waitForShutdown()
  {
    final Thread t = queryThread.get();
    if (t != null)
    {
      try
      {
        t.join();
      }
      catch (Exception e)
      {
        Debug.debugException(e);
      }
    }

    resultCode.compareAndSet(null, ResultCode.SUCCESS);
    return resultCode.get();
  }



  /**
   * Parse the provided filter as a SCIM filter.
   *
   * @param filter  The string representation of the SCIM filter.
   *
   * @return  The parsed filter.
   */
  private SCIMFilter parseFilter(final String filter)
  {
    final String[] split = filter.split(" ", 3);

    String filterOp = null;
    if (split[1].equalsIgnoreCase("eq"))
    {
      filterOp = "equalsIgnoreCase";
    }
    else if (split[1].equalsIgnoreCase("co"))
    {
      filterOp = "contains";
    }
    else if (split[1].equalsIgnoreCase("sw"))
    {
      filterOp = "startsWith";
    }
    else if (split[1].equalsIgnoreCase("pr"))
    {
      filterOp = "present";
    }

    String value = null;
    if (split[2] != null)
    {
      value = split[2].substring(1, split[2].length() - 1);
    }

    return new SCIMFilter(split[0], filterOp, value);
  }
}
