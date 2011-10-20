/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri.wink;

import com.unboundid.scim.schema.CoreSchema;
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
    instances.add(new CRUDResource(CoreSchema.USER_DESCRIPTOR));
    instances.add(new CRUDResource(CoreSchema.GROUP_DESCRIPTOR));
    instances.add(new QueryResource(CoreSchema.USER_DESCRIPTOR));
    instances.add(new QueryResource(CoreSchema.GROUP_DESCRIPTOR));
    instances.add(new XMLQueryResource(CoreSchema.USER_DESCRIPTOR));
    instances.add(new JSONQueryResource(CoreSchema.USER_DESCRIPTOR));

    return instances;
  }
}
