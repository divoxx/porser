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
 * A semantic marker for <code>SwitchboardUser</code> implementors that
 * are clients; also, specifies client-specific methods for the switchboard
 * to use.  Clients request and are assigned <code>Server</code> objects by the
 * switchboard.  Clients may also use the object-server facilities of the
 * switchboard, requesting objects for processing, processing those objects
 * and then sending the processed objects back to the switchboard for
 * output (via the Switchboard's {@link SwitchboardRemote#nextObject(int)
 * nextObject} and
 * {@link SwitchboardRemote#putObject(int,NumberedObject,long) putObject}
 * methods).
 * <p>
 * Implementors should ensure that they get
 * {@link SwitchboardRemote#clientNextObjectInterval} from the settings
 * of the switchboard, for use when processing objects.  This is accomplished
 * in <code>AbstractClient</code> by the
 * {@link AbstractClient#setNextObjectInterval} method.
 * <p>
 * <b>A note on fault tolerance</b>: In order to ensure the
 * fault-tolerance of clients, implementors should ensure that they
 * use socket factories that set the <tt>SO_TIMEOUT</tt> values of
 * their TCP/IP sockets to some integer greater than 0, by providing
 * custom socket factories that provide at least the functionality of
 * {@link danbikel.util.TimeoutSocketFactory TimeoutSocketFactory}.
 * Subclasses that use sockets other than TCP/IP sockets should have
 * similar non-infinite timeouts.
 *
 * @see Server
 * @see SwitchboardRemote
 * @see AbstractClient
 */
public interface Client extends SwitchboardUser {
  /**
   * Tells a client that its server has died.  If the specified server
   * ID is the same as that of the current server for the client, the
   * client may respond either by eventually requesting a new server,
   * or by committing suicide, by calling their implementation of
   * {@link SwitchboardUser#die}.
   * <p>
   * <b>Important synchronization note</b>: This method should be
   * non-blocking.  That is, it shouldn't wait for some switchboard
   * resource (such as a new server) or information to become
   * available before returning.  If this condition is not met, then
   * deadlock could occur.
   * <p> */
  public void serverDown(int serverId) throws RemoteException;
}
