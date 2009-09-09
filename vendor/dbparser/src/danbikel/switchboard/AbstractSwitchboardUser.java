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

import danbikel.util.TimeoutSocketFactory;
import danbikel.util.proxy.Retry;
import java.io.Serializable;
import java.io.PrintWriter;
import java.util.Properties;
import java.net.*;
import java.rmi.*;
import java.rmi.server.*;
import java.lang.reflect.Proxy;

/**
 * Provides a convenient default implementation of the interface
 * <code>SwitchboardUser</code>, so that subclasses that need to
 * implement the <code>Client</code> and <code>Server</code> interfaces
 * need only implement the methods introduced in those subinterfaces,
 * as is the case with the <code>AbstractClient</code> and
 * <code>AbstractServer</code> implementations provided by this package.
 * <p>
 * Put another way, the interface hierarchy of <code>SwitchboardUser</code>,
 * <code>Client</code> and <code>Server</code> is mirrored by the
 * implementation hierarchy of <code>AbstractSwitchboardUser</code>,
 * <code>AbstractClient</code> and <code>AbstractServer</code>:
 * <pre>
 *   Interface Hierarchy          Implementation Hierarchy
 *   -------------------          ------------------------
 *     SwitchboardUser             AbstractSwitchboardUser
 *       /        \                  /             \
 *      /          \                /               \
 *   Client      Server       AbstractClient   AbstractServer
 * </pre>
 * <p>
 *
 * @see AbstractClient
 * @see AbstractServer
 */
public abstract class AbstractSwitchboardUser
  extends RemoteServer implements SwitchboardUser {

  /** The fallback-default timeout value for client- (switchboard-) side
      sockets.  The value of this constant is <tt>120000</tt>. */
  protected static final int defaultTimeout = 120000;


  /**
   * The value to specify for switchboard users to try indefinitely to
   * re-acquire the switchboard when first starting up or in the event
   * of a switchboard crash.
   */
  protected static final int infiniteTries = -1;

  /**
   * The fallback-default maximum number of times to try contacting
   * the switchboard after it goes down or when registering for the first time.
   * The current value of this constant is {@link #infiniteTries}.
   *
   * @see #maxSwitchboardTries
   */
  protected static final int defaultMaxSwitchboardTries = infiniteTries;

  // inner classes

  /**
   * Class to implement a thread that detects whether the switchboard
   * has gone down, by noticing when it has been too long since its
   * last call to {@link AbstractSwitchboardUser#alive()}.
   */
  protected class Alive implements Runnable {
    protected Alive() {}

    public void run() {
      while (aliveTimeout < 0)
	;
      while (!timeToDie) {
	synchronized (aliveSynch) {
	  try { aliveSynch.wait(aliveTimeout); }
	  catch (InterruptedException ie) {
	    // if this thread is interrupted, it's probably time to die, so...
	    continue;
	  }
	}
	if (!timeToDie)
	  if (aliveRenewed)
	    aliveRenewed = false;
	  else {
	    registered = false;
	    System.err.println(className + ": switchboard appears to have " +
			       "gone down;\n\tattempting to re-acquire and " +
			       "re-register");
	    try {
	      reRegister();
	      System.err.println(className + ": successfully re-registered");
	    }
	    catch (RemoteException re) {
	      //System.err.println(re);
	    }
	  }
      }
    }
  }

  /**
   * Provides an appropriate overridden definition of {@link Retry#keepTrying}
   * that checks the value of {@link AbstractSwitchboardUser#timeToDie}.
   */
  protected static class SBUserRetry extends Retry {
    AbstractSwitchboardUser sbUser;

    protected SBUserRetry(Object stub, AbstractSwitchboardUser sbUser,
			  int retries, long sleep) {
      super(stub, retries, sleep);
      this.sbUser = sbUser;
    }

    protected static
    synchronized Object proxyFor(Object stub, AbstractSwitchboardUser sbUser,
				 int retries, long sleep) {
      Class stubClass = stub.getClass();
      return Proxy.newProxyInstance(stubClass.getClassLoader(),
				    stubClass.getInterfaces(),
				    new SBUserRetry(stub, sbUser,
						    retries, sleep));
    }

    protected boolean keepTrying() {
      return !sbUser.timeToDie;
    }
  }


  /** The name of the runtime type of the subclass, cached here in
      this abstract class' constructor for convenience. */
  protected String className;

  /** A handle onto the switchboard. */
  protected SwitchboardRemote switchboard = null;

  /** The bootstrap registry name of the switchboard. */
  protected String switchboardName = null;

  /** Flag that indicates whether this client is currently registered. */
  protected volatile boolean registered = false;

  /** The object on which to synchronize death.  It is important that
      subclasses never invoke this object's <code>notify</code> or
      <code>notifyAll</code> method, for otherwise the semantics of
      certain methods of this class will be corrupted.   However, subclasses
      may wait on this object.  */
  protected Object dieSynch = new Object();
  /** The boolean indicating that this object is ready to die, and will
      wait for the length of the timeout value before actually committing
      suicide (by unexporting itself via the {@link #unexportWhenDead}
      method). */
  protected volatile boolean timeToDie = false;
  /** The boolean indicating that it this object is dead, allowing
      {@link #unexportWhenDead} to run.  This data member is private,
      so that not even a malicious subclass can violate the semantics
      that once this value is <code>true</code>, it <i>never</i> gets set to
      <code>false</code>.  However, we keep {@link #timeToDie} protected,
      allowing subclasses to easily access it. */
  private volatile boolean dead = false;

  /** The value for which switchboard-side (RMI client-side) sockets will
      timeout (a value of 0 indicates infinite timeout). */
  protected int timeout = defaultTimeout;

  /** The maximum number of times the {@link #getSwitchboard} method will
      try to get a new <code>Switchboard</code> when the current instance
      has gone down or when registering for the first time.  A value of
      {@link #infiniteTries} indicates an infinite number of tries. */
  protected int maxSwitchboardTries = defaultMaxSwitchboardTries;

  /** The unique ID of this switchboard user, assigned by the switchboard. */
  protected int id = -1;

  protected Object aliveSynch = new Object();
  protected int aliveTimeout = -1;
  private boolean aliveRenewed = false;

  /**
   * A no-arg constructor for concrete subclasses that wish to have
   * stand-alone (non-exported) constructors.
   */
  protected AbstractSwitchboardUser() {}

  /**
   * Constructs a switchboard user whose switchboard-side (RMI client-side)
   * sockets will have the specified timeout.  This constructor must be
   * called by the constructor of a subclass.
   *
   * @param timeout the timeout value for switchboard-side (RMI client-side)
   * sockets, in milliseconds; a value of 0 indicates infinite timeout
   */
  protected AbstractSwitchboardUser(int timeout) throws RemoteException {
    this(timeout, 0);
  }

  /**
   * Constructs a switchboard user accepting RMI calls on the
   * specified port, and whose switchboard-side (RMI client-side)
   * sockets will have the specified timeout.  This constructor must be
   * called by the constructor of a subclass.
   *
   * @param timeout the timeout value for switchboard-side (RMI client-side)
   * sockets, in milliseconds; a value of 0 indicates infinite timeout
   * @param port the port on which to receive RMI calls
   */
  protected AbstractSwitchboardUser(int timeout, int port)
    throws RemoteException {
    this(port,
	 new TimeoutSocketFactory(timeout, 0),
	 new TimeoutSocketFactory(timeout, 0));
    this.timeout = timeout;
  }

  /**
   * Constructs a switchboard user receiving RMI calls on the specified
   * port, constructing client- and server-side sockets from the
   * specified socket factories.
   *
   * @param port the port on which this object will receive RMI calls
   * @param csf the client-side socket factory
   * @param ssf the server-side server socket factory
   */
  protected AbstractSwitchboardUser(int port,
				    RMIClientSocketFactory csf,
				    RMIServerSocketFactory ssf)
    throws RemoteException {
    //UnicastRemoteObject.exportObject(this, port, csf, ssf);
    UnicastRemoteObject.exportObject(this, port);
    setClassName();
  }

  /**
   * Sets the system property <tt>"java.security.policy"</tt> to be the
   * URL of the specified resource obtained from the
   * <code>SwitchboardRemote</code> class.
   *
   * @param resource the resource to obtain from {@link SwitchboardRemote}
   * that will be the value of the system property
   * <tt>"java.security.policy"</tt>
   */
  protected static void setPolicyFile(String resource) {
    setPolicyFile(SwitchboardRemote.class, resource);
  }

  /**
   * Sets the system property <tt>"java.security.policy"</tt> to be the
   * URL of the specified resource obtained from the specified class.
   *
   * @param resource the resource to obtain from the specified class
   * that will be the value of the system property
   * <tt>"java.security.policy"</tt>
   */
  protected static void setPolicyFile(Class cl, String resource) {
    if (System.getProperty("java.security.policy") == null) {
      URL policyURL = cl.getResource(resource);
      System.setProperty("java.security.policy", policyURL.toString());
    }
  }

  protected static void disableHttp(String property) {
    if (System.getProperty("java.rmi.server.disableHttp") == null)
      System.setProperty("java.rmi.server.disableHttp", property);
  }

  /**
   * Repeatedly tries to get the switchboard stub from the bootstrap registry.
   * The number of times is determined by the value of
   * {@link #maxSwitchboardTries}.  There will be no error output
   * for the repeated attempts (no verbosity).
   */
  protected void getSwitchboard(String name)
    throws MalformedURLException {
    getSwitchboard(name, false);
  }
  /**
   * Repeatedly tries to get the switchboard stub from the bootstrap registry.
   * The number of times is determined by the value of
   * {@link #maxSwitchboardTries}.  Each attempt that results in an
   * error will be printed out to <code>System.err</code>.
   */
  protected void getSwitchboard(String name, boolean verbose)
    throws MalformedURLException {
    boolean autoFlush = true;
    getSwitchboard(name, verbose, new PrintWriter(System.err, autoFlush));
  }

  /**
   * Repeatedly tries to get the switchboard stub from the bootstrap registry.
   * The number of times is determined by the value of
   * {@link #maxSwitchboardTries}.  If <code>verbose</code> is
   * <code>true</code>, each attempt that results in an
   * error will be printed out to the specified <code>PrintWriter</code>.
   *
   * @param name the name of the switchboard in the bootstrap registry
   * @param verbose if <code>true</code>, indicates to print out
   * each failed attempt to get the switchboard to the specified error
   * writer
   * @param err the character stream to which to output error messages
   */
  protected void getSwitchboard(String name, boolean verbose,
				PrintWriter err)
    throws MalformedURLException {
    switchboardName = name;
    int tries = maxSwitchboardTries;
    boolean tryInfinitely = tries == infiniteTries;
    boolean success = false;
    for (int i = 0; !success && (tryInfinitely || i < tries); i++) {
      if (timeToDie)
	break;
      try {
	switchboard = (SwitchboardRemote)Naming.lookup(name);
	// actually try the switchboard, to make sure its not an old stub
	switchboard.getKeepAliveMaxRetries();
	success = true;
      }
      catch (RemoteException re) {
	if (verbose)
	  err.println("couldn't get switchboard: " + re);;
      }
      catch (NotBoundException nbe) {
	if (verbose)
	  err.println("couldn't get switchboard: " + nbe);;
      }
    }
  }

  /**
   * Repeatedly tries the specified number of times to get the switchboard
   * stub from the bootstrap registry.
   *
   * @param name the name of the switchboard in the bootstrap registry
   * @param tries the number of times to try to get the switchboard from
   * the bootstrap registry
   * @return a stub from which to access the switchboard
   *
   * @throws MalformedURLException if the specified name is a malformed URL
   */
  public static SwitchboardRemote getSwitchboard(String name, int tries)
    throws MalformedURLException {
    return getSwitchboard(name, tries, false);
  }

  /**
   * Repeatedly tries the specified number of times to get the switchboard
   * stub from the bootstrap registry.
   *
   * @param name the name of the switchboard in the bootstrap registry
   * @param tries the number of times to try to get the switchboard from
   * the bootstrap registry
   * @param verbose if <code>true</code>, indicates to print error messages
   * to <code>System.err</code> if an exception is raised during any of the
   * attempts to get the switchboard
   * @return a stub from which to access the switchboard
   *
   * @throws MalformedURLException if the specified name is a malformed URL
   */
  public static SwitchboardRemote getSwitchboard(String name, int tries,
						 boolean verbose)
    throws MalformedURLException {
    boolean autoFlush = true;
    return getSwitchboard(name, tries, verbose,
			  new PrintWriter(System.err, autoFlush));
  }

  /**
   * Repeatedly tries the specified number of times to get the switchboard
   * stub from the bootstrap registry.
   *
   * @param name the name of the switchboard in the bootstrap registry
   * @param tries the number of times to try to get the switchboard from
   * the bootstrap registry
   * @param verbose if <code>true</code>, indicates to print error messages
   * to the specified <code>PrintWriter</code> if an exception is raised
   * during any of the attempts to get the switchboard
   * @param err the writer to which error messages should be printed if
   * <code>verbose</code> is <code>true</code> (no messages are printed if
   * <code>verbose</code> is <code>false</code>)
   * @return a stub from which to access the switchboard
   *
   * @throws MalformedURLException if the specified name is a malformed URL
   */
  public static SwitchboardRemote getSwitchboard(String name, int tries,
						 boolean verbose,
						 PrintWriter err)
    throws MalformedURLException {
    if (tries < 1) {
      String msg = "AbstractSwitchboardUser.getSwitchboard: " +
		   "tries must be greater than zero";
      throw new IllegalArgumentException(msg);
    }
    SwitchboardRemote switchboard = null;
    boolean success = false;
    for (int i = 0; !success && i < tries; i++) {
      try {
	switchboard = (SwitchboardRemote)Naming.lookup(name);
	// actually try the switchboard, to make sure its not an old stub
	switchboard.getKeepAliveMaxRetries();
	success = true;
      }
      catch (RemoteException re) {
	if (verbose)
	  err.println("couldn't get switchboard: " + re);;
      }
      catch (NotBoundException nbe) {
	if (verbose)
	  err.println("couldn't get switchboard: " + nbe);;
      }
    }
    return switchboard;
  }

  /** Sets the className data member to the name of the runtime class
      of this instance. */
  private final void setClassName() { className = getClass().getName(); }

  /** Returns the host on which this switchboard user is running. */
  public String host() throws RemoteException {
    String thisHost = null;
    try {
      thisHost = java.net.InetAddress.getLocalHost().getHostName();
    }
    catch (java.net.UnknownHostException uhe) {
      throw new java.rmi.UnknownHostException(className, uhe);
    }
    return thisHost;
  }

  /**
   * Does nothing; called by the default implementation of {@link
   * #unexportWhenDead}.  If a subclass has additional cleanup to be done
   * prior to unexporting, this method should be overridden.
   */
  protected void cleanup() {}

  /**
   * Unexports this object when it considers itself dead.
   * This method waits on {@link #dieSynch}, continually checking the
   * {@link #timeToDie} flag.  When this flag is true, it means the
   * {@link #die} method has been invoked.  This method then grabs
   * the lock on <code>dieSynch</code> one last time, waiting
   * a maximum of {@link #timeout}, or {@link #defaultTimeout} if
   * <code>timeout</code> is 0.
   * <p>
   * <b>N.B.</b>: Concrete subclasses should either devote a separate
   * thread to call this method immediately after construction, or
   * ensure that it is called as the last method in the subclass'
   * main thread.
   */
  public void unexportWhenDead() throws RemoteException {

    synchronized (dieSynch) {
      while (!timeToDie)
	try { dieSynch.wait(); }
	catch (InterruptedException ie) { }
    }

    if (!dead)
      System.err.println("it is time to die; waiting " + timeout + " ms");
    else
      System.err.println("asked to die right now!");

    int waitTime = nonZeroTimeout();

    // note the "if" instead of a "while": this is because we never want
    // to wait more than once, and since, because of coordination
    // using timeToDie, we could only get to this point if we're
    // waiting for alive to be called (in other words, no other method
    // other than alive should invoke notifyAll on dieSynch at this point)
    if (!dead) {
      synchronized (dieSynch) {
	try {dieSynch.wait(waitTime); }
	catch (InterruptedException ie) { }
	dead = true; // ensure that condition is met at this point
      }
    }

    cleanup();

    System.err.print(className + ": unexporting self...");
    System.err.flush();

    UnicastRemoteObject.unexportObject(this, false);

    System.err.println("done");

    switchboard = null;
  }

  /**
   * Tells the switchboard user to commit suicide.  This method will
   * typically be invoked by the switchboard when object processing
   * is complete, but may also be invoked by a switchboard user itself
   * to commit suicide.  Typically, if a switchboard user wishes to
   * commit suicide gracefully, it should invoke this method on itself
   * with a <code>now</code> value of <code>false</code>, to allow the
   * switchboard a final successful call to <code>alive</code>, which
   * will return false.  The switchboard will typically invoke this
   * method on its users with a <code>now</code> value of
   * <code>true</code>.
   *
   * @param now if <code>false</code>, indicates that this switchboard user
   * should wait gracefully for a final invocation by the switchboard
   * of its {@link #alive} method, so that it can return false; otherwise,
   * this method should cause the switchboard user to die as soon as possible
   */
  public void die(boolean now) throws RemoteException {
    synchronized (dieSynch) {
      timeToDie = true;
      if (now)
	dead = true;
      dieSynch.notifyAll();
    }
    synchronized(aliveSynch) {
      aliveSynch.notifyAll();
    }
  }

  public boolean alive() throws RemoteException {
    if (!timeToDie) {
      // keep Alive thread happy
      aliveRenewed = true;
      synchronized (aliveSynch) {
	aliveSynch.notifyAll();
      }
      return true;
    }
    else {
      if (!dead) {
	synchronized (dieSynch) {
	  dead = true;
	  dieSynch.notifyAll();
	  return false;
	}
      }
      else {
	// someone must have called die(true), and so the
	// unexportWhenDead sequence has already begun
	return false;
      }
    }
  }

  /**
   * Returns the value of {@link #timeout} (set at construction time),
   * or {@link #defaultTimeout} if <code>timeout</code> is 0.
   */
  protected final int nonZeroTimeout() {
    return (timeout == 0 ? defaultTimeout : timeout);
  }

  public int id() throws RemoteException { return id; }

  /**
   * Sets {@link #aliveTimeout} to be a reasonable value.  This
   * default implementation sets <code>aliveTimeout</code> to be
   * twice the maximum time the switchboard will try to call
   * a users' {@link #alive} method.  That is,
   * the value set by this method is equal to the value of the expression
   * <pre>
   * 2 * ((switchboard.getKeepAliveMaxRetries() + 1) *
   *      switchboard.getKeepAliveInterval());
   * </pre>
   */
  protected void getAliveTimeout() throws RemoteException {
    aliveTimeout =
      2 * ((switchboard.getKeepAliveMaxRetries() + 1) *
	   switchboard.getKeepAliveInterval());
  }

  /**
   * Starts a thread using the {@link AbstractSwitchboardUser.Alive} class
   * to detect when the switchboard goes down and re-register this user.
   */
  protected void startAliveThread()
    throws RemoteException {
    if (!registered)
      throw new RemoteException(className + ": startAliveThread: cannot " +
				"be called until registered with switchboard");
    getAliveTimeout();
    new Thread(new Alive(), "Alive thread for SBUser No. " + id).start();
  }

  abstract protected void reRegister() throws RemoteException;
}
