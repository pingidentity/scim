/*
 * Copyright 2011-2019 Ping Identity Corporation
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * A complex type containing metadata about the resource.
 */
public class Meta
{
  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>Meta</code> instances.
   */
  public static final AttributeValueResolver<Meta> META_RESOLVER =
      new AttributeValueResolver<Meta>() {
        /**
         * {@inheritDoc}
         */
        @Override
        public Meta toInstance(final SCIMAttributeValue value) {
          String l = value.getSubAttributeValue("location",
              STRING_RESOLVER);
          Meta meta = new Meta(
              value.getSubAttributeValue("created",
                  DATE_RESOLVER),
              value.getSubAttributeValue("lastModified",
                  DATE_RESOLVER),
              l == null ? null : URI.create(l),
              value.getSubAttributeValue("version",
                  STRING_RESOLVER));
          Collection<String> diagInfoValues =
              value.getSubAttributeValues("__diagnostic_info",
                                          STRING_RESOLVER);
          if (diagInfoValues != null)
          {
            meta.setDiagnosticInfo(
                diagInfoValues.toArray(new String[diagInfoValues.size()]));
          }
          return meta;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor attributeDescriptor,
            final Meta value) throws InvalidResourceException {
          Collection<SCIMAttribute> attributes =
              new ArrayList<SCIMAttribute>(3);

          if(value.created != null)
          {
            AttributeDescriptor subAttributeDescriptor =
                attributeDescriptor.getSubAttribute("created");
            attributes.add(SCIMAttribute.create(
                subAttributeDescriptor,
                SCIMAttributeValue.createDateValue(value.created)));
          }
          if(value.lastModified != null)
          {
            AttributeDescriptor subAttributeDescriptor =
                attributeDescriptor.getSubAttribute("lastModified");
            attributes.add(SCIMAttribute.create(
                subAttributeDescriptor,
                SCIMAttributeValue.createDateValue(value.lastModified)));
          }
          if(value.location != null)
          {
            AttributeDescriptor subAttributeDescriptor =
                attributeDescriptor.getSubAttribute("location");
            attributes.add(SCIMAttribute.create(
                subAttributeDescriptor,
                SCIMAttributeValue.createStringValue(
                    value.location.toString())));
          }
          if(value.version != null)
          {
            AttributeDescriptor subAttributeDescriptor =
                attributeDescriptor.getSubAttribute("version");
            attributes.add(SCIMAttribute.create(
                subAttributeDescriptor,
                SCIMAttributeValue.createStringValue(value.version)));
          }
          if(value.diagnosticInfo != null && value.diagnosticInfo.length > 0)
          {
            SCIMAttributeValue[] diagInfoValues =
                new SCIMAttributeValue[value.diagnosticInfo.length];
            for (int i = 0; i < value.diagnosticInfo.length; i++)
            {
              diagInfoValues[i] =
                  SCIMAttributeValue.createStringValue(value.diagnosticInfo[i]);
            }
            AttributeDescriptor subAttributeDescriptor =
                attributeDescriptor.getSubAttribute("__diagnostic_info");
            attributes.add(SCIMAttribute.create(
                subAttributeDescriptor,
                diagInfoValues));
          }

          return SCIMAttributeValue.createComplexValue(attributes);
        }
      };

  private Date created;
  private Date lastModified;
  private URI location;
  private String version;
  private String[] diagnosticInfo;

  /**
   * Create an instance of the SCIM meta attribute.
   *
   * @param created         The time the Resource was added to the
   *                        Service Provider.
   * @param lastModified    The most recent time the details of a Resource
   *                        were updated at the Service Provider.
   * @param location        The URI of the Resource.
   * @param version         The version of the Resource.
   */
  public Meta(final Date created, final Date lastModified, final URI location,
              final String version) {
    this.created = created;
    this.lastModified = lastModified;
    this.location = location;
    this.version = version;
    this.diagnosticInfo = new String[0];
  }

  /**
   * Retrieves the time the Resource was added to the Service Provider.
   *
   * @return The time the Resource was added to the Service Provider.
   */
  public Date getCreated() {
    return created;
  }

  /**
   * Sets the time the Resource was added to the Service Provider.
   *
   * @param created The time the Resource was added to the Service Provider.
   */
  public void setCreated(final Date created) {
    this.created = created;
  }

  /**
   * Retrieves the most recent time the details of a Resource were updated at
   * the Service Provider.
   *
   * @return The most recent time the details of a Resource were updated at
   * the Service Provider.
   */
  public Date getLastModified() {
    return lastModified;
  }

  /**
   * Sets the most recent time the details of a Resource were updated at
   * the Service Provider.
   *
   * @param lastModified The most recent time the details of a Resource were
   * updated at the Service Provider.
   */
  public void setLastModified(final Date lastModified) {
    this.lastModified = lastModified;
  }

  /**
   * Retrieves the URI of the Resource.
   *
   * @return The URI of the Resource.
   */
  public URI getLocation() {
    return location;
  }

  /**
   * Sets the URI of the Resource.
   *
   * @param location The URI of the Resource.
   */
  public void setLocation(final URI location) {
    this.location = location;
  }

  /**
   * Retrieves the version of the Resource.
   *
   * @return The version of the Resource.
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version of the Resource being returned.
   *
   * @param version The version of the Resource being returned.
   */
  public void setVersion(final String version) {
    this.version = version;
  }

  /**
   * Retrieves the diagnostic information.
   *
   * @return The diagnostic information.
   */
  public String[] getDiagnosticInfo()
  {
    return diagnosticInfo;
  }

  /**
   * Sets the diagnostic information.
   *
   * @param diagnosticInfo   The diagnostic information.
   */
  public void setDiagnosticInfo(final String[] diagnosticInfo)
  {
    this.diagnosticInfo = diagnosticInfo;
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

    Meta meta = (Meta) o;

    if (created != null ? !created.equals(meta.created) :
        meta.created != null) {
      return false;
    }
    if (lastModified != null ? !lastModified.equals(meta.lastModified) :
        meta.lastModified != null) {
      return false;
    }
    if (location != null ? !location.equals(meta.location) :
        meta.location != null) {
      return false;
    }
    if (version != null ? !version.equals(meta.version) :
        meta.version != null) {
      return false;
    }

    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = created != null ? created.hashCode() : 0;
    result = 31 * result + (lastModified != null ? lastModified.hashCode() : 0);
    result = 31 * result + (location != null ? location.hashCode() : 0);
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Meta{" +
        "created=" + created +
        ", lastModified=" + lastModified +
        ", location=" + location +
        ", version='" + version + '\'' +
        '}';
  }
}
