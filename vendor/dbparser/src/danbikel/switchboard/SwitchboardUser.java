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

import java.rmi.*;

/**
 * An interface that both types of users of the switchboard (clients
 * and servers) must implement, providing useful information about the
 * switchboard user, as well as a means to determine whether the
 * switchboard user is alive and to tell it when it is allowed to die.
 * <p>
 * Concrete implementations of this interface must ensure that they
 * appropriately deal with switchboard failures.  An acceptable
 * response to a switchboard failure is either simply to commit
 * suicide, or to wait patiently for a new switchboard to become
 * available.  The latter option is preferable when developing
 * fault-tolerant systems.  Detection of a swtichboard's failure will
 * necessarily occur as a <code>RemoteException</code> during an
 * invocation of one of the switchboard's methods.  Because client and
 * server state (such as unique IDs) is <i>not</i> maintained by the
 * switchboard in its log file, it is necessary for switchboard users
 * to re-register before re-invoking the method that had failed.
 * Another consequence of the lack of persistence of switchboard user
 * state is that if a client is in the middle of processing an object
 * obtained from the switchboard when the switchboard goes down, it
 * should cease processing, re-register and get a new object via the
 * {@link SwitchboardRemote#nextObject} method.  The
 * <code>AbstractSwitchboardUser</code> class provides methods to make
 * compliance with the general contract of this interface straightforward
 * for concrete subclasses.
 * <p>
 * The interface hierarchy of <code>SwitchboardUser</code>, <code>Client</code>
 * and <code>Server</code> is mirrored by the implementation hierarchy
 * of <code>AbstractSwitchboardUser</code>, <code>AbstractClient</code>
 * and <code>AbstractServer</code>:
 * <pre>
 *   Interface Hierarchy          Implementation Hierarchy
 *   -------------------          ------------------------
 *     SwitchboardUser             AbstractSwitchboardUser
 *       /        \                  /             \
 *      /          \                /               \
 *   Client      Server       AbstractClient   AbstractServer
 * </pre>
 * The purpose of the abstract implementation hierarchy is to provide
 * convenient default implementations that comply with their respective
 * interfaces' general contracts.
 * <p>
 * <b>A note on fault tolerance</b>: In order to ensure the fault-tolerance
 * of switchboard users, concrete implementors of this interface should
 * ensure that they use socket factories that set the <tt>SO_TIMEOUT</tt>
 * values of their TCP/IP sockets to some integer greater than 0.  This
 * will mean that the Java Remote Method Protocol (JRMP) implementation
 * can never hang when a remote object crashes (as is possible with
 * Sun's JRMP implementation).  {@link danbikel.util.TimeoutSocketFactory 
 * TimeoutSocketFactory} is one such factory for creating both client- and
 * server-side sockets.  Implementors that use sockets other than TCP/IP
 * sockets should have similar non-infinite timeouts.
 *
 * @see Client
 * @see Server
 * @see AbstractSwitchboardUser
 */
public interface SwitchboardUser extends Remote {

  /** Returns the ID number for this switchboard user. */
  public int id() throws RemoteException;

  /** Returns the hostname on which this switchboard user is running. */
  public String host() throws RemoteException;

  /**
   * Tells the switchboard user to commit suicide.  This method will
   * typically be invoked by the switchboard when sentence processing
   * is complete, but may also be invoked by a switchboard user itself
   * to commit suicide.  Typically, if a switchboard user wishes to
   * commit suicide gracefully, it should invoke this method on itself
   * with a <code>now</code> value of <code>false</code>, to allow the
   * switchboard a final successful call to <code>alive</code>, which
   * will return false.  The switchboard will typically invoke this
   * method on its users with a <code>now</code> value of
   * <code>true</code>.
   * <p>
   * <b>Important synchronization note</b>: This method should be
   * non-blocking.  That is, it shouldn't wait for some switchboard
   * resource or information to become available before returning.
   * If this condition is not met, then deadlock could occur.
   * <p>
   *
   * @param now if <code>false</code>, indicates that this switchboard user
   * should wait gracefully for a final invocation by the switchboard
   * of its {@link #alive} method, so that it can return false; otherwise,
   * this method should cause the switchboard user to die as soon as possible
   */
  public void die(boolean now) throws RemoteException;

  /**
   * A simple "ping" method for the switchboard to continually make
   * sure its users are alive.  Implementations are required to allow
   * one last call returning <code>false</code> after invocation of
   * <code>die(false)</code> to indicate to switchboard that user is
   * dying safely.  However, implementations should also not wait
   * indefinitely for this last call, instead using an appropriate
   * maximum timeout value (such as the timeout of the transport
   * layer, if available) before unexporting themselves.
   *
   * @see AbstractSwitchboardUser
   */
  public boolean alive() throws RemoteException;
}
