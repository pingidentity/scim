LinkBuilders
===============================================================================

Description 
===============================================================================
- This is a simple example of an application that uses LinkBuilders concept.
  The LinksBuilder provides a developer a mechanism for automatic generation of all the resource 
  alternate links, as well as provides an ability to create a single custom link.
  The service is used to get the list of existing users
- The list of users is returned as an xml from this URI (GET):
     http://localhost:8080/LinkBuilders/rest/users
- A single user information can be obtain from this URI (GET):
     http://localhost:8080/LinkBuilders/rest/users/{userId}
     
-Wink features used in this example:
   ServiceDocument, LinkBuilders, SyndFeed, SyndEntry
     
Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
