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
package org.apache.wink.example.googledocs;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CLIHelper {

    private static final char PROXY_HOST_ORT  = 'H';
    private static final char PROXY_PORT_OPT  = 'P';
    private static final char DELETE_OPT      = 'd';
    private static final char LIST_OPT        = 'l';
    private static final char UPLOAD_FILE_OPT = 'f';
    private static final char PASSWORD_OPT    = 'p';
    private static final char USER_OPT        = 'u';

    private final Options     options         = new Options();
    private CommandLine       commandLine     = null;
    private boolean           hasProxy        = false;

    @SuppressWarnings("static-access")
    public CLIHelper() {

        Option userOption =
            OptionBuilder.withArgName("user").hasArg()
                .withDescription("Full username. Example: user@gmail.com").isRequired(true)
                .withLongOpt("user").create(USER_OPT);
        Option passwordOption =
            OptionBuilder.withArgName("password").isRequired(true).hasArg()
                .withDescription("Password").withLongOpt("password").create(PASSWORD_OPT);
        Option uploadFileOption =
            OptionBuilder.withArgName("file").isRequired(false).hasArg()
                .withDescription("Path to a file to upload").withLongOpt("upload")
                .create(UPLOAD_FILE_OPT);
        Option listFilesOption =
            OptionBuilder.hasArg(false).withDescription("List files").withLongOpt("list")
                .create(LIST_OPT);
        Option deleteOption =
            OptionBuilder.withArgName("document id").hasArg(true)
                .withDescription("Delete document. Use --list to get a document id.")
                .withLongOpt("delete").create(DELETE_OPT);
        Option proxyHostOption =
            OptionBuilder.isRequired(false).withArgName("host").hasArg(true)
                .withDescription("Proxy host").withLongOpt("proxy").create(PROXY_HOST_ORT);
        Option proxyPortOption =
            OptionBuilder.isRequired(false).withArgName("port").hasArg(true)
                .withDescription("Proxy port").withLongOpt("port").create(PROXY_PORT_OPT);

        OptionGroup group = new OptionGroup();
        group.setRequired(true);
        group.addOption(uploadFileOption);
        group.addOption(listFilesOption);
        group.addOption(deleteOption);

        options.addOptionGroup(group);
        options.addOption(proxyHostOption);
        options.addOption(proxyPortOption);
        options.addOption(passwordOption);
        options.addOption(userOption);
    }

    public void printHelp() {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("GoogleDocsClient", options, true);
    }

    public void init(String[] args) throws ParseException {
        GnuParser parser = new GnuParser();
        commandLine = parser.parse(options, args);

        boolean hasHostOption = commandLine.hasOption(PROXY_HOST_ORT);
        boolean hasPortOption = commandLine.hasOption(PROXY_PORT_OPT);
        if (hasHostOption && !hasPortOption) {
            throw new ParseException("Proxy host was specified, but proxy port was not.");
        }
        if (!hasHostOption && hasPortOption) {
            throw new ParseException("Proxy port was specified, but proxy host was not.");
        }
        if (hasHostOption && hasPortOption) {
            hasProxy = true;
        }

    }

    public boolean hasProxy() {
        return hasProxy;
    }

    public boolean isList() {
        return commandLine.hasOption(LIST_OPT);
    }

    public boolean isUpload() {
        return commandLine.hasOption(UPLOAD_FILE_OPT);
    }

    public boolean isDelete() {
        return commandLine.hasOption(DELETE_OPT);
    }

    public String getUploadFilename() {
        return commandLine.getOptionValue(UPLOAD_FILE_OPT);
    }

    public String getDeleteId() {
        return commandLine.getOptionValue(DELETE_OPT);
    }

    public String getProxyHost() {
        return commandLine.getOptionValue(PROXY_HOST_ORT);
    }

    public String getProxyPort() {
        return commandLine.getOptionValue(PROXY_PORT_OPT);
    }

    public String getEmail() {
        return commandLine.getOptionValue(USER_OPT);
    }

    public String getPassword() {
        return commandLine.getOptionValue(PASSWORD_OPT);
    }

}
