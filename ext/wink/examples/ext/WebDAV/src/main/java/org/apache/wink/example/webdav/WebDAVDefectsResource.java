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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.http.OPTIONS;
import org.apache.wink.common.model.synd.SyndBase;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndFeed;
import org.apache.wink.common.utils.ProviderUtils;
import org.apache.wink.example.qadefect.legacy.DefectBean;
import org.apache.wink.example.qadefect.resources.DefectAsset;
import org.apache.wink.example.qadefect.resources.DefectsResource;
import org.apache.wink.server.utils.LinkBuilders;
import org.apache.wink.webdav.WebDAVHeaders;
import org.apache.wink.webdav.WebDAVMethod;
import org.apache.wink.webdav.model.Getcontentlength;
import org.apache.wink.webdav.server.WebDAVResponseBuilder;
import org.apache.wink.webdav.server.WebDAVUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This resource handles WebDAV methods for collection of defects.
 */
@Path(DefectsResource.DEFECTS_PATH)
public class WebDAVDefectsResource extends DefectsResource {

    private static final Logger logger = LoggerFactory.getLogger(WebDAVDefectsResource.class);

    @Context
    private LinkBuilders        linkProcessor;
    @Context
    private UriInfo             uriInfo;
    @Context
    private Providers           providers;

    /**
     * Handles WebDAV PROPFIND method.
     */
    @WebDAVMethod.PROPFIND
    @Consumes( {MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    @Produces(MediaType.APPLICATION_XML)
    public Response findProperties(String body, @Context HttpHeaders headers) throws IOException {
        // create a new instance of a CollectionPropertyHandler that holds an
        // instance of DefectPropertyHandler. The DefectPropertyHandler is used
        // to set
        // the properties of entries in the collection.
        WebDAVResponseBuilder.CollectionPropertyHandler handler =
            new WebDAVResponseBuilder.CollectionPropertyHandler(new DefectPropertyHandler());

        // get the feed of the defects
        SyndFeed feed =
            super.getDefects(null, null, null).getSyndFeed(providers, linkProcessor, uriInfo);
        // use the feed to create the propfind response
        String depth = headers.getRequestHeaders().getFirst(WebDAVHeaders.DEPTH);
        return WebDAVResponseBuilder.create(uriInfo).propfind(feed, body, depth, handler);
    }

    /**
     * Handles WebDAV PROPFIND method.
     */
    @Path(DEFECT_PATH)
    @WebDAVMethod.PROPFIND
    @Consumes( {MediaType.APPLICATION_XML, MediaType.TEXT_XML})
    @Produces(MediaType.APPLICATION_XML)
    public Response findProperties(String body, @PathParam("defect") String defectId)
        throws IOException {
        SyndEntry entry = super.getDefect(defectId).getSyndEntry(providers, uriInfo, linkProcessor);
        return WebDAVResponseBuilder.create(uriInfo).propfind(entry,
                                                              body,
                                                              new DefectPropertyHandler());
    }

    /**
     * We need to support also updates with
     * <code>application/octet-stream</code> or unspecified MIME types. It
     * creates new document if it does not exist - WebDAV uses PUT method for
     * creating of new resources instead of POST.
     * 
     * @throws Exception
     */
    @Path(DEFECT_PATH)
    @PUT
    @Consumes( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_XML,
        MediaType.APPLICATION_OCTET_STREAM})
    @Produces( {MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML})
    public DefectAsset updateDefect(String body, @PathParam("defect") String defectId)
        throws Exception {

        // the media type of the defect is not sent because it is not known,
        // so we need to guess it
        body = body.trim();
        DefectAsset asset = new DefectAsset();
        if (body.startsWith("<defect")) {
            // read the incoming entity as xml
            DefectBean defect =
                ProviderUtils.readFromString(providers,
                                             body,
                                             DefectBean.class,
                                             MediaType.APPLICATION_XML_TYPE);
            asset.setDefect(defect);
        } else if (body.startsWith("<entry")) {
            // read the incoming entity as an atom entry
            SyndEntry entry =
                ProviderUtils.readFromString(providers,
                                             body,
                                             SyndEntry.class,
                                             MediaType.APPLICATION_ATOM_XML_TYPE);
            asset.setSyndEntry(entry, providers);
        } else {
            logger.error("WebDAV defect update can only handle xml or atom entry");
            throw new WebApplicationException(Response.Status.UNSUPPORTED_MEDIA_TYPE);
        }

        asset.getDefect().setId(defectId);

        try {
            // update the defect
            return super.updateDefect(asset, linkProcessor, defectId);
        } catch (WebApplicationException e) {
            if (Response.Status.NOT_FOUND.getStatusCode() == e.getResponse().getStatus()) {
                // create a new defect if it does not exist
                asset = (DefectAsset)super.createDefect(asset, uriInfo).getEntity();
                return asset;
            } else {
                throw e;
            }
        }
    }

    @OPTIONS
    public Response getOptions() {
        return WebDAVUtils.getOptions(uriInfo);
    }

    @Path(DEFECT_PATH)
    @OPTIONS
    public Response getOptionsDefect() {
        return getOptions();
    }

    @Path(DEFECT_PATH)
    @WebDAVMethod.LOCK
    @Produces(MediaType.APPLICATION_XML)
    public Response lock(String body) {
        return WebDAVUtils.msCompatibilityLock(body);
    }

    @Path(DEFECT_PATH)
    @WebDAVMethod.UNLOCK
    public Response unlock() {
        return WebDAVUtils.msCompatibilityUnlock();
    }

    /**
     * This property handler overrides the
     * {@link WebDAVResponseBuilder.PropertyHandler#setPropertyValue(WebDAVResponseBuilder, org.apache.wink.webdav.model.Response, Object, SyndBase)}
     * method in order to set the getcontentlength property of a defect
     */
    public class DefectPropertyHandler extends WebDAVResponseBuilder.PropertyHandler {

        public DefectPropertyHandler() {
        }

        @Override
        public void setPropertyValue(WebDAVResponseBuilder builder,
                                     org.apache.wink.webdav.model.Response response,
                                     Object property,
                                     SyndBase synd) {
            // if the property being set is getcontentlength
            if (property instanceof Getcontentlength) {
                // serialize the synd to atom to get its length as atom
                try {
                    String body =
                        ProviderUtils.writeToString(providers,
                                                    synd,
                                                    MediaType.APPLICATION_ATOM_XML_TYPE);
                    ((Getcontentlength)property).setValue(String.valueOf(body.length()));
                    response.setPropertyOk(property);
                    return;
                } catch (IOException e) {
                    throw new WebApplicationException(e);
                }
            }
            // delegate to the parent for all other properties
            super.setPropertyValue(builder, response, property, synd);
        }
    }

}
