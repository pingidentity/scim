UnboundID SCIM SDK for Java
Copyright 2011 UnboundID Corp.  All Rights Reserved.

This package contains the UnboundID SCIM SDK for Java Client.  It
contains the following elements:

- LICENSE.txt -- Provides information about the licenses under which the
  UnboundID SCIM SDK for Java is made available.

- ThirdPartyLicenses.txt -- Provides information about the licenses and terms
  of use for included components.

- build.xml -- An Ant build script to build and run the UnboundID SCIM
  Reference Implementation for Java Client.

- docs -- Javadocs describing the client example.

- ext -- Directory containing JAXB tool libraries.

- lib -- Directory containing SCIM client dependencies.

- resource -- Directory containing the SCIM core schema files.

- src -- Directory containing the client source code.

Package usage:

 The intent of this package is to provide a demonstrable SCIM Service Provider
 and Consumer client that exercise the basic SCIM operations.  To that end,
 an ant build script has been provided that 1) starts the SCIM server with
 sample data and 2) launches a client that creates, edits and deletes a new User.

 To try out the client first start the SCIM server by running the ant task
 'run-server'; e.g., from the command line: 'ant run-server'. The server
 should start in the foreground on port 8181.  On successful startup,
 two sample users are added, including a user with the username 'bjensen' and
 password 'password'.  Note that the SCIM server is backed by the UnboundID
 In-Memory Directory Server therefore all data is reset between server
 restarts.

 Verify the server is ready by performing a standard SCIM query via curl or
 your browser; e.g., 'curl http://localhost:8181/Users --user bjensen:password'.

 To run the client (create, update and delete a new User) open another shell and
 run the default ant task; e.g., 'ant'.

 The bundled examples are simple.  Enhance them by modifying the client source
 found in src and rebuild/run via the default ant task.

 For further information visit UnboundID labs at http://www.unboundid.com.











