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

package com.unboundid.scim.data;

import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.Collection;



/**
 * This class represents the SCIM Service Provider Configuration.
 */
public class ServiceProviderConfig extends BaseResource
{
  /**
   * A <code>ResourceFactory</code> for creating
   * <code>ServiceProviderConfig</code> instances.
   */
  public static final ResourceFactory<ServiceProviderConfig>
      SERVICE_PROVIDER_CONFIG_RESOURCE_FACTORY =
      new ResourceFactory<ServiceProviderConfig>() {
    public ServiceProviderConfig createResource(
        final ResourceDescriptor resourceDescriptor,
        final SCIMObject scimObject) {
      return new ServiceProviderConfig(resourceDescriptor, scimObject);
    }
  };



  /**
   * Construct an empty <code>ServiceProviderConfig</code> with the specified
   * <code>ResourceDescriptor</code>.
   *
   * @param resourceDescriptor The resource descriptor for this SCIM resource.
   */
  public ServiceProviderConfig(final ResourceDescriptor resourceDescriptor) {
    super(resourceDescriptor);
  }



  /**
   * Construct a <code>ServiceProviderConfig</code> with the specified
   * <code>ResourceDescriptor</code> and backed by the given
   * <code>SCIMObject</code>.
   *
   * @param resourceDescriptor The resource descriptor for this SCIM resource.
   * @param scimObject         The <code>SCIMObject</code> containing all the
   *                           SCIM attributes and their values.
   */
  public ServiceProviderConfig(final ResourceDescriptor resourceDescriptor,
                               final SCIMObject scimObject) {
    super(resourceDescriptor, scimObject);
  }



  /**
   * Retrieves the PATCH configuration options.
   *
   * @return The PATCH configuration options.
   */
  public PatchConfig getPatchConfig()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "patch", PatchConfig.PATCH_CONFIG_RESOLVER);
  }



  /**
   * Specifies the PATCH configuration options.
   *
   * @param patchConfig The PATCH configuration options.
   * @return this resource instance.
   */
  public ServiceProviderConfig setPatchConfig(final PatchConfig patchConfig)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "patch", PatchConfig.PATCH_CONFIG_RESOLVER, patchConfig);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Retrieves the BULK configuration options.
   *
   * @return The BULK configuration options.
   */
  public BulkConfig getBulkConfig()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "bulk", BulkConfig.BULK_CONFIG_RESOLVER);
  }



  /**
   * Specifies the BULK configuration options.
   *
   * @param bulkConfig The BULK configuration options.
   * @return this resource instance.
   */
  public ServiceProviderConfig setBulkConfig(final BulkConfig bulkConfig)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "bulk", BulkConfig.BULK_CONFIG_RESOLVER, bulkConfig);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Retrieves the FILTER configuration options.
   *
   * @return The FILTER configuration options.
   */
  public FilterConfig getFilterConfig()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "filter", FilterConfig.FILTER_CONFIG_RESOLVER);
  }



  /**
   * Specifies the FILTER configuration options.
   *
   * @param filterConfig The FILTER configuration options.
   * @return this resource instance.
   */
  public ServiceProviderConfig setFilterConfig(final FilterConfig filterConfig)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "filter", FilterConfig.FILTER_CONFIG_RESOLVER, filterConfig);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Retrieves the Change Password configuration options.
   *
   * @return The Change Password configuration options.
   */
  public ChangePasswordConfig getChangePasswordConfig()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "changePassword", ChangePasswordConfig.CHANGE_PASSWORD_CONFIG_RESOLVER);
  }



  /**
   * Specifies the Change Password configuration options.
   *
   * @param changePasswordConfig The Change Password configuration options.
   * @return this resource instance.
   */
  public ServiceProviderConfig setChangePasswordConfig(
      final ChangePasswordConfig changePasswordConfig)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "changePassword",
          ChangePasswordConfig.CHANGE_PASSWORD_CONFIG_RESOLVER,
          changePasswordConfig);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Retrieves the SORT configuration options.
   *
   * @return The SORT configuration options.
   */
  public SortConfig getSortConfig()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "sort", SortConfig.SORT_CONFIG_RESOLVER);
  }



  /**
   * Specifies the SORT configuration options.
   *
   * @param sortConfig The SORT configuration options.
   * @return this resource instance.
   */
  public ServiceProviderConfig setSortConfig(final SortConfig sortConfig)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "sort", SortConfig.SORT_CONFIG_RESOLVER, sortConfig);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Retrieves the ETag configuration options.
   *
   * @return The ETag configuration options.
   */
  public ETagConfig getETagConfig()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "etag", ETagConfig.ETAG_CONFIG_RESOLVER);
  }



  /**
   * Specifies the ETag configuration options.
   *
   * @param etagConfig The ETag configuration options.
   * @return this resource instance.
   */
  public ServiceProviderConfig setETagConfig(final ETagConfig etagConfig)
  {
    try {
      setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
          "etag", ETagConfig.ETAG_CONFIG_RESOLVER, etagConfig);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Retrieves the supported Authentication Schemes.
   *
   * @return The supported Authentication Schemes.
   */
  public Collection<AuthenticationScheme> getAuthenticationSchemes()
  {
    return getAttributeValues(
        SCIMConstants.SCHEMA_URI_CORE,
        "authenticationSchemes",
        AuthenticationScheme.AUTHENTICATION_SCHEME_RESOLVER);
  }



  /**
   * Sets the supported Authentication Schemes.
   *
   * @param authenticationSchemes The supported Authentication Schemes.
   * @return this resource instance.
   */
  public ServiceProviderConfig setAuthenticationSchemes(
      final Collection<AuthenticationScheme> authenticationSchemes)
  {
    try {
      setAttributeValues(SCIMConstants.SCHEMA_URI_CORE,
                         "authenticationSchemes",
                         AuthenticationScheme.AUTHENTICATION_SCHEME_RESOLVER,
                         authenticationSchemes);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }



  /**
   * Retrieves the XML data format configuration options.
   *
   * @return The XML data format configuration options.
   */
  public XmlDataFormatConfig getXmlDataFormatConfig()
  {
    return getSingularAttributeValue(
        SCIMConstants.SCHEMA_URI_CORE,
        "xmlDataFormat",
        XmlDataFormatConfig.XML_DATA_FORMAT_CONFIG_RESOLVER);
  }



  /**
   * Specifies the XML data format configuration options.
   *
   * @param xmlDataFormatConfig The XML data format configuration options.
   * @return this resource instance.
   */
  public ServiceProviderConfig setXmlDataFormatConfig(
      final XmlDataFormatConfig xmlDataFormatConfig)
  {
    try {
      setSingularAttributeValue(
          SCIMConstants.SCHEMA_URI_CORE,
          "xmlDataFormat",
          XmlDataFormatConfig.XML_DATA_FORMAT_CONFIG_RESOLVER,
          xmlDataFormatConfig);
    } catch (InvalidResourceException e) {
      // This should never happen as these are core attributes...
      throw new RuntimeException(e);
    }
    return this;
  }
}
