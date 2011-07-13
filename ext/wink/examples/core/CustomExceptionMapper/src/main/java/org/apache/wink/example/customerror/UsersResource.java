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

package org.apache.wink.example.customerror;

import java.util.Collection;
import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.wink.common.annotations.Workspace;

/**
 * Sample service for demonstrating the use of an
 * {@link javax.ws.rs.ext.ExceptionMapper} to map a
 * {@link UserNotExistException} to a human readable format
 */
@Workspace(workspaceTitle = "Demo Users Service", collectionTitle = "Users")
@Path("users")
public class UsersResource {

    static private int                    maxUserID = 0;

    static private HashMap<Integer, User> users     = new HashMap<Integer, User>() {
                                                        private static final long serialVersionUID =
                                                                                                       -1744322767380017394L;
                                                        {
                                                            put(maxUserID,
                                                                new User("John", "Smith",
                                                                         maxUserID,
                                                                         "John.Smith@mail.com"));
                                                            put(++maxUserID,
                                                                new User("John", "Doe", maxUserID,
                                                                         "John.Doe@mail.com"));
                                                            put(++maxUserID,
                                                                new User("Pogos", "Pogosyan",
                                                                         maxUserID,
                                                                         "Pogos.Pogosyan@mail.com"));
                                                            put(++maxUserID,
                                                                new User("Hans", "Meier",
                                                                         maxUserID,
                                                                         "Hans.Meier@mail.com"));
                                                            put(++maxUserID,
                                                                new User("Ali", "Vali", maxUserID,
                                                                         "Ali.Vali@mail.com"));
                                                            put(++maxUserID,
                                                                new User("Ploni", "Almoni",
                                                                         maxUserID,
                                                                         "Ploni.Almoni@mail.com"));
                                                        }
                                                    };

    /**
     * A collection of users
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlRootElement(name = "users")
    public static class Users {
        @XmlElementRef
        Collection<User> users;

        public Users() {
        }

        public Users(Collection<User> users) {
            this.users = users;
        }
    }

    /**
     * Get a list of all the existing users as xml
     * 
     * @return an instance of Users
     */
    @GET
    @Produces( {MediaType.APPLICATION_XML})
    public Users getUsers() {
        return new Users(users.values());
    }

    /**
     * Create a new user by receiving it as xml, and returning it as xml
     * 
     * @return the created user
     */
    @POST
    @Consumes( {MediaType.APPLICATION_XML})
    @Produces( {MediaType.APPLICATION_XML})
    public User putUser(User user) {
        maxUserID++;
        user.setId(maxUserID);
        users.put(user.getId(), user);
        return user;
    }

    /**
     * Get a user as xml
     * 
     * @param id the id of the user to get
     * @return the user as specified by the id
     * @throws UserNotExistException if there is no user for the specified id. if
     *             thrown, this exception is mapped to a human readable message
     *             using the {@link UserNotExistExceptionMapper}
     */
    @Path("{id}")
    @GET
    @Produces( {MediaType.APPLICATION_XML})
    public User getUser(@PathParam("id") int id) throws UserNotExistException {
        User u = users.get(id);
        if (u == null) {
            throw new UserNotExistException(String.valueOf(id));
        }
        return u;
    }
}
