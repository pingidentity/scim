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

package org.apache.wink.example.multipart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.wink.common.internal.utils.MediaTypeUtils;
import org.apache.wink.common.model.multipart.BufferedInMultiPart;
import org.apache.wink.common.model.multipart.BufferedOutMultiPart;
import org.apache.wink.common.model.multipart.InMultiPart;
import org.apache.wink.common.model.multipart.InPart;
import org.apache.wink.common.model.multipart.OutPart;

/**
 * A resource that demonstrate the usage of MultiPart.
 */

@Path("/MP")
public class MultiPartResource {

    // user storage 
    private static ArrayList<User> usersList = new ArrayList<User>();
    static {
        User u = new User("Moshe", "tornado", 1, "moshe@gmail.com");
        usersList.add(u);
        u = new User("Jhon", "Dow", 2, "Jhon@gmail.com");
        usersList.add(u);
    }
    

    /**
     * This method return all the users in a multipart/mixed representation
     * it demonstrat the usage of BufferedOutMultiPart and OutPart. 
     * @return
     */
    @Path("users")
    @GET
    @Produces( {"multipart/mixed"})
    public BufferedOutMultiPart getUsers() {
        BufferedOutMultiPart mp = new BufferedOutMultiPart();

        for (User user : usersList) {
            OutPart op = new OutPart();
            op.setContentType(MediaType.APPLICATION_XML);
            op.setBody(user);
            mp.addPart(op);
        }
        return mp;
    }
    
    /**
     *  This method add users and returns the updated user list in multipart/mixed representation
     *  It demonstrate the usage of BufferedInMultiPart and InPart
     */

    @Path("users")
    @POST
    @Produces( {"multipart/mixed"})
    @Consumes( {"multipart/mixed"})
    public BufferedOutMultiPart addUsers(BufferedInMultiPart inMP) throws IOException {
        List<InPart> parts = inMP.getParts();
        for (InPart p : parts) {
            User u = p.getBody(User.class, null);
            usersList.add(u);

        }
        return getUsers();
    }
    
    /**
     * This method come to demonstrate the way to work with huge Multipart messages. 
     * it uses the InMultiPart which does not buffer the content of the message.   
     * @param inMP
     * @return
     * @throws IOException
     */

    @Path("files")
    @POST
    @Produces( MediaType.TEXT_PLAIN)
    @Consumes( MediaTypeUtils.MULTIPART_FORM_DATA)
    public String uploadFiles(InMultiPart inMP) throws IOException {
        StringBuilder sb = new StringBuilder();
        int fileID = 0;

        while (inMP.hasNext()) {
            File f = File.createTempFile("apache-wink-Multipart-example" + fileID, ".tmp");
            FileOutputStream fos = new FileOutputStream(f);
            InPart part = inMP.next();
            MultivaluedMap<String, String> heades = part.getHeaders();
            String CDHeader = heades.getFirst("Content-Disposition");
            InputStream is = part.getInputStream();
            int size = 0;
            int i = 0;
            byte[] ba = new byte[8196];
            while ((i = is.read(ba)) != -1) {
                fos.write(ba, 0, i);
                size += i;
            }
            fos.close();
            
            String fileName ="";
            Pattern p =  Pattern.compile("filename=\".*\""); 
            Matcher m = p.matcher(CDHeader);            
            if (m.find()){
                fileName = m.group();
            }
            
            //String filename = CDHeader. 
            sb.append("uploaded a file, " + fileName + ", size = " + size + " bytes\n");
        }
        return sb.toString();
    }

}
