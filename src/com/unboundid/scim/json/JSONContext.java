/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.json;

import com.unboundid.scim.schema.Address;
import com.unboundid.scim.schema.Error;
import com.unboundid.scim.schema.Group;
import com.unboundid.scim.schema.Meta;
import com.unboundid.scim.schema.Name;
import com.unboundid.scim.schema.PluralAttribute;
import com.unboundid.scim.schema.Resource;
import com.unboundid.scim.schema.Response;
import com.unboundid.scim.schema.User;
import com.unboundid.scim.sdk.SCIMConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONWriter;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.Writer;



/**
 * This class provides methods to read and write Simple Cloud Identity
 * Management (SCIM) objects in JSON format. This class and its methods are
 * required to be thread-safe.
 */
public class JSONContext
{

  /**
   * Create a new JSON context.
   */
  public JSONContext()
  {
  }


  /**
   * Writes a SCIM resource object to its JSON representation.
   *
   * @param writer    The writer to which the JSON representation will be
   *                  written.
   * @param resource  The SCIM resource to be written.
   * @throws IOException If an error occurs while writing the object.
   */
  public void writeResource(final Writer writer, final Resource resource)
    throws IOException
  {
    try
    {
      writeResource(new JSONWriter(writer), resource);
    }
    catch (Exception e)
    {
      throw new IOException("Error writing JSON to a character stream", e);
    }
  }



  /**
   * Reads a SCIM response from a string containing the response's JSON
   * representation.
   *
   * @param jsonString    The string from which the JSON representation will be
   *                      read.
   * @param resourceName  Indicates the name of resources expected in the
   *                      response.
   *
   * @return  The SCIM response that was read.
   *
   * @throws IOException  If an error occurs while reading the response.
   */
  public Response readResponse(final String jsonString,
                               final String resourceName)
      throws IOException
  {
    try
    {
      return readResponse(new JSONTokener(jsonString), resourceName);
    }
    catch (Exception e)
    {
      throw new IOException("Error reading a response from a JSON string", e);
    }
  }



  /**
   * Writes a SCIM resource object to a JSON writer.
   *
   * @param jsonWriter  The JSON writer to which the user will be written.
   * @param resource    The resource to be written.
   *
   * @throws JSONException   If an error occurs while writing the resource.
   */
  private void writeResource(final JSONWriter jsonWriter,
                             final Resource resource)
      throws JSONException
  {
    jsonWriter.object();

    // Write out the schemas for the resource.
    jsonWriter.key("schemas");
    jsonWriter.array();
    jsonWriter.value(SCIMConstants.SCHEMA_URI_CORE);
    jsonWriter.endArray();

    final String id = resource.getId();
    if (id != null)
    {
      jsonWriter.key("id");
      jsonWriter.value(id);
    }

    final Meta meta = resource.getMeta();
    if (meta != null)
    {
      jsonWriter.key("meta");
      jsonWriter.object();

      final XMLGregorianCalendar lastModified = meta.getLastModified();
      if (lastModified != null)
      {
        jsonWriter.key("lastModified");
        jsonWriter.value(lastModified.toXMLFormat());
      }

      final XMLGregorianCalendar created = meta.getCreated();
      if (created != null)
      {
        jsonWriter.key("created");
        jsonWriter.value(created.toXMLFormat());
      }

      final String location = meta.getLocation();
      if (location != null)
      {
        jsonWriter.key("location");
        jsonWriter.value(location);
      }

      final String version = meta.getVersion();
      if (version != null)
      {
        jsonWriter.key("version");
        jsonWriter.value(version);
      }

      jsonWriter.endObject();
    }

    if (resource instanceof User)
    {
      writeUser(jsonWriter, (User) resource);
    }
    else if (resource instanceof Group)
    {
      writeGroup(jsonWriter, (Group) resource);
    }
    else
    {
      throw new IllegalArgumentException("Cannot write resources of class " +
                                         resource.getClass().getName());
    }

    jsonWriter.endObject();
  }



  /**
   * Writes attributes from a SCIM user object to a JSON writer.
   *
   * @param jsonWriter  The JSON writer to which the attributes will be written.
   * @param user        The user whose attributes are to be written.
   *
   * @throws JSONException   If an error occurs while writing the attributes.
   */
  private void writeUser(final JSONWriter jsonWriter, final User user)
      throws JSONException
  {
    final String userName = user.getUserName();
    if (userName != null)
    {
      jsonWriter.key("userName");
      jsonWriter.value(userName);
    }

    final String externalId = user.getUserName();
    if (externalId != null)
    {
      jsonWriter.key("externalId");
      jsonWriter.value(externalId);
    }

    final Name name = user.getName();
    if (name != null)
    {
      jsonWriter.key("name");
      jsonWriter.object();

      final String formatted = name.getFormatted();
      if (formatted != null)
      {
        jsonWriter.key("formatted");
        jsonWriter.value(formatted);
      }

      final String familyName = name.getFamilyName();
      if (familyName != null)
      {
        jsonWriter.key("familyName");
        jsonWriter.value(familyName);
      }

      final String givenName = name.getGivenName();
      if (givenName != null)
      {
        jsonWriter.key("givenName");
        jsonWriter.value(givenName);
      }

      final String middleName = name.getMiddleName();
      if (middleName != null)
      {
        jsonWriter.key("middleName");
        jsonWriter.value(middleName);
      }

      final String honorificPrefix = name.getHonorificPrefix();
      if (honorificPrefix != null)
      {
        jsonWriter.key("honorificPrefix");
        jsonWriter.value(honorificPrefix);
      }

      final String honorificSuffix = name.getHonorificSuffix();
      if (honorificSuffix != null)
      {
        jsonWriter.key("honorificSuffix");
        jsonWriter.value(honorificSuffix);
      }

      jsonWriter.endObject();
    }

    final String displayName = user.getDisplayName();
    if (displayName != null)
    {
      jsonWriter.key("displayName");
      jsonWriter.value(displayName);
    }

    final String nickName = user.getNickName();
    if (nickName != null)
    {
      jsonWriter.key("nickName");
      jsonWriter.value(nickName);
    }

    final String profileUrl = user.getProfileUrl();
    if (profileUrl != null)
    {
      jsonWriter.key("profileUrl");
      jsonWriter.value(profileUrl);
    }

    final String title = user.getTitle();
    if (title != null)
    {
      jsonWriter.key("title");
      jsonWriter.value(title);
    }

    final String userType = user.getUserType();
    if (userType != null)
    {
      jsonWriter.key("userType");
      jsonWriter.value(userType);
    }

    final String preferredLanguage = user.getPreferredLanguage();
    if (preferredLanguage != null)
    {
      jsonWriter.key("preferredLanguage");
      jsonWriter.value(preferredLanguage);
    }

    final String locale = user.getLocale();
    if (locale != null)
    {
      jsonWriter.key("locale");
      jsonWriter.value(locale);
    }

    final String timezone = user.getTimezone();
    if (timezone != null)
    {
      jsonWriter.key("timezone");
      jsonWriter.value(timezone);
    }

    final User.Emails emails = user.getEmails();
    if (emails != null && !emails.getEmail().isEmpty())
    {
      jsonWriter.key("emails");
      jsonWriter.array();

      for (final PluralAttribute plural : emails.getEmail())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }

    final User.PhoneNumbers phoneNumbers = user.getPhoneNumbers();
    if (phoneNumbers != null && !phoneNumbers.getPhoneNumber().isEmpty())
    {
      jsonWriter.key("phoneNumbers");
      jsonWriter.array();

      for (final PluralAttribute plural : phoneNumbers.getPhoneNumber())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }

    final User.Ims ims = user.getIms();
    if (ims != null && !ims.getIm().isEmpty())
    {
      jsonWriter.key("ims");
      jsonWriter.array();

      for (final PluralAttribute plural : ims.getIm())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }

    final User.Photos photos = user.getPhotos();
    if (photos != null && !photos.getPhoto().isEmpty())
    {
      jsonWriter.key("photos");
      jsonWriter.array();

      for (final PluralAttribute plural : photos.getPhoto())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }

    final User.Addresses addresses = user.getAddresses();
    if (addresses != null && !addresses.getAddress().isEmpty())
    {
      jsonWriter.key("addresses");
      jsonWriter.array();

      for (final Address address : addresses.getAddress())
      {
        jsonWriter.object();

        final String formatted = address.getFormatted();
        if (formatted != null)
        {
          jsonWriter.key("formatted");
          jsonWriter.value(formatted);
        }

        final String streetAddress = address.getStreetAddress();
        if (streetAddress != null)
        {
          jsonWriter.key("streetAddress");
          jsonWriter.value(streetAddress);
        }

        final String locality = address.getLocality();
        if (locality != null)
        {
          jsonWriter.key("locality");
          jsonWriter.value(locality);
        }

        final String region = address.getRegion();
        if (region != null)
        {
          jsonWriter.key("region");
          jsonWriter.value(region);
        }

        final String postalCode = address.getPostalCode();
        if (postalCode != null)
        {
          jsonWriter.key("postalCode");
          jsonWriter.value(postalCode);
        }

        final String country = address.getCountry();
        if (country != null)
        {
          jsonWriter.key("country");
          jsonWriter.value(country);
        }

        if (address.isPrimary() != null && address.isPrimary())
        {
          jsonWriter.key("primary");
          jsonWriter.value(true);
        }

        final String type = address.getType();
        if (type != null)
        {
          jsonWriter.key("type");
          jsonWriter.value(type);
        }

        jsonWriter.endObject();
      }

      jsonWriter.endArray();
    }

    final User.Groups groups = user.getGroups();
    if (groups != null && !groups.getGroup().isEmpty())
    {
      jsonWriter.key("groups");
      jsonWriter.array();

      for (final PluralAttribute plural : groups.getGroup())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }

    final User.Entitlements entitlements = user.getEntitlements();
    if (entitlements != null && !entitlements.getEntitlement().isEmpty())
    {
      jsonWriter.key("entitlements");
      jsonWriter.array();

      for (final PluralAttribute plural : entitlements.getEntitlement())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }

    final User.Roles roles = user.getRoles();
    if (roles != null && !roles.getRole().isEmpty())
    {
      jsonWriter.key("roles");
      jsonWriter.array();

      for (final PluralAttribute plural : roles.getRole())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }
  }



  /**
   * Writes attributes from a SCIM group object to a JSON writer.
   *
   * @param jsonWriter  The JSON writer to which the attributes will be written.
   * @param group       The group whose attributes are to be written.
   *
   * @throws JSONException   If an error occurs while writing the attributes.
   */
  private void writeGroup(final JSONWriter jsonWriter, final Group group)
      throws JSONException
  {
    final String displayName = group.getDisplayName();
    if (displayName != null)
    {
      jsonWriter.key("displayName");
      jsonWriter.value(displayName);
    }

    final Group.Members members = group.getMembers();
    if (members != null && !members.getMember().isEmpty())
    {
      jsonWriter.key("members");
      jsonWriter.array();

      for (final PluralAttribute plural : members.getMember())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }
  }



  /**
   * Reads a SCIM resource from a JSON object.
   *
   * @param jsonObject  The JSON object from which the resource will be read.
   * @param resourceName The name of the resource to be read.
   *
   * @return  The SCIM resource that was read.
   *
   * @throws Exception  If an error occurs while reading the user.
   */
  private Resource readResource(final JSONObject jsonObject,
                                final String resourceName)
      throws Exception
  {
    final Resource resource;
    if (resourceName.equals(SCIMConstants.RESOURCE_NAME_USER))
    {
      resource = readUser(jsonObject);
    }
    else if (resourceName.equals(SCIMConstants.RESOURCE_NAME_GROUP))
    {
      resource = readGroup(jsonObject);
    }
    else
    {
      throw new IllegalArgumentException(
          "Cannot read '" + resourceName + "' resources");
    }

    final DatatypeFactory datatypeFactory;
    try
    {
      datatypeFactory = DatatypeFactory.newInstance();
    }
    catch (DatatypeConfigurationException e)
    {
      throw new RuntimeException("Unable to create a DatatypeFactory", e);
    }

    resource.setId(jsonObject.optString("id", null));

    final JSONObject metaObject = jsonObject.optJSONObject("meta");
    if (metaObject != null)
    {
      final Meta meta = new Meta();

      final String lastModified = metaObject.optString("lastModified", null);
      if (lastModified != null)
      {
        meta.setLastModified(
            datatypeFactory.newXMLGregorianCalendar(lastModified));
      }

      final String created = metaObject.optString("created", null);
      if (created != null)
      {
        meta.setCreated(
            datatypeFactory.newXMLGregorianCalendar(created));
      }

      meta.setLocation(metaObject.optString("location", null));
      meta.setVersion(metaObject.optString("version", null));

      resource.setMeta(meta);
    }

    return resource;
  }



  /**
   * Reads a SCIM user from a JSON object.
   *
   * @param jsonObject  The JSON object from which the user will be read.
   *
   * @return  The SCIM user that was read.
   *
   * @throws Exception  If an error occurs while reading the user.
   */
  private User readUser(final JSONObject jsonObject)
      throws Exception
  {
    final User user = new User();

    user.setUserName(jsonObject.optString("userName", null));
    user.setExternalId(jsonObject.optString("externalId", null));

    final JSONObject nameObject = jsonObject.optJSONObject("name");
    if (nameObject != null)
    {
      final Name name = new Name();

      name.setFormatted(nameObject.optString("formatted", null));
      name.setFamilyName(nameObject.optString("familyName", null));
      name.setGivenName(nameObject.optString("givenName", null));
      name.setMiddleName(nameObject.optString("middleName", null));
      name.setHonorificPrefix(nameObject.optString("honorificPrefix", null));
      name.setHonorificSuffix(nameObject.optString("honorificSuffix", null));

      user.setName(name);
    }

    user.setDisplayName(jsonObject.optString("displayName", null));
    user.setNickName(jsonObject.optString("nickName", null));
    user.setProfileUrl(jsonObject.optString("profileUrl", null));
    user.setTitle(jsonObject.optString("title", null));
    user.setUserType(jsonObject.optString("userType", null));
    user.setPreferredLanguage(jsonObject.optString("preferredLanguage", null));
    user.setLocale(jsonObject.optString("locale", null));
    user.setTimezone(jsonObject.optString("timezone", null));

    final JSONArray emailsArray = jsonObject.optJSONArray("emails");
    if (emailsArray != null)
    {
      final User.Emails emails = new User.Emails();

      for (int i = 0; i < emailsArray.length(); i++)
      {
        final JSONObject emailObject = emailsArray.getJSONObject(i);
        emails.getEmail().add(toPlural(emailObject));
      }

      user.setEmails(emails);
    }

    final JSONArray phoneNumbersArray = jsonObject.optJSONArray("phoneNumbers");
    if (phoneNumbersArray != null)
    {
      final User.PhoneNumbers phoneNumbers = new User.PhoneNumbers();

      for (int i = 0; i < phoneNumbersArray.length(); i++)
      {
        final JSONObject o = phoneNumbersArray.getJSONObject(i);
        phoneNumbers.getPhoneNumber().add(toPlural(o));
      }

      user.setPhoneNumbers(phoneNumbers);
    }

    final JSONArray imsArray = jsonObject.optJSONArray("ims");
    if (imsArray != null)
    {
      final User.Ims ims = new User.Ims();

      for (int i = 0; i < imsArray.length(); i++)
      {
        final JSONObject o = imsArray.getJSONObject(i);
        ims.getIm().add(toPlural(o));
      }

      user.setIms(ims);
    }

    final JSONArray photosArray = jsonObject.optJSONArray("photos");
    if (photosArray != null)
    {
      final User.Photos photos = new User.Photos();

      for (int i = 0; i < photosArray.length(); i++)
      {
        final JSONObject o = photosArray.getJSONObject(i);
        photos.getPhoto().add(toPlural(o));
      }

      user.setPhotos(photos);
    }

    final JSONArray addressesArray = jsonObject.optJSONArray("addresses");
    if (addressesArray != null)
    {
      final User.Addresses addresses = new User.Addresses();

      for (int i = 0; i < addressesArray.length(); i++)
      {
        final JSONObject o = addressesArray.getJSONObject(i);

        final Address address = new Address();

        address.setFormatted(o.optString("formatted", null));
        address.setStreetAddress(o.optString("streetAddress", null));
        address.setLocality(o.optString("locality", null));
        address.setRegion(o.optString("region", null));
        address.setPostalCode(o.optString("postalCode", null));
        address.setCountry(o.optString("country", null));
        address.setPrimary(o.optBoolean("primary", false));
        address.setType(o.optString("type", null));

        addresses.getAddress().add(address);
      }

      user.setAddresses(addresses);
    }

    final JSONArray groupsArray = jsonObject.optJSONArray("groups");
    if (groupsArray != null)
    {
      final User.Groups groups = new User.Groups();

      for (int i = 0; i < groupsArray.length(); i++)
      {
        final JSONObject o = groupsArray.getJSONObject(i);
        groups.getGroup().add(toPlural(o));
      }

      user.setGroups(groups);
    }

    final JSONArray entitlementsArray = jsonObject.optJSONArray("entitlements");
    if (entitlementsArray != null)
    {
      final User.Entitlements entitlements = new User.Entitlements();

      for (int i = 0; i < entitlementsArray.length(); i++)
      {
        final JSONObject o = entitlementsArray.getJSONObject(i);
        entitlements.getEntitlement().add(toPlural(o));
      }

      user.setEntitlements(entitlements);
    }

    final JSONArray rolesArray = jsonObject.optJSONArray("roles");
    if (rolesArray != null)
    {
      final User.Roles roles = new User.Roles();

      for (int i = 0; i < rolesArray.length(); i++)
      {
        final JSONObject o = rolesArray.getJSONObject(i);
        roles.getRole().add(toPlural(o));
      }

      user.setRoles(roles);
    }

    return user;
  }



  /**
   * Reads a SCIM Group from a JSON object.
   *
   * @param jsonObject  The JSON object from which the group will be read.
   *
   * @return  The SCIM group that was read.
   *
   * @throws Exception  If an error occurs while reading the user.
   */
  private Group readGroup(final JSONObject jsonObject)
      throws Exception
  {
    final Group group = new Group();

    group.setDisplayName(jsonObject.optString("displayName", null));

    final JSONArray membersArray = jsonObject.optJSONArray("members");
    if (membersArray != null)
    {
      final Group.Members members = new Group.Members();

      for (int i = 0; i < membersArray.length(); i++)
      {
        final JSONObject memberObject = membersArray.getJSONObject(i);
        members.getMember().add(toPlural(memberObject));
      }

      group.setMembers(members);
    }

    return group;
  }



  /**
   * Reads a SCIM response from a JSON tokener.
   *
   * @param tokener       The tokener from which the response will be read.
   * @param resourceName  Indicates the name of resources expected in the
   *                      response.
   *
   * @return  The SCIM response that was read.
   *
   * @throws Exception  If an error occurs while reading the response.
   */
  private Response readResponse(final JSONTokener tokener,
                                final String resourceName)
      throws Exception
  {
    final JSONObject jsonObject = new JSONObject(tokener);

    final Response response = new Response();
    if (jsonObject.has("Resource"))
    {
      final JSONObject resourceObject = jsonObject.optJSONObject("Resource");
      if (resourceObject != null)
      {
        response.setResource(readResource(resourceObject, resourceName));
      }
    }
    else if (jsonObject.has("Errors"))
    {
      final Response.Errors errors = new Response.Errors();
      final JSONArray errorsArray = jsonObject.getJSONArray("Errors");
      for (int i = 0; i < errorsArray.length(); i++)
      {
        final JSONObject errorObject = errorsArray.getJSONObject(i);

        final Error error = new Error();
        error.setDescription(errorObject.optString("description", null));
        error.setCode(errorObject.optString("code", null));
        error.setUri(errorObject.optString("uri", null));

        errors.getError().add(error);
      }

      response.setErrors(errors);
    }
    else if (jsonObject.has("totalResults"))
    {
      response.setTotalResults(jsonObject.optLong("totalResults"));

      if (jsonObject.has("itemsPerPage"))
      {
        response.setItemsPerPage(jsonObject.optInt("itemsPerPage"));
      }
      if (jsonObject.has("startIndex"))
      {
        response.setStartIndex(jsonObject.optLong("startIndex"));
      }

      final JSONArray resourcesArray = jsonObject.optJSONArray("Resources");
      if (resourcesArray != null)
      {
        final Response.Resources resources = new Response.Resources();
        for (int i = 0; i < resourcesArray.length(); i++)
        {
          final JSONObject resourceObject = resourcesArray.getJSONObject(i);
          resources.getResource().add(
              readResource(resourceObject, resourceName));
        }

        response.setResources(resources);
      }
    }
    else
    {
      // This must be a single resource without a response object.
      response.setResource(readResource(jsonObject, resourceName));
    }

    return response;
  }



  /**
   * Write the provided plural attribute to a JSON writer.
   *
   * @param jsonWriter  The JSON writer to which the user will be written.
   * @param plural      The user to be written.
   *
   * @throws JSONException   If an error occurs while writing the user.
   */
  private static void writePlural(final JSONWriter jsonWriter,
                                  final PluralAttribute plural)
      throws JSONException
  {
    jsonWriter.object();

    final String value = plural.getValue();
    if (value != null)
    {
      jsonWriter.key("value");
      jsonWriter.value(value);
    }

    if (plural.isPrimary() != null && plural.isPrimary())
    {
      jsonWriter.key("primary");
      jsonWriter.value(true);
    }

    final String type = plural.getType();
    if (type != null)
    {
      jsonWriter.key("type");
      jsonWriter.value(type);
    }

    jsonWriter.endObject();
  }



  /**
   * Parse the provided JSON object as a plural attribute value.
   *
   * @param jsonObject  The JSON object representing a plural attribute value.
   *
   * @return  The parsed attribute value.
   */
  private static PluralAttribute toPlural(final JSONObject jsonObject)
  {
    final PluralAttribute pluralAttribute = new PluralAttribute();

    pluralAttribute.setValue(jsonObject.optString("value", null));
    pluralAttribute.setPrimary(jsonObject.optBoolean("primary", false));
    pluralAttribute.setType(jsonObject.optString("type", null));

    return pluralAttribute;
  }
}
