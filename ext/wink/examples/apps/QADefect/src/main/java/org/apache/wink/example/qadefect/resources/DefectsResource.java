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

import java.io.IOException;
import java.io.InputStream;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.wink.common.annotations.Workspace;
import org.apache.wink.common.internal.utils.MediaTypeUtils;
import org.apache.wink.common.internal.utils.OpenSearchUtils;
import org.apache.wink.common.model.opensearch.OpenSearchDescription;
import org.apache.wink.common.model.opensearch.OpenSearchImage;
import org.apache.wink.common.model.opensearch.OpenSearchParameter;
import org.apache.wink.common.model.opensearch.OpenSearchQuery;
import org.apache.wink.common.model.opensearch.OpenSearchUrl;
import org.apache.wink.example.qadefect.legacy.DataStore;
import org.apache.wink.example.qadefect.legacy.DefectBean;
import org.apache.wink.example.qadefect.utils.SearchMap;
import org.apache.wink.server.utils.LinkBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Workspace(workspaceTitle = "QA Defects", collectionTitle = "Defects")
@Path(DefectsResource.DEFECTS_PATH)
public class DefectsResource {

    private static final Logger logger                 =
                                                           LoggerFactory
                                                               .getLogger(DefectsResource.class);

    public static final String  DEFECTS_PATH           = "defects";
    public static final String  DEFECT_VAR             = "defect";
    public static final String  DEFECT_PATH            = "{" + DEFECT_VAR + "}";
    public static final String  DEFECT_ATTACHMENT_PATH = DEFECT_PATH + "/attachment";
    public static final String  SEVERIIY               = "severity";
    public static final String  ASSIGNED_TO            = "assignedTo";
    public static final String  URN_ASSIGNED_TO        = "urn:hp:defect:assignedTo";
    public static final String  URN_SEVERIIY           = "urn:hp:defect:severity";
    public static final String  FTS                    = "q";

    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML,
        "text/csv"})
    public DefectsAsset getDefects(@QueryParam(FTS) String query,
                                   @QueryParam(SEVERIIY) String severity,
                                   @QueryParam(ASSIGNED_TO) String assignedTo) {

        DataStore store = DataStore.getInstance();

        // fill search parameters
        // the parameters may be absent, the SearchMap will do the filtering
        SearchMap searchParameters = new SearchMap();
        searchParameters.put(FTS, query);
        searchParameters.put(SEVERIIY, severity);
        searchParameters.put(ASSIGNED_TO, assignedTo);

        // get the defects that match the search criteria
        Collection<DefectBean> defects = store.getDefects(searchParameters);
        return new DefectsAsset(defects);
    }

    /**
     * Add OpenSearch representation to this defect collection resource.
     * OpenSearch description document describes search infrastructure
     * interface. (see <a href="http://www.opensearch.org/">OpenSearch</a>)
     */
    @GET
    @Produces(MediaTypeUtils.OPENSEARCH)
    public OpenSearchDescription getOpenSearch(@Context UriInfo info) {
        String baseUri = info.getAbsolutePath().toString();
        OpenSearchDescription openSearchDescription = new OpenSearchDescription();
        openSearchDescription.setShortName("HP Defect Manager search engine");
        openSearchDescription.setDescription("You can search defects in HP Defect Manager");
        openSearchDescription.setLongName("HP Defect Manager search engine");
        openSearchDescription.setContact("john.smith@example.com");
        openSearchDescription.setDeveloper("John Smith");
        openSearchDescription.addLanguage("en-US");
        openSearchDescription.setTags("defect bug");
        openSearchDescription.addInputEncoding("UTF-8");
        openSearchDescription.addOutputEncoding("UTF-8");

        // set OpenSearch URL parameters
        OpenSearchParameter severityParameter =
            new OpenSearchParameter(SEVERIIY, URN_SEVERIIY, false);
        OpenSearchParameter ftsParameter =
            new OpenSearchParameter(FTS, OpenSearchParameter.OpenSearchParams.searchTerms
                .toString(), false);
        OpenSearchParameter assignedToParameter =
            new OpenSearchParameter(ASSIGNED_TO, URN_ASSIGNED_TO, false);

        // create Search URL & populate search parameters for browsers
        OpenSearchUrl openSearchUrlForBrowsers = new OpenSearchUrl();
        openSearchUrlForBrowsers.addOpenSearchParameter(ftsParameter);
        openSearchUrlForBrowsers.setType(MediaType.TEXT_HTML);

        // create Search URL & populate search parameters
        OpenSearchUrl openSearchUrl = new OpenSearchUrl();
        openSearchUrl.addOpenSearchParameter(severityParameter);
        openSearchUrl.addOpenSearchParameter(ftsParameter);
        openSearchUrl.addOpenSearchParameter(assignedToParameter);
        openSearchUrl.setType(MediaType.TEXT_HTML);

        // create open search base uri
        StringBuilder openSearchUrlBuilder = new StringBuilder(baseUri);

        openSearchUrl.setBaseUri(openSearchUrlBuilder.toString());
        openSearchUrlForBrowsers.setBaseUri(openSearchUrlBuilder.toString());

        // add URLs to OpenSearch
        openSearchDescription.addUrl(openSearchUrlForBrowsers);
        openSearchDescription.addUrl(openSearchUrl);

        // add OpenSearch Query element
        OpenSearchQuery openSearchQuery = new OpenSearchQuery();
        openSearchQuery.setRole(OpenSearchQuery.QueryRole.example.toString());
        openSearchQuery.setSearchTerms("Search Terms");
        openSearchDescription.addQuery(openSearchQuery);

        // add OpenSearch Images
        OpenSearchImage openSearchImage;
        openSearchImage =
            OpenSearchUtils.createOpenSearchImage(MediaTypeUtils.IMAGE_JPEG, openSearchUrlBuilder
                .toString() + "splash.jpg");
        openSearchDescription.addNewImage(openSearchImage);

        return openSearchDescription;
    }

    @POST
    @Consumes({"text/csv"})
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public Response createDefects(DefectsAsset defects,
                                  @Context LinkBuilders linkProcessor,
                                  @Context UriInfo uriInfo) {

        // add the defects to the defects store
        DataStore store = DataStore.getInstance();
        String id = null;
        for (DefectBean defect : defects.getDefects()) {
            id = store.getDefectUniqueId();
            defect.setId(id);
            store.putDefect(id, defect);
        }

        // return the created defects and set the status code to created (201)
        URI location = uriInfo.getAbsolutePathBuilder().segment(id).build();
        return Response.created(location).entity(defects).build();
    }

    @POST
    @Consumes( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public Response createDefect(DefectAsset asset, @Context UriInfo uriInfo) throws IOException,
        Exception {

        // if content was not sent => there is no meaning for the defect, throw
        // exception.
        if (asset.getDefect() == null) {
            logger.error("The content of the defect is missing");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        // add the defect to the defects store and set it a new ID
        DataStore store = DataStore.getInstance();
        String id = store.getDefectUniqueId();
        DefectBean defect = asset.getDefect();
        defect.setId(id);
        store.putDefect(id, defect);

        // return the defect and set the status code to created (201)
        URI location = uriInfo.getAbsolutePathBuilder().segment(id).build();
        return Response.created(location).entity(asset).build();
    }

    @Path(DEFECT_PATH)
    @GET
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML, MediaType.TEXT_HTML})
    public DefectAsset getDefect(@PathParam("defect") String defectId) {
        // fetch the defect bean from the store, throw 404 in case it does not
        // exist
        DataStore store = DataStore.getInstance();
        DefectBean defect = store.getDefect(defectId);
        if (defect == null) {
            logger.info("Defect {} does not exist", defectId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return new DefectAsset(defect);
    }

    /**
     * <p>
     * This method is handling GET requests for specific defects' attachment.
     * <ul>
     * <em>Examples of handled URIs:</em>
     * <li><code>/defects/1/attachment</code> - returns defects' attachment in
     * JPEG format.
     *
     * @param defectId defect id
     * @return resource of defects' attachment
     */
    @Path(DEFECT_ATTACHMENT_PATH)
    @GET
    @Produces( {MediaTypeUtils.IMAGE_JPEG})
    public InputStream getDefectAttachement(@PathParam("defect") String defectId) {

        DataStore store = DataStore.getInstance();

        // create data object (populated with store data)
        DefectBean defect = store.getDefect(defectId);
        if (defect == null) {
            logger.info("Defect {} does not exist", defectId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        String path = defect.getPathToAttachment();
        if (path == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        InputStream attachmentStream =
            this.getClass().getResourceAsStream("../representation/" + path);
        return attachmentStream;
    }

    @Path(DEFECT_PATH)
    @PUT
    @Consumes( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML})
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public DefectAsset updateDefect(DefectAsset asset,
                                    @Context LinkBuilders linkProcessor,
                                    @PathParam("defect") String defectId) {

        DataStore store = DataStore.getInstance();
        // verify Defect exist in the store, return 404 otherwise
        DefectBean bean = store.getDefect(defectId);
        if (bean == null) {
            logger.info("Defect {} does not exist", defectId);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        // set Id in the resources data for cases that element <id> is missing
        // in the request body
        DefectBean defect = asset.getDefect();
        defect.setId(defectId);

        // update defect legacy bean to the store
        store.putDefect(defectId, defect);
        return asset;
    }

    @Path(DEFECT_PATH)
    @DELETE
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public DefectAsset deleteDefect(@PathParam("defect") String defectId) {
        DataStore store = DataStore.getInstance();
        DefectBean defect = store.getDefect(defectId);
        store.removeDefect(defectId);
        return new DefectAsset(defect);
    }

}
