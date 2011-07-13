Jaxb Example
===============================================================================

Description 
===============================================================================
- This example demonstrates how to use JAXB objects in association with application/xml media type
- There are three types of JAXB objects: Person, Address and Phone.
- There are two context resolvers: PersonContextResolver and AddressContextResolver;
  one for providing a JAXBContext for Person and one for providing a JAXBContext for Address.
- The application maintains a store of the three types of JAXB objects. Each JAXB object is
  associated with an id in the store, which is used for retrieval of the object from the store.
- The http://localhost:8080/Jaxb/rest/info/person/{id} URI is used to retrieve and create a
  Person in application/xml
- The http://localhost:8080/Jaxb/rest/info/address/{id} URI is used to retrieve and create an
  Address in application/xml 
- The http://localhost:8080/Jaxb/rest/info/phone/{id} URI is used to retrieve and create a Phone
  in application/xml

Build
===============================================================================
- see build instructions in examples/readme.txt file

--- readme.txt EOF ---
