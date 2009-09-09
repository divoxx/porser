/* *           Copyright (c) 2004, Daniel M. Bikel.
 *                         All rights reserved.
 * 
 *                Developed at the University of Pennsylvania
 *                Institute for Research in Cognitive Science
 *                    3401 Walnut Street
 *                    Philadelphia, Pennsylvania 19104
 * 			
 * 
 * For research or educational purposes only.  Do not redistribute.  For
 * complete license details, please read the file LICENSE that accompanied
 * this software.
 * 
 * DISCLAIMER
 * 
 * Daniel M. Bikel makes no representations or warranties about the suitability of
 * the Software, either express or implied, including but not limited to the
 * implied warranties of merchantability, fitness for a particular purpose, or
 * non-infringement. Daniel M. Bikel shall not be liable for any damages suffered
 * by Licensee as a result of using, modifying or distributing the Software or its
 * derivatives.
 * 
 */
    package danbikel.util;

import java.net.*;
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * A <code>ServerSocket</code> subclass that delivers <code>Socket</code>
 * objects via its implementation of {@link #accept} that have
 * had their timeout values set to the value specified at construction.
 */
public class TimeoutServerSocket extends ServerSocket {

  private int timeout;
  
  /**
   * Constructs a server socket on the specified port, that
   * delivers sockets with the specified timeout value via the
   * {@link #accept} method.
   */
  public TimeoutServerSocket(int timeout, int port) throws IOException {
    super(port);
    this.timeout = timeout;
  }

  /**
   * Creates a socket with the timeout value specified at construction,
   * then calls <code>ServerSocket.implAccept</code> to wait for a
   * connection.
   */
  public Socket accept() throws IOException {
    Socket s = new TimeoutSocket(timeout);
    implAccept(s);
    return s;
  }
}

