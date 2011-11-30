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

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.Debug;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMService;
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
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.PromptTrustManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.wink.client.ApacheHttpClientConfig;

import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.unboundid.scim.sdk.Debug.debugException;
import static com.unboundid.util.StaticUtils.NO_STRINGS;
import static com.unboundid.scim.ri.RIMessages.*;
import static com.unboundid.util.StaticUtils.getExceptionMessage;
import static org.apache.http.params.CoreConnectionPNames.SO_REUSEADDR;

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
 *   <LI>"-x" or "--xml" -- Specifies XML format in requests rather than
 *       JSON format.</LI>
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
  private BooleanArgument trustAll;
  private BooleanArgument useSSL;
  private FileArgument    keyStorePasswordFile;
  private FileArgument    trustStorePasswordFile;
  private StringArgument  certificateNickname;
  private StringArgument  keyStoreFormat;
  private StringArgument  keyStorePath;
  private StringArgument  keyStorePassword;
  private StringArgument  trustStoreFormat;
  private StringArgument  trustStorePath;
  private StringArgument  trustStorePassword;

  // The argument used to indicate whether to generate output in CSV format.
  private BooleanArgument csvFormat;

  // The argument used to indicate whether to use XML format in requests rather
  // than JSON format.
  private BooleanArgument xmlFormat;

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

  // The argument used to specify the filters for the queries.
  private StringArgument filter;

  // The argument used to specify the name of resources to be queried.
  private StringArgument resourceName;

  // The argument used to specify the timestamp format.
  private StringArgument timestampFormat;

  // The prompt trust manager that will be shared by all connections created
  // for which it is appropriate.  This will allow them to benefit from the
  // common cache.
  private final AtomicReference<PromptTrustManager> promptTrustManager =
      new AtomicReference<PromptTrustManager>();


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
    return INFO_QUERY_TOOL_DESC.get();
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public void addToolArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    host = new StringArgument(
        'h', "hostname", true, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_HOSTNAME.get(),
        INFO_QUERY_TOOL_ARG_DESC_HOSTNAME.get(),
        "localhost");
    parser.addArgument(host);


    port = new IntegerArgument(
        'p', "port", true, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_PORT.get(),
        INFO_QUERY_TOOL_ARG_DESC_PORT.get(),
        1, 65535, 80);
    parser.addArgument(port);


    authID = new StringArgument(
        null, "authID", false, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_AUTHID.get(),
        INFO_QUERY_TOOL_ARG_DESC_AUTHID.get());
    parser.addArgument(authID);


    authPassword = new StringArgument(
        'w', "authPassword", false, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_AUTH_PASSWORD.get(),
        INFO_QUERY_TOOL_ARG_DESC_AUTH_PASSWORD.get());
    parser.addArgument(authPassword);


    authPasswordFile = new FileArgument(
        'j', "authPasswordFile", false, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_AUTH_PASSWORD_FILE.get(),
        INFO_QUERY_TOOL_ARG_DESC_AUTH_PASSWORD_FILE.get(),
        true, true, true, false);
    parser.addArgument(authPasswordFile);


    resourceName = new StringArgument(
        null, "resourceName", false, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_RESOURCE_NAME.get(),
        INFO_QUERY_TOOL_ARG_DESC_RESOURCE_NAME.get(),
        null, Arrays.asList("User"));
    parser.addArgument(resourceName);


    xmlFormat = new BooleanArgument(
        'x', "xml", 1,
        INFO_QUERY_TOOL_ARG_DESC_XML_FORMAT.get());
    parser.addArgument(xmlFormat);

    filter = new StringArgument(
        'f', "filter", false, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_FILTER.get(),
        INFO_QUERY_TOOL_ARG_DESC_FILTER.get());
    parser.addArgument(filter);


    attributes = new StringArgument(
        'A', "attribute", false, 0,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_ATTRIBUTE.get(),
        INFO_QUERY_TOOL_ARG_DESC_ATTRIBUTE.get());
    parser.addArgument(attributes);


    numThreads = new IntegerArgument(
        't', "numThreads", true, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_NUM_THREADS.get(),
        INFO_QUERY_TOOL_ARG_DESC_NUM_THREADS.get(),
        1, Integer.MAX_VALUE, 1);
    parser.addArgument(numThreads);


    collectionInterval = new IntegerArgument(
        'i', "intervalDuration", true, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_INTERVAL_DURATION.get(),
        INFO_QUERY_TOOL_ARG_DESC_INTERVAL_DURATION.get(), 1,
        Integer.MAX_VALUE, 5);
    parser.addArgument(collectionInterval);


    numIntervals = new IntegerArgument(
        'I', "numIntervals", true, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_NUM_INTERVALS.get(),
        INFO_QUERY_TOOL_ARG_DESC_NUM_INTERVALS.get(),
        1, Integer.MAX_VALUE,
        Integer.MAX_VALUE);
    parser.addArgument(numIntervals);

    ratePerSecond = new IntegerArgument(
        'r', "ratePerSecond", false, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_RATE_PER_SECOND.get(),
        INFO_QUERY_TOOL_ARG_DESC_RATE_PER_SECOND.get(),
        1, Integer.MAX_VALUE);
    parser.addArgument(ratePerSecond);

    warmUpIntervals = new IntegerArgument(
        null, "warmUpIntervals", true, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_WARM_UP_INTERVALS.get(),
        INFO_QUERY_TOOL_ARG_DESC_WARM_UP_INTERVALS.get(),
        0, Integer.MAX_VALUE, 0);
    parser.addArgument(warmUpIntervals);

    final LinkedHashSet<String> allowedFormats = new LinkedHashSet<String>(3);
    allowedFormats.add("none");
    allowedFormats.add("with-date");
    allowedFormats.add("without-date");
    timestampFormat = new StringArgument(
        null, "timestampFormat", true, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_TIMESTAMP_FORMAT.get(),
        INFO_QUERY_TOOL_ARG_DESC_TIMESTAMP_FORMAT.get(),
        allowedFormats, "none");
    parser.addArgument(timestampFormat);

    csvFormat = new BooleanArgument(
        'c', "csv", 1,
        INFO_QUERY_TOOL_ARG_DESC_CSV_FORMAT.get());
    parser.addArgument(csvFormat);

    randomSeed = new IntegerArgument(
        'R', "randomSeed", false, 1,
        INFO_QUERY_TOOL_ARG_PLACEHOLDER_RANDOM_SEED.get(),
        INFO_QUERY_TOOL_ARG_DESC_RANDOM_SEED.get());
    parser.addArgument(randomSeed);

    useSSL = new BooleanArgument('Z', "useSSL", 1,
         INFO_SCIM_TOOL_DESCRIPTION_USE_SSL.get());
    parser.addArgument(useSSL);

    trustAll = new BooleanArgument('X', "trustAll", 1,
         INFO_SCIM_TOOL_DESCRIPTION_TRUST_ALL.get());
    parser.addArgument(trustAll);

    keyStorePath = new StringArgument('K', "keyStorePath", false, 1,
         INFO_SCIM_TOOL_PLACEHOLDER_PATH.get(),
         INFO_SCIM_TOOL_DESCRIPTION_KEY_STORE_PATH.get());
    parser.addArgument(keyStorePath);

    keyStorePassword = new StringArgument('W', "keyStorePassword", false, 1,
         INFO_SCIM_TOOL_PLACEHOLDER_PASSWORD.get(),
         INFO_SCIM_TOOL_DESCRIPTION_KEY_STORE_PASSWORD.get());
    parser.addArgument(keyStorePassword);

    keyStorePasswordFile = new FileArgument('u', "keyStorePasswordFile", false,
         1, INFO_SCIM_TOOL_PLACEHOLDER_PATH.get(),
         INFO_SCIM_TOOL_DESCRIPTION_KEY_STORE_PASSWORD_FILE.get());
    parser.addArgument(keyStorePasswordFile);

    keyStoreFormat = new StringArgument(null, "keyStoreFormat", false, 1,
         INFO_SCIM_TOOL_PLACEHOLDER_FORMAT.get(),
         INFO_SCIM_TOOL_DESCRIPTION_KEY_STORE_FORMAT.get());
    parser.addArgument(keyStoreFormat);

    trustStorePath = new StringArgument('P', "trustStorePath", false, 1,
         INFO_SCIM_TOOL_PLACEHOLDER_PATH.get(),
         INFO_SCIM_TOOL_DESCRIPTION_TRUST_STORE_PATH.get());
    parser.addArgument(trustStorePath);

    trustStorePassword = new StringArgument('T', "trustStorePassword", false, 1,
         INFO_SCIM_TOOL_PLACEHOLDER_PASSWORD.get(),
         INFO_SCIM_TOOL_DESCRIPTION_TRUST_STORE_PASSWORD.get());
    parser.addArgument(trustStorePassword);

    trustStorePasswordFile = new FileArgument('U', "trustStorePasswordFile",
         false, 1, INFO_SCIM_TOOL_PLACEHOLDER_PATH.get(),
         INFO_SCIM_TOOL_DESCRIPTION_TRUST_STORE_PASSWORD_FILE.get());
    parser.addArgument(trustStorePasswordFile);

    trustStoreFormat = new StringArgument(null, "trustStoreFormat", false, 1,
         INFO_SCIM_TOOL_PLACEHOLDER_FORMAT.get(),
         INFO_SCIM_TOOL_DESCRIPTION_TRUST_STORE_FORMAT.get());
    parser.addArgument(trustStoreFormat);

    certificateNickname = new StringArgument('N', "certNickname", false, 1,
         INFO_SCIM_TOOL_PLACEHOLDER_CERT_NICKNAME.get(),
         INFO_SCIM_TOOL_DESCRIPTION_CERT_NICKNAME.get());
    parser.addArgument(certificateNickname);

    parser.addDependentArgumentSet(authID, authPassword, authPasswordFile);
    parser.addExclusiveArgumentSet(authPassword, authPasswordFile);
    parser.addExclusiveArgumentSet(keyStorePassword, keyStorePasswordFile);
    parser.addExclusiveArgumentSet(trustStorePassword, trustStorePasswordFile);
    parser.addExclusiveArgumentSet(trustAll, trustStorePath);
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
      "--xml",
      "--filter", "userName eq 'user.[1-1000000]'",
      "--attribute", "userName",
      "--attribute", "name",
      "--numThreads", "8"
    };
    examples.put(args, INFO_QUERY_TOOL_EXAMPLE_1.get());

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

    // Create a value pattern for the filter.
    final ValuePattern filterPattern;
    if (filter.isPresent())
    {
      try
      {
        filterPattern = new ValuePattern(filter.getValue(), seed);
      }
      catch (ParseException pe)
      {
        Debug.debugException(pe);
        err(ERR_QUERY_TOOL_BAD_FILTER_PATTERN.get(pe.getMessage()));
        return ResultCode.PARAM_ERROR;
      }
    }
    else
    {
      filterPattern = null;
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


    // We will use Apache's HttpClient library for this tool.
    final HttpParams params = new BasicHttpParams();
    params.setBooleanParameter(SO_REUSEADDR, true);
    DefaultHttpClient.setDefaultHttpParams(params);

    SSLUtil sslUtil;
    try
    {
      sslUtil = createSSLUtil();
    }
    catch (LDAPException e)
    {
      debugException(e);
      err(e.getMessage());
      return e.getResultCode();
    }

    final SchemeRegistry schemeRegistry = new SchemeRegistry();
    final String schemeName;
    if (sslUtil != null)
    {
      final SSLSocketFactory socketFactory;
      try
      {
        socketFactory = new SSLSocketFactory(sslUtil.createSSLContext());
      }
      catch (GeneralSecurityException e)
      {
        debugException(e);
        err(ERR_SCIM_TOOL_CANNOT_CREATE_SSL_CONTEXT.get(
            getExceptionMessage(e)));
        return ResultCode.LOCAL_ERROR;
      }
      schemeName = "https";
      final Scheme scheme = new Scheme(schemeName, 443, socketFactory);
      schemeRegistry.register(scheme);
    }
    else
    {
      schemeName = "http";
      final Scheme scheme =
          new Scheme(schemeName, 80, PlainSocketFactory.getSocketFactory());
      schemeRegistry.register(scheme);
    }

    final ThreadSafeClientConnManager mgr =
        new ThreadSafeClientConnManager(schemeRegistry);
    mgr.setMaxTotal(numThreads.getValue());

    final DefaultHttpClient httpClient = new DefaultHttpClient(mgr, params);

    final ApacheHttpClientConfig clientConfig =
        new ApacheHttpClientConfig(httpClient);
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
        httpClient.getCredentialsProvider().setCredentials(
            new AuthScope(host.getValue(), port.getValue()),
            new UsernamePasswordCredentials(authID.getValue(), password));
      }
      catch (IOException e)
      {
        Debug.debugException(e);
        err(ERR_QUERY_TOOL_SET_BASIC_AUTH.get(e.getMessage()));
        return ResultCode.LOCAL_ERROR;
      }
    }

    // Create the SCIM client to use for the queries.
    final URI uri =
        URI.create(schemeName + "://"+ host.getValue() + ":" + port.getValue());
    final SCIMService service = new SCIMService(uri, clientConfig);

    if (xmlFormat.isPresent())
    {
      service.setContentType(MediaType.APPLICATION_XML_TYPE);
      service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    }

    // Retrieve the resource schema.
    final ResourceDescriptor resourceDescriptor;
    try
    {
      resourceDescriptor =
        service.getResourceDescriptor(resourceName.getValue(), null);
      if(resourceDescriptor == null)
      {
        throw new ResourceNotFoundException("Resource " +
            resourceName.getValue() +
            " is not defined by the service provider");
      }
    }
    catch (SCIMException e)
    {
      Debug.debugException(e);
      err(ERR_QUERY_TOOL_RETRIEVE_RESOURCE_SCHEMA.get(e.getMessage()));
      return ResultCode.OTHER;
    }

    final SCIMEndpoint<? extends BaseResource> endpoint =
        service.getEndpoint(resourceDescriptor,
            BaseResource.BASE_RESOURCE_FACTORY);

    // Create the threads to use for the searches.
    final CyclicBarrier barrier = new CyclicBarrier(numThreads.getValue() + 1);
    final QueryRateThread[] threads =
         new QueryRateThread[numThreads.getValue()];
    for (int i=0; i < threads.length; i++)
    {
      threads[i] =
          new QueryRateThread(i, endpoint, filterPattern, attrs, barrier,
              queryCounter, resourceCounter, queryDurations, errorCounter,
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
          out(INFO_QUERY_TOOL_WARM_UP_COMPLETED.get());
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

    return resultCode;
  }



  /**
   * Creates the SSLUtil instance to use for secure communication.
   *
   * @return  The SSLUtil instance to use for secure communication, or
   *          {@code null} if secure communication is not needed.
   *
   * @throws LDAPException   If a problem occurs while creating the SSLUtil
   *                         instance.
   */
  private SSLUtil createSSLUtil()
          throws LDAPException
  {
    if (useSSL.isPresent())
    {
      KeyManager keyManager = null;
      if (keyStorePath.isPresent())
      {
        char[] pw = null;
        if (keyStorePassword.isPresent())
        {
          pw = keyStorePassword.getValue().toCharArray();
        }
        else if (keyStorePasswordFile.isPresent())
        {
          try
          {
            pw = keyStorePasswordFile.getNonBlankFileLines().get(0).
                      toCharArray();
          }
          catch (Exception e)
          {
            Debug.debugException(e);
            throw new LDAPException(ResultCode.LOCAL_ERROR,
                 ERR_SCIM_TOOL_CANNOT_READ_KEY_STORE_PASSWORD.get(
                      getExceptionMessage(e)), e);
          }
        }

        try
        {
          keyManager = new KeyStoreKeyManager(keyStorePath.getValue(), pw,
               keyStoreFormat.getValue(), certificateNickname.getValue());
        }
        catch (Exception e)
        {
          Debug.debugException(e);
          throw new LDAPException(ResultCode.LOCAL_ERROR,
               ERR_SCIM_TOOL_CANNOT_CREATE_KEY_MANAGER.get(
                    getExceptionMessage(e)), e);
        }
      }

      TrustManager trustManager;
      if (trustAll.isPresent())
      {
        trustManager = new TrustAllTrustManager(false);
      }
      else if (trustStorePath.isPresent())
      {
        char[] pw = null;
        if (trustStorePassword.isPresent())
        {
          pw = trustStorePassword.getValue().toCharArray();
        }
        else if (trustStorePasswordFile.isPresent())
        {
          try
          {
            pw = trustStorePasswordFile.getNonBlankFileLines().get(0).
                      toCharArray();
          }
          catch (Exception e)
          {
            Debug.debugException(e);
            throw new LDAPException(ResultCode.LOCAL_ERROR,
                 ERR_SCIM_TOOL_CANNOT_READ_TRUST_STORE_PASSWORD.get(
                      getExceptionMessage(e)), e);
          }
        }

        trustManager = new TrustStoreTrustManager(trustStorePath.getValue(), pw,
             trustStoreFormat.getValue(), true);
      }
      else
      {
        trustManager = promptTrustManager.get();
        if (trustManager == null)
        {
          final PromptTrustManager m = new PromptTrustManager();
          promptTrustManager.compareAndSet(null, m);
          trustManager = promptTrustManager.get();
        }
      }

      return new SSLUtil(keyManager, trustManager);
    }
    else
    {
      return null;
    }
  }



}
