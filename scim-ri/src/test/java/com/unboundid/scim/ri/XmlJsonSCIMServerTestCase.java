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

package com.unboundid.scim.ri;

import com.unboundid.scim.sdk.SCIMService;

import javax.ws.rs.core.MediaType;

/**
 * This class provides test coverage for the SCIMServer class with XML content
 * and JSON response.
 */
public class XmlJsonSCIMServerTestCase extends SCIMServerTestCase
{
  @Override
  protected SCIMService createSCIMService(String userName, String password)
  {
    SCIMService service = super.createSCIMService(userName, password);
    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    return service;
  }
}
