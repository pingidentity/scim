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

package org.apache.wink.example.bookmarks;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Memory store of bookmarks, created just for the needs of Bookmarks demo.
 * <p>
 * Memory store contains several predefined bookmarks. It is possible to list
 * all bookmarks, to get a particular bookmark and update an existing bookmark.
 * It is also possible to check whether the bookmark of a particular key exists.
 * <p>
 * BookmarkStore is singleton. The class is thread-safe.
 */
public class BookmarkStore {

    // --- singleton ---

    private static BookmarkStore             store     = new BookmarkStore();
    private static final Map<String, String> bookmarks =
                                                           Collections
                                                               .synchronizedMap(new HashMap<String, String>());
    private static int                       id        = 0;

    static {
        bookmarks.put("my-bookmark", "My demo bookmark");
        bookmarks.put(String.valueOf(++id), "First demo bookmark");
        bookmarks.put(String.valueOf(++id), "Second demo bookmark");
        bookmarks.put(String.valueOf(++id), "Third demo bookmark");
    }

    /**
     * Provides instance of this class (singleton)
     * 
     * @return instance of this class
     */
    public static BookmarkStore getInstance() {
        return store;
    }

    /**
     * Constructor. Bookmarks are initialized with some data.
     */
    private BookmarkStore() {
    }

    /**
     * Gives hash map with all existing bookmarks and its keys.
     * 
     * @return all existing bookmarks in memory store.
     */
    public Map<String, String> getBookmarks() {
        return Collections.unmodifiableMap(bookmarks);
    }

    /**
     * Updates or creates bookmark of given key with provided value.
     * 
     * @param key bookmark key.
     * @param bookmark bookmark value.
     */
    public void putBookmark(String key, String bookmark) {
        bookmarks.put(key, bookmark);
    }

    /**
     * Gives bookmark of requested bookmark Id.
     * 
     * @param key requested bookmark key (bookmark Id).
     * @return requested bookmark, or <tt>null</tt> if bookmark with such key
     *         does not exist.
     */
    public String getBookmark(String key) {
        return bookmarks.get(key);
    }

    /**
     * Returns <tt>true</tt> is memory store has bookmark for the provided key
     * (bookmark Id).
     * 
     * @param key requested bookmark key (bookmark Id).
     * @return <tt>true</tt> if memory store has bookmark of the provided key.
     */
    public boolean containsBookmark(String key) {
        return bookmarks.containsKey(key);
    }

    /**
     * Generate Bookmark ID
     * 
     * @return Bookmark ID
     */
    public static String getNewId() {
        return String.valueOf(++id);
    }

    /**
     * Delete bookmark of given key.
     * 
     * @param key bookmark key.
     */
    public void deleteBookmark(String key) {
        bookmarks.remove(key);
    }

}
