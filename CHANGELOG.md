# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).


## v1.8.26 - TBD
Updated the LDAPSDK to version 6.0.0 to avoid CVE-2018-1000134.

Updated Apache HttpClient to version 4.5.13 to avoid CVE-2020-13956.


## v1.8.25 - 2020-08-24
Update Release-Notes.txt

Fixed an issue that could cause an exception when creating a resource using certain types of DNTemplate.


## v1.8.24 - 2020-06-08
Update Release-Notes.txt


## v1.8.23 - 2020-06-08
Update Release-Notes.txt

Update BulkResource.java

Made JSON the default SCIM response content type for requests that did not specify an accept content type.


## v1.8.22 - 2019-04-22
Fixed a bug where the startIndex value for SCIM requests would be incorrect if the used LDAPSearch element had more than one baseDN defined in the scim-resources XML file.

Fixed a problem in which a member could not be added to a group via SCIM unless the group's object class was groupOfUniqueNames.

Update copyrights to include 2019


## v1.8.21 - 2018-11-20
Updated release notes for 1.8.21 release.

Fixed a bug where the totalResults value for SCIM requests using page parameters would be incorrect if the used LDAPSearch element had more than one baseDN defined in the scim-resources XML file.

Fix typos in CONTRIBUTING.md

JsonUnmarshaller.unmarshalResources(...) to handle a non-list response

Add project contribution guidelines

Update the UnboundID SCIM SDK Free Use License for Ping Identity

Remove reference to unboundid-scimri.jar file

Remove Oracle JDK 7 and OpenJDK 6 build targets.

Update organization in parent POM to Ping Identity

Update GitHub repository URL

Update copyright notices to Ping Identity and the year 2018


## v1.8.19 - 2017-12-08
Update release notes for 1.8.19 release

Improve group user membership performance


## v1.8.18 - 2017-05-16
Update release notes for the 1.8.18 release.

Add a .gitignore file.

Fix checkstyle errors

Add testIsQuoteFilterValue

Merged SCIM Release Note changes between last release (1.8.16) and current snapshot (1.8.18).

Cleaned up some code for checkstyle.

Updated the SCIM 1.1 SDK to improve the performance of the User groups and Group members attributes. The virtual attribute "isDirectMemberOf" is used by this enhancement, and should be enabled.

Addressed an issue in SCIM filter processing where the parser may evaluate compound statements as a quoted value.

Added code to escape special characters in SCIM values when they are mapped to a DN in LDAP.


## v1.8.16 - 2017-02-13
Updated Release-Notes.txt

Updated the SCIM interface to reliably produce JSON rather than XML in response to a GET operation if an Accept header is not present, or when an Accept header provided by the client does not indicate a preference between JSON and XML.

Updated the SCIM 1.1 interface to treat query parameters (such as sortBy, sortOrder, startIndex) case-insensitively.

SCIM 1.1 clients can obtain diagnostic information about how the Directory Server processes a search query, by specifying attributes=debugsearchindex as a query parameter.

The SCIM 1.1 interface has been changed to reject searches specifying a sortBy parameter that cannot be processed, rather than processing the search as if the parameter had not been present.

Update SCIMService.java

Repackaged Apache Wink / Jersey wrapper classes to avoid classpath conflicts when using Apache Wink.


## v1.8.15 - 2016-11-07
Updated cases where filtered and paged SCIM searches for groups with missing members were not returned.

Updated build-tools module with an UnboundID profile to hold osslicense-maven-plugin configuration, and changed LDAP SDK dependency to com.unboundid:unboundid-ldapsdk:3.2.0

Updated release notes for next version to note javadoc/Java 8 changes

Javadocs generation fails due to changes introduced in Java 8 SE. As a result, javadocs may be missing specifically when building with Java 8 SE.


## v1.8.14 - 2016-09-26
Fix typo in putDateValue method name

Updated the SCIM SDK to allow custom Users and Groups endpoint names and resource descriptor names if an implementation uses custom identifiers. Also changed the default strict mode for resource descriptors to allow for unknown attributes and custom schema. Attribute descriptors in the CoreSchema class are now marked with public accessibility to make them easier to reference in resource subclasses.


## v1.8.13 - 2016-08-04
Addressed an issue where repeated SCIMEndpoint and BulkEndpoint requests could hang when an underyling Response object was not closed properly.

Updated scim-sdk to include runtime jersey-apache-connector dependency.

Updating release notes for next development version


## v1.8.12 - 2016-07-29
Update Release-Notes.txt for 1.8.12 release

updated branded images with current logo

Corrected scim-ldap pom for Maven central

Fix the schema check for required attributes in the LDAPBackend implementation of the PATCH operation.

Simplify the use of properties in the Maven POMs:
    * Remove scimsdk.version and scimsdk.parent.version from the parent POM.
    * Remove scimsdk.doc.version from the scim-sdk POM and replace with a scimsdk.version property, which is used by the replacer plugin.
    * Remove unused scimsdk.version property from the scim-sdk assembly POM.

Added package-level README files.

Update the scimsdk.version that is interpolated into the Maven coordinates in the getting started docs to 1.8.12 and always use a non-snapshot version.


## v1.8.11 - 2016-01-28
Remove the SCIM RI and move the scim-query-rate tool to the scim-sdk module.

Update copyrights and project URL in getting started documentation.

Remove updateGoogleCode.sh script, which is no longer needed.

Update copyright notice years.


## v1.8.9 - 2015-12-16
Updating the release notes


## v1.8.8 - 2015-12-15
[ no changes ]


## v1.8.7 - 2015-12-15
Fix an issue where multivalued If-Match or If-None-Match headers could not be parsed.

Remove Streaming query implementation

Fix the schema check for required attributes in the LDAPBackend implementation of the PATCH operation.

Setting versions in assembly poms.


## v1.8.6 - 2015-09-03
[ no changes ]


## v1.8.5 - 2015-09-03
pom.xml changes to support github

Fixed an issue where null JSON values were converted to "null" strings. The JSON parser has been updated to ignore null attribute values, and to ignore attribute arrays without any non-null values.

Fixed an issue where an attempt to give a required attribute a value missing a required sub-attribute would not be rejected.

Replaced Subversion revision/path with Git revision/branch

Updated POM to replace Subversion revision number/path with Git revision number and branch

Updated SCM references, maven javadoc plugin version

Removed unused OAuthSecurityHandler class. Fixed bug introduced during the Jersey migration to the SCIMQueryRate tool.

Removed ApacheHttpClientConfig and ClientConfig from the Wink compatibility layer since they didn't work well and we should encourage everyone to just use the Jersey's ClientConfig instead.

Removed constructors that took Wink's ClientConfig from RestClient and SCIMService.

Updated SCIMQueryRate and ClientExample to use the new Apache HTTPClient 4.5 APIs.

Fixed issue where cookies are not correctly handled by the Wink compatibility layer.

Removed duplicate dependency in POM file that was causing build warnings.

Updated dependency for httpclient to 4.5 in scim-sdk assembly POM.

Updated version numbers in POM files that were left off from r4365.

Updated release notes to include the Wink to Jersey migration.

Implement a Wink facade that uses Jersey underneath.  Update code to use either the Wink facade, or directly use Jersey.  This change was needed for the Jersey work in the Broker to work properly.


## v1.8.2 - 2015-04-15
Updating release notes for SCIM 1.8.2 release

Added amethod enabling retrieval of only schema-declared (not generated) sub-attributes from a complex AttributeDescriptor.

Addressed an issue where SCIM filter processing may not handle date literals properly when converting to an LDAP filter. This may have produced incorrect search results with date-based filtering.

There are some instances where the Simple Paged Results control is not able to estimate the total matches for the query, or is already returning the entire set of matches.  In these instances we should fall back to using the results listener total.

Fixed checkstyle validation problems in r4279

Updated the LDAP Backend test code to allow for testing without VLV / Simple Paged Results controls. This change adds a test to ensure that simple paged results work as intended. Also added logic to the control processing to get the estimated count from the simple paged results control if the VLV reponse control is not available. Also changed the VLV control processing to look for multiple VLV response controls. We will not log an error when this happens, and we will try not to use a VLV response with zero matches if possible. The results are still unpredictable, but should be more stable.

Update to UserResourceMapperTestCase which does not assume the order of returned filter components from SCIMFilter.parse.

Update javadoc to address building on JDK 8.

Addressed an issue where SCIM filter processing may not handle date literals properly when converting to an LDAP filter. This may have produced incorrect search results with date-based filtering.


## v1.8.1 - 2015-01-22
Update copyright notices

Updated mapper so that PATCH operations with an attribute in meta.attributes and new values will be mapped to a single LDAP modify operation with change type of replace.

Update to the latest version of Jetty 8.x

Updated TelephoneNumberTransformation so it preserves the original format w/o adding, modifying, or removing any digits during the process. Removed the libphonenumber library as a dependency.

Fixed issue introduced in r4022 that could cause some attribute and/or value deletes in a PATCH request to be mapped to LDAP modify replace operations.

When mapping a PATCH operation to LDAP, map the SCIM request directly to a set of LDAP modifications instead of using entry diff.

Use the Permissive Modify Request Control to conform to SCIM spec when adding existing values or deleting non-existent values.

Added UnboundID SCIM extension for streamed query, based on the SCIM 2.0 query-using-POST and designed to take advantage of LDAP's SimplePagedResultsControl.

Rolling back r3910 as it breaks the Broker's SCIM implementation.

When mapping a PATCH operation to LDAP, map the SCIM request directly to a set of LDAP modifications instead of using entry diff.

Add a serial version UID to ComplexValue, and fix a spacing issue in creating-resources.html.

Update the poms to reference a common LDAP SDK version.  Update the release notes date

Updated documentation on using custom resources to reflect new APIs.

Minimize the number of places the SCIM sdk version must be changed by using pom-file inheritance.

Reduce thread contention in ResourceDescriptor.

When marshalling a resource, do not include the schemas attribute if the resource is itself a schema (ResourceDescriptor)

Updated the SCIM SDK version reference in the reference implementation POMs.

Adding unit-test changes for DS-10179.

Added checks for changes to read-only attributes in putResource and postResource operations which were previously only performed in patchResource. Also added checks for deletion of read-only attributes in simple, multi-valued, and meta attributes.

Several ease-of-use enhancements, including:
  1. New overload of SCIMService.getEndpoint that bypasses the need to obtain a ResourceDescriptor from the schema endpoint.
  2. Added the ComplexValue type that models arbitrary complex attributes as a Map of sub-attribute name to SimpleValue.
  3. Added several new APIs to BaseResource that utilize ComplexValue and SimpleValue types to provide a simpler way to set and retrieve resource attributes.

Fixed bug where using AttributeDescriptor.createMultiValuedAttribute with a null multiValuedChildName will create a single valued attribute instead.

Removed redundant references to java.net.ConnectException and ConnectTimeoutException in getStatusCode() where these exceptions are still mapped to status code -1.  Changed the processing of client resource response entity in delete() from reading an InputStream to the consumeContent() method and added special handling for SocketTimeoutException.

Added URL suffix support for POST and PATCH operations. Added support for using URL suffix notation in the SCIM SDK client.

Fix up the SCIM SDK client example.

Update JavaDoc so it is inline with the updated behavior introduced in r3511.

Enable querying using a filter based on the members, groups, and manager.managerId derived attributes.

Updated references of OAuth draft spec to RFCs.

Updated the SCIM client to use Apache HttpClient by default instead of Java's HttpUrlConnection.

Use the Content-Location header instead of Location for non POST responses. Updated the client expect Location only for 201 or 3xx response codes and Content-Location otherwise. However, it will fall back to use the Location header if Content-Location is not present.

Changed the ResourceMapper in SCIM LDAP so it will only include an attribute descriptor in the schema if there is a mapping or derivation definition. If not, it will issue an warning to the debug logger. Enable debug logging when using the InMemroyServerTool so the user will be warned about unmapped attribute definitions. Commented out all attribute definitions without default mappings in the OOTB resource.xml files.

Removed authenticate method from the SCIMBackend API. Authentication is now outside the scope of the SCIM SDK. Moved existing authentication via SASL BIND to the individual backend implementation classes.

Updated SCIM maven dependencies to latest compatible release versions.

Update to copyright year in all SCIM source code subcomponents: scim-ldap, scim-ri, scim-sdk, doc/getting-starting and tests

Added support for matching SCIM filters with co and sw operators against binary attributes. Fixed issue where the number of resources returned from a query maybe less than totalResults.


## v1.6.2 - 2014-04-04
Added implicit schema for meta.attributes


## v1.6.1 - 2014-03-25
Updating Release Notes

Add an option for implicit schema declaration which works similarly to the older versions of SCIM.  The older versions of SCIM unmarshalled JSON data by looking at each key and treating it as either a schema or an attribute name.  But this could lead to attribute descriptions being created for non-existent schemas, which would run into bugs later.

Minor changes:
  - added environment variable checking
  - added error checking for svn merge calls
  - removed reference to CVSDude


## v1.6.0 - 2014-03-04
It is now up to the developer to declare all core common attributes and normative sub-attributes when creating a schema pragmatically. Only declared attributes will be available when the schema is marshalled. However, core common and normative attributes are added by the getter methods if they are not declared. This ensures dependent code can reliably retrieve all core attributes without fear of NPE.
  - The common core schema attributes will always be accessible regardless if they are declared when the schema was created.
  - All multi-valued attributes are expected to have sub-attributes. If they are not defined, the normative sub-attributes type, primary, operation, display are still accessible.
  - All multi-valued attributes are expected to be declared as complex. However, the SDK will handle a non-complex multi-valued attribute defined by a schema read from JSON or XML. In this case, the normative value sub-attribute will be accessible.
  - All values of multi-valued attributes are stored as complex values. However, as an convenience, the value of the normative value sub-attribute (if declared) is accessible as a simple value (SCIMAttributeValue.get*Value() method).
  - Fixed bug where deleting a sub-attribute using the meta.attributes sub-attribute during a PATCH doesn't work. Multi-valued attributes may be deleted with or without specifying the value sub-attribute (phoneNumbers vs phoneNumbers.value).

Remove logic to add cross-origin response headers as it is now obsoleted by the CORS filter implementation.

Make the attribute used for ETags configurable. Added ability to retrieve current ETag from the PreconditionFailedException. Added NotModifiedException.

Refactor the versioning support code in the SCIM-SDK to support If-Match and If-None-Match headers for GET, PUT, PATCH, and DELETE operations.

Correctly handle the case where the modify timestamp is not present in the LDAP entry by using "0" as the etag.

Remove the requirement to have the meta.version attribute for PUT and PATCH operations in a Bulk request.

Added a new "invert" argument to the boolean transformation

Fixed issue where creating a ResourceDescriptor with the common attribute might result in multiple AttributeDescriptors for those attributes.

Fixed issue where creating a Multi Valued Attribute Descriptor with the common sub-attributes might result in multiple AttributeDescriptors for those sub-attributes.

Fixed a regression introduced by the versioning support patch (r3158) where the bulkId is sent back to the client as meta.version.

Fix a bug where authentication with SCIM would fail because the password provided contains a colon character.

Added versioning support to the SCIM SDK.

Removed the scim-extension module.

Added some methods to SCIMEndpoint to facilitate easy usage of the etag w/o having to specify one explicitly.

Deprecated some methods from SCIMEndpoint to encourage the use of the new methods.


## v1.5.0 - 2013-11-15
Fix an issue where the JSON parser would not throw an exception if it encountered an extended schema attribute that was ambiguous (i.e. not a core schema attribute and not wrapped using the extension schema URN).

Fix an issue where the Identity Access API would not return search results when filtering by an operational attribute unless the "attributes" query parameter was also used. The root problem was that the SCIM SDK and the core server were both performing their own paring of the SCIMAttributes to return.

Fix an issue in the ManagerDerivedAttribute where it could throw the wrong exception if it does not find the resource specified by the managerId attribute.

Fix an issue where the SCIMEndpoint would silently filter out read-only attributes (because of the way it used the Diff class internally) from the payload it sends for PATCH operations.

Change startIndex to an int instead of long.

Add ForbiddenException and RequestEntityTooLargeException classes.

Updated the examples so they no longer use deprecated APIs.

Fix an issue in the JSONParser where it could let non-core attributes slip through when they were not qualified with a schema URN. This changes the parsing logic to iterate over the actual JSON objects and validate their schema, rather than iterating over the schemas and looking for which JSON objects fit into that schema.

Fix a bug in the Diff.apply() method where it did not correctly handle multi-valued attributes that used the 'value' sub-attribute.

Fix an oversight from r2889 where a few usages of the PostReadRequestContol were not guarded by a check for whether the LDAP server supports it.

Update the AbstractSCIMResource to return a ResourceNotFoundException (404) instead of an InvalidResourceException (400) in a few places where the 404 is more appropriate.

Update the LDAPBackend to make it configurable regarding its use of the SimplePagedResultsControl. This will make it more extensible to different types of backend LDAP servers if they do not support this control.

Serve some default content at the SCIM base URL to indicate that the SCIM service is available.

Minor cleanup of some POMs to reflect the current version (1.5.0-SNAPSHOT).

Update HttpClient to the next point version (4.2.6), to match what is in the core server.

Update the LDAPBackend to make it configurable regarding its use of the PostReadRequestContol and VirtualListViewRequestControl. This will make it more extensible to different types of backend LDAP servers if they do not support these controls.

Fix a bug in the AbstractSCIMResource which was introduced as part of the Identity Access API. It was generating incorrect SCIMFilters by using the resource schema URN instead of assuming the core SCIM schema URN when not specified in the filter. The exception to this is only for the Identity Access API, which allows you to request raw LDAP attributes without including a schema URN.

Fix an issue where an invalid JSON response was produced for an LDAP group which has a member DN that is not a descendant to the Users base DN.

The Groups derived attribute is now configured to tell it whether to rely on the isMemberOf attribute or not.

Fix a problem with the SCIMQueryAttributes class where it handled AttributePaths incorrectly, using the schema of the resource itself instead of assuming the SCIM core schema for query attributes that don't explicitly include the schema URN.

Refactor the SCIMBackend API so ResourceDescriptors may be retrieved.

Use Path templates with one Wink resource instead of creating a Wink resource per resource type.

Add the HttpServletRequest into the various *Request objects so they can be used by the SCIMBackend.

Update the Diff.[to/from]PartialResource() methods to allow read-only attributes to be included in the partial resources that are generated.

Fix a problem with the Diff.apply() method where it did not properly handle complex multi-valued attributes when applying the diff to a source resource.

Update the SCIM trunk to be version 1.4.6-SNAPSHOT.


## v1.4.4 - 2013-08-15
Redo the PUT operation again to remove the read only attributes during the getModifiableLDAPAttributeTypes on the ResourceMapper.

Fix checkstyle errors from the previous commit. Fix the Maven Checkstyle Plugin configuration to make sure that checkstyle runs when you do "mvn package".

Provide factory method for creating a Diff from a partial resource in PATCH form.

Provide a Diff.apply() method which can produce a target resource with the Diff applied to a specified source resource.

Enhance the SCIMObject.removeAttribute() method to support removing specific sub-attributes.

Fix an issue where the equals() method on SCIMAttribute depended on the ordering of the values in the attribute.

Change the way we handle PUT for read only attributes again to prevent unintended deletion of read only attrs.


## v1.4.2 - 2013-08-12
Fix a bug in the ResourceDescriptor where the AttributeDescriptors it would generate on the fly (for the Identity Access API) were declared read-only.

Fix a regression with the ResourceMapper where it did not use the correct schema URN when processing the attributes in a PATCH request.

Adds support for the "operation" sub-attribute to the Entry class that represents a single value of a multi-valued attribute. This sub-attribute is used to mark a specific value for deletion in a PATCH operation.

Update the ResourceMapper to fix a problem where it did not properly handle multi-valued attributes which had simple values when processing a PATCH operation.


## v1.4.1 - 2013-07-26
Fix a bug that allowed SCIM requests to update attributes that were designated as 'read only' in the SCIM Resource Schema.

Update the CSS file used for the SCIM SDK javadoc documentation so that the generated javadoc looks fine with either Java 6 or Java 7. The previous version looked fine with Java 6 but had serious formatting problems when built with Java 7.

Fix a checkstyle error in ResourceMapper.java.

Fix a bug where the ResourceMapper.toLDAPAttributeTypes() method did not consider the schema URN of the attributes it was converting.

Make the ConstructedValue class public, so that it can be reused in the Identity Broker.

Update the project homepage URL in some README and release notes files.

Update the POMs to version 1.4.1-SNAPSHOT.

Added a utility class to generate a diff between two SCIM resources which is useful for performing update operations using PATCH instead of PUT.

Make name.formatted and name.familyName required since they are mapped to the cn and sn LDAP attributes.

Fix a problem with the updateGoogleCode.sh script where it did not properly trust the SVN server certificates.

Update the updateGoogleCode.sh script to point to the new (in-house) SVN server.

Fix an issue where the updateGoogleCode.sh script could silently ignore SVN errors when synchronizing the UnboundID repository with Google Code.

Cleanup some dead links in the SCIM POM files and update some of the dependency versions.

Fix a backwards compatibility break in the BulkOperation and PreemptiveAuthInterceptor classes.

Prerequisite changes for a performance improvement to the way LDAP static groups are retrieved by SCIM.

Revert several of the changes from r2584 because there have been a couple significant issues with the 4.3-beta1 release of the Apache HttpClient. Version 4.2.5 was released a few weeks after 4.3-beta1 and has the fixes we need for DS-8517. Since 4.2.5 is a stable GA, this commit updates SCIM to depend on that version.

Map "gt" and "lt" SCIM filters to a greater/less than equal to AND NOT equal LDAP filter.

Ensure that each element of a SCIM bulk response's operations list includes a location field when required.

Update SCIM to use version 1.3.0 of the Wink libraries and version 4.3-beta1 of the Apache HttpClient, which fixes some concurrency issues. The APIs have changed significantly and most of the classes we were previously using are now deprecated. There are no functional changes included here.

Fix the PreemptiveAuthInterceptor to work with the new version of Apache HttpClient.


## v1.3.2 - 2013-04-26
Fix an issue in the maven-release-plugin configuration where it did not include the GPG signing flag.

Update the SCIM release notes and prepare the POMs and README files for the 1.3.2 release.

Update the bulk operation response to consistently include the location field.

Fix an issue where the SCIM SDK would not populate the response body and would return only the HTTP headers when validating an OAuth token.

Fix an issue where the SCIM SDK would present multiple WWW-Authenticate headers when validating an OAuth token.

Fix an issue where the SCIM SDK incorrectly assumed that OAuth bearer tokens would be base64-encoded.

Fixed an issue where exceptions thrown during unmarshalling of bulk operations will fail the entire bulk request.

Protect SCIM from XML entity expansion attacks.

Update the scim-query-rate tool to support authenticating with an OAuth bearer token.

Fix a bug in SCIM that prevented simpleMultiValued attributes from returning matching resources when using a custom schema urn.

Imported blind TrustStrategy for scim unit test to address nightly failures on IBM JDKs (AIX and linux x64)


## v1.3.0 - 2013-01-25
Fix a problem in the SCIM build where the file permissions in the zip packages were not getting set correctly.

Update the SCIM LDAPBackend so that it respects the "count" parameter when searching across multiple base DNs. It cannot perfectly handle sorting and pagination when the search spans multiple base DNs (because a VLV index is per-backend), but this fix does make sure that exactly the correct number of results are returned, based on the "maxResults" configuration setting and the "count" parameter from the incoming request.

Update the LDAP-to-SCIM result code mapping to make sure that client errors return HTTP status code 400, not 500.

Fix a bug with the scim-query-rate tool in which it would not work correctly against the Identity Access API.

Fix an issue with the XMLStreamMarshaller where it did not correctly handle invalid XML characters. Attributes that are not explicitly declared as BINARY in the schema may now be returned as base64-encoded strings if they contain any invalid XML characters. The server will add the "base64Encoded=true" attribute to any XML elements for which this is done, so that the client will know the data is encoded.

Fix an issue where the entries under an excluded base DN could still be exposed via a SCIM query.

Fix a NPE if a client tries to delete an entry that has already been deleted.

Fix an issue where the Identity Access API would only return a single value for a multi-valued operational attribute.

Add a size-limit to the LDAP search requests generated by SCIM if they are not going to use the VLV request control. This prevents certain searches from taking a very long time and/or bogging down the server when they're only going to return a small subset of the total results to the SCIM client.

Update the SCIM trunk so that all UnboundID copyrights include the year 2013.

Update the SCIM Release Notes for the upcoming release version 1.2.1.

Fix a potential NPE when searching for an entry that doesn't exist or is not exposed via SCIM.

Remove the restriction that the resources.xml file must contain at least one SCIM resource definition. There are certain cases where it may be useful to configure SCIM not to expose any endpoints.

Fix a bug with r2068 which inadvertently removed the resources.xsd file from the scim-ldap jar file.

Update the version of the jaxb2-maven-plugin to fix a few bugs with the build.

Update the GPG signing options so that artifact signing will work on the new build infrastructure.

Fix a problem where the "Location" header could contain the API version multiple times.

Fix a problem where the "TotalResults" counter could be incorrect when doing a search.

Update the SCIM Reference Implementation and SDK to use the 'entryUUID' as the SCIM ID by default, instead of the DN. This is still configurable via the resources.xml file.

Update the SCIM Reference Implementation and SDK to treat schema URNs and JSON attribute names as case-insensitive, per the SCIM specification.

Add support for multiple base DNs per resource to the SCIM Reference Implementation and SDK. This way a particular resource type, such as "User", can live under multiple base DNs.

Add support for specifying arbitrary URL query parameters when performing a search via SCIM.

Fix a problem where the "Location" header did not contain the API version.

Improve JSON parsing to be more lenient and handle data from multiple schemas more efficiently.

Add support for a "non-strict mode" and a "default schema" in the ResourceDescriptor and Attribute-related classes so that callers can request and use attributes that are not explicitly defined in the schema.

Add support for multiple base DNs in the LDAP Search Parameters for a given resource.

Add support for a new "base-id" query parameter which can be used to specify the SCIM resource ID of the search base entry to use.

Add support for a new "scope" query parameter which can be used to specify the LDAP search scope to use when performing a search. Valid values are "base", "one", "sub", or "subordinate". The default is "sub".

Added basic schema checking at the scim-ldap layer for POST, PUT, and PATCH operations. It currently makes sure that all required attributes are present and the PATCH operation will not cause schema violations.

Fixed issue with SCIMEndpoint where non 200 status codes from PATCH operations are handled incorrectly.

Make LDAP error to HTTP status code mapping more consistent. Map most LDAP errors to HTTP 500 status code instead of 400 since it is unlikely that these errors are caused by the client.


## v1.2.0 - 2012-10-24
Update the version of the LDAP SDK dependency in the SCIM-LDAP public POM.

Fix a bug in the AbstractSCIMResource class where it would not always return the proper "WWW-Authenticate" response header.

Clean up the dependencies section of SCIM-RI client POM and prepare the other POMs for the SCIM 1.2.0 release.

Disable the legacy SCIM-Extension tests, as they have now been ported to the SCIM-RI module and the core server.

Add Windows batch scripts for the SCIM RI tools so that they can be used on Windows platforms.

Update the updateGoogleCode.sh script to fix a couple of outdated SVN URLs and to better handle the case where there are a lot of non-SCIM commits to the Components repository.

-Fix an issue with r1775 where the SCIM-RI module depended on the standalone-ds-test-harness, which is an internal dependency and does not resolve when building the code from Google Code.

Update the SCIM-LDAP module to use the latest version of the LDAP SDK.

Add support for authentication to SCIM via OAuth 2.0 bearer tokens. This requires an OAuthTokenHandler extension built with the UnboundID Server SDK in order to decode and validate tokens.

Addressed a few issues:
  - Moved most of the test cases from scim-extension to scim-ri so they work with the in memory server to maintain good test coverage after the scim-extension module is removed.
  - Fixed a bug with the XML un-marshaller where it doesn't parse multi-valued simple attributes correctly.
  - Added the ability to configure resource limits problematically for the SCIMServer in the RI.

Comply with the for SCIM 1.1 schema changes:
  - phoneNumbers attribute now uses the RFC3966 syntax.
  - The Group displayName attribute now required.
  - The Group members attribute is no longer required.
  To accommodate for possible differences in LDAP's phone number format, several arguments are added to the new TelephoneNumberTransformation so that the mapping behavior may be customized to fit installation environment. Details about the arguments are in the resources.xml file.

Add support for the PATCH operation in the SCIM SDK and server implementation. This allows for resources to be partially updated without having to send the entire contents of the resource across the network, reducing network and processing overhead. This is especially beneficial for resources with many attributes, such as Groups.

Update the versions of Jetty and the LDAP SDK to match what is currently in the core server.

Update the SCIM unit tests to test against the 3.5.0.0-GA Directory release.

Enhance the SCIMException code to handle more types of errors and add a generic ConnectException to represent a true connection error where there is no response from the target server.

Refactor some of the StaticUtils methods.

Updating the SCIM extension POM to provide the ability to use @VersionRestriction.

Updating to use version 1.0-SNAPSHOT of ETAH.

Added the ability to allow the build to ignore test failures if the ignore.test.failures property is set to true.


## v1.1.1 - 2012-05-08
Update SCIM to use release version 0.10 of ETAH.

Updated SCIM guide to version 1.1.1

This patch fixes a limitation where modifications to attributes that are mapped to a RDN naming attribute will result in an error instead of performing an additional modify DN operation to change the RDN. I've also added a unit-test for this case.

Change UserResource.isActive() to return a Boolean instead of a preemptive to allow a null return value to indicate a value is not present in the resource. Javadoc is also updated to indicate that null is a possible return value. Added unit-test to make sure all UserResource.get methods that return a simple value will not throw an unexpected exception.

Updating to use the 1.0 version of the coverage-diff-plugin.

Updating the SCIM extension POM to take advantage of the code coverage functionality provided by ETAH.

Add OAuth bearer token support to the SCIM Client SDK.


## v1.1.0 - 2012-02-22
Cleanup the SCIM POMs after a timed-out release build. The build timed out because there are now many more unit tests than before, and a release build ultimately runs them several times (once before tagging, once on the tag itself, and once after tagging).

Adding release notes for the SCIM 1.1.0 release.

Fix an issue in which Base64-encoded binary values caused a class cast exception.

Add bulk operations to the SDK getting-started guide.

Fix some HTML issues in the creating-resources.html file where Java parameterized type information was lost because the < and > characters were not escaped.

Update the SCIM-Extension to use the 0.9 release version of the ETAH test library.

Update the SCIM-RI test client and README to refer to SCIM version 1.1.0.

Update the examples in the SCIM-SDK Getting Started Guide to reflect some new changes in the API.

Updated draft of SCIM admin guide for 1.1.0 delivery.

The tmpDataDir extension argument is no longer required because a default value is provided.

The SCIM servlet extension now returns an error indicating that administrative action is required if a configuration change is received when the servlet has not been created.

Fix an issue where large bulk requests could cause the Directory Server to run out of memory.

Add methods to the SCIMService class for specifying the user-agent string to use in the HTTP headers.

Expose the PasswordAttributeMapper on the ResourceMapper class, so that caller may use it to determine which LDAP attribute maps to the SCIM 'password' attribute, if any.

Provide a new PreemptiveAuthInterceptor class which can be used to configure the SCIMService to use preemptive authentication. This reduces the overhead of making requests over an authenticated connection.

Update the scim-query-rate tool to use preemptive basic auth, in order to increase performance.

Update the ResourceMapper to throw SCIMException everywhere rather than InvalidResourceException. This is needed so that HTTP status codes other than 400 can be propagated back to the client.

Update the SCIM extension configuration files to use a detailed format HTTP log publisher rather than a common format log publisher.

Fix an issue where the SCIM server continued to report the resource's URI in the "meta.location" attribute and the "Location" HTTP header using the DN, when the SCIM extension was configured to not use DNs as resource IDs.

Fix an issue where an error was not returned when a user without the unindexed-search privilege performed dynamic group lookup.

The SCIM servlet extension now checks that all LDAP attributes referenced in the resource definitions are defined in the server's LDAP schema.

Fix an issue whereby a query of a user resource could result in unnecessary searches of the user's group memberships when the query requested specific attributes not including groups.

Update to the latest SCIM core XML schema. The elements of the SchemaAttribute sequence have been re-ordered.

Reinstate an API in the resource mapper that was being used by the Sync Server.

Update the SCIM resource configuration to allow the SCIM resource ID to be mapped to an LDAP attribute as an alternative to mapping to the LDAP entry DN. The DN does not necessarily meet the requirements of the SCIM specification regarding resource ID immutability since LDAP permits entries to be renamed or moved. The entryUUID attribute, whose read-only value is assigned by the Directory Server, is a possible alternative mapping.

Added bulk operations arguments, per SCIM 238
  -Update SCIM to use version 1.1.0-SNAPSHOT for the next development iteration.
  -Update the <distributionManagement> section of the top-level POM to publish to the Engineering Repo by default. This is necessary to keep the core server from having a build dependency (on SCIM) that comes from the QA Repo.
  -The SCIM-Release job in Hudson has been updated to publish the SCIM-SDK, SCIM-Extension, and SCIM-RI zips to the QA repository.

Update the SCIM Reference Implementation to use Jetty version 8.1.0, which fixes several problems in the IO layer with respect to the latest JVMs and browsers.

Add a commit template for SCIM.

There was a bug in the JSON parser. In content that contains a single resource, and also in the Bulk request (where there are resources of mixed type), the schemas element of each resource specifies which schemas are used. However, in the query response content, where there are multiple resources of the same type, the schemas are specified once for all resources rather than for each resource.

Fix an issue where extension attributes in a JSON query response were being discarded during client parsing of the response.

Update the dependencies for SCIM-RI to use a newer version of Jetty and also to remove the wink-client-apache-httpclient dependency since it is pulled in transitively by the SCIM-LDAP module.

Fix some cases where emitted XML did not validate against the SCIM core XML schema, due to the incorrect sequence of elements. Also namespace declarations for resource extension attributes are now omitted where they are not used.

Add the xmlDataFormat attribute to the ServiceProviderConfig endpoint to indicate that the XML data format is supported.

Update the SCIMEndpoint to catch RuntimeExceptions and wrap them with a SCIMException.

Add support for Bulk operations as defined by the SCIM protocol.

Update to add new query rate argument as per SCIM -229
  -Update the SCIM POM files to use the new "unboundid.svn.cvsdude.com" URL for source code management.
  -Change the SCIM SDK to use the same implementation of the Java Servlet API as the core server.
  -Clean up the license lookup table for SCIM.
  -Fix a bug in the example usage for the SCIM RI in-memory server tool.

Add "--resourceID" argument to scim-query-rate tool to support resource retrieval by ID.

Add developer information to the SCIM-SDK and SCIM-LDAP public POM files, because this is required to be able to publish them to the Maven Central open-source repository.

Fix an issue with the scim-query-rate tool when run with the --numIntervals option.  If any of the threads created by the tool were blocked waiting for a server response then the tool would also block after the prescribed number of intervals and would not release any of the connections it had been using.


## v1.0.0 - 2012-01-06
Initial public release
