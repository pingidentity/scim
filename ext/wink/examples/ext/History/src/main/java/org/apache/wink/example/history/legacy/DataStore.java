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

package org.apache.wink.example.history.legacy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of a demo memory store of legacy beans. Singleton class.
 * <p/>
 * The class is thread-safe. All method see (or change) at the moment when they
 * are invoked, later changes don't affect the returned collections, values...
 */
public class DataStore {

    private static DataStore defectStore = createSingletonInstance();

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
                           new Date(50505050), "tester1", "1-critical", "New", "developer1", 0);
        DefectBean defectBean2 =
            new DefectBean("2", "Cancel button is missing",
                           "In Payment Services Confirmation page, the Cancel button is missing.",
                           new Date(50505051), "tester2", "4-minor", "New", "developer2", 0);
        DefectBean defectBean3 =
            new DefectBean(
                           "3",
                           "Login failure",
                           "When trying to login with the user 'Admin', I'm getting an error 'The user is locked'.",
                           new Date(50505052), "tester3", "1-critical", "Assigned", "developer3", 0);

        DefectBean defectBean4 =
            new DefectBean(
                           "4",
                           "Missing expiration field",
                           "In Payment Services page, the expiration field of credit card is missing.",
                           new Date(50505052), "tester2", "2-high", "Assigned", "developer3", 0);

        DefectBean defectBean5 =
            new DefectBean(
                           "1",
                           "Missing asterisk for mandatory fields",
                           "In Payment Services page, there is no indication for mandatory fields.",
                           new Date(50505052), "tester2", "4-minor", "Assigned", "developer3", 0);

        DefectBean defectBean6 =
            new DefectBean(
                           "2",
                           "User is not locked",
                           "When trying to login with the same user three times with wrong password, the user doesn't get locked.",
                           new Date(50505052), "tester3", "2-high", "Fixed", "developer2", 0);

        DefectBean defectBean7 =
            new DefectBean("3", "Logout button", "The pages are missing the button 'logout'.",
                           new Date(50505052), "tester1", "2-high", "New", "developer1", 0);

        DefectBean defectBean8 =
            new DefectBean("4", "Error messages", "Error messages are not displayed in red color",
                           new Date(50505052), "tester1", "4-minor", "Fixed", "developer1", 0);

        DefectBean defectBean9 =
            new DefectBean(
                           "5",
                           "Submit button is disabled in Edit Profile page",
                           "When trying to submit changes in Edit Profile page, the Submit button is disabled.",
                           new Date(50505052), "tester3", "2-high", "New", "developer1", 0);

        DefectBean defectBean10 =
            new DefectBean("1", "Id field is disabled in Edit Profile page",
                           "Cannot change the Id in Edit Profile page, it is disabled.",
                           new Date(50505052), "tester1", "2-high", "New", "developer1", 0);

        DefectBean defectBean11 =
            new DefectBean("2", "Cannot login to the system",
                           "Cannot login to the system with the user 'David'.", new Date(50505052),
                           "tester3", "1-critical", "Rejected", "developer1", 0);

        DefectBean defectBean12 =
            new DefectBean("3", "Checkbox is disabled",
                           "Checkbox is disabled in Edit Profile page", new Date(50505052),
                           "tester3", "2-high", "Rejected", "developer1", 0);

        DefectBean defectBean13 =
            new DefectBean("6", "Missing message text",
                           "Missing message text in Edit Profile page", new Date(50505052),
                           "tester3", "4-minor", "Rejected", "developer1", 0);

        DefectBean defectBean14 =
            new DefectBean(
                           "7",
                           "Wrong translation for the word 'user'",
                           "When moving to French language the translation of 'user' word is incorrect",
                           new Date(50505052), "tester3", "2-high", "Rejected", "developer1", 0);

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

    private final Map<String, List<DefectBean>> defects;
    private long                                defectId = 0;

    /**
     * Constructor of memory defect store. It is initialized with test data.
     */
    private DataStore() {
        defects = new HashMap<String, List<DefectBean>>();
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
     * The returned collection is a copy of actual collection. This means that
     * changes in the store will not effect the returned collection. Make sure
     * to call this method each time the collection might have been changed.
     * <p>
     * Only the last revision of each defect will be returned.
     * <p>
     * Deleted defects are skipped.
     * 
     * @return collection of defect beans
     */
    public Collection<DefectBean> getDefects() {
        synchronized (defects) {
            ArrayList<DefectBean> set = new ArrayList<DefectBean>();
            for (List<DefectBean> list : defects.values()) {
                DefectBean defect = getLastDefect(list);
                if (defect != null && !defect.isDeleted()) {
                    set.add(defect);
                }
            }
            return set;
        }
    }

    /**
     * returns last defect in the list
     * 
     * @param list
     * @return
     */
    private DefectBean getLastDefect(List<DefectBean> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /**
     * <p>
     * Updates or creates defect of given key with provided value.
     * <p>
     * The defect will be added to the end of the revision list and defect's
     * revision will be updated. The method does not validate that the update is
     * allowed.
     * 
     * @param key defect key.
     * @param defect defect bean.
     */
    public void putDefect(String key, DefectBean defect) {
        synchronized (defects) {
            try {
                long parseLong = Long.parseLong(key);
                if (this.defectId < parseLong) {
                    this.defectId = parseLong + 1;
                }

            } catch (NumberFormatException e) {
            }
            List<DefectBean> list = this.defects.get(key);
            if (list == null) {
                list = new ArrayList<DefectBean>();
                this.defects.put(key, list);
            }
            list.add(defect);
            defect.setRevision(list.size() - 1);
        }
    }

    /**
     * Returns defect of requested defect id. The undeleted defect with the last
     * revision will be returned.
     * 
     * @param key requested defect key (defect id).
     * @return requested defect, or <tt>null</tt> if Defect with such key does
     *         not exist.
     */
    public DefectBean getDefect(String key) {
        synchronized (defects) {
            List<DefectBean> list = this.defects.get(key);
            DefectBean defect = getLastDefect(list);
            if (defect == null || defect.isDeleted()) {
                return null;
            }
            return defect;
        }
    }

    /**
     * returns true if the defect with the given id was deleted if not found,
     * returns false
     * 
     * @param key
     * @return
     */
    public boolean isDefectDeleted(String key) {
        synchronized (defects) {
            List<DefectBean> list = this.defects.get(key);
            DefectBean defect = getLastDefect(list);
            if (defect != null && defect.isDeleted()) {
                return true;
            }
            return false;
        }
    }

    /**
     * Returns defect of requested defect id and revision. The defect is
     * returned even if it was deleted
     * 
     * @param key
     * @param revision
     * @return requested defect, or <tt>null</tt> if Defect with such key and
     *         revision does not exist.
     */
    public DefectBean getDefect(String key, int revision) {
        synchronized (defects) {
            List<DefectBean> list = this.defects.get(key);
            if (list != null && list.size() > revision) {
                return list.get(revision);
            }
            return null;
        }
    }

    /**
     * Returns history of the specific defect
     * 
     * @param key
     * @return list with history of the specific defect, empty list if not found
     */
    public List<DefectBean> getDefectHistory(String key) {
        synchronized (defects) {
            List<DefectBean> list = this.defects.get(key);
            if (list != null) {
                return list;
            }
            return Collections.emptyList();
        }
    }

    /**
     * deleted defect. The delete is actually adding new revision.
     * 
     * @param key requested defect key (defect id).
     * @return previous defect bean associated with specified key, or
     *         <tt>null<tt> if there was no mapping for key.
     */
    public DefectBean deleteDefect(String key) {
        synchronized (defects) {
            List<DefectBean> list = this.defects.get(key);
            DefectBean defect = getLastDefect(list);
            if (defect == null) {
                return null;
            }
            if (defect.isDeleted()) {
                return null;
            }
            DefectBean deletedDefect = new DefectBean(defect);
            deletedDefect.setDeleted(true);
            list.add(deletedDefect);
            deletedDefect.setRevision(list.size() - 1);
            return null;
        }
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

}
