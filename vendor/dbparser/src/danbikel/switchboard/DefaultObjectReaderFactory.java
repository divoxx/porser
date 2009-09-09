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
 * The default factory used to construct <code>ObjectReader</code> objects
 * by the <code>Switchboard</code> class.  This class returns
 * <code>DefaultObjectReader</code> objects.
 */
class DefaultObjectReaderFactory
  implements ObjectReaderFactory {

  /** Constructs a new object reader factory. */
  public DefaultObjectReaderFactory() {
  }

  public ObjectReader get(InputStream in) throws IOException {
    return new DefaultObjectReader(in);
  }

  public ObjectReader get(InputStream in, String encoding, int bufSize)
    throws IOException {
    return new DefaultObjectReader(in, encoding, bufSize);
  }

  public ObjectReader get(String filename, String encoding,
			  int bufSize)
    throws IOException {
    return new DefaultObjectReader(filename, encoding, bufSize);
  }
}
