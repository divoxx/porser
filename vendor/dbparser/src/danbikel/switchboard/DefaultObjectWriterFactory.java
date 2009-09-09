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

class DefaultObjectWriterFactory implements ObjectWriterFactory {

  public ObjectWriter get(OutputStream os,
			  boolean append, boolean emptyFile)
    throws IOException {

    boolean noHeader = append && !emptyFile;
    return (noHeader ?
	    (ObjectWriter)new DefaultNoHeaderObjectWriter(os) :
	    (ObjectWriter)new DefaultObjectWriter(os));
  }

  public ObjectWriter get(OutputStream os, String encoding, int bufSize,
			  boolean append, boolean emptyFile)
    throws IOException {
    boolean noHeader = append && !emptyFile;
    return (noHeader ?
	    (ObjectWriter)new DefaultNoHeaderObjectWriter(os, bufSize) :
	    (ObjectWriter)new DefaultObjectWriter(os, bufSize));
  }

  public ObjectWriter get(String filename, String encoding, int bufSize,
			  boolean append)
    throws IOException {
    boolean emptyFile = true;
    File file = new File(filename);
    if (file.length() > 0)
      emptyFile = false;

    FileOutputStream os = new FileOutputStream(filename, append);

    return get(os, encoding, bufSize, append, emptyFile);
  }
}
