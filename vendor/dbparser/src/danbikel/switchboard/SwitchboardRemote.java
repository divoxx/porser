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

import java.io.*;
import java.util.*;
import java.rmi.*;

/**
 * The methods by which both clients and servers register with a
 * single RMI-accessible Switchboard object.  Clients and servers both
 * get their settings from the switchboard, and then, upon a client's
 * request, the switchboard assigns the client to a server.
 * Implementations are encouraged to load-balance when assigning
 * clients to servers, typically via assigning a client to the server
 * with the least number of clients.  The switchboard also serves as a
 * "object server" for clients.
 *
 * @see SwitchboardUser
 */
public interface SwitchboardRemote extends Remote {
  // settings constants

  /**
   * The property to specify how long, in milliseconds, the SO_TIMEOUT
   * value should be for the switchboard's RMI-client (caller) sockets.
   * <p>
   * The value of this constant is
   * <code>"switchboard.socketTimeout"</code>.
   *
   * @see Switchboard#setSettings(Properties)
   */
  public final static String socketTimeout = "switchboard.socketTimeout";

  /**
   * The property to specify how often clients and servers should ping
   * the "keep-alive" socket connected to the switchboard.  The value
   * of this property should be (the string representation of) an
   * integer, representing milliseconds between pings.
   * <p>
   * The value of this constant is
   * <code>"switchboard.keepAliveInterval"</code>.
   *
   * @see Switchboard#setSettings(Properties)
   */
  public final static String keepAliveInterval =
    "switchboard.keepAliveInterval";
  /**
   * The property to specify at most how many times the switchboard attempts to
   * contact clients and servers before considering them dead (after an
   * initial failure, thus making 0 a legal value for this property).
   * <p>
   * The value of this constant is
   * <code>"switchboard.keepAliveMaxRetries"</code>.
   *
   * @see Switchboard#setSettings(Properties)
   */
  public final static String keepAliveMaxRetries =
    "switchboard.keepAliveMaxRetries";

  /**
   * The property to specify whether the switchboard should kill all
   * of a server's clients when it detects that the server has died.
   * This property should have the value <tt>"false"</tt> when servers
   * are stateless. The value of this property should be (the string
   * representation of) a boolean (conversion is performed by the
   * method <code>Boolean.valueOf</code>).
   * <p>
   * The value of this constant is
   * <code>"switchboard.serverDeathKillClients"</code>.
   *
   * @see Switchboard#setSettings(Properties)
   */
  public final static String serverDeathKillClients =
    "switchboard.serverDeathKillClients";

  /**
   * The property to specify whether the switchboard should sort the log
   * file entries when creating its final output file.  The default
   * behavior is to sort.
   *
   * @see Switchboard#setSettings(Properties)
   */
  public final static String sortOutput =
    "switchboard.sortOutput";

  public final static String switchboardPolicyFile =
    "switchboard.policyFile";

  public final static String switchboardDisableHttp =
    "switchboard.disableHttp";

  /**
   * The property to specify the interval, in milliseconds, between
   * client requests for an object to process, after {@link #nextObject(int)}
   * returns <code>null</code>.  This value of this property should be
   * used by all implementations of the <code>Client</code> interface
   * when complying with the general contract of the {@link #nextObject(int)}
   * method.
   * <p>
   * Note that this property does not apply to the switchboard
   * itself, but only to clients, and therefore its value is not set
   * internally by {@link Switchboard#setSettings(Properties)}.
   * <p>
   * The value of this constant is
   * <code>"switchboard.client.nextObjectInterval"</code>.
   *
   * @see #nextObject(int)
   */
  public final static String clientNextObjectInterval =
    "switchboard.client.nextObjectInterval";

  public final static String clientPolicyFile =
    "switchboard.client.policyFile";

  public final static String clientDisableHttp =
    "switchboard.client.disableHttp";

  public final static String serverPolicyFile =
    "switchboard.server.policyFile";

  public final static String serverDisableHttp =
    "switchboard.server.disableHttp";

  /**
   * Gets the value for the specified  setting (property) from the internal
   * settings (<code>Properties</code>) object of the switchboard.
   */
  public String getSetting(String settingName) throws RemoteException;

  /**
   * Gets the settings contained within this switchboard, so
   * that clients and servers all have the same settings.
   */
  public Properties getSettings() throws RemoteException;

  /**
   * Registers a client with the switchboard.  The switchboard
   * may optionally output a message about the registration to a
   * log file.
   *
   * @param client the client that is registering
   * @return a unique ID for the registering client that is
   * greater than or equal to 0
   *
   * @throws RegistrationException if there is a problem during
   * registration
   */
  public int register(Client client) throws RemoteException;

  /**
   * Register a server with the switchboard.
   * <p>
   * The <code>Server</code> interface has a method allowing the
   * server to specify a maximum number of clients it is willing to
   * accept, {@link Server#maxClients}.  If the return value of the
   * server's <code>maxClients</code> method is {@link
   * Server#acceptUnlimitedClients}, then the server is registered to
   * accept an essentially limitless number of clients (the default is
   * currently 100,000).
   * <p>
   * If the server's {@link Server#acceptClientsOnlyByRequest} method
   * returns <code>true</code>, a client can gain access to the server
   * only by requesting it directly, via the {@link
   * #getServer(int,int)} method.  This allows a client-server pair to
   * "arrange" to be hooked together, such as when it is desirable to
   * have a single server per client.  In such a scenario, a server
   * whose <code>acceptClientsOnlyByRequest</code> method returns
   * <code>true</code> would register, then pass its server ID number
   * to a client so that the client could specifically request this
   * server using <code>getServer(int,int)</code>, whereby the
   * switchboard would be informed as to their connection.  If the
   * client and server making this arrangement are both running on the
   * same host, the client could optionally make all method calls
   * directly to the server, instead of via RMI.  In such a scheme,
   * the switchboard becomes merely an object server.
   *
   * @param server the server being registered
   * @return a unique ID for the registering server that is greater
   * than or equal to 0
   *
   * @throws RegistrationException if there is a problem during
   * registration, including if the <code>Server.maxClients</code>
   * method does not return either a non-zero positive integer or
   * <code>Server.acceptUnlimitedClients</code>.
   *
   * @see Server#maxClients
   * @see #getServer(int,int)
   */
  public int register(Server server) throws RemoteException;

  /**
   * Returns a <code>Server</code> for use by a client.  If the
   * switchboard contains multiple servers, it should try to
   * load-balance them, typically by assigning the requesting client
   * the server with the lowest load.  This method should typically be
   * called once per life of the client.  The switchboard may
   * optionally record the connection between the client and server to
   * a log file.
   * <p>
   * It is guaranteed that if a valid server is found, its data will
   * be updated by calling its <code>maxClients</code> and
   * <code>acceptClientsOnlyByRequest</code> methods and caching their
   * values.  This method may also update other servers' data.
   * <p>
   * While a primary purpose of this method is for clients to get a
   * server initially, or simply when their current server fails, this
   * method may be called in the middle of a run simply to provide
   * dynamic load-balancing.  Clients may, for example, implement a
   * policy of obtaining a new server prior to the processing of each
   * object (if they are using the object-serving functionality of the
   * switchboard).  With such a policy, as new servers are brought up
   * and registered, existing clients using more heavily-loaded
   * servers will be migrated to those new, initially-lightly-loaded
   * servers until the loads are as balanced as possible.
   *
   * @param clientId the ID of the client invoking this method to
   * request a server
   * @return a <code>Server</code> for use by the caller,
   * or <code>null</code> if there are currently no servers registered with this
   * switchboard, if all servers are fully loaded with their maximum
   * number of clients or are only accepting clients by request
   *
   * @throws UnrecognizedClientException if the specified client ID is not valid
   *
   * @see Server#maxClients
   * @see Server#acceptClientsOnlyByRequest
   */
  public Server getServer(int clientId) throws RemoteException;

  /**
   * Returns a <code>Server</code> associated with the
   * specified server ID to the requesting client.  If the server
   * with the specified ID exists but its maximum number of clients
   * is greater than 0 and has been reached, then <code>null</code> is returned.
   * <p>
   * It is guaranteed that if a valid server is found,
   * its data will be updated by calling its <code>maxClients</code>
   * and <code>acceptClientsOnlyByRequest</code> methods and caching
   * their values.  Other servers' data may also be updated by this method.
   *
   * @param clientId the ID of the client invoking this method to
   * request a server
   * @param serverId the ID of the server being requested by the client
   * invoking this method
   * @return the server associated with <code>serverId</code>, or
   * <code>null</code> if the requested server has a non-zero number of
   * clients equal to its maximum (that is, if it is fully loaded)
   *
   * @throws UnrecognizedClientException if the specified client ID is not valid
   * @throws UnrecognizedServerException if the specified server ID is not valid
   *
   * @see Server#maxClients
   * @see Server#acceptClientsOnlyByRequest
   */
  public Server getServer(int clientId, int serverId)
    throws RemoteException;

  /**
   * Registers the specified consumer of processed objects with this
   * switchboard.  It is guaranteed that the consumer's
   * {@link Consumer#newFile(String,String)} method will be invoked
   * before this registration method returns.  Note that it is possible
   * that all objects from the current file could have been processed
   * before this method returns.
   *
   * @param consumer the consumer to be registered
   *
   * @see Consumer#newFile(String,String)
   */
  public void registerConsumer(Consumer consumer) throws RemoteException;

  /**
   * Gets the next object for the specified client; returns <code>null</code>
   * if there is not currently an object to be processed.  Clients should
   * continually call this method, waiting a fixed interval between calls,
   * until they are told to die.  The interval between calls should be the
   * value of the property {@link #clientNextObjectInterval}.<br>
   *
   * <b>N.B.</b>: <code>NumberedObject</code> instances contain two data
   * members that are <i>immutable</i>: the object's unique ID number, and
   * another unique ID number indicating the file from which the object was
   * read, for which there is no public accessor.  It is therefore crucial that
   * clients manipulate and use <i>the very same</i>
   * <code>NumberedObject</code> <i>instance</i> as the second argument to the
   * {@link #putObject(int,NumberedObject,long)} method.
   *
   * @param clientId the ID number of the client requesting the next object
   * @return the next object to process in the input file specified to
   * the switchboard, or <code>null</code> if there are no more objects
   *
   * @throws UnrecognizedClientException if the specified client ID is not valid
   *
   * @see #clientNextObjectInterval
   */
  public NumberedObject nextObject(int clientId) throws RemoteException;

  /**
   * Sends a processed object back to the switchboard object.<br>
   *
   * <b>N.B.</b>: <code>NumberedObject</code> instances contain two data
   * members that are <i>immutable</i>: the object's unique ID number, and
   * another unique ID number indicating the file from which the object was
   * read, for which there is no public accessor.  It is therefore crucial that
   * clients manipulate and use <i>the very same</i>
   * <code>NumberedObject</code> <i>instance</i> that was retrieved using the
   * {@link #nextObject(int)} method as the second argument to the this method.
   *
   * @param clientId the ID number of the client requesting the next object
   * @param obj the processed object and its number, or <code>null</code>
   * if the client was unable to process the object
   * @param millis the number of milliseconds the client took to process the
   * object
   *
   * @throws UnrecognizedClientException if the specified client ID is not valid
   */
  public void putObject(int clientId,
			NumberedObject obj,
			long millis) throws RemoteException;

  public int getKeepAliveInterval() throws RemoteException;
  public int getKeepAliveMaxRetries() throws RemoteException;
}
