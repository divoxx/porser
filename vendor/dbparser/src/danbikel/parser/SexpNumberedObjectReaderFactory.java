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
    package danbikel.parser;

import danbikel.switchboard.*;
import java.io.*;

/**
 * The default <code>NumberedSentenceReaderFactory</code> used by
 * <code>Switchboard</code>.  This class provides new instances of
 * <code>SexpNumberedSentenceReader</code> objects.
 *
 * @see SexpNumberedObjectReader
 */
public class SexpNumberedObjectReaderFactory
  implements ObjectReaderFactory {

  /** Constructs a new factory. */
  public SexpNumberedObjectReaderFactory() {
  }

  /**
   * Returns a new {@link SexpNumberedObjectReader} constructed with the
   * specified input stream argument.
   *
   * @param in the input stream around which to construct a new
   * {@link SexpNumberedObjectReader}
   *
   * @return a new {@link SexpNumberedObjectReader} constructed with the
   * specified input stream argument
   */
  public ObjectReader get(InputStream in) throws IOException {
    return new SexpNumberedObjectReader(in);
  }

  /**
   * Returns a new {@link SexpNumberedObjectReader} constructed with the
   * specified arguments.
   * @param in the input stream around which to construct the returned
   * {@link SexpNumberedObjectReader}
   * @param encoding the character encoding to use for reading S-expressions
   * from the specified input stream
   * @param bufSize the buffer size for the S-expression reader in the returned
   * {@link SexpNumberedObjectReader} to use
   * @return a new {@link SexpNumberedObjectReader} constructed with the
   * specified arguments
   * @throws IOException if there is a problem constructing a new
   * {@link SexpNumberedObjectReader} using the specified arguments of
   * this method
   */
  public ObjectReader get(InputStream in, String encoding,
			  int bufSize)
    throws IOException {
    return new SexpNumberedObjectReader(in, encoding, bufSize);
  }


  /**
   * Returns a new {@link SexpNumberedObjectReader} constructed with the
   * specified arguments.
   * @param filename the name of the file around which to construct the returned
   * {@link SexpNumberedObjectReader}
   * @param encoding the character encoding to use for reading S-expressions
   * from the specified input stream
   * @param bufSize the buffer size for the S-expression reader in the returned
   * {@link SexpNumberedObjectReader} to use
   * @return a new {@link SexpNumberedObjectReader} constructed with the
   * specified arguments
   * @throws IOException if there is a problem constructing a new
   * {@link SexpNumberedObjectReader} using the specified arguments of
   * this method
   */
  public ObjectReader get(String filename, String encoding,
			  int bufSize)
    throws IOException {
    return new SexpNumberedObjectReader(filename, encoding, bufSize);
  }
}
