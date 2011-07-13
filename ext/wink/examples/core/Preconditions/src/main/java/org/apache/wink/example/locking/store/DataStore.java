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

package org.apache.wink.example.locking.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.wink.example.locking.legacy.DefectBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a demo memory store of legacy beans. Singleton class.
 * <p/>
 * The class is thread-safe. All method see (or change) at the moment when they
 * are invoked, later changes don't affect the returned collections, values...
 */
public class DataStore {

    private static DataStore defectStore = createSingletonInstance();

    final static Logger      logger      = LoggerFactory.getLogger(DataStore.class);

    /**
     * Constructor of mock defect store. It is initialized with test data.
     * 
     * @return initialized instance
     */
    private static DataStore createSingletonInstance() {
        DataStore store = new DataStore();

        DefectBean defectBean1 =
            new DefectBean(
                           "1",
                           "Cannot perform payment",
                           "In Payment Services page, error message appears when trying to perform payment with valid credit card.",
                           new Date(50505050), "tester1", "1-critical", "New", "developer1");
        DefectBean defectBean2 =
            new DefectBean("2", "Cancel button is missing",
                           "In Payment Services Confirmation page, the Cancel button is missing.",
                           new Date(50505051), "tester2", "4-minor", "New", "developer2");
        DefectBean defectBean3 =
            new DefectBean(
                           "3",
                           "Login failure",
                           "When trying to login with the user 'Admin', I'm getting an error 'The user is locked'.",
                           new Date(50505052), "tester3", "1-critical", "Assigned", "developer3");

        DefectBean defectBean4 =
            new DefectBean(
                           "4",
                           "Missing expiration field",
                           "In Payment Services page, the expiration field of credit card is missing.",
                           new Date(50505052), "tester2", "2-high", "Assigned", "developer3");

        DefectBean defectBean5 =
            new DefectBean(
                           "5",
                           "Missing asterisk for mandatory fields",
                           "In Payment Services page, there is no indication for mandatory fields.",
                           new Date(50505052), "tester2", "4-minor", "Assigned", "developer3");

        DefectBean defectBean6 =
            new DefectBean(
                           "6",
                           "User is not locked",
                           "When trying to login with the same user three times with wrong password, the user doesn't get locked.",
                           new Date(50505052), "tester3", "2-high", "Fixed", "developer2");

        DefectBean defectBean7 =
            new DefectBean("7", "Logout button", "The pages are missing the button 'logout'.",
                           new Date(50505052), "tester1", "2-high", "New", "developer1");

        DefectBean defectBean8 =
            new DefectBean("8", "Error messages", "Error messages are not displayed in red color",
                           new Date(50505052), "tester1", "4-minor", "Fixed", "developer1");

        DefectBean defectBean9 =
            new DefectBean(
                           "9",
                           "Submit button is disabled in Edit Profile page",
                           "When trying to submit changes in Edit Profile page, the Submit button is disabled.",
                           new Date(50505052), "tester3", "2-high", "New", "developer1");

        DefectBean defectBean10 =
            new DefectBean("10", "Id field is disabled in Edit Profile page",
                           "Cannot change the Id in Edit Profile page, it is disabled.",
                           new Date(50505052), "tester1", "2-high", "New", "developer1");

        DefectBean defectBean11 =
            new DefectBean("11", "Cannot login to the system",
                           "Cannot login to the system with the user 'David'.", new Date(50505052),
                           "tester3", "1-critical", "Rejected", "developer1");

        DefectBean defectBean12 =
            new DefectBean("12", "Checkbox is disabled",
                           "Checkbox is disabled in Edit Profile page", new Date(50505052),
                           "tester3", "2-high", "Rejected", "developer1");

        DefectBean defectBean13 =
            new DefectBean("13", "Missing message text",
                           "Missing message text in Edit Profile page", new Date(50505052),
                           "tester3", "4-minor", "Rejected", "developer1");

        DefectBean defectBean14 =
            new DefectBean(
                           "14",
                           "Wrong translation for the word 'user'",
                           "When moving to French language the translation of 'user' word is incorrect",
                           new Date(50505052), "tester3", "2-high", "Rejected", "developer1");

        store.putDefect(defectBean1.getId(), defectBean1);
        store.putDefect(defectBean2.getId(), defectBean2);
        store.putDefect(defectBean3.getId(), defectBean3);
        store.putDefect(defectBean4.getId(), defectBean4);
        store.putDefect(defectBean5.getId(), defectBean5);
        store.putDefect(defectBean6.getId(), defectBean6);
        store.putDefect(defectBean7.getId(), defectBean7);
        store.putDefect(defectBean8.getId(), defectBean8);
        store.putDefect(defectBean9.getId(), defectBean9);
        store.putDefect(defectBean10.getId(), defectBean10);
        store.putDefect(defectBean11.getId(), defectBean11);
        store.putDefect(defectBean12.getId(), defectBean12);
        store.putDefect(defectBean13.getId(), defectBean13);
        store.putDefect(defectBean14.getId(), defectBean14);

        return store;
    }

    private final Map<String, DefectBean> defects;
    private long                          defectId     = 0;
    private long                          lastModified = System.currentTimeMillis();

    /**
     * Constructor of memory defect store. It is initialized with test data.
     */
    private DataStore() {
        defects = Collections.synchronizedMap(new HashMap<String, DefectBean>());
    }

    /**
     * Returns instance of demo store of defect beans.
     * 
     * @return the instance of demo defect store
     */
    public static DataStore getInstance() {
        return defectStore;
    }

    /**
     * Returns all defect beans in a collection.
     * <p>
     * 
     * @return collection of defect beans
     */
    public Collection<DefectBean> getDefects() {
        return Collections.unmodifiableCollection(defects.values());
    }

    /**
     * Updates or creates defect of given key with provided value.
     * 
     * @param key defect key.
     * @param Defect defect bean.
     */
    public void putDefect(String key, DefectBean Defect) {
        this.defects.put(key, Defect);
        try {
            synchronized (defects) {
                long parseLong = Long.parseLong(key);
                if (this.defectId < parseLong) {
                    this.defectId = parseLong + 1;
                }
            }
        } catch (NumberFormatException e) {
        }
        this.lastModified = System.currentTimeMillis();
    }

    /**
     * Gives Defect of requested defect id.
     * 
     * @param key requested defect key (defect id).
     * @return requested defect, or <tt>null</tt> if Defect with such key does
     *         not exist.
     */
    public DefectBean getDefect(String key) {
        return this.defects.get(key);
    }

    /**
     * Returns <tt>true</tt> is memory store has defect for the provided key
     * (defect id).
     * 
     * @param key requested defect key (defect id).
     * @return <tt>true</tt> if the memory store holds the defect of the
     *         provided key.
     */
    public boolean containsDefect(String key) {
        return this.defects.containsKey(key);
    }

    /**
     * Removes defect legacy bean from memory store for specified defect key.
     * Removes also the key.
     * 
     * @param key requested defect key (defect id).
     * @return previous defect bean associated with specified key, or
     *         <tt>null<tt> if there was no mapping for key.
     */
    public DefectBean removeDefect(String key) {
        DefectBean old = this.defects.remove(key);
        this.lastModified = System.currentTimeMillis();
        return old;
    }

    /**
     * This method finds the highest Id of the defect beans in the memory store
     * and assuming it is Integer it returns value incremented by 1. In case it
     * is not Integer it returns String generated from the current time.
     * 
     * @return unique defect Id
     */
    public String getDefectUniqueId() {
        return Long.toString(++defectId);
    }

    public long getLastModified() {
        return lastModified;
    }

    /**
     * Returns last modified date rounded to the second.
     * 
     * @return
     */
    public Date getLastModifiedIgnoreMillis() {
        return new Date((lastModified / 1000l) * 1000l);
    }

}
