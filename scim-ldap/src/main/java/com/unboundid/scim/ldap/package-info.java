/*
 * Copyright 2011-2012 UnboundID Corp.
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

/**
 * This package contains classes that map SCIM resources to LDAP entries and
 * vice versa. It also contains several APIs that may be used to implement
 * custom behaviours for the mapping configuration file to extend its
 * capabilities above and beyond those provided out of the box. Each extension
 * type varies in the amount of control the implementation have over the mapping
 * process and  the amount of effort required for implementation. The extension
 * types include:
 * <BR><BR>
 * <ul>
 *   <li>{@link com.unboundid.scim.ldap.Transformation} - For altering the
 *   values of mapped attribute</li>
 *   <li>{@link com.unboundid.scim.ldap.DerivedAttribute} - For generating the
 *   value of a read-only SCIM attribute</li>
 *   <li>{@link com.unboundid.scim.ldap.ResourceMapper} - For overriding the
 *   behaviour of any part of the mapping process</li>
 * </ul>
 */
package com.unboundid.scim.ldap;
