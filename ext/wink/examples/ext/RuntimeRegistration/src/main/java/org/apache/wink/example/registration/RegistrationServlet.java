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
package org.apache.wink.example.registration;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.wink.server.utils.RegistrationUtils;

/**
 * The servlet that is responsible to register new classes. It expects to
 * receive a parameter "class" with a class name to register. The class must be
 * already located on the classpath.
 */
public class RegistrationServlet extends HttpServlet {

    private static final long serialVersionUID = -7599938005716546425L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
        IOException {
        String parameter = req.getParameter("class");
        if (parameter != null && parameter.length() > 0) {
            try {
                Class<?> cls = Class.forName(parameter);
                RegistrationUtils.registerClasses(getServletContext(), cls);
                resp.getWriter().println(String.format("Class %s registered.", parameter));
            } catch (ClassNotFoundException e) {
                resp.getWriter().println(String.format("Class %s not found.", parameter));
            }
        } else {
            resp.getWriter().println("Missing or empty parameter 'class'.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doGet(req, resp);
    }
}
