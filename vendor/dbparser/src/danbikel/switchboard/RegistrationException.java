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
 * An exception raised when a switchboard user cannot be registered
 * properly with the switchboard.
 *
 * @see SwitchboardRemote
 * @see Switchboard
 */
public class RegistrationException extends RemoteException {
  /** Constructs a new <code>RegistrationException</code>. */
  public RegistrationException() { super(); }
  /** Constructs a new <code>RegistrationException</code> with the specified
      message. */
  public RegistrationException(String msg) { super(msg); }
  /** Constructs a new <code>RegistrationException</code> with the specified
      message and nested exception. */
  public RegistrationException(String msg, Throwable ex) { super(msg, ex); }
}
