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

/**
 * Specifies methods for writing objects to an unerlying
 * <code>Writer</code> or <code>OutputStream</code> object.
 * This interface contains a strict subset of the methods specified
 * in <code>ObjectOutput</code>, making it easy to adapt classes that
 * already implement <code>ObjectOutput</code> to become implementors
 * of this interface.
 */
public interface ObjectWriter {
  /**
   * Closes the underlying stream or <code>Writer</code> of this
   * <code>ObjectWriter</code> object.
   */
  public void close() throws IOException;

  /**
   * Writes the specified object to the underlying stream or
   * <code>Writer</code>.
   *
   * @throws IOException if the underlying <code>Writer</code> or
   * output stream throws an <code>IOException</code>
   */
  public void writeObject(Object obj) throws IOException;
}
