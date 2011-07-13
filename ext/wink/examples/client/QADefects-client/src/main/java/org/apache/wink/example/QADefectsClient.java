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
package org.apache.wink.example;

import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.common.internal.utils.MediaTypeUtils;
import org.apache.wink.common.model.app.AppCollection;
import org.apache.wink.common.model.app.AppService;
import org.apache.wink.common.model.app.AppWorkspace;
import org.apache.wink.common.model.atom.AtomEntry;
import org.apache.wink.common.model.atom.AtomFeed;

/**
 * This is an example of consuming a service exposed in Atom
 */
public class QADefectsClient {

    private static final String COLLECTION_TITLE = "Defects";
    private static final String WORKSPACE_TITLE  = "QA Defects";
    static String               defectsFeed      = "http://localhost:8080/QADefect/rest";
    static String               testFeed         =
                                                     "http://localhost:8080/QADefect/rest/defects/13/tests";

    public static void main(String[] args) {
        try {

            // First Step: Perform Service Introspection.
            // This step is not always required (only if Collection URL is not
            // known in advance)

            // Create Rest Client

            RestClient restClient = new RestClient();

            // Create new Resource on given URL
            Resource resource = restClient.resource(defectsFeed);

            AppService service =
                resource.accept(MediaTypeUtils.ATOM_SERVICE_DOCUMENT).get(AppService.class);

            // Find workspace by it's title
            AppWorkspace workspace = service.getWorkspace(WORKSPACE_TITLE);

            // Find collection by title
            AppCollection collection = workspace.getCollection(COLLECTION_TITLE);
            String href = collection.getHref();

            // Get collection of defects
            Resource feedResource = restClient.resource(href);
            AtomFeed feed = feedResource.accept(MediaType.APPLICATION_ATOM_XML).get(AtomFeed.class);

            // Browse through collection of defects.
            listDefects(feed);
            listTests(restClient, testFeed);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void listDefects(AtomFeed feed) {
        List<AtomEntry> entries = feed.getEntries();
        for (AtomEntry atomEntry : entries) {
            System.out.println("ID> " + atomEntry.getId()
                + " Title> "
                + atomEntry.getTitle().getValue());
        }
    }

    private static void listTests(RestClient restClient, String url) {
        Resource feedResource = restClient.resource(url);
        AtomFeed feed = feedResource.accept(MediaType.APPLICATION_ATOM_XML).get(AtomFeed.class);
        List<AtomEntry> entries = feed.getEntries();
        for (AtomEntry atomEntry : entries) {
            System.out.println("ID> " + atomEntry.getId()
                + " Title> "
                + atomEntry.getTitle().getValue());
            System.out.println("Content> " + atomEntry.getContent().getValue());
        }
    }

}
