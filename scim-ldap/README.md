# scim-ldap
This package contains classes that map SCIM resources to LDAP entries and 
vice versa. It also contains several APIs that may be used to implement custom 
behaviours for the mapping configuration file to extend its capabilities above 
and beyond those provided out of the box. Each extension type varies in the 
amount of control the implementation has over the mapping process and the 
amount of effort required for implementation. The extension types include:

* `com.unboundid.scim.ldap.Transformation` - For altering the values of mapped 
attributes
* `com.unboundid.scim.ldap.DerivedAttribute` - For generating the value of a 
read-only SCIM attribute
* `com.unboundid.scim.ldap.ResourceMapper` - For overriding the behaviour of 
any part of the mapping process