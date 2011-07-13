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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.cli.ParseException;
import org.apache.wink.client.ClientConfig;
import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.ClientWebException;
import org.apache.wink.client.Resource;
import org.apache.wink.client.RestClient;
import org.apache.wink.common.model.synd.SyndEntry;
import org.apache.wink.common.model.synd.SyndFeed;
import org.apache.wink.common.model.synd.SyndLink;
import org.apache.wink.common.model.synd.SyndPerson;

public class GoogleDocsClient {

    private final static String lineSeparator = System.getProperty("line.separator");
    private final String        URL           =
                                                  "http://docs.google.com/feeds/documents/private/full/";

    private RestClient          restClient;
    private CLIHelper           cliHelper;

    /**
     * main
     */
    public static void main(String[] args) {

        CLIHelper cliHelper = new CLIHelper();

        if (args.length < 1) {
            cliHelper.printHelp();
        } else {
            try {
                cliHelper.init(args);
            } catch (ParseException e) {
                System.out.println(e.getMessage());
                cliHelper.printHelp();
            }
            new GoogleDocsClient(cliHelper).run();
        }
    }

    private GoogleDocsClient(CLIHelper cliHelper) {
        ClientConfig config = new ClientConfig();

        if (cliHelper.hasProxy()) {
            config.proxyHost(cliHelper.getProxyHost());
            config.proxyPort(Integer.valueOf(cliHelper.getProxyPort()));
        }

        // add google authentication handler
        config.handlers(new GoogleAuthHandler(cliHelper.getEmail(), cliHelper.getPassword()));
        restClient = new RestClient(config);
        this.cliHelper = cliHelper;
    }

    private void run() {
        try {
            if (cliHelper.isList()) {
                String listFiles = listFiles();
                System.out.println(listFiles);
            } else if (cliHelper.isUpload()) {
                String filename = cliHelper.getUploadFilename();
                String uploadFile = uploadFile(filename);
                System.out.println("Uploaded: " + uploadFile);
            } else if (cliHelper.isDelete()) {
                String id = cliHelper.getDeleteId();
                delete(id);
                System.out.println(id + " deleted.");
            } else {
                System.out.println("Nothing to do.");
            }
        } catch (ClientWebException e) {
            // error during client invocation
            // usually it happens when status code starting with 400 or 500 is
            // returned
            ClientResponse response = e.getResponse();
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Message: " + response.getMessage());
            System.out.println("Body: " + response.getEntity(String.class));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * returns list of Google document files, the list is returned as String
     */
    public String listFiles() throws ClientWebException {
        // create a resource
        Resource listOfDocumentsResource = restClient.resource(URL);

        // invoke GET on the resource and parse the result as SyndFeed
        SyndFeed list = listOfDocumentsResource.get(SyndFeed.class);

        // convert SyndFeed to String
        return feedToString(list);
    }

    /**
     * delete file
     * 
     * @param id - id as it appears in edit link
     */
    public void delete(String id) {
        Resource resource = restClient.resource(URL + id);
        ClientResponse response = resource.delete(ClientResponse.class);
        if (response.getStatusCode() != 200) {
            throw new ClientWebException(null, response);
        }
    }

    /**
     * Upload a file
     * 
     * @param filename - full filename to upload
     * @return location of the uploaded file
     * @throws FileNotFoundException - file was not found
     * @throws ClientWebException - error occurred during the upload
     */
    public String uploadFile(String filename) throws FileNotFoundException, ClientWebException {
        Resource listOfDocumentsResource = restClient.resource(URL);

        File file = new File(filename);
        ClientResponse clientResponse =
            listOfDocumentsResource.header("Slug", file.getName())
                .contentType(mapContentType(filename)).post(ClientResponse.class,
                                                            new FileInputStream(file));
        if (clientResponse.getStatusCode() == Status.CREATED.getStatusCode()) {
            return clientResponse.getHeaders().getFirst(HttpHeaders.LOCATION);
        }
        throw new ClientWebException(null, clientResponse);

    }

    private String mapContentType(String filename) {
        String upperFileName = filename.toUpperCase();
        if (upperFileName.endsWith("CSV")) {
            return "text/csv";
        }
        if (upperFileName.endsWith("TSV")) {
            return "text/tab-separated-values";
        }
        if (upperFileName.endsWith("TAB")) {
            return "text/tab-separated-values";
        }
        if (upperFileName.endsWith("HTML")) {
            return "text/html";
        }
        if (upperFileName.endsWith("HTM")) {
            return "text/html";
        }
        if (upperFileName.endsWith("DOC")) {
            return "application/msword";
        }
        if (upperFileName.endsWith("ODS")) {
            return "application/x-vnd.oasis.opendocument.spreadsheet";
        }
        if (upperFileName.endsWith("ODT")) {
            return "application/vnd.oasis.opendocument.text";
        }
        if (upperFileName.endsWith("RTF")) {
            return "application/rtf";
        }
        if (upperFileName.endsWith("SXW")) {
            return "application/vnd.sun.xml.writer";
        }
        if (upperFileName.endsWith("TXT")) {
            return "text/plain";
        }
        if (upperFileName.endsWith("XLS")) {
            return "application/vnd.ms-excel";
        }
        if (upperFileName.endsWith("PPT")) {
            return "application/vnd.ms-powerpoint";
        }
        if (upperFileName.endsWith("PPS")) {
            return "application/vnd.ms-powerpoint";
        }
        throw new RuntimeException("Unknown extension.");
    }

    /**
     * converts the SyndFeed to flat list to display it to user
     */
    private String feedToString(SyndFeed list) {
        StringBuilder buf = new StringBuilder();
        for (SyndEntry entry : list.getEntries()) {
            buf.append('"');
            buf.append(entry.getTitle().getValue());
            buf.append('"');
            List<SyndPerson> authors = entry.getAuthors();
            if (authors.size() > 0) {
                buf.append(" by ");
                buf.append(authors.get(0).getName());
            }
            buf.append(" published at ");
            buf.append(String.valueOf(entry.getPublished()));

            SyndLink edit = null;
            for (SyndLink link : entry.getLinks()) {
                if (link.getRel().equals("edit")) {
                    edit = link;
                    break;
                }
            }
            if (edit != null) {
                buf.append(" edit id: ");
                buf.append(edit.getHref().substring(URL.length()));
            }
            buf.append(lineSeparator);
        }
        return buf.toString();
    }

}
