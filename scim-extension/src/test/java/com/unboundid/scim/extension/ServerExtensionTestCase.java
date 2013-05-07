/*
 * Copyright 2011-2013 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import com.unboundid.directory.tests.externalinstance.BaseTestCase;
import com.unboundid.directory.tests.externalinstance.ExternalInstance;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.scim.sdk.PreemptiveAuthInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.wink.client.httpclient.ApacheHttpClientConfig;
import org.apache.wink.client.ClientConfig;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import javax.net.ssl.SSLContext;

import java.io.File;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;




/**
 * Base class for all scim-extension test cases.
 */
public class ServerExtensionTestCase extends BaseTestCase
{
  /**
   * Configure the SCIM extension.
   *
   * @param instance          The external instance where the extension is to be
   *                          configured.
   * @param listenPort        The HTTP listen port.
   * @param secureListenPort  The HTTPS listen port.
   *
   * @throws Exception  If the extension cannot be configured.
   */
  protected static void configureExtension(final ExternalInstance instance,
                                           final int listenPort,
                                           final int secureListenPort)
      throws Exception
  {
    instance.dsconfig(
        "set-log-publisher-prop",
        "--publisher-name", "Server SDK Extension Debug Logger",
        "--set", "enabled:true");

    instance.dsconfig(
        "create-http-servlet-extension",
        "--extension-name", "SCIM",
        "--type", "third-party",
        "--set", "extension-class:" +
                 "com.unboundid.scim.extension.SCIMServletExtension",
        "--set", "extension-argument:" +
                 "resourceMappingFile=extensions/" +
                 "com.unboundid.scim-extension/config/resources.xml",
        "--set", "extension-argument:contextPath=/",
        "--set", "extension-argument:debugEnabled"
        );

    instance.dsconfig(
        "create-log-publisher",
        "--publisher-name", "HTTP Common Access",
        "--type", "common-log-file-http-operation",
        "--set", "enabled:true",
        "--set", "log-file:logs/http-common-access",
        "--set", "rotation-policy:24 Hours Time Limit Rotation Policy",
        "--set", "rotation-policy:Size Limit Rotation Policy",
        "--set", "retention-policy:File Count Retention Policy",
        "--set", "retention-policy:Free Disk Space Retention Policy");

    instance.dsconfig(
        "create-log-publisher",
        "--publisher-name", "HTTP Detailed Access",
        "--type", "detailed-http-operation",
        "--set", "enabled:true",
        "--set", "log-file:logs/http-detailed-access",
        "--set", "log-request-headers:header-names-and-values",
        "--set", "log-response-headers:header-names-and-values",
        "--set", "log-request-parameters:parameter-names-and-values",
        "--set", "rotation-policy:24 Hours Time Limit Rotation Policy",
        "--set", "rotation-policy:Size Limit Rotation Policy",
        "--set", "retention-policy:File Count Retention Policy",
        "--set", "retention-policy:Free Disk Space Retention Policy");

    instance.dsconfig(
        "create-connection-handler",
        "--handler-name", "HTTP",
        "--type", "http",
        "--set", "enabled:true",
        "--set", "http-servlet-extension:SCIM",
        "--set", "http-operation-log-publisher:HTTP Detailed Access",
        "--set", "listen-port:" + listenPort);

    instance.dsconfig(
        "create-connection-handler",
        "--handler-name", "HTTPS",
        "--type", "http",
        "--set", "enabled:true",
        "--set", "http-servlet-extension:" + "SCIM",
        "--set", "http-operation-log-publisher:HTTP Detailed Access",
        "--set", "listen-port:" + secureListenPort,
        "--set", "use-ssl:true",
        "--set", "key-manager-provider:JKS",
        "--set", "trust-manager-provider:JKS");

    // Verify that the tmpDataDir has been created with the correct permissions.
    if (OperatingSystem.isUNIXBased(OperatingSystem.local()))
    {
      final File tmpDataDir =
          new File(instance.getInstanceRoot(),
                   "extensions/com.unboundid.scim-extension/tmp-data");
      final List<String> output = new ArrayList<String>();
      final int rc = FilePermission.exec(
          "ls",
          new String[] { "-ld", tmpDataDir.getAbsolutePath() },
          null, null, output);
      assertEquals(rc, 0);
      assertTrue(output.get(0).startsWith("drwx------"));
    }
  }



  /**
   * Create an SSL-enabled Wink client config that can connect to the provided
   * external instance using the Directory Manager and Password as the HTTP
   * Basic Auth credentials. The returned client config may be used to create
   * a SCIM service object.
   *
   * @param instance    The external instance the client will connect to.
   * @param sslContext  The SSL context to be used to create SSL sockets.
   *
   * @return  An Apache Wink client.
   */
  protected ClientConfig createManagerClientConfig(
      final ExternalInstance instance, final SSLContext sslContext)
  {
    return createClientConfig(instance.getLdapHost(),
                              instance.getDirmanagerDN(),
                              instance.getDirmanagerPassword(),
                              sslContext);
  }



  /**
   * Create an SSL-enabled Wink client config from the provided information.
   * The returned client config may be used to create a SCIM service object.
   *
   * @param host        The host name the client will connect to.
   * @param userName    The HTTP Basic Auth user name.
   * @param password    The HTTP Basic Auth password.
   * @param sslContext  The SSL context to be used to create SSL sockets.
   *
   * @return  An Apache Wink client.
   */
  protected ClientConfig createClientConfig(
      final String host, final String userName, final String password,
      final SSLContext sslContext)
  {
    SSLSocketFactory sslSocketFactory;
    try
    {
      sslSocketFactory = new SSLSocketFactory(
        new TrustStrategy()
        {
          public boolean isTrusted(final X509Certificate[] chain,
                                   final String authType)
            throws CertificateException
          {
            return true;
          }
        },
        SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }
    catch (final Exception e)
    {
      sslSocketFactory = new SSLSocketFactory(sslContext);
    }

    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    httpClientBuilder.setRetryHandler(new StandardHttpRequestRetryHandler());
    httpClientBuilder.setServiceUnavailableRetryStrategy(
            new DefaultServiceUnavailableRetryStrategy());

    SocketConfig.Builder socketConfig = SocketConfig.custom();
    socketConfig.setSoKeepAlive(true);
    socketConfig.setSoReuseAddress(true);
    socketConfig.setTcpNoDelay(true);
    socketConfig.setSoTimeout(10000);
    httpClientBuilder.setDefaultSocketConfig(socketConfig.build());

    RequestConfig.Builder requestConfig = RequestConfig.custom();
    requestConfig.setAuthenticationEnabled(true);
    requestConfig.setStaleConnectionCheckEnabled(true);
    requestConfig.setConnectTimeout(10000);
    requestConfig.setConnectionRequestTimeout(30000);
    httpClientBuilder.setDefaultRequestConfig(requestConfig.build());

    RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistry =
            RegistryBuilder.create();
    socketFactoryRegistry.register("http", PlainSocketFactory.INSTANCE);
    socketFactoryRegistry.register("https", sslSocketFactory);

    final PoolingHttpClientConnectionManager mgr =
          new PoolingHttpClientConnectionManager(socketFactoryRegistry.build());
    mgr.setDefaultSocketConfig(socketConfig.build());
    mgr.setDefaultMaxPerRoute(20);
    mgr.setMaxTotal(200);
    httpClientBuilder.setConnectionManager(mgr);

    final CredentialsProvider credentialsProvider =
            new BasicCredentialsProvider();
    final Credentials credentials =
            new UsernamePasswordCredentials(userName, password);
    credentialsProvider.setCredentials(AuthScope.ANY, credentials);
    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
    httpClientBuilder.addInterceptorFirst(new PreemptiveAuthInterceptor());

    return new ApacheHttpClientConfig(httpClientBuilder.build());
  }



  /**
   * Get a SCIM monitor attribute as a string.
   *
   * @param instance   The external instance containing the monitor data.
   * @param attribute  The desired monitor attribute.
   * @return  The value of the SCIM monitor attribute as a string.
   * @throws LDAPException  If the SCIM monitor entry could not be read.
   */
  protected String getMonitorAsString(final ExternalInstance instance,
                                      final String attribute)
      throws LDAPException
  {
    return getMonitorAsString(instance, "HTTP", attribute);
  }



  /**
   * Get a SCIM monitor attribute as a string.
   *
   * @param instance   The external instance containing the monitor data.
   * @param connectionHandler   The name of the connection handler.
   * @param attribute  The desired monitor attribute.
   * @return  The value of the SCIM monitor attribute as a string.
   * @throws LDAPException  If the SCIM monitor entry could not be read.
   */
  protected String getMonitorAsString(final ExternalInstance instance,
                                      final String connectionHandler,
                                      final String attribute)
      throws LDAPException
  {
    final String dn = getMonitorEntryDN(connectionHandler);
    final Entry entry = instance.readEntry(dn);
    return entry.getAttributeValue(attribute);
  }



  /**
   * Get the SCIM monitor entry DN.
   *
   * @param connectionHandler  The name of the connection handler.
   * @return The DN of the SCIM Servlet monitor entry.
   */
  protected String getMonitorEntryDN (final String connectionHandler)
  {
    final StringBuilder builder = new StringBuilder();
    builder.append("cn=SCIM Servlet (");
    builder.append(connectionHandler);
    builder.append(") [from ThirdPartyHTTPServletExtension:SCIM],cn=monitor");
    return builder.toString();
  }
}
