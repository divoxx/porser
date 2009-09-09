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
    package danbikel.lisp;

import java.io.*;
import java.util.zip.*;
import java.net.URL;

/**
 * A class for tokenizing simple S-expressions, where there are only strings
 * delimited by whitespace or parentheses (as implemented by {@link
 * WordTokenizer}).  Comments are lines where the first non-whitespace
 * character is a semicolon (the character ';').
 */
public class SexpTokenizer extends WordTokenizer {

  private static InputStream streamFromFile(File file)
    throws FileNotFoundException, IOException {
    InputStream is = new FileInputStream(file);
    return file.getName().endsWith(".gz") ? new GZIPInputStream(is) : is;
  }
  private static InputStream streamFromFile(String filename)
    throws FileNotFoundException, IOException {
    InputStream is = new FileInputStream(filename);
    return filename.endsWith(".gz") ? new GZIPInputStream(is) : is;
  }

  /**
   * Constructs a <code>SexpTokenizer</code> with the specified stream
   * and comment-recognition option.
   *
   * @param inStream the input stream for this tokenizer
   * @param comments indicates whether to recognizes comment lines
   * 
   * @see #commentChar(int)
   */
  public SexpTokenizer(Reader inStream, boolean comments) {
    super(inStream);
    ordinaryChar('(');
    ordinaryChar(')');
    if (comments)
      commentChar(';');
  }

  /**
   * Constructs a <code>SexpTokenizer</code> with the specified stream,
   * comment-recognition option and set of ordinary characters, which are
   * actually those characters to be treated as metacharacters (i.e., not
   * part of tokens, but delimiters of tokens).
   *
   * @param inStream the input stream for this tokenizer
   * @param comments indicates whether to recognizes comment lines
   * @param ordinary an array of all characters to be treated as metacharacters
   *
   * @see #ordinaryChar(char)
   * @see #commentChar(int)
   */
  public SexpTokenizer(Reader inStream, boolean comments, char[] ordinary) {
    super(inStream);
    for (int i = 0; i < ordinary.length; ++i)
      ordinaryChar(ordinary[i]);
    if (comments)
      commentChar(';');
  }

  /**
   * Constructs a <code>SexpTokenizer</code> with the specified stream
   * and the default comment-recognition option, which is <code>true</code>.
   *
   * @param inStream the input stream for this tokenizer
   */
  public SexpTokenizer(Reader inStream) {
    this(inStream, true);
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   * The tokenizer will recognize comment lines.
   *
   * @param filename the name of the file containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   *
   * @throws UnsupportedEncodingException if the specified encoding is not
   * supported
   * @throws FileNotFoundException if the specified file does not exist
   */
  public SexpTokenizer(String filename, String encoding, int bufSize)
    throws UnsupportedEncodingException, FileNotFoundException, IOException {
    this(new BufferedReader(new InputStreamReader(streamFromFile(filename),
						  encoding),
			    bufSize));
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   *
   * @param filename the name of the file containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   * @param comments whether this tokenizer will recognize comments
   *
   * @throws UnsupportedEncodingException if the specified encoding is not
   * supported
   * @throws FileNotFoundException if the specified file does not exist
   */
  public SexpTokenizer(String filename, String encoding, int bufSize,
		       boolean comments)
    throws UnsupportedEncodingException, FileNotFoundException, IOException {
    this(new BufferedReader(new InputStreamReader(streamFromFile(filename),
						  encoding),
			    bufSize),
	 comments);
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   * The tokenizer will recognize comment lines.
   *
   * @param file the file containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   *
   * @throws UnsupportedEncodingException if the specified encoding is not
   * supported
   * @throws FileNotFoundException if the specified file does not exist
   */
  public SexpTokenizer(File file, String encoding, int bufSize)
    throws UnsupportedEncodingException, FileNotFoundException, IOException {
    this(new BufferedReader(new InputStreamReader(streamFromFile(file),
						  encoding),
			    bufSize));
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>FileInputStream</code>.
   *
   * @param file the file containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   * @param comments whether this tokenizer will recognize comments
   *
   * @throws UnsupportedEncodingException if the specified encoding is not
   * supported
   * @throws FileNotFoundException if the specified file does not exist
   */
  public SexpTokenizer(File file, String encoding, int bufSize,
		       boolean comments)
    throws UnsupportedEncodingException, FileNotFoundException, IOException {
    this(new BufferedReader(new InputStreamReader(streamFromFile(file),
						  encoding),
			    bufSize),
	 comments);
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>InputStreamReader</code>.
   * The tokenizer will recognize comment lines.
   *
   * @param stream the stream of bytes, encoded with <code>encoding</code>,
   * containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   *
   * @throws UnsupportedEncodingException if the specified encoding is not
   * supported
   */
  public SexpTokenizer(InputStream stream, String encoding, int bufSize)
    throws UnsupportedEncodingException {
    this(new BufferedReader(new InputStreamReader(stream, encoding), bufSize));
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>InputStreamReader</code>.
   *
   * @param stream the stream of bytes, encoded with <code>encoding</code>,
   * containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * that will be created
   * @param comments whether this tokenizer will recognize comments
   *
   * @throws UnsupportedEncodingException if the specified encoding is not
   * supported
   */
  public SexpTokenizer(InputStream stream, String encoding, int bufSize,
		       boolean comments)
    throws UnsupportedEncodingException {
    this(new BufferedReader(new InputStreamReader(stream, encoding), bufSize),
	 comments);
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around an <code>InputStreamReader</code>
   * around the stream created by calling <code>url.openStream()</code>.
   * The tokenizer will recognize comment lines.
   *
   * @param url the url from which to get the stream containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   *
   * @throws UnsupportedEncodingException if the specified encoding is not
   * supported
   * @throws IOException if the underlying call to {@link URL#openStream()}
   * or the construction of any of the readers throws an
   * <code>IOException</code>
   */
  public SexpTokenizer(URL url, String encoding, int bufSize)
    throws UnsupportedEncodingException, IOException {
    this(new BufferedReader(new InputStreamReader(url.openStream(),
						  encoding),
			    bufSize));
  }

  /**
   * Convenience constructor, creating a <code>SexpTokenizer</code> around a
   * <code>BufferedReader</code> around a <code>InputStreamReader</code>
   * around the stream created by calling <code>url.openStream()</code>.
   * @param url the url from which to get the stream containing S-expressions
   * @param encoding the encoding of the file to be tokenized
   * @param bufSize the size of the buffer of the <code>BufferedReader</code>
   * @param comments whether this tokenizer will recognize comments
   *
   * @throws UnsupportedEncodingException if the specified encoding is not
   * supported
   * @throws IOException if the underlying call to {@link URL#openStream()}
   * or the construction of any of the readers throws an
   * <code>IOException</code>
   */
  public SexpTokenizer(URL url, String encoding, int bufSize, boolean comments)
    throws UnsupportedEncodingException, IOException {
    this(new BufferedReader(new InputStreamReader(url.openStream(),
						  encoding),
			    bufSize),
	 comments);
  }
}
