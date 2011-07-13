/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *  
 *******************************************************************************/

package org.apache.wink.example.jaxb;

import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This example demonstrates how JAXB objects can be used in association with
 * application/xml media type.
 * <p>
 * There are three types of JAXB objects: Person, Address and Phone.
 * <p>
 * There are two context resolvers: PersonContextResolver and
 * AddressContextResolver. The two context resolvers are registered, one for
 * providing a JAXBContext for Person and one for providing a JAXBContext for
 * Address.
 * <p>
 * When a Person or Address is consumed or produced, the SDK will locate the
 * correct ContextResolver and use the JAXBContext returned from it to marshal
 * and unmarshal the JAXB object. Since we do not provide a ContextResolver for
 * Phone, the default JAXBContext of the SDK is used to marshal and unmarshal
 * the Phone.
 * <p>
 * The application maintains a store of the three types of JAXB objects. Each
 * JAXB object is associated with an id in the store, which is used for
 * retrieval of the object from the store.
 * <ul>
 * <li>The http://[host]:[port]/Jaxb/rest/info/person/{id} URI is used to
 * retrieve and create a Person in application/xml</li>
 * <li>The http://[host]:[port]/Jaxb/rest/info/address/{id} URI is used to
 * retrieve and create an Address in application/xml</li>
 * <li>The http://[host]:[port]/Jaxb/rest/info/phone/{id} URI is used to
 * retrieve and create a Phone in application/xml</li>
 * </ul>
 */
@Path("info")
public class JaxbResource {

    /**
     * The store that holds the JAXB instances
     */
    private static Store store = new Store();

    /**
     * This method is used to retrieve a Person as XML from the store by its id
     * when the application/xml media type is requested
     * 
     * @param id The id of the person to get. If the id does not exist in the
     *            store, the Http Not Found (406) is returned
     * @return the person JAXB object
     */
    @Path("person/{id}")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Person getPerson(@PathParam("id") String id) {
        Person person = store.getPerson(id);
        if (person == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return person;
    }

    /**
     * This method is used to create a Person from XML when the sent entity
     * media type is application/xml and the accepted type is also
     * application/xml
     * 
     * @param id id of the person to create
     * @param person the person object to create in the store
     * @return a Response instance indicating that the person was created
     */
    @Path("person/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response postPerson(@PathParam("id") String id, Person person) {
        store.putPerson(id, person);
        return Response.created(URI.create("info/" + id)).entity(getPerson(id)).build();
    }

    /**
     * This method is used to retrieve an Address as XML from the store by its
     * id when the application/xml media type is requested
     * 
     * @param id The id of the address to get. If the id does not exist in the
     *            store, the Http Not Found (406) is returned
     * @return the address JAXB object
     */
    @Path("address/{id}")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Address getAddress(@PathParam("id") String id) {
        Address address = store.getAddress(id);
        if (address == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return address;
    }

    /**
     * This method is used to create an Address from XML when the sent entity
     * media type is application/xml and the accepted type is also
     * application/xml
     * 
     * @param id id of the address to create
     * @param address the address object to create in the store
     * @return a Response instance indicating that the address was created
     */
    @Path("address/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response postAddress(@PathParam("id") String id, Address address) {
        store.putAddress(id, address);
        return Response.created(URI.create("info/" + id)).entity(getAddress(id)).build();
    }

    /**
     * This method is used to retrieve a Phone as XML from the store by its id
     * when the application/xml media type is requested
     * 
     * @param id The id of the phone to get. If the id does not exist in the
     *            store, the Http Not Found (406) is returned
     * @return the phone JAXB object
     */
    @Path("phone/{id}")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Phone getPhone(@PathParam("id") String id) {
        Phone phone = store.getPhone(id);
        if (phone == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return phone;
    }

    /**
     * This method is used to create a Phone from XML when the sent entity media
     * type is application/xml and the accepted type is also application/xml
     * 
     * @param id id of the phone to create
     * @param phone the phone object to create in the store
     * @return a Response instance indicating that the phone was created
     */
    @Path("phone/{id}")
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response postPhone(@PathParam("id") String id, Phone phone) {
        store.putPhone(id, phone);
        return Response.created(URI.create("info/" + id)).entity(getPhone(id)).build();
    }

}
