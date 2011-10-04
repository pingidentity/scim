UnboundID SCIM SDK for Java
Copyright 2011 UnboundID Corp.  All Rights Reserved.

This package contains the UnboundID SCIM SDK for Java Client.  It
contains the following elements:

- LICENSE.txt -- Provides information about the licenses under which the
  UnboundID SCIM SDK for Java is made available.

- ThirdPartyLicenses.txt -- Provides information about the licenses and terms
  of use for included components.

- pom.xml -- A Maven pom to build and run the UnboundID SCIM
  Reference Implementation for Java Client.

- docs -- Javadocs describing the client example.

- schema -- Directory containing the SCIM core schema files.

- src -- Directory containing the client source code.

Package usage:

 The intent of this package is to provide a demonstrable SCIM Service Provider
 and Consumer client that exercise the basic SCIM operations.  To that end,
 a Maven build has been provided that builds and packages the client.

 To try out the client first start the SCIM server by running the
 in-memory-scim-server tool located in the server directory:

 tools/in-memory-scim-server --useResourcesFile config/resources.xml --ldifFile ldif/spec-compat.ldif

 The server should start in the foreground and note the port. On successful
 startup, two sample users are added, including a user with the username
 'bjensen' and password 'password'.  Note that the SCIM server is backed by
 the UnboundID In-Memory Directory Server therefore all data is reset between
 server restarts.

 Verify the server is ready by performing a standard SCIM query via curl or
 your browser; e.g., 'curl http://localhost:port/Users --user bjensen:password'.

 To run the client (create, update and delete a new User) open another shell and
 build the project:

 mvn package

 After a successful build, go to the assembled project at
 target/scim-ri-client-1.0.0-SNAPSHOT and run the example tests:

 bin/client-test localhost port "cn=directory manager" password

 The bundled examples are simple.  Enhance them by modifying the client source
 found in src and rebuild.

 For further information visit UnboundID labs at http://www.unboundid.com.











