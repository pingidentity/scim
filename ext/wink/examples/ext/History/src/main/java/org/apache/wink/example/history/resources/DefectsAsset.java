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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.annotations.Asset;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndFeed;
import org.apache.wink.common.model.synd.SyndPerson;
import org.apache.wink.common.model.synd.SyndText;
import org.apache.wink.example.history.legacy.DefectBean;
import org.apache.wink.server.utils.LinkBuilders;
import org.apache.wink.server.utils.SystemLinksBuilder;

/**
 * Defects asset for producing a list of defects as SyndFeed
 */
@Asset
public class DefectsAsset {

    private List<DefectBean> defects;
    private boolean          history;

    /**
     * Constructor. Metadata and data objects are empty.
     */
    public DefectsAsset() {
        this.defects = new LinkedList<DefectBean>();
    }

    /**
     * Constructor. Creates resource metadata from defect bean collection. This
     * constructor will be used when we want HTML representation for the
     * resource.
     * 
     * @param collection defect bean collection
     */
    public DefectsAsset(Collection<DefectBean> defects) {
        this(defects, false);
    }

    public DefectsAsset(Collection<DefectBean> defects, boolean history) {
        this();
        this.defects.addAll(defects);
        this.history = history;
    }

    public List<DefectBean> getDefects() {
        return defects;
    }

    /**
     * Called for producing the entity of a response that supports SyndFeed is
     * made (such as application/atom+xml or application/json)
     * @throws IOException 
     */
    @Produces
    public SyndFeed getSyndFeed(@Context Providers providers,
                                @Context LinkBuilders linkBuilders,
                                @Context UriInfo uriInfo) throws IOException {
        SyndFeed feed = new SyndFeed();
        feed.setId("urn:com:hp:qadefects:defects");
        feed.setTitle(new SyndText("Defects"));
        feed.addAuthor(new SyndPerson("admin"));
        feed.setUpdated(new Date());

        feed.setBase(uriInfo.getAbsolutePath().toString());

        boolean editable = true;

        SystemLinksBuilder systemLinksBuilder = linkBuilders.createSystemLinksBuilder();

        // generate history links
        if (history) {
            // all defects in the collection are the history of the same defect
            // and they all have the
            // same defect id, so we can use the id of the first one
            systemLinksBuilder.subResource(DefectsResource.DEFECT_HISTORY_URL)
                .pathParam(DefectsResource.DEFECT_VAR, defects.get(0).getId()).build(feed
                    .getLinks());
            if (!defects.isEmpty()) {
                // if this is a history of a defect, then the last defect in the
                // list is the latest state of the defect
                editable = !(defects.get(defects.size() - 1).isDeleted());
            }
        } else {
            // generate system links
            systemLinksBuilder.build(feed.getLinks());
        }

        // set the entries
        for (DefectBean defect : defects) {
            DefectAsset defectAsset = new DefectAsset(defect, true, history);
            // set whether to generate the edit link or not
            defectAsset.setEditable(editable);
            SyndEntry entry = defectAsset.getSyndEntry(providers, uriInfo, linkBuilders);
            // we don't want content for entries
            entry.setContent(null);
            feed.addEntry(entry);
        }

        return feed;
    }
}
