Locking Example
===============================================================================

Description 
===============================================================================
- This example extends the SimpleDefect example and adds the functionality 
  of Optimistic Locking (OCC).
- In this example the locking is implemented using the hash code of the object.
- Other implementations can use revisions (see the History example)
- The entities returned by the server contain an Entity Tag (ETag).
- While updating the defects, send the "If-Match" http header 
  with the value of the entity tag in order to invoke the OCC verification.
- In order to retrieve the entry only if it was updated, send 
  the "If-None-Match" http header with the value of ETag.
- The list of all defects is returned as an Atom feed on this URI:
  http://localhost:8080/Preconditions/rest/defects

Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
  
