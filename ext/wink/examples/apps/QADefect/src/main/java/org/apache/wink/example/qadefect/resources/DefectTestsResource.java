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

import java.util.List;

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
import org.apache.wink.example.qadefect.legacy.DefectBean;
import org.apache.wink.example.qadefect.legacy.TestBean;
import org.apache.wink.server.utils.LinkBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Handler for requests for collection of tests that are related to the specific
 * defect
 * <p>
 * Resource is invoked for HTTP requests with URI:
 * <code>http://[server]:[port]/qa/defects-list/2/tests</code>
 */
@Path("/defects/{defect}/tests")
@Workspace(workspaceTitle = "QA Defects", collectionTitle = "Tests of Defect")
public class DefectTestsResource {

    private static final Logger logger = LoggerFactory.getLogger(DefectTestsResource.class);

    /**
     * Method is handling GET requests to retrieve tests that are related to the
     * specific defect.
     * 
     * @param defectId defectId path variable
     * @return response object
     */
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
    public TestsAsset getTests(@Context LinkBuilders linkProcessor,
                               @Context UriInfo uriInfo,
                               @PathParam("defect") String defectId) {

        // initialize memory store
        DataStore store = DataStore.getInstance();

        // create data object (populated with store data)
        DefectBean defect = store.getDefect(defectId);
        if (defect == null) {
            logger.error("Defect {} was not found", defectId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        List<TestBean> tests = defect.getTests();
        if (tests == null || tests.isEmpty()) {
            logger.error("Defect {} has not tests", defectId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return new TestsAsset(tests);
    }

}
