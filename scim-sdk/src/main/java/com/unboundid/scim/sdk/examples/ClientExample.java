/*
 * Copyright 2011 UnboundID Corp.
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
package com.unboundid.scim.sdk.examples;

import com.unboundid.scim.data.AttributeValueResolver;
import com.unboundid.scim.data.BaseResource;
import com.unboundid.scim.data.Entry;
import com.unboundid.scim.data.Manager;
import com.unboundid.scim.data.Name;
import com.unboundid.scim.data.ResourceFactory;
import com.unboundid.scim.data.UserResource;
import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMEndpoint;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMService;

import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Collection;

/**
 * A simple client example.
 */
public class ClientExample {

  /**
   * A device resource extension.
   */
  public static class DeviceResource extends BaseResource
  {
    /**
     * Create a new empty device resource.
     *
     * @param resourceDescriptor The resource descriptor of this resource.
     */
    public DeviceResource(final ResourceDescriptor resourceDescriptor) {
      super(resourceDescriptor);
    }

    /**
     * Create a device resource based on the provided SCIMObject.
     *
     * @param resourceDescriptor The resource descriptor of this resource.
     * @param scimObject The SCIMObject containing all the attributes and
     * values.
     */
    public DeviceResource(final ResourceDescriptor resourceDescriptor,
                          final SCIMObject scimObject) {
      super(resourceDescriptor, scimObject);
    }

    /**
     * Retrieves the vendor name of this device.
     *
     * @return The vendor name of this device.
     */
    public String getVendorName()
    {
      return getSingularAttributeValue("urn:com:example:device:1.0",
          "vendorName", AttributeValueResolver.STRING_RESOLVER);
    }
  }

  /**
   * The resource factory that can be used to create device resource instances.
   */
  public static final ResourceFactory<DeviceResource> DEVICE_RESOURCE_FACTORY =
      new ResourceFactory<DeviceResource>() {
        /**
         * {@inheritDoc}
         */
        public DeviceResource createResource(
            final ResourceDescriptor resourceDescriptor,
            final SCIMObject scimObject) {
          return new DeviceResource(resourceDescriptor, scimObject);
        }
      };

  /**
   * The main method.
   *
   * @param args Parameters for the application.
   * @throws Exception If an error occurs.
   */
  public static void main(final String[] args) throws Exception
  {
    URI uri = URI.create("http://localhost:52959");
    SCIMService scimService = new SCIMService(uri,
        "uid=bjensen,dc=example,dc=com", "password");
    scimService.setAcceptType(MediaType.APPLICATION_JSON_TYPE);

    // Core user resource CRUD operation example
    SCIMEndpoint<UserResource> endpoint = scimService.getUserEndpoint();

    UserResource user = endpoint.get("uid=bjensen,dc=example,dc=com");

    Name name = user.getName();

    Collection<Entry<String>> phoneNumbers = user.getPhoneNumbers();
    if(phoneNumbers != null)
    {
      for(Entry<String> phoneNumber : phoneNumbers)
      {
        System.out.println(phoneNumber);
      }
    }

    // Attribute extension example
    Manager manager = user.getSingularAttributeValue(
        "urn:scim:schemas:extension:enterprise:1.0",  "manager",
        Manager.MANAGER_RESOLVER);
    if(manager == null)
    {
      manager = new Manager("uid=jsmith,dc=example,dc=com", "Manager John");
    }
    manager.setDisplayName("Best in the world");
    user.setSingularAttributeValue("urn:scim:schemas:extension:enterprise:1.0",
        "manager", Manager.MANAGER_RESOLVER, manager);

    String employeeNumber =
        user.getSingularAttributeValue(
            "urn:scim:schemas:extension:enterprise:1.0",  "employeeNumber",
            AttributeValueResolver.STRING_RESOLVER);

    user.setSingularAttributeValue("urn:scim:schemas:extension:enterprise:1.0",
        "department",  AttributeValueResolver.STRING_RESOLVER, "sales");

    user.setNickName("brad");

    endpoint.update(user);

    // Resource type extension example
    ResourceDescriptor deviceDescriptor =
        scimService.getResourceDescriptor("Device", null);
    SCIMEndpoint<DeviceResource> deviceEndpoint =
        scimService.getEndpoint(deviceDescriptor, DEVICE_RESOURCE_FACTORY);
  }
}
