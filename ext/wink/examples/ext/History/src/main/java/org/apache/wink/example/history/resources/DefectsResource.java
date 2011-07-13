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

package org.apache.wink.example.history.resources;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.common.annotations.Workspace;
import org.apache.wink.common.http.HttpStatus;
import org.apache.wink.example.history.legacy.DataStore;
import org.apache.wink.example.history.legacy.DefectBean;
import org.apache.wink.server.utils.LinkBuilders;

/**
 * A sample application for managing defects of a defect tracking application.
 * The defects are managed in an in-memory store, while also maintaining the
 * history of every defect. The complete history of a defect is available, as
 * well as the individual revision of each defect.
 */
@Workspace(workspaceTitle = "QA Defects", collectionTitle = "Defects")
@Path("defects")
public class DefectsResource {

    public static final String REVISION           = "rev";
    public static final String DEFECT_VAR         = "defect";
    public static final String DEFECT_URL         = "{" + DEFECT_VAR + "}";
    public static final String DEFECT_HISTORY_URL = DEFECT_URL + "/history";

    /**
     * Handles GET requests for returning a list of the latest revision of
     * existing defects.
     * <ul>
     * <em>Examples of requests:</em>
     * <li><code>/defects</code> - returns a list of defects in a format
     * specified by the <code>Accept</code> header
     * <li><code>/defects?alt=application/atom+xml</code> - returns an Atom feed
     * representation. Each defect is represented by Atom entry.
     * <li><code>/defects?alt=application/json</code> - returns a Json
     * representation
     * <li><code>/defects?alt=application/json&callback=getDefects</code> -
     * returns Json representation wrapped in javascript function
     * <code>getDefects</code>
     * </ul>
     * 
     * @return DefectsAsset with the list of defects
     */
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
    public DefectsAsset getDefects(@Context LinkBuilders linkProcessor, @Context UriInfo uriInfo) {
        // create data object (populated with store data)
        DataStore store = DataStore.getInstance();
        Collection<DefectBean> defects = store.getDefects();
        return new DefectsAsset(defects);
    }

    /**
     * Handles POST requests for creating a new defect. The server side decides
     * about the new resource URI (Location header) and returns a status code
     * 201 (created). The defect can be received as an Atom entry with xml
     * content (application/atom+xml) or just xml (application/xml), and can be
     * returned as Atom entry, Json or xml.
     * <p>
     * If the defect already existed and was previously deleted, it is
     * undeleted. If the defect is a new defect, the Id must be left blank.
     * 
     * @return response with a "created" status code and the created defect
     *         asset
     */
    @POST
    @Consumes( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML})
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public Response createDefect(DefectAsset asset,
                                 @Context LinkBuilders linkProcessor,
                                 @Context UriInfo uriInfo) {

        // if content was not sent => there is no meaning for the defect, throw
        // exception.
        DefectBean defect = asset.getDefect();
        if (defect == null) {
            throw new WebApplicationException(
                                              new RuntimeException(
                                                                   "The content of the defect is missing"),
                                              Response.Status.BAD_REQUEST);
        }

        DataStore store = DataStore.getInstance();
        if (defect.getId() != null) {
            // undelete operation
            if (!store.isDefectDeleted(defect.getId())) {
                // the defect was already undeleted!
                // it's a conflict!
                return Response.status(Status.CONFLICT).build();
            }
            // this is undelete operation, there is no need to populate new id
        } else {
            // this is a new defect so generate an id
            defect.setId(store.getDefectUniqueId());
        }

        // validate that the user didn't send deleted=true by mistake
        defect.setDeleted(false);

        // add defect legacy bean to the memory store
        store.putDefect(defect.getId(), defect);

        URI location = uriInfo.getAbsolutePathBuilder().segment(defect.getId()).build();
        return Response.created(location).entity(asset).build();
    }

    /**
     * <p>
     * Handles GET requests for a single defect.
     * <ul>
     * <em>Examples of handled URIs:</em>
     * <li><code>/defects/3</code> - returns a defect in a format specified by
     * the <code>Accept</code> header
     * <li><code>/defects/3?alt=application/atom%2Bxml</code> - returns a defect
     * as an Atom entry, where xml of defect is serialized in the
     * <code>atom:content</code> element
     * <li><code>/defects/3?alt=application/json</code> - returns javascript
     * (JSon) format
     * <li><code>/defects/3?alt=application/json&callback=getDefect</code> -
     * returns javascript (JSon) format wrapped in javascript function getDefect
     * <li><code>/defects/3?alt=application/xml</code> - returns serialization
     * of defect into XML
     * </ul>
     * <p>
     * A specific revision of the defect can be retrieved by specifying the
     * "rev" matrix parameter adjacent to the defect id on the uri, like so:
     * <code>/defects/3;rev=1</code>. If no revision is specified then the
     * latest revision is returned.
     * 
     * @return DefectAsset with the requested defect
     */
    @Path(DEFECT_URL)
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public DefectAsset getDefect(@PathParam(DEFECT_VAR) PathSegment segment,
                                 @MatrixParam(REVISION) @DefaultValue("-1") int defectRevision) {

        // create data object (populated with store data)
        DataStore store = DataStore.getInstance();
        String defectId = segment.getPath();
        DefectBean defect = store.getDefect(defectId);
        if (defectRevision != -1) {
            // get specific revision
            // even if the defect was deleted, it is returned
            defect = store.getDefect(defectId, defectRevision);
        }
        if (defect == null) {
            throw new WebApplicationException(new RuntimeException("Defect " + defectId
                + " not found"), Response.Status.NOT_FOUND);
        }

        DefectAsset asset = new DefectAsset(defect);
        asset.setEditable(!store.isDefectDeleted(defectId));
        return asset;
    }

    /**
     * Handles a GET request for the history of a defect. The history can be
     * requested as an Atom feed or as Json. <em>Examples of handled URIs:</em>
     * <li><code>/defects/3/history</code> - returns a defect in a format
     * specified by the <code>Accept</code> header <li>
     * <code>/defects/3/history?alt=application/atom%2Bxml</code> - returns the
     * history as an Atom feed <li>
     * <code>/defects/3/history?alt=application/json</code> - returns the
     * history as Json <li>
     * <code>/defects/3/history?alt=application/json&callback=getDefect</code> -
     * returns the history as Json wrapped in a javascript
     * <code>getDefect</code> method
     * 
     * @param segement
     * @return
     */
    @Path(DEFECT_HISTORY_URL)
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
    public DefectsAsset getDefectHistory(@PathParam(DEFECT_VAR) PathSegment segement) {
        // create data object (populated with store data)
        DataStore store = DataStore.getInstance();
        String defectId = segement.getPath();
        // get the history of the defect
        List<DefectBean> defectHistory = store.getDefectHistory(defectId);
        if (defectHistory.isEmpty()) {
            throw new WebApplicationException(new RuntimeException("Defect " + defectId
                + " not found"), Response.Status.NOT_FOUND);
        }
        return new DefectsAsset(defectHistory, true);
    }

    /**
     * <p>
     * Handles PUT requests for updating a defect. It updates an existing defect
     * with data received in the request body.
     * <p>
     * The defect being updated can be sent as an Atom entry with xml content
     * (application/atom+xml) or just xml (application/xml).
     * <p>
     * The <code>defectRevision</code> parameter of the method exists to make
     * sure that no revision was specified in the update request.
     * 
     * @return DefectAsset with the updated defect
     */
    @Path(DEFECT_URL)
    @PUT
    @Consumes( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML})
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public DefectAsset updateDefect(DefectAsset asset,
                                    @PathParam(DEFECT_VAR) PathSegment segement,
                                    @MatrixParam(REVISION) @DefaultValue("-1") int defectRevision)
        throws IOException {

        if (defectRevision != -1) {
            // it's impossible to update a specific revision
            throw new WebApplicationException(HttpStatus.METHOD_NOT_ALLOWED.getCode());
        }

        // obtain data object from the memory store
        // if the defect was deleted, it cannot be updated anymore.
        DataStore store = DataStore.getInstance();
        String defectId = segement.getPath();
        DefectBean defect = store.getDefect(defectId);
        if (defect == null) {
            throw new WebApplicationException(new RuntimeException("Defect " + defectId
                + " not found"), Response.Status.NOT_FOUND);
        }

        // validate that user didn't send deleted flag, since it will mark
        // defect as deleted.
        // to delete a defect, DELETE http method should be used
        defect = asset.getDefect();
        defect.setDeleted(false);

        // set Id in the asset for cases that element <id> is missing
        // in the request body
        defect.setId(defectId);

        // update defect legacy bean to the memory store
        store.putDefect(defectId, defect);

        return asset;
    }

    /**
     * Handles DELETE requests for deleting an existing defect.
     * <p>
     * The <code>defectRevision</code> parameter of the method exists to make
     * sure that no revision was specified in the delete request.
     * 
     * @return DefectAsset with the deleted defect
     */
    @Path(DEFECT_URL)
    @DELETE
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public Object deleteDocument(@PathParam(DEFECT_VAR) PathSegment segement,
                                 @MatrixParam(REVISION) @DefaultValue("-1") int defectRevision) {
        if (defectRevision != -1) {
            // it's impossible to update a specific revision
            throw new WebApplicationException(HttpStatus.METHOD_NOT_ALLOWED.getCode());
        }

        // obtain data object from memory store
        // if the object was already deleted, null will be returned
        DataStore store = DataStore.getInstance();
        String defectId = segement.getPath();
        DefectBean defect = store.getDefect(defectId);
        if (defect == null) {
            throw new WebApplicationException(new RuntimeException("Defect " + defectId
                + " not found"), Response.Status.NOT_FOUND);
        }

        // remove defect legacy bean from memory store
        store.deleteDefect(defectId);

        return new DefectAsset(defect);
    }
}
