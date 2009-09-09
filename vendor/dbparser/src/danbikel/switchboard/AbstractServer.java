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
    package danbikel.switchboard;

import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.*;

/**
 * Provides a convenient default implementation of the <code>Server</code>
 * interface, allowing subclasses to focus solely on the services they will
 * provide to clients.
 * <p>
 * <b>N.B.</b>: Subclasses should take care to properly call the
 * <code>AbstractSwitchboardUser.unexportWhenDead</code> method.
 * <p>
 * <b>A note on fault tolerance</b>: In order to ensure the
 * fault-tolerance of clients, subclasses should ensure that they use
 * socket factories that set the <tt>SO_TIMEOUT</tt> values of their
 * TCP/IP sockets to some integer greater than 0, by calling the
 * constructors of this class with non-zero <tt>timeout</tt>
 * arguments, or by providing custom socket factories that provide at
 * least the functionality of {@link danbikel.util.TimeoutSocketFactory
 * TimeoutSocketFactory}.
 * Subclasses that use sockets other than TCP/IP sockets should have
 * similar non-infinite timeouts.
 *
 * @see AbstractSwitchboardUser#unexportWhenDead
 */
public abstract class AbstractServer
  extends AbstractSwitchboardUser implements Server {

  // data members
  /** The maximum number of clients this server is willing to accept. */
  protected int maxClients;
  /** Indicates whether this server will only accept clients that specifically
      request it. */
  protected boolean acceptClientsOnlyByRequest;

  /**
   * Constructs a non-exported <code>AbstractServer</code> object.
   */
  protected AbstractServer() { }

  /**
   * Constructs a new server with the specified timeout value for its
   * RMI sockets, to receive RMI calls on an anonymous port.
   * <p>
   * <b>Warning</b>: Using a value of <tt>0</tt> for the
   * <code>timeout</code> argument will cause the distributed system
   * implemented by this package to be non-fault-tolerant.  Use a timeout
   * value greater than <tt>0</tt> to ensure the fault-tolerance of
   * the distributed computing system.
   * <p>
   * @param timeout the time in milliseconds before client-side
   * (switchboard-side) sockets used for this remote object time out; a value
   * of <tt>0</tt> specifies infinite timeout, which is dangerous
   */
  protected AbstractServer(int timeout) throws RemoteException {
    super(timeout);
  }

  /**
   * Constructs a new server with the specified timeout value for its
   * RMI sockets, to receive RMI calls on the specified port.
   * <p>
   * <b>Warning</b>: Using a value of <tt>0</tt> for the
   * <code>timeout</code> argument will cause the distributed system
   * implemented by this package to be non-fault-tolerant.  Use a timeout
   * value greater than <tt>0</tt> to ensure the fault-tolerance of
   * the distributed computing system.
   * <p>
   * @param timeout the time in milliseconds before client-side
   * (switchboard-side) sockets used for this remote object time out; a value
   * of <tt>0</tt> specifies infinite timeout, which is dangerous
   * @param port the port on which to receive RMI calls
   */
  protected AbstractServer(int timeout, int port) throws RemoteException {
    this(Server.acceptUnlimitedClients, false, timeout, port);
  }

  /**
   * Constructs a new server that will accept no more than the specified
   * number of clients, will optionally accept clients only by request,
   * that will use the specified timeout for its RMI sockets and
   * will accept RMI calls on the specified port.
   *
   * @param maxClients the maximum number of clients this server is
   * willing to accept
   * @param acceptClientsOnlyByRequest if <code>true</code>, then
   * this server will only accept clients that request it specifically
   * @param timeout the timeout value, in milliseconds, to be used for the
   * client-side (switchboard-side) RMI sockets of this object
   * @param port the port on which to receive RMI calls
   */
  protected AbstractServer(int maxClients, boolean acceptClientsOnlyByRequest,
			   int timeout, int port) throws RemoteException {
    super(timeout, port);
    this.maxClients = maxClients;
    this.acceptClientsOnlyByRequest = acceptClientsOnlyByRequest;
  }

  /**
   * Constructs a new server that will accept RMI calls on the specified
   * port, using the specified socket factories to create RMI sockets.
   * <p>
   * <b>Warning</b>: Custom socket factories should ensure that their
   * sockets have some sort of timeout mechanism (such as implemented
   * for TCP/IP sockets by {@link danbikel.util.TimeoutSocketFactory}).
   * If sockets do not time out, then the distributed system implemented
   * by this package will not be fault-tolerant.
   * <p>
   * @param port the port on which to receive RMI calls
   * @param csf the factory from which to create client-side RMI sockets
   * @param ssf the factory from which to create server-side RMI sockets
   */
  protected AbstractServer(int port,
			   RMIClientSocketFactory csf,
			   RMIServerSocketFactory ssf)
    throws RemoteException {
    this(Server.acceptUnlimitedClients, false, port, csf, ssf);
  }

  /**
   * Constructs a new server that will accept no more than the specified
   * number of clients, will optionally accept clients only by request,
   * will accept RMI calls on the specified port and will use the
   * specified socket factories to create its RMI sockets.
   * <p>
   * <b>Warning</b>: Custom socket factories should ensure that their
   * sockets have some sort of timeout mechanism (such as implemented
   * for TCP/IP sockets by {@link danbikel.util.TimeoutSocketFactory}).
   * If sockets do not time out, then the distributed system implemented
   * by this package will not be fault-tolerant.
   * <p>
   * @param maxClients the maximum number of clients this server is
   * willing to accept
   * @param acceptClientsOnlyByRequest if <code>true</code>, then
   * this server will only accept clients that request it specifically
   * @param port the port on which to receive RMI calls
   * @param csf the factory from which to create client-side RMI sockets
   * @param ssf the factory from which to create server-side RMI sockets
   */
  protected AbstractServer(int maxClients,
			   boolean acceptClientsOnlyByRequest,
			   int port,
			   RMIClientSocketFactory csf,
			   RMIServerSocketFactory ssf)
    throws RemoteException {
    super(port, csf, ssf);
    this.maxClients = maxClients;
    this.acceptClientsOnlyByRequest = acceptClientsOnlyByRequest;
  }

  /**
   * Calls {@link #setPolicyFile(String)} with the value of the
   * {@link SwitchboardRemote#serverPolicyFile} property obtained from
   * the specified <code>Properties</code> object.
   *
   * @param props the <code>Properties</code> object from which to
   * obtain the value of the {@link SwitchboardRemote#serverPolicyFile}
   * property
   */
  protected static void setPolicyFile(java.util.Properties props) {
    setPolicyFile(props.getProperty(SwitchboardRemote.serverPolicyFile));
  }

  /**
   * Calls {@link #setPolicyFile(Class,String)} with the specified class
   * and the value of the {@link SwitchboardRemote#serverPolicyFile}
   * property obtained from the specified <code>Properties</code> object.
   *
   * @param props the <code>Properties</code> object from which to
   * obtain the value of the {@link SwitchboardRemote#serverPolicyFile}
   * property
   */
  public static void setPolicyFile(Class cl, java.util.Properties props) {
    setPolicyFile(cl,
		  props.getProperty(SwitchboardRemote.switchboardPolicyFile));
  }

  public static void disableHttp(java.util.Properties props) {
    String property = props.getProperty(SwitchboardRemote.serverDisableHttp);
    if (property != null)
      disableHttp(property);
  }

  /**
   * Re-registers this server with the switchboard.  This method should
   * only be called in the event of a switchboard failure.
   */
  protected void reRegister() throws RemoteException {
    if (registered)
      return;
    try {
      register(switchboardName);
    }
    catch (MalformedURLException mue) {
      System.err.println(mue);
    }
  }

  /**
   * Registers this server with the specified switchboard.
   */
  protected void register(String switchboardName)
    throws RemoteException, MalformedURLException {
    while (!registered) {
      try {
	getSwitchboard(switchboardName);
	id = switchboard.register((Server)this);
	registered = true;
      }
      catch (RemoteException re) {
	System.err.println("error trying to register (" + re + ")");
      }
    }
  }

  /** Returns the maximum number of clients this server is willing to accept. */
  public int maxClients() throws RemoteException {
    return maxClients;
  }
  /** Returns whether this server will only accept clients that specifically
      request it. */
  public boolean acceptClientsOnlyByRequest() {
    return acceptClientsOnlyByRequest;
  }
}
