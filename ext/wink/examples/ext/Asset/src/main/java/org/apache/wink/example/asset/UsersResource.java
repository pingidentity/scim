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

package org.apache.wink.example.asset;

import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.wink.common.annotations.Workspace;

/**
 * Sample service for demonstrating the use of Asset concept
 */
@Workspace(workspaceTitle = "Users Service", collectionTitle = "Users")
@Path("users")
public class UsersResource {

    static private int                    maxUserID = 0;

    static private HashMap<Integer, User> users     = new HashMap<Integer, User>() {
                                                        private static final long serialVersionUID =
                                                                                                       -2823817865466342159L;
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
     * Get a list of all the existing users as xml or as json
     * 
     * @return an instance of Users
     */
    @GET
    @Produces( {MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
        MediaType.APPLICATION_JSON})
    public UsersAsset getUsers() {
        return new UsersAsset(users.values());
    }

    /**
     * Create a new user by receiving it as xml, and returning it as xml or json
     * 
     * @return the created user
     */
    @POST
    @Consumes( {MediaType.APPLICATION_XML})
    @Produces( {MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    public User putUser(User user) {
        maxUserID++;
        user.setId(maxUserID);
        users.put(user.getId(), user);
        return user;
    }

}
