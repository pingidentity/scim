/*
 * Copyright 2011-2015 UnboundID Corp.
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
package com.unboundid.scim.sdk.examples;

import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.Manager;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.PreemptiveAuthInterceptor;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMService;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.httpclient.ApacheHttpClientConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * A simple client example.
 */
public class ClientExample {

  /**
   * A device resource extension.
   */
  public static class DeviceResource extends BaseResource
  {
    /**
     * Create a new empty device resource.
     *
     * @param resourceDescriptor The resource descriptor of this resource.
     */
    public DeviceResource(final ResourceDescriptor resourceDescriptor) {
      super(resourceDescriptor);
    }

    /**
     * Create a device resource based on the provided SCIMObject.
     *
     * @param resourceDescriptor The resource descriptor of this resource.
     * @param scimObject The SCIMObject containing all the attributes and
     * values.
     */
    public DeviceResource(final ResourceDescriptor resourceDescriptor,
                          final SCIMObject scimObject) {
      super(resourceDescriptor, scimObject);
    }

    /**
     * Retrieves the vendor name of this device.
     *
     * @return The vendor name of this device.
     */
    public String getVendorName()
    {
      return getSingularAttributeValue("urn:com:example:device:1.0",
          "vendorName", AttributeValueResolver.STRING_RESOLVER);
    }
  }

  /**
   * The resource factory that can be used to create device resource instances.
   */
  public static final ResourceFactory<DeviceResource> DEVICE_RESOURCE_FACTORY =
      new ResourceFactory<DeviceResource>() {
        /**
         * {@inheritDoc}
         */
        public DeviceResource createResource(
            final ResourceDescriptor resourceDescriptor,
            final SCIMObject scimObject) {
          return new DeviceResource(resourceDescriptor, scimObject);
        }
      };

  /**
   * The main method.
   *
   * @param args Parameters for the application.
   * @throws Exception If an error occurs.
   */
  public static void main(final String[] args) throws Exception {
    final URI uri = URI.create("https://localhost:8443");
    final ClientConfig clientConfig =
        createHttpBasicClientConfig("bjensen", "password");
    final SCIMService scimService = new SCIMService(uri, clientConfig);
    scimService.setAcceptType(MediaType.APPLICATION_JSON_TYPE);

    // Core user resource CRUD operation example
    final SCIMEndpoint<UserResource> endpoint = scimService.getUserEndpoint();

    // Query for a specified user
    Resources<UserResource> resources =
        endpoint.query("userName eq \"bjensen\"");
    if (resources.getItemsPerPage() == 0) {
      System.out.println("User bjensen not found");
      return;
    }
    UserResource user = resources.iterator().next();

    Name name = user.getName();
    if (name != null) {
      System.out.println(name);
    }

    Collection<Entry<String>> phoneNumbers = user.getPhoneNumbers();
    if(phoneNumbers != null) {
      for(Entry<String> phoneNumber : phoneNumbers) {
        System.out.println(phoneNumber);
      }
    }

    // Attribute extension example
    Manager manager = user.getSingularAttributeValue(
        "urn:scim:schemas:extension:enterprise:1.0",  "manager",
        Manager.MANAGER_RESOLVER);
    if(manager == null) {
      resources = endpoint.query("userName eq \"jsmith\"");
      if (resources.getItemsPerPage() > 0) {
        UserResource boss = resources.iterator().next();
        manager = new Manager(boss.getId(), null);
      } else {
        System.out.println("User jsmith not found");
      }
    }

    user.setSingularAttributeValue("urn:scim:schemas:extension:enterprise:1.0",
        "manager", Manager.MANAGER_RESOLVER, manager);

    String employeeNumber =
        user.getSingularAttributeValue(
            "urn:scim:schemas:extension:enterprise:1.0",  "employeeNumber",
            AttributeValueResolver.STRING_RESOLVER);
    if (employeeNumber != null) {
      System.out.println("employeeNumber: " + employeeNumber);
    }

    user.setSingularAttributeValue("urn:scim:schemas:extension:enterprise:1.0",
        "department",  AttributeValueResolver.STRING_RESOLVER, "sales");

    user.setTitle("Vice President");

    // Update the user
    endpoint.update(user);

    // Demonstrate retrieval by SCIM ID
    user = endpoint.get(user.getId());

    // Resource type extension example
    ResourceDescriptor deviceDescriptor =
        scimService.getResourceDescriptor("Device", null);
    SCIMEndpoint<DeviceResource> deviceEndpoint =
        scimService.getEndpoint(deviceDescriptor, DEVICE_RESOURCE_FACTORY);
  }



  /**
   * Create an SSL-enabled Wink client config from the provided information.
   * The returned client config may be used to create a SCIM service object.
   * IMPORTANT: This should not be used in production because no validation
   * is performed on the server certificate returned by the SCIM service.
   *
   * @param userName    The HTTP Basic Auth user name.
   * @param password    The HTTP Basic Auth password.
   *
   * @return  An Apache Wink client config.
   */
  public static ClientConfig createHttpBasicClientConfig(
      final String userName, final String password) {
    SSLSocketFactory sslSocketFactory;
    try {
      final SSLContext sslContext = SSLContext.getInstance("TLS");

      // Do not use these settings in production.
      sslContext.init(null,
                      new TrustManager[] { new BlindTrustManager() },
                      new SecureRandom());
      sslSocketFactory = new SSLSocketFactory(
              sslContext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    } catch (KeyManagementException e) {
      throw new RuntimeException(e.getLocalizedMessage());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e.getLocalizedMessage());
    }

    final HttpParams params = new BasicHttpParams();
    DefaultHttpClient.setDefaultHttpParams(params);
    params.setBooleanParameter(CoreConnectionPNames.SO_REUSEADDR, true);
    params.setBooleanParameter(CoreProtocolPNames.USE_EXPECT_CONTINUE, true);
    params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK,
                               true);

    final SchemeRegistry schemeRegistry = new SchemeRegistry();
    schemeRegistry.register(new Scheme(
            "http", 80, PlainSocketFactory.getSocketFactory()));
    schemeRegistry.register(new Scheme("https", 443, sslSocketFactory));

    final PoolingClientConnectionManager mgr =
            new PoolingClientConnectionManager(schemeRegistry);
    mgr.setMaxTotal(200);
    mgr.setDefaultMaxPerRoute(20);

    final DefaultHttpClient httpClient = new DefaultHttpClient(mgr, params);

    final Credentials credentials =
            new UsernamePasswordCredentials(userName, password);
    httpClient.getCredentialsProvider().setCredentials(AuthScope.ANY,
                                                       credentials);
    httpClient.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);

    ClientConfig clientConfig = new ApacheHttpClientConfig(httpClient);
    clientConfig.setBypassHostnameVerification(true);

    return clientConfig;
  }



  /**
   * An X509TrustManager which trusts everything. Do not use in production.
   */
  static class BlindTrustManager implements X509TrustManager {
    /**
     * {@inheritDoc}
     */
    public void checkClientTrusted(final X509Certificate[] chain,
                                   final String authType)
        throws CertificateException {
    }

    /**
     * {@inheritDoc}
     */
    public void checkServerTrusted(final X509Certificate[] chain,
                                   final String authType)
        throws CertificateException {
    }

    /**
     * {@inheritDoc}
     */
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }
}
