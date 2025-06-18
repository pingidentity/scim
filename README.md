[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.unboundid.product.scim/scim-sdk/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.unboundid.product.scim/scim-sdk)
[![Javadocs](http://javadoc.io/badge/com.unboundid.product.scim/scim-sdk.svg)](http://javadoc.io/doc/com.unboundid.product.scim/scim-sdk)

# SCIM 1.1 SDK for Java

This is UnboundID's open source Java SDK for the 1.1 version of the SCIM specification. Use it to create client applications that communicate with the UnboundID Data Store and any 3rd party SCIM 1.1-compliant service provider. 

## About SCIM 1.1

> The System for Cross-domain Identity Management (SCIM) specification is designed to make managing user identities in cloud-based applications and services easier. The specification suite seeks to build upon experience with existing schemas and deployments, placing specific emphasis on simplicity of development and integration, while applying existing authentication, authorization, and privacy models. Its intent is to reduce the cost and complexity of user management operations by providing a common user schema and extension model, as well as binding documents to provide patterns for exchanging this schema using standard protocols. In essence: make it fast, cheap, and easy to move users in to, out of, and around the cloud. 

See the [SCIM 1.1 Core Schema](http://www.simplecloud.info/specs/draft-scim-core-schema-01.html) and [SCIM 1.1 Protocol](http://www.simplecloud.info/specs/draft-scim-api-01.html) specifications for more information. 

## Getting started

This SDK contains utilities for interacting with different types of SCIM Endpoints, Resources, Schemas, Attributes, Filters, and other objects. The <code>SCIMService</code> and <code>SCIMEndpoint</code> classes provide a starting point for connecting to a REST endpoint and issuing queries or invoking SCIM operations on existing resources.

Release notes can be found in [Release-Notes.txt](resource/Release-Notes.txt).

### Example

```java
public class Client {
  public static void main (String[] args) {
    final URI uri = URI.create("https://example.com:443");
    final SCIMService scimService = new SCIMService(uri, "bjensen", "password");
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
    if (phoneNumbers != null) {
      for (Entry<String> phoneNumber : phoneNumbers) {
        System.out.println(phoneNumber);
      }
    }
  }
}
```

### Maven coordinates

Check Maven Central for the latest available version of the SCIM 1.1 SDK.

```xml
<dependency>
    <groupId>com.unboundid.product.scim</groupId>
    <artifactId>scim-sdk</artifactId>
    <version>VERSION</version>
</dependency>
```

# Reporting issues

Please report bug reports and enhancement requests through this project's [issue tracker](https://github.com/pingidentity/scim/issues). See the [contribution guidelines](CONTRIBUTING.md) for more information.

## License

See the [LICENSE.txt](resource/licenses/LICENSE.txt) file for more info.
