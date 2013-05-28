/*
 * Copyright 2011-2013 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.extension;

import com.unboundid.directory.tests.externalinstance.CommandOutput;
import com.unboundid.directory.tests.externalinstance.DirectoryInstance;
import com.unboundid.directory.tests.externalinstance.ExternalInstance;
import com.unboundid.directory.tests.externalinstance.ExternalInstanceManager;
import com.unboundid.directory.tests.externalinstance.TestCaseUtils;
import com.unboundid.directory.tests.externalinstance.standalone.
    ExternalInstanceIdImpl;
import com.unboundid.directory.tests.externalinstance.util.FutureCondition;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.AuthenticationScheme;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.GroupResource;
import com.unboundid.scim.data.Manager;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.ServiceProviderConfig;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.CoreSchema;
import com.unboundid.scim.sdk.BulkOperation;
import com.unboundid.scim.sdk.BulkResponse;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.ResourceConflictException;
import com.unboundid.scim.sdk.ResourceNotFoundException;
import com.unboundid.scim.sdk.Resources;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMException;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMService;
import com.unboundid.scim.sdk.SortParameters;
import com.unboundid.scim.sdk.Status;
import com.unboundid.scim.sdk.UnauthorizedException;
import com.unboundid.scim.sdk.Version;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.apache.wink.client.ClientConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;



/**
 * Test coverage for the SCIM server extension. This class tests the extension
 * running in the Directory Server, and serves as a base class to allow the
 * extension to be tested in the Proxy Server.
 *
 * NOTE: The SCIM-Extension module is being deprecated and moved into the core
 *       build so please do not add new test cases in this class. Instead, add
 *       them to the SCIMServerTestCase class in the SCIM-RI module. If the test
 *       case will only work with a full Directory Server, please add them to
 *       the SCIMHTTPServletExtensionTest class in the core build.
 */
@Test(sequential = true, enabled = false)
public class SCIMExtensionTestCase extends ServerExtensionTestCase
{
  /**
   * The Directory Server external instance.
   */
  protected DirectoryInstance dsInstance;

  /**
   * A reference to the external instance where the SCIM extension is installed.
   */
  protected ExternalInstance scimInstance;

  /**
   * The SCIM service client.
   */
  protected SCIMService service;

  /**
   * The SCIM secure service client.
   */
  protected SCIMService secureService;

  /**
   * Base DN of the Directory Server (for Users).
   */
  protected String userBaseDN;

  /**
   * Base DN of the Directory Server (for Groups).
   */
  protected String groupBaseDN;

  /**
   * The non-secure SCIM port.
   */
  protected int scimPort;

  /**
   * The secure SCIM port.
   */
  protected int scimSecurePort;

  /**
   * The SSLContext to use.
   */
  protected SSLContext sslContext;


  /**
   * Set up SCIM services before running the tests.
   *
   * @throws Exception  If an error occurs.
   */
  protected void setupServices() throws Exception
  {
    scimPort = TestCaseUtils.getFreePort();
    scimSecurePort = TestCaseUtils.getFreePort();
    configureExtension(scimInstance, scimPort, scimSecurePort);

    // Use the external instance trust store as our client trust store.
    final File pwFile =
        scimInstance.fileRelativeToInstanceRoot("config/truststore.pin");
    final char[] pw =
        TestCaseUtils.readFileToLines(pwFile).get(0).toCharArray();
    final KeyManager keyManager = null;
    final TrustManager trustManager =
        new TrustStoreTrustManager(
            scimInstance.fileRelativeToInstanceRoot("config/truststore"),
            pw, "JKS", true);

    final SSLUtil sslUtil = new SSLUtil(keyManager, trustManager);
    sslContext = sslUtil.createSSLContext();
//    final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());

    final ClientConfig clientConfig =
        createManagerClientConfig(scimInstance, sslContext);

    final URI uri = new URI("http", null, scimInstance.getLdapHost(), scimPort,
                            null, null, null);
    service = new SCIMService(uri, clientConfig);

    final URI secureUri = new URI("https", null, scimInstance.getLdapHost(),
                                  scimSecurePort, null, null, null);
    secureService = new SCIMService(secureUri, clientConfig);

    dsInstance.dsconfig(
        "set-access-control-handler-prop",
        "--add", "global-aci:" +
                 "(targetcontrol=\"1.3.6.1.1.13.2 || 1.2.840.113556.1.4.473 " +
                 "|| 2.16.840.1.113730.3.4.9\")" +
                 "(version 3.0;" +
                 "acl \"Authenticated access to controls used by the " +
                 "SCIM plugin\"; allow (all) userdn=\"ldap:///all\";)",
        "--add", "global-aci:" +
                 "(targetattr=\"entryUUID || ds-entry-unique-id || " +
                 "createTimestamp || ds-create-time || " +
                 "modifyTimestamp || ds-update-time\")" +
                 "(version 3.0;" +
                 "acl \"Authenticated read access to operational attributes " +
                 "used by the SCIM extension\"; " +
                 "allow (read,search,compare) userdn=\"ldap:///all\";)",
        "--add", "global-aci:" +
                 "(targetattr!=\"userPassword\")" +
                 "(version 3.0; " +
                 "acl \"Allow anonymous read access for anyone\"; " +
                 "allow (read,search,compare) userdn=\"ldap:///anyone\";)",
        "--add", "global-aci:" +
                 "(targetattr=\"*\")" +
                 "(version 3.0; " +
                 "acl \"Allow users to update their own entries\"; " +
                 "allow (write) userdn=\"ldap:///self\";)"
        );
  }



  /**
   * Set up before running the tests.
   *
   * @throws Exception  If an error occurs.
   */
  @BeforeClass(alwaysRun = true)
  public void setup() throws Exception
  {
    final ExternalInstanceManager m = ExternalInstanceManager.singleton();

    dsInstance = m.getExternalInstance(
        ExternalInstanceIdImpl.BasicDirectoryServer);

    dsInstance.runSetup("--ldapsPort",
        String.valueOf(TestCaseUtils.getFreePort()),
        "--generateSelfSignedCertificate",
        "--doNotStart");

    final File extensionZipFile =
        new File(System.getProperty("extensionZipFile"));
    dsInstance.installExtension(extensionZipFile);

    dsInstance.startInstance();
    dsInstance.addBaseEntry();
    dsInstance.addEntry("dn: ou=people," + dsInstance.getPrimaryBaseDN(),
                        "objectClass: organizationalUnit",
                        "ou: people");

    groupBaseDN = dsInstance.getPrimaryBaseDN();
    userBaseDN = "ou=people," + dsInstance.getPrimaryBaseDN();

    dsInstance.getDnsToDumpOnFailureMutable().add("cn=monitor");

    dsInstance.dsconfig(
        "set-plugin-prop",
        "--plugin-name", "Processing Time Histogram",
        "--reset", "invoke-for-internal-operations");

    dsInstance.dsconfig(
        "set-log-publisher-prop",
        "--publisher-name", "File-Based Access Logger",
        "--set", "suppress-internal-operations:false"
    );

    dsInstance.dsconfig(
        "set-group-implementation-prop",
        "--implementation-name", "Static",
        "--set", "cache-user-to-group-mappings:false"
    );

    dsInstance.dsconfig(
        "set-virtual-attribute-prop",
        "--name", "Virtual Static member",
        "--set", "enabled:true",
        "--set", "allow-retrieving-membership:true"
    );

    dsInstance.dsconfig(
        "set-virtual-attribute-prop",
        "--name", "Virtual Static uniqueMember",
        "--set", "enabled:true",
        "--set", "allow-retrieving-membership:true"
    );

    dsInstance.restartInstance();

    scimInstance = dsInstance;
    setupServices();

    //
    //This can be helpful if you want to make SCIM requests from your browser
    //or some other REST client:
    //
    //JOptionPane.showConfirmDialog(null, "SCIM is listening at: " +
    //        uri.toString(), null, JOptionPane.OK_CANCEL_OPTION);

  }



  /**
   * Tear down after the tests are finished.
   */
  @AfterClass(alwaysRun = true)
  public void tearDown()
  {
    //
    //This can be helpful if you want to attach a debugger to the external
    //instance that is running the SCIM server. Just wait for the dialog to pop
    //up, then navigate to the external instance root, stop it, modify the
    //java.properties to include -Xdebug, etc, and then start it back up. You
    //can leave the dialogue up until you are finished debugging, as it will
    //block this thread.
    //
    //JOptionPane.showConfirmDialog(null, "About to shutdown...", null,
    //                                JOptionPane.OK_CANCEL_OPTION);

    final ExternalInstanceManager m = ExternalInstanceManager.singleton();
    dsInstance.stopInstance();
    m.destroyInstance(dsInstance.getInstanceId());
  }



  /**
   * Tests that the plugin can be installed and enabled.
   *
   * @throws Exception  If the test fails.
   */
  @Test(enabled = false)
  public void enablePlugin() throws Exception
  {
    assertEquals(getMonitorAsString(scimInstance, "version"), Version.VERSION);
  }


  /**
   * Tests the groups attribute and makes sure the type sub-attribute is
   * correct.
   *
   * @throws Exception  If the test fails.
   */
  @Test(enabled = false)
  public void testGroupsAttribute() throws Exception
  {
    dsInstance.addEntry("dn: uid=testGroupMember1," + userBaseDN,
        "objectclass: top",
        "objectclass: person",
        "objectclass: organizationalPerson",
        "objectclass: inetOrgPerson",
        "uid: testGroupMember1",
        "cn: testGroupMember1",
        "givenname: Test1",
        "sn: User");

    dsInstance.addEntry("dn: uid=testGroupMember2," + userBaseDN,
        "objectclass: top",
        "objectclass: person",
        "objectclass: organizationalPerson",
        "objectclass: inetOrgPerson",
        "uid: testGroupMember2",
        "cn: testGroupMember2",
        "givenname: Test2",
        "sn: User");

    String testGroupBaseDN = "ou=groups," + groupBaseDN;
    dsInstance.addEntry("dn: " + testGroupBaseDN,
        "objectClass: organizationalUnit",
        "ou: groups");

    dsInstance.addEntry("dn: cn=testGroup1," + testGroupBaseDN,
        "objectclass: top",
        "objectclass: groupOfNames",
        "cn: testGroup1",
        "member: uid=testGroupMember1," + userBaseDN);

    dsInstance.addEntry("dn: cn=testGroup2," + testGroupBaseDN,
        "objectclass: top",
        "objectclass: groupOfUniqueNames",
        "cn: testGroup2",
        "uniqueMember: uid=testGroupMember2," + userBaseDN,
        "uniqueMember: cn=testGroup1," + testGroupBaseDN);

    dsInstance.addEntry("dn: cn=testGroup3," + testGroupBaseDN,
        "objectclass: top",
        "objectclass: groupOfURLs",
        "cn: testGroup3",
        "memberURL: ldap:///" + userBaseDN + "??sub?(givenName=Test2)");

    //This group is virtual static
    dsInstance.addEntry("dn: cn=testGroup4," + testGroupBaseDN,
        "objectclass: top",
        "objectclass: groupOfUniqueNames",
        "objectclass: ds-virtual-static-group",
        "cn: testGroup4",
        "ds-target-group-dn: cn=testGroup2," + testGroupBaseDN);

    // Test using the isMemberOf attribute.
    testGroupsInternal();

    // Now test without using the isMemberOf attribute.
    dsInstance.dsconfig(
        "set-virtual-attribute-prop",
        "--name", "isMemberOf",
        "--set", "enabled:false"
    );

    testGroupsInternal();

    //Cleanup
    dsInstance.dsconfig(
        "set-virtual-attribute-prop",
        "--name", "isMemberOf",
        "--set", "enabled:true"
    );
  }



  /**
   * Retrieve the user resource that has the provided userName value.
   *
   * @param userName  The userName of the user to be retrieved.
   *
   * @return  The user resource.
   *
   * @throws SCIMException  If the resource could not be retrieved.
   */
  private UserResource getUser(final String userName)
      throws SCIMException
  {
    SCIMEndpoint<UserResource> endpoint = service.getUserEndpoint();

    final Resources<UserResource> resources =
        endpoint.query("userName eq \"" + userName + "\"");

    return resources.iterator().next();
  }


  /**
   * Retrieve the group resource that has the provided displayName value.
   *
   * @param displayName  The displayName of the group to be retrieved.
   *
   * @return  The user resource.
   *
   * @throws SCIMException  If the resource could not be retrieved.
   */
  private GroupResource getGroup(final String displayName)
      throws SCIMException
  {
    SCIMEndpoint<GroupResource> endpoint = service.getGroupEndpoint();

    final Resources<GroupResource> resources =
        endpoint.query("displayName eq \"" + displayName + "\"");

    return resources.iterator().next();
  }


  /**
   * Tests the groups attribute and makes sure the type sub-attribute is
   * correct.
   *
   * @throws Exception  If the test fails.
   */
  private void testGroupsInternal() throws Exception
  {
    final GroupResource group1 = getGroup("testGroup1");
    final GroupResource group2 = getGroup("testGroup2");
    final GroupResource group3 = getGroup("testGroup3");
    final GroupResource group4 = getGroup("testGroup4");

    UserResource user = getUser("testGroupMember1");
    assertEquals(user.getGroups().size(), 3);
    for(Entry<String> group : user.getGroups())
    {
      if(group.getValue().equals(group1.getId()))
      {
        //Group 1 is a static group
        assertEquals(group.getDisplay(), "testGroup1");
        assertTrue(group.getType().equalsIgnoreCase("direct"));
      }
      else if(group.getValue().equals(group2.getId()))
      {
        //Group 2 contains Group 1 (thus the type is indirect)
        assertEquals(group.getDisplay(), "testGroup2");
        assertTrue(group.getType().equalsIgnoreCase("indirect"));
      }
      else if(group.getValue().equals(group4.getId()))
      {
        //Group 4 is a virtual static group
        assertEquals(group.getDisplay(), "testGroup4");
        assertTrue(group.getType().equalsIgnoreCase("indirect"));
      }
      else
      {
        fail("Group ID should be testGroup1, testGroup2, or testGroup4");
      }
    }

    user = getUser("testGroupMember2");
    assertEquals(user.getGroups().size(), 3);
    for(Entry<String> group : user.getGroups())
    {
      if(group.getValue().equals(group2.getId()))
      {
        //Group 2 is a static group
        assertEquals(group.getDisplay(), "testGroup2");
        assertTrue(group.getType().equalsIgnoreCase("direct"));
      }
      else if(group.getValue().equals(group3.getId()))
      {
        //Group 3 is a dynamic group
        assertEquals(group.getDisplay(), "testGroup3");
        assertTrue(group.getType().equalsIgnoreCase("indirect"));
      }
      else if(group.getValue().equalsIgnoreCase(group4.getId()))
      {
        //Group 4 is a virtual static group
        assertEquals(group.getDisplay(), "testGroup4");
        assertTrue(group.getType().equalsIgnoreCase("indirect"));
      }
      else
      {
        assertTrue(false, "Group ID should be testGroup2, testGroup3, " +
                            "or testGroup4");
      }
    }
  }

  /**
   * Tests modifying a user password through SCIM using modify.
   *
   * @throws Exception  If the test fails.
   */
  @Test(enabled = false)
  public void testPasswordModify() throws Exception
  {
    //Add an entry to the Directory
    dsInstance.addEntry("dn: uid=testPasswordModify," + userBaseDN,
                      "objectclass: top",
                      "objectclass: person",
                      "objectclass: organizationalPerson",
                      "objectclass: inetOrgPerson",
                      "uid: testPasswordModify",
                      "userPassword: oldPassword",
                      "cn: testPasswordModify",
                      "givenname: Test",
                      "sn: User");

    //Update the entry via SCIM
    final ClientConfig clientConfig =
        createClientConfig(scimInstance.getLdapHost(),
            "uid=testPasswordModify," + userBaseDN, "oldPassword", sslContext);
    final URI uri = new URI("http", null, scimInstance.getLdapHost(), scimPort,
                            null, null, null);
    SCIMService userService = new SCIMService(uri, clientConfig);
    SCIMEndpoint<UserResource> userEndpoint = userService.getUserEndpoint();
    UserResource user = getUser("testPasswordModify");
    assertNotNull(user);

    //Verify that not including the password attribute in the PUT will not
    //affect the current value
    user.setPassword(null);
    user.setTitle("Engineer");
    user = userEndpoint.update(user);
    assertNotNull(user);
    assertEquals(user.getTitle(), "Engineer");
    dsInstance.checkCredentials("uid=testPasswordModify," + userBaseDN,
                                      "oldPassword");

    //Now change the password
    user.setPassword("anotherPassword");

    UserResource returnedUser = userEndpoint.update(user, "id");

    //Verify what is returned from the SDK
    assertEquals(returnedUser.getId(), user.getId());
    assertNull(returnedUser.getPassword());

    //We shouldn't be able to use this service anymore since it is using
    //the old credentials
    try
    {
      userEndpoint.get(user.getId());
      assertTrue(false, "Expected Unauthorized return code");
    }
    catch(SCIMException e)
    {
      // Expected.
    }

    //Verify the password was changed in the Directory
    dsInstance.checkCredentials("uid=testPasswordModify," + userBaseDN,
                                      "anotherPassword");
  }



  /**
   * Tests basic resource creation through SCIM.
   *
   * @throws Exception  If the test fails.
   */
  @Test(enabled = false)
  public void testCreate() throws Exception
  {
    //Create a new user
    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    UserResource manager = userEndpoint.newResource();
    manager.setUserName("myManager");
    manager.setName(
        new Name("Mr. Manager", "Manager", null, null, "Mr.", null));
    manager = userEndpoint.create(manager);

    UserResource user = userEndpoint.newResource();
    user.setUserName("jdoe");
    user.setName(
        new Name("John C. Doe", "Doe", "Charles", "John", null, "Sr."));
    user.setPassword("newPassword");
    user.setTitle("Vice President");
    user.setUserType("Employee");
    Collection<Entry<String>> emails = new HashSet<Entry<String>>(1);
    emails.add(new Entry<String>("j.doe@example.com", "work", true));
    user.setEmails(emails);
    user.setLocale(Locale.US.getDisplayName());
    user.setTimeZone(TimeZone.getDefault().getID());
    user.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER,
            new Manager(manager.getId(), "Mr. Manager"));

    //Verify basic properties of UserResource
    assertEquals(user.getUserName(), "jdoe");
    assertEquals(user.getName().getFormatted(), "John C. Doe");
    assertEquals(user.getPassword(), "newPassword");
    assertEquals(user.getTitle(), "Vice President");
    assertEquals(user.getUserType(), "Employee");
    assertEquals(user.getEmails().iterator().next().getValue(),
                          "j.doe@example.com");
    assertEquals(user.getLocale(), Locale.US.getDisplayName());
    assertEquals(user.getTimeZone(), TimeZone.getDefault().getID());
    assertEquals(user.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER).getDisplayName(), "Mr. Manager");

    //Mark the start and end time with a 500ms buffer on either side, because
    //the Directory Server will record the actual createTimestamp using the
    //TimeThread, which only updates once every 100ms.
    Date startTime = new Date(System.currentTimeMillis() - 500);
    String beforeCount =
        getMonitorAsString(scimInstance, "user-resource-post-successful");
    UserResource returnedUser = userEndpoint.create(user);
    String afterCount =
        getMonitorAsString(scimInstance, "user-resource-post-successful");
    Date createTime = returnedUser.getMeta().getCreated();
    Date endTime = new Date(System.currentTimeMillis() + 500);

    try
    {
      //Try to create the same user again
      userEndpoint.create(user);
      fail("Expected a 409 response when trying to create duplicate user");
    }
    catch (ResourceConflictException e)
    {
      //expected (error code is 409)
    }

    //Verify what is returned from the SDK
    assertNotNull(returnedUser);
    assertTrue(returnedUser.getMeta().getLocation().toString().endsWith(
        returnedUser.getId()));
    assertEquals(returnedUser.getUserName(), user.getUserName());
    assertEquals(returnedUser.getName().getFormatted(),
                   user.getName().getFormatted());
    assertNull(returnedUser.getPassword());
    assertEquals(returnedUser.getTitle(), user.getTitle());
    assertEquals(returnedUser.getUserType(), user.getUserType());
    assertEquals(returnedUser.getEmails().iterator().next().getValue(),
        user.getEmails().iterator().next().getValue());
    assertTrue(createTime.after(startTime));
    assertTrue(createTime.before(endTime));
    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 1);

    //TODO: no LDAP schema for Locale
    //assertEquals(returnedUser.getLocale(), user.getLocale());

    //TODO: no LDAP schema for TimeZone
    //assertEquals(returnedUser.getTimeZone(), user.getTimeZone());

    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER).getManagerId(),
                 user.getSingularAttributeValue(
                   SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
                   Manager.MANAGER_RESOLVER).getManagerId());

    //Verify what is actually in the Directory
    SearchResultEntry entry = dsInstance.getConnectionPool().getEntry(
                                          "uid=jdoe," + userBaseDN, "*", "+");
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("iNetOrgPerson"));
    assertEquals(entry.getAttributeValue("cn"), "John C. Doe");
    assertEquals(entry.getAttributeValue("givenName"), "John");
    assertEquals(entry.getAttributeValue("sn"), "Doe");
    assertEquals(entry.getAttributeValue("title"), "Vice President");
    assertEquals(entry.getAttributeValue("mail"), "j.doe@example.com");
    assertTrue(entry.getAttributeValue("manager").equalsIgnoreCase(
        "uid=myManager," + userBaseDN));
    dsInstance.checkCredentials(entry.getDN(), "newPassword");

    System.out.println("Full user entry:\n" + entry.toLDIFString());

    // Verify that a query returns all attributes including extension
    // attributes.
    returnedUser = userEndpoint.query("userName eq \"jdoe\"").iterator().next();
    System.out.println(returnedUser);
    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER).getManagerId(),
                 user.getSingularAttributeValue(
                   SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
                   Manager.MANAGER_RESOLVER).getManagerId());

    //Create a new group
    SCIMEndpoint<GroupResource> grpEndpoint = service.getGroupEndpoint();
    GroupResource group = grpEndpoint.newResource();
    group.setDisplayName("Engineering");
    Collection<Entry<String>> members = new HashSet<Entry<String>>();
    members.add(new Entry<String>(returnedUser.getId(), "User", false));
    group.setMembers(members);

    //Verify the basic properties of GroupResource
    assertEquals(group.getDisplayName(), "Engineering");
    assertEquals(group.getMembers().iterator().next().getValue(),
                          returnedUser.getId());

    //Do the create and verify what is returned from the endpoint
    startTime = new Date(System.currentTimeMillis() - 500);
    beforeCount =
        getMonitorAsString(scimInstance, "group-resource-post-successful");
    GroupResource returnedGroup = grpEndpoint.create(group);
    afterCount =
        getMonitorAsString(scimInstance, "group-resource-post-successful");
    createTime = returnedGroup.getMeta().getCreated();
    endTime = new Date(System.currentTimeMillis() + 500);

    assertNotNull(returnedGroup);
    assertEquals(returnedGroup.getDisplayName(), group.getDisplayName());
    assertTrue(createTime.after(startTime));
    assertTrue(createTime.before(endTime));
    assertEquals(returnedGroup.getMembers().iterator().next().getValue(),
        group.getMembers().iterator().next().getValue());
    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 1);

    //Verify what is actually in the Directory
    SearchResultEntry groupEntry = dsInstance.getConnectionPool().getEntry(
        "cn=Engineering," + groupBaseDN, "*", "+");
    assertNotNull(groupEntry);
    assertTrue(groupEntry.hasObjectClass("groupOfUniqueNames"));
    assertEquals(groupEntry.getAttributeValue("cn"), "Engineering");
    assertTrue(groupEntry.getAttributeValue("uniqueMember").
        equalsIgnoreCase(entry.getDN()), entry.toLDIFString());

    System.out.println("Full group entry:\n" + groupEntry.toLDIFString());
  }



  /**
   * Tests a basic modify operation against a user resource using PUT.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testModifyWithPut() throws Exception
  {
    //Add an entry to the Directory
    dsInstance.addEntry("dn: uid=testModifyWithPut," + userBaseDN,
                      "objectclass: top",
                      "objectclass: person",
                      "objectclass: organizationalPerson",
                      "objectclass: inetOrgPerson",
                      "uid: testModifyWithPut",
                      "userPassword: oldPassword",
                      "cn: testModifyWithPut",
                      "givenname: Test",
                      "sn: User");

    //Update the entry via SCIM
    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    UserResource user = getUser("testModifyWithPut");
    assertNotNull(user);
    //This will change the 'cn' to 'Test User'
    user.setName(new Name("Test User", "User", null, "Test", null, null));
    user.setUserType("Employee");
    user.setPassword("anotherPassword");
    user.setTitle("Chief of Operations");
    user.setDisplayName("Test Modify with PUT");
    user.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
              AttributeValueResolver.STRING_RESOLVER, "456");

    //Mark the start and end time with a 500ms buffer on either side, because
    //the Directory Server will record the actual modifyTimestamp using the
    //TimeThread, which only updates once every 100ms.
    Date startTime = new Date(System.currentTimeMillis() - 500);
    UserResource returnedUser = userEndpoint.update(user);
    Date lastModified = returnedUser.getMeta().getLastModified();
    Date endTime = new Date(System.currentTimeMillis() + 500);

    //Verify what is returned from the SDK
    assertEquals(returnedUser.getId(), user.getId());
    assertTrue(user.getMeta().getLocation().toString().endsWith(user.getId()));
    assertEquals(returnedUser.getUserName(), "testModifyWithPut");
    assertEquals(returnedUser.getName().getFormatted(), "Test User");
    assertEquals(returnedUser.getName().getGivenName(), "Test");
    assertEquals(returnedUser.getName().getFamilyName(), "User");
    assertNull(returnedUser.getPassword());
    assertEquals(returnedUser.getTitle(), user.getTitle());
    assertEquals(returnedUser.getDisplayName(), user.getDisplayName());
    assertTrue(lastModified.after(startTime));
    assertTrue(lastModified.before(endTime));
    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
              AttributeValueResolver.STRING_RESOLVER), "456");

    //Verify the contents of the entry in the Directory
    SearchResultEntry entry =
      dsInstance.getConnectionPool().getEntry(
          "uid=testModifyWithPut," + userBaseDN);
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("inetOrgPerson"));
    assertEquals(entry.getAttributeValue("uid"), "testModifyWithPut");
    assertEquals(entry.getAttributeValue("cn"), "Test User");
    assertEquals(entry.getAttributeValue("givenname"), "Test");
    assertEquals(entry.getAttributeValue("sn"), "User");
    assertEquals(entry.getAttributeValue("title"), "Chief of Operations");
    assertEquals(entry.getAttributeValue("employeeNumber"), "456");
    assertEquals(entry.getAttributeValue("displayName"),
                                                   "Test Modify with PUT");
    dsInstance.checkCredentials(entry.getDN(), "anotherPassword");

    //Make the exact same update again; this should result in no net change to
    //the Directory entry, but should not fail.
    String beforeCount =
        getMonitorAsString(scimInstance, "user-resource-put-successful");
    returnedUser = userEndpoint.update(user);
    String afterCount =
        getMonitorAsString(scimInstance, "user-resource-put-successful");

    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 1);

    // Again, an update with the previously returned content should not fail.
    userEndpoint.update(returnedUser);

    beforeCount =
        getMonitorAsString(scimInstance, "user-resource-put-404");
    try
    {
      //Try to update an entry that doesn't exist
      user.setId("uid=fakeUserName," + userBaseDN);
      userEndpoint.update(user);
      fail("Expected ResourceNotFoundException when updating " +
            "non-existent user");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }
    afterCount =
        getMonitorAsString(scimInstance, "user-resource-put-404");
    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 1);
  }



  /**
   * Tests a basic modify operation against a User using PATCH.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testModifyWithPatch() throws Exception
  {
    //Add an entry to the Directory
    dsInstance.addEntry("dn: uid=testModifyWithPatch," + userBaseDN,
                        "objectclass: top",
                        "objectclass: person",
                        "objectclass: organizationalPerson",
                        "objectclass: inetOrgPerson",
                        "uid: testModifyWithPatch",
                        "userPassword: oldPassword",
                        "cn: testModifyWithPatch",
                        "givenname: Test",
                        "sn: User",
                        "telephoneNumber: 512-123-4567",
                        "homePhone: 972-987-6543",
                        "mail: testEmail.1@example.com",
                        "mail: testEmail.2@example.com");

    //Update the entry via SCIM
    UserResource user = getUser("testModifyWithPatch");
    assertNotNull(user);

    //This will change the 'cn' to 'Test User'
    user.setName(new Name("Test User", "User", null, "Test", null, null));

    //Other simple attributes to patch
    user.setUserType("Employee");
    user.setPassword("anotherPassword");
    user.setTitle("Chief of Operations");
    user.setDisplayName("Test Modify with PATCH");

    //Try an attribute in the extended schema
    user.setSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
            AttributeValueResolver.STRING_RESOLVER, "456");

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    List<SCIMAttribute> attrsToUpdate = new ArrayList<SCIMAttribute>(6);
    final SCIMObject scimObject = user.getScimObject();

    attrsToUpdate.add(
          scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "name"));
    attrsToUpdate.add(
          scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "userType"));
    attrsToUpdate.add(
          scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "password"));
    attrsToUpdate.add(
          scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "title"));
    attrsToUpdate.add(
          scimObject.getAttribute(
              SCIMConstants.SCHEMA_URI_CORE, "displayName"));
    attrsToUpdate.add(
          scimObject.getAttribute(
              SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber"));

    userEndpoint.update(user.getId(), attrsToUpdate, null);

    //Verify the contents of the entry in the Directory
    SearchResultEntry entry =
            dsInstance.getConnectionPool().getEntry(
                    "uid=testModifyWithPatch," + userBaseDN);
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("inetOrgPerson"));
    assertEquals(entry.getAttributeValue("uid"), "testModifyWithPatch");
    assertEquals(entry.getAttributeValue("cn"), "Test User");
    assertEquals(entry.getAttributeValue("givenname"), "Test");
    assertEquals(entry.getAttributeValue("sn"), "User");
    assertEquals(entry.getAttributeValue("title"), "Chief of Operations");
    assertEquals(entry.getAttributeValue("employeeNumber"), "456");
    assertEquals(entry.getAttributeValue("displayName"),
            "Test Modify with PATCH");
    dsInstance.checkCredentials(entry.getDN(), "anotherPassword");

    //Verify that existing attributes did not get touched
    assertEquals(entry.getAttributeValue("telephoneNumber"), "512-123-4567");
    assertEquals(entry.getAttributeValue("homePhone"), "972-987-6543");
    assertEquals(entry.getAttributeValues("mail").length, 2);

    //Make the exact same update again; this should result in no net change to
    //the Directory entry, but should not fail.
    String monitoryEntryDN = getMonitorEntryDN("HTTP");

    long beforeCount = scimInstance.getMonitorAsLong(
            monitoryEntryDN, "user-resource-patch-successful");

    userEndpoint.update(user.getId(), attrsToUpdate, null);

    long afterCount = scimInstance.getMonitorAsLong(
            monitoryEntryDN, "user-resource-patch-successful");

    assertEquals(beforeCount, afterCount - 1);

    beforeCount = scimInstance.getMonitorAsLong(
            monitoryEntryDN, "user-resource-patch-404");
    try
    {
      //Try to update an entry that doesn't exist
      userEndpoint.update(
              "uid=fakeUserName," + userBaseDN, attrsToUpdate, null);
      fail("Expected ResourceNotFoundException when patching " +
              "non-existent user");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }
    afterCount = scimInstance.getMonitorAsLong(
            monitoryEntryDN, "user-resource-patch-404");
    assertEquals(beforeCount, afterCount - 1);

    //Try a more complex patch, where we simultaneously delete a few attributes
    //and update a few more. Specifically, we are going to delete the phone
    //numbers completely and also remove one of the name attributes and replace
    //it with another.
    attrsToUpdate.clear();

    List<String> attrsToDelete = list("phoneNumbers", "name.familyName");

    SCIMAttributeValue value =
            SCIMAttributeValue.createStringValue("testEmail.1@example.com");
    SCIMAttribute email1Value = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                 "emails").getSubAttribute("value"), value);

    value = SCIMAttributeValue.createStringValue("work");
    SCIMAttribute emailType = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                 "emails").getSubAttribute("type"), value);

    value = SCIMAttributeValue.createStringValue("delete");
    SCIMAttribute email1Operation = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                 "emails").getSubAttribute("operation"), value);

    SCIMAttributeValue email2 = SCIMAttributeValue.createComplexValue(
                                  email1Value, emailType, email1Operation);

    value = SCIMAttributeValue.createStringValue("testEmail.3@example.com");
    SCIMAttribute email3Value = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
                 "emails").getSubAttribute("value"), value);

    SCIMAttributeValue email3 = SCIMAttributeValue.createComplexValue(
                                  email3Value, emailType);

    SCIMAttribute emails = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
           "emails"), email2, email3);

    attrsToUpdate.add(emails);

    value = SCIMAttributeValue.createStringValue("PatchedUser");
    SCIMAttributeValue familyNameValue = SCIMAttributeValue.createComplexValue(
         SCIMAttribute.create(CoreSchema.USER_DESCRIPTOR.getAttribute(
               SCIMConstants.SCHEMA_URI_CORE, "name").getSubAttribute(
                   "familyName"), value));

    SCIMAttribute familyName = SCIMAttribute.create(
         CoreSchema.USER_DESCRIPTOR.getAttribute(SCIMConstants.SCHEMA_URI_CORE,
             "name"), familyNameValue);

    attrsToUpdate.add(familyName);

    //Do the PATCH, this time testing the version of the method that returns
    //the resource.

    //Mark the start and end time with a 500ms buffer on either side, because
    //the Directory Server will record the actual modifyTimestamp using the
    //TimeThread, which only updates once every 100ms.
    Date startTime = new Date(System.currentTimeMillis() - 500);
    String[] attrsToGet = { "userName", "title", "userType",
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION + ":employeeNumber" };
    UserResource returnedUser = userEndpoint.update(user.getId(), null,
                                  attrsToUpdate, attrsToDelete, attrsToGet);
    Date lastModified = returnedUser.getMeta().getLastModified();
    Date endTime = new Date(System.currentTimeMillis() + 500);

    //Verify the contents of the entry in the Directory
    entry = dsInstance.getConnectionPool().getEntry(
                    "uid=testModifyWithPatch," + userBaseDN);
    assertNotNull(entry);
    assertTrue(entry.hasObjectClass("inetOrgPerson"));
    assertEquals(entry.getAttributeValue("uid"), "testModifyWithPatch");
    assertEquals(entry.getAttributeValue("cn"), "Test User");
    assertEquals(entry.getAttributeValue("givenname"), "Test");
    assertEquals(entry.getAttributeValue("sn"), "PatchedUser");
    assertEquals(entry.getAttributeValue("title"), "Chief of Operations");
    assertEquals(entry.getAttributeValue("employeeNumber"), "456");
    assertEquals(entry.getAttributeValue("displayName"),
            "Test Modify with PATCH");
    dsInstance.checkCredentials(entry.getDN(), "anotherPassword");

    assertFalse(entry.hasAttribute("telephoneNumber"));
    assertFalse(entry.hasAttribute("homePhone"));
    assertEquals(entry.getAttributeValues("mail").length, 2);
    List<String> emailList = Arrays.asList(entry.getAttributeValues("mail"));
    assertFalse(emailList.contains("testEmail.1@example.com"));
    assertTrue(emailList.contains("testEmail.2@example.com"));
    assertTrue(emailList.contains("testEmail.3@example.com"));

    //Verify what is returned from the SDK
    assertEquals(returnedUser.getId(), user.getId());
    assertTrue(
            returnedUser.getMeta().getLocation().toString()
                    .endsWith(user.getId()));
    assertEquals(returnedUser.getUserName(), "testModifyWithPatch");
    assertEquals(returnedUser.getTitle(), user.getTitle());
    assertEquals(returnedUser.getUserType(), user.getUserType());
    assertNull(returnedUser.getPassword());
    assertTrue(lastModified.after(startTime));
    assertTrue(lastModified.before(endTime));
    assertEquals(returnedUser.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "employeeNumber",
            AttributeValueResolver.STRING_RESOLVER), "456");
  }



  /**
   * Tests the basic DELETE functionality via SCIM.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testDelete() throws Exception
  {
    dsInstance.getConnectionPool().add(
       generateUserEntry("testDelete", userBaseDN, "Test", "User", "password"));

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final String userID = getUser("testDelete").getId();
    String beforeCount =
        getMonitorAsString(scimInstance, "user-resource-delete-successful");
    userEndpoint.delete(userID);
    String afterCount =
        getMonitorAsString(scimInstance, "user-resource-delete-successful");

    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 1);

    beforeCount =
        getMonitorAsString(scimInstance, "user-resource-delete-404");
    try
    {
      //Should throw ResourceNotFoundException
      userEndpoint.delete(userID);
      fail("Expected ResourceNotFoundException when deleting " +
              "non-existent user");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }
    afterCount =
        getMonitorAsString(scimInstance, "user-resource-delete-404");
    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 1);
  }



  /**
   * Tests the behavior of DELETE operations over SCIM if soft-deletes are
   * enabled on the Directory Server.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testSoftDelete() throws Exception
  {
    //Enable soft-deletes on the DS instance
    dsInstance.dsconfig(
        "create-soft-delete-policy",
        "--policy-name", "default-soft-delete-policy",
        "--set", "auto-soft-delete-connection-criteria:Requests by Root Users");

    dsInstance.dsconfig(
            "set-global-configuration-prop",
            "--set", "soft-delete-policy:default-soft-delete-policy");

    scimInstance.getConnectionPool().add(generateUserEntry(
            "testSoftDelete", userBaseDN, "Test", "User", "password"));

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    UserResource user = getUser("testSoftDelete");
    String userID = user.getId();

    String beforeCount =
            getMonitorAsString(scimInstance, "user-resource-delete-successful");
    userEndpoint.delete(userID);
    String afterCount =
            getMonitorAsString(scimInstance, "user-resource-delete-successful");

    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
            Integer.valueOf(afterCount) - 1);

    beforeCount =
            getMonitorAsString(scimInstance, "user-resource-delete-404");
    try
    {
      //Should throw ResourceNotFoundException
      userEndpoint.delete(userID);
      fail("Expected ResourceNotFoundException when deleting " +
              "non-existent user");
    }
    catch(ResourceNotFoundException e)
    {
      //expected
    }
    afterCount =
            getMonitorAsString(scimInstance, "user-resource-delete-404");
    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
            Integer.valueOf(afterCount) - 1);

    //Try to recreate the user. This should succeed, and now we will have both
    //a real and a soft-deleted version.
    user = userEndpoint.create(user);
    assertEntryExists(scimInstance.getConnectionPool(),
                        "uid=testSoftDelete," + userBaseDN);

    //Delete the user again (now there will be two soft-deleted entries)
    userEndpoint.delete(user.getId());

    //Delete the soft-deleted entries
    SearchResult result = scimInstance.getConnectionPool().search(
      userBaseDN, SearchScope.SUB, "(objectclass=ds-soft-delete-entry)", "1.1");
    assertEquals(result.getEntryCount(), 2);
    for(SearchResultEntry e : result.getSearchEntries())
    {
      scimInstance.getConnectionPool().delete(e.getDN());
    }

    //Cleanup
    dsInstance.dsconfig("set-global-configuration-prop",
            "--reset", "soft-delete-policy");

    dsInstance.dsconfig("delete-soft-delete-policy",
                        "--policy-name", "default-soft-delete-policy");
  }



  /**
   * Tests retrieval of a simple user resource.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testRetrieve() throws Exception
  {
    testRetrieve(service, "HTTP");
  }



  /**
   * Tests retrieval of a simple user resource over SSL.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testRetrieveSecure() throws Exception
  {
    testRetrieve(secureService, "HTTPS");
  }



  /**
   * Tests retrieval of a simple user resource.
   *
   * @param service  The SCIM service with which to invoke operations.
   * @param connectionHandler  The name of the connection handler that will
   *                           handle the operations.
   *
   * @throws Exception If the test fails.
   */
  private void testRetrieve(final SCIMService service,
                            final String connectionHandler)
      throws Exception
  {
    final String uid = "testRetrieve-" + connectionHandler;
    final String dn = "uid=" + uid + "," + userBaseDN;

    //Add an entry to the Directory
    dsInstance.addEntry("dn: " + dn,
                      "objectclass: top",
                      "objectclass: person",
                      "objectclass: organizationalPerson",
                      "objectclass: inetOrgPerson",
                      "uid: " + uid,
                      "userPassword: anotherPassword",
                      "cn: testRetrieve",
                      "givenname: Test",
                      "sn: User",
                      "title: Chief of R&D",
                      "displayName: John Smith",
                      "mail: jsmith@example.com",
                      "employeeType: Engineer");

    final String userID = getUser(uid).getId();

    //Try to retrieve the user via SCIM
    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    String beforeCount =
        getMonitorAsString(scimInstance, connectionHandler,
                           "user-resource-get-successful");
    UserResource user = userEndpoint.get(userID);
    String afterCount =
        getMonitorAsString(scimInstance, connectionHandler,
                           "user-resource-get-successful");
    assertNotNull(user);
    assertEquals(user.getId(), userID);
    assertTrue(user.getMeta().getLocation().toString().endsWith(userID),
               "location='" + user.getMeta().getLocation() + "' userID='" +
               userID + "'");
    assertEquals(user.getUserName(), uid);
    assertEquals(user.getUserType(), "Engineer");
    assertEquals(user.getName().getFormatted(), "testRetrieve");
    assertEquals(user.getName().getGivenName(), "Test");
    assertEquals(user.getName().getFamilyName(), "User");
    assertEquals(user.getTitle(), "Chief of R&D");
    assertEquals(user.getDisplayName(), "John Smith");
    assertEquals(user.getEmails().iterator().next().getValue(),
                                                "jsmith@example.com");
    assertNull(user.getPassword());

    //Retrieve the user with only the 'userName' and 'password' attribute
    user = userEndpoint.get(userID, null, "userName", "password");
    assertNotNull(user);
    assertEquals(user.getId(), userID);
    assertEquals(user.getUserName(), uid);
    assertNull(user.getUserType());
    assertNull(user.getName());
    assertNull(user.getTitle());
    assertNull(user.getDisplayName());
    assertNull(user.getEmails());
    assertNull(user.getPassword());

    //Make sure the stats were updated properly
    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 1);
  }



  /**
   * Tests the list/query resources functionality using all the different
   * filter types.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testFiltering() throws Exception
  {
    final SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

    long halfwayTime = 0;

    // Create some users.
    final long NUM_USERS = 10;
    for (int i = 0; i < NUM_USERS; i++)
    {
      if(i == NUM_USERS / 2)
      {
        //Record the time when half the entries have been created
        halfwayTime = System.currentTimeMillis();
        TestCaseUtils.sleep(300);
      }

      final String uid = "filterUser." + i;

      com.unboundid.ldap.sdk.Entry e;

      if(i % 2 == 0)
      {
        e = generateUserEntry(uid, userBaseDN, "Test", "User", "password",
                new Attribute("mail", uid + "@example.com",
                                            "evenNumber@example.com"));
      }
      else if(i % 3 == 0)
      {
        e = generateUserEntry(uid, userBaseDN, "Test", "User", "password",
                new Attribute("displayName", uid));
      }
      else
      {
        e = generateUserEntry(uid, userBaseDN, "Test", "User", "password",
                new Attribute("title", "Engineer"));
      }

      dsInstance.getConnectionPool().add(e);
    }

    SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    //
    // All of the filters used below first check if the userName starts with
    // "filterUser" so that we only consider the users created for this test.
    //

    String beforeCount =
        getMonitorAsString(scimInstance, "user-resource-query-successful");
    //Test 'eq' (equals)
    Resources<UserResource> results =
        userEndpoint.query("userName sw \"filterUser\" and " +
                               "emails eq \"filterUser.6@example.com\"");
    assertEquals(results.getTotalResults(), 1);
    Iterator<UserResource> iter = results.iterator();
    UserResource user = iter.next();
    assertEquals(user.getUserName(), "filterUser.6");
    assertFalse(iter.hasNext());

    //Test 'co' (contains)
    results =
      userEndpoint.query("userName sw \"filterUser\" and " +
                             "emails co \"User.4@example\"");
    assertEquals(results.getTotalResults(), 1);
    iter = results.iterator();
    user = iter.next();
    assertEquals(user.getUserName(), "filterUser.4");
    assertFalse(iter.hasNext());

    //Test 'sw' (starts with)
    results =
      userEndpoint.query("userName sw \"filterUser\" and title sw \"Eng\"");
    assertEquals(results.getTotalResults(), 3);
    iter = results.iterator();
    while(iter.hasNext())
    {
      user = iter.next();
      String uid = user.getUserName();
      int idx = Integer.parseInt(uid.substring(uid.indexOf(".") + 1));
      assertTrue(idx % 2 != 0);
      assertTrue(idx % 3 != 0);
    }

    //Test 'pr' (present)
    results = userEndpoint.query("userName sw \"filterUser\" and emails pr");
    assertEquals(results.getTotalResults(), 5);
    iter = results.iterator();
    while(iter.hasNext())
    {
      user = iter.next();
      String uid = user.getUserName();
      int idx = Integer.parseInt(uid.substring(uid.indexOf(".") + 1));
      assertTrue(idx % 2 == 0);
    }

    //Test 'gt' (greater than)
    Date halfwayDate = new Date(halfwayTime);
    String formattedTime = formatter.format(halfwayDate);
    results = userEndpoint.query("userName sw \"filterUser\" and " +
                                 "meta.created gt \"" + formattedTime + "\"");
    assertEquals(results.getTotalResults(), 5);
    iter = results.iterator();
    while(iter.hasNext())
    {
      user = iter.next();
      String uid = user.getUserName();
      int idx = Integer.parseInt(uid.substring(uid.indexOf(".") + 1));
      assertTrue(idx >= 5);
    }

    //Test 'lt' (less than)
    results = userEndpoint.query("userName sw \"filterUser\" and " +
                                 "meta.created lt \"" + formattedTime + "\"");
    assertEquals(results.getTotalResults(), 5);
    iter = results.iterator();
    while(iter.hasNext())
    {
      user = iter.next();
      String uid = user.getUserName();
      int idx = Integer.parseInt(uid.substring(uid.indexOf(".") + 1));
      assertTrue(idx < 5);
    }
    String afterCount =
        getMonitorAsString(scimInstance, "user-resource-query-successful");
    assertEquals(beforeCount == null ? 0 : Integer.valueOf(beforeCount),
        Integer.valueOf(afterCount) - 6);
  }



  /**
   * Tests Group resources and the groups attribute.
   *
   * @throws Exception  If the test fails.
   */
  @Test(enabled = false)
  public void testGroups() throws Exception
  {
    final SCIMEndpoint<GroupResource> groupEndpoint =
        service.getGroupEndpoint();
    final SCIMEndpoint<UserResource> userEndpoint =
        service.getUserEndpoint();

    // Create a user
    UserResource user1 = userEndpoint.newResource();
    user1.setUserName("groupsUser.1");
    user1.setName(new Name("User 1", "GroupsUser", null, "Test", null, null));
    user1 = userEndpoint.create(user1);

    //Create a static group via SCIM
    GroupResource groupOfUniqueNames = groupEndpoint.newResource();
    groupOfUniqueNames.setDisplayName("groupOfUniqueNames");
    final List<Entry<String>> members = new ArrayList<Entry<String>>(1);
    members.add(new Entry<String>(user1.getId(), null, false));
    groupOfUniqueNames.setMembers(members);
    groupOfUniqueNames = groupEndpoint.create(groupOfUniqueNames);

    //Create a static group via LDAP
    dsInstance.getConnectionPool().add(
        generateGroupOfNamesEntry("groupOfNames", groupBaseDN,
                                  "uid=groupsUser.1," + userBaseDN));

    //Create a dynamic group via LDAP
    dsInstance.addEntry(
        "dn: cn=groupOfURLs," + groupBaseDN,
        "objectClass: groupOfURLs",
        "cn: groupOfURLs",
        "memberURL: ldap:///" + userBaseDN + "??sub?(sn=GroupsUser)"
    );

    //Create a virtual static group via LDAP
    dsInstance.addEntry(
        "dn: cn=testVirtualStaticGroup," + groupBaseDN,
        "objectclass: top",
        "objectclass: groupOfNames",
        "objectclass: ds-virtual-static-group",
        "cn: testVirtualStaticGroup",
        "ds-target-group-dn: cn=groupOfURLs," + groupBaseDN);

    //Retrieve the groups via SCIM
    GroupResource groupOfNames = getGroup("groupOfNames");

    GroupResource groupOfURLs = getGroup("groupOfURLs");

    GroupResource virtualStaticGroup = getGroup("testVirtualStaticGroup");

    // Make sure that a query doesn't unnecessarily process groups etc.
    // There should be no more than two internal searches.
    long beforeCount = dsInstance.getMonitorAsLong(
        "cn=Processing Time Histogram,cn=monitor", "searchOpsTotalCount");
    user1 = userEndpoint.query("id eq \"" + user1.getId() + "\"",
                               null, null, "userName").iterator().next();
    assertNull(user1.getGroups(),
               "User has the groups attribute that was not requested");
    long afterCount = dsInstance.getMonitorAsLong(
        "cn=Processing Time Histogram,cn=monitor", "searchOpsTotalCount");
    assertTrue(afterCount > beforeCount && afterCount <= beforeCount + 2);

    // Verify that the groups attribute is set correctly.
    user1 = userEndpoint.get(user1.getId());
    assertNotNull(user1.getGroups(), "User does not have the groups attribute");
    assertEquals(user1.getGroups().size(), 4,
                 "User has the wrong number of groups");
    final ArrayList<String> groupIDs = new ArrayList<String>();
    final ArrayList<String> displayNames = new ArrayList<String>();
    for (final Entry<String> groupEntry : user1.getGroups())
    {
      groupIDs.add(groupEntry.getValue());
      displayNames.add(groupEntry.getDisplay());
      if(groupEntry.getValue().equals(groupOfNames.getId()))
      {
        assertEquals(groupEntry.getDisplay(), "groupOfNames");
        assertTrue(groupEntry.getType().equalsIgnoreCase("direct"));
      }
      else if(groupEntry.getValue().equals(groupOfUniqueNames.getId()))
      {
        assertEquals(groupEntry.getDisplay(), "groupOfUniqueNames");
        assertTrue(groupEntry.getType().equalsIgnoreCase("direct"));
      }
      else if(groupEntry.getValue().equals(groupOfURLs.getId()))
      {
        assertEquals(groupEntry.getDisplay(), "groupOfURLs");
        assertTrue(groupEntry.getType().equalsIgnoreCase("indirect"));
      }
      else if(groupEntry.getValue().equals(virtualStaticGroup.getId()))
      {
        assertEquals(groupEntry.getDisplay(), "testVirtualStaticGroup");
        assertTrue(groupEntry.getType().equalsIgnoreCase("indirect"));
      }
      else
      {
        fail("Unexpected group ID: " + groupEntry.getValue());
      }
    }

    System.out.println("groupIDs=" + groupIDs);
    System.out.println("groupOfUniqueNames=" + groupOfUniqueNames.getId());
    assertTrue(groupIDs.contains(groupOfNames.getId()));
    assertTrue(groupIDs.contains(groupOfUniqueNames.getId()));
    assertTrue(groupIDs.contains(groupOfURLs.getId()));
    assertTrue(groupIDs.contains(virtualStaticGroup.getId()));
    assertTrue(displayNames.contains("groupOfNames"));
    assertTrue(displayNames.contains("groupOfUniqueNames"));
    assertTrue(displayNames.contains("groupOfURLs"));
    assertTrue(displayNames.contains("testVirtualStaticGroup"));

    // Verify that the members attribute is set correctly.
    assertNotNull(groupOfNames.getMembers());
    assertEquals(groupOfNames.getMembers().size(), 1);
    Entry<String> member = groupOfNames.getMembers().iterator().next();
    assertEquals(member.getValue(), user1.getId());
    assertTrue(member.getType().equalsIgnoreCase("user"));

    assertNotNull(groupOfUniqueNames.getMembers());
    assertEquals(groupOfUniqueNames.getMembers().size(), 1);
    member = groupOfUniqueNames.getMembers().iterator().next();
    assertEquals(member.getValue(), user1.getId());
    assertTrue(member.getType().equalsIgnoreCase("user"));

    assertNotNull(groupOfURLs.getMembers());
    assertEquals(groupOfURLs.getMembers().size(), 1);
    member = groupOfURLs.getMembers().iterator().next();
    assertEquals(member.getValue(), user1.getId());
    assertTrue(member.getType().equalsIgnoreCase("user"));

    assertNotNull(virtualStaticGroup.getMembers());
    assertEquals(virtualStaticGroup.getMembers().size(), 1);
    member = virtualStaticGroup.getMembers().iterator().next();
    assertEquals(member.getValue(), user1.getId());
    assertTrue(member.getType().equalsIgnoreCase("user"));

    //Test updating a Group via PATCH. First, add a new user to the DS and then
    //patch the groupOfUniqueNames to add this user as a member.
    dsInstance.getConnectionPool().add(
        generateUserEntry("patchUser", userBaseDN, "Test", "User", "password"));

    UserResource user2 = getUser("patchUser");

    SCIMAttributeValue value =
            SCIMAttributeValue.createStringValue(user2.getId());
    SCIMAttribute idValue = SCIMAttribute.create(
            CoreSchema.GROUP_DESCRIPTOR.getAttribute(
               SCIMConstants.SCHEMA_URI_CORE, "members")
                    .getSubAttribute("value"), value);

    value = SCIMAttributeValue.createStringValue(
                user2.getName().getFormatted());
    SCIMAttribute displayValue = SCIMAttribute.create(
            CoreSchema.GROUP_DESCRIPTOR.getAttribute(
                SCIMConstants.SCHEMA_URI_CORE, "members")
                    .getSubAttribute("display"), value);

    SCIMAttributeValue complexValue =
            SCIMAttributeValue.createComplexValue(idValue, displayValue);
    SCIMAttribute membersAttr = SCIMAttribute.create(
            CoreSchema.GROUP_DESCRIPTOR.getAttribute(
                SCIMConstants.SCHEMA_URI_CORE, "members"), complexValue);

    List<SCIMAttribute> attrsToUpdate = new ArrayList<SCIMAttribute>(1);
    attrsToUpdate.add(membersAttr);

    groupEndpoint.update(groupOfUniqueNames.getId(), attrsToUpdate, null);

    groupOfUniqueNames = getGroup("groupOfUniqueNames");
    assertNotNull(groupOfUniqueNames.getMembers());
    assertEquals(groupOfUniqueNames.getMembers().size(), 2);
    for(Entry<String> entry : groupOfUniqueNames.getMembers())
    {
      assertTrue(entry.getValue().equals(user1.getId()) ||
                 entry.getValue().equals(user2.getId()));
      assertTrue(entry.getType().equalsIgnoreCase("user"));
    }
  }



  /**
   * Provides test coverage for pagination.
   *
   * @throws Exception  If the test fails.
   */
  @Test(enabled = false)
  public void testPagination() throws Exception
  {
    // Create some users.
    final Set<String> userDNs = new HashSet<String>();
    final long NUM_USERS = 10;
    for (int i = 0; i < NUM_USERS; i++)
    {
      final String uid = "paginationUser." + i;
      dsInstance.getConnectionPool().add(
          generateUserEntry(uid, userBaseDN,
                            "Test", "User", "password"));
      userDNs.add("uid=" + uid + "," + userBaseDN);
    }

    // Fetch the users one page at a time with page size equal to 1.
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    int pageSize = 1;
    for (long startIndex = 1; startIndex <= NUM_USERS; startIndex += pageSize)
    {
      final Resources<UserResource> resources =
          userEndpoint.query("userName sw \"paginationUser\"", null,
                             new PageParameters(startIndex, pageSize));
      assertEquals(resources.getTotalResults(), NUM_USERS);
      assertEquals(resources.getStartIndex(), startIndex);
      assertEquals(resources.getItemsPerPage(), pageSize);
    }
    assertEquals(userDNs.size(), NUM_USERS);

    // Create some groups.
    final long NUM_GROUPS = 10;
    for (int i = 0; i < NUM_GROUPS; i++)
    {
      final String cn = "paginationGroup." + i;
      dsInstance.getConnectionPool().add(
          generateGroupOfNamesEntry(cn, groupBaseDN, userDNs));
    }

    // Fetch the groups one page at a time with page size equal to 3.
    final SCIMEndpoint<GroupResource> groupEndpoint =
           service.getGroupEndpoint();
    pageSize = 3;
    final Set<String> groupIDs = new HashSet<String>();
    for (long startIndex = 1; startIndex <= NUM_GROUPS; startIndex += pageSize)
    {
      final Resources<GroupResource> resources =
        groupEndpoint.query("displayName sw \"paginationGroup\"",
          new SortParameters("displayName", "ascending"),
          new PageParameters(startIndex, pageSize));

      assertEquals(resources.getTotalResults(), NUM_GROUPS);
      assertEquals(resources.getStartIndex(), startIndex);

      int numResources = 0;
      for (final GroupResource resource : resources)
      {
        numResources++;
        groupIDs.add(resource.getId());
      }
      assertEquals(resources.getItemsPerPage(), numResources);
    }
    assertEquals(groupIDs.size(), NUM_GROUPS);

    // Attempt to fetch resources from a non-existent page.
    final long startIndex = NUM_GROUPS + 1;
    final Resources<GroupResource> resources =
        groupEndpoint.query("displayName sw \"paginationGroup\"",
          new SortParameters("displayName", "ascending"),
          new PageParameters(startIndex, pageSize));
    assertEquals(resources.getTotalResults(), NUM_GROUPS);
    assertEquals(resources.getItemsPerPage(), 0);
    assertEquals(resources.getStartIndex(), startIndex);
  }



  /**
   * Tests the maxResults configuration setting.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testMaxResults() throws Exception
  {
    // Lower the maxResults setting.
    final int maxResults = 1;
    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--add", "extension-argument:maxResults=" + maxResults);

    // Create some users.
    final long NUM_USERS = 10;
    for (int i = 0; i < NUM_USERS; i++)
    {
      final String uid = "maxResultsUser." + i;
      dsInstance.getConnectionPool().add(
          generateUserEntry(uid, userBaseDN, "Test", "User", "password"));
    }

    // Try to fetch more users than can be returned.
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    final Resources<UserResource> resources = userEndpoint.query(null);
    assertEquals(resources.getTotalResults(), maxResults);

    //Clean up
    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--remove", "extension-argument:maxResults=" + maxResults);
  }



  /**
   * Tests that an attempt to set an invalid configuration is handled correctly.
   *
   * @throws Exception If the test passes.
   */
  @Test(expectedExceptions = AssertionError.class, enabled = false)
  public void testInvalidConfiguration() throws Exception
  {
    // Create a resources configuration that is not valid because it defines
    // no resources.
    final String resourcesFile = TestCaseUtils.createTempFile(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>",
        "<scim-ldap:resources xmlns:" +
        "scim-ldap=\"http://www.unboundid.com/scim-ldap\">",
        "</scim-ldap:resources>");
    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--set", "extension-argument:useResourcesFile=" + resourcesFile);
  }



  /**
   * Tests the Service Provider Config endpoint.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testServiceProviderConfig() throws Exception
  {
    final ServiceProviderConfig config = service.getServiceProviderConfig();

    // These assertions need to be updated as optional features are implemented.
    assertTrue(config.getPatchConfig().isSupported());
    assertTrue(config.getBulkConfig().isSupported());
    assertTrue(config.getBulkConfig().getMaxOperations() > 0);
    assertTrue(config.getBulkConfig().getMaxPayloadSize() > 0);
    assertTrue(config.getFilterConfig().isSupported());
    assertTrue(config.getFilterConfig().getMaxResults() > 0);
    assertTrue(config.getChangePasswordConfig().isSupported());
    assertTrue(config.getSortConfig().isSupported());
    assertFalse(config.getETagConfig().isSupported());
    assertTrue(config.getAuthenticationSchemes().size() > 0);
    assertTrue(config.getXmlDataFormatConfig().isSupported());

    for (final AuthenticationScheme s : config.getAuthenticationSchemes())
    {
      assertNotNull(s.getName());
      assertNotNull(s.getDescription());
    }
  }



  /**
   * Tests HTTP Basic Auth.
   *
   * @throws Exception  If the test fails.
   */
  @Test(enabled = false)
  public void testBasicAuth() throws Exception
  {
    // Create a new user.
    final String dn = "uid=basicAuthUser," + userBaseDN;
    dsInstance.getConnectionPool().add(
        generateUserEntry(
            "basicAuthUser", userBaseDN, "Basic", "User", "password"));
    final String id = getUser("basicAuthUser").getId();

    // Create a client service that authenticates as the user.
    final URI uri = new URI("http", null, scimInstance.getLdapHost(),
                            scimPort, null, null, null);
    final SCIMService basicAuthService = new SCIMService(uri, dn, "password");

    // Check that the authenticated user can read its own entry.
    final SCIMEndpoint<UserResource> endpoint =
        basicAuthService.getUserEndpoint();
    final UserResource userResource = endpoint.get(id);
    assertNotNull(userResource);

    System.out.println("basicAuthUser = " + userResource);
    assertNotNull(userResource.getMeta().getCreated());
    assertNotNull(userResource.getMeta().getLastModified());
  }



  /**
   * Tests HTTP Basic Auth with the wrong credentials.
   *
   * @throws UnauthorizedException          If the test passes.
   * @throws LDAPException                  If the test fails.
   * @throws SCIMException                  If the test fails.
   * @throws URISyntaxException             If the test fails.
   */
  @Test(expectedExceptions = UnauthorizedException.class, enabled = false)
  public void testBasicAuthInvalidCredentials()
      throws UnauthorizedException, LDAPException, SCIMException,
             URISyntaxException
  {
    // Create a new user.
    final String dn = "uid=invalidCredentials," + userBaseDN;
    dsInstance.getConnectionPool().add(
        generateUserEntry(
            "invalidCredentials", userBaseDN, "Basic", "User", "password"));
    final String id = getUser("invalidCredentials").getId();

    // Create a client service that authenticates with the wrong password.
    final URI uri = new URI("http", null, scimInstance.getLdapHost(),
                            scimPort, null, null, null);
    final SCIMService basicAuthService = new SCIMService(uri, dn, "assword");

    basicAuthService.getUserEndpoint().get(id);
  }



  /**
   * Tests the SCIM Bulk operation using JSON.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testBulkJson() throws Exception
  {
    final MediaType origContentType = service.getContentType();
    final MediaType origAcceptType = service.getAcceptType();

    service.setContentType(MediaType.APPLICATION_JSON_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    try
    {
      testBulk(MediaType.APPLICATION_JSON_TYPE);
    }
    finally
    {
      service.setContentType(origContentType);
      service.setAcceptType(origAcceptType);
    }
  }



  /**
   * Tests the SCIM Bulk operation using XML.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testBulkXml() throws Exception
  {
    final MediaType origContentType = service.getContentType();
    final MediaType origAcceptType = service.getAcceptType();

    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    try
    {
      testBulk(MediaType.APPLICATION_XML_TYPE);
    }
    finally
    {
      service.setContentType(origContentType);
      service.setAcceptType(origAcceptType);
    }
  }



  /**
   * Tests the SCIM Bulk operation.
   *
   * @param mediaType  The media type to use for the operation.
   *
   * @throws Exception If the test fails.
   */
  private void testBulk(final MediaType mediaType) throws Exception
  {
    final String mediaSubType = mediaType.getSubtype();
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();
    final SCIMEndpoint<GroupResource> groupEndpoint =
        service.getGroupEndpoint();

    UserResource userAlice = userEndpoint.newResource();
    userAlice.setName(new Name("Alice Ecila", "Ecila", null,
                               "Alice", null, null));
    userAlice.setUserName("alice-" + mediaSubType);

    UserResource userBob = userEndpoint.newResource();
    userBob.setName(new Name("Bob The Builder", "Builder", "The",
                             "Bob", null, null));
    userBob.setUserName("bob-" + mediaSubType);
    userBob = userEndpoint.create(userBob);
    userBob.setTitle("Construction Worker");
    userBob.setSingularAttributeValue(
        SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
        Manager.MANAGER_RESOLVER,
        new Manager("bulkId:alice", "Miss Manager"));

    UserResource userDave = userEndpoint.newResource();
    userDave.setName(new Name("Dave Allen", "Allen", null,
                              "Dave", null, null));
    userDave.setUserName("dave-" + mediaSubType);
    userDave = userEndpoint.create(userDave);

    GroupResource group = groupEndpoint.newResource();
    group.setDisplayName("group-" + mediaSubType);
    Collection<Entry<String>> members = new ArrayList<Entry<String>>();
    members.add(new Entry<String>("bulkId:alice", null));
    group.setMembers(members);

    final String beforeBulkSuccessCount =
        getMonitorAsString(scimInstance,
                           "bulk-resource-post-successful");
    final String beforeBulkContentCount =
        getMonitorAsString(scimInstance,
                           "bulk-resource-post-content-" + mediaSubType);
    final String beforeBulkResponseCount =
        getMonitorAsString(scimInstance,
                           "bulk-resource-post-response-" + mediaSubType);

    final String beforeUserPostSuccessCount =
        getMonitorAsString(scimInstance, "user-resource-post-successful");
    final String beforeUserPutSuccessCount =
        getMonitorAsString(scimInstance, "user-resource-put-successful");
    final String beforeUserDeleteSuccessCount =
        getMonitorAsString(scimInstance, "user-resource-delete-successful");
    final String beforeGroupPostSuccessCount =
        getMonitorAsString(scimInstance, "group-resource-post-successful");

    final List<BulkOperation> operations = new ArrayList<BulkOperation>();

    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.POST, "alice", null,
        "/Users", userAlice));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.PUT, "bob", null,
        "/Users/" + userBob.getId(), userBob));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.DELETE, null, null,
        "/Users/" + userDave.getId(), null));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.POST, "group", null,
        "/Groups", group));

    final BulkResponse response = service.processBulkRequest(operations);
    final List<BulkOperation> responses = new ArrayList<BulkOperation>();
    for (final BulkOperation o : response)
    {
      responses.add(o);
    }

    final String afterBulkSuccessCount =
        getMonitorAsString(scimInstance,
                           "bulk-resource-post-successful");
    final String afterBulkContentCount =
        getMonitorAsString(scimInstance,
                           "bulk-resource-post-content-" + mediaSubType);
    final String afterBulkResponseCount =
        getMonitorAsString(scimInstance,
                           "bulk-resource-post-response-" + mediaSubType);

    assertTrue(Integer.valueOf(afterBulkSuccessCount) >
               (beforeBulkSuccessCount == null ?
                0 : Integer.valueOf(beforeBulkSuccessCount)));
    assertTrue(Integer.valueOf(afterBulkContentCount) >
               (beforeBulkContentCount == null ?
                0 : Integer.valueOf(beforeBulkContentCount)));
    assertTrue(Integer.valueOf(afterBulkResponseCount) >
               (beforeBulkResponseCount == null ?
                0 : Integer.valueOf(beforeBulkResponseCount)));

    final String afterUserPostSuccessCount =
        getMonitorAsString(scimInstance, "user-resource-post-successful");
    final String afterUserPutSuccessCount =
        getMonitorAsString(scimInstance, "user-resource-put-successful");
    final String afterUserDeleteSuccessCount =
        getMonitorAsString(scimInstance, "user-resource-delete-successful");
    final String afterGroupPostSuccessCount =
        getMonitorAsString(scimInstance, "group-resource-post-successful");

    assertEquals(Integer.parseInt(afterUserPostSuccessCount),
                 (beforeUserPostSuccessCount == null ?
                  0 : Integer.parseInt(beforeUserPostSuccessCount)) + 1);
    assertEquals(Integer.parseInt(afterUserPutSuccessCount),
                 (beforeUserPutSuccessCount == null ?
                  0 : Integer.parseInt(beforeUserPutSuccessCount)) + 1);
    assertEquals(Integer.parseInt(afterUserDeleteSuccessCount),
                 (beforeUserDeleteSuccessCount == null ?
                  0 : Integer.parseInt(beforeUserDeleteSuccessCount)) + 1);
    assertEquals(Integer.parseInt(afterGroupPostSuccessCount),
                 (beforeGroupPostSuccessCount == null ?
                  0 : Integer.parseInt(beforeGroupPostSuccessCount)) + 1);


    assertEquals(responses.size(), operations.size());

    for (int i = 0; i < operations.size(); i++)
    {
      final BulkOperation o = operations.get(i);
      final BulkOperation r = responses.get(i);

      assertEquals(o.getMethod(), r.getMethod());
      assertEquals(o.getBulkId(), r.getBulkId());

      if (o.getMethod().equals(BulkOperation.Method.POST)
             || o.getMethod().equals(BulkOperation.Method.PUT))
      {
        assertNotNull(r.getLocation());
      }

      assertNotNull(r.getStatus());

      if (o.getMethod().equals(BulkOperation.Method.POST))
      {
        assertEquals(r.getStatus().getCode(), "201");
      }
      else
      {
        assertEquals(r.getStatus().getCode(), "200");
      }
    }

    userAlice = userEndpoint.query(
        "userName eq \"alice-" + mediaSubType + "\"").iterator().next();
    assertEquals(responses.get(0).getBulkId(), "alice");
    assertTrue(responses.get(0).getLocation().endsWith(userAlice.getId()));

    try
    {
      userEndpoint.get(userDave.getId());
      fail("User Dave was not deleted");
    }
    catch (SCIMException e)
    {
      // Expected.
      assertEquals(e.getStatusCode(), 404);
    }

    userBob = userEndpoint.get(userBob.getId());
    assertEquals(userBob.getTitle(), "Construction Worker");
    assertEquals(userBob.getSingularAttributeValue(
            SCIMConstants.SCHEMA_URI_ENTERPRISE_EXTENSION, "manager",
            Manager.MANAGER_RESOLVER).getManagerId(),
                 userAlice.getId());

    group = groupEndpoint.query(
        "displayName eq \"group-" + mediaSubType + "\"").iterator().next();
    assertEquals(responses.get(3).getBulkId(), "group");
    assertTrue(responses.get(3).getLocation().endsWith(group.getId()));
    assertEquals(group.getMembers().iterator().next().getValue(),
                 userAlice.getId());

    groupEndpoint.delete(group.getId());
  }



  /**
   * Tests that the server returns the correct response for invalid SCIM Bulk
   * operations.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testInvalidBulk()
      throws Exception
  {
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final UserResource testUser = userEndpoint.newResource();
    testUser.setName(new Name("Test Invalid Bulk", "Bulk", null,
                              "Test", null, null));
    testUser.setUserName("test-invalid-bulk");

    final SCIMEndpoint<GroupResource> groupEndpoint =
        service.getGroupEndpoint();

    final GroupResource testGroup = groupEndpoint.newResource();
    testGroup.setDisplayName("test-invalid-bulk");
    final Collection<Entry<String>> members = new ArrayList<Entry<String>>();
    members.add(new Entry<String>("bulkId:undefined", null));
    testGroup.setMembers(members);

    // Missing method.
    testInvalidBulkOperation(
        BulkOperation.createRequest(null, null, null, "/Users", null),
        "400");

    // POST with missing bulkId.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.POST, null, null,
                                    "/Users", testUser),
        "400");

    // Missing path.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.DELETE, null, null,
                                    null, null),
        "400");

    // POST with missing data.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.POST, "user", null,
                                    "/Users", null),
        "400");

    // POST specifies path with a resource ID.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.POST, "user", null,
                                    "/Users/1", testUser),
        "400");

    // DELETE specifies a path with no resource ID.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.DELETE, null, null,
                                    "/Users", null),
        "400");

    // Undefined bulkId reference in the data.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.POST, "group", null,
                                    "/Groups/", testGroup),
        "409");

    // PATCH a resource that doesn't exist.
    testInvalidBulkOperation(
        BulkOperation.createRequest(BulkOperation.Method.PATCH, null, null,
                                    "/Users/1", testUser),
        "404");
  }



  /**
   * Tests that the server returns the correct response for an invalid SCIM Bulk
   * operation.
   *
   * @param o                     An invalid operation.
   * @param expectedResponseCode  The expected response code.
   *
   * @throws Exception If the test fails.
   */
  private void testInvalidBulkOperation(final BulkOperation o,
                                        final String expectedResponseCode)
      throws Exception
  {
    // We allow either the request to fail or the individual operation to fail
    // within the request.
    final BulkResponse response;
    try
    {
      response = service.processBulkRequest(Arrays.asList(o));
      final Status status = response.iterator().next().getStatus();

      assertNotNull(status);
      assertEquals(status.getCode(), expectedResponseCode);
      assertNotNull(status.getDescription());
    }
    catch (SCIMException e)
    {
      assertEquals(String.valueOf(e.getStatusCode()), expectedResponseCode);
      assertNotNull(e.getMessage());
    }
  }



  /**
   * Tests that the server returns the correct response to a SCIM Bulk request
   * which contains two operations with the same bulkId.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testDuplicateBulkId()
      throws Exception
  {
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final UserResource testUser1 = userEndpoint.newResource();
    testUser1.setName(new Name("Test Duplicate BulkId", "BulkId", null,
                              "Test", null, null));
    testUser1.setUserName("test-duplicate-bulkid-1");

    final UserResource testUser2 = userEndpoint.newResource();
    testUser2.setName(new Name("Test Duplicate BulkId", "BulkId", null,
                              "Test", null, null));
    testUser2.setUserName("test-duplicate-bulkid-2");

    final List<BulkOperation> operations = new ArrayList<BulkOperation>(2);
    operations.add(
        BulkOperation.createRequest(BulkOperation.Method.POST, "bulkid1", null,
                                    "/Users", testUser1));
    operations.add(
        BulkOperation.createRequest(BulkOperation.Method.POST, "bulkid1", null,
                                    "/Users", testUser2));

    final BulkResponse bulkResponse = service.processBulkRequest(operations);
    final List<BulkOperation> responses = new ArrayList<BulkOperation>();
    for (final BulkOperation o : bulkResponse)
    {
      responses.add(o);
    }

    assertEquals(responses.get(0).getStatus().getCode(), "201");
    assertEquals(responses.get(1).getStatus().getCode(), "400");
  }



  /**
   * Tests the bulkMaxPayloadSize configuration setting.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testBulkMaxPayloadSize() throws Exception
  {
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < 100; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null, "/Users/" + i, null));
    }

    // Lower the bulkMaxPayloadSize setting.
    int bulkMaxPayloadSize = 1000;
    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--add", "extension-argument:bulkMaxPayloadSize=" +
                 bulkMaxPayloadSize);

    try
    {
      try
      {
        service.processBulkRequest(operations);
        fail("The bulkMaxPayloadSize was intentionally exceeded but the " +
             "bulk request was not rejected");
      }
      catch (SCIMException e)
      {
        // Expected.
        assertEquals(e.getStatusCode(), 413);
      }
    }
    finally
    {
      //Clean up
      scimInstance.dsconfig(
          "set-http-servlet-extension-prop",
          "--extension-name", "SCIM",
          "--remove", "extension-argument:bulkMaxPayloadSize=" +
                      bulkMaxPayloadSize);
    }

    // Increase the bulkMaxPayloadSize setting.
    bulkMaxPayloadSize = 10000;
    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--add", "extension-argument:bulkMaxPayloadSize=" +
                 bulkMaxPayloadSize);

    try
    {
      // The same request should now succeed (although all the operations fail)
      service.processBulkRequest(operations);
    }
    finally
    {
      //Clean up
      scimInstance.dsconfig(
          "set-http-servlet-extension-prop",
          "--extension-name", "SCIM",
          "--remove", "extension-argument:bulkMaxPayloadSize=" +
                      bulkMaxPayloadSize);
    }
  }



  /**
   * Tests the bulkMaxOperations configuration setting.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testBulkMaxOperations() throws Exception
  {
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < 100; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null, "/Users/" + i, null));
    }

    // Lower the bulkMaxOperations setting.
    int bulkMaxOperations = 99;
    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--add", "extension-argument:bulkMaxOperations=" +
                 bulkMaxOperations);

    try
    {
      final String beforeDelete404Count =
          getMonitorAsString(scimInstance, "user-resource-delete-404");
      try
      {
        service.processBulkRequest(operations);
        fail("The bulkMaxOperations was intentionally exceeded but the " +
             "bulk request was not rejected");
      }
      catch (SCIMException e)
      {
        // Expected.
        assertEquals(e.getStatusCode(), 413);
      }

      // Ensure no delete operations were attempted.
      final String afterDelete404Count =
          getMonitorAsString(scimInstance, "user-resource-delete-404");
      assertEquals((afterDelete404Count == null ?
                    0 : Integer.parseInt(afterDelete404Count)),
                   (beforeDelete404Count == null ?
                    0 : Integer.parseInt(beforeDelete404Count)));
    }
    finally
    {
      //Clean up
      scimInstance.dsconfig(
          "set-http-servlet-extension-prop",
          "--extension-name", "SCIM",
          "--remove", "extension-argument:bulkMaxOperations=" +
                      bulkMaxOperations);
    }

    // Increase the bulkMaxPayloadSize setting.
    bulkMaxOperations = 100;
    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--add", "extension-argument:bulkMaxOperations=" +
                 bulkMaxOperations);

    try
    {
      // The same request should now succeed (although all the operations fail)
      service.processBulkRequest(operations);
    }
    finally
    {
      //Clean up
      scimInstance.dsconfig(
          "set-http-servlet-extension-prop",
          "--extension-name", "SCIM",
          "--remove", "extension-argument:bulkMaxOperations=" +
                      bulkMaxOperations);
    }
  }



  /**
   * Tests the Bulk request failOnErrors value.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testFailOnErrors() throws Exception
  {
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < 10; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null, "/Users/" + i, null));
    }

    assertEquals(getSize(service.processBulkRequest(operations, 1)), 1);
    assertEquals(getSize(service.processBulkRequest(operations, 9)), 9);
    assertEquals(getSize(service.processBulkRequest(operations, 10)), 10);
    assertEquals(getSize(service.processBulkRequest(operations, -1)), 10);
  }



  /**
   * Determine the number of operations in a bulk response.
   *
   * @param bulkResponse  The bulk response to be sized.
   *
   * @return  The number of operations in the bulk response.
   */
  private int getSize(final BulkResponse bulkResponse)
  {
    int count = 0;
    for (final BulkOperation o : bulkResponse)
    {
      count++;
    }

    return count;
  }



  /**
   * Our implementation of Bulk operations permits a bulkId reference to be
   * used as a resource ID in the path. This method tests that feature.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testBulkIdInPath() throws Exception
  {
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    final UserResource testUser = userEndpoint.newResource();
    testUser.setName(new Name("Test BulkId In Path", "Path", null,
                              "Test", null, null));
    testUser.setUserName("test-bulkid-in-path");

    final List<BulkOperation> operations = new ArrayList<BulkOperation>();

    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.POST, "user", null,
        "/Users", testUser));
    operations.add(BulkOperation.createRequest(
        BulkOperation.Method.DELETE, null, null,
        "/Users/" + "bulkId:user", null));

    final BulkResponse response = service.processBulkRequest(operations);
    final List<BulkOperation> responses = new ArrayList<BulkOperation>();
    for (final BulkOperation o : response)
    {
      responses.add(o);
    }

    assertEquals(responses.size(), operations.size());

    for (int i = 0; i < operations.size(); i++)
    {
      final BulkOperation o = operations.get(i);
      final BulkOperation r = responses.get(i);

      if (o.getMethod().equals(BulkOperation.Method.POST))
      {
        assertEquals(r.getStatus().getCode(), "201");
      }
      else
      {
        assertEquals(r.getStatus().getCode(), "200");
      }
    }

    assertEquals(userEndpoint.query(
        "userName eq \"test-bulkid-in-path\"").getTotalResults(), 0);
  }



  /**
   * Test that concurrent, conflicting bulk operations don't cause anything
   * bad to happen.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testConcurrentBulkOperations() throws Exception
  {
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    // Create bulk operations to add some users, modify them, and delete them.
    final List<UserResource> users = new ArrayList<UserResource>();
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < 10; i++)
    {
      final UserResource user = userEndpoint.newResource();
      user.setName(new Name("User " + i, "User", null, "Test", null, null));
      user.setUserName("user." + i);
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.POST, "bulkid." + i, null, "/Users", user));
      users.add(user);
    }

    for (int i = 0; i < 10; i++)
    {
      final UserResource user = users.get(i);
      user.setTitle("Updated Title");
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.PUT, null, null,
          "/Users/bulkId:bulkid." + i, user));
    }

    for (int i = 0; i < 10; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null,
          "/Users/bulkId:bulkid." + i, null));
    }

    final AtomicInteger numSuccessfulRequests = new AtomicInteger(0);
    final AtomicInteger numCompletedRequests = new AtomicInteger(0);
    final AtomicInteger numCaughtExceptions = new AtomicInteger(0);

    // Create a runnable to execute a single bulk request with the same
    // operations.
    final Runnable runnable = new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          final BulkResponse bulkResponse =
              service.processBulkRequest(operations);

          boolean successful = true;
          for (final BulkOperation o : bulkResponse)
          {
            final String statusCode = o.getStatus().getCode();
            if (!(statusCode.equals("200") || statusCode.equals("201") ||
                  statusCode.equals("404") || statusCode.equals("409")))
            {
              System.out.println(
                  "Unexpected status code in bulk operation response: " + o);
              successful = false;
            }
          }

          if (successful)
          {
            numSuccessfulRequests.incrementAndGet();
          }
        }
        catch (Exception e)
        {
          numCaughtExceptions.incrementAndGet();
          // Ignore.
        }
        numCompletedRequests.incrementAndGet();
      }
    };

    // Execute a bunch of requests in parallel.
    final int numRequests = 100;
    final ThreadPoolExecutor executor =
        new ThreadPoolExecutor(16,                   // min # threads
                               16,                   // max # threads
                               5, TimeUnit.MINUTES,  // kill after idle
                               new LinkedBlockingQueue<Runnable>());

    Future<?> future = null;
    for (int i = 0; i < numRequests; i++)
    {
      future = executor.submit(runnable);
    }


    // Make sure all the requests were completed.
    future.get();
    TestCaseUtils.assertFutureCondition(new FutureCondition(1000, 120)
    {
      @Override
      public boolean testCondition() throws Exception
      {
        return numCompletedRequests.get() == numRequests;
      }

      @Override
      protected String getConditionDetails()
      {
        return "numCompletedRequests=" + numCompletedRequests +
               " numRequests=" + numRequests +
               " numSuccessfulRequests=" + numSuccessfulRequests +
               " numCaughtExceptions=" + numCaughtExceptions;
      }
    });

    assertEquals(numCaughtExceptions.get(), 0);
    assertEquals(numSuccessfulRequests.get(), numCompletedRequests.get());
  }



  /**
   * Process a bunch of bulk requests in parallel.
   *
   * @param numRequests           The total number of bulk requests to process.
   * @param numThreads            The number of threads to use.
   * @param operations            The bulk operations for the content of each
   *                              request.
   * @param numSuccessfulRequests Returns the number of successful requests.
   * @param numFailedRequests     Returns the number of failed requests.
   *
   * @throws Exception  If the requests could not be processed.
   */
  private void processBulkRequestsInParallel(
      final int numRequests, final int numThreads,
      final List<BulkOperation> operations,
      final AtomicInteger numSuccessfulRequests,
      final AtomicInteger numFailedRequests)
      throws Exception
  {
    numSuccessfulRequests.set(0);
    numFailedRequests.set(0);

    // Create a runnable to execute a single bulk request with the same
    // operations.
    final Runnable runnable = new Runnable()
    {
      @Override
      public void run()
      {
        try
        {
          service.processBulkRequest(operations);
          numSuccessfulRequests.incrementAndGet();
        }
        catch (Exception e)
        {
          numFailedRequests.incrementAndGet();
          // Ignore.
        }
      }
    };

    // Execute a bunch of requests in parallel.
    final ThreadPoolExecutor executor =
        new ThreadPoolExecutor(numThreads,           // min # threads
                               numThreads,           // max # threads
                               5, TimeUnit.MINUTES,  // kill after idle
                               new LinkedBlockingQueue<Runnable>());

    Future<?> future = null;
    for (int i = 0; i < numRequests; i++)
    {
      future = executor.submit(runnable);
    }


    // Make sure all the requests were completed.
    future.get();
    TestCaseUtils.assertFutureCondition(new FutureCondition(1000, 120)
    {
      @Override
      public boolean testCondition() throws Exception
      {
        return numSuccessfulRequests.get() + numFailedRequests.get() ==
               numRequests;
      }

      @Override
      protected String getConditionDetails()
      {
        return "numSuccessfulRequests=" + numSuccessfulRequests +
               " numFailedRequests=" + numFailedRequests;
      }
    });
  }



  /**
   * Tests the bulkMaxOperations configuration setting.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testBulkMaxConcurrentRequests() throws Exception
  {
    final SCIMEndpoint<UserResource> userEndpoint = service.getUserEndpoint();

    // Create some bulk operations.
    final int numUsers = 1;
    final List<UserResource> users = new ArrayList<UserResource>();
    final List<BulkOperation> operations = new ArrayList<BulkOperation>();
    for (int i = 0; i < numUsers; i++)
    {
      final UserResource user = userEndpoint.newResource();
      user.setName(new Name("User " + i, "User", null, "Test", null, null));
      user.setUserName("user." + i);
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.POST, "bulkid." + i, null, "/Users", user));
      users.add(user);
    }

    for (int i = 0; i < numUsers; i++)
    {
      final UserResource user = users.get(i);
      user.setTitle("Updated Title");
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.PUT, null, null,
          "/Users/bulkId:bulkid." + i, user));
    }

    for (int i = 0; i < numUsers; i++)
    {
      operations.add(BulkOperation.createRequest(
          BulkOperation.Method.DELETE, null, null,
          "/Users/bulkId:bulkid." + i, null));
    }

    final AtomicInteger numSuccessfulRequests = new AtomicInteger(0);
    final AtomicInteger numFailedRequests = new AtomicInteger(0);

    final int numRequests = 20;
    final int numThreads = 2;

    // Set the bulkMaxConcurrentRequests setting to 1.
    scimInstance.dsconfig(
        "set-http-servlet-extension-prop",
        "--extension-name", "SCIM",
        "--add", "extension-argument:bulkMaxConcurrentRequests=1");
    int bulkMaxConcurrentRequests = 1;
    try
    {
      processBulkRequestsInParallel(numRequests, numThreads, operations,
                                    numSuccessfulRequests,
                                    numFailedRequests);
      assertTrue(numSuccessfulRequests.get() > 0);
      assertTrue(numFailedRequests.get() > 0);

      // Set the bulkMaxConcurrentRequests setting to 2.
      scimInstance.dsconfig(
          "set-http-servlet-extension-prop",
          "--extension-name", "SCIM",
          "--remove", "extension-argument:bulkMaxConcurrentRequests=" +
                      bulkMaxConcurrentRequests,
          "--add", "extension-argument:bulkMaxConcurrentRequests=2");
      bulkMaxConcurrentRequests = 2;

      processBulkRequestsInParallel(numRequests, numThreads, operations,
                                    numSuccessfulRequests,
                                    numFailedRequests);
      assertEquals(numSuccessfulRequests.get(), numRequests);
      assertEquals(numFailedRequests.get(), 0);

      // Set the bulkMaxConcurrentRequests setting back to 1.
      scimInstance.dsconfig(
          "set-http-servlet-extension-prop",
          "--extension-name", "SCIM",
          "--remove", "extension-argument:bulkMaxConcurrentRequests=" +
                      bulkMaxConcurrentRequests,
          "--add", "extension-argument:bulkMaxConcurrentRequests=1");
      bulkMaxConcurrentRequests = 1;

      processBulkRequestsInParallel(numRequests, numThreads, operations,
                                    numSuccessfulRequests,
                                    numFailedRequests);
      assertTrue(numSuccessfulRequests.get() > 0);
      assertTrue(numFailedRequests.get() > 0);
    }
    finally
    {
      //Clean up
      scimInstance.dsconfig(
          "set-http-servlet-extension-prop",
          "--extension-name", "SCIM",
          "--remove", "extension-argument:bulkMaxConcurrentRequests=" +
                      bulkMaxConcurrentRequests);
    }
  }



  /**
   * Tests that an invalid configuration on the HTTP Servlet extension
   * can be recovered from.
   *
   * @throws Exception If the test fails.
   */
  @Test(enabled = false)
  public void testConfigurationChange()
      throws Exception
  {
    scimInstance.stopInstance();
    try
    {
      // Remove a required extension argument.
      scimInstance.dsconfigOffline(
          new String[]
          {
              "set-http-servlet-extension-prop",
              "--extension-name", "SCIM",
              "--remove",
              "extension-argument:resourceMappingFile=extensions/" +
              "com.unboundid.scim-extension/config/resources.xml"
          });
      scimInstance.startInstance();

      // The servlet should not be available.
      try
      {
        service.getServiceProviderConfig();
        fail("The servlet extension should not be running");
      }
      catch (SCIMException e)
      {
        // Expected.
        assertEquals(e.getStatusCode(), 404);
      }

      // Put back the required argument.
      final String[] fullArgs = TestCaseUtils.combineArgs(
          scimInstance.withLdapArgs(
              "set-http-servlet-extension-prop",
              "--extension-name", "SCIM",
              "--add",
              "extension-argument:resourceMappingFile=extensions/" +
              "com.unboundid.scim-extension/config/resources.xml"
          ), "-n");
      final CommandOutput output =
          scimInstance.runCommandTolerateFailures("dsconfig", null, null,
                                                  fullArgs);
      if (output.getReturnValue() != 0)
      {
        assertTrue(output.getStdout().contains("will not take effect"));
        scimInstance.stopInstance();
        scimInstance.startInstance();
      }

      // The servlet should now be available.
      service.getServiceProviderConfig();
    }
    finally
    {
      scimInstance.startInstance();
    }
  }
}
