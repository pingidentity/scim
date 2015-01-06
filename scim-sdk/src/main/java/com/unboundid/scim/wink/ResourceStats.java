/*
 * Copyright 2011-2015 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */

package com.unboundid.scim.wink;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class holds various statistics of each SCIM resource being served.
 */
public class ResourceStats
{
  /**
   * Number of query requests that were successful.
   */
  public static final String QUERY_OK = "query-successful";

  /**
   * Number of query requests that failed with code 400 Bad Request.
   */
  public static final String QUERY_BAD_REQUEST = "query-400";

  /**
   * Number of query requests that failed with code 401 Unauthorized.
   */
  public static final String QUERY_UNAUTHORIZED = "query-401";

  /**
   * Number of query requests that failed with code 403 Forbidden.
   */
  public static final String QUERY_FORBIDDEN = "query-403";

  /**
   * Number of query requests that failed with code 404 Not Found.
   */
  public static final String QUERY_NOT_FOUND = "query-404";

  /**
   * Number of query requests that failed with code 500 Internal Server Error.
   */
  public static final String QUERY_INTERNAL_SERVER_ERROR = "query-500";

  /**
   * Number of query requests that failed with code 505 Not Implemented.
   */
  public static final String QUERY_NOT_IMPLEMENTED = "query-505";

  /**
   * Number of query requests that responded in XML format.
   */
  public static final String QUERY_RESPONSE_XML = "query-response-xml";

  /**
   * Number of query requests that responded in JSON format.
   */
  public static final String QUERY_RESPONSE_JSON = "query-response-json";


  /**
   * Number of get requests that were successful.
   */
  public static final String GET_OK = "get-successful";

  /**
   * Number of versioned get requests that were not modified.
   */
  public static final String GET_NOT_MODIFIED = "get-304";

  /**
   * Number of get requests that failed with code 400 Bad Request.
   */
  public static final String GET_BAD_REQUEST = "get-400"

  /**
   * Number of get requests that failed with code 401 Unauthorized.
   */;
  public static final String GET_UNAUTHORIZED = "get-401";

  /**
   * Number of get requests that failed with code 403 Forbidden.
   */
  public static final String GET_FORBIDDEN = "get-403";

  /**
   * Number of get requests that failed with code 404 Not Found.
   */
  public static final String GET_NOT_FOUND = "get-404";

  /**
   * Number of get requests that failed with code 500 Internal Server Error.
   */
  public static final String GET_INTERNAL_SERVER_ERROR = "get-500";

  /**
   * Number of get requests that failed with code 505 Not Implemented.
   */
  public static final String GET_NOT_IMPLEMENTED = "get-505";

  /**
   * Number of get requests that responded in XML format.
   */
  public static final String GET_RESPONSE_XML = "get-response-xml";

  /**
   * Number of get requests that responded in JSON format.
   */
  public static final String GET_RESPONSE_JSON = "get-response-json";

  /**
   * Number of put requests that were successful.
   */
  public static final String PUT_OK = "put-successful";

  /**
   * Number of put requests that failed with code 400 Bad Request.
   */
  public static final String PUT_BAD_REQUEST = "put-400";

  /**
   * Number of put requests that failed with code 401 Unauthorized.
   */
  public static final String PUT_UNAUTHORIZED = "put-401";

  /**
   * Number of put requests that failed with code 403 Forbidden.
   */
  public static final String PUT_FORBIDDEN = "put-403";

  /**
   * Number of put requests that failed with code 404 Not Found.
   */
  public static final String PUT_NOT_FOUND = "put-404";

  /**
   * Number of put requests that failed with code 409 Conflict.
   */
  public static final String PUT_CONFLICT = "put-409";

  /**
   * Number of put requests that failed with code 412 Precondition Failed.
   */
  public static final String PUT_PRECONDITION_FAILED = "put-412";

  /**
   * Number of put requests that failed with code 500 Internal Server Error.
   */
  public static final String PUT_INTERNAL_SERVER_ERROR = "put-500";

  /**
   * Number of put requests that failed with code 505 Not Implemented.
   */
  public static final String PUT_NOT_IMPLEMENTED = "put-505";

  /**
   * Number of put requests that responded in XML format.
   */
  public static final String PUT_RESPONSE_XML = "put-response-xml";

  /**
   * Number of put requests that responded in JSON format.
   */
  public static final String PUT_RESPONSE_JSON = "put-response-json";

  /**
   * Number of put requests with content in XML format.
   */
  public static final String PUT_CONTENT_XML = "put-content-xml";

  /**
   * Number of put requests with content in JSON format.
   */
  public static final String PUT_CONTENT_JSON = "put-content-json";


  /**
   * Number of post requests that were successful.
   */
  public static final String POST_OK = "post-successful";

  /**
   * Number of post requests that failed with code 400 Bad Request.
   */
  public static final String POST_BAD_REQUEST = "post-400";

  /**
   * Number of post requests that failed with code 401 Unauthorized.
   */
  public static final String POST_UNAUTHORIZED = "post-401";

  /**
   * Number of post requests that failed with code 403 Forbidden.
   */
  public static final String POST_FORBIDDEN = "post-403";

  /**
   * Number of post requests that failed with code 409 Conflict.
   */
  public static final String POST_CONFLICT = "post-409";

  /**
   * Number of post requests that failed with code 413 Entity Too Large.
   */
  public static final String POST_REQUEST_ENTITY_TOO_LARGE = "post-413";

  /**
   * Number of post requests that failed with code 500 Internal Server Error.
   */
  public static final String POST_INTERNAL_SERVER_ERROR = "post-500";

  /**
   * Number of post requests that failed with code 505 Not Implemented.
   */
  public static final String POST_NOT_IMPLEMENTED = "post-505";

  /**
   * Number of post requests that responded in XML format.
   */
  public static final String POST_RESPONSE_XML = "post-response-xml";

  /**
   * Number of post requests that responded in JSON format.
   */
  public static final String POST_RESPONSE_JSON = "post-response-json";

  /**
   * Number of post requests with content in XML format.
   */
  public static final String POST_CONTENT_XML = "post-content-xml";

  /**
   * Number of post requests with content in JSON format.
   */
  public static final String POST_CONTENT_JSON = "post-content-json";


  /**
   * Number of patch requests that were successful.
   */
  public static final String PATCH_OK = "patch-successful";

  /**
   * Number of patch requests that failed with code 400 Bad Request.
   */
  public static final String PATCH_BAD_REQUEST = "patch-400";

  /**
   * Number of patch requests that failed with code 401 Unauthorized.
   */
  public static final String PATCH_UNAUTHORIZED = "patch-401";

  /**
   * Number of patch requests that failed with code 403 Forbidden.
   */
  public static final String PATCH_FORBIDDEN = "patch-403";

  /**
   * Number of patch requests that failed with code 404 Not Found.
   */
  public static final String PATCH_NOT_FOUND = "patch-404";

  /**
   * Number of patch requests that failed with code 409 Conflict.
   */
  public static final String PATCH_CONFLICT = "patch-409";

  /**
   * Number of patch requests that failed with code 412 Precondition Failed.
   */
  public static final String PATCH_PRECONDITION_FAILED = "patch-412";

  /**
   * Number of patch requests that failed with code 500 Internal Server Error.
   */
  public static final String PATCH_INTERNAL_SERVER_ERROR = "patch-500";

  /**
   * Number of patch requests that failed with code 505 Not Implemented.
   */
  public static final String PATCH_NOT_IMPLEMENTED = "patch-505";

  /**
   * Number of patch requests that responded in XML format.
   */
  public static final String PATCH_RESPONSE_XML = "patch-response-xml";

  /**
   * Number of patch requests that responded in JSON format.
   */
  public static final String PATCH_RESPONSE_JSON = "patch-response-json";

  /**
   * Number of patch requests with content in XML format.
   */
  public static final String PATCH_CONTENT_XML = "patch-content-xml";

  /**
   * Number of patch requests with content in JSON format.
   */
  public static final String PATCH_CONTENT_JSON = "patch-content-json";


  /**
   * Number of delete requests that were successful.
   */
  public static final String DELETE_OK = "delete-successful";

  /**
   * Number of delete requests that failed with code 400 Bad Request.
   */
  public static final String DELETE_BAD_REQUEST = "delete-400";

  /**
   * Number of delete requests that failed with code 401 Unauthorized.
   */
  public static final String DELETE_UNAUTHORIZED = "delete-401";

  /**
   * Number of delete requests that failed with code 403 Forbidden.
   */
  public static final String DELETE_FORBIDDEN = "delete-403";

  /**
   * Number of delete requests that failed with code 404 Not Found.
   */
  public static final String DELETE_NOT_FOUND = "delete-404";

  /**
   * Number of delete requests that failed with code 409 Conflict.
   */
  public static final String DELETE_CONFLICT = "delete-409";

  /**
   * Number of delete requests that failed with code 412 Precondition Failed.
   */
  public static final String DELETE_PRECONDITION_FAILED = "delete-412";

  /**
   * Number of delete requests that failed with code 500 Internal Server Error.
   */
  public static final String DELETE_INTERNAL_SERVER_ERROR = "delete-500";

  /**
   * Number of delete requests that failed with code 505 Not Implemented.
   */
  public static final String DELETE_NOT_IMPLEMENTED = "delete-505";

  private final String name;
  private final ConcurrentHashMap<String, AtomicLong> stats =
      new ConcurrentHashMap<String, AtomicLong>();

  /**
   * Create a new ResourceStats instance with the provided name.
   *
   * @param name The name of this ResourceStats instance, usually it should
   *             be the name of the SCIM resource being served.
   */
  public ResourceStats(final String name) {
    this.name = name;
  }

  /**
   * Increments a single statistical value.
   *
   * @param stat The name of the statistical value to increment.
   */
  void incrementStat(final String stat)
  {
    AtomicLong num = stats.get(stat);
    if(num == null)
    {
      num = new AtomicLong();
      AtomicLong prev = stats.putIfAbsent(stat, num);
      if(prev != null)
      {
        num = prev;
      }
    }
    num.getAndIncrement();
  }

  /**
   * Retrieves a single statistical value.
   *
   * @param stat The name of the statistical value to retrieve.
   * @return The statistical value or 0 if it is not present.
   */
  public long getStat(final String stat)
  {
    AtomicLong i = stats.get(stat);
    if(i != null)
    {
      return i.get();
    }
    return 0;
  }

  /**
   * Retrieves all statistical values that are present.
   *
   * @return All statistical values that are present.
   */
  public Map<String, Long> getStats()
  {
    Map<String, Long> map = new HashMap<String, Long>(stats.size());
    for(Map.Entry<String, AtomicLong> entry : stats.entrySet())
    {
      map.put(entry.getKey(), entry.getValue().get());
    }
    return map;
  }

  /**
   * Retrieves the name of this ResourceStats instance, usually the name of
   * the SCIM resource being served.
   *
   * @return The name of this ResourceStats instance, usually the name of
   * the SCIM resource being served.
   */
  public String getName() {
    return name;
  }
}
