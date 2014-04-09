/*
 * Copyright 2012-2014 UnboundID Corp.
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

package com.unboundid.scim.sdk;

import com.unboundid.scim.data.BaseResource;



/**
 * This class represents an individual operation within a bulk operation
 * request or response. The fields are defined as follows, in the order that
 * they must be encoded when XML is the content type:
 *
 * <pre>
 * Each operation corresponds to a single HTTP request against a Resource
 * endpoint. REQUIRED.
 *
 *    method
 *        The HTTP method of the current operation. Possible values are POST,
 *        PUT, PATCH or DELETE. REQUIRED.
 *    bulkId
 *        The transient identifier of a newly created Resource, unique within
 *        a bulk request and created by the Consumer. The bulkId serves as a
 *        surrogate Resource id enabling Consumers to uniquely identify newly
 *        created Resources in the Response and cross reference new Resources
 *        in and across operations within a bulk request. REQUIRED when method
 *        is POST.
 *    version
 *        The current Resource version. Version is REQUIRED if the Service
 *        Provider supports ETags and the method is PUT, DELETE, or PATCH.
 *    path
 *        The Resource's relative path. If the method is POST the value must
 *        specify a Resource type endpoint; e.g., /Users or /Groups whereas
 *        all other method values must specify the path to a specific Resource;
 *        e.g., /Users/2819c223-7f76-453a-919d-413861904646.
 *        REQUIRED in a request.
 *    location
 *        The Resource endpoint URL. REQUIRED in a response, except in the
 *        event of a POST failure.
 *    data
 *        The Resource data as it would appear for a single POST, PUT or PATCH
 *        Resource operation. REQUIRED in a request when method is POST, PUT
 *        and PATCH.
 *    status
 *        A complex type that contains information about the success or failure
 *        of one operation within the bulk job. REQUIRED in a response.
 *
 *        code
 *            The HTTP response code that would have been returned if a single
 *            HTTP request would have been used. REQUIRED.
 *        description
 *            A human readable error message. REQUIRED when an error occurred.
 *
 * </pre>
 */
public class BulkOperation
{
  /**
   * The different methods that are supported within bulk operations.
   */
  public enum Method
  {
    /**
     * The POST method, used to create a new SCIM resource.
     */
    POST,

    /**
     * The PUT method, used to replace the entire contents of a SCIM resource.
     */
    PUT,

    /**
     * The PATCH method, used to modify, add or delete attributes of a SCIM
     * resource.
     */
    PATCH,

    /**
     * The DELETE method, used to delete a SCIM resource.
     */
    DELETE
  }



  /**
   * The HTTP method of the operation. Possible values are POST, PUT, PATCH or
   * DELETE.
   */
  private final Method method;

  /**
   * The bulk operation identifier, required when the method is POST.
   */
  private final String bulkId;

  /**
   * The current resource version, or {@code null} if not provided.
   */
  private final String version;

  /**
   * The relative path of the resource, required in a request.
   */
  private final String path;

  /**
   * The resource endpoint URL, or {code null} if this is a request, or if this
   * is the response to a failed POST operation.
   */
  private final String location;

  /**
   * The resource data as it would appear for a single POST, PUT or PATCH
   * operation. This field is required in a request when method is POST, PUT
   * and PATCH.
   */
  private final BaseResource data;

  /**
   * Information about the success or failure of the operation.
   */
  private final Status status;


  /**
   * Construct a new BulkOperation object.
   *
   * @param method      The HTTP method of the operation. Possible values are
   *                    POST, PUT, PATCH or DELETE.
   * @param bulkId      The bulk operation identifier, required when the method
   *                    is POST.
   * @param version     The current resource version, or {@code null} if not
   *                    provided.
   * @param path        The relative path of the resource, or {@code null} if
   *                    this is a response.
   * @param location    The resource endpoint URL, or {code null} if this is a
   *                    request, or if this is the response to a failed POST
   *                    operation.
   * @param data        The resource data as it would appear for a single POST,
   *                    PUT or PATCH operation.
   * @param status      Information about the success or failure of the
   *                    operation, or {@code null} if this is a request.
   */
  public BulkOperation(final Method method,
                       final String bulkId,
                       final String version,
                       final String path,
                       final String location,
                       final BaseResource data,
                       final Status status)
  {
    this.method     = method;
    this.bulkId     = bulkId;
    this.version    = version;
    this.path       = path;
    this.location   = location;
    this.data       = data;
    this.status     = status;
  }



  /**
   * Create a new operation for a bulk request.
   *
   * @param method      The HTTP method of the operation. Possible values are
   *                    POST, PUT, PATCH or DELETE.
   * @param bulkId      The bulk operation identifier, required when the method
   *                    is POST.
   * @param version     The current resource version, or {@code null} if not
   *                    provided.
   * @param path        The relative path of the resource, or {@code null} if
   *                    this is a response.
   * @param data        The resource data as it would appear for a single POST,
   *                    PUT or PATCH operation.
   *
   * @return  The new bulk request operation.
   */
  public static BulkOperation createRequest(final Method method,
                                            final String bulkId,
                                            final String version,
                                            final String path,
                                            final BaseResource data)
  {
    return new BulkOperation(method, bulkId, version, path, null, data, null);
  }



  /**
   * Create a new operation for a bulk response.
   *
   * @param method      The HTTP method of the operation. Possible values are
   *                    POST, PUT, PATCH or DELETE.
   * @param bulkId      The bulk operation identifier, required when the method
   *                    is POST.
   * @param version     The resource version, or {@code null} if version is
   *                    not available or not supported.
   * @param location    The resource endpoint URL, or {@code null} if this is
   *                    the response to a DELETE operation or a failed POST
   *                    operation.
   * @param status      Information about the success or failure of the
   *                    operation.
   *
   * @return  The new bulk request operation.
   */
  public static BulkOperation createResponse(final Method method,
                                             final String bulkId,
                                             final String version,
                                             final String location,
                                             final Status status)
  {
    return new BulkOperation(method, bulkId, version, null, location, null,
                             status);
  }



  /**
   * Retrieve HTTP method of the operation. Possible values are POST, PUT,
   * PATCH or DELETE.
   * @return  The HTTP method of the operation. Possible values are POST, PUT,
   *          PATCH or DELETE.
   */
  public Method getMethod()
  {
    return method;
  }



  /**
   * Retrieve the bulk operation identifier, required when the method is POST.
   * @return  The bulk operation identifier, required when the method is POST.
   */
  public String getBulkId()
  {
    return bulkId;
  }



  /**
   * Retrieve the The current resource version, or {@code null} if not provided.
   * @return  The The current resource version, or {@code null} if not provided.
   */
  public String getVersion()
  {
    return version;
  }



  /**
   * Retrieve the The relative path of the resource, or {@code null} if this
   * is a response.
   * @return  The The relative path of the resource, or {@code null} if this
   *          is a response.
   */
  public String getPath()
  {
    return path;
  }



  /**
   * Retrieve the resource endpoint URL, or {@code null} if this is a request,
   * or if this is the response to a failed POST operation.
   * @return  The resource endpoint URL, or {@code null} if this is a request,
   *          or if this is the response to a failed POST operation.
   */
  public String getLocation()
  {
    return location;
  }



  /**
   * Retrieve the resource data as it would appear for a single POST, PUT or
   * PATCH operation.
   * @return  The resource data as it would appear for a single POST, PUT or
   *          PATCH operation.
   */
  public BaseResource getData()
  {
    return data;
  }



  /**
   * Retrieve information about the success or failure of the operation, or
   * {@code null} if this is a request.
   * @return  Information about the success or failure of the operation, or
   *          {@code null} if this is a request.
   */
  public Status getStatus()
  {
    return status;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("BulkOperation");
    sb.append("{method='").append(method).append('\'');
    sb.append(", bulkId='").append(bulkId).append('\'');
    sb.append(", version='").append(version).append('\'');
    sb.append(", path='").append(path).append('\'');
    sb.append(", location='").append(location).append('\'');
    sb.append(", data=").append(data);
    sb.append(", status=").append(status);
    sb.append('}');
    return sb.toString();
  }
}
