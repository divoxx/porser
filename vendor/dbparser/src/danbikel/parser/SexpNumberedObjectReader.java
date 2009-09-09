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

import danbikel.util.Text;
import danbikel.lisp.*;
import danbikel.switchboard.*;
import java.io.*;

/**
 * Reads an underlying stream with a <code>SexpTokenizer</code>,
 * converting S-expressions of the form
 * <tt>(num&nbsp;processed&nbsp;obj)</tt>, where <code>obj</code>
 * is a <code>Sexp</code> and <tt>processed</tt> is a <code>Symbol</code>
 * whose print-name is the output of <code>String.valueOf(boolean)</code>, to
 * NumberedObject objects.
 *
 * @see SexpNumberedObjectReaderFactory
 */
public class SexpNumberedObjectReader implements ObjectReader {
  private SexpTokenizer tok;

  /**
   * Constructs a new instance, reading numbered S-expressions from the
   * specified input stream using the default character encoding.
   * @param in the input stream from which to read numbered S-expressions
   */
  public SexpNumberedObjectReader(InputStream in) {
    tok = new SexpTokenizer(new InputStreamReader(in));
  }

  /**
   * Constructs a new numbered object reader from the specified filename,
   * file encoding and buffer size, by building a <code>SexpTokenizer</code>
   * from the specified arguments.
   * @param in the input stream from which to read numbered S-expressions
   * @param encoding the character encoding to use when reading from the
   * specified file
   * @param bufSize the buffer size to use when reading from the specified
   * file
   *
   * @see SexpTokenizer#SexpTokenizer(InputStream,String,int)
   */
  public SexpNumberedObjectReader(InputStream in, String encoding, int bufSize)
    throws IOException {
    tok = new SexpTokenizer(in, encoding, bufSize);
  }


  /**
   * Constructs a new numbered object reader from the specified filename,
   * file encoding and buffer size, by building a <code>SexpTokenizer</code>
   * from the specified arguments.
   * @param filename the filename from which to read numbered S-expressions
   * @param encoding the character encoding to use when reading from the
   * specified file
   * @param bufSize the buffer size to use when reading from the specified
   * file
   *
   * @see SexpTokenizer#SexpTokenizer(String,String,int)
   */
  public SexpNumberedObjectReader(String filename, String encoding,
				  int bufSize)
    throws IOException {
    tok = new SexpTokenizer(filename, encoding, bufSize);
  }

  /**
   * Returns a {@link NumberedObject} instance constructed from the next
   * numbered S-expression in the stream or file that this reader wraps.
   * A numbered S-expression is a list of the form
   * <tt>(num&nbsp;processed&nbsp;obj)</tt>, where <code>obj</code>
   * is a <code>Sexp</code> and <tt>processed</tt> is a <code>Symbol</code>
   * whose print-name is the output of <code>String.valueOf(boolean)</code>, to
   * NumberedObject objects.
   * @return a {@link NumberedObject} instance constructed from the next
   * numbered S-expression in the stream or file that this reader wraps.
   * @throws IOException if there is a problem reading from the underlying
   * stream, or if the S-expression read is not in the proper format
   */
  public Object readObject() throws IOException {
    Sexp sent = Sexp.read(tok);
    if (sent == null)
      return null;
    if (sent.isSymbol())
      throw new IOException("Sexp is wrong type for NubmeredObject: Symbol");
    if (sent.list().size() != 3 ||
	sent.list().get(0).isList() || sent.list().get(1).isList())
      throw new IOException("Sexp has wrong format for Object");
    if (Text.isAllDigits(sent.list().get(0).toString()) == false)
      throw new IOException("first element of Sexp representing " +
			    "NubmeredObject is not all digits: " +
			    sent.list().get(0));
    int sentNum = Integer.parseInt(sent.list().get(0).toString());
    boolean processed =
      Boolean.valueOf(sent.list().get(1).toString()).booleanValue();
    return new NumberedObject(sentNum, processed, sent.list().get(2));
  }

  /**
   * Closes the underlying S-expression reader.
   * @throws IOException if there is a problem closing the underlying stream
   * used by the underlying S-expression reader
   */
  public void close() throws IOException {
    tok.close();
  }
}
