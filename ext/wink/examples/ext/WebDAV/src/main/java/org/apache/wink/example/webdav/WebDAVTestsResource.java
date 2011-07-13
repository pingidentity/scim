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

package org.apache.wink.example.webdav;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.http.OPTIONS;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndFeed;
import org.apache.wink.example.qadefect.resources.TestsResource;
import org.apache.wink.server.utils.LinkBuilders;
import org.apache.wink.webdav.WebDAVHeaders;
import org.apache.wink.webdav.WebDAVMethod;
import org.apache.wink.webdav.server.WebDAVResponseBuilder;
import org.apache.wink.webdav.server.WebDAVUtils;

/**
 * This resource handles WebDAV methods for collection of tests.
 */
@Path(TestsResource.TESTS_PATH)
public class WebDAVTestsResource extends TestsResource {

    @Context
    private Providers    providers;
    @Context
    private LinkBuilders linkProcessor;
    @Context
    private UriInfo      uriInfo;

    /**
     * Handles WebDAV PROPFIND method.
     */
    @WebDAVMethod.PROPFIND
    @Consumes( {MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    @Produces(MediaType.APPLICATION_XML)
    public Response findProperties(String body, @Context HttpHeaders headers) throws IOException {
        SyndFeed feed = super.getTests(uriInfo).getSyndFeed(providers, linkProcessor, uriInfo);
        return WebDAVResponseBuilder.create(uriInfo).propfind(feed,
                                                              body,
                                                              headers.getRequestHeaders()
                                                                  .getFirst(WebDAVHeaders.DEPTH));
    }

    /**
     * Handles WebDAV PROPFIND method.
     */
    @Path(TEST_PATH)
    @WebDAVMethod.PROPFIND
    @Consumes( {MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    @Produces(MediaType.APPLICATION_XML)
    public Response findProperties(String body, @PathParam("test") String testId)
        throws IOException {
        SyndEntry entry = super.getTest(testId).getSyndEntry(providers, uriInfo, linkProcessor);
        return WebDAVResponseBuilder.create(uriInfo).propfind(entry, body);
    }

    @OPTIONS
    public Response getOptions() {
        return WebDAVUtils.getOptions(uriInfo);
    }

    @Path(TEST_PATH)
    @OPTIONS
    public Response getOptionsTest() {
        return getOptions();
    }

}
