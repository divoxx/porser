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

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Specification for a consumer of objects that have already been processed
 * by switchboard clients, allowing arbitrary post-processing in a distributed
 * object-processing run involving a switchboard.  It is guaranteed that after
 * each <code>NumberedObject</code> has been processed by some client, every
 * registered consumer's {@link #consume} method will be called with that
 * numbered object as the argument.  A consumer may collect information,
 * write information to the output file or do nothing; the action taken
 * after consumption is implementation-dependent.  It is also guaranteed
 * that when the switchboard has determined that the input file contains
 * no more objects to be processed, every registered consumer's
 * {@link #processingComplete} method will be called.  The behavior of a
 * consumer upon invocation of this method is also implementation-dependent.
 *
 * @see Switchboard#registerConsumer(Consumer)
 */
public interface Consumer extends Remote {
  /**
   * Indicates that the consumer is about to be consuming objects from
   * the specified input file.  The name of the output filename is also
   * a parameter, in case the consumer needs to create an output file
   * when this method is invoked.
   *
   * @param inputFilename the name of the input file for which object
   * processing has commenced
   * @param outputFilename the name of the (optional) output file to be
   * created from the specified input file
   */
  public void newFile(String inputFilename, String outputFilename)
    throws RemoteException;

  /**
   * Tells this consumer to consume the specified object that has been
   * processed by one of the switchboard's clients.
   *
   * @param numObj the processed switchboard object to be consumed by this
   * consumer instance
   */
  public void consume(NumberedObject numObj) throws RemoteException;

  /**
   * Indicates that processing is complete for the specified input/output
   * file pair.
   *
   * @param inputFilename the name of the current input file
   * @param outputFilename the name of the output file
   */
  public void processingComplete(String inputFilename, String outputFilename)
    throws RemoteException;
}