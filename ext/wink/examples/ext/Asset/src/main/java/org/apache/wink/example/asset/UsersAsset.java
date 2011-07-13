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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.annotations.Asset;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndFeed;
import org.apache.wink.common.model.synd.SyndPerson;
import org.apache.wink.common.model.synd.SyndText;

@Asset
public class UsersAsset {

    private List<User> users;

    public UsersAsset(Collection<User> users) {
        this.users = new ArrayList<User>(users.size());
        this.users.addAll(users);
    }

    @Produces(MediaType.APPLICATION_XML)
    public Users getUsers() {
        return new Users(users);
    }

    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
    public SyndFeed getSyndFeed(@Context Providers providers, @Context UriInfo uriInfo) {
        SyndFeed synd = new SyndFeed();
        synd.setId("urn:com:hp:users");
        synd.setTitle(new SyndText("Users"));
        synd.addAuthor(new SyndPerson("admin"));
        synd.setUpdated(new Date());

        // set the entries
        for (User user : users) {
            SyndEntry entry = new SyndEntry();
            entry.setId(Integer.toString(user.getId()));
            entry.setPublished(new Date(System.currentTimeMillis()));
            entry.setUpdated(new Date(System.currentTimeMillis()));
            entry.setTitle(new SyndText(user.getLastName() + " " + user.getFirstName()));
            synd.addEntry(entry);
        }
        synd.setBase(uriInfo.getAbsolutePath().toString());
        return synd;
    }

}
