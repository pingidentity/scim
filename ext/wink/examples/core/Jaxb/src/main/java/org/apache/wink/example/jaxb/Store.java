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

package org.apache.wink.example.jaxb;

import java.util.HashMap;
import java.util.Map;

public class Store {

    private Map<String, Person>  persons   = new HashMap<String, Person>();
    private Map<String, Address> addresses = new HashMap<String, Address>();
    private Map<String, Phone>   phones    = new HashMap<String, Phone>();

    public Store() {
        persons.put("1", new Person("John", "Smith", "john.smith@email.com"));
        addresses.put("1", new Address("New York", "5th", "64"));
        phones.put("1", new Phone(768, 5555678));

        persons.put("2", new Person("Jane", "Smith", "jane.smith@email.com"));
        addresses.put("2", new Address("New Orleans", "Burbon", "70"));
        phones.put("2", new Phone(513, 5554321));
    }

    public Person getPerson(String id) {
        return persons.get(id);
    }

    public void putPerson(String id, Person p) {
        persons.put(id, p);
    }

    public Address getAddress(String id) {
        return addresses.get(id);
    }

    public void putAddress(String id, Address a) {
        addresses.put(id, a);
    }

    public Phone getPhone(String id) {
        return phones.get(id);
    }

    public void putPhone(String id, Phone p) {
        phones.put(id, p);
    }

}
