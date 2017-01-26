/*
 * Copyright 2017 UnboundID Corp.
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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;




/**
 * Filter to normalize request parameter names to lower case.
 */
@PreMatching
public class RequestParamFilter implements ContainerRequestFilter
{
  /**
   * {@inheritDoc}
   */
  public void filter(final ContainerRequestContext request)
      throws IOException
  {
    UriBuilder ub = request.getUriInfo().getRequestUriBuilder();
    final MultivaluedMap<String, String> map =
        request.getUriInfo().getQueryParameters();
    ub.replaceQuery(null);
    for (Map.Entry<String, List<String>> param : map.entrySet())
    {
      ub.queryParam(param.getKey().toLowerCase(), param.getValue().toArray());
    }
    request.setRequestUri(request.getUriInfo().getBaseUri(), ub.build());
  }
}
