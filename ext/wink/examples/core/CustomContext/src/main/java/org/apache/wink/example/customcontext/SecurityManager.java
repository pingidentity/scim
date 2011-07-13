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
package org.apache.wink.example.customcontext;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("serial")
public class SecurityManager {

    public static final String                    READ_ONLY        = "READ_ONLY";
    public static final String                    READ_WRITE       = "READ_WRITE";
    private static final long                     serialVersionUID = 1L;

    public static Map<String, CustomerPermission> customers        =
                                                                       new HashMap<String, CustomerPermission>() {
                                                                           {
                                                                               put("admin",
                                                                                   new CustomerPermission(
                                                                                                          "admin",
                                                                                                          READ_WRITE));
                                                                               put("john",
                                                                                   new CustomerPermission(
                                                                                                          "john",
                                                                                                          READ_ONLY));
                                                                           }
                                                                       };

    public static class CustomerPermission {
        public String customerId;
        public String permission;

        public CustomerPermission(String customerId, String permission) {
            this.customerId = customerId;
            this.permission = permission;
        }

        public boolean isWriteAllowed() {
            return permission.equalsIgnoreCase(READ_WRITE);
        }

    }

    public CustomerPermission getPermission(String customerId) {
        return customers.get(customerId);
    }
}
