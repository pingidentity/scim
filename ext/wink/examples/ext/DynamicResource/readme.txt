DynamicResource Example
===============================================================================

Description 
===============================================================================
- This example demonstrates the usage of dynamic resources. 
  The Path and Workspace information is defined using the DynamicResource interface and not 
  by annotation as in regular resources.
  MyApplication class implements a WinkApplication and return the dynamic resource 
  in its getInstances method. It is also in charge in this example for setting the Path, 
  WorkspaceTitle and CollectionTitle information to the resource. 
  Functionality wise, this example is the same as the Bookmarks example. 
- A list of predefined bookmarks is returned as an Atom feed on this URI:
     http://localhost:8080/DynamicResource/rest/bookmarks
      
-Wink features used in this example:
   DynamicResource, ServiceDocument, LinkBuilders, SyndFeed, SyndEntry
   
Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
