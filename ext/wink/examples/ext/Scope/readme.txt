Scope
===============================================================================

Description 
===============================================================================
- This is a simple example of an application that uses Scope annotation.
  The Scope annotation provides a user an ability to override the JAX-RS 
  default definitions of scope. Using this annotation it's possible to define
  a singleton resources and per-request providers. 
- The Singleton counter can be obtain from this URI (GET):
     http://localhost:8080/Scope/rest/singleton
- The Prototyoe counter can be obtain from this URI (GET):
    http://localhost:8080/Scope/rest/prototype
     
-Wink features used in this example:
   Scope, LinkBuilders
     
Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
