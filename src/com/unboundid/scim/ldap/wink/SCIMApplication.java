/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ldap.wink;

import com.unboundid.scim.sdk.SCIMConstants;
import org.apache.wink.common.WinkApplication;

import java.util.HashSet;
import java.util.Set;



/**
 * This class is a JAX-RS Application that returns the SCIM resource
 * implementations.
 */
public class SCIMApplication extends WinkApplication
{
  @Override
  public Set<Object> getInstances()
  {
    final Set<Object> instances = new HashSet<Object>();
    instances.add(new CRUDResource(SCIMConstants.RESOURCE_NAME_USER));
    instances.add(new QueryResource(SCIMConstants.RESOURCE_ENDPOINT_USERS));

    return instances;
  }
}
