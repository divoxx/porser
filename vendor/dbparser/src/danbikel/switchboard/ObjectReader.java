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
 * Specifies methods for reading objects from an underlying
 * <code>Reader</code> or <code>InputStream</code> object.
 * This interface contains a strict subset of the methods specified
 * in <code>ObjectInput</code>, making it easy to adapt classes that
 * already implement <code>ObjectInput</code> to become implementors
 * of this interface.
 *
 * @see ObjectReaderFactory
 */
public interface ObjectReader {
  /**
   * Reads and returns the next object from the underlying
   * <code>Reader</code> or stream.
   *
   * @return the next object of the underlying <code>Reader</code> or stream, or
   * <code>null</code> if the end of the file or stream has been
   * reached
   *
   * @throws IOException if the underlying <code>Reader</code> or
   * input stream throws an <code>IOException</code>
   */
  public Object readObject() throws IOException;

  /**
   * Closes the underlying stream or <code>Reader</code> of this
   * <code>ObjectReader</code> object.
   */
  public void close() throws IOException;
}
