/*
 * Copyright 2011 UnboundID Corp.
 * All Rights Reserved.
 */
package com.unboundid.scim.ri;



import java.net.Socket;

import com.unboundid.ldap.listener.LDAPListenerClientConnection;
import com.unboundid.ldap.listener.LDAPListenerExceptionHandler;
import com.unboundid.ldap.sdk.LDAPException;



/**
 * This class provides an LDAP listener exception handler that will print
 * messages about any unexpected problems to standard error.
 */
public final class StandardErrorListenerExceptionHandler
       implements LDAPListenerExceptionHandler
{
  /**
   * Creates a new instance of this class.
   */
  public StandardErrorListenerExceptionHandler()
  {
    // No implementation required.
  }



  /**
   * {@inheritDoc}
   */
  public void connectionCreationFailure(final Socket socket,
                                        final Throwable cause)
  {
    System.err.println("Unable to establish a new connection:");
    cause.printStackTrace();
  }



  /**
   * {@inheritDoc}
   */
  public void connectionTerminated(
                   final LDAPListenerClientConnection connection,
                   final LDAPException cause)
  {
    System.err.println("Client connection unexpectedly terminated:");
    cause.printStackTrace();
  }
}
