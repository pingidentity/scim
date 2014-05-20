/*
 * Copyright 2014 UnboundID Corp.
 * All Rights Reserved.
 */

package com.unboundid.scim.ri;

import com.unboundid.scim.sdk.SCIMService;

import javax.ws.rs.core.MediaType;

/**
 * This class provides test coverage for the SCIMServer class with XML content
 * and XML response using URL suffix.
 */
public class XmlDotXmlSCIMServerTestCase extends SCIMServerTestCase
{
  @Override
  protected SCIMService createSCIMService(String userName, String password)
  {
    SCIMService service = super.createSCIMService(userName, password);
    service.setContentType(MediaType.APPLICATION_XML_TYPE);
    service.setAcceptType(MediaType.APPLICATION_XML_TYPE);
    service.setUseUrlSuffix(true);
    return service;
  }
}
