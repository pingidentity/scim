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
import org.apache.wink.example.simpledefects.legacy.DefectBean;
import org.apache.wink.server.utils.LinkBuilders;

/**
 * Defects resource (based on collection resource)
 */
@Asset
public class DefectsAsset {

    private List<DefectBean> defects;

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
        this();
        this.defects.addAll(defects);
    }

    public List<DefectBean> getDefects() {
        return defects;
    }

    @Produces
    public SyndFeed getSyndFeed(@Context Providers providers,
                                @Context LinkBuilders linkBuilders,
                                @Context UriInfo uriInfo) throws IOException {
        SyndFeed synd = new SyndFeed();
        synd.setId("urn:com:hp:qadefects:defects");
        synd.setTitle(new SyndText("Defects"));
        synd.addAuthor(new SyndPerson("admin"));
        synd.setUpdated(new Date());

        // set the entries
        for (DefectBean defect : getDefects()) {
            DefectAsset defectAsset = new DefectAsset(defect, true);
            SyndEntry entry = defectAsset.getSyndEntry(providers, uriInfo, linkBuilders);
            synd.addEntry(entry);
        }

        synd.setBase(uriInfo.getAbsolutePath().toString());
        linkBuilders.createSystemLinksBuilder().build(synd.getLinks());
        return synd;
    }

}
