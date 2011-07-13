CustomContext Example
===============================================================================

Description 
===============================================================================
- This is a simple example of an applications that uses JAX-RS Context Resolver mechanism to 
  implement application level customer authorization. 
  UserPermission object holds authorization information about customer (customer id and permission level).
  SecurityContextResolver is responsible to create CustomerPermission object per request.
  Resource method can receive per request instance of CustomerPermission, to decide if customer is 
  authorized to perform the operation.

  Only customers with sufficient permission are allowed to create new users in the system.          
  
- The service is used to get the list of existing users, add a new user and get a user by its id
- The list of users is returned as an xml from this URI (GET):
     http://localhost:8080/CustomContext/rest/users
- A new user is created from xml on this URI (POST) only in case Client has sufficient permissions:
     http://localhost:8080/CustomContext/rest/users?custId=admin
     Only requests that use "custId=admin" are executed successfully.
- A user is retrieved as xml from this URI (GET):
     http://localhost:8080/CustomContext/rest/users/{id}
  If the user id does not exist, a human readable message of the exception is returned


Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
