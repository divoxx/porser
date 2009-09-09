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
import java.util.*;

/**
 * This class provides the abstract base type for S-epxressions, which are
 * either symbols or lists.
 *
 * @see Symbol
 * @see SexpList
 */
abstract public class Sexp implements Externalizable {
  private static final String className = Sexp.class.getName();

  // all Sexp objects are constructed via the read method
  Sexp() {}

  /**
   * Returns this object cast to a <code>Symbol</code>.
   * @exception ClassCastException if this object is not type-compatible with
   * <code>Symbol</code>
   *
   * @return this object cast to a <code>Symbol</code>.
   */
  public final Symbol symbol() { return (Symbol)this; }

  /**
   * Returns this object cast to a <code>SexpList</code>.
   * @exception ClassCastException if this object is not type-compatible with
   * <code>SexpList</code>
   *
   * @return this object case to a <code>SexpList</code>.
   */
  public final SexpList list() { return (SexpList)this; }


  /**
   * Returns <code>true</code> if this is an instance of a
   * <code>SexpList</code>, <code>false</code> otherwise.
   *
   * @return <code>true</code> if this is an instance of a
   * <code>SexpList</code>, <code>false</code> otherwise.
   */
  public abstract boolean isList();
  /**
   * Returns <code>true</code> if this is an instance of a <code>Symbol</code>,
   * <code>false</code> otherwise.
   *
   * @return <code>true</code> if this is an instance of a <code>Symbol</code>,
   * <code>false</code> otherwise.
   */
  public abstract boolean isSymbol();

  /**
   * Returns a deep copy of this S-expression.
   *
   * @return a deep copy of this S-expression.
   */
  abstract public Sexp deepCopy();

  /**
   * Returns a canonical version of this S-expression.  If this
   * S-expression is a symbol, this object is returned;
   * otherwise, if it is a zero-element list,
   * <code>SexpList.emptyList</code> is returned; otherwise, if it is
   * a key in <code>map</code>, the map's value for this list is
   * returned; otherwise, this list is added as a reflexive key-value
   * pair in <code>map</code>, after its <code>trimToSize</code> method
   * has been invoked.  Note that this method has a superset of
   * the functionality of <code>SexpList.getCanonical(SexpList)</code>.
   *
   * @param map the reflexive map of <code>SexpList</code> objects with
   * which to canonicalize this <code>Sexp</code> object
   *
   * @see SexpList#getCanonical(SexpList)
   * @see SexpList#trimToSize
   *
   * @return a canonical version of this S-expression, using the specified
   * map to perform canonicalization
   */
  public final Sexp getCanonical(Map map) {
    if (this.isSymbol())
      return this;
    else {
      if (this.list().size() == 0)
	return SexpList.emptyList;
      else {
	Sexp mapElt = (Sexp)map.get(this);
	if (mapElt == null) {
	  this.list().trimToSize();
	  // make sure if any elements are themselves lists, that they are
	  // canonicalized
	  SexpList thisList = this.list();
	  int size = thisList.size();
	  for (int i = 0; i < size; i++) {
	    Sexp curr = thisList.get(i);
	    if (curr.isList())
	      thisList.set(i, curr.getCanonical(map));
	  }

	  /*
	  SexpList newList = new SexpList.HashCache(thisList, true);
	  map.put(newList, newList);
	  return newList;
	  */
	  map.put(this, this);
	  return this;
	}
	else {
	  return mapElt;
	}
      }
    }
  }

  /**
   * Returns the S-expression contained in the stream held by <code>tok</code>.
   * If there are no tokens remaining in <code>tok</code>, this method returns
   * <code>null</code>.
   *
   * @param tok the tokenizer from which to read an S-expression
   * @return the next S-expression that can be read from the specified
   * S-expression tokenizer
   *
   * @exception IOException if there is an unexpected end of stream, mismatched
   * parentheses or an unexpected character
   */
  public static Sexp read(SexpTokenizer tok) throws IOException {
    while (tok.nextToken() != StreamTokenizer.TT_EOF) {
      switch (tok.ttype) {
      case StreamTokenizer.TT_WORD:
	return Symbol.add(tok.sval);
      case '(':
	SexpList list = new SexpList();
	while (tok.nextToken() != ')') {
	  if (tok.ttype == StreamTokenizer.TT_EOF)
	    throw new IOException(className + ": error: "+
				  "unexpected end of stream (line " +
				  tok.lineno() + ", num read " + tok.numCharsRead() +
				  ")\n\tpartial list: " + list);
	  tok.pushBack();
	  Sexp listElement = Sexp.read(tok);
	  list.add(listElement);
	}
	return list;
      case ')':
	throw new IOException(className + ": error: mismatched parentheses");
      default:
	throw new IOException(className + ": error: " +
			      "unexpected character: " + tok.ttype);
      }
    }
    // if the tokenizer has no more tokens, return null
    return null;
  }

  public static Sexp read(SexpTokenizer tok, char open, char close)
    throws IOException {
    while (tok.nextToken() != StreamTokenizer.TT_EOF) {
      int ttype = tok.ttype;
      if (ttype == StreamTokenizer.TT_WORD) {
	return Symbol.add(tok.sval);
      }
      else if (ttype == open) {
	SexpList list = new SexpList();
	while (tok.nextToken() != close) {
	  if (tok.ttype == StreamTokenizer.TT_EOF)
	    throw new IOException(className + ": error: "+
				  "unexpected end of stream (line " +
				  tok.lineno() + ", num read " + tok.numCharsRead() +
				  ")\n\tpartial list: " + list);
	  tok.pushBack();
	  Sexp listElement = Sexp.read(tok, open, close);
	  list.add(listElement);
	}
	return list;
      }
      else if (ttype == close) {
	throw new IOException(className + ": error: mismatched parentheses");
      }
      else {
	throw new IOException(className + ": error: " +
			      "unexpected character: " + tok.ttype);
      }
    }
    // if the tokenizer has no more tokens, return null
    return null;
  }

  /**
   * Returns the S-expression contained in the specified string.  If the string
   * contains no tokens, this method returns <code>null</code>.
   *
   * @param in the string from which to read an S-expression
   * @return the first S-expression contained in the specified string
   *
   * @throws IOException if there was an underlying problem reading from
   * the specified string
   */
  public static Sexp read(String in) throws IOException {
    return read(new SexpTokenizer(new StringReader(in)));
  }


  /**
   * Reads this {@link Sexp} object from the specified object stream (to be
   * implemented by all concrete subclasses).
   *
   * @param oi the object input stream
   * @throws IOException if the underlying stream has an exception
   * @throws ClassNotFoundException if the class of the object that is read
   * cannot be found
   */
  public abstract void readExternal(ObjectInput oi)
    throws IOException, ClassNotFoundException;

  /**
   * Writes this {@link Sexp} object from the specified object stream (to be
   * implemented by all concrete subclasses).
   * @param oo the object output stream to which this object will be written
   * @throws IOException if the underlying stream thrown an exception
   */
  public abstract void writeExternal(ObjectOutput oo) throws IOException;

  /**
   * A (very simple) test driver for this class.
   * @param args arguments (all currently ignored)
   */
  public static void main(String[] args) {
    try {
      //System.out.println(Sexp.read("(a"));
      Sexp s1 = Sexp.read("(foo bar)");
      Sexp s2 = Sexp.read("(bar soap)");
      //s1.add(Symbol.add("baz"));
      StringReader sr = new StringReader("[NP the_DT man_NN NP]");
      SexpTokenizer tok = new SexpTokenizer(sr, true, new char[]{'[', ']'});
      Sexp s3 = Sexp.read(tok, '[', ']');
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }
}
