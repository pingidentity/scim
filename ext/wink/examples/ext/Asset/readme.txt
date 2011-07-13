Asset
===============================================================================

Description 
===============================================================================
- This is a simple example of an application that uses Wink Asset concept.
  UsersAsset asset encapsulates a logic of mapping a collection of Users to different media types (atom, xml, json). 
  The UsersAsset can be reused by different applications.    
- The service is used to get the list of existing users
- The list of users is returned as an xml from this URI (GET):
     http://localhost:8080/Asset/rest/users
- A new user is created from xml by POSTing to
     http://localhost:8080/Asset/rest/users
     
- Wink features used in this example:
   Asset, ServiceDocument, LinkBuilders, SyndFeed, SyndEntry

Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
