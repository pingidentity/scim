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
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.annotations.Asset;
import org.apache.wink.common.model.atom.AtomConstants;
import org.apache.wink.common.model.synd.SyndCategory;
import org.apache.wink.common.model.synd.SyndContent;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndPerson;
import org.apache.wink.common.model.synd.SyndText;
import org.apache.wink.common.utils.ProviderUtils;
import org.apache.wink.example.history.legacy.DefectBean;
import org.apache.wink.server.utils.LinkBuilders;
import org.apache.wink.server.utils.SingleLinkBuilder;
import org.apache.wink.server.utils.SystemLinksBuilder;

/**
 * Defect asset for producing and consuming a defect as xml or SyndEntry
 */
@Asset
public class DefectAsset {

    private DefectBean defect;
    private boolean    child;
    private boolean    history;
    private boolean    editable;

    public DefectAsset() {
        this(null);
    }

    public DefectAsset(DefectBean defect) {
        this(defect, false);
    }

    public DefectAsset(DefectBean defect, boolean child) {
        this(defect, child, false);
    }

    public DefectAsset(DefectBean defect, boolean child, boolean history) {
        this.defect = defect;
        this.child = child;
        this.history = history;
        this.editable = true;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Called for producing the entity of a response with an application/xml
     * content
     */
    @Produces( {MediaType.APPLICATION_XML})
    public DefectBean getDefect() {
        return defect;
    }

    /**
     * Called for producing the entity of a response that supports SyndEntry is
     * made (such as application/atom+xml or application/json)
     * 
     * @throws IOException
     */
    @Produces( {MediaType.WILDCARD, MediaType.APPLICATION_JSON})
    public SyndEntry getSyndEntry(@Context Providers providers,
                                  @Context UriInfo uriInfo,
                                  @Context LinkBuilders linkBuilders) throws IOException {
        SyndEntry entry = new SyndEntry();
        String id = defect.getId();
        entry.setId("urn:com:hp:qadefects:defect:" + id);
        entry.setTitle(new SyndText(defect.getName()));
        entry.setSummary(new SyndText(defect.getDescription()));
        entry.addAuthor(new SyndPerson(defect.getAuthor()));
        entry.addCategory(new SyndCategory("urn:com:hp:qadefects:categories:severity", defect
            .getSeverity(), null));
        entry.addCategory(new SyndCategory("urn:com:hp:qadefects:categories:status", defect
            .getStatus(), null));
        if (defect.getCreated() != null) {
            entry.setPublished(new Date(defect.getCreated().getTime()));
        }

        // serialize the defect xml
//        String contentString =
//            ProviderUtils.writeToString(providers, defect, MediaType.APPLICATION_XML_TYPE);
        entry.setContent(new SyndContent(defect, MediaType.APPLICATION_XML));

        // set base uri if this is a standalone entry
        if (!child) {
            entry.setBase(uriInfo.getAbsolutePath().toString());
        }

        // generate the edit link
        SingleLinkBuilder singleLinkBuilder = linkBuilders.createSingleLinkBuilder();
        if (editable) {
            singleLinkBuilder.subResource(DefectsResource.DEFECT_URL)
                .pathParam(DefectsResource.DEFECT_VAR, id).rel(AtomConstants.ATOM_REL_EDIT)
                .build(entry.getLinks());
        }

        // if this entry is not part of a history response, then generate the
        // history link
        if (!history) {
            singleLinkBuilder.subResource(DefectsResource.DEFECT_HISTORY_URL)
                .pathParam(DefectsResource.DEFECT_VAR, id).rel(AtomConstants.ATOM_REL_HISTORY)
                .type(MediaType.APPLICATION_ATOM_XML_TYPE).build(entry.getLinks());
        }

        // generate system links to self and alternate.
        // for the system links we add the revision of the defect to the defect
        // id
        String idAndRev =
            String.format("%s;%s=%s", id, DefectsResource.REVISION, defect.getRevision());
        linkBuilders.createSystemLinksBuilder().subResource(DefectsResource.DEFECT_URL)
            .pathParam(DefectsResource.DEFECT_VAR, idAndRev)
            .types(SystemLinksBuilder.LinkType.SELF, SystemLinksBuilder.LinkType.ALTERNATE)
            .build(entry.getLinks());

        return entry;
    }

    /**
     * Called for consuming the entity of a request with an application/xml
     * content
     */
    @Consumes(MediaType.APPLICATION_XML)
    public void setDefect(DefectBean defect) {
        this.defect = defect;
    }

    /**
     * Called for consuming the entity of a request that supports SyndEntry is
     * made (such as application/atom+xml)
     * 
     * @throws IOException
     */
    @Consumes
    public void setSyndEntry(SyndEntry entry, @Context Providers providers) throws IOException {
        defect = null;
        SyndContent content = entry.getContent();
        if (content == null) {
            return;
        }
        String value = content.getValue();
        String type = content.getType();
        if (value == null || !MediaType.APPLICATION_XML.equalsIgnoreCase(type)) {
            return;
        }
        // deserialize the defect xml
        defect =
            ProviderUtils.readFromString(providers,
                                         value,
                                         DefectBean.class,
                                         MediaType.APPLICATION_XML_TYPE);
    }
}
