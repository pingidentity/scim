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
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

import org.apache.wink.common.annotations.Asset;
import org.apache.wink.common.model.synd.SyndContent;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndPerson;
import org.apache.wink.common.model.synd.SyndText;
import org.apache.wink.common.utils.ProviderUtils;
import org.apache.wink.example.qadefect.legacy.TestBean;
import org.apache.wink.server.utils.LinkBuilders;

/**
 * Test resource
 */
@Asset
public class TestAsset {

    private TestBean test;
    private boolean  child;

    /**
     * Constructor. Metadata and data objects are empty.
     */
    public TestAsset() {
        this(null);
    }

    public TestAsset(TestBean test) {
        this(test, false);
    }

    /**
     * Constructor. Creates resource data and metadata from test bean.
     * 
     * @param bean defect bean
     */
    public TestAsset(TestBean test, boolean child) {
        this.test = test;
        this.child = child;
    }

    @Produces
    public TestBean getTest() {
        return test;
    }

    @Consumes
    public void setTest(TestBean test) {
        this.test = test;
    }

    @Produces( {MediaType.WILDCARD, MediaType.APPLICATION_JSON})
    public SyndEntry getSyndEntry(@Context Providers providers,
                                  @Context UriInfo uriInfo,
                                  @Context LinkBuilders linkProcessor) throws IOException {
        SyndEntry entry = new SyndEntry();
        entry.setId("urn:com:hp:qadefects:test:" + test.getId());
        entry.setTitle(new SyndText(test.getName()));
        entry.setSummary(new SyndText(test.getDescription()));
        entry.addAuthor(new SyndPerson(test.getAuthor()));
        if (test.getCreated() != null) {
            entry.setPublished(new Date(test.getCreated().getTime()));
        }

        // serialize the defect xml
        String contentString =
            ProviderUtils.writeToString(providers, test, MediaType.APPLICATION_XML_TYPE);
        entry.setContent(new SyndContent(contentString, MediaType.APPLICATION_XML, false));

        if (!child) {
            entry.setBase(uriInfo.getAbsolutePath().toString());
        }

        linkProcessor.createSystemLinksBuilder().resource(TestsResource.class).subResource(test
            .getId()).build(entry.getLinks());
        return entry;
    }

    @Consumes
    public void setSyndEntry(SyndEntry entry, @Context Providers providers) throws IOException {
        test = null;
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
        test =
            ProviderUtils.readFromString(providers,
                                         value,
                                         TestBean.class,
                                         MediaType.APPLICATION_XML_TYPE);
    }

}
