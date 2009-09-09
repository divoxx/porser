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
 * A specification for constructing <code>ObjectReader</code> instances.
 * This type of factory is used by the switchboard.
 *
 * @see ObjectReader
 * @see Switchboard
 */
public interface ObjectReaderFactory {
  /**
   * Gets a new object reader for the specified input stream, using
   * a default character encoding and buffer size, if applicable.
   */
  public ObjectReader get(InputStream in) throws IOException;
  
  /**
   * Gets a new object reader for the specified input stream.
   * If the implementation is character-based, the specified encoding
   * should be used; otherwise, the encoding argument should be
   * ignored.  Implementations should use buffering for their underlying read
   * operations, using the specified buffer size (if possible).
   */
  public ObjectReader get(InputStream in, String encoding,
			  int bufSize)
    throws IOException;

  /**
   * Gets a new object reader for the specified filename.
   * If the implementation is character-based, the specified encoding
   * should be used; otherwise, the encoding argument should be
   * ignored.  Implementations should use buffering for their underlying read
   * operations, using the specified buffer size (if possible).
   */
  public ObjectReader get(String filename, String encoding,
			  int bufSize)
    throws IOException;
}
