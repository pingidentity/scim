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

package org.apache.wink.example.qadefect.legacy;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.apache.wink.example.qadefect.resources.DefectsResource;
import org.apache.wink.example.qadefect.utils.SearchMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a demo memory store of legacy beans. Singleton class.
 * <p/>
 * The class is thread-safe. All method see (or change) at the moment when they
 * are invoked, later changes don't affect the returned collections, values...
 */
public class DataStore {

    private static DataStore    defectStore = createSingletonInstance();

    private final static Logger logger      = LoggerFactory.getLogger(DataStore.class);

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
                           new Date(50505050), "tester1", "1-critical", "New", "developer1", null,
                           "attachementDefect1.jpg");
        DefectBean defectBean2 =
            new DefectBean("2", "Cancel button is missing",
                           "In Payment Services Confirmation page, the Cancel button is missing.",
                           new Date(50505051), "tester2", "4-minor", "New", "developer2", null,
                           "attachementDefect245.jpg");
        DefectBean defectBean3 =
            new DefectBean(
                           "3",
                           "Login failure",
                           "When trying to login with the user 'Admin', I'm getting an error 'The user is locked'.",
                           new Date(50505052), "tester3", "1-critical", "Assigned", "developer3",
                           null, "attachementDefect3.jpg");

        DefectBean defectBean4 =
            new DefectBean(
                           "4",
                           "Missing expiration field",
                           "In Payment Services page, the expiration field of credit card is missing.",
                           new Date(50505052), "tester2", "2-high", "Assigned", "developer3", null,
                           "attachementDefect245.jpg");

        DefectBean defectBean5 =
            new DefectBean(
                           "5",
                           "Missing asterisk for mandatory fields",
                           "In Payment Services page, there is no indication for mandatory fields.",
                           new Date(50505052), "tester2", "4-minor", "Assigned", "developer3",
                           null, "attachementDefect245.jpg");

        DefectBean defectBean6 =
            new DefectBean(
                           "6",
                           "User is not locked",
                           "When trying to login with the same user three times with wrong password, the user doesn't get locked.",
                           new Date(50505052), "tester3", "2-high", "Fixed", "developer2", null,
                           null);

        DefectBean defectBean7 =
            new DefectBean("7", "Logout button", "The pages are missing the button 'logout'.",
                           new Date(50505052), "tester1", "2-high", "New", "developer1", null, null);

        DefectBean defectBean8 =
            new DefectBean("8", "Error messages", "Error messages are not displayed in red color",
                           new Date(50505052), "tester1", "4-minor", "Fixed", "developer1", null,
                           null);

        DefectBean defectBean9 =
            new DefectBean(
                           "9",
                           "Submit button is disabled in Edit Profile page",
                           "When trying to submit changes in Edit Profile page, the Submit button is disabled.",
                           new Date(50505052), "tester3", "2-high", "New", "developer1", null, null);

        DefectBean defectBean10 =
            new DefectBean("10", "Id field is disabled in Edit Profile page",
                           "Cannot change the Id in Edit Profile page, it is disabled.",
                           new Date(50505052), "tester1", "2-high", "New", "developer1", null, null);

        DefectBean defectBean11 =
            new DefectBean("11", "Cannot login to the system",
                           "Cannot login to the system with the user 'David'.", new Date(50505052),
                           "tester3", "1-critical", "Rejected", "developer1", null, null);
        DefectBean defectBean12 =
            new DefectBean("12", "Checkbox is disabled",
                           "Checkbox is disabled in Edit Profile page", new Date(50505052),
                           "tester3", "2-high", "Rejected", "developer1", null, null);
        DefectBean defectBean13 =
            new DefectBean("13", "Missing message text",
                           "Missing message text in Edit Profile page", new Date(50505052),
                           "tester3", "4-minor", "Rejected", "developer1", null, null);
        DefectBean defectBean14 =
            new DefectBean(
                           "14",
                           "Wrong translation for the word 'user'",
                           "When moving to French language the translation of 'user' word is incorrect",
                           new Date(50505052), "tester3", "2-high", "Rejected", "developer1", null,
                           null);

        TestBean testBean1 =
            new TestBean("1", "Payment Services",
                         "Try to perform payment through the Payment Services pages", new Date(),
                         "test1", "Failed", Arrays.asList(defectBean1, defectBean2));

        TestBean testBean2 =
            new TestBean("2", "Login as admin", "Try to login with admin user", new Date(),
                         "test1", "Failed", Arrays.asList(defectBean1, defectBean3));

        TestBean testBean3 =
            new TestBean("3", "Shutdown application", "Try to shutdown application", new Date(),
                         "test1", "Completed", null);

        TestBean testBean4 =
            new TestBean("4", "Payment Services", "Check the fields in Payment Services page",
                         new Date(), "test1", "Failed", Arrays.asList(defectBean4, defectBean5));

        TestBean testBean5 =
            new TestBean(
                         "5",
                         "Check locking mechanism",
                         "Login with the same user three times with wrong password and check that after the third time the user gets locked",
                         new Date(), "test2", "Failed", Arrays.asList(defectBean4, defectBean6));

        TestBean testBean6 =
            new TestBean("6", "Check logout button", "Check that logout button is working",
                         new Date(), "test2", "Failed", Arrays.asList(defectBean7));

        TestBean testBean7 =
            new TestBean(
                         "7",
                         "Check error message visibility",
                         "Check that error messages are displayed in the correct font and in red color",
                         new Date(), "test1", "Failed", Arrays.asList(defectBean8));

        TestBean testBean8 =
            new TestBean("8", "Check Edit Profile page",
                         "Perform changes in Edit Profile page and submit it", new Date(), "test1",
                         "Failed", Arrays.asList(defectBean9,
                                                 defectBean10,
                                                 defectBean11,
                                                 defectBean12,
                                                 defectBean13));

        TestBean testBean9 =
            new TestBean("9", "Check authorization of Admin",
                         "Login as Admin user and check that you can create a new user",
                         new Date(), "test1", "Completed", null);

        TestBean testBean10 =
            new TestBean(
                         "10",
                         "Check authorization of regular user",
                         "Login as regular user and check that you don't have the option to create a new user",
                         new Date(), "test1", "Completed", null);

        TestBean testBean11 =
            new TestBean(
                         "11",
                         "Check Left Menu options for regular user",
                         "Login as regular user and check that on the Left Menu all the options for regular users exist",
                         new Date(), "test1", "Completed", null);

        TestBean testBean12 =
            new TestBean(
                         "12",
                         "Check Left Menu options for Admin user",
                         "Login as Admin user and check that on the Left Menu all the options for Admin users exist",
                         new Date(), "test1", "Completed", null);

        TestBean testBean13 =
            new TestBean("13", "Check login mechanism", "Check login mechanism for several users",
                         new Date(), "test2", "Failed", Arrays.asList(defectBean11));

        TestBean testBean14 =
            new TestBean("14", "Check words translations",
                         "Check words translations for all pages", new Date(), "test2", "Failed",
                         Arrays.asList(defectBean13, defectBean14));

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

        store.putTest(testBean1.getId(), testBean1);
        store.putTest(testBean2.getId(), testBean2);
        store.putTest(testBean3.getId(), testBean3);
        store.putTest(testBean4.getId(), testBean4);
        store.putTest(testBean5.getId(), testBean5);
        store.putTest(testBean6.getId(), testBean6);
        store.putTest(testBean7.getId(), testBean7);
        store.putTest(testBean8.getId(), testBean8);
        store.putTest(testBean9.getId(), testBean9);
        store.putTest(testBean10.getId(), testBean10);
        store.putTest(testBean11.getId(), testBean11);
        store.putTest(testBean12.getId(), testBean12);
        store.putTest(testBean13.getId(), testBean13);
        store.putTest(testBean14.getId(), testBean14);
        return store;
    }

    private final Map<String, DefectBean> defects;
    private final Map<String, TestBean>   tests;
    private long                          defectId = 0;
    private long                          testId   = 0;

    /**
     * Constructor of memory defect store. It is initialized with test data.
     */
    private DataStore() {
        defects = Collections.synchronizedMap(new HashMap<String, DefectBean>());
        tests = Collections.synchronizedMap(new HashMap<String, TestBean>());
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
     * 
     * @return collection of defect beans
     */
    public Collection<DefectBean> getDefects() {
        synchronized (defects) {
            return new HashSet<DefectBean>(defects.values());
        }
    }

    /**
     * returns defects using the search parameters. If search parameters are
     * null or empty, all defects are returned.
     * 
     * @param searchParameters
     * @return collection of defects
     */
    public Collection<DefectBean> getDefects(SearchMap searchParameters) {
        if (searchParameters == null || searchParameters.isEmpty()) {
            return getDefects();
        }

        logger.trace("Defect search was actived.");
        // in real world there should have been a sql query, but for example
        // purpose
        // we'll do search manually
        Set<DefectBean> hashSet = new TreeSet<DefectBean>(new Comparator<DefectBean>() {

            public int compare(DefectBean o1, DefectBean o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        synchronized (defects) {
            l_defects: for (DefectBean defect : defects.values()) {
                for (Entry<String, String> entry : searchParameters.entrySet()) {
                    String value = entry.getValue();
                    logger.trace("Parameter: {}; value: {}", entry.getKey(), value);

                    if (entry.getKey().equals(DefectsResource.ASSIGNED_TO)) {
                        if (defect.getAssignedTo() == null || !defect.getAssignedTo()
                            .equalsIgnoreCase(value)) {
                            logger.trace("Defect " + defect.getId()
                                + " was skipped, since it doesn't match "
                                + DefectsResource.ASSIGNED_TO
                                + " parameter: "
                                + defect.getAssignedTo());
                            continue l_defects;
                        }
                    }
                    if (entry.getKey().equals(DefectsResource.FTS)) {
                        if ((defect.getName() != null && !defect.getName().toLowerCase()
                            .contains(value)) && (defect.getDescription() != null && !defect
                            .getDescription().toLowerCase().contains(value))
                            && (defect.getSeverity() != null && !defect.getSeverity().toLowerCase()
                                .contains(value))
                            && (defect.getStatus() != null && !defect.getStatus().toLowerCase()
                                .contains(value))) {
                            logger.trace("Defect " + defect.getId()
                                + " was skipped, since it doesn't match "
                                + DefectsResource.FTS
                                + " parameter.");
                            continue l_defects;
                        }
                    }
                    if (entry.getKey().equals(DefectsResource.SEVERIIY)) {
                        if (defect.getSeverity() == null || !defect.getSeverity()
                            .equalsIgnoreCase(value)) {
                            logger.trace("Defect " + defect.getId()
                                + " was skipped, since it doesn't match "
                                + DefectsResource.SEVERIIY
                                + " parameter: "
                                + defect.getSeverity());
                            continue l_defects;
                        }
                    }
                }
                // defect matched all filters
                // add it to result collection
                logger.trace("Defect " + defect.getId() + " was added to result.");
                hashSet.add(defect);
            }
        }
        return hashSet;
    }

    /**
     * Returns all tests beans in collection.
     * <p>
     * The returned collection is a copy of actual collection. This means that
     * changes in the store will not effect the returned collection. Make sure
     * to call this method each time the collection might have been changed.
     */
    public Collection<TestBean> getTests() {
        synchronized (tests) {
            return new HashSet<TestBean>(tests.values());
        }
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
        return this.defects.remove(key);
    }

    /**
     * returns true if store contains test with the provided key
     * 
     * @param key
     * @return
     */
    public boolean containsTest(String key) {
        return tests.containsKey(key);
    }

    /**
     * returns TestBean if the store contains test with the provided key, or
     * null otherwise
     * 
     * @param key
     * @return TestBean
     */
    public TestBean getTest(String key) {
        return tests.get(key);
    }

    /**
     * adds new test to store. If store already contains a test with such key,
     * the old value is replaced with a new one
     * 
     * @param key
     * @param value
     */
    public void putTest(String key, TestBean value) {
        this.tests.put(key, value);
        try {
            synchronized (tests) {
                long parseLong = Long.parseLong(key);
                if (this.testId < parseLong) {
                    this.testId = parseLong + 1;
                }
            }
        } catch (NumberFormatException e) {
        }
    }

    /**
     * Removes test legacy bean from memory store for specified key. Removes
     * also the key.
     * 
     * @param key requested test key (test id).
     * @return previous test bean associated with specified key, or
     *         <tt>null<tt> if there was no mapping for key.
     */
    public TestBean removeTest(String key) {
        return tests.remove(key);
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

    /**
     * @return unique test id
     */
    public String getTestUniqueId() {
        return Long.toString(++testId);
    }
}
