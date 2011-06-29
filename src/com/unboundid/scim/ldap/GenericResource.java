/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap;

import com.unboundid.scim.schema.Meta;
import com.unboundid.scim.schema.Resource;
import com.unboundid.scim.sdk.SCIMAttribute;
import com.unboundid.scim.sdk.SCIMConstants;
import com.unboundid.scim.sdk.SCIMObject;



/**
 * This class wraps a SCIM Object into a Resource.
 */
public class GenericResource extends Resource
{
  /**
   * The SCIM object representing the resource.
   */
  private final SCIMObject scimObject;



  /**
   * Create a new generic resource.
   *
   * @param scimObject  The SCIM object representing the resource.
   */
  public GenericResource(final SCIMObject scimObject)
  {
    this.scimObject = scimObject;

    Meta meta = null;
    final SCIMAttribute a =
        scimObject.getAttribute(SCIMConstants.SCHEMA_URI_CORE, "meta");
    if (a != null)
    {
      meta = new Meta();
    }

    super.setMeta(meta);
    super.setId(scimObject.getResourceID());
  }



  /**
   * Sets the value of the meta property.
   *
   * @param value allowed object is
   *              {@link com.unboundid.scim.schema.Meta }
   */
  @Override
  public void setMeta(final Meta value)
  {
    // TODO
  }



  /**
   * Sets the value of the id property.
   *
   * @param value allowed object is
   *              {@link String }
   */
  @Override
  public void setId(final String value)
  {
    // TODO
  }



  /**
   * Retrieve the SCIM object representing the resource.
   *
   * @return  The SCIM object representing the resource.
   */
  public SCIMObject getScimObject()
  {
    return scimObject;
  }
}
