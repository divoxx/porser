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
import java.io.*;
import java.util.*;

/**
 * Provides static methods to write out the contents of a <code>Map</code>
 * or a <code>Set</code> in an S-expression format.
 */
public class SymbolicCollectionWriter implements Serializable {
  private final static String lineSep = System.getProperty("line.separator");
  private final static char[] zeroCharArr = new char[0];


  private SymbolicCollectionWriter() {}

  /**
   * Writes out the contents of the specified set in an S-expression format.
   * The S-expression will have the form
   * <pre>
   * (name (element1 element2 elementN))
   * </pre>
   * or
   * <pre>
   * (element1 element2  ... elementN)
   * </pre>
   * if the specified name is <tt>null</tt>, where <tt>element</tt><i>i</i>
   * is the result of {@link #valueOf(Object)} for an object found in the
   * specified set, and where a single space character separates set elements.
   *
   * @param set the set to write out to the specified character writer
   * @param name the name of the set, or <tt>null</tt> if the set is to be
   * unnamed
   * @param writer the character writer to which to output the specified set
   * as an S-expression
   * @throws IOException
   */
  public static void writeSet(Set set, Symbol name, Writer writer)
    throws IOException {
    writeSet(set, name, writer, " ");
  }

  /**
   * Writes out the contents of the specified set in an S-expression format.
   * The S-expression will have the form
   * <pre>
   * (name (element1 element2 elementN))
   * </pre>
   * or
   * <pre>
   * (element1 element2  ... elementN)
   * </pre>
   * if the specified name is <tt>null</tt>, where <tt>element</tt><i>i</i>
   * is the result of {@link #valueOf(Object)} for an object found in the
   * specified set.<br>
   *
   * @param set the set to write out to the specified character writer
   * @param name the name of the set, or <tt>null</tt> if the set is to be
   * unnamed
   * @param writer the character writer to which to output the specified set
   * as an S-expression
   * @param sep the string to separate set elements
   * @throws IOException if the specified writer throws an
   * <code>IOException</code> during writing
   * @throws IllegalArgumentException if the specified separator string
   * does not consist entirely of whitespace characters
   */
  public static void writeSet(Set set, Symbol name, Writer writer, String sep)
      throws IOException {
    if (!Text.isAllWhitespace(sep)) {
      String className = SymbolicCollectionWriter.class.getName();
      throw new IllegalArgumentException(className +
					 ": error: separator has " +
					 "non-whitespace character");
    }

    writer.write("(");
    if (name != null) {
      writer.write(name.toString());
      writer.write(" (");
    }

    char[] initWhitespaceArr = zeroCharArr;
    // if the separator is the system-dependent line separator, then we do
    // some nice formatting, indenting every element the same amount
    if (sep.equals(lineSep)) {
      int initWhitespaceSize = (name == null ? 1 : name.toString().length() + 3);
      initWhitespaceArr = new char[initWhitespaceSize];
      Arrays.fill(initWhitespaceArr, ' ');
    }
    String initWhitespaceStr = new String(initWhitespaceArr);

    Iterator it = set.iterator();
    for (boolean first = true; it.hasNext(); first = false) {
      if (!first)
	writer.write(initWhitespaceStr);
      writer.write(valueOf(it.next()));
      if (it.hasNext())
	writer.write(sep);
    }
    if (name != null)
      writer.write(")");
    writer.write(")\n");
  }

  /**
   * Writes the specified set to the specified writer (with no header).
   * Synonymous with <code>writeSet(set,&nbsp;null,&nbsp;writer)</code>.
   * @param set the set whose elements are to be written to the specified
   * writer
   * @param writer the writer to which to write the specified set
   * @throws IOException if there is a problem writing the elements
   * of the specified set to the specified writer
   */
  public static void writeSet(Set set, Writer writer) throws IOException {
    writeSet(set, null, writer);
  }

  /**
   * Writes out the contents of <code>map</code> in an S-expression format.
   * Each <code>key-value</code> pair is written on its own line as
   * <pre> (name key value) </pre>
   * where <code>key</code> is the result of calling
   * <code>valueOf(key)</code> and <code>value</code> is the result of
   * calling <code>valueOf(value)</code>.
   * If <code>name</code> is <code>null</code>, then the format will be
   * <pre> (key value) </pre>
   * If the <code>value</code> in a <code>key-value</code> pair is an
   * instance of <code>Set</code> then each of that set's members is
   * output in a space-separated list within parentheses; otherwise,
   * the normal string representation of <code>value</code> is
   * written.
   *
   * @param map the map to write out
   * @param name the name to prepend to each key-value pair, or
   * <code>null</code> if the key-value pairs are to be unnamed
   * @param writer the output stream to write to
   *
   * @see #valueOf(Object)
   */
  public static void writeMap(Map map, Symbol name, Writer writer)
    throws IOException {
    Iterator it = map.keySet().iterator();
    while (it.hasNext()) {
      writer.write("(");
      if (name != null) {
	writer.write(name.toString());
	writer.write(" ");
      }
      Object key = it.next();
      Object value = map.get(key);
      writer.write(valueOf(key));
      writer.write(" ");

      if (value instanceof Set) {
	writer.write("(");
	Iterator setIterator = ((Set)value).iterator();
	while (setIterator.hasNext()) {
	  writer.write(valueOf(setIterator.next()));
	  if (setIterator.hasNext())
	    writer.write(" ");
	}
	writer.write(")");
      }
      else
	writer.write(valueOf(value));

      writer.write(")\n");
    }
  }

  /**
   * An alias for <code>writeMap(map, null, writer)</code>.
   *
   * @param map the map to write out
   * @param writer the output stream to write to
   */
  public static void writeMap(Map map, Writer writer) throws IOException {
    writeMap(map, null, writer);
  }

  /**
   * If the specified object is not an instance of a <code>Sexp</code> object
   * but <i>is</i> an instance of a <code>SexpConvertible</code> object,
   * then the value returned is the string representation of
   * <code>((SexpConvertible)obj).toSexp()</code>; otherwise, the value
   * returned is that returned by <code>String.valueOf(obj)</code>.
   */
  public final static String valueOf(Object obj) {
    if (!(obj instanceof Sexp) && obj instanceof SexpConvertible)
      return String.valueOf(((SexpConvertible)obj).toSexp());
    else
      return String.valueOf(obj);
  }
}
