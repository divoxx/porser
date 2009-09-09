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
 * A factory for returning <code>TextObjectWriter</code> objects.
 * This factory is provided as a convenient output mechanism to
 * complement custom <code>ObjectReader</code> factories that are
 * using character-based input.
 *
 * @see TextObjectWriter */
public class TextObjectWriterFactory implements ObjectWriterFactory {

  /** Constructs a new <code>TextObjectWriterFactory</code>. */
  public TextObjectWriterFactory() {
  }

  public ObjectWriter get(OutputStream os,
			  boolean append, boolean emptyFile)
    throws IOException {
    return new TextObjectWriter(os);
  }

  public ObjectWriter get(OutputStream os, String encoding, int bufSize,
			  boolean append, boolean emptyFile)
    throws IOException {
    return new TextObjectWriter(os, encoding, bufSize);
  }

  public ObjectWriter get(String filename, String encoding, int bufSize,
			  boolean append)
    throws IOException {
    return new TextObjectWriter(filename, encoding, bufSize, append);
  }
}
