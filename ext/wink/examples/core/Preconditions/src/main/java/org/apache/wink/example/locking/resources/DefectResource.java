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

package org.apache.wink.example.locking.resources;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.apache.wink.common.annotations.Workspace;
import org.apache.wink.example.locking.legacy.DefectBean;
import org.apache.wink.example.locking.legacy.DefectsBean;
import org.apache.wink.example.locking.store.DataStore;

/**
 * <p>
 * This example demonstrates usage of Preconditions to create an <a
 * href="http://en.wikipedia.org/wiki/Optimistic_concurrency_control">Optimistic
 * Concurrency Control</a> functionality.
 */
@Path("defects")
@Workspace(workspaceTitle = "QA Defects", collectionTitle = "Defects")
public class DefectResource {

    public static final String DEFECT     = "defect";
    public static final String DEFECT_URL = "/{" + DEFECT + "}";

    /**
     * memory store
     */
    private DataStore          store      = DataStore.getInstance();

    /**
     * Returns the collection of defects.
     * <p>
     * If the store wasn't modified from the last call, the method returns 304
     * (NOT_MODIFIED).
     */
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public Response getDefectsCollection(@Context Request request) {

        // verify that the store was modified since the last call
        Date lastModifiedIgnoreMillis = store.getLastModifiedIgnoreMillis();
        ResponseBuilder precondition = request.evaluatePreconditions(lastModifiedIgnoreMillis);
        if (precondition != null) {
            // the collection wasn't modified, 304 will be returned
            return precondition.build();
        }

        // the collection was modified, retrieve it from the store
        Collection<DefectBean> defects = store.getDefects();

        // return the collection and add its last modified date on the response
        return Response.ok(new DefectsBean(defects)).lastModified(lastModifiedIgnoreMillis).build();
    }

    /**
     * Returns a single defect.
     * <p>
     * If the defect with the given id doesn't exist in the store, 404 NOT_FOUND
     * is returned
     * <p>
     * If IF_NONE_MATCH header present, the defect will be returned only if it
     * was modified since the previous call.
     */
    @GET
    @Path(DEFECT_URL)
    @Produces(MediaType.APPLICATION_XML)
    public Response getDefect(@Context Request request, @PathParam(DEFECT) String defectId) {

        // get defect from the store
        DefectBean bean = store.getDefect(defectId);
        if (bean == null) {
            // defect was not found
            return Response.status(Status.NOT_FOUND).build();
        }

        // create defect's etag
        EntityTag defectBeanEtag = new EntityTag(String.valueOf(bean.hashCode()));

        // evaluate the precondition
        ResponseBuilder precondition = request.evaluatePreconditions(defectBeanEtag);
        if (precondition != null) {
            // defect was not modified, return 304
            return precondition.build();
        }

        // create response the defect and its entity tag
        return Response.ok(bean).tag(defectBeanEtag).build();
    }

    /**
     * Adds a new defect to the collection. The created defect is returned along
     * with its etag to the client.
     */
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response addDefect(@Context UriInfo uriInfo, DefectBean bean) throws IOException,
        URISyntaxException {

        // verify that bean was sent
        if (bean == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // set unique Id in the new defect bean:
        // - Id in the input data is ignored, actually there should be no Id
        // there,
        bean.setId(store.getDefectUniqueId());

        // add defect bean to the memory store
        store.putDefect(bean.getId(), bean);

        // header Location (absolute URI) must exist on the response in case of
        // status code 201
        URI location = new URI(uriInfo.getAbsolutePath() + "/" + bean.getId());

        // create entity tag, so the Client can use it for OCC
        EntityTag entityTag = new EntityTag(String.valueOf(bean.hashCode()));

        return Response.status(Status.CREATED).entity(bean).location(location).tag(entityTag)
            .build();
    }

    /**
     * Updates defect.
     * <p>
     * The defect is updated only if the If-Match header is present and
     * evaluation of precondition succeeds. This is done to ensure the OCC.
     */
    @PUT
    @Path(DEFECT_URL)
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response updateDefect(@Context Request request,
                                 @PathParam(DEFECT) String defectId,
                                 @HeaderParam(HttpHeaders.IF_MATCH) String ifMatchHeader,
                                 DefectBean updatedBean) throws IOException {

        if (ifMatchHeader == null) {
            // IF-MATCH header wasn't sent, cannot validate the precondition
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // obtain data object from the memory store
        DefectBean bean = store.getDefect(defectId);
        if (bean == null) {
            // not found, return 404
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        // create defect's etag
        EntityTag defectBeanEtag = new EntityTag(String.valueOf(bean.hashCode()));

        ResponseBuilder preconditions = request.evaluatePreconditions(defectBeanEtag);
        if (preconditions != null) {
            return preconditions.build();
        }

        updatedBean.setId(defectId);

        // update defect legacy bean to the memory store
        store.putDefect(defectId, updatedBean);

        Response response =
            Response.ok(updatedBean).tag(new EntityTag(String.valueOf(updatedBean.hashCode())))
                .build();
        return response;
    }

    /**
     * Deletes defect.
     * <p>
     * The defect is deleted only if If-Match is present and valid to ensure
     * OCC.
     */
    @DELETE
    @Path(DEFECT_URL)
    @Produces(MediaType.APPLICATION_XML)
    public Object deleteDocument(@Context Request request,
                                 @PathParam(DEFECT) String defectId,
                                 @HeaderParam(HttpHeaders.IF_MATCH) String ifMatchHeader) {

        if (ifMatchHeader == null) {
            // IF-MATCH header wasn't sent, cannot validate the precondition
            throw new WebApplicationException(Status.BAD_REQUEST);
        }

        // obtain data object from memory store
        DefectBean bean = store.getDefect(defectId);
        if (bean == null) {
            // defect not found, return 404
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        // create defect's etag
        EntityTag defectBeanEtag = new EntityTag(String.valueOf(bean.hashCode()), false);

        ResponseBuilder preconditions = request.evaluatePreconditions(defectBeanEtag);
        if (preconditions != null) {
            return preconditions.build();
        }

        return store.removeDefect(defectId);
    }

}
