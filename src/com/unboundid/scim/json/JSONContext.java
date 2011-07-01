/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.json;

import com.unboundid.scim.schema.Address;
import com.unboundid.scim.schema.Error;
import com.unboundid.scim.schema.Meta;
import com.unboundid.scim.schema.Name;
import com.unboundid.scim.schema.PluralAttribute;
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
   * Writes a SCIM user object to its JSON representation.
   *
   * @param writer The writer to which the JSON representation will be written.
   * @param user   The SCIM user to be written.
   * @throws IOException If an error occurs while writing the object.
   */
  public void writeUser(final Writer writer, final User user)
    throws IOException
  {
    try
    {
      writeUser(new JSONWriter(writer), user);
    }
    catch (Exception e)
    {
      throw new IOException("Error writing JSON to a character stream", e);
    }
  }


  /**
   * Reads a SCIM user object from a string containing the user's JSON
   * representation.
   *
   * @param jsonString The string from which the JSON representation will be
   *                   read.
   * @return The SCIM user that was read.
   * @throws IOException If an error occurs while reading the object.
   */
  public User readUser(final String jsonString)
    throws IOException
  {
    try
    {
      return readUser(new JSONTokener(jsonString));
    }
    catch (Exception e)
    {
      throw new IOException("Error reading a user from a JSON string", e);
    }
  }



  /**
   * Reads a SCIM response from a string containing the response's JSON
   * representation.
   *
   * @param jsonString  The string from which the JSON representation will be
   *                    read.
   *
   * @return  The SCIM response that was read.
   *
   * @throws IOException  If an error occurs while reading the response.
   */
  public Response readResponse(final String jsonString)
      throws IOException
  {
    try
    {
      return readResponse(new JSONTokener(jsonString));
    }
    catch (Exception e)
    {
      throw new IOException("Error reading a response from a JSON string", e);
    }
  }



  /**
   * Writes a SCIM user object to a JSON writer.
   *
   * @param jsonWriter  The JSON writer to which the user will be written.
   * @param user        The user to be written.
   *
   * @throws JSONException   If an error occurs while writing the user.
   */
  private void writeUser(final JSONWriter jsonWriter, final User user)
      throws JSONException
  {
    jsonWriter.object();

    // Write out the schemas for the user.
    jsonWriter.key("schemas");
    jsonWriter.array();
    jsonWriter.object();
    jsonWriter.key("uri");
    jsonWriter.value(SCIMConstants.SCHEMA_URI_CORE);
    jsonWriter.endObject();
    jsonWriter.endArray();

    final String id = user.getId();
    if (id != null)
    {
      jsonWriter.key("id");
      jsonWriter.value(id);
    }

    final Meta meta = user.getMeta();
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

    // TODO utcOffset

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

    final User.PhotoUrls photoUrls = user.getPhotoUrls();
    if (photoUrls != null && !photoUrls.getPhotoUrl().isEmpty())
    {
      jsonWriter.key("photoUrls");
      jsonWriter.array();

      for (final PluralAttribute plural : photoUrls.getPhotoUrl())
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

    final User.MemberOf memberOf = user.getMemberOf();
    if (memberOf != null && !memberOf.getGroup().isEmpty())
    {
      jsonWriter.key("memberOf");
      jsonWriter.array();

      for (final PluralAttribute plural : memberOf.getGroup())
      {
        writePlural(jsonWriter, plural);
      }

      jsonWriter.endArray();
    }

    jsonWriter.endObject();
  }



  /**
   * Reads a SCIM user object from a JSON tokener.
   *
   * @param tokener  The tokener from which the object will be read.
   *
   * @return  The SCIM user that was read.
   *
   * @throws Exception  If an error occurs while reading the object.
   */
  private User readUser(final JSONTokener tokener)
      throws Exception
  {
    return readUser(new JSONObject(tokener));
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
    final DatatypeFactory datatypeFactory;
    try
    {
      datatypeFactory = DatatypeFactory.newInstance();
    }
    catch (DatatypeConfigurationException e)
    {
      throw new RuntimeException("Unable to create a DatatypeFactory", e);
    }
    final User user = new User();

    user.setId(jsonObject.optString("id", null));

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

      user.setMeta(meta);
    }

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

    // TODO utcOffset

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

    final JSONArray photoUrlsArray = jsonObject.optJSONArray("photoUrls");
    if (photoUrlsArray != null)
    {
      final User.PhotoUrls photoUrls = new User.PhotoUrls();

      for (int i = 0; i < photoUrlsArray.length(); i++)
      {
        final JSONObject o = photoUrlsArray.getJSONObject(i);
        photoUrls.getPhotoUrl().add(toPlural(o));
      }

      user.setPhotoUrls(photoUrls);
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

    final JSONArray memberOfArray = jsonObject.optJSONArray("memberOf");
    if (memberOfArray != null)
    {
      final User.MemberOf memberOf = new User.MemberOf();

      for (int i = 0; i < memberOfArray.length(); i++)
      {
        final JSONObject o = memberOfArray.getJSONObject(i);
        memberOf.getGroup().add(toPlural(o));
      }

      user.setMemberOf(memberOf);
    }

    return user;
  }



  /**
   * Reads a SCIM response from a JSON tokener.
   *
   * @param tokener  The tokener from which the response will be read.
   *
   * @return  The SCIM response that was read.
   *
   * @throws Exception  If an error occurs while reading the response.
   */
  private Response readResponse(final JSONTokener tokener)
      throws Exception
  {
    final JSONObject jsonObject = new JSONObject(tokener);

    final JSONObject responseObject = jsonObject.getJSONObject("Response");

    final Response response = new Response();
    if (responseObject.has("Resource"))
    {
      final JSONObject resourceObject =
          responseObject.optJSONObject("Resource");
      if (resourceObject != null)
      {
        response.setResource(readUser(resourceObject));
      }
    }
    else if (responseObject.has("Errors"))
    {
      final Response.Errors errors = new Response.Errors();
      final JSONArray errorsArray = responseObject.getJSONArray("Errors");
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
    else
    {
      if (responseObject.has("totalResults"))
      {
        response.setTotalResults(responseObject.optLong("totalResults"));
      }
      if (responseObject.has("itemsPerPage"))
      {
        response.setItemsPerPage(responseObject.optInt("itemsPerPage"));
      }
      if (responseObject.has("startIndex"))
      {
        response.setStartIndex(responseObject.optLong("startIndex"));
      }

      final JSONArray resourcesArray = responseObject.optJSONArray("Resources");
      if (resourcesArray != null)
      {
        final Response.Resources resources = new Response.Resources();
        for (int i = 0; i < resourcesArray.length(); i++)
        {
          final JSONObject resourceObject = resourcesArray.getJSONObject(i);
          resources.getResource().add(readUser(resourceObject));
        }

        response.setResources(resources);
      }
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
