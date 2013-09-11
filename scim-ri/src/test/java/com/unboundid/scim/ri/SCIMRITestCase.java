/*
 * Copyright 2011-2013 UnboundID Corp.
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



import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPRequest;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.ldap.sdk.schema.Schema;
import com.unboundid.scim.SCIMTestCase;
import com.unboundid.scim.ldap.LDAPBackend;
import com.unboundid.scim.sdk.PreemptiveAuthInterceptor;
import com.unboundid.scim.sdk.SCIMService;
import com.unboundid.scim.wink.ResourceStats;
import com.unboundid.scim.wink.SCIMApplication;
import com.unboundid.util.LDAPTestUtils;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.wink.client.httpclient.ApacheHttpClientConfig;
import org.apache.wink.client.ClientConfig;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import javax.ws.rs.core.UriBuilder;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.List;

/**
 * This class provides the superclass for all SCIM RI test cases.
 */
@Test(sequential=true)
public abstract class SCIMRITestCase extends SCIMTestCase
{
  // An in-memory directory server instance that can be used for testing.
  private static volatile InMemoryDirectoryServer testDS = null;

  // A SCIM server that can be used for testing.
  private static volatile SCIMServer testSS;

  private static volatile SCIMApplication scimApplication;

  // The port number of the SCIM server instance.
  private static int ssPort = -1;

  // The LDAP SCIM backend.
  private static LDAPBackend ldapBackend;

  /**
   * The SCIM service to be used to access the server.
   */
  protected static SCIMService service;

  /**
   * The base DN for groups.
   */
  protected final String groupBaseDN = "dc=example,dc=com";

  /**
   * The base DN for users.
   */
  protected final String userBaseDN = "ou=people,dc=example,dc=com";


  /**
   * Creates the in-memory directory server instance that can be used for
   * testing.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @BeforeSuite()
  public synchronized void setUpTestSuite()
         throws Exception
  {
    if (testDS != null)
    {
      return;
    }

    final InMemoryDirectoryServerConfig cfg =
         new InMemoryDirectoryServerConfig("dc=example,dc=com",
              "o=example.com");
    cfg.addAdditionalBindCredentials("cn=Directory Manager", "password");
    cfg.addAdditionalBindCredentials("cn=Manager", "password");
    cfg.setSchema(Schema.getDefaultStandardSchema());
    cfg.setListenerExceptionHandler(
         new StandardErrorListenerExceptionHandler());

    final File resourceDir = new File(System.getProperty("unit.resource.dir"));

    final File serverKeyStore   = new File(resourceDir, "server.keystore");

    final SSLUtil serverSSLUtil = new SSLUtil(
         new KeyStoreKeyManager(serverKeyStore, "password".toCharArray(),
              "JKS", "server-cert"),
         new TrustAllTrustManager());
    final SSLUtil clientSSLUtil = new SSLUtil(new TrustAllTrustManager());

    cfg.setListenerConfigs(
        InMemoryListenerConfig.createLDAPConfig("LDAP", 0),
        InMemoryListenerConfig.createLDAPSConfig(
            "LDAPS",
            null, 0, serverSSLUtil.createSSLServerSocketFactory(),
            clientSSLUtil.createSSLSocketFactory()));

    testDS = new InMemoryDirectoryServer(cfg);
    testDS.startListening();

    SCIMServerConfig config = new SCIMServerConfig();
    config.setResourcesFile(getFile("resource/resources.xml"));
    reconfigureTestSuite(config);
  }



  /**
   * Cleans up after all tests in the suite have completed.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  @AfterSuite()
  public synchronized void cleanUpTestSuite()
         throws Exception
  {
    if (testSS != null)
    {
      testSS.shutdown();
      testSS = null;
      scimApplication = null;
    }

    ldapBackend.finalizeBackend();

    if (testDS != null)
    {
      testDS.shutDown(true);
      testDS = null;
    }
  }



  /**
   * Configures the SCIMServer to use the given resource mapping file.
   *
   * @param ssConfig the SCIM Server config.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  protected synchronized void reconfigureTestSuite(
      SCIMServerConfig ssConfig) throws Exception
  {
    if (testSS != null)
    {
      testSS.shutdown();
    }

    if(ldapBackend != null)
    {
      ldapBackend.finalizeBackend();
    }

    ssPort = getFreePort();
    ssConfig.setListenPort(ssPort);
    ssConfig.setMaxThreads(16);
    if(ssConfig.getResourcesFile() == null)
    {
      ssConfig.setResourcesFile(getFile("resource/resources.xml"));
    }

    final LDAPExternalServerConfig ldapConfig =
        new LDAPExternalServerConfig();
    ldapConfig.setDsHost("localhost");
    ldapConfig.setDsPort(testDS.getListenPort("LDAP"));
    ldapConfig.setDsBaseDN("dc=example,dc=com");
    ldapConfig.setDsBindDN("cn=Directory Manager");
    ldapConfig.setDsBindPassword("password");
    ldapConfig.setNumConnections(16);

    testSS = SCIMServer.getInstance();
    testSS.initializeServer(ssConfig);

    ldapBackend = new ExternalLDAPBackend(testSS.getResourceMappers(),
                                          ldapConfig);
    ldapBackend.getConfig().setMaxResults(ssConfig.getMaxResults());
    ldapBackend.setSupportsPostReadRequestControl(true);
    ldapBackend.setSupportsVLVRequestControl(true);

    scimApplication = testSS.registerBackend("/", ldapBackend);
    testSS.startListening();

    // Start a client for the SCIM operations.
    service = createSCIMService("cn=Manager", "password");
  }



  /**
   * Create an Wink client config from the provided information.
   * The returned client config may be used to create a SCIM service object.
   *
   * @param userName    The HTTP Basic Auth user name.
   * @param password    The HTTP Basic Auth password.
   *
   * @return  An Apache Wink client.
   */
  protected SCIMService createSCIMService(final String userName,
                                          final String password)
  {
    final HttpParams params = new BasicHttpParams();
    DefaultHttpClient.setDefaultHttpParams(params);
    params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 30000);
    params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 30000);
    params.setBooleanParameter(CoreConnectionPNames.SO_REUSEADDR, true);
    params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
    params.setBooleanParameter(
            CoreConnectionPNames.STALE_CONNECTION_CHECK, true);
    params.setParameter(
            ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);

    final SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme(
            "http", 80, PlainSocketFactory.getSocketFactory()));

    final PoolingClientConnectionManager mgr =
            new PoolingClientConnectionManager(schemeRegistry);
    mgr.setMaxTotal(200);
    mgr.setDefaultMaxPerRoute(20);

    final DefaultHttpClient httpClient = new DefaultHttpClient(mgr, params);

    final Credentials credentials =
            new UsernamePasswordCredentials(userName, password);
    httpClient.getCredentialsProvider().setCredentials(
            AuthScope.ANY, credentials);
    httpClient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);

    final ClientConfig clientConfig = new ApacheHttpClientConfig(httpClient);
    clientConfig.setBypassHostnameVerification(true);

    return new SCIMService(URI.create("http://localhost:" + getSSTestPort()),
        clientConfig);
  }



  /**
   * Validate a returned location value.
   *
   * @param uri       The location URI to be validated.
   * @param endpoint  The expected resource endpoint name.
   * @param id        The expected resource ID.
   */
  protected static void assertLocation(final URI uri, final String endpoint,
                                       final String id)
  {
    final UriBuilder builder = UriBuilder.fromPath("v1/" + endpoint);
    builder.scheme("http");
    builder.host("localhost");
    builder.port(getSSTestPort());
    builder.path(id);

    assertEquals(uri, builder.build());
  }



  /**
   * Retrieves an in-memory directory server instance that can be used for
   * testing purposes.  It will be started, but will not have any data.  It
   * will allow base DNs of "dc=example,dc=com" and "o=example.com" and will
   * have an additional bind DN of "cn=Directory Manager" with a password of
   * "password".  It will be listening on an automatically-selected port.
   *
   * @return  An empty in-memory directory server instance that may be used for
   *          testing.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  protected static InMemoryDirectoryServer getTestDS()
            throws Exception
  {
    return getTestDS(false, false);
  }



  /**
   * Retrieves an in-memory directory server instance that can be used for
   * testing purposes.  It will be started, and may optionally contain a basic
   * set of entries.
   *
   * @param  addBaseEntry  Indicates whether to add the "dc=example,dc=com"
   *                       entry.  If this is {@code false}, then the server
   *                       instance returned will be empty.
   * @param  addUserEntry  Indicates whether to add
   *                       "ou=People,dc=example,dc=com" and
   *                       "uid=test.user,ou=People,dc=example,dc=com" entries.
   *                       This will only be used if {@code addBaseEntry} is
   *                       {@code true}.
   *
   * @return  An in-memory directory server instance that may be used for
   *          testing, optionally populated with test entries.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  protected static synchronized InMemoryDirectoryServer
                                     getTestDS(final boolean addBaseEntry,
                                               final boolean addUserEntry)
            throws Exception
  {
    testDS.clear();

    if (addBaseEntry)
    {
      testDS.add(
           "dn: dc=example,dc=com",
           "objectClass: top",
           "objectClass: domain",
           "dc: example");

      if (addUserEntry)
      {
        testDS.add(
             "dn: ou=People,dc=example,dc=com",
             "objectClass: top",
             "objectClass: organizationalUnit",
             "ou: People");

        testDS.add(
             "dn: uid=test.user,ou=People,dc=example,dc=com",
             "objectClass: top",
             "objectClass: person",
             "objectClass: organizationalPerson",
             "objectClass: inetOrgPerson",
             "uid: test.user",
             "givenName: Test",
             "sn: User",
             "cn: Test User",
             "userPassword: password");
      }
    }

    return testDS;
  }



  /**
   * Retrieves the port of the SCIM server instance that can be
   * used for testing.
   *
   * @return  The port of the SCIM server instance that can be
   *          used for testing.
   */
  protected static synchronized int getSSTestPort()
  {
    return ssPort;
  }



  /**
   * Creates and returns a handle to an empty temporary file.  It will be marked
   * for deletion when the JVM exits.
   *
   * @return  A handle to the temporary file that was created.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  protected static File createTempFile()
            throws Exception
  {
    File f = File.createTempFile("scimri-", ".tmp");
    f.deleteOnExit();
    return f;
  }



  /**
   * Creates and returns a handle to an empty temporary file with the specified
   * lines.  It will be marked for deletion when the JVM exits.
   *
   * @param  lines  The set of lines to include in the file that is created.
   *
   * @return  A handle to the temporary file that was created.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  protected static File createTempFile(String... lines)
            throws Exception
  {
    File f = File.createTempFile("scimri-", ".tmp");
    f.deleteOnExit();

    if (lines.length > 0)
    {
      BufferedWriter w = new BufferedWriter(new FileWriter(f));
      try
      {
        for (String line : lines)
        {
          w.write(line);
          w.newLine();
        }
      }
      finally
      {
        w.close();
      }
    }

    return f;
  }



  /**
   * Creates and returns a handle to an empty directory in a temporary working
   * space.  It will not automatically be cleaned up when the JVM exits, but
   * should be cleaned when the build process is re-started.
   *
   * @return  A handle to the directory that was created.
   *
   * @throws  Exception  if an unexpected problem occurs.
   */
  protected static File createTempDir()
            throws Exception
  {
    final File f = File.createTempFile("scimri-", ".tmp");
    assertTrue(f.delete());
    assertTrue(f.mkdir());
    return f;
  }



  /**
   * Deletes the specified file.  If the provided file is a directory, then all
   * of the files and directories that it contains will be removed as well.
   *
   * @param  f  The reference to the file or directory to delete.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  protected static void delete(File f)
            throws Exception
  {
    if (f.isDirectory())
    {
      for (File subFile : f.listFiles())
      {
        delete(subFile);
      }
    }

    f.delete();
  }



  /**
   * Writes the provided content to standard output.  The string representations
   * of the provided objects will be concatenated, and it will be terminated
   * with a line separator.
   *
   * @param  content  The objects that comprise content to be written.
   */
  protected static void out(final Object... content)
  {
    for (Object o : content)
    {
      System.out.print(String.valueOf(o));
    }
    System.out.println();
  }



  /**
   * Writes the provided content to standard error.  The string representations
   * of the provided objects will be concatenated, and it will be terminated
   * with a line separator.
   *
   * @param  content  The objects that comprise content to be written.
   */
  protected static void err(final Object... content)
  {
    for (Object o : content)
    {
      System.err.print(String.valueOf(o));
    }
    System.err.println();
  }



  /**
   * Calculates an MD5 digest for the contents of the specified file.
   *
   * @param  f  The file for which to retrieve the MD5 digest.
   *
   * @return  The MD5 digest for the requested file.
   *
   * @throws  Exception  If a problem occurs while attempting to compute the MD5
   *                     digest for the specified file.
   */
  protected static byte[] getMD5Digest(final File f)
            throws Exception
  {

    final FileInputStream inputStream = new FileInputStream(f);

    try
    {
      final MessageDigest md5 = MessageDigest.getInstance("MD5");
      final byte[] buffer = new byte[8192];

      while (true)
      {
        final int bytesRead = inputStream.read(buffer);
        if (bytesRead < 0)
        {
          break;
        }
        else
        {
          md5.update(buffer, 0, bytesRead);
        }
      }

      return md5.digest();
    }
    finally
    {
      inputStream.close();
    }
  }



  /**
   * Calculates an MD5 digest for the provided array.
   *
   * @param  b  The array for which to calculate the MD5 digest.
   *
   * @return  The MD5 digest for the provided array.
   *
   * @throws  Exception  If a problem occurs while attempting to compute the MD5
   *                     digest for the provided array.
   */
  protected static byte[] getMD5Digest(final byte[] b)
            throws Exception
  {
    return MessageDigest.getInstance("MD5").digest(b);
  }



  /**
   * Generates a domain entry with the provided information.  It will include
   * the top and domain object classes and will use dc as the RDN attribute.  It
   * may optionally include additional attributes.
   *
   * @param  name                  The name for the domain, which will be used
   *                               as the value of the "dc" attribute.  It must
   *                               not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateDomainEntry(final String name, final String parentDN,
                                      final Attribute... additionalAttributes)
  {
    return LDAPTestUtils.generateDomainEntry(name, parentDN,
         additionalAttributes);
  }



  /**
   * Generates a domain entry with the provided information.  It will include
   * the top and domain object classes and will use dc as the RDN attribute.  It
   * may optionally include additional attributes.
   *
   * @param  name                  The name for the domain, which will be used
   *                               as the value of the "dc" attribute.  It must
   *                               not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateDomainEntry(final String name, final String parentDN,
                       final Collection<Attribute> additionalAttributes)
  {
    return LDAPTestUtils.generateDomainEntry(name, parentDN,
         additionalAttributes);
  }



  /**
   * Generates an organization entry with the provided information.  It will
   * include the top and organization object classes and will use o as the RDN
   * attribute.  It may optionally include additional attributes.
   *
   * @param  name                  The name for the organization, which will be
   *                               used as the value of the "o" attribute.  It
   *                               must not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateOrgEntry(final String name, final String parentDN,
                                   final Attribute... additionalAttributes)
  {
    return LDAPTestUtils.generateOrgEntry(name, parentDN, additionalAttributes);
  }



  /**
   * Generates an organization entry with the provided information.  It will
   * include the top and organization object classes and will use o as the RDN
   * attribute.  It may optionally include additional attributes.
   *
   * @param  name                  The name for the organization, which will be
   *                               used as the value of the "o" attribute.  It
   *                               must not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateOrgEntry(final String name, final String parentDN,
                       final Collection<Attribute> additionalAttributes)
  {
    return LDAPTestUtils.generateOrgEntry(name, parentDN, additionalAttributes);
  }



  /**
   * Generates an organizationalUnit entry with the provided information.  It
   * will include the top and organizationalUnit object classes and will use ou
   * as the RDN attribute.  It may optionally include additional attributes.
   *
   * @param  name                  The name for the organizationalUnit, which
   *                               will be used as the value of the "ou"
   *                               attribute.  It must not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateOrgUnitEntry(final String name, final String parentDN,
                                       final Attribute... additionalAttributes)
  {
    return LDAPTestUtils.generateOrgUnitEntry(name, parentDN,
         additionalAttributes);
  }



  /**
   * Generates an organizationalUnit entry with the provided information.  It
   * will include the top and organizationalUnit object classes and will use ou
   * as the RDN attribute.  It may optionally include additional attributes.
   *
   * @param  name                  The name for the organizationalUnit, which
   *                               will be used as the value of the "ou"
   *                               attribute.  It must not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateOrgUnitEntry(final String name, final String parentDN,
                       final Collection<Attribute> additionalAttributes)
  {
    return LDAPTestUtils.generateOrgUnitEntry(name, parentDN,
         additionalAttributes);
  }



  /**
   * Generates a country entry with the provided information.  It will include
   * the top and country object classes and will use c as the RDN attribute.  It
   * may optionally include additional attributes.
   *
   * @param  name                  The name for the country (typically a
   *                               two-character country code), which will be
   *                               used as the value of the "c" attribute.  It
   *                               must not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateCountryEntry(final String name, final String parentDN,
                                       final Attribute... additionalAttributes)
  {
    return LDAPTestUtils.generateCountryEntry(name, parentDN,
         additionalAttributes);
  }



  /**
   * Generates a country entry with the provided information.  It will include
   * the top and country object classes and will use c as the RDN attribute.  It
   * may optionally include additional attributes.
   *
   * @param  name                  The name for the country (typically a
   *                               two-character country code), which will be
   *                               used as the value of the "c" attribute.  It
   *                               must not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateCountryEntry(final String name, final String parentDN,
                       final Collection<Attribute> additionalAttributes)
  {
    return LDAPTestUtils.generateCountryEntry(name, parentDN,
         additionalAttributes);
  }



  /**
   * Generates a user entry with the provided information.  It will include the
   * top, person, organizationalPerson, and inetOrgPerson object classes, will
   * use uid as the RDN attribute, and will have givenName, sn, and cn
   * attributes.  It may optionally include additional attributes.
   *
   * @param  uid                   The value to use for the "uid: attribute.  It
   *                               must not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  firstName             The first name for the user.  It must not be
   *                               {@code null}.
   * @param  lastName              The last name for the user.  It must not be
   *                               {@code null}.
   * @param  password              The password for the user.  It may be
   *                               {@code null} if the user should not have a
   *                               password.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateUserEntry(final String uid, final String parentDN,
                                    final String firstName,
                                    final String lastName,
                                    final String password,
                                    final Attribute... additionalAttributes)
  {
    return LDAPTestUtils.generateUserEntry(uid, parentDN, firstName, lastName,
         password, additionalAttributes);
  }



  /**
   * Generates a user entry with the provided information.  It will include the
   * top, person, organizationalPerson, and inetOrgPerson object classes, will
   * use uid as the RDN attribute, and will have givenName, sn, and cn
   * attributes.  It may optionally include additional attributes.
   *
   * @param  uid                   The value to use for the "uid: attribute.  It
   *                               must not be {@code null}.
   * @param  parentDN              The DN of the entry below which the new
   *                               entry should be placed.  It may be
   *                               {@code null} if the new entry should not have
   *                               a parent.
   * @param  firstName             The first name for the user.  It must not be
   *                               {@code null}.
   * @param  lastName              The last name for the user.  It must not be
   *                               {@code null}.
   * @param  password              The password for the user.  It may be
   *                               {@code null} if the user should not have a
   *                               password.
   * @param  additionalAttributes  A set of additional attributes to include in
   *                               the generated entry.  It may be {@code null}
   *                               or empty if no additional attributes should
   *                               be included.
   *
   * @return  The generated entry.
   */
  protected Entry generateUserEntry(final String uid, final String parentDN,
                       final String firstName, final String lastName,
                       final String password,
                       final Collection<Attribute> additionalAttributes)
  {
    return LDAPTestUtils.generateUserEntry(uid, parentDN, firstName, lastName,
         password, additionalAttributes);
  }



  /**
   * Generates a group entry with the provided information.  It will include
   * the top and groupOfNames object classes and will use cn as the RDN
   * attribute.
   *
   * @param  name       The name for the group, which will be used as the value
   *                    of the "cn" attribute.  It must not be {@code null}.
   * @param  parentDN   The DN of the entry below which the new entry should be
   *                    placed.  It may be {@code null} if the new entry should
   *                    not have a parent.
   * @param  memberDNs  The DNs of the users that should be listed as members of
   *                    the group.
   *
   * @return  The generated entry.
   */
  protected Entry generateGroupOfNamesEntry(final String name,
                                                final String parentDN,
                                                final String... memberDNs)
  {
    return LDAPTestUtils.generateGroupOfNamesEntry(name, parentDN, memberDNs);
  }



  /**
   * Generates a group entry with the provided information.  It will include
   * the top and groupOfNames object classes and will use cn as the RDN
   * attribute.
   *
   * @param  name       The name for the group, which will be used as the value
   *                    of the "cn" attribute.  It must not be {@code null}.
   * @param  parentDN   The DN of the entry below which the new entry should be
   *                    placed.  It may be {@code null} if the new entry should
   *                    not have a parent.
   * @param  memberDNs  The DNs of the users that should be listed as members of
   *                    the group.
   *
   * @return  The generated entry.
   */
  protected Entry generateGroupOfNamesEntry(final String name,
                           final String parentDN,
                           final Collection<String> memberDNs)
  {
    return LDAPTestUtils.generateGroupOfNamesEntry(name, parentDN, memberDNs);
  }



  /**
   * Generates a group entry with the provided information.  It will include
   * the top and groupOfUniqueNames object classes and will use cn as the RDN
   * attribute.
   *
   * @param  name       The name for the group, which will be used as the value
   *                    of the "cn" attribute.  It must not be {@code null}.
   * @param  parentDN   The DN of the entry below which the new entry should be
   *                    placed.  It may be {@code null} if the new entry should
   *                    not have a parent.
   * @param  memberDNs  The DNs of the users that should be listed as members of
   *                    the group.
   *
   * @return  The generated entry.
   */
  protected Entry generateGroupOfUniqueNamesEntry(final String name,
                                                      final String parentDN,
                                                      final String... memberDNs)
  {
    return LDAPTestUtils.generateGroupOfUniqueNamesEntry(name, parentDN,
         memberDNs);
  }



  /**
   * Generates a group entry with the provided information.  It will include
   * the top and groupOfUniqueNames object classes and will use cn as the RDN
   * attribute.
   *
   * @param  name       The name for the group, which will be used as the value
   *                    of the "cn" attribute.  It must not be {@code null}.
   * @param  parentDN   The DN of the entry below which the new entry should be
   *                    placed.  It may be {@code null} if the new entry should
   *                    not have a parent.
   * @param  memberDNs  The DNs of the users that should be listed as members of
   *                    the group.
   *
   * @return  The generated entry.
   */
  protected Entry generateGroupOfUniqueNamesEntry(final String name,
                       final String parentDN,
                       final Collection<String> memberDNs)
  {
    return LDAPTestUtils.generateGroupOfUniqueNamesEntry(name, parentDN,
         memberDNs);
  }



  /**
   * Indicates whether the specified entry exists in the server.
   *
   * @param  conn  The connection to use to communicate with the directory
   *               server.
   * @param  dn    The DN of the entry for which to make the determination.
   *
   * @return  {@code true} if the entry exists, or {@code false} if not.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected boolean entryExists(final LDAPInterface conn, final String dn)
            throws LDAPException
  {
    return LDAPTestUtils.entryExists(conn, dn);
  }



  /**
   * Indicates whether the specified entry exists in the server and matches the
   * given filter.
   *
   * @param  conn    The connection to use to communicate with the directory
   *                 server.
   * @param  dn      The DN of the entry for which to make the determination.
   * @param  filter  The filter the entry is expected to match.
   *
   * @return  {@code true} if the entry exists and matches the specified filter,
   *          or {@code false} if not.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected boolean entryExists(final LDAPInterface conn, final String dn,
                             final String filter)
            throws LDAPException
  {
    return LDAPTestUtils.entryExists(conn, dn, filter);
  }



  /**
   * Indicates whether the specified entry exists in the server.  This will
   * return {@code true} only if the target entry exists and contains all values
   * for all attributes of the provided entry.  The entry will be allowed to
   * have attribute values not included in the provided entry.
   *
   * @param  conn   The connection to use to communicate with the directory
   *                server.
   * @param  entry  The entry to compare against the directory server.
   *
   * @return  {@code true} if the entry exists in the server and is a superset
   *          of the provided entry, or {@code false} if not.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected boolean entryExists(final LDAPInterface conn, final Entry entry)
            throws LDAPException
  {
    return LDAPTestUtils.entryExists(conn, entry);
  }



  /**
   * Ensures that an entry with the provided DN exists in the directory.
   *
   * @param  conn  The connection to use to communicate with the directory
   *               server.
   * @param  dn    The DN of the entry for which to make the determination.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist.
   */
  protected void assertEntryExists(final LDAPInterface conn, final String dn)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertEntryExists(conn, dn);
  }



  /**
   * Ensures that an entry with the provided DN exists in the directory.
   *
   * @param  conn    The connection to use to communicate with the directory
   *                 server.
   * @param  dn      The DN of the entry for which to make the determination.
   * @param  filter  A filter that the target entry must match.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist or does not
   *                          match the provided filter.
   */
  protected void assertEntryExists(final LDAPInterface conn, final String dn,
                                   final String filter)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertEntryExists(conn, dn, filter);
  }



  /**
   * Ensures that an entry exists in the directory with the same DN and all
   * attribute values contained in the provided entry.  The server entry may
   * contain additional attributes and/or attribute values not included in the
   * provided entry.
   *
   * @param  conn   The connection to use to communicate with the directory
   *                server.
   * @param  entry  The entry expected to be present in the directory server.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist or does not
   *                          match the provided filter.
   */
  protected void assertEntryExists(final LDAPInterface conn, final Entry entry)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertEntryExists(conn, entry);
  }



  /**
   * Retrieves a list containing the DNs of the entries which are missing from
   * the directory server.
   *
   * @param  conn  The connection to use to communicate with the directory
   *               server.
   * @param  dns   The DNs of the entries to try to find in the server.
   *
   * @return  A list containing all of the provided DNs that were not found in
   *          the server, or an empty list if all entries were found.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected List<String> getMissingEntryDNs(final LDAPInterface conn,
                                            final String... dns)
            throws LDAPException
  {
    return LDAPTestUtils.getMissingEntryDNs(conn, dns);
  }



  /**
   * Retrieves a list containing the DNs of the entries which are missing from
   * the directory server.
   *
   * @param  conn  The connection to use to communicate with the directory
   *               server.
   * @param  dns   The DNs of the entries to try to find in the server.
   *
   * @return  A list containing all of the provided DNs that were not found in
   *          the server, or an empty list if all entries were found.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected List<String> getMissingEntryDNs(final LDAPInterface conn,
                                            final Collection<String> dns)
            throws LDAPException
  {
    return LDAPTestUtils.getMissingEntryDNs(conn, dns);
  }



  /**
   * Ensures that all of the entries with the provided DNs exist in the
   * directory.
   *
   * @param  conn  The connection to use to communicate with the directory
   *               server.
   * @param  dns   The DNs of the entries for which to make the determination.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If any of the target entries does not exist.
   */
  protected void assertEntriesExist(final LDAPInterface conn,
                                    final String... dns)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertEntriesExist(conn, dns);
  }



  /**
   * Ensures that all of the entries with the provided DNs exist in the
   * directory.
   *
   * @param  conn  The connection to use to communicate with the directory
   *               server.
   * @param  dns   The DNs of the entries for which to make the determination.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If any of the target entries does not exist.
   */
  protected void assertEntriesExist(final LDAPInterface conn,
                                    final Collection<String> dns)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertEntriesExist(conn, dns);
  }



  /**
   * Retrieves a list containing all of the named attributes which do not exist
   * in the target entry.
   *
   * @param  conn            The connection to use to communicate with the
   *                         directory server.
   * @param  dn              The DN of the entry to examine.
   * @param  attributeNames  The names of the attributes expected to be present
   *                         in the target entry.
   *
   * @return  A list containing the names of the attributes which were not
   *          present in the target entry, an empty list if all specified
   *          attributes were found in the entry, or {@code null} if the target
   *          entry does not exist.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected List<String> getMissingAttributeNames(final LDAPInterface conn,
                              final String dn, final String... attributeNames)
            throws LDAPException
  {
    return LDAPTestUtils.getMissingAttributeNames(conn, dn, attributeNames);
  }



  /**
   * Retrieves a list containing all of the named attributes which do not exist
   * in the target entry.
   *
   * @param  conn            The connection to use to communicate with the
   *                         directory server.
   * @param  dn              The DN of the entry to examine.
   * @param  attributeNames  The names of the attributes expected to be present
   *                         in the target entry.
   *
   * @return  A list containing the names of the attributes which were not
   *          present in the target entry, an empty list if all specified
   *          attributes were found in the entry, or {@code null} if the target
   *          entry does not exist.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected List<String> getMissingAttributeNames(final LDAPInterface conn,
                              final String dn,
                              final Collection<String> attributeNames)
            throws LDAPException
  {
    return LDAPTestUtils.getMissingAttributeNames(conn, dn, attributeNames);
  }



  /**
   * Ensures that the specified entry exists in the directory with all of the
   * specified attributes.
   *
   * @param  conn            The connection to use to communicate with the
   *                         directory server.
   * @param  dn              The DN of the entry to examine.
   * @param  attributeNames  The names of the attributes that are expected to be
   *                         present in the provided entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist or does not
   *                          contain all of the specified attributes.
   */
  protected void assertAttributeExists(final LDAPInterface conn,
                                       final String dn,
                                       final String... attributeNames)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertAttributeExists(conn, dn, attributeNames);
  }



  /**
   * Ensures that the specified entry exists in the directory with all of the
   * specified attributes.
   *
   * @param  conn            The connection to use to communicate with the
   *                         directory server.
   * @param  dn              The DN of the entry to examine.
   * @param  attributeNames  The names of the attributes that are expected to be
   *                         present in the provided entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist or does not
   *                          contain all of the specified attributes.
   */
  protected void assertAttributeExists(final LDAPInterface conn,
                                       final String dn,
                                       final Collection<String> attributeNames)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertAttributeExists(conn, dn, attributeNames);
  }



  /**
   * Retrieves a list of all provided attribute values which are missing from
   * the specified entry.
   *
   * @param  conn             The connection to use to communicate with the
   *                          directory server.
   * @param  dn               The DN of the entry to examine.
   * @param  attributeName    The attribute expected to be present in the target
   *                          entry with the given values.
   * @param  attributeValues  The values expected to be present in the target
   *                          entry.
   *
   * @return  A list containing all of the provided values which were not found
   *          in the entry, an empty list if all provided attribute values were
   *          found, or {@code null} if the target entry does not exist.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected List<String> getMissingAttributeValues(final LDAPInterface conn,
                              final String dn, final String attributeName,
                              final String... attributeValues)
            throws LDAPException
  {
    return LDAPTestUtils.getMissingAttributeValues(conn, dn, attributeName,
         attributeValues);
  }



  /**
   * Retrieves a list of all provided attribute values which are missing from
   * the specified entry.  The target attribute may or may not contain
   * additional values.
   *
   * @param  conn             The connection to use to communicate with the
   *                          directory server.
   * @param  dn               The DN of the entry to examine.
   * @param  attributeName    The attribute expected to be present in the target
   *                          entry with the given values.
   * @param  attributeValues  The values expected to be present in the target
   *                          entry.
   *
   * @return  A list containing all of the provided values which were not found
   *          in the entry, an empty list if all provided attribute values were
   *          found, or {@code null} if the target entry does not exist.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   */
  protected List<String> getMissingAttributeValues(final LDAPInterface conn,
                              final String dn, final String attributeName,
                              final Collection<String> attributeValues)
            throws LDAPException
  {
    return LDAPTestUtils.getMissingAttributeValues(conn, dn, attributeName,
         attributeValues);
  }



  /**
   * Ensures that the specified entry exists in the directory with all of the
   * specified values for the given attribute.  The attribute may or may not
   * contain additional values.
   *
   * @param  conn             The connection to use to communicate with the
   *                          directory server.
   * @param  dn               The DN of the entry to examine.
   * @param  attributeName    The name of the attribute to examine.
   * @param  attributeValues  The set of values which must exist for the given
   *                          attribute.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist, does not
   *                          contain the specified attribute, or that attribute
   *                          does not have all of the specified values.
   */
  protected void assertValueExists(final LDAPInterface conn, final String dn,
                                   final String attributeName,
                                   final String... attributeValues)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertValueExists(conn, dn, attributeName, attributeValues);
  }



  /**
   * Ensures that the specified entry exists in the directory with all of the
   * specified values for the given attribute.  The attribute may or may not
   * contain additional values.
   *
   * @param  conn             The connection to use to communicate with the
   *                          directory server.
   * @param  dn               The DN of the entry to examine.
   * @param  attributeName    The name of the attribute to examine.
   * @param  attributeValues  The set of values which must exist for the given
   *                          attribute.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry does not exist, does not
   *                          contain the specified attribute, or that attribute
   *                          does not have all of the specified values.
   */
  protected void assertValueExists(final LDAPInterface conn, final String dn,
                                   final String attributeName,
                                   final Collection<String> attributeValues)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertValueExists(conn, dn, attributeName, attributeValues);
  }



  /**
   * Ensures that the specified entry does not exist in the directory.
   *
   * @param  conn  The connection to use to communicate with the directory
   *               server.
   * @param  dn    The DN of the entry expected to be missing.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry is found in the server.
   */
  protected void assertEntryMissing(final LDAPInterface conn, final String dn)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertEntryMissing(conn, dn);
  }



  /**
   * Ensures that the specified entry exists in the directory but does not
   * contain any of the specified attributes.
   *
   * @param  conn            The connection to use to communicate with the
   *                         directory server.
   * @param  dn              The DN of the entry expected to be present.
   * @param  attributeNames  The names of the attributes expected to be missing
   *                         from the entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry is missing from the server, or
   *                          if it contains any of the target attributes.
   */
  protected void assertAttributeMissing(final LDAPInterface conn,
                                        final String dn,
                                        final String... attributeNames)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertAttributeMissing(conn, dn, attributeNames);
  }



  /**
   * Ensures that the specified entry exists in the directory but does not
   * contain any of the specified attributes.
   *
   * @param  conn            The connection to use to communicate with the
   *                         directory server.
   * @param  dn              The DN of the entry expected to be present.
   * @param  attributeNames  The names of the attributes expected to be missing
   *                         from the entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry is missing from the server, or
   *                          if it contains any of the target attributes.
   */
  protected void assertAttributeMissing(final LDAPInterface conn,
                                        final String dn,
                                        final Collection<String> attributeNames)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertAttributeMissing(conn, dn, attributeNames);
  }



  /**
   * Ensures that the specified entry exists in the directory but does not
   * contain any of the specified attribute values.
   *
   * @param  conn             The connection to use to communicate with the
   *                          directory server.
   * @param  dn               The DN of the entry expected to be present.
   * @param  attributeName    The name of the attribute to examine.
   * @param  attributeValues  The values expected to be missing from the target
   *                          entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry is missing from the server, or
   *                          if it contains any of the target attribute values.
   */
  protected void assertValueMissing(final LDAPInterface conn, final String dn,
                                    final String attributeName,
                                    final String... attributeValues)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertValueMissing(conn, dn, attributeName, attributeValues);
  }



  /**
   * Ensures that the specified entry exists in the directory but does not
   * contain any of the specified attribute values.
   *
   * @param  conn             The connection to use to communicate with the
   *                          directory server.
   * @param  dn               The DN of the entry expected to be present.
   * @param  attributeName    The name of the attribute to examine.
   * @param  attributeValues  The values expected to be missing from the target
   *                          entry.
   *
   * @throws  LDAPException  If a problem is encountered while trying to
   *                         communicate with the directory server.
   *
   * @throws  AssertionError  If the target entry is missing from the server, or
   *                          if it contains any of the target attribute values.
   */
  protected void assertValueMissing(final LDAPInterface conn, final String dn,
                                    final String attributeName,
                                    final Collection<String> attributeValues)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertValueMissing(conn, dn, attributeName, attributeValues);
  }



  /**
   * Ensures that the result code for the provided result matches one of the
   * given acceptable result codes.
   *
   * @param  result                 The LDAP result to examine.
   * @param  acceptableResultCodes  The set of result codes that are considered
   *                                acceptable.
   *
   * @throws  AssertionError  If the result code from the provided result did
   *                          not match any of the acceptable values.
   */
  protected void assertResultCodeEquals(final LDAPResult result,
                      final ResultCode... acceptableResultCodes)
            throws AssertionError
  {
    LDAPTestUtils.assertResultCodeEquals(result, acceptableResultCodes);
  }



  /**
   * Ensures that the result code for the provided LDAP exception matches one of
   * the given acceptable result codes.
   *
   * @param  exception              The LDAP exception to examine.
   * @param  acceptableResultCodes  The set of result codes that are considered
   *                                acceptable.
   *
   * @throws  AssertionError  If the result code from the provided exception did
   *                          not match any of the acceptable values.
   */
  protected void assertResultCodeEquals(final LDAPException exception,
                      final ResultCode... acceptableResultCodes)
            throws AssertionError
  {
    LDAPTestUtils.assertResultCodeEquals(exception, acceptableResultCodes);
  }



  /**
   * Processes the provided request using the given connection and ensures that
   * the result code matches one of the provided acceptable values.
   *
   * @param  conn                   The connection to use to communicate with
   *                                the directory server.
   * @param  request                The request to be processed.
   * @param  acceptableResultCodes  The set of result codes that are considered
   *                                acceptable.
   *
   * @return  The result returned from processing the requested operation.
   *
   * @throws  AssertionError  If the result code returned by the server did not
   *                          match any acceptable values.
   */
  protected LDAPResult assertResultCodeEquals(final LDAPConnection conn,
                            final LDAPRequest request,
                            final ResultCode... acceptableResultCodes)
            throws AssertionError
  {
    return LDAPTestUtils.assertResultCodeEquals(conn, request,
         acceptableResultCodes);
  }



  /**
   * Ensures that the result code for the provided result does not match any of
   * the given unacceptable result codes.
   *
   * @param  result                   The LDAP result to examine.
   * @param  unacceptableResultCodes  The set of result codes that are
   *                                  considered unacceptable.
   *
   * @throws  AssertionError  If the result code from the provided result
   *                          matched any of the unacceptable values.
   */
  protected void assertResultCodeNot(final LDAPResult result,
                      final ResultCode... unacceptableResultCodes)
            throws AssertionError
  {
    LDAPTestUtils.assertResultCodeNot(result, unacceptableResultCodes);
  }



  /**
   * Ensures that the result code for the provided result does not match any of
   * the given unacceptable result codes.
   *
   * @param  exception                The LDAP exception to examine.
   * @param  unacceptableResultCodes  The set of result codes that are
   *                                  considered unacceptable.
   *
   * @throws  AssertionError  If the result code from the provided result
   *                          matched any of the unacceptable values.
   */
  protected void assertResultCodeNot(final LDAPException exception,
                      final ResultCode... unacceptableResultCodes)
            throws AssertionError
  {
    LDAPTestUtils.assertResultCodeNot(exception, unacceptableResultCodes);
  }



  /**
   * Processes the provided request using the given connection and ensures that
   * the result code does not match any of the given unacceptable values.
   *
   * @param  conn                     The connection to use to communicate with
   *                                  the directory server.
   * @param  request                  The request to be processed.
   * @param  unacceptableResultCodes  The set of result codes that are
   *                                  considered unacceptable.
   *
   * @return  The result returned from processing the requested operation.
   *
   * @throws  AssertionError  If the result code from the provided result
   *                          matched any of the unacceptable values.
   */
  protected LDAPResult assertResultCodeNot(final LDAPConnection conn,
                            final LDAPRequest request,
                            final ResultCode... unacceptableResultCodes)
            throws AssertionError
  {
    return LDAPTestUtils.assertResultCodeNot(conn, request,
         unacceptableResultCodes);
  }



  /**
   * Ensures that the provided LDAP result contains a matched DN value.
   *
   * @param  result  The LDAP result to examine.
   *
   * @throws  AssertionError  If the provided result did not contain a matched
   *                          DN value.
   */
  protected void assertContainsMatchedDN(final LDAPResult result)
            throws AssertionError
  {
    LDAPTestUtils.assertContainsMatchedDN(result);
  }



  /**
   * Ensures that the provided LDAP exception contains a matched DN value.
   *
   * @param  exception  The LDAP exception to examine.
   *
   * @throws  AssertionError  If the provided exception did not contain a
   *                          matched DN value.
   */
  protected void assertContainsMatchedDN(final LDAPException exception)
            throws AssertionError
  {
    LDAPTestUtils.assertContainsMatchedDN(exception);
  }



  /**
   * Ensures that the provided LDAP result does not contain a matched DN value.
   *
   * @param  result  The LDAP result to examine.
   *
   * @throws  AssertionError  If the provided result contained a matched DN
   *                          value.
   */
  protected void assertMissingMatchedDN(final LDAPResult result)
            throws AssertionError
  {
    LDAPTestUtils.assertMissingMatchedDN(result);
  }



  /**
   * Ensures that the provided LDAP exception does not contain a matched DN
   * value.
   *
   * @param  exception  The LDAP exception to examine.
   *
   * @throws  AssertionError  If the provided exception contained a matched DN
   *                          value.
   */
  protected void assertMissingMatchedDN(final LDAPException exception)
            throws AssertionError
  {
    LDAPTestUtils.assertMissingMatchedDN(exception);
  }



  /**
   * Ensures that the provided LDAP result has the given matched DN value.
   *
   * @param  result     The LDAP result to examine.
   * @param  matchedDN  The matched DN value expected to be found in the
   *                    provided result.  It must not be {@code null}.
   *
   * @throws  LDAPException  If either the found or expected matched DN values
   *                         could not be parsed as a valid DN.
   *
   * @throws  AssertionError  If the provided LDAP result did not contain a
   *                          matched DN, or if it had a matched DN that
   *                          differed from the expected value.
   */
  protected void assertMatchedDNEquals(final LDAPResult result,
                                       final String matchedDN)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertMatchedDNEquals(result, matchedDN);
  }



  /**
   * Ensures that the provided LDAP exception has the given matched DN value.
   *
   * @param  exception  The LDAP exception to examine.
   * @param  matchedDN  The matched DN value expected to be found in the
   *                    provided exception.  It must not be {@code null}.
   *
   * @throws  LDAPException  If either the found or expected matched DN values
   *                         could not be parsed as a valid DN.
   *
   * @throws  AssertionError  If the provided LDAP exception did not contain a
   *                          matched DN, or if it had a matched DN that
   *                          differed from the expected value.
   */
  protected void assertMatchedDNEquals(final LDAPException exception,
                                       final String matchedDN)
            throws LDAPException, AssertionError
  {
    LDAPTestUtils.assertMatchedDNEquals(exception, matchedDN);
  }



  /**
   * Ensures that the provided LDAP result has one or more referral URLs.
   *
   * @param  result  The LDAP result to examine.
   *
   * @throws  AssertionError  If the provided result does not have any referral
   *                          URLs.
   */
  protected void assertHasReferral(final LDAPResult result)
            throws AssertionError
  {
    LDAPTestUtils.assertHasReferral(result);
  }



  /**
   * Ensures that the provided LDAP exception has one or more referral URLs.
   *
   * @param  exception  The LDAP exception to examine.
   *
   * @throws  AssertionError  If the provided exception does not have any
   *                          referral URLs.
   */
  protected void assertHasReferral(final LDAPException exception)
            throws AssertionError
  {
    LDAPTestUtils.assertHasReferral(exception);
  }



  /**
   * Ensures that the provided LDAP result does not have any referral URLs.
   *
   * @param  result  The LDAP result to examine.
   *
   * @throws  AssertionError  If the provided result has one or more referral
   *                          URLs.
   */
  protected void assertMissingReferral(final LDAPResult result)
            throws AssertionError
  {
    LDAPTestUtils.assertMissingReferral(result);
  }



  /**
   * Ensures that the provided LDAP exception does not have any referral URLs.
   *
   * @param  exception  The LDAP exception to examine.
   *
   * @throws  AssertionError  If the provided exception has one or more referral
   *                          URLs.
   */
  protected void assertMissingReferral(final LDAPException exception)
            throws AssertionError
  {
    LDAPTestUtils.assertMissingReferral(exception);
  }



  /**
   * Ensures that the provided LDAP result includes at least one control with
   * the specified OID.
   *
   * @param  result  The LDAP result to examine.
   * @param  oid     The OID of the control which is expected to be present in
   *                 the result.
   *
   * @return  The first control found with the specified OID.
   *
   * @throws  AssertionError  If the provided LDAP result does not include any
   *                          control with the specified OID.
   */
  protected Control assertHasControl(final LDAPResult result, final String oid)
            throws AssertionError
  {
    return LDAPTestUtils.assertHasControl(result, oid);
  }



  /**
   * Ensures that the provided LDAP exception includes at least one control with
   * the specified OID.
   *
   * @param  exception  The LDAP exception to examine.
   * @param  oid        The OID of the control which is expected to be present
   *                    in the exception.
   *
   * @return  The first control found with the specified OID.
   *
   * @throws  AssertionError  If the provided LDAP exception does not include
   *                          any control with the specified OID.
   */
  protected Control assertHasControl(final LDAPException exception,
                                     final String oid)
            throws AssertionError
  {
    return LDAPTestUtils.assertHasControl(exception, oid);
  }



  /**
   * Ensures that the provided search result entry includes at least one control
   * with the specified OID.
   *
   * @param  entry  The search result entry to examine.
   * @param  oid    The OID of the control which is expected to be present in
   *                the search result entry.
   *
   * @return  The first control found with the specified OID.
   *
   * @throws  AssertionError  If the provided search result entry does not
   *                          include any control with the specified OID.
   */
  protected Control assertHasControl(final SearchResultEntry entry,
                                     final String oid)
            throws AssertionError
  {
    return LDAPTestUtils.assertHasControl(entry, oid);
  }



  /**
   * Ensures that the provided search result reference includes at least one
   * control with the specified OID.
   *
   * @param  reference  The search result reference to examine.
   * @param  oid        The OID of the control which is expected to be present
   *                    in the search result reference.
   *
   * @return  The first control found with the specified OID.
   *
   * @throws  AssertionError  If the provided search result reference does not
   *                          include any control with the specified OID.
   */
  protected Control assertHasControl(final SearchResultReference reference,
                                     final String oid)
            throws AssertionError
  {
    return LDAPTestUtils.assertHasControl(reference, oid);
  }



  /**
   * Ensures that the provided LDAP result does not include any control with
   * the specified OID.
   *
   * @param  result  The LDAP result to examine.
   * @param  oid     The OID of the control which is not expected to be present
   *                 in the result.
   *
   * @throws  AssertionError  If the provided LDAP result includes any control
   *                          with the specified OID.
   */
  protected void assertMissingControl(final LDAPResult result, final String oid)
            throws AssertionError
  {
    LDAPTestUtils.assertMissingControl(result, oid);
  }



  /**
   * Ensures that the provided LDAP exception does not include any control with
   * the specified OID.
   *
   * @param  exception  The LDAP exception to examine.
   * @param  oid        The OID of the control which is not expected to be
   *                    present in the exception.
   *
   * @throws  AssertionError  If the provided LDAP exception includes any
   *                          control with the specified OID.
   */
  protected void assertMissingControl(final LDAPException exception,
                                      final String oid)
            throws AssertionError
  {
    LDAPTestUtils.assertMissingControl(exception, oid);
  }



  /**
   * Ensures that the provided search result entry does not includes any control
   * with the specified OID.
   *
   * @param  entry  The search result entry to examine.
   * @param  oid    The OID of the control which is not expected to be present
   *                in the search result entry.
   *
   * @throws  AssertionError  If the provided search result entry includes any
   *                          control with the specified OID.
   */
  protected void assertMissingControl(final SearchResultEntry entry,
                                      final String oid)
            throws AssertionError
  {
    LDAPTestUtils.assertMissingControl(entry, oid);
  }



  /**
   * Ensures that the provided search result reference does not includes any
   * control with the specified OID.
   *
   * @param  reference  The search result reference to examine.
   * @param  oid        The OID of the control which is not expected to be
   *                    present in the search result reference.
   *
   * @throws  AssertionError  If the provided search result reference includes
   *                          any control with the specified OID.
   */
  protected void assertMissingControl(final SearchResultReference reference,
                                      final String oid)
            throws AssertionError
  {
    LDAPTestUtils.assertMissingControl(reference, oid);
  }



  /**
   * Ensures that the provided search result indicates that at least one search
   * result entry was returned.
   *
   * @param  result  The search result to examine.
   *
   * @return  The number of search result entries that were returned.
   *
   * @throws  AssertionError  If the provided search result indicates that no
   *                          entries were returned.
   */
  protected int assertEntryReturned(final SearchResult result)
            throws AssertionError
  {
    return LDAPTestUtils.assertEntryReturned(result);
  }



  /**
   * Ensures that the provided search exception indicates that at least one
   * search result entry was returned.
   *
   * @param  exception  The search exception to examine.
   *
   * @return  The number of search result entries that were returned.
   *
   * @throws  AssertionError  If the provided search exception indicates that no
   *                          entries were returned.
   */
  protected int assertEntryReturned(final LDAPSearchException exception)
            throws AssertionError
  {
    return LDAPTestUtils.assertEntryReturned(exception);
  }



  /**
   * Ensures that the specified search result entry was included in provided
   * search result.
   *
   * @param  result  The search result to examine.
   * @param  dn      The DN of the entry expected to be included in the
   *                 search result.
   *
   * @return  The search result entry with the provided DN.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a valid
   *                         DN.
   *
   * @throws  AssertionError  If the specified entry was not included in the
   *                          set of entries that were returned, or if a search
   *                          result listener was used which makes the
   *                          determination impossible.
   */
  protected SearchResultEntry assertEntryReturned(final SearchResult result,
                                                  final String dn)
            throws LDAPException, AssertionError
  {
    return LDAPTestUtils.assertEntryReturned(result, dn);
  }



  /**
   * Ensures that the specified search result entry was included in provided
   * search exception.
   *
   * @param  exception  The search exception to examine.
   * @param  dn         The DN of the entry expected to be included in the
   *                    search exception.
   *
   * @return  The search result entry with the provided DN.
   *
   * @throws  LDAPException  If the provided string cannot be parsed as a valid
   *                         DN.
   *
   * @throws  AssertionError  If the specified entry was not included in the
   *                          set of entries that were returned, or if a search
   *                          result listener was used which makes the
   *                          determination impossible.
   */
  protected SearchResultEntry assertEntryReturned(
                                   final LDAPSearchException exception,
                                   final String dn)
            throws LDAPException, AssertionError
  {
    return LDAPTestUtils.assertEntryReturned(exception, dn);
  }



  /**
   * Ensures that the provided search result indicates that no search result
   * entries were returned.
   *
   * @param  result  The search result to examine.
   *
   * @throws  AssertionError  If the provided search result indicates that one
   *                          or more entries were returned.
   */
  protected void assertNoEntriesReturned(final SearchResult result)
            throws AssertionError
  {
    LDAPTestUtils.assertNoEntriesReturned(result);
  }



  /**
   * Ensures that the provided search exception indicates that no search result
   * entries were returned.
   *
   * @param  exception  The search exception to examine.
   *
   * @throws  AssertionError  If the provided search exception indicates that
   *                          one or more entries were returned.
   */
  protected void assertNoEntriesReturned(final LDAPSearchException exception)
            throws AssertionError
  {
    LDAPTestUtils.assertNoEntriesReturned(exception);
  }



  /**
   * Ensures that the provided search result indicates that the expected number
   * of entries were returned.
   *
   * @param  result              The search result to examine.
   * @param  expectedEntryCount  The number of expected search result entries.
   *
   * @throws  AssertionError  If the number of entries returned does not match
   *                          the expected value.
   */
  protected void assertEntriesReturnedEquals(final SearchResult result,
                                             final int expectedEntryCount)
            throws AssertionError
  {
    LDAPTestUtils.assertEntriesReturnedEquals(result, expectedEntryCount);
  }



  /**
   * Ensures that the provided search exception indicates that the expected
   * number of entries were returned.
   *
   * @param  exception           The search exception to examine.
   * @param  expectedEntryCount  The number of expected search result entries.
   *
   * @throws  AssertionError  If the number of entries returned does not match
   *                          the expected value.
   */
  protected void assertEntriesReturnedEquals(
                      final LDAPSearchException exception,
                      final int expectedEntryCount)
            throws AssertionError
  {
    LDAPTestUtils.assertEntriesReturnedEquals(exception, expectedEntryCount);
  }



  /**
   * Ensures that the provided search result indicates that at least one search
   * result reference was returned.
   *
   * @param  result  The search result to examine.
   *
   * @return  The number of search result references that were returned.
   *
   * @throws  AssertionError  If the provided search result indicates that no
   *                          references were returned.
   */
  protected int assertReferenceReturned(final SearchResult result)
            throws AssertionError
  {
    return LDAPTestUtils.assertReferenceReturned(result);
  }



  /**
   * Ensures that the provided search exception indicates that at least one
   * search result reference was returned.
   *
   * @param  exception  The search exception to examine.
   *
   * @return  The number of search result references that were returned.
   *
   * @throws  AssertionError  If the provided search exception indicates that no
   *                          references were returned.
   */
  protected int assertReferenceReturned(final LDAPSearchException exception)
            throws AssertionError
  {
    return LDAPTestUtils.assertReferenceReturned(exception);
  }



  /**
   * Ensures that the provided search result indicates that no search result
   * references were returned.
   *
   * @param  result  The search result to examine.
   *
   * @throws  AssertionError  If the provided search result indicates that one
   *                          or more references were returned.
   */
  protected void assertNoReferencesReturned(final SearchResult result)
            throws AssertionError
  {
    LDAPTestUtils.assertNoReferencesReturned(result);
  }



  /**
   * Ensures that the provided search exception indicates that no search result
   * references were returned.
   *
   * @param  exception  The search exception to examine.
   *
   * @throws  AssertionError  If the provided search exception indicates that
   *                          one or more references were returned.
   */
  protected void assertNoReferencesReturned(final LDAPSearchException exception)
            throws AssertionError
  {
    LDAPTestUtils.assertNoReferencesReturned(exception);
  }



  /**
   * Ensures that the provided search result indicates that the expected number
   * of references were returned.
   *
   * @param  result                  The search result to examine.
   * @param  expectedReferenceCount  The number of expected search result
   *                                 references.
   *
   * @throws  AssertionError  If the number of references returned does not
   *                          match the expected value.
   */
  protected void assertReferencesReturnedEquals(final SearchResult result,
                      final int expectedReferenceCount)
            throws AssertionError
  {
    LDAPTestUtils.assertReferencesReturnedEquals(result,
         expectedReferenceCount);
  }



  /**
   * Ensures that the provided search exception indicates that the expected
   * number of references were returned.
   *
   * @param  exception               The search exception to examine.
   * @param  expectedReferenceCount  The number of expected search result
   *                                 references.
   *
   * @throws  AssertionError  If the number of references returned does not
   *                          match the expected value.
   */
  protected void assertReferencesReturnedEquals(
                      final LDAPSearchException exception,
                      final int expectedReferenceCount)
            throws AssertionError
  {
    LDAPTestUtils.assertReferencesReturnedEquals(exception,
         expectedReferenceCount);
  }



  /**
   * Ensures that the provided condition is true.
   *
   * @param  condition  The condition to ensure is true.
   *
   * @throws  AssertionError  If the condition is not true.
   */
  protected static void assertTrue(final boolean condition)
            throws AssertionError
  {
    Assert.assertTrue(condition);
  }



  /**
   * Ensures that the provided condition is true.
   *
   * @param  condition  The condition to ensure is true.
   * @param  message    The message to use if the condition is not true.
   *
   * @throws  AssertionError  If the condition is not true.
   */
  protected static void assertTrue(final boolean condition,
                                   final String message)
            throws AssertionError
  {
    Assert.assertTrue(condition, message);
  }



  /**
   * Ensures that the provided condition is false.
   *
   * @param  condition  The condition to ensure is false.
   *
   * @throws  AssertionError  If the condition is not false.
   */
  protected static void assertFalse(final boolean condition)
            throws AssertionError
  {
    Assert.assertFalse(condition);
  }



  /**
   * Ensures that the provided condition is false.
   *
   * @param  condition  The condition to ensure is false.
   * @param  message    The message to use if the condition is not false.
   *
   * @throws  AssertionError  If the condition is not false.
   */
  protected static void assertFalse(final boolean condition,
                                    final String message)
            throws AssertionError
  {
    Assert.assertFalse(condition, message);
  }



  /**
   * Ensures that the provided object is {@code null}.
   *
   * @param  o  The object for which to make the determination.
   *
   * @throws  AssertionError  If the provided object is not {@code null}.
   */
  protected static void assertNull(final Object o)
            throws AssertionError
  {
    Assert.assertNull(o);
  }



  /**
   * Ensures that the provided object is {@code null}.
   *
   * @param  o        The object for which to make the determination.
   * @param  message  The message to use if the object is not {@code null}.
   *
   * @throws  AssertionError  If the provided object is not {@code null}.
   */
  protected static void assertNull(final Object o, final String message)
            throws AssertionError
  {
    Assert.assertNull(o, message);
  }



  /**
   * Ensures that the provided object is not {@code null}.
   *
   * @param  o  The object for which to make the determination.
   *
   * @throws  AssertionError  If the provided object is {@code null}.
   */
  protected static void assertNotNull(final Object o)
            throws AssertionError
  {
    Assert.assertNotNull(o);
  }



  /**
   * Ensures that the provided object is not {@code null}.
   *
   * @param  o        The object for which to make the determination.
   * @param  message  The message to use if the object is {@code null}.
   *
   * @throws  AssertionError  If the provided object is {@code null}.
   */
  protected static void assertNotNull(final Object o, final String message)
            throws AssertionError
  {
    Assert.assertNotNull(o, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final boolean actual,
                                     final boolean expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final boolean actual,
                                     final boolean expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final byte actual,
                                     final byte expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final byte actual,
                                     final byte expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final byte[] actual,
                                     final byte[] expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final byte[] actual,
                                     final byte[] expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final char actual,
                                     final char expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final char actual,
                                     final char expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final double actual,
                                     final double expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final double actual,
                                     final double expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final float actual,
                                     final float expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final float actual,
                                     final float expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final int actual,
                                     final int expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final int actual,
                                     final int expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final long actual,
                                     final long expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final long actual,
                                     final long expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final short actual,
                                     final short expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final short actual,
                                     final short expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final String actual,
                                     final String expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final String actual,
                                     final String expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided values are logically equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final Object actual,
                                     final Object expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided values are logically equal.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final Object actual,
                                     final Object expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided arrays contain values which are logically equal
   * and in the same order.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final Object[] actual,
                                     final Object[] expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided arrays contain values which are logically equal
   * and in the same order.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final Object[] actual,
                                     final Object[] expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided collections contain values which are logically
   * equal and in the same order.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final Collection<?> actual,
                                     final Collection<?> expected)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected);
  }



  /**
   * Ensures that the provided collections contain values which are logically
   * equal and in the same order.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEquals(final Collection<?> actual,
                                     final Collection<?> expected,
                                     final String message)
            throws AssertionError
  {
    Assert.assertEquals(actual, expected, message);
  }



  /**
   * Ensures that the provided arrays contain values which are logically
   * equivalent.  The order in which the elements occur is irrelevant.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEqualsNoOrder(final Object[] actual,
                                            final Object[] expected)
            throws AssertionError
  {
    Assert.assertEqualsNoOrder(actual, expected);
  }



  /**
   * Ensures that the provided arrays contain values which are logically
   * equivalent.  The order in which the elements occur is irrelevant.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use if the values are not equal.
   *
   * @throws  AssertionError  If the provided values are not equal.
   */
  protected static void assertEqualsNoOrder(final Object[] actual,
                                            final Object[] expected,
                                            final String message)
            throws AssertionError
  {
    Assert.assertEqualsNoOrder(actual, expected, message);
  }



  /**
   * Ensures that the provided objects are references to the same element.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   *
   * @throws  AssertionError  If the provided objects are not the same.
   */
  protected static void assertSame(final Object actual, final Object expected)
            throws AssertionError
  {
    Assert.assertSame(actual, expected);
  }



  /**
   * Ensures that the provided objects are references to the same element.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The expected value.
   * @param  message   The message to use for the assertion error if the objects
   *                   are not the same.
   *
   * @throws  AssertionError  If the provided objects are not the same.
   */
  protected static void assertSame(final Object actual, final Object expected,
                                   final String message)
            throws AssertionError
  {
    Assert.assertSame(actual, expected, message);
  }



  /**
   * Ensures that the provided objects are references to different elements.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The value expected to be different.
   *
   * @throws  AssertionError  If the provided objects are the same.
   */
  protected static void assertNotSame(final Object actual,
                                      final Object expected)
            throws AssertionError
  {
    Assert.assertNotSame(actual, expected);
  }



  /**
   * Ensures that the provided objects are references to different elements.
   *
   * @param  actual    The actual value encountered.
   * @param  expected  The value expected to be different.
   * @param  message   The message to use for the assertion error if the objects
   *                   are the same.
   *
   * @throws  AssertionError  If the provided objects are the same.
   */
  protected static void assertNotSame(final Object actual,
                                      final Object expected,
                                      final String message)
            throws AssertionError
  {
    Assert.assertNotSame(actual, expected, message);
  }



  /**
   * Throws an {@code AssertionError}.
   *
   * @throws  AssertionError  Always.
   */
  protected static void fail()
            throws AssertionError
  {
    Assert.fail();
  }



  /**
   * Throws an {@code AssertionError}.
   *
   * @param  message  The message to use for the {@code AssertionError}.
   *
   * @throws  AssertionError  Always.
   */
  protected static void fail(final String message)
            throws AssertionError
  {
    Assert.fail(message);
  }



  /**
   * Throws an {@code AssertionError}.
   *
   * @param  message  The message to use for the {@code AssertionError}.
   * @param  cause    The exception that triggered the failure.
   *
   * @throws  AssertionError  Always.
   */
  protected static void fail(final String message, final Throwable cause)
            throws AssertionError
  {
    Assert.fail(message, cause);
  }



  /**
   * Selects a port that may be used for a local service.
   *
   * @return  The port number that may be used.
   *
   * @throws IOException  If a problem occurs trying to identify a port that
   *                      may be used.
   */
  protected static int getFreePort()
      throws IOException
  {
    final ServerSocket socket = bindFreePort();

    try
    {
      return socket.getLocalPort();
    }
    finally
    {
      socket.close();
    }
  }



  /**
   * Determine a free server socket port on the local host and bind to it.
   *
   * @return The bound server socket.
   *
   * @throws IOException  If a problem occurs trying to identify a port that
   *                      may be used.
   */
  private static ServerSocket bindFreePort() throws IOException
  {
    ServerSocket serverSocket;
    serverSocket = new ServerSocket();
    serverSocket.setReuseAddress(true);
    serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));

    return serverSocket;
  }



  /**
   * Obtain a reference to a file anywhere under the scim root directory.
   *
   * @param path  The relative path to the desired file.
   *
   * @return  A reference to the desired file.
   */
  protected static File getFile(final String path)
  {
    final File baseDir = new File(System.getProperty("main.basedir"));
    return new File(baseDir, path);
  }

  /**
   * Retrieve the stats for a given resource.
   *
   * @param resourceName  The name of the resource.
   *
   * @return  The stats for the requested resource.
   */
  protected static ResourceStats getStatsForResource(String resourceName)
  {
    return scimApplication.getStatsForResource(resourceName);
  }

}
