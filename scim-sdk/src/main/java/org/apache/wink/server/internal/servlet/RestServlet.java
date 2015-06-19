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

package org.apache.wink.server.internal.servlet;

import org.apache.wink.server.internal.DeploymentConfiguration;

import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;

/**
 *  Wink compatibility layer class - see Wink docs.
 */
public abstract class RestServlet extends HttpServlet
{
  /**
   *  Wink compatibility layer class - see Wink docs.
   * @param configuration Wink compatibility layer class - see Wink docs.
   * @return Wink compatibility layer class - see Wink docs.
   * @throws ClassNotFoundException Wink compatibility layer class
   * - see Wink docs.
   * @throws InstantiationException Wink compatibility layer class
   * - see Wink docs.
   * @throws IllegalAccessException Wink compatibility layer class
   * - see Wink docs.
   */
  protected abstract Application getApplication(
      DeploymentConfiguration configuration)
      throws ClassNotFoundException, InstantiationException,
      IllegalAccessException;
}
