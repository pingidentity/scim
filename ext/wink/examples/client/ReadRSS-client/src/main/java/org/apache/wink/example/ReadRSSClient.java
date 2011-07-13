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

import javax.ws.rs.core.MediaType;

import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.common.model.rss.RssFeed;
import org.apache.wink.common.model.rss.RssItem;

/**
 * This is an example of consuming an RSS Feed
 */
public class ReadRSSClient {

    public static void main(String[] args) {
        try {
            // create the rest client instance
            RestClient restClient = new RestClient();

            // create the resource instance to interact with
            String rss_url = "http://www.rssboard.org/files/rss-2.0-sample.xml";
            Resource feedResource = restClient.resource(rss_url);

            // perform a GET on the resource. The resource will be returned as
            // an Rss object
            RssFeed rss = feedResource.accept(MediaType.APPLICATION_XML_TYPE).get(RssFeed.class);

            System.out.println("RSS Title = " + rss.getChannel().getTitle());
            System.out.println("RSS Descritpion = " + rss.getChannel().getDescription());
            System.out.println("RSS Link = " + rss.getChannel().getLink());
            int itemCount = 0;
            for (RssItem item : rss.getChannel().getItems()) {
                System.out.println("Item " + ++itemCount + ":");
                System.out.println("\tTitle = " + item.getTitle());
                System.out.println("\tDescription = " + item.getDescription());
                System.out.println("\tLink = " + item.getLink());
            }

            //System.out.println("\nComplete XML contents:");
            //JAXB.marshal(rss, System.out);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
