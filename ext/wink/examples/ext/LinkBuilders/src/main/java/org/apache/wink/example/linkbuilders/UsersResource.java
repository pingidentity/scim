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

package org.apache.wink.example.linkbuilders;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.wink.common.annotations.Workspace;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndFeed;
import org.apache.wink.common.model.synd.SyndPerson;
import org.apache.wink.common.model.synd.SyndText;
import org.apache.wink.server.utils.LinkBuilders;

/**
 * Sample service for demonstrating the use of LinkBuilders concept
 */
@Workspace(workspaceTitle = "Users Service", collectionTitle = "Users")
@Path("users")
public class UsersResource {

    private static final String           USER_ID   = "userId";
    private static final String           USER_INFO = "{" + USER_ID + "}";

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
     * Get a list of all the existing users. The method returns SyndFeed as a
     * response. That is, Client will be able to request a users collection
     * information as an XML.
     * 
     * @param linkBuilders reference to LinkBuilders instance
     * @return an instance of SyndFeed
     */
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML})
    public SyndFeed getUsers(@Context LinkBuilders linkBuilders) {

        // Build SyndFeed that holds users collection.
        // Generate a system links for collection and for each collection member
        // by using LinkBuilders
        SyndFeed usersSyndFeed = createUsersSyndFeed(users.values(), linkBuilders);

        return usersSyndFeed;
    }

    /**
     * This method builds SyndFeed that contains a collection of all users It
     * generate a system links for the collection and for each collection
     * members.
     * 
     * @param users collection
     * @param linkBuilders reference to LinkBuilders instance
     * @return SyndFeed syndication feed
     */
    private SyndFeed createUsersSyndFeed(Collection<User> users, LinkBuilders linkBuilders) {
        SyndFeed usersSyndFeed = new SyndFeed();
        usersSyndFeed.setId("urn:com:hp:users");
        usersSyndFeed.setTitle(new SyndText("Users"));
        usersSyndFeed.addAuthor(new SyndPerson("admin"));
        usersSyndFeed.setUpdated(new Date());

        // 1. Generate collection system links: "edit", "alternate"
        linkBuilders.createSystemLinksBuilder(). // Create SystemLinksBuilder
            build(usersSyndFeed.getLinks()); // Build all system links

        // 2. Add "custom" collection link with type "search". Make it always be
        // absolute
        linkBuilders.createSingleLinkBuilder(). // Create instance of
                                                // SingleLinkBuilder
            queryParam("q", "username"). // Set link query parameter
            rel("search"). // Set link relation type
            relativize(false). // Create absolute URI
            build(usersSyndFeed.getLinks()); // Build link

        // 3. Populate and set the collection members
        for (User user : users) {
            SyndEntry userSyndEntry = createUserSyndEntry(user);

            // generate collection member (single user) system links: "edit",
            // "alternate"
            // all link must be relative to users collection URI "users"
            linkBuilders.createSystemLinksBuilder().subResource(userSyndEntry.getId())
                .build(userSyndEntry.getLinks());

            usersSyndFeed.addEntry(userSyndEntry);
        }
        return usersSyndFeed;
    }

    /**
     * Get a single user by user id. The method returns SyndEntry as a response.
     * That is, Client will be able to request a user information as an XML.
     * 
     * @param userId an id of a user that is to be retrieved
     * @param linkBuilders reference to LinkBuilders instance
     * @return an instance of SyndEntry
     * @throws URISyntaxException
     */
    @Path(USER_INFO)
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML})
    public SyndEntry getUserInfo(@PathParam(USER_ID) Integer userId,
                                 @Context LinkBuilders linkBuilders) throws URISyntaxException {

        // 1. Build SyndEntry that holds a single user information
        User user = users.get(userId);
        if (user == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        // 2. Create SyndEntry that hold single user information
        SyndEntry userSyndEntry = createUserSyndEntry(user);

        // 3. Generate all user supported system links: "edit", "alternate" etc.
        linkBuilders.createSystemLinksBuilder(). // Create SystemLinksBuilder
            build(userSyndEntry.getLinks()); // Build all system links

        return userSyndEntry;
    }

    /**
     * Create a SyndEntry that contains a single user data
     * 
     * @param user object
     * @return SyndEntry syndication entry
     */
    private SyndEntry createUserSyndEntry(User user) {
        SyndEntry entry = new SyndEntry();
        entry.setId(Integer.toString(user.getId()));
        entry.setPublished(new Date(System.currentTimeMillis()));
        entry.setUpdated(new Date(System.currentTimeMillis()));
        entry.setTitle(new SyndText(user.getLastName() + " " + user.getFirstName()));
        return entry;
    }
}
