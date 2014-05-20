/*
 * Copyright 2014 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri;

import com.unboundid.scim.sdk.SCIMService;

import javax.ws.rs.core.MediaType;

/**
 * This class provides test coverage for the SCIMServer class with JSON content
 * and JSON response using URL suffix.
 */
public class JsonDotJsonSCIMServerTestCase extends SCIMServerTestCase
{
  @Override
  protected SCIMService createSCIMService(String userName, String password)
  {
    SCIMService service = super.createSCIMService(userName, password);
    service.setContentType(MediaType.APPLICATION_JSON_TYPE);
    service.setAcceptType(MediaType.APPLICATION_JSON_TYPE);
    service.setUseUrlSuffix(true);
    return service;
  }
}

