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
 * Delivers sockets with timeout values (set via
 * <code>Socket.setSoTimeout</code>).  In the case of
 * <code>createServerSocket</code>, the <code>ServerSocket</code>
 * returned is of type <code>TimeoutServerSocket</code>, which
 * delivers sockets via its implementation of <code>accept</code> with
 * the specified server-side timeout value.
 *
 * @see Socket#setSoTimeout(int)
 */
public class TimeoutSocketFactory
  implements RMIClientSocketFactory, RMIServerSocketFactory, Serializable {


  private int clientTimeout;
  private int serverTimeout;

  /**
   * Constructs a new <code>TimeoutSocketFactory</code> with the
   * specified timeout values for server and client sockets.
   *
   * @param clientTimeout the timeout value to be set for client-side sockets;
   * a value of 0 indicates an infinite timeout
   * @param serverTimeout the timeout value to be set for server-side sockets;
   * a value of 0 indicates an infinite timeout
   *
   * @throws IllegalArgumentException if either <code>clientTimeout</code>
   * or <code>serverTimeout</code> is less than 0
   */
  public TimeoutSocketFactory(int clientTimeout, int serverTimeout) {
    if (clientTimeout < 0 || serverTimeout < 0)
      throw new IllegalArgumentException();
    this.clientTimeout = clientTimeout;
    this.serverTimeout = serverTimeout;
  }
  /**
   * Returns a <code>TimeoutServerSocket</code> constructed with the
   * <code>serverTimeout</code> value specified at construction of
   * this factory and with the specified port.  A
   * <code>TimeoutServerSocket</code> delivers sockets via its
   * implementation of <code>accept</code> with the specified
   * server-side timeout value.
   *
   * @see TimeoutServerSocket
   * @see TimeoutServerSocket#accept
   */
  public ServerSocket createServerSocket(int port) throws IOException {
    return new TimeoutServerSocket(serverTimeout, port);
  }

  /**
   * Returns a <code>Socket</code> object created on the specified
   * host and port, having set its timeout value to the
   * <code>clientTimeout</code> value specified at construction of
   * this factory.
   *
   * @see Socket#setSoTimeout(int)
   */
  public Socket createSocket(String host, int port) throws IOException {
    return new TimeoutSocket(host, port, clientTimeout);
  }

  public void setTimeout(int clientTimeout, int serverTimeout) {
    this.clientTimeout = clientTimeout;
    this.serverTimeout = serverTimeout;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof TimeoutSocketFactory))
      return false;
    TimeoutSocketFactory other = (TimeoutSocketFactory)obj;
    return (this.clientTimeout == other.clientTimeout &&
	    this.serverTimeout == other.serverTimeout);
  }

  public int hashCode() {
    return ((31 * serverTimeout) + clientTimeout);
  }
}

