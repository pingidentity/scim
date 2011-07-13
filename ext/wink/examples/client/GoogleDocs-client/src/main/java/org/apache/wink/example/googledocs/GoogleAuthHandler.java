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
/*******************************************************************************
 *******************************************************************************/
package org.apache.wink.example.googledocs;

import java.io.BufferedReader;
import java.io.StringReader;

import javax.ws.rs.core.MediaType;

import org.apache.wink.client.ClientRequest;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.RestClient;
import org.apache.wink.client.handlers.ClientHandler;
import org.apache.wink.client.handlers.HandlerContext;
import org.apache.wink.common.internal.MultivaluedMapImpl;

/**
 * Fetches a Google Authentication token for Google Docs
 */
public class GoogleAuthHandler implements ClientHandler {

    private final String email;
    private final String password;
    private String       authHeader = null;

    public GoogleAuthHandler(String email, String password) {
        super();
        this.email = email;
        this.password = password;
    }

    public ClientResponse handle(ClientRequest request, HandlerContext context) throws Exception {

        if (authHeader == null) {

            // invoke a call to google authentication service
            RestClient client = new RestClient();
            org.apache.wink.client.Resource authResource =
                client.resource("https://www.google.com/accounts/ClientLogin");
            MultivaluedMapImpl<String, String> params = new MultivaluedMapImpl<String, String>();
            params.putSingle("accountType", "HOSTED_OR_GOOGLE");
            params.putSingle("Email", email);
            params.putSingle("Passwd", password);
            params.putSingle("service", "writely");
            params.putSingle("source", "my-test-application");

            // send auth request to google
            // in real application the flow should be checking for error at this
            // point.
            // may be the handler can handle this error and create
            // authentication
            // but in the example the error will be just thrown to the user with
            // the exception
            String post =
                authResource.contentType(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                    .post(String.class, params);

            BufferedReader reader = new BufferedReader(new StringReader(post));
            String auth = null;
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.startsWith("Auth")) {
                    auth = line.split("=")[1];
                    break;
                }
            }
            authHeader = "GoogleLogin auth=" + auth;
        }
        request.getHeaders().add("Authorization", authHeader);

        // continue the chain
        // in real application it's useful to check the response if it returned
        // 401
        // and then to re-request auth token, but for example purpose
        // it's simplier just to return the error to the user.
        return context.doChain(request);
    }

}
