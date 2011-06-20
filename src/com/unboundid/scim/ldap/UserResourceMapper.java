/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.scim.config.AttributeDescriptor;
import com.unboundid.scim.config.ResourceDescriptor;
import com.unboundid.scim.config.ResourceDescriptorManager;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMAttributeValue;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;
import com.unboundid.scim.sdk.SCIMQueryAttributes;

import java.util.ArrayList;
import java.util.List;



/**
 * This class provides a mapping between a User in the SCIM core schema and
 * the LDAP inetOrgPerson object class. The specific attribute mappings are
 * fixed and can not be changed through configuration. This mapping is not able
 * to preserve all information from SCIM to LDAP, nor from LDAP to SCIM.
 *
 * The following attributes are mapped:
 * userName, name, addresses, emails, phoneNumbers, displayName,
 * preferredLanguage, title.
 *
 * These attributes are not mapped: externalId, nickName, profileUrl, userType,
 * locale, utcOffset, ims, photos, memberOf.
 */
public class UserResourceMapper extends ResourceMapper
{
  /**
   * {@inheritDoc}
   */
  @Override
  public void initializeMapper()
  {
    // No implementation required.
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeMapper()
  {
    // No implementation required.
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public List<Attribute> toLDAPAttributes(final SCIMObject scimObject)
  {
    final String coreSchema = SCIMConstants.SCHEMA_URI_CORE;
    final List<Attribute> attributes = new ArrayList<Attribute>();

    final SCIMAttribute userName =
        scimObject.getAttribute(coreSchema, "userName");
    if (userName != null)
    {
      attributes.add(
          new Attribute("uid", userName.getSingularValue().getStringValue()));
    }

    final SCIMAttribute name =
        scimObject.getAttribute(coreSchema, "name");
    if (name != null)
    {
      final SCIMAttributeValue value = name.getSingularValue();

      final SCIMAttribute formatted = value.getAttribute("formatted");
      if (formatted != null)
      {
        attributes.add(
            new Attribute("cn", formatted.getSingularValue().getStringValue()));
      }

      final SCIMAttribute familyName = value.getAttribute("familyName");
      if (familyName != null)
      {
        attributes.add(
            new Attribute("sn",
                          familyName.getSingularValue().getStringValue()));
      }

      final SCIMAttribute givenName = value.getAttribute("givenName");
      if (givenName != null)
      {
        attributes.add(
            new Attribute("givenName",
                          givenName.getSingularValue().getStringValue()));
      }
    }

    final SCIMAttribute addresses =
        scimObject.getAttribute(coreSchema, "addresses");
    if (addresses != null)
    {
      final List<String> homeAddressValues = new ArrayList<String>();
      boolean foundWorkAddress = false;
      for (SCIMAttributeValue v : addresses.getPluralValues())
      {
        final SCIMAttribute address = v.getAttribute("address");
        if (address != null)
        {
          v = address.getSingularValue();
        }

        final SCIMAttribute typeAttr = v.getAttribute("type");
        if (typeAttr != null)
        {
          final String type = typeAttr.getSingularValue().getStringValue();
          if (type.equalsIgnoreCase("work"))
          {
            if (!foundWorkAddress)
            {
              foundWorkAddress = true;

              final SCIMAttribute formatted = v.getAttribute("formatted");
              if (formatted != null)
              {
                final String s = formatted.getSingularValue().getStringValue();
                attributes.add(
                    new Attribute("postalAddress",
                                  s.replaceAll("\n", "\\$")));
              }

              final SCIMAttribute country = v.getAttribute("country");
              if (country != null)
              {
                attributes.add(
                    new Attribute("c",
                                  country.getSingularValue().getStringValue()));
              }

              final SCIMAttribute locality = v.getAttribute("locality");
              if (locality != null)
              {
                attributes.add(
                    new Attribute(
                        "l", locality.getSingularValue().getStringValue()));
              }

              final SCIMAttribute postalCode = v.getAttribute("postalCode");
              if (postalCode != null)
              {
                attributes.add(
                    new Attribute(
                        "postalCode",
                        postalCode.getSingularValue().getStringValue()));
              }

              final SCIMAttribute region = v.getAttribute("region");
              if (region != null)
              {
                attributes.add(
                    new Attribute(
                        "st", region.getSingularValue().getStringValue()));
              }

              final SCIMAttribute streetAddress =
                  v.getAttribute("streetAddress");
              if (streetAddress != null)
              {
                attributes.add(
                    new Attribute(
                        "street",
                        streetAddress.getSingularValue().getStringValue()));
              }
            }
          }
          else if (type.equalsIgnoreCase("home"))
          {
            final SCIMAttribute formatted = v.getAttribute("formatted");
            if (formatted != null)
            {
              final String s = formatted.getSingularValue().getStringValue();
              homeAddressValues.add(s.replaceAll("\n", "\\$"));
              attributes.add(
                  new Attribute("homePostalAddress",
                                s.replaceAll("\n", "\\$")));
            }
          }
        }
      }

      if (!homeAddressValues.isEmpty())
      {
        attributes.add(new Attribute("homePostalAddress", homeAddressValues));
      }
    }

    final SCIMAttribute emails = scimObject.getAttribute(coreSchema, "emails");
    if (emails != null)
    {
      final List<String> values = new ArrayList<String>();

      for (SCIMAttributeValue v : emails.getPluralValues())
      {
        final SCIMAttribute email = v.getAttribute("email");
        if (email != null)
        {
          v = email.getSingularValue();
        }

        final SCIMAttribute typeAttr = v.getAttribute("type");
        if (typeAttr != null)
        {
          final String type = typeAttr.getSingularValue().getStringValue();
          final SCIMAttribute number = v.getAttribute("value");

          if (type.equalsIgnoreCase("work"))
          {
            values.add(number.getSingularValue().getStringValue());
          }
        }
      }

      if (!values.isEmpty())
      {
        attributes.add(new Attribute("mail", values));
      }
    }

    final SCIMAttribute phoneNumbers =
        scimObject.getAttribute(coreSchema, "phoneNumbers");
    if (phoneNumbers != null)
    {
      final List<String> workNumbers = new ArrayList<String>();
      final List<String> homeNumbers = new ArrayList<String>();
      final List<String> faxNumbers  = new ArrayList<String>();

      for (SCIMAttributeValue v : phoneNumbers.getPluralValues())
      {
        final SCIMAttribute phoneNumber = v.getAttribute("phoneNumber");
        if (phoneNumber != null)
        {
          v = phoneNumber.getSingularValue();
        }

        final SCIMAttribute typeAttr = v.getAttribute("type");
        if (typeAttr != null)
        {
          final String type = typeAttr.getSingularValue().getStringValue();
          final SCIMAttribute number = v.getAttribute("value");

          if (type.equalsIgnoreCase("work"))
          {
            workNumbers.add(number.getSingularValue().getStringValue());
          }
          else if (type.equalsIgnoreCase("home"))
          {
            homeNumbers.add(number.getSingularValue().getStringValue());
          }
          else if (type.equalsIgnoreCase("fax"))
          {
            faxNumbers.add(number.getSingularValue().getStringValue());
          }
        }
      }

      if (!workNumbers.isEmpty())
      {
        attributes.add(new Attribute("telephoneNumber", workNumbers));
      }

      if (!homeNumbers.isEmpty())
      {
        attributes.add(new Attribute("homePhone", homeNumbers));
      }

      if (!faxNumbers.isEmpty())
      {
        attributes.add(new Attribute("facsimileTelephoneNumber", faxNumbers));
      }
    }

    final SCIMAttribute displayName =
        scimObject.getAttribute(coreSchema, "userName");
    if (displayName != null)
    {
      attributes.add(
          new Attribute("displayName",
                        displayName.getSingularValue().getStringValue()));
    }

    final SCIMAttribute preferredLanguage =
        scimObject.getAttribute(coreSchema, "preferredLanguage");
    if (preferredLanguage != null)
    {
      attributes.add(
          new Attribute("preferredLanguage",
                        preferredLanguage.getSingularValue().getStringValue()));
    }

    final SCIMAttribute title =
        scimObject.getAttribute(coreSchema, "title");
    if (title != null)
    {
      attributes.add(
          new Attribute("title",
                        title.getSingularValue().getStringValue()));
    }

    return attributes;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public List<SCIMAttribute> toSCIMAttributes(
      final String resourceName,
      final Entry entry, final SCIMQueryAttributes queryAttributes)
  {
    final List<SCIMAttribute> attributes = new ArrayList<SCIMAttribute>();

    final ResourceDescriptor resourceDescriptor =
        ResourceDescriptorManager.instance().getResourceDescriptor(
            resourceName);
    if (resourceDescriptor == null)
    {
      return attributes;
    }

    if (queryAttributes.isAttributeRequested("name"))
    {
      final AttributeDescriptor descriptor =
          resourceDescriptor.getAttribute("name");
      final List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>();

      final String cn = entry.getAttributeValue("cn");
      if (cn != null)
      {
        subAttributes.add(
            SCIMAttribute.createSingularAttribute(
                descriptor.getAttribute("formatted"),
                SCIMAttributeValue.createStringValue(cn)));
      }

      final String sn = entry.getAttributeValue("sn");
      if (sn != null)
      {
        subAttributes.add(
            SCIMAttribute.createSingularAttribute(
                descriptor.getAttribute("familyName"),
                SCIMAttributeValue.createStringValue(sn)));
      }

      final String givenName = entry.getAttributeValue("givenName");
      if (givenName != null)
      {
        subAttributes.add(
            SCIMAttribute.createSingularAttribute(
                descriptor.getAttribute("givenName"),
                SCIMAttributeValue.createStringValue(givenName)));
      }

      final SCIMAttributeValue value =
          SCIMAttributeValue.createComplexValue(subAttributes);
      final SCIMAttribute name =
          SCIMAttribute.createSingularAttribute(descriptor, value);
      attributes.add(name);
    }

    if (queryAttributes.isAttributeRequested("addresses"))
    {
      final AttributeDescriptor addressesDescriptor =
          resourceDescriptor.getAttribute("addresses");
      final AttributeDescriptor addressDescriptor =
          addressesDescriptor.getAttribute("address");
      final ArrayList<SCIMAttributeValue> addresses =
          new ArrayList<SCIMAttributeValue>();

      // Map just the first value of each inetOrgPerson address-related
      // attribute as a work, non-primary address.
      if (entry.hasAttribute("postalAddress") || entry.hasAttribute("street"))
      {
        List<SCIMAttribute> subAttributes = new ArrayList<SCIMAttribute>();

        final String postalAddress = entry.getAttributeValue("postalAddress");
        if (postalAddress != null)
        {
          final String formatted = postalAddress.replaceAll("\\$", "\n");

          subAttributes.add(
              SCIMAttribute.createSingularAttribute(
                  addressDescriptor.getAttribute("formatted"),
                  SCIMAttributeValue.createStringValue(
                      formatted)));
        }

        final String country = entry.getAttributeValue("c");
        if (country != null)
        {
          subAttributes.add(
              SCIMAttribute.createSingularAttribute(
                  addressDescriptor.getAttribute("country"),
                  SCIMAttributeValue.createStringValue(country)));
        }

        final String locality = entry.getAttributeValue("l");
        if (locality != null)
        {
          subAttributes.add(
              SCIMAttribute.createSingularAttribute(
                  addressDescriptor.getAttribute("locality"),
                  SCIMAttributeValue.createStringValue(locality)));
        }

        final String postalCode = entry.getAttributeValue("postalCode");
        if (postalCode != null)
        {
          subAttributes.add(
              SCIMAttribute.createSingularAttribute(
                  addressDescriptor.getAttribute("postalCode"),
                  SCIMAttributeValue.createStringValue(postalCode)));
        }

        final String region = entry.getAttributeValue("st");
        if (region != null)
        {
          subAttributes.add(
              SCIMAttribute.createSingularAttribute(
                  addressDescriptor.getAttribute("region"),
                  SCIMAttributeValue.createStringValue(region)));
        }

        final String streetAddress = entry.getAttributeValue("street");
        if (streetAddress != null)
        {
          subAttributes.add(
              SCIMAttribute.createSingularAttribute(
                  addressDescriptor.getAttribute("streetAddress"),
                  SCIMAttributeValue.createStringValue(streetAddress)));
        }

        subAttributes.add(
            SCIMAttribute.createSingularAttribute(
                addressDescriptor.getAttribute("type"),
                SCIMAttributeValue.createStringValue("work")));

        addresses.add(
            SCIMAttributeValue.createComplexValue(
                SCIMAttribute.createSingularAttribute(
                    addressDescriptor,
                    SCIMAttributeValue.createComplexValue(subAttributes))));
      }

      // Map each value of homePostalAddress as a home, non-primary address.
      if (entry.hasAttribute("homePostalAddress"))
      {
        for (final String value : entry.getAttributeValues("homePostalAddress"))
        {
          final String formattedValue = value.replaceAll("\\$", "\n");

          addresses.add(
              SCIMAttributeValue.createComplexValue(
                  SCIMAttribute.createSingularAttribute(
                      addressDescriptor,
                      SCIMAttributeValue.createComplexValue(
                          SCIMAttribute.createSingularAttribute(
                              addressDescriptor.getAttribute("formatted"),
                              SCIMAttributeValue.createStringValue(
                                  formattedValue)),
                          SCIMAttribute.createSingularAttribute(
                              addressDescriptor.getAttribute("type"),
                              SCIMAttributeValue.createStringValue("home"))))));
        }
      }

      if (!addresses.isEmpty())
      {
        attributes.add(
            SCIMAttribute.createPluralAttribute(
                addressesDescriptor,
                addresses.toArray(new SCIMAttributeValue[addresses.size()])));
      }
    }

    if (queryAttributes.isAttributeRequested("emails"))
    {
      // Map each value of mail as a work, non-primary email.
      if (entry.hasAttribute("mail"))
      {
        final AttributeDescriptor emailsDescriptor =
            resourceDescriptor.getAttribute("emails");
        final AttributeDescriptor emailDescriptor =
            emailsDescriptor.getAttribute("email");

        final ArrayList<SCIMAttributeValue> emails =
            new ArrayList<SCIMAttributeValue>();

        for (final String value : entry.getAttributeValues("mail"))
        {
          emails.add(
              SCIMAttributeValue.createComplexValue(
                  SCIMAttribute.createSingularAttribute(
                      emailDescriptor,
                      SCIMAttributeValue.createComplexValue(
                          SCIMAttribute.createSingularAttribute(
                              emailDescriptor.getAttribute("value"),
                              SCIMAttributeValue.createStringValue(value)),
                          SCIMAttribute.createSingularAttribute(
                              emailDescriptor.getAttribute("type"),
                              SCIMAttributeValue.createStringValue("work"))))));
        }

        if (!emails.isEmpty())
        {
          attributes.add(
              SCIMAttribute.createPluralAttribute(
                  emailsDescriptor,
                  emails.toArray(new SCIMAttributeValue[emails.size()])));
        }
      }
    }

    if (queryAttributes.isAttributeRequested("phoneNumbers"))
    {
      final AttributeDescriptor phoneNumbersDescriptor =
          resourceDescriptor.getAttribute("phoneNumbers");
      final AttributeDescriptor phoneNumberDescriptor =
          phoneNumbersDescriptor.getAttribute("phoneNumber");
      final ArrayList<SCIMAttributeValue> numbers =
          new ArrayList<SCIMAttributeValue>();

      // Map each value of telephoneNumber as a work, non-primary number.
      if (entry.hasAttribute("telephoneNumber"))
      {
        for (final String value : entry.getAttributeValues("telephoneNumber"))
        {
          numbers.add(
              SCIMAttributeValue.createComplexValue(
                  SCIMAttribute.createSingularAttribute(
                      phoneNumberDescriptor,
                      SCIMAttributeValue.createComplexValue(
                          SCIMAttribute.createSingularAttribute(
                              phoneNumberDescriptor.getAttribute("value"),
                              SCIMAttributeValue.createStringValue(value)),
                          SCIMAttribute.createSingularAttribute(
                              phoneNumberDescriptor.getAttribute("type"),
                              SCIMAttributeValue.createStringValue("work"))))));
        }
      }

      // Map each value of facsimileTelephoneNumber as a fax, non-primary
      // number.
      if (entry.hasAttribute("facsimileTelephoneNumber"))
      {
        for (final String value :
            entry.getAttributeValues("facsimileTelephoneNumber"))
        {
          numbers.add(
              SCIMAttributeValue.createComplexValue(
                  SCIMAttribute.createSingularAttribute(
                      phoneNumberDescriptor,
                      SCIMAttributeValue.createComplexValue(
                          SCIMAttribute.createSingularAttribute(
                              phoneNumberDescriptor.getAttribute("value"),
                              SCIMAttributeValue.createStringValue(value)),
                          SCIMAttribute.createSingularAttribute(
                              phoneNumberDescriptor.getAttribute("type"),
                              SCIMAttributeValue.createStringValue("fax"))))));
        }
      }

      // Map each value of homePhone as home, non-primary number.
      if (entry.hasAttribute("homePhone"))
      {
        for (final String value : entry.getAttributeValues("homePhone"))
        {
          numbers.add(
              SCIMAttributeValue.createComplexValue(
                  SCIMAttribute.createSingularAttribute(
                      phoneNumberDescriptor,
                      SCIMAttributeValue.createComplexValue(
                          SCIMAttribute.createSingularAttribute(
                              phoneNumberDescriptor.getAttribute("value"),
                              SCIMAttributeValue.createStringValue(value)),
                          SCIMAttribute.createSingularAttribute(
                              phoneNumberDescriptor.getAttribute("type"),
                              SCIMAttributeValue.createStringValue("home"))))));
        }
      }

      if (!numbers.isEmpty())
      {
        attributes.add(
            SCIMAttribute.createPluralAttribute(
                phoneNumbersDescriptor,
                numbers.toArray(new SCIMAttributeValue[numbers.size()])));
      }
    }

    if (queryAttributes.isAttributeRequested("displayName"))
    {
      final String displayName = entry.getAttributeValue("displayName");
      if (displayName != null)
      {
        attributes.add(
            SCIMAttribute.createSingularAttribute(
                resourceDescriptor.getAttribute("displayName"),
                SCIMAttributeValue.createStringValue(displayName)));
      }
    }

    if (queryAttributes.isAttributeRequested("preferredLanguage"))
    {
      final String preferredLanguage =
          entry.getAttributeValue("preferredLanguage");
      if (preferredLanguage != null)
      {
        attributes.add(
            SCIMAttribute.createSingularAttribute(
                resourceDescriptor.getAttribute("preferredLanguage"),
                SCIMAttributeValue.createStringValue(preferredLanguage)));
      }
    }

    if (queryAttributes.isAttributeRequested("title"))
    {
      final String title = entry.getAttributeValue("title");
      if (title != null)
      {
        attributes.add(
            SCIMAttribute.createSingularAttribute(
                resourceDescriptor.getAttribute("title"),
                SCIMAttributeValue.createStringValue(title)));
      }
    }

    if (queryAttributes.isAttributeRequested("userName"))
    {
      final String uid = entry.getAttributeValue("uid");
      if (uid != null)
      {
        attributes.add(
            SCIMAttribute.createSingularAttribute(
                resourceDescriptor.getAttribute("userName"),
                SCIMAttributeValue.createStringValue(uid)));
      }
    }

    return attributes;
  }
}
