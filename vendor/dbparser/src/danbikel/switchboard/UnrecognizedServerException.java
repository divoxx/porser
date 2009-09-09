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
 * An exception raised when a switchboard method that has a server ID
 * parameter is called with an invalid server ID.  This exception
 * is thrown, for example, by the {@link Switchboard#getServer(int,int)}
 * method if a client requests an invalid server.
 *
 * @see SwitchboardRemote
 * @see Switchboard
 */
public class UnrecognizedServerException extends RemoteException {
  /** Constructs a new <code>UnrecognizedServerException</code>. */
  public UnrecognizedServerException() { super(); }
  /** Constructs a new <code>UnrecognizedServerException</code> with the
      specified message. */
  public UnrecognizedServerException(String msg) { super(msg); }
  /** Constructs a new <code>UnrecognizedServerException</code> with the
      specified message and nested exception. */
  public UnrecognizedServerException(String msg, Throwable ex) {
    super(msg, ex);
  }
}
