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
package org.apache.wink.example.qadefect.resources;

import java.net.URI;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.wink.common.annotations.Workspace;
import org.apache.wink.example.qadefect.legacy.DataStore;
import org.apache.wink.example.qadefect.legacy.TestBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Workspace(workspaceTitle = "QA Defects", collectionTitle = "Tests")
@Path(TestsResource.TESTS_PATH)
public class TestsResource {

    private static final Logger logger             = LoggerFactory.getLogger(TestsResource.class);

    public static final String  TESTS_PATH         = "tests";
    public static final String  TEST_PATH          = "{test}";

    /**
     * <p>
     * Handler for requests for collection of tests of QA application
     * <p>
     * Resource is invoked for HTTP requests with URI:
     * <code>http://[server]:[port]/qa/tests-list</code>
     */

    private static final String URL_TO_REDIRECT    = "/applicationJSPs/testCollection.jsp";
    private static final String SHOW_ALL_ATTR_NAME = "showAll";

    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
    public TestsAsset getTests(@Context UriInfo uriInfo) {

        // initialize the memory store
        DataStore store = DataStore.getInstance();

        // create data object (populated with store data)
        Collection<TestBean> tests = store.getTests();
        if (tests == null) {
            logger.error("No tests found");
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return new TestsAsset(tests);

    }

    /**
     * Handles the GET request to collection of Tests in HTML representation.
     * 
     * @return Response with new Location to redirect
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response redirectToApplication(@Context HttpServletRequest request) {
        String locationStr =
            request.getContextPath() + URL_TO_REDIRECT + "?" + SHOW_ALL_ATTR_NAME + "=" + true;
        URI location = URI.create(locationStr);
        return Response.seeOther(location).build();
    }

    @Path(TEST_PATH)
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public TestAsset getTest(@PathParam("test") String testId) {

        // initialize memory store
        DataStore store = DataStore.getInstance();

        // create data object (populated with store data)
        TestBean test = store.getTest(testId);
        if (test == null) {
            logger.error("Test {} was not found", testId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        // create a defect resource and set its data and metadata
        return new TestAsset(test);
    }

}
