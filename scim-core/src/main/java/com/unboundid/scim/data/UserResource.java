/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.data;

import com.unboundid.scim.schema.ResourceDescriptor;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;

import java.util.Collection;

/**
 * This class represents a User resource.
 */
public class UserResource extends BaseResource
{
  /**
   * A <code>ResourceFactory</code> for creating <code>UserResource</code>
   * instances.
   */
  public static final ResourceFactory<UserResource> USER_RESOURCE_FACTORY =
      new ResourceFactory<UserResource>() {
        /**
         * {@inheritDoc}
         */
        public UserResource createResource(
            final ResourceDescriptor resourceDescriptor,
            final SCIMObject scimObject) {
          return new UserResource(resourceDescriptor, scimObject);
        }
      };

  /**
   * Construct an empty <code>UserResource</code> with the specified
   * <code>ResourceDescriptor</code>.
   *
   * @param resourceDescriptor The resource descriptor for this SCIM resource.
   */
  public UserResource(final ResourceDescriptor resourceDescriptor) {
    super(resourceDescriptor);
  }

  /**
   * Construct a <code>UserResource</code> with the specified
   * <code>ResourceDescriptor</code> and backed by the given
   * <code>SCIMObject</code>.
   *
   * @param resourceDescriptor The resource descriptor for this SCIM resource.
   * @param scimObject         The <code>SCIMObject</code> containing all the
   *                           SCIM attributes and their values.
   */
  public UserResource(final ResourceDescriptor resourceDescriptor,
                      final SCIMObject scimObject) {
    super(resourceDescriptor, scimObject);
  }

  /**
   * Retrieves the Unique identifier for the User, typically used by the user
   * to directly authenticate to the service provider. Often displayed to the
   * user as their unique identifier within the system (as opposed to id o
   * r externalId, which are generally opaque and not user-friendly
   * identifiers).
   *
   * @return The Unique identifier for the User.
   */
  public String getUserName()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "userName", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the Unique identifier for the User.
   *
   * @param userName The Unique identifier for the User
   * @return this resource instance.
   */
  public UserResource setUserName(final String userName)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "userName", AttributeValueResolver.STRING_RESOLVER, userName);
    return this;
  }

  /**
   * Retrieves the components of the User's real name.
   *
   * @return The components of the User's real name or <code>null</code> if
   * it is not specified.
   */
  public Name getName()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "name", Name.NAME_RESOLVER);
  }

  /**
   * Sets the components of the User's real name.
   *
   * @param name The components of the User's real name.
   * @return this resource instance.
   */
  public UserResource setName(final Name name)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "name", Name.NAME_RESOLVER, name);
    return this;
  }

  /**
   * Retrieves the name of the User, suitable for display to end-users.
   *
   * @return The name of the User, suitable for display to end-users or
   * <code>null</code> if it is not specified.
   */
  public String getDisplayName()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "displayName", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the name of the User, suitable for display to end-users.
   *
   * @param displayName The name of the User, suitable for display to end-users.
   * @return this resource instance.
   */
  public UserResource setDisplayName(final String displayName)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "displayName", AttributeValueResolver.STRING_RESOLVER, displayName);
    return this;
  }

  /**
   * Retrieves the casual way to address the user in real life, e.g. "Bob" or
   * "Bobby" instead of "Robert".
   *
   * @return The casual way to address the user in real life.
   */
  public String getNickName()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "nickName", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the casual way to address the user in real life, e.g. "Bob" or
   * "Bobby" instead of "Robert".
   *
   * @param nickName The casual way to address the user in real life.
   * @return this resource instance.
   */
  public UserResource setNickName(final String nickName)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "nickName", AttributeValueResolver.STRING_RESOLVER, nickName);
    return this;
  }

  /**
   * Retrieves the URL to a page representing the User's online profile.
   *
   * @return The URL to a page representing the User's online profile or
   * <code>null</code> if it is not specified.
   */
  public String getProfileUrl()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "profileUrl", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the URL to a page representing the User's online profile.
   *
   * @param url The URL to a page representing the User's online profile.
   * @return this resource instance.
   */
  public UserResource setProfileUrl(final String url)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "profileUrl", AttributeValueResolver.STRING_RESOLVER, url);
    return this;
  }

  /**
   * Retrieves the user’s title, such as “Vice President”.
   *
   * @return The user’s title or <code>null</code> if it is not specified.
   */
  public String getTitle()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "title", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the user’s title, such as “Vice President”.
   *
   * @param title The user’s title.
   * @return this resource instance.
   */
  public UserResource setTitle(final String title)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "title", AttributeValueResolver.STRING_RESOLVER, title);
    return this;
  }

  /**
   * Retrieves the the organization to user relationship. Typical values used
   * might be "Contractor", "Employee", "Intern", "Temp", "External", and
   * "Unknown" but any value may be used.
   *
   * @return The the organization to user relationship or <code>null</code>
   * if it is not specified.
   */
  public String getUserType()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "userType", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the the organization to user relationship.
   *
   * @param userType The the organization to user relationship.
   * @return this resource instance.
   */
  public UserResource setUserType(final String userType)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "userType", AttributeValueResolver.STRING_RESOLVER, userType);
    return this;
  }

  /**
   * Retrieves the User's preferred written or spoken language. Generally
   * used for localizing the interface presented to the user.
   *
   * @return The User's preferred written or spoken language or
   * <code>null</code> if it is not specified.
   */
  public String getPreferredLanguage()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "preferredLanguage", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the User's preferred written or spoken language.
   *
   * @param language The User's preferred written or spoken language.
   * @return this resource instance.
   */
  public UserResource setPreferredLanguage(final String language)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "preferredLanguage", AttributeValueResolver.STRING_RESOLVER, language);
    return this;
  }

  /**
   * Retrieves the User's default location for purposes of localizing items
   * such as currency, date time format, numerical representations, etc.
   *
   * @return The User's default location for purposes of localizing items or
   * <code>null</code> if it is not specified.
   */
  public String getLocale()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "locale", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the User's default location for purposes of localizing items.
   *
   * @param locale The User's default location for purposes of localizing items.
   * @return this resource instance.
   */
  public UserResource setLocale(final String locale)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "displayName", AttributeValueResolver.STRING_RESOLVER, locale);
    return this;
  }

  /**
   * Retrieves the User's time zone in the public-domain time zone database
   * format; e.g.,'America/Denver'.
   *
   * @return The User's time zone or <code>null</code> if it is not specified.
   */
  public String getTimeZone()
  {
    return getSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "timezone", AttributeValueResolver.STRING_RESOLVER);
  }

  /**
   * Sets the User's time zone in the public-domain time zone database
   * format; e.g.,'America/Denver'.
   *
   * @param timeZone The User's time zone
   * @return this resource instance.
   */
  public UserResource setTimeZone(final String timeZone)
  {
    setSingularAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "timezone", AttributeValueResolver.STRING_RESOLVER, timeZone);
    return this;
  }

  /**
   * Retrieves the E-mail addresses for the User.
   *
   * @return The E-mail addresses for the User or <code>null</code> if it is
   * not specified.
   */
  public Collection<Entry<String>> getEmails()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "emails", Entry.STRINGS_RESOLVER);
  }

  /**
   * Sets the E-mail addresses for the User.
   *
   * @param emails The E-mail addresses for the User.
   * @return this resource instance.
   */
  public UserResource setEmails(final Collection<Entry<String>> emails)
  {
    setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "emails", Entry.STRINGS_RESOLVER, emails);
    return this;
  }

  /**
   * Retrieves the Phone numbers for the User.
   *
   * @return The Phone numbers for the User or <code>null</code>
   * if it is not specified.
   */
  public Collection<Entry<String>> getPhoneNumbers()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "phoneNumbers", Entry.STRINGS_RESOLVER);
  }

  /**
   * Sets the Phone numbers for the User.
   *
   * @param phoneNumbers The Phone numbers for the User.
   * @return this resource instance.
   */
  public UserResource setPhoneNumbers(
      final Collection<Entry<String>> phoneNumbers)
  {
    setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "phoneNumbers", Entry.STRINGS_RESOLVER, phoneNumbers);
    return this;
  }

  /**
   * Retrieves the Instant messaging address for the User.
   *
   * @return The Instant messaging address for the User or <code>null</code>
   * if it is not specified.
   */
  public Collection<Entry<String>> getIms()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "ims", Entry.STRINGS_RESOLVER);
  }

  /**
   * Sets the Instant messaging address for the User.
   *
   * @param ims The Instant messaging address for the User.
   * @return this resource instance.
   */
  public UserResource setIms(final Collection<Entry<String>> ims)
  {
    setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "ims", Entry.STRINGS_RESOLVER, ims);
    return this;
  }

  /**
   * Retrieves the URL of a photo of the User.
   *
   * @return The URL of a photo of the User or <code>null</code> if
   * it is not specified.
   */
  public Collection<Entry<String>> getPhotos()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "photos", Entry.STRINGS_RESOLVER);
  }

  /**
   * Sets the URL of a photo of the User.
   *
   * @param photos The URL of a photo of the User.
   * @return this resource instance.
   */
  public UserResource setPhotos(final Collection<Entry<String>> photos)
  {
    setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "photos", Entry.STRINGS_RESOLVER, photos);
    return this;
  }

  /**
   * Retrieves the physical mailing address for this User.
   *
   * @return The physical mailing address for this User or <code>null</code> if
   * it is not specified.
   */
  public Collection<Address> getAddresses()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "addresses", Address.ADDRESS_RESOLVER);
  }

  /**
   * Sets the physical mailing address for this User.
   *
   * @param addresses The physical mailing address for this User.
   * @return this resource instance.
   */
  public UserResource setAddresses(final Collection<Address> addresses)
  {
    setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "addresses", Address.ADDRESS_RESOLVER, addresses);
    return this;
  }

  /**
   * Retrieves the list of groups that the user belongs to.
   *
   * @return The list of groups that the user belongs to or <code>null</code> if
   * it is not specified.
   */
  public Collection<Entry<String>> getGroups()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "groups", Entry.STRINGS_RESOLVER);
  }

  /**
   * Sets the list of groups that the user belongs to.
   *
   * @param groups The list of groups that the user belongs to.
   * @return this resource instance.
   */
  public UserResource setGroups(final Collection<Entry<String>> groups)
  {
    setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "groups", Entry.STRINGS_RESOLVER, groups);
    return this;
  }

  /**
   * Retrieves the list of entitlements for the User that represent a thing
   * the User has. That is, an entitlement is an additional right to a thing,
   * object or service.
   *
   * @return The list of entitlements for the User or <code>null</code> if
   * it is not specified.
   */
  public Collection<Entry<String>> getEntitlements()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "entitlements", Entry.STRINGS_RESOLVER);
  }

  /**
   * Sets the list of entitlements for the User.
   *
   * @param entitlements The list of entitlements for the User
   * @return this resource instance.
   */
  public UserResource setEntitlements(
      final Collection<Entry<String>> entitlements)
  {
    setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "entitlements", Entry.STRINGS_RESOLVER, entitlements);
    return this;
  }

  /**
   * Retrieves the list of roles for the User that collectively represent who
   * the User is; e.g., 'Student', "Faculty".
   *
   * @return The list of roles for the User or <code>null</code> if
   * it is not specified.
   */
  public Collection<Entry<String>> getRoles()
  {
    return getPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "roles", Entry.STRINGS_RESOLVER);
  }

  /**
   * Sets the list of roles for the User.
   *
   * @param roles The list of roles for the User.
   * @return this resource instance.
   */
  public UserResource setRoles(final Collection<Entry<String>> roles)
  {
    setPluralAttributeValue(SCIMConstants.SCHEMA_URI_CORE,
        "roles", Entry.STRINGS_RESOLVER, roles);
    return this;
  }
}
