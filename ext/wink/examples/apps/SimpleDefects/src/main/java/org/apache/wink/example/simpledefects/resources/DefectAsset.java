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
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.annotations.Asset;
import org.apache.wink.common.model.synd.SyndCategory;
import org.apache.wink.common.model.synd.SyndContent;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndPerson;
import org.apache.wink.common.model.synd.SyndText;
import org.apache.wink.common.utils.ProviderUtils;
import org.apache.wink.example.simpledefects.legacy.DefectBean;
import org.apache.wink.server.utils.LinkBuilders;

/**
 * Defect resource (based on document resource)
 */
@Asset
public class DefectAsset {

    private DefectBean defect;
    private boolean    child;

    public DefectAsset() {
        this(null);
    }

    public DefectAsset(DefectBean defect) {
        this(defect, false);
    }

    public DefectAsset(DefectBean defect, boolean child) {
        this.defect = defect;
        this.child = child;
    }

    @Produces( {MediaType.APPLICATION_XML})
    public DefectBean getDefect() {
        return defect;
    }

    @Produces( {MediaType.WILDCARD, MediaType.APPLICATION_JSON})
    public SyndEntry getSyndEntry(@Context Providers providers,
                                  @Context UriInfo uriInfo,
                                  @Context LinkBuilders linkBuilders) throws IOException {
        SyndEntry entry = new SyndEntry();
        entry.setId("urn:com:hp:qadefects:defect:" + defect.getId());
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
        String contentString =
            ProviderUtils.writeToString(providers, defect, MediaType.APPLICATION_XML_TYPE);
        entry.setContent(new SyndContent(contentString, MediaType.APPLICATION_XML, false));

        // set base uri if this is a standalone entry
        if (!child) {
            entry.setBase(uriInfo.getAbsolutePath().toString());
        }

        // generate system links
        linkBuilders.createSystemLinksBuilder().subResource(defect.getId()).build(entry.getLinks());
        return entry;
    }

    @Consumes(MediaType.APPLICATION_XML)
    public void setDefect(DefectBean defect) {
        this.defect = defect;
    }

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
