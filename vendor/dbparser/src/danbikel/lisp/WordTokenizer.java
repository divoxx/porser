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
import java.util.BitSet;

/**
 * A simple tokenizer for words only (no numbers and no significant eol chars).
 * Many of the methods and some of the data members of {@link
 * java.io.StreamTokenizer} exist in this class, but at present, this class
 * does not extend <code>StreamTokenizer</code>.  This class recognizes
 * comments as beginning with an optionally-specified comment character.
 * A comment is a line where the first non-whitespace character is the
 * comment character.
 */
public class WordTokenizer {
  private static final String className = WordTokenizer.class.getName();

  /**
   * Included as a public data member so that javadoc can resolve
   * external links to members of the <code>StreamTokenizer</code> class.
   */
  public StreamTokenizer javadocHack;

  // constants
  private final static char[] lineSep =
    System.getProperty("line.separator").toCharArray();
  private final static int lineSepLength = lineSep.length;
  private final static int initialBufSize = 1024;

  // data members

  /**
   * The type of the last token read, using the type definitions in {@link
   * java.io.StreamTokenizer}.  Because this tokenizer only reads ordinary
   * characters and words, the value of <code>ttype</code> will only ever be
   * {@link java.io.StreamTokenizer#TT_EOF}, {@link
   * java.io.StreamTokenizer#TT_WORD} or an ordinary character.
   *
   * @see #ordinaryChar
   * @see #ordinaryChars
   */
  public int ttype;

  /**
   * Contains the most recent word tokenized by this tokenizer.
   */
  public String sval;

  private boolean pushedBack = false;
  private boolean firstCharRead = false;
  private char commentChar = 0;
  private BitSet ordinary;
  private Reader inStream;
  private int lastChar;
  private int lineno = 1;
  private long numCharsRead = 0;
  private int linenoOfLastToken = 0;
  private int lineSepMatchIdx = 0;
  private StringBuilder buf = new StringBuilder(initialBufSize);

  /**
   * Creates a new tokenizer object.
   *
   * @param inStream the stream to be tokenized.
   */
  public WordTokenizer(Reader inStream) {
    this.inStream = inStream;
    ordinary = new BitSet(Byte.MAX_VALUE);
  }

  /**
   * Specifies a character to be treated as the start of a comment
   * on the current line.  The comment character must have an integer value
   * greater than 0.
   * @param ch the character to be treated as the start of a single-line comment
   */
  public void commentChar(int ch) {
    if (ch <= 0 || ch > Character.MAX_VALUE)
      throw new IllegalArgumentException(className +
					 ": commentChar out of range: " + ch);
    commentChar = (char)ch;
  }

  /**
   * Specifies a character to treated as a token delimiter,
   * to be contained in {@link #ttype} after it is read.  The character
   * must be in the range of <code>0 <= ch <= Byte.MAX_VALUE</code>.
   *
   * @param ch the character to be tested
   */
  public void ordinaryChar(char ch) {
    if (ch < 0 || ch > Byte.MAX_VALUE)
      throw new IndexOutOfBoundsException(className +
					  ": ordinaryChar called with " + ch);
    ordinary.set((int)ch);
  }

  /**
   * Specifies a range of characters to treated as a token delimiter, to be
   * contained in {@link #ttype} after it is read.  The characters
   * must be in the range of <code>0 <= ch <= Byte.MAX_VALUE</code>.
   *
   * @param low the lowest-valued character in a range to be treated as
   * token delimiters
   * @param hi the highest-valued character in a range to be treated as
   * token delimiters
   */
  public void ordinaryChars(int low, int hi) {
    if (low > hi || low > Byte.MAX_VALUE)
      throw new IndexOutOfBoundsException(className +
					  ": ordinaryChars called with " +
					  low + " and " + hi);
    int bitSetSize = ordinary.size();
    for (int i = low; i <= hi && i < bitSetSize; i++)
      ordinary.set(i);
  }


  /**
   * Reads the next token from the underlying character stream and returns
   * its type, which is also stored in {@link #ttype}.
   *
   * @return the type of the token that was just read
   * (also stored in {@link #ttype})
   * @throws IOException if there was a problem reading the next token
   * from the underlying stream
   */
  public int nextToken() throws IOException {
    if (pushedBack) {
      pushedBack = false;
      return ttype;
    }
    if (!firstCharRead) {
      lastChar = readChar();  // read the first character
      firstCharRead = true;
    }
    while (Character.isWhitespace((char)lastChar))
      lastChar = readChar();
    if (commentChar > 0 &&
	lastChar == commentChar &&
	lineno > linenoOfLastToken) {
      while (lastChar == commentChar) {
	int currLineNo = lineno;
	while (lineno == currLineNo)
	  lastChar = readChar();
	while (Character.isWhitespace((char)lastChar))
	  lastChar = readChar();
      }
    }
    if (lastChar == -1) {
      ttype = StreamTokenizer.TT_EOF;
    }
    else if (isOrdinary(lastChar)) {
      ttype = lastChar;
      linenoOfLastToken = lineno;
      lastChar = readChar();
    }
    else {
      ttype = StreamTokenizer.TT_WORD;
      linenoOfLastToken = lineno;
      buf.setLength(0);
      while (lastChar != -1 &&
	     !Character.isWhitespace((char)lastChar) &&
	     !isOrdinary(lastChar)) {
	buf.append((char)lastChar);
	lastChar = readChar();
      }
      sval = buf.toString();
    }
    return ttype;
  }

  private final boolean isOrdinary(int ch) {
    return ch < Byte.MAX_VALUE && ordinary.get(ch);
  }

  /**
   * Causes the most recent token read (either a word or ordinary character)
   * to be pushed back, so that it is the next token returned by
   * {@link #nextToken}.
   */
  public void pushBack() {
    pushedBack = true;
  }

  /**
   * Returns the line number of the underlying character stream.
   *
   * @return the line number of the underlying character stream.
   */
  public int lineno() { return lineno; }

  /**
   * Closes the underlying stream.
   *
   * @throws IOException if the underlying stream throws an
   * <code>IOException</code> while being closed
   */
  public void close() throws IOException {
    inStream.close();
  }

  /**
   * Returns the number of characters read from the underlying reader for
   * this word tokenizer.
   * @return the number of characters read
   */
  public long numCharsRead() {
    return numCharsRead;
  }

  // helper methods
  private final int readChar() throws IOException {
    int ch = inStream.read();
    numCharsRead++;

    // as we read chars, we continuously try to match the line separator
    // string; as soon as a character differs, we reset our match index,
    // lineSepMatchIdx, back to zero.  if we reach the length of the line
    // separator string, then we've just read a newline, and so we increase
    // lineno and reset lineSepMatchIdx;
    // alternatively, a newline character also delimits lines (the "default"
    // line separator, as it were)
    if (ch == '\n') {
      lineno++;
      lineSepMatchIdx = 0;
    }
    else if (ch == lineSep[lineSepMatchIdx]) {
      lineSepMatchIdx++;
      if (lineSepMatchIdx == lineSepLength) {
	lineno++;
	lineSepMatchIdx = 0;
      }
    }
    else
      lineSepMatchIdx = 0;

    return ch;
  }
}
