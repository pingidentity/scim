MultiPart Example
===============================================================================

Description 
===============================================================================
- This example demonstrate the usage of the Multipart module. 
- It demonstrate two kind of Multipart usage:
  The first is a simple user management, it uses buffered in and out multipart, 
  while each part hold the xml representation of a user
  usage: 
  GET http://localhost:8080/MultiPart/rest/MP/users
  to get the users list in a MultiPart format
  
  POST http://localhost:8080/MultiPart/rest/MP/users
  with users information in multipart format to add users to the list. 
  
  
  The second one is file uploader it upload, it uses the InMultiPart class which 
  enable it to handle Messages of a huge size, it upload the files and save them to 
  the temp directory
      
  usage:
  using a browser open the page located in http://localhost:8080/MultiPart/UploadFiles.html
  select 2 files and upload them   
    
-Wink features used in this example:
   Multipart, ServiceDocument
     
Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
  