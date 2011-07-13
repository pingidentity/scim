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

package org.apache.wink.example.simpledefects.resources;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.wink.common.annotations.Workspace;
import org.apache.wink.example.simpledefects.legacy.DataStore;
import org.apache.wink.example.simpledefects.legacy.DefectBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Handler for requests for collection of defects of QA application
 * <p>
 * The Resource is invoked for HTTP requests with URI:
 * <code>http://[server]:[port]/qa/defects</code>
 */
@Path(DefectsResource.DEFECTS_URL)
@Workspace(workspaceTitle = "QA Defects", collectionTitle = "Defects")
public class DefectsResource {

    private static final Logger logger      = LoggerFactory.getLogger(DefectsResource.class);

    public static final String  DEFECTS_URL = "/defects";
    public static final String  DEFECT      = "defect";
    public static final String  DEFECT_URL  = "{" + DEFECT + "}";

    /**
     * <p>
     * Method is handling GET requests for collection of defects.
     * <ul>
     * <em>Examples of handled URIs:</em>
     * <li><code>/defects</code> - returns defects in representation native to
     * browser or specified in header <code>Accept:</code>
     * <li><code>/defects?alt=application/atom%2Bxml</code> - returns Atom feed
     * representation. Each defect is represented by Atom entry.
     * <li><code>/defects?alt=text/javascript</code> - returns javascript (Json)
     * representation
     * <li><code>/defects?alt=text/javascript&callback=getDefects</code> -
     * returns javascript (Json) representation wrapped in javascript function
     * getDefects
     * </ul>
     * 
     * @return response with requested resource representation
     */
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
    public DefectsAsset getDefects() {
        DataStore store = DataStore.getInstance();
        Collection<DefectBean> defects = store.getDefects();
        DefectsAsset asset = new DefectsAsset(defects);
        return asset;
    }

    @POST
    @Consumes( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML})
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public Response createDefect(DefectAsset asset, @Context UriInfo uriInfo) {
        // if content was not sent => there is no meaning for the defect, throw
        // exception.
        DefectBean defect = asset.getDefect();
        if (defect == null) {
            logger.error("The content of the defect is missing");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // set unique Id in the new defect bean:
        // - Id in the input data is ignored, actually there should be no Id
        // there,
        // - resource collection always decides about the unique Id of its own
        // entries,
        // - in our case we use a helper method to generate a unique Id
        defect.setId(DataStore.getInstance().getDefectUniqueId());

        // add defect legacy bean to the memory store
        DataStore store = DataStore.getInstance();
        store.putDefect(defect.getId(), defect);

        // return status code 201 (created) with the created defect
        URI location = uriInfo.getAbsolutePathBuilder().segment(defect.getId()).build();
        return Response.status(Response.Status.CREATED).entity(asset).location(location)
            .tag(new EntityTag(String.valueOf(defect.hashCode()))).build();
    }

    @Path(DEFECT_URL)
    @GET
    @Produces( {MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML,
        MediaType.APPLICATION_JSON})
    public DefectAsset getDefect(@PathParam(DEFECT) String defectId) {
        // initialize memory store
        DataStore store = DataStore.getInstance();
        // create data object (populated with store data)
        DefectBean defect = store.getDefect(defectId);
        if (defect == null) {
            logger.error("Defect {} was not found", defectId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        // create a new instance of a defect asset
        return new DefectAsset(defect);
    }

    @Path(DEFECT_URL)
    @PUT
    @Consumes( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML})
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public DefectAsset updateDefect(DefectAsset asset,
                                    @Context UriInfo uriInfo,
                                    @PathParam(DEFECT) String defectId) throws IOException {

        // initialize the memory store
        DataStore store = DataStore.getInstance();

        // verify the defect exists in the store
        DefectBean defect = store.getDefect(defectId);
        if (defect == null) {
            logger.error("Defect {} was not found", defectId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        // set Id in the resources data for cases that element <id> is missing
        // in the request body
        defect = asset.getDefect();
        defect.setId(defectId);
        // update defect legacy bean to the memory store
        store.putDefect(defect.getId(), defect);
        return asset;
    }

    /**
     * Method id handling DELETE requests. DELETE request deletes existing
     * resource. Server side deletes resource, if it exists, and returns status
     * code 200 (OK). Requested representation of the deleted resource is
     * returned in the response.
     * 
     * @param defectId defect id from request URL
     * @return requested resource representation of the just deleted resource
     */
    @Path(DEFECT_URL)
    @DELETE
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public DefectAsset deleteDefect(@Context UriInfo uriInfo, @PathParam(DEFECT) String defectId) {

        // initialize the memory store
        DataStore store = DataStore.getInstance();

        // obtain data object from memory store
        DefectBean defect = store.getDefect(defectId);
        if (defect == null) {
            logger.error("Defect {} was not found", defectId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        // remove defect legacy bean from memory store
        store.removeDefect(defect.getId());

        // create new asset and set defect bean that is being deleted as its
        // data object
        DefectAsset asset = new DefectAsset(defect);
        return asset;
    }

}
