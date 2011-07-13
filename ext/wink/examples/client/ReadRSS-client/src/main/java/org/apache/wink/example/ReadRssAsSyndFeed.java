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

import java.io.IOException;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.common.internal.model.ModelUtils;
import org.apache.wink.common.model.atom.AtomFeed;
import org.apache.wink.common.model.atom.ObjectFactory;
import org.apache.wink.common.model.rss.RssFeed;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndFeed;

/**
 * This is an example of reading an RSS Feed using SyndFeed APIs. (Mapping RSS
 * into Syndication Object Model)
 */
public class ReadRssAsSyndFeed {

    public static void main(String[] args) {
        try {
            // create the rest client instance
            RestClient restClient = new RestClient();

            // create the resource instance to interact with
            String rss_url = "http://www.rssboard.org/files/rss-2.0-sample.xml";
            Resource feedResource = restClient.resource(rss_url);

            // perform a GET on the resource. The resource will be returned as an Rss object
            RssFeed rssFeed = feedResource.accept(MediaType.APPLICATION_XML).get(RssFeed.class);

            // Map RSS into SyndFeed
            SyndFeed syndFeed = new SyndFeed();
            syndFeed = rssFeed.toSynd(syndFeed);

            // Now access RSS using SyndFeed APIs
            if (syndFeed.getTitle() != null) {
                System.out.println("Title = " + syndFeed.getTitle().getValue());
            }
            if (syndFeed.getSubtitle() != null) {
                System.out.println("Descritpion = " + syndFeed.getSubtitle().getValue());
            }
            if (syndFeed.getLink("alternate") != null) {
                System.out.println("Link = " + syndFeed.getLink("alternate").getHref());
            }
            int itemCount = 0;
            for (SyndEntry syndEntry : syndFeed.getEntries()) {
                System.out.println("Item " + ++itemCount + ":");
                if (syndEntry.getTitle() != null) {
                    System.out.println("\tTitle = " + syndEntry.getTitle().getValue());
                }
                if (syndEntry.getSummary() != null) {
                    System.out.println("\tDescription = " + syndEntry.getSummary().getValue());
                }
                if (syndEntry.getLink("alternate") != null) {
                    System.out.println("\tLink = " + syndEntry.getLink("alternate").getHref());
                }
            }

            System.out.println("\nComplete XML contents (mapped into SyndFeed / Atom format):");
            Marshaller m = AtomFeed.getMarshaller();
            JAXBElement<AtomFeed> element =
                (new ObjectFactory()).createFeed(new AtomFeed(syndFeed));
            try {
                ModelUtils.marshal(m, element, System.out);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
