/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri.wink;

import com.unboundid.scim.config.SchemaManager;
import com.unboundid.scim.schema.ResourceDescriptor;
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
  public Set<Class<?>> getClasses()
  {
    final Set<Class<?>> classes = new HashSet<Class<?>>();
    classes.add(MonitorResource.class);

    return classes;
  }



  @Override
  public Set<Object> getInstances()
  {
    final Set<Object> instances = new HashSet<Object>();
    ResourceDescriptor userResource = SchemaManager.instance().
        getResourceDescriptor(SCIMConstants.RESOURCE_NAME_USER);
    instances.add(new CRUDResource(userResource));
    instances.add(new CRUDResource(SchemaManager.instance().
        getResourceDescriptor(SCIMConstants.RESOURCE_NAME_GROUP)));
    instances.add(new QueryResource(userResource));
    instances.add(new XMLQueryResource(userResource));
    instances.add(new JSONQueryResource(userResource));

    return instances;
  }
}
