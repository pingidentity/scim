/*
 * Copyright 2012-2024 Ping Identity Corporation
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

import com.unboundid.scim.sdk.OAuthTokenHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import java.io.InputStream;



/**
 * This class is a JAX-RS resource for the Bulk operation, where JSON
 * content type is specified in the URL path.
 */
@Path("Bulk.json")
public class JSONBulkResource extends AbstractBulkResource
{
  /**
   * Create a new instance of the bulk resource.
   *
   * @param application        The SCIM JAX-RS application associated with this
   *                           resource.
   * @param tokenHandler       The token handler to use for OAuth
   *                           authentication.
   */
  public JSONBulkResource(final SCIMApplication application,
                          final OAuthTokenHandler tokenHandler)
  {
    super(application, tokenHandler);
  }



  /**
   * Implement the POST operation consuming and producing JSON format.
   *
   * @param inputStream      The content to be consumed.
   * @param request          The HTTP servlet request.
   * @param securityContext  The security context for the request.
   * @param headers          The request headers.
   * @param uriInfo          The URI info for the request.
   *
   * @return  The response to the request.
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response doJsonJsonPost(final InputStream inputStream,
                                 @Context final HttpServletRequest request,
                                 @Context final SecurityContext securityContext,
                                 @Context final HttpHeaders headers,
                                 @Context final UriInfo uriInfo)
  {
    final RequestContext requestContext =
        new RequestContext(request, securityContext, headers, uriInfo,
                           MediaType.APPLICATION_JSON_TYPE,
                           MediaType.APPLICATION_JSON_TYPE);
    return postBulk(requestContext, inputStream);
  }
}
