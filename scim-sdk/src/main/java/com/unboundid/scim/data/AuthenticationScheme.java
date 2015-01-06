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

package com.unboundid.scim.data;

import com.unboundid.scim.schema.AttributeDescriptor;
import com.unboundid.scim.sdk.InvalidResourceException;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents the AuthenticationSchemes complex attribute in the
 * Service Provider Config.
 */
public class AuthenticationScheme
{
  /**
   * The <code>AttributeValueResolver</code> that resolves SCIM attribute values
   * to/from <code>AuthenticationScheme</code> instances.
   */
  public static final AttributeValueResolver<AuthenticationScheme>
      AUTHENTICATION_SCHEME_RESOLVER =
      new AttributeValueResolver<AuthenticationScheme>()
      {
        public AuthenticationScheme toInstance(final SCIMAttributeValue value) {
          Boolean p = value.getSubAttributeValue("primary",
              BOOLEAN_RESOLVER);
          return new AuthenticationScheme(
              value.getSubAttributeValue("name",
                  STRING_RESOLVER),
              value.getSubAttributeValue("description",
                  STRING_RESOLVER),
              value.getSubAttributeValue("specUrl",
                  STRING_RESOLVER),
              value.getSubAttributeValue("documentationUrl",
                  STRING_RESOLVER),
              value.getSubAttributeValue("type",
                  STRING_RESOLVER),
              p == null ? false : p);
        }

        @Override
        public SCIMAttributeValue fromInstance(
            final AttributeDescriptor addressDescriptor,
            final AuthenticationScheme value)
            throws InvalidResourceException {
          final List<SCIMAttribute> subAttributes =
              new ArrayList<SCIMAttribute>(8);

          if (value.type != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("type"),
                    SCIMAttributeValue.createStringValue(value.type)));
          }

          if (value.name != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("name"),
                    SCIMAttributeValue.createStringValue(value.name)));
          }

          if (value.description != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("description"),
                    SCIMAttributeValue.createStringValue(value.description)));
          }

          if (value.specUrl != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("specUrl"),
                    SCIMAttributeValue.createStringValue(value.specUrl)));
          }

          if (value.documentationUrl != null)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("documentationUrl"),
                    SCIMAttributeValue.createStringValue(
                        value.documentationUrl)));
          }

          if (value.primary)
          {
            subAttributes.add(
                SCIMAttribute.create(
                    addressDescriptor.getSubAttribute("primary"),
                    SCIMAttributeValue.createBooleanValue(value.primary)));
          }

          return SCIMAttributeValue.createComplexValue(subAttributes);
        }
      };



  private String name;
  private String description;
  private String specUrl;
  private String documentationUrl;
  private String type;
  private boolean primary;

  /**
   * Create a value of the SCIM AuthenticationSchemes attribute.
   *
   * @param name              The name of the Authentication Scheme.
   * @param description       The description of the Authentication Scheme.
   * @param specUrl           A HTTP addressable URL pointing to the
   *                          Authentication Scheme's specification.
   * @param documentationUrl  A HTTP addressable URL pointing to the
   *                          Authentication Scheme's usage documentation.
   * @param type              The type of Authentication Scheme.
   * @param primary           Specifies whether this value is the primary value.
   */
  public AuthenticationScheme(final String name,
                              final String description,
                              final String specUrl,
                              final String documentationUrl,
                              final String type,
                              final boolean primary) {
    this.name = name;
    this.description = description;
    this.specUrl = specUrl;
    this.documentationUrl = documentationUrl;
    this.primary = primary;
    this.type = type;
  }

  /**
   * Retrieves the name of the Authentication Scheme.
   *
   * @return The name of the Authentication Scheme.
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the Authentication Scheme.
   *
   * @param name The name of the Authentication Scheme.
   */
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * Retrieves the description of the Authentication Scheme.
   *
   * @return The description of the Authentication Scheme.
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the Authentication Scheme.
   *
   * @param description The description of the Authentication Scheme.
   */
  public void setDescription(final String description) {
    this.description = description;
  }

  /**
   * Retrieves the HTTP addressable URL pointing to the Authentication Scheme's
   * specification.
   *
   * @return The the HTTP addressable URL pointing to the Authentication
   *         Scheme's specification, or {@code null} if there is none.
   */
  public String getSpecUrl() {
    return specUrl;
  }

  /**
   * Sets the HTTP addressable URL pointing to the Authentication Scheme's
   * specification.
   * @param specUrl The HTTP addressable URL pointing to the Authentication
   *                Scheme's specification.
   */
  public void setSpecUrl(final String specUrl) {
    this.specUrl = specUrl;
  }

  /**
   * Retrieves the HTTP addressable URL pointing to the Authentication Scheme's
   * usage documentation.
   * @return The HTTP addressable URL pointing to the Authentication Scheme's
   *         usage documentation.
   */
  public String getDocumentationUrl() {
    return documentationUrl;
  }

  /**
   * Sets the HTTP addressable URL pointing to the Authentication Scheme's
   * usage documentation.
   * @param documentationUrl The HTTP addressable URL pointing to the
   *                         Authentication Scheme's usage documentation.
   */
  public void setDocumentationUrl(final String documentationUrl) {
    this.documentationUrl = documentationUrl;
  }

  /**
   * Indicates whether this value is the primary value.
   *
   * @return <code>true</code> if this value is the primary value or
   * <code>false</code> otherwise.
   */
  public boolean isPrimary() {
    return primary;
  }

  /**
   * Specifies whether this value is the primary value.
   *
   * @param primary Whether this value is the primary value.
   */
  public void setPrimary(final boolean primary) {
    this.primary = primary;
  }

  /**
   * Retrieves the type of Authentication Scheme.
   *
   * @return The type of Authentication Scheme.
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of Authentication Scheme.
   *
   * @param type The type of Authentication Scheme.
   */
  public void setType(final String type) {
    this.type = type;
  }



  @Override
  public boolean equals(final Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }

    final AuthenticationScheme that = (AuthenticationScheme) o;

    if (primary != that.primary)
    {
      return false;
    }
    if (description != null ? !description.equals(that.description) :
        that.description != null)
    {
      return false;
    }
    if (documentationUrl != null ?
        !documentationUrl.equals(that.documentationUrl) :
        that.documentationUrl != null)
    {
      return false;
    }
    if (name != null ? !name.equals(that.name) : that.name != null)
    {
      return false;
    }
    if (specUrl != null ? !specUrl.equals(that.specUrl) : that.specUrl != null)
    {
      return false;
    }
    if (type != null ? !type.equals(that.type) : that.type != null)
    {
      return false;
    }

    return true;
  }



  @Override
  public int hashCode()
  {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (specUrl != null ? specUrl.hashCode() : 0);
    result = 31 * result + (documentationUrl != null ?
                            documentationUrl.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (primary ? 1 : 0);
    return result;
  }



  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("AuthenticationScheme");
    sb.append("{name='").append(name).append('\'');
    sb.append(", description='").append(description).append('\'');
    sb.append(", specUrl='").append(specUrl).append('\'');
    sb.append(", documentationUrl='").append(documentationUrl).append('\'');
    sb.append(", type='").append(type).append('\'');
    sb.append(", primary=").append(primary);
    sb.append('}');
    return sb.toString();
  }



  /**
   * Convenience method that creates a new AuthenticationScheme instances for
   * HTTP BASIC.
   *
   * @param primary Whether this authentication scheme is primary
   *
   * @return A new AuthenticationScheme instances for HTTP BASIC.
   */
  public static AuthenticationScheme createBasic(final boolean primary)
  {
    return new AuthenticationScheme(
        "Http Basic",
        "The HTTP Basic Access Authentication scheme. This scheme is not " +
            "considered to be a secure method of user authentication (unless " +
            "used in conjunction with some external secure system such as " +
            "SSL), as the user name and password are passed over the network " +
            "as cleartext.",
        "http://www.ietf.org/rfc/rfc2617.txt",
        "http://en.wikipedia.org/wiki/Basic_access_authentication",
        "httpbasic", primary);
  }



  /**
   * Convenience method that creates a new AuthenticationScheme instances for
   * OAuth 2.
   *
   * @param primary Whether this authentication scheme is primary
   *
   * @return A new AuthenticationScheme instances for OAuth 2.
   */
  public static AuthenticationScheme createOAuth2(final boolean primary)
  {
    return new AuthenticationScheme(
        "OAuth 2.0",
        "The OAuth 2.0 Bearer Token Authentication scheme. OAuth enables " +
            "clients to access protected resources by obtaining an access " +
            "token, which is defined in RFC 6750 as \"a string " +
            "representing an access authorization issued to the client\", " +
            "rather than using the resource owner's credentials directly.",
        "http://tools.ietf.org/html/rfc6750",
        "http://oauth.net/2/",
        "oauth2", primary);
  }
}
