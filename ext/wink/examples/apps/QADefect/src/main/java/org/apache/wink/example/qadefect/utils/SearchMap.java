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

package org.apache.wink.example.qadefect.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Extends the HashMap and used by the search functionality. The idea is to keep
 * meaningful search parameters in the map, but to ignore all the rest. Only
 * parameters with not null key and value are meaningful.
 */
public class SearchMap extends HashMap<String, String> {

    private static final long serialVersionUID = -6361482020746418444L;

    @Override
    public String put(String key, String value) {
        if (key != null && value != null) {
            return super.put(key, value.toLowerCase());
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        throw new UnsupportedOperationException();
    }

}
