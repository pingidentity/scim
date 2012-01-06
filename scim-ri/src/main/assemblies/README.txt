# 
# UnboundID SCIM Reference Implementation
# Copyright 2011-2012 UnboundID Corp.
# All Rights Reserved.
# 

This package provides a demonstrable SCIM Service Provider and Client example
that exercises the basic SCIM operations. The following components are included:

  client
    | -- pom.xml
    | -- src
    |     | -- main
    |           | -- java
    |           | -- assemblies
    |
  server
    | -- config
    |      | -- resources.xml
    |      | -- resources.xsd
    |
    | -- ldif
    |      | -- spec-compat.ldif
    |
    | -- lib
    |     | <libs>
    |     | ......
    |
    | -- tools
    |     | -- in-memory-scim-server
    |     | -- scim-query-rate
    |
  javadoc
    | -- <html files>
    |
  LICENSE.txt


 To try out the client, first start the SCIM server by switching to the 'server'
 directory and running the 'in-memory-scim-server' tool:

 >$ tools/in-memory-scim-server --resourceMappingFile config/resources.xml --ldifFile ldif/spec-compat.ldif

 The server should start in the foreground and note the port. On successful
 startup, two sample users are added, including a user with the username
 'bjensen' and password 'password'. Note that the SCIM server is backed by
 the UnboundID In-Memory Directory Server, and thus all data is reset between
 server restarts. There are several other configuration arguments available
 on this tool. Run with "--help" to list them all.

 Verify the server is ready by performing a standard SCIM query via curl or
 your browser; e.g., 'curl http://localhost:port/Users --user bjensen:password'.

 To run the client (create, update and delete a new User) open another shell,
 switch to the 'client' folder, and build the project using Maven:

 >$ mvn package

 After a successful build, go to the assembled project at
 target/scim-ri-client-1.0.0 and run the example tests:

 >$ bin/client-test localhost port "cn=directory manager" password

 The bundled examples are simple. Enhance them by modifying the client source
 found under client/src/main/java and then rebuild.

 For further information visit UnboundID labs at
 http://www.unboundid.com/labs/projects/simple-cloud-identity-management-scim.
