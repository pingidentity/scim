/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim.schema.Address;
import com.unboundid.scim.schema.Name;
import com.unboundid.scim.schema.PluralAttribute;
import com.unboundid.scim.schema.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;



/**
 * This class provides a number of static utility functions for converting
 * Simple Cloud Identity Management (SCIM) objects to and from LDAP SDK objects.
 * This mapping should be made pluggable.
 */
public final class LDAPUtil
{
  /**
   * Map SCIM user attributes into a collection of LDAP attributes conforming
   * to the inetOrgPerson object class (defined in RFC 2798).
   *
   * @param user  The SCIM user whose attributes are to be mapped. Not all
   *              attributes and values have a mapping.
   *
   * @return  A collection of inetOrgPerson attributes.
   */
  public static Collection<Attribute> inetOrgPersonAttributesFromUser(
      final User user)
  {
    // Not yet implemented.
    return new ArrayList<Attribute>();
  }



  /**
   * Add an address to a SCIM user object.
   *
   * @param user     The SCIM user.
   * @param address  The address to be added.
   */
  private static void addAddress(final User user, final Address address)
  {
    User.Addresses addresses = user.getAddresses();
    if (addresses == null)
    {
      addresses = new User.Addresses();
      user.setAddresses(addresses);
    }

    addresses.getAddress().add(address);
  }



  /**
   * Add an email to a SCIM user object.
   *
   * @param user   The SCIM user.
   * @param email  The email to be added.
   */
  private static void addEmail(final User user, final PluralAttribute email)
  {
    User.Emails emails = user.getEmails();
    if (emails == null)
    {
      emails = new User.Emails();
      user.setEmails(emails);
    }

    emails.getEmail().add(email);
  }



  /**
   * Add a phone number to a SCIM user object.
   *
   * @param user         The SCIM user.
   * @param phoneNumber  The phone number to be added.
   */
  private static void addPhoneNumber(final User user,
                                     final PluralAttribute phoneNumber)
  {
    User.PhoneNumbers phoneNumbers = user.getPhoneNumbers();
    if (phoneNumbers == null)
    {
      phoneNumbers = new User.PhoneNumbers();
      user.setPhoneNumbers(phoneNumbers);
    }

    phoneNumbers.getPhoneNumber().add(phoneNumber);
  }



  /**
   * Map an LDAP entry conforming to the inetOrgPerson object class (defined
   * in RFC 2798) into a SCIM user.
   *
   * @param entry           The LDAP inetOrgPerson entry whose attributes are
   *                        to be mapped. Not all attributes and values have a
   *                        mapping.
   * @param scimAttributes  The SCIM attributes that have been requested, or
   *                        empty if all available attributes are requested.
   *
   * @return  A SCIM user.
   */
  public static User userFromInetOrgPersonEntry(
      final Entry entry, final String ... scimAttributes)
  {
    final User user = new User();

    Set<String> attrSet = new HashSet<String>();
    for (final String a : scimAttributes)
    {
      attrSet.add(a.toLowerCase());
    }

    if (attrSet.isEmpty() || attrSet.contains("id"))
    {
      user.setId(entry.getAttributeValue("entryUUID"));
    }

    if (attrSet.isEmpty() || attrSet.contains("username"))
    {
      user.setUserName(entry.getAttributeValue("uid"));
    }

    if (attrSet.isEmpty() || attrSet.contains("name"))
    {
      final Name name = new Name();
      name.setFamilyName(entry.getAttributeValue("sn"));
      name.setFormatted(entry.getAttributeValue("cn"));
      name.setGivenName(entry.getAttributeValue("givenName"));
      user.setName(name);
    }

    if (attrSet.isEmpty() || attrSet.contains("addresses"))
    {
      // Map just the first value of each inetOrgPerson address-related
      // attribute as a work, non-primary address.
      if (entry.hasAttribute("postalAddress") || entry.hasAttribute("street"))
      {
        final String formattedValue =
            entry.getAttributeValue("postalAddress").replaceAll("$", "\n");
        final Address address = new Address();
        address.setCountry(entry.getAttributeValue("c"));
        address.setFormatted(formattedValue);
        address.setLocality(entry.getAttributeValue("l"));
        address.setPostalCode(entry.getAttributeValue("postalCode"));
        address.setRegion(entry.getAttributeValue("st"));
        address.setStreetAddress(entry.getAttributeValue("street"));
        address.setType("work");
        addAddress(user, address);
      }

      // Map each value of homePostalAddress as a home, non-primary address.
      if (entry.hasAttribute("homePostalAddress"))
      {
        for (final String value : entry.getAttributeValues("homePostalAddress"))
        {
          final String formattedValue = value.replaceAll("$", "\n");
          final Address address = new Address();
          address.setType("home");
          address.setFormatted(formattedValue);
          addAddress(user, address);
        }
      }
    }

    if (attrSet.isEmpty() || attrSet.contains("emails"))
    {
      // Map each value of mail as a work, non-primary email.
      if (entry.hasAttribute("mail"))
      {
        for (final String value : entry.getAttributeValues("mail"))
        {
          final PluralAttribute email = new PluralAttribute();
          email.setType("work");
          email.setValue(value);
          addEmail(user, email);
        }
      }
    }

    if (attrSet.isEmpty() || attrSet.contains("phonenumbers"))
    {
      // Map each value of telephoneNumber as a work, non-primary number.
      if (entry.hasAttribute("telephoneNumber"))
      {
        for (final String value : entry.getAttributeValues("telephoneNumber"))
        {
          final PluralAttribute phoneNumber = new PluralAttribute();
          phoneNumber.setType("work");
          phoneNumber.setValue(value);
          addPhoneNumber(user, phoneNumber);
        }
      }

      // Map each value of facsimileTelephoneNumber as a fax, non-primary
      // number.
      if (entry.hasAttribute("facsimileTelephoneNumber"))
      {
        for (final String value :
            entry.getAttributeValues("facsimileTelephoneNumber"))
        {
          final PluralAttribute phoneNumber = new PluralAttribute();
          phoneNumber.setType("fax");
          phoneNumber.setValue(value);
          addPhoneNumber(user, phoneNumber);
        }
      }

      // Map each value of homePhone as home, non-primary number.
      if (entry.hasAttribute("homePhone"))
      {
        for (final String value : entry.getAttributeValues("homePhone"))
        {
          final PluralAttribute phoneNumber = new PluralAttribute();
          phoneNumber.setType("home");
          phoneNumber.setValue(value);
          addPhoneNumber(user, phoneNumber);
        }
      }
    }

    if (attrSet.isEmpty() || attrSet.contains("department"))
    {
      user.setDepartment(entry.getAttributeValue("departmentNumber"));
    }

    if (attrSet.isEmpty() || attrSet.contains("displayname"))
    {
      user.setDisplayName(entry.getAttributeValue("displayName"));
    }

    if (attrSet.isEmpty() || attrSet.contains("employeenumber"))
    {
      user.setEmployeeNumber(entry.getAttributeValue("employeeNumber"));
    }

    if (attrSet.isEmpty() || attrSet.contains("organization"))
    {
      user.setOrganization(entry.getAttributeValue("o"));
    }

    if (attrSet.isEmpty() || attrSet.contains("preferredlanguage"))
    {
      user.setPreferredLanguage(entry.getAttributeValue("preferredLanguage"));
    }

    if (attrSet.isEmpty() || attrSet.contains("title"))
    {
      user.setTitle(entry.getAttributeValue("title"));
    }


    // employeeType -> userType seems wrong
    // manager -> manager is not possible because in LDAP it is a DN
    // labeledURI -> profileURL may be reasonable

    return user;
  }
}
