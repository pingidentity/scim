CustomExceptionMapper Example
===============================================================================

Description 
===============================================================================
- This is a simple example of a service that throws a UserNotExistException and 
  maps the exception to a human readable format
- The service is used to get the list of existing users, add a new user and get a user by its id
- The list of users is returned as an xml from this URI (GET):
     http://localhost:8080/CustomExceptionMapper/rest/users
- A user is created from xml on this URI (POST):
     http://localhost:8080/CustomExceptionMapper/rest/users
- A user is retrieved as xml from this URI (GET):
     http://localhost:8080/CustomExceptionMapper/rest/users/{id}
  If the user id does not exist, a human readable message of the exception is returned


Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
