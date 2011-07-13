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
package org.apache.wink.example.scope;

import java.util.ArrayList;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.wink.common.model.synd.SyndLink;
import org.apache.wink.server.utils.LinkBuilders;

/**
 * Displays a counter.
 */
public abstract class BaseCounter {

    private int counter = 0;

    public abstract String getHeader();

    public abstract Class<?> getLinkClass();

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getCounter(@Context LinkBuilders linkBuilders) {
        // StringBuffer is used to allow non-synchronized context
        StringBuffer buf = new StringBuffer();
        buf.append("");
        buf.append("<html><body><h2>" + getHeader() + "</h2>");
        buf.append(String.format("<p>The counter result is %d.</p>", ++counter));
        buf.append("<p>Try to refreshing the page to see if the counter will change.</p>");

        // create "see also" link
        ArrayList<SyndLink> list = new ArrayList<SyndLink>();
        linkBuilders.createSingleLinkBuilder().resource(getLinkClass()).build(list);
        buf.append("<p>See also <a href=\"" + list.get(0).getHref()
            + "\">Prototype Counter</a></p>");
        buf.append("</body></html>");
        return buf.toString();
    }
}
