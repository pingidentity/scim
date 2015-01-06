/*
 * Copyright 2014-2015 UnboundID Corp.
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


package com.unboundid.scim.data;

import com.unboundid.scim.sdk.ListResponse;
import com.unboundid.scim.sdk.PageParameters;
import com.unboundid.scim.sdk.SortParameters;

import java.util.List;


/**
 * UnboundID extension for a streamed query based on the SCIM 2.0 Query using
 * POST.  This extension is intended to be used for queries across a
 * large set of potential results, as it does not require the full result set
 * to be held in memory or processed as a whole.
 * <p/>
 * StreamedQuery does not support sorting or random access into the result set.
 * As a result it only supports setting page size, not start index.
 */
public class StreamedQueryRequest extends QueryRequest {

  /**
   * Schema URN for the UnboundID streamed search extension.
   */
  public static final String STREAMED_SEARCH_REQUEST_SCHEMA =
      "urn:unboundId:scim:api:messages:2.0:StreamedSearchRequest";

  /**
   * Name of the request/response attribute for retrieving the next
   * page of results.
   */
  public static final String RESUME_TOKEN_ATTRIBUTE = "resumeToken";


  /**
   * Construct a new StreamedQueryRequest with the specified parameters.
   * @param filter      The SCIM filter string.
   * @param attributes  Requested attributes, or null to return all attributes.
   * @param pageSize    The number of records to return in the results page.
   */
  public StreamedQueryRequest(
      final String filter,
      final List<String> attributes,
      final int pageSize)
  {
    getSchemas().add(STREAMED_SEARCH_REQUEST_SCHEMA);
    setFilter(filter);
    setAttributes(attributes);
    setPageSize(pageSize);
  }


  /**
   * Set the requested page size for query results.  A page size of 0
   * indicates that the server should select the page size.
   * @param pageSize page size.
   */
  public void setPageSize(final int pageSize)
  {
    super.setPageParameters(new PageParameters(0, pageSize));
  }

  /**
   * Get the page size for this query request.
   * @return The page size for the query request.
   */
  public int getPageSize()
  {
    return super.getPageParameters().getCount();
  }

  /**
   * Get the continuation token associated with this query request.
   * @return The resume token currently associated with the request, or null.
   */
  public String getResumeToken()
  {
    return (String)getExtensionAttribute(
        STREAMED_SEARCH_REQUEST_SCHEMA, RESUME_TOKEN_ATTRIBUTE);
  }

  /**
   * Sorting is not supported by the StreamedQuery, so this method always
   * returns null.
   * @return Always null.
   */
  @Override
  public SortParameters getSortParameters()
  {
    return null;
  }

  /**
   * Sorting is not supported by a StreamedQuery.
   * @param sortParameters Unsupported.
   */
  @Override
  public void setSortParameters(final SortParameters sortParameters)
  {
    throw new UnsupportedOperationException(
        "StreamedQuery does not support sorting.");
  }

  /**
   * A full paging specification is not supported by StreamedQuery.  Use
   * {@link #setPageSize} instead.
   * @param pageParameters Unsupported.
   */
  @Override
  public void setPageParameters(final PageParameters pageParameters)
  {
    throw new UnsupportedOperationException("Use setPageSize() instead.");
  }


  /**
   * Determine if the query has more results available, and if so update
   * the request object for retrieving the next page.
   * @param lastResponse The most recent response received from the SCIM
   *                     Server.
   * @return true if more results are available.
   */
  public boolean hasMoreResults(final ListResponse lastResponse)
  {
    String resumeToken = (String)lastResponse.getExtensionAttribute(
        STREAMED_SEARCH_REQUEST_SCHEMA,
        RESUME_TOKEN_ATTRIBUTE);

    setExtensionAttribute(STREAMED_SEARCH_REQUEST_SCHEMA,
        RESUME_TOKEN_ATTRIBUTE, resumeToken);

    return resumeToken != null && !resumeToken.isEmpty();
  }
}
