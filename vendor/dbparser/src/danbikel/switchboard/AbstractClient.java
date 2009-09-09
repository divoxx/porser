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

import danbikel.util.proxy.Reconnect;
import danbikel.util.proxy.Retry;
import java.util.*;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * Provides a convenient default implementation of the
 * <code>Client</code> interface, as well as other convenient utility
 * methods.  A subclass need only provide a concrete implementation of
 * the {@link #process(Object)} method.
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
public abstract class AbstractClient
  extends AbstractSwitchboardUser implements Client {

  // public constants
  public static final int defaultNextObjectInterval = 10000;

  // private constants
  private static final boolean debug = false;

  // inner class

  // data members
  /** The interval at which clients should keep pinging the switchboard
      for more objects after receving <code>null</code> from the
      {@link SwitchboardRemote#nextObject(int)} method. */
  protected int nextObjectInterval = defaultNextObjectInterval;

  /** The server assigned to this client. */
  protected volatile Server server = null;
  /** The ID number of the server assigned to this client, cached here
      for convenience. */
  protected volatile int serverId = -1;

  // fault-tolerant settings
  /** Indicates whether the server has been wrapped in proxies for fault
      tolerance, via a call to the {@link #tolerateFaults} or
      {@link #getFaultTolerantServer} method.  Initially <code>false</code>. */
  protected volatile boolean faultTolerant = false;
  /** Cache of the value of the parameter of the same name in the
      {@link #tolerateFaults} or {@link #getFaultTolerantServer} methods.
      Initially <code>-1</code>. */
  protected volatile int retries = -1;
  /** Cache of the value of the parameter of the same name in the
      {@link #tolerateFaults} or {@link #getFaultTolerantServer} methods.
      Initially <code>-1</code>. */
  protected volatile int sleepTime = -1;
  /** Cache of the value of the parameter of the same name in the
      {@link #tolerateFaults} or {@link #getFaultTolerantServer} methods.
      Initially <code>false</code>.  */
  protected volatile boolean failover = false;

  /**
   * The pseudorandom number generator used by {@link #sleepRandom},
   * initialized at construction with a random seed based on the
   * current time.
   */
  protected Random rand = new Random(System.currentTimeMillis());



  /**
   * Constructs a non-exported <code>AbstractClient</code> object.
   */
  protected AbstractClient() throws RemoteException { }

  /**
   * Constructs a new client with the specified timeout, to be set for
   * switchboard-side (RMI client-side) sockets.  This constructor
   * must be called by the constructor of a subclass.
   * <p>
   * <b>Warning</b>: Using a value of <tt>0</tt> for the
   * <code>timeout</code> argument will cause the distributed system
   * implemented by this package to be non-fault-tolerant.  Use a timeout
   * value greater than <tt>0</tt> to ensure the fault-tolerance of
   * the distributed computing system.
   * <p>
   * @param timeout the time in milliseconds before client-side
   * (switchboard-side) sockets used for this remote object time out;
   * a value of <tt>0</tt> specifies infinite timeout, which is
   * dangerous
   */
  protected AbstractClient(int timeout) throws RemoteException {
    super(timeout);
  }

  /**
   * Constructs a new client taking RMI calls on the specified port,
   * with the specified timeout to be set for switchboard-side (RMI
   * client-side) sockets.  This constructor must be called
   * by the constructor of a subclass.
   * <p>
   * <b>Warning</b>: Using a value of <tt>0</tt> for the
   * <code>timeout</code> argument will cause the distributed system
   * implemented by this package to be non-fault-tolerant.  Use a timeout
   * value greater than <tt>0</tt> to ensure the fault-tolerance of
   * the distributed computing system.
   * <p>
   * @param timeout the time in milliseconds before client-side
   * (switchboard-side) sockets used for this remote object time out;
   * a value of <tt>0</tt> specifies infinite timeout, which is dangerous
   * @param port the port on which this remote object is to receive
   * remote method invocations
   */
  protected AbstractClient(int timeout, int port) throws RemoteException {
    super(timeout, port);
  }

  /**
   * Constructs a new client taking RMI calls on the specified port
   * and using the specified socket factories.  This constructor must be
   * called by the constructor of a subclass.
   * <p>
   * <b>Warning</b>: Custom socket factories should ensure that their
   * sockets have some sort of timeout mechanism (such as implemented
   * for TCP/IP sockets by {@link danbikel.util.TimeoutSocketFactory}).
   * If sockets do not time out, then the distributed system implemented
   * by this package will not be fault-tolerant.
   * <p>
   * @param port the port on which this remote object is to receive
   * remote method invocations
   */
  protected AbstractClient(int port,
			   RMIClientSocketFactory csf,
			   RMIServerSocketFactory ssf)
    throws RemoteException {
    super(port, csf, ssf);
  }

  /**
   * Calls {@link #setPolicyFile(String)} with the value of the
   * {@link SwitchboardRemote#clientPolicyFile} property obtained from
   * the specified <code>Properties</code> object.
   *
   * @param props the <code>Properties</code> object from which to
   * obtain the value of the {@link SwitchboardRemote#clientPolicyFile}
   * property
   */
  protected static void setPolicyFile(Properties props) {
    setPolicyFile(props.getProperty(SwitchboardRemote.clientPolicyFile));
  }

  /**
   * Calls {@link #setPolicyFile(Class,String)} with the specified class
   * and the value of the {@link SwitchboardRemote#clientPolicyFile}
   * property obtained from the specified <code>Properties</code> object.
   *
   * @param props the <code>Properties</code> object from which to
   * obtain the value of the {@link SwitchboardRemote#clientPolicyFile}
   * property
   */
  public static void setPolicyFile(Class cl, java.util.Properties props) {
    setPolicyFile(cl,
		  props.getProperty(SwitchboardRemote.clientPolicyFile));
  }

  public static void disableHttp(Properties props) {
    String property = props.getProperty(SwitchboardRemote.clientDisableHttp);
    disableHttp(property);
  }

  /**
   * Gets a new server and wraps it in proxies that ensure the fault
   * tolerance of calls to that server.  The proxies used
   * are implemented via the {@link Failover} and
   * {@link AbstractSwitchboardUser.SBUserRetry SBUserRetry} classes.
   * <p>
   * Each time a call is made to this method, the parameter values are
   * cached in this object, in the {@link #retries}, {@link
   * #sleepTime} and {@link #failover} data members.  This allows
   * the {@link #reRegister} method to properly re-wrap a new server
   * when it gets one after a switchboard failure.
   *
   * @param retries the number of times to re-try the server in the event
   * of failure; a value of {@link danbikel.util.proxy.Retry#retryIndefinitely}
   * will cause the proxy to re-try indefinitely
   * @param sleepTime the time, in milliseconds, to sleep between retries
   * @param failover indicates whether to wrap the server in a
   * failover proxy
   */
  protected void getFaultTolerantServer(int retries, int sleepTime,
					boolean failover)
    throws RemoteException {
    getServer();
    tolerateFaults(retries, sleepTime, failover);
  }

  /**
   * Wraps the current server in proxies that ensure the fault tolerance
   * of calls to that server.  Each time a call is made to this method,
   * the parameter values are cached in this object, in the {@link #retries},
   * {@link #sleepTime} and {@link #failover} data members.  This allows
   * the {@link #reRegister} method to properly re-wrap a new server
   * when it gets one after a switchboard failure.  If this method is called
   * with <code>retries == 0</code> and <code>failover == false</code>,
   * then it simply returns, having done no proxy wrapping.
   * <p>
   * <b>N.B.</b>: This method re-assigns the <code>protected</code>
   * data member <code>server</code>.  If subclasses have cached a reference
   * to the server in a local data member, they should override this method
   * such that this implementation is called
   * (<code>super.tolerateFaults(...)</code>) and then the server re-cached,
   * as shown in the following example code:
   * <pre>
   * public class MyClient extends AbstractClient {
   *   <font color=red>// local reference to the server of a type implemented
   *   // by the actual (concrete) servers in a particular distributed system</font>
   *   private MyServerInterface server;
   *
   *   protected void tolerateFaults(int retries, int sleepTime,
   *                                 boolean failover) throws RemoteException {
   *     super.tolerateFaults(retries, sleepTime, failover);
   *     server = (MyServerInterface)super.server;
   *   }
   *   ...
   * }
   * </pre>
   * <p>
   *
   * @param retries the number of times to re-try the server in the event
   * of failure; a value of {@link danbikel.util.proxy.Retry#retryIndefinitely}
   * will cause the proxy to re-try indefinitely
   * @param sleepTime the time, in milliseconds, to sleep between retries
   * @param failover indicates whether to wrap the server in a
   * failover proxy
   *
   * @see #server
   */
  protected void tolerateFaults(int retries, int sleepTime, boolean failover) {
    if (retries == 0 && !failover) {
      faultTolerant = false;
      return;
    }

    // cache these settings, in case we need to get a new server
    this.retries = retries;
    this.sleepTime = sleepTime;
    this.failover = failover;

    // indicate that this method has been called, in case we need to get a
    // new server and re-wrap it in fault-tolerant proxies using this method
    faultTolerant = true;

    if (failover)
      server = (Server)Failover.proxyFor(server, this, switchboard);

    server =
      (Server)SBUserRetry.proxyFor(server, retries, sleepTime);
  }

  /**
   * A hook that is called by <code>reRegister</code>, so that clients
   * may perform any emergency procedures prior to re-registering and getting
   * a new server.  This default implementation does nothing.
   */
  protected void switchboardFailure() { }

  /**
   * Re-registers this client with the switchboard after a switchboard
   * failure.  First, this method calls {@link #switchboardFailure}.
   * Next, it calls {@link #register} to perform the actual re-acquiring
   * of the switchboard and re-registering of this client.
   * Finally, it gets a new server (via {@link #getServer}) and, if the
   * previous server was fault-tolerant, calls {@link #tolerateFaults}
   * with the same settings that were previously used.
   *
   * @see #faultTolerant
   * @see #retries
   * @see #sleepTime
   * @see #failover
   */
  protected void reRegister() throws RemoteException {
    if (registered)
      return;
    // before anything, call switchboardFailure
    switchboardFailure();

    if (timeToDie)
      return;

    // first, re-register this client
    try { register(switchboardName); }
    catch (MalformedURLException mue) {
      System.err.println(className + ": error registering (" + mue + ")");
    }
    // next, tell old server that it should re-register
    /*
    try {
      server.reRegister();
    }
    catch (RemoteException re) {
      System.err.println("trouble telling server to re-register (" + re + ")");
    }
    */
    server = null;
    // finally, get a new server
    getServer();
    if (faultTolerant)  // if tolerateFaults had previously been called...
      tolerateFaults(retries, sleepTime, failover);
  }

  /**
   * Registers this client with the specified switchboard, caching
   * the switchboard to an internal data member.  After registering,
   * this client will have a valid ID number assigned to it.  This method
   * will not return until a switchboard stub has been obtained from the
   * bootstrap registry and this client is successfully registered.
   *
   * @param switchboardName the name of the switchboard in the bootstrap
   * registry
   * @throws RegistrationException if this client could not be registered
   */
  protected void register(String switchboardName)
    throws RemoteException, MalformedURLException {
    while (!registered) {
      try {
	getSwitchboard(switchboardName);
	id = switchboard.register((Client)this);
	setNextObjectInterval();
	registered = true;
      }
      catch (RemoteException re) {
	if (debug)
	  System.err.println(className + ": error registering (" + re + ")");
      }
    }
  }

  /**
   * Attempts to get the {@link SwitchboardRemote#clientNextObjectInterval}
   * setting from the switchboard and set the {@link #nextObjectInterval}
   * data member to it.  If there is a remote method failure or if
   * the property is not in the switchboard's settings, this method
   * silently leaves the {@link #nextObjectInterval} unchanged.
   */
  protected void setNextObjectInterval() {
    String nextObjectIntervalStr = null;
    try {
      nextObjectIntervalStr =
	switchboard.getSetting(SwitchboardRemote.clientNextObjectInterval);
    }
    catch (RemoteException re) {}
    if (nextObjectIntervalStr != null)
      nextObjectInterval = Integer.parseInt(nextObjectIntervalStr);
  }

  /**
   * Currently, this method does nothing, as all server failures can be
   * handled by making the server fault-tolerant.
   *
   * @see #tolerateFaults
   */
  public void serverDown(int serverId) throws RemoteException {
    if (serverId == this.serverId) {
      //server = null;
    }
  }

  /**
   * Unless it is time to die, this method continually tries the switchboard
   * until it can assign this client a server.
   * <p>
   * <b>N.B.</b>: This method re-assigns the <code>protected</code>
   * data member <code>server</code>.  If subclasses have cached a reference
   * to the server in a local data member, they should override this method
   * such that this implementation is called
   * (<code>super.getServer()</code>) and then the server re-cached,
   * as shown in the following example code:
   * <pre>
   * public class MyClient extends AbstractClient {
   *   <font color=red>// local reference to the server of a type implemented
   *   // by the actual (concrete) servers in a particular distributed system</font>
   *   private MyServerInterface server;
   *
   *   protected void getServer() throws RemoteException {
   *     super.getServer();
   *     server = (MyServerInterface)super.server;
   *   }
   *   ...
   * }
   * </pre>
   * <p>
   *
   * @see #server
   */
  protected void getServer() throws RemoteException {
    while (!timeToDie && server == null) {
      if (debug)
	System.err.println(className + ": waiting for new server...");
      server = switchboard.getServer(id);
      //sleepRandom(5000);
    }
    if (debug)
      System.err.println(className + ": got it!");

    serverId = server.id();
  }

  /**
   * Sleeps for a random interval between 0 and the specified number of
   * milliseconds.
   *
   * @param maxMillis the maximum number of milliseconds this method
   * will cause the current thread to sleep
   * @return the number of milliseconds that this method tried to
   * cause the current thread to sleep (may be less if an
   * <code>InterruptedException</code> was thrown during the sleep
   * call)
   */
  protected int sleepRandom(int maxMillis) {
    int randMillis = rand.nextInt(maxMillis);
    try { Thread.sleep(randMillis); }
    catch (InterruptedException ie) {}
    return randMillis;
  }

  protected void cleanup() {
    server = null;
  }

  /**
   * This is the only purely abstract method of this class, allowing
   * subclasses to focus purely on their job, which is object-processing.
   *
   * @param obj the object to be processed
   * @return the processed object, or <code>null</code> if the object
   * was unable to be processed
   */
  protected abstract Object process(Object obj) throws RemoteException;

  /**
   * A convenience method that simply calls {@link #processObjects} and then
   * {@link #unexportWhenDead}.
   */
  protected void processObjectsThenDie() throws RemoteException {
    processObjects();
    unexportWhenDead();
  }

  /**
   * Gets objects from the switchboard until there are no more to get,
   * processes them by invoking the {@link #process(Object)} method
   * and puts the processed objects back to the switchboard, handling
   * any RMI-related errors along the way.
   *
   * @see SwitchboardRemote#clientNextObjectInterval
   * @see SwitchboardRemote#nextObject(int)
   */
  protected void processObjects() throws RemoteException {
    int numProcessed = 0;

    NumberedObject obj = null;
    while (!timeToDie) {
      try {
	obj = switchboard.nextObject(id);
	if (obj == null) {
	  if (!timeToDie) {
	    synchronized (dieSynch) {
	      try { dieSynch.wait(nextObjectInterval); }
	      catch (InterruptedException ie) {}
	    }
	  }
	  continue;
	}

	Object processed = null;
	long processingTime = System.currentTimeMillis();
	try { processed = process(obj.get()); }
	catch (RemoteException re) {
	  System.err.println(className +
			     ": warning: error processing object No. " +
			     obj.number() + " (" + re + ")");
	  processed = null;
	}
	processingTime = System.currentTimeMillis() - processingTime;

	if (processed != null) {
	  obj.setProcessed(true);
	  obj.set(processed);
	}

	switchboard.putObject(id, obj, processingTime);
	numProcessed++;
      }
      catch (RemoteException re) {
	registered = false;
	System.err.println(className + ": switchboard must have gone down (" +
			   re + "); " + "\n\twill attempt to re-register");
	reRegister();
	if (registered) {
	  System.err.println(className + ": successfully re-registered");
	}
      }
    }
  }
}
