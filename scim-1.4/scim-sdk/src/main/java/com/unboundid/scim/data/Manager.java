/*
 * Copyright 2011-2013 UnboundID Corp.
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

import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;

import java.util.ArrayList;
import java.util.List;

/**
 * The User's manager. A complex type that optionally allows Service Providers
 * to represent organizational hierarchy by referencing the "id" attribute of
 * another User
 */
public class Manager
{
  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Manager</code> instances.
   */
  public static final AttributeValueResolver<Manager> MANAGER_RESOLVER =
      new AttributeValueResolver<Manager>() {
        /**
         * {@inheritDoc}
         */
        public Manager toInstance(final SCIMAttributeValue value) {
          return new Manager(
              value.getSubAttributeValue("managerId",
                  STRING_RESOLVER),
              value.getSubAttributeValue("displayName",
                  STRING_RESOLVER));
        }

        /**
         * {@inheritDoc}
         */
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Manager value) throws InvalidResourceException {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(2);

          if (value.managerId != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    attributeDescriptor.getSubAttribute("managerId"),
                    SCIMAttributeValue.createStringValue(value.managerId)));
          }

          if (value.displayName != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    attributeDescriptor.getSubAttribute("displayName"),
                    SCIMAttributeValue.createStringValue(value.displayName)));
          }
          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };

  private String managerId;

  private String displayName;

  /**
   * Creates a SCIM enterprise user extension 'manager' attribute. Any of the
   * arguments may be {@code null} if they are not to be included.
   *
   * @param managerId        The id of the SCIM resource representing the User's
   *                         manager.
   * @param displayName      The displayName of the User's manager.
   */
  public Manager(final String managerId, final String displayName) {
    this.displayName = displayName;
    this.managerId = managerId;
  }

  /**
   * Retrieves the displayName of the User's manager.
   *
   * @return The displayName of the User's manager.
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets the displayName of the User's manager.
   *
   * @param displayName The displayName of the User's manager.
   */
  public void setDisplayName(final String displayName) {
    this.displayName = displayName;
  }

  /**
   * Retrieves the id of the SCIM resource representing the User's manager.
   *
   * @return The id of the SCIM resource representing the User's manager.
   */
  public String getManagerId() {
    return managerId;
  }

  /**
   * Sets the id of the SCIM resource representing the User's manager.
   *
   * @param managerId The id of the SCIM resource representing the User's
   *                  manager.
   */
  public void setManagerId(final String managerId) {
    this.managerId = managerId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Manager manager = (Manager) o;

    if (displayName != null ? !displayName.equals(manager.displayName) :
        manager.displayName != null) {
      return false;
    }
    if (managerId != null ? !managerId.equals(manager.managerId) :
        manager.managerId != null) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = managerId != null ? managerId.hashCode() : 0;
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Manager{" +
        "displayName='" + displayName + '\'' +
        ", managerId='" + managerId + '\'' +
        '}';
  }
}
