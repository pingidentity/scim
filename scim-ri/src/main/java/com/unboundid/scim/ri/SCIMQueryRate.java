/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri;

import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.util.ColumnFormatter;
import com.unboundid.util.CommandLineTool;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.FormattableColumn;
import com.unboundid.util.HorizontalAlignment;
import com.unboundid.util.OutputFormat;
import com.unboundid.util.ValuePattern;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.BooleanArgument;
import com.unboundid.util.args.FileArgument;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;

import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import static com.unboundid.util.StaticUtils.NO_STRINGS;



/**
 * This class provides a tool that can be used to query a SCIM server repeatedly
 * using multiple threads.  It can help provide an estimate of the query
 * performance that a SCIM server is able to achieve.  The query filter may be
 * a value pattern as described in the {@link com.unboundid.util.ValuePattern}
 * class.  This makes it possible to query over a range of resources rather
 * than repeatedly performing queries with the same filter.
 * <BR><BR>
 * All of the necessary information is provided using command line arguments.
 * Supported arguments are as follows:
 * <UL>
 *   <LI>"-h {address}" or "--hostname {address}" -- Specifies the address of
 *       the SCIM server.  If this isn't specified, then a default of
 *       "localhost" will be used.</LI>
 *   <LI>"-p {port}" or "--port {port}" -- Specifies the port number of the
 *       SCIM server.  If this isn't specified, then a default port of 80
 *       will be used.</LI>
 *   <LI>"--authID {userName}" -- Specifies the authentication ID to use when
 *       authenticating using basic auth.</LI>
 *   <LI>"-w {password}" or "--authPassword {password}" -- Specifies the
 *       password to use when authenticating using basic auth or a
 *       password-based SASL mechanism.</LI>
 *   <LI>"--resourceName {resource-name}" -- specifies the name of resources to
 *       be queried.  If this isn't specified, then a default of "User" will
 *       be used.</LI>
 *   <LI>"--endpointURI {endpointURI}" -- specifies the endpoint URI to use for
 *       the queries.  If this isn't specified, then a default of "/Users" will
 *       be used. The endpoint may have a .json or .xml suffix to indicate that
 *       the resources should be returned in the specified form. The default is
 *       to use JSON.</LI>
 *   <LI>"-f {filter}" or "--filter {filter}" -- specifies the filter to use for
 *       the queries.  It may be a simple filter, or it may be a value pattern
 *       to express a range of filters. If this isn't specified, then no
 *       filtering is requested.</LI>
 *   <LI>"-A {name}" or "--attribute {name}" -- specifies the name of an
 *       attribute that should be included in resources returned from the
 *       server. If this isn't specified, then all resource attributes will be
 *       requested. Multiple attributes may be requested with multiple instances
 *       of this argument.</LI>
 *   <LI>"-t {num}" or "--numThreads {num}" -- specifies the number of
 *       concurrent threads to use when performing the queries.  If this is not
 *       provided, then a default of one thread will be used.</LI>
 *   <LI>"-i {sec}" or "--intervalDuration {sec}" -- specifies the length of
 *       time in seconds between lines out output.  If this is not provided,
 *       then a default interval duration of five seconds will be used.</LI>
 *   <LI>"-I {num}" or "--numIntervals {num}" -- specifies the maximum number of
 *       intervals for which to run.  If this is not provided, then it will
 *       run forever.</LI>
 *   <LI>"-r {queries-per-second}" or "--ratePerSecond {queries-per-second}"
 *       -- specifies the target number of queries to perform per second.  It
 *       is still necessary to specify a sufficient number of threads for
 *       achieving this rate.  If this option is not provided, then the tool
 *       will run at the maximum rate for the specified number of threads.</LI>
 *   <LI>"--warmUpIntervals {num}" -- specifies the number of intervals to
 *       complete before beginning overall statistics collection.</LI>
 *   <LI>"--timestampFormat {format}" -- specifies the format to use for
 *       timestamps included before each output line.  The format may be one of
 *       "none" (for no timestamps), "with-date" (to include both the date and
 *       the time), or "without-date" (to include only time time).</LI>
 *   <LI>"-c" or "--csv" -- Generate output in CSV format rather than a
 *       display-friendly format.</LI>
 * </UL>
 */
public class SCIMQueryRate
    extends CommandLineTool
{
  // Arguments used to communicate with a SCIM server.
  private FileArgument    authPasswordFile;
  private IntegerArgument port;
  private StringArgument  authID;
  private StringArgument  authPassword;
  private StringArgument  host;

  // The argument used to indicate whether to generate output in CSV format.
  private BooleanArgument csvFormat;

  // The argument used to specify the collection interval.
  private IntegerArgument collectionInterval;

  // The argument used to specify the number of intervals.
  private IntegerArgument numIntervals;

  // The argument used to specify the number of threads.
  private IntegerArgument numThreads;

  // The argument used to specify the seed to use for the random number
  // generator.
  private IntegerArgument randomSeed;

  // The target rate of searches per second.
  private IntegerArgument ratePerSecond;

  // The number of warm-up intervals to perform.
  private IntegerArgument warmUpIntervals;

  // The argument used to specify the attributes to return.
  private StringArgument attributes;

  // The argument used to specify the endpoint URI for the queries.
  private StringArgument endpointURI;

  // The argument used to specify the filters for the queries.
  private StringArgument filter;

  // The argument used to specify the name of resources to be queried.
  private StringArgument resourceName;

  // The argument used to specify the timestamp format.
  private StringArgument timestampFormat;



  /**
   * Parse the provided command line arguments and make the appropriate set of
   * changes.
   *
   * @param  args  The command line arguments provided to this program.
   */
  public static void main(final String[] args)
  {
    final ResultCode resultCode = main(args, System.out, System.err);
    if (resultCode != ResultCode.SUCCESS)
    {
      System.exit(resultCode.intValue());
    }
  }



  /**
   * Parse the provided command line arguments and make the appropriate set of
   * changes.
   *
   * @param  args       The command line arguments provided to this program.
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   *
   * @return  A result code indicating whether the processing was successful.
   */
  public static ResultCode main(final String[] args,
                                final OutputStream outStream,
                                final OutputStream errStream)
  {
    final SCIMQueryRate queryRate = new SCIMQueryRate(outStream, errStream);
    return queryRate.runTool(args);
  }



  /**
   * Creates a new instance of this tool.
   *
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   */
  public SCIMQueryRate(final OutputStream outStream,
                       final OutputStream errStream)
  {
    super(outStream, errStream);
  }



  /**
   * Retrieves the name for this tool.
   *
   * @return  The name for this tool.
   */
  @Override()
  public String getToolName()
  {
    return "scim-query-rate";
  }



  /**
   * Retrieves the description for this tool.
   *
   * @return  The description for this tool.
   */
  @Override()
  public String getToolDescription()
  {
    return "Perform repeated resource queries against a SCIM server.";
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void addToolArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    String description = "The IP address or resolvable name to use to " +
                         "connect to the server.  If this is not " +
                         "provided, then a default value of 'localhost' " +
                         "will be used.";
    host = new StringArgument('h', "hostname", true, 1, "{host}",
                              description, "localhost");
    parser.addArgument(host);


    description = "The port to use to connect to the server.  If " +
                  "this is not provided, then a default value of 80 will " +
                  "be used.";
    port = new IntegerArgument('p', "port", true, 1, "{port}",
                               description, 1, 65535, 80);
    parser.addArgument(port);


    description = "The ID to use to authenticate to the server when " +
                  "performing basic authentication";
    authID = new StringArgument(null, "authID", false, 1, "{userName}",
                                description);
    parser.addArgument(authID);


    description = "The password to use to authenticate to the server when " +
                  "performing basic authentication or a password-based " +
                  "SASL mechanism.";
    authPassword = new StringArgument('w', "authPassword", false, 1,
                                      "{password}", description);
    parser.addArgument(authPassword);


    description = "The path to the file containing the password to use to " +
                  "authenticate to the server when performing basic " +
                  "authentication or a password-based SASL mechanism.";
    authPasswordFile = new FileArgument('j', "authPasswordFile", false, 1,
                                        "{path}", description, true, true, true,
                                        false);
    parser.addArgument(authPasswordFile);


    description = "The name of resources to be queried.  If this " +
                  "isn't specified, then a default of 'User' will " +
                  "be used.";
    resourceName = new StringArgument(null, "resourceName", false, 1,
                                     "{resource-name}", description, null,
                                     Arrays.asList("User"));
    parser.addArgument(resourceName);


    description = "The endpoint URI to use for the queries.  If this " +
                  "isn't specified, then a default of '/Users' will " +
                  "be used. The endpoint may have a '.json' or '.xml' " +
                  "suffix to indicate that the resources should be " +
                  "returned in the specified form. The default is to " +
                  "use JSON.";
    endpointURI = new StringArgument(null, "endpointURI", false, 1,
                                     "{endpointURI}", description, null,
                                     Arrays.asList("/Users"));
    parser.addArgument(endpointURI);


    description = "The filter to use for the queries.  It may be a simple " +
                  "filter, or it may be a value pattern to express a range " +
                  "of filters (e.g., \"userName eq user.[1-1000]\"). If this " +
                  "isn't specified, then no filtering is requested.";
    filter = new StringArgument('f', "filter", false, 1, "{filter}",
                                description);
    parser.addArgument(filter);


    description = "The name of an attribute to include in resources returned " +
                  "from the queries.  Multiple attributes may be requested " +
                  "by providing this argument multiple times.  If no request " +
                  "attributes are provided, then the resources returned will " +
                  "include all available attributes.";
    attributes = new StringArgument('A', "attribute", false, 0, "{name}",
                                    description);
    parser.addArgument(attributes);


    description = "The number of threads to use to perform the queries.  If " +
                  "this is not provided, then a default of one thread will " +
                  "be used.";
    numThreads = new IntegerArgument('t', "numThreads", true, 1, "{num}",
                                     description, 1, Integer.MAX_VALUE, 1);
    parser.addArgument(numThreads);


    description = "The length of time in seconds between output lines.  If " +
                  "this is not provided, then a default interval of five " +
                  "seconds will be used.";
    collectionInterval = new IntegerArgument('i', "intervalDuration", true, 1,
                                             "{num}", description, 1,
                                             Integer.MAX_VALUE, 5);
    parser.addArgument(collectionInterval);


    description = "The maximum number of intervals for which to run.  If " +
                  "this is not provided, then the tool will run until it is " +
                  "interrupted.";
    numIntervals = new IntegerArgument('I', "numIntervals", true, 1, "{num}",
                                       description, 1, Integer.MAX_VALUE,
                                       Integer.MAX_VALUE);
    parser.addArgument(numIntervals);

    description = "The target number of queries to perform per second.  It " +
                  "is still necessary to specify a sufficient number of " +
                  "threads for achieving this rate.  If this option is not " +
                  "provided, then the tool will run at the maximum rate for " +
                  "the specified number of threads.";
    ratePerSecond = new IntegerArgument('r', "ratePerSecond", false, 1,
                                        "{searches-per-second}", description,
                                        1, Integer.MAX_VALUE);
    parser.addArgument(ratePerSecond);

    description = "The number of intervals to complete before beginning " +
                  "overall statistics collection.  Specifying a nonzero " +
                  "number of warm-up intervals gives the client and server " +
                  "a chance to warm up without skewing performance results.";
    warmUpIntervals = new IntegerArgument(null, "warmUpIntervals", true, 1,
         "{num}", description, 0, Integer.MAX_VALUE, 0);
    parser.addArgument(warmUpIntervals);

    description = "Indicates the format to use for timestamps included in " +
                  "the output.  A value of 'none' indicates that no " +
                  "timestamps should be included.  A value of 'with-date' " +
                  "indicates that both the date and the time should be " +
                  "included.  A value of 'without-date' indicates that only " +
                  "the time should be included.";
    final LinkedHashSet<String> allowedFormats = new LinkedHashSet<String>(3);
    allowedFormats.add("none");
    allowedFormats.add("with-date");
    allowedFormats.add("without-date");
    timestampFormat = new StringArgument(null, "timestampFormat", true, 1,
         "{format}", description, allowedFormats, "none");
    parser.addArgument(timestampFormat);

    description = "Generate output in CSV format rather than a " +
                  "display-friendly format";
    csvFormat = new BooleanArgument('c', "csv", 1, description);
    parser.addArgument(csvFormat);

    description = "Specifies the seed to use for the random number generator.";
    randomSeed = new IntegerArgument('R', "randomSeed", false, 1, "{value}",
         description);
    parser.addArgument(randomSeed);

    parser.addDependentArgumentSet(authID, authPassword, authPasswordFile);
    parser.addExclusiveArgumentSet(authPassword, authPasswordFile);
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public LinkedHashMap<String[],String> getExampleUsages()
  {
    final LinkedHashMap<String[],String> examples =
         new LinkedHashMap<String[],String>();

    final String[] args =
    {
      "--hostname", "server.example.com",
      "--port", "80",
      "--authID", "admin",
      "--authPassword", "password",
      "--endpointURI", "/Users.xml",
      "--filter", "userName eq 'user.[1-1000000]'",
      "--attribute", "userName",
      "--attribute", "name",
      "--numThreads", "8"
    };
    final String description =
         "Test query performance by querying randomly across a set of one " +
         "million users with eight concurrent threads.  The user resources " +
         "returned to the client will be in XML form and will include the " +
         "userName and name attributes.";
    examples.put(args, description);

    return examples;
  }



  /**
   * Performs the actual processing for this tool.  In this case, it gets a
   * connection to the directory server and uses it to perform the requested
   * searches.
   *
   * @return  The result code for the processing that was performed.
   */
  @Override()
  public ResultCode doToolProcessing()
  {
    // Determine the random seed to use.
    final Long seed;
    if (randomSeed.isPresent())
    {
      seed = Long.valueOf(randomSeed.getValue());
    }
    else
    {
      seed = null;
    }

    // Create a value patterns for the filter.
    final ValuePattern filterPattern;
    try
    {
      // TODO provide seed after updating LDAP SDK
      filterPattern = new ValuePattern(filter.getValue());
    }
    catch (ParseException pe)
    {
      Debug.debugException(pe);
      err("Unable to parse the filter pattern:  ", pe.getMessage());
      return ResultCode.PARAM_ERROR;
    }

    // Get the attributes to return.
    final String[] attrs;
    if (attributes.isPresent())
    {
      final List<String> attrList = attributes.getValues();
      attrs = new String[attrList.size()];
      attrList.toArray(attrs);
    }
    else
    {
      attrs = NO_STRINGS;
    }


    // If the --ratePerSecond option was specified, then limit the rate
    // accordingly.
    FixedRateBarrier fixedRateBarrier = null;
    if (ratePerSecond.isPresent())
    {
      final int intervalSeconds = collectionInterval.getValue();
      final int ratePerInterval = ratePerSecond.getValue() * intervalSeconds;

      fixedRateBarrier =
           new FixedRateBarrier(1000L * intervalSeconds, ratePerInterval);
    }


    // Determine whether to include timestamps in the output and if so what
    // format should be used for them.
    final boolean includeTimestamp;
    final String timeFormat;
    if (timestampFormat.getValue().equalsIgnoreCase("with-date"))
    {
      includeTimestamp = true;
      timeFormat       = "dd/MM/yyyy HH:mm:ss";
    }
    else if (timestampFormat.getValue().equalsIgnoreCase("without-date"))
    {
      includeTimestamp = true;
      timeFormat       = "HH:mm:ss";
    }
    else
    {
      includeTimestamp = false;
      timeFormat       = null;
    }


    // Determine whether any warm-up intervals should be run.
    final long totalIntervals;
    final boolean warmUp;
    int remainingWarmUpIntervals = warmUpIntervals.getValue();
    if (remainingWarmUpIntervals > 0)
    {
      warmUp = true;
      totalIntervals = 0L + numIntervals.getValue() + remainingWarmUpIntervals;
    }
    else
    {
      warmUp = true;
      totalIntervals = 0L + numIntervals.getValue();
    }


    // Create the table that will be used to format the output.
    final OutputFormat outputFormat;
    if (csvFormat.isPresent())
    {
      outputFormat = OutputFormat.CSV;
    }
    else
    {
      outputFormat = OutputFormat.COLUMNS;
    }

    final ColumnFormatter formatter = new ColumnFormatter(includeTimestamp,
         timeFormat, outputFormat, " ",
         new FormattableColumn(15, HorizontalAlignment.RIGHT, "Recent",
                  "Queries/Sec"),
         new FormattableColumn(15, HorizontalAlignment.RIGHT, "Recent",
                  "Avg Dur ms"),
         new FormattableColumn(15, HorizontalAlignment.RIGHT, "Recent",
                  "Resources/Query"),
         new FormattableColumn(15, HorizontalAlignment.RIGHT, "Recent",
                  "Errors/Sec"),
         new FormattableColumn(15, HorizontalAlignment.RIGHT, "Overall",
                  "Queries/Sec"),
         new FormattableColumn(15, HorizontalAlignment.RIGHT, "Overall",
                  "Avg Dur ms"));


    // Create values to use for statistics collection.
    final AtomicLong        queryCounter    = new AtomicLong(0L);
    final AtomicLong        resourceCounter = new AtomicLong(0L);
    final AtomicLong        errorCounter    = new AtomicLong(0L);
    final AtomicLong        queryDurations  = new AtomicLong(0L);


    // Determine the length of each interval in milliseconds.
    final long intervalMillis = 1000L * collectionInterval.getValue();


    // Create the SCIM client to use for the queries.
    SCIMClient client = new SCIMClient(host.getValue(), port.getValue(), "");
    if (authID.isPresent())
    {
      try
      {
        final String password;
        if (authPassword.isPresent())
        {
          password = authPassword.getValue();
        }
        else if (authPasswordFile.isPresent())
        {
          password = authPasswordFile.getNonBlankFileLines().get(0);
        }
        else
        {
          password = null;
        }

        client.setBasicAuth(authID.getValue(), password);
      }
      catch (IOException e)
      {
        Debug.debugException(e);
        err("Unable to set basic authentication:  ", e.getMessage());
        return ResultCode.LOCAL_ERROR;
      }
    }

    try
    {
      client.startClient();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
      err("Unable to start the SCIM client:  ", e.getMessage());
      return ResultCode.LOCAL_ERROR;
    }

    // Check that a connection can be established.
    try
    {
      client.getResources(resourceName.getValue(),
                          endpointURI.getValue(),
                          "userName eq 'no-user'",
                          null, null, attrs);
    }
    catch (IOException e)
    {
      Debug.debugException(e);
      err("Unable to connect to the server:  ", e.getMessage());
      return ResultCode.CONNECT_ERROR;
    }

    // Create the threads to use for the searches.
    final CyclicBarrier barrier = new CyclicBarrier(numThreads.getValue() + 1);
    final QueryRateThread[] threads =
         new QueryRateThread[numThreads.getValue()];
    for (int i=0; i < threads.length; i++)
    {
      threads[i] =
          new QueryRateThread(i, client,
                              resourceName.getValue(), endpointURI.getValue(),
                              filterPattern, attrs, barrier, queryCounter,
                              resourceCounter, queryDurations, errorCounter,
                              fixedRateBarrier);
      threads[i].start();
    }


    // Display the table header.
    for (final String headerLine : formatter.getHeaderLines(true))
    {
      out(headerLine);
    }


    // Indicate that the threads can start running.
    try
    {
      barrier.await();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
    }
    long overallStartTime = System.nanoTime();
    long nextIntervalStartTime = System.currentTimeMillis() + intervalMillis;


    boolean setOverallStartTime = false;
    long    lastDuration        = 0L;
    long    lastNumEntries      = 0L;
    long    lastNumErrors       = 0L;
    long    lastNumSearches     = 0L;
    long    lastEndTime         = System.nanoTime();
    for (long i=0; i < totalIntervals; i++)
    {
      final long startTimeMillis = System.currentTimeMillis();
      final long sleepTimeMillis = nextIntervalStartTime - startTimeMillis;
      nextIntervalStartTime += intervalMillis;
      try
      {
        if (sleepTimeMillis > 0)
        {
          Thread.sleep(sleepTimeMillis);
        }
      }
      catch (Exception e)
      {
        Debug.debugException(e);
      }

      final long endTime          = System.nanoTime();
      final long intervalDuration = endTime - lastEndTime;

      final long numSearches;
      final long numEntries;
      final long numErrors;
      final long totalDuration;
      if (warmUp && (remainingWarmUpIntervals > 0))
      {
        numSearches   = queryCounter.getAndSet(0L);
        numEntries    = resourceCounter.getAndSet(0L);
        numErrors     = errorCounter.getAndSet(0L);
        totalDuration = queryDurations.getAndSet(0L);
      }
      else
      {
        numSearches   = queryCounter.get();
        numEntries    = resourceCounter.get();
        numErrors     = errorCounter.get();
        totalDuration = queryDurations.get();
      }

      final long recentNumSearches = numSearches - lastNumSearches;
      final long recentNumEntries = numEntries - lastNumEntries;
      final long recentNumErrors = numErrors - lastNumErrors;
      final long recentDuration = totalDuration - lastDuration;

      final double numSeconds = intervalDuration / 1000000000.0d;
      final double recentSearchRate = recentNumSearches / numSeconds;
      final double recentErrorRate  = recentNumErrors / numSeconds;

      final double recentAvgDuration;
      final double recentEntriesPerSearch;
      if (recentNumSearches > 0L)
      {
        recentEntriesPerSearch = 1.0d * recentNumEntries / recentNumSearches;
        recentAvgDuration = 1.0d * recentDuration / recentNumSearches / 1000000;
      }
      else
      {
        recentEntriesPerSearch = 0.0d;
        recentAvgDuration = 0.0d;
      }


      if (warmUp && (remainingWarmUpIntervals > 0))
      {
        out(formatter.formatRow(recentSearchRate, recentAvgDuration,
             recentEntriesPerSearch, recentErrorRate, "warming up",
             "warming up"));

        remainingWarmUpIntervals--;
        if (remainingWarmUpIntervals == 0)
        {
          out("Warm-up completed.  Beginning overall statistics collection.");
          setOverallStartTime = true;
        }
      }
      else
      {
        if (setOverallStartTime)
        {
          overallStartTime    = lastEndTime;
          setOverallStartTime = false;
        }

        final double numOverallSeconds =
             (endTime - overallStartTime) / 1000000000.0d;
        final double overallSearchRate = numSearches / numOverallSeconds;

        final double overallAvgDuration;
        if (numSearches > 0L)
        {
          overallAvgDuration = 1.0d * totalDuration / numSearches / 1000000;
        }
        else
        {
          overallAvgDuration = 0.0d;
        }

        out(formatter.formatRow(recentSearchRate, recentAvgDuration,
             recentEntriesPerSearch, recentErrorRate, overallSearchRate,
             overallAvgDuration));

        lastNumSearches = numSearches;
        lastNumEntries  = numEntries;
        lastNumErrors   = numErrors;
        lastDuration    = totalDuration;
      }

      lastEndTime = endTime;
    }


    // Stop all of the threads.
    ResultCode resultCode = ResultCode.SUCCESS;
    for (final QueryRateThread t : threads)
    {
      t.signalShutdown();
    }
    for (final QueryRateThread t : threads)
    {
      final ResultCode r = t.waitForShutdown();
      if (resultCode == ResultCode.SUCCESS)
      {
        resultCode = r;
      }
    }

    try
    {
      client.stopClient();
    }
    catch (Exception e)
    {
      Debug.debugException(e);
    }

    return resultCode;
  }
}
