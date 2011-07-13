History Example
===============================================================================

Description 
===============================================================================
- This example extends the SimpleDefect example and adds the functionality 
  of revisions. 
- The example doesn't handle locking problems, see the Preconditions example
  for a demonstration of how to use the ETag and preconditions.
- The list of all defects is returned as an Atom feed on this URI:
  http://localhost:8080/History/rest/defects
- Each defect includes a revision id
- Atom feed returns the latest revision and links to the 'history' feed.
- Deleted defects disappear from the feed, but their history feed is still available.
- Updating the defect will create a new revision.
- Updating the deleted defect will undelete it.
- It's possible to access a specific revision of the defect by specifying the revision in the url:
  http://localhost:8080/History/rest/defects/1;rev=1
- It is possible to view the versions history of a specific defect in the url:
  http://localhost:8080/History/rest/defects/1/history 
  
-Wink features used in this example:
   Asset, ServiceDocument, DynamicResource, LinkBuilders, SyndFeed, SyndEntry
     
Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
  