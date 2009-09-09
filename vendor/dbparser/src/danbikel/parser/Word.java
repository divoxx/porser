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

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * A Word object is a structured representation of a word.  It includes:
 * <ul>
 * <li> the word itself
 * <li> the word's part of speech
 * <li> an optional representation of the word's features
 * </ul>
 *
 * @see WordFactory
 * @see Words
 * @see WordFeatures
 */
public class Word implements Serializable, Cloneable, SexpConvertible {

  // constants
  private final static String className = Word.class.getName();

  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method
  /** The word itself. */
  protected Symbol word;
  /** The part-of-speech of {@link #word}. */
  protected Symbol tag;
  /**
   * A word-feature vector of {@link #word}.
   *
   * @see WordFeatures
   */
  protected Symbol features;

  /**
   * Constructs a new instance with <code>null</code> for all data members.
   */
  protected Word() {}

  /**
   * Creates a new Word object with the specified word and part of speech.
   *
   * @param word the word itself (all lowercase).
   * @param tag its part-of-speech tag.
   */
  public Word(Symbol word, Symbol tag) {
    this(word, tag, null);
  }

  /**
   * Creates a new Word object with the specified word, part of speech
   * and word-feature vector.
   * @param word the word
   * @param tag the word's part of speech
   * @param features the word's feature vector (see {@link WordFeatures})
   */
  public Word(Symbol word, Symbol tag, Symbol features) {
    this.word = word;
    this.tag = tag;
    this.features = features;
  }

  /**
   * Constructs a word using the symbols contained in the specified
   * S-expression, which must be a list of at least two symbols.  The first
   * symbol is taken to be the word, the second its part of speech.  The
   * optional third symbol is the word's feature vector ({@link #features} is
   * left <code>null</code> if the specified list has only two elements).
   *
   * @param s the S-expression from which to construct a new {@link Word}
   *          instance; this S-expression must be a {@link SexpList} of length
   *          at least two, and containing all {@link Symbol} objects as its
   *          elements
   */
  public Word(Sexp s) {
    checkSexp(s);
    SexpList sexp = s.list();
    int sexpLen = sexp.length();
    word = sexp.symbolAt(0);
    tag = sexp.symbolAt(1);
    features = (sexpLen >= 3 ? sexp.symbolAt(2) : null);
  }

  /**
   * Checks that the S-expression passed to {@link #Word(Sexp)} is the right
   * format. Throws an {@link IllegalArgumentException} if the specified {@link
   * Sexp} is not in the right format.
   *
   * @param s the S-expression from which to construct a new {@link Word}
   *          instance; this S-expression must be a {@link SexpList} of length
   *          at least two, and containing all {@link Symbol} objects as its
   *          elements
   */
  protected void checkSexp(Sexp s) {
    if (s.isList() == false)
      throw new IllegalArgumentException(className +
					 ": S-expression passed to " +
					 "constructor is not a list");
    SexpList sexp = s.list();
    int sexpLen = sexp.length();
    if (!(sexpLen >= 2 && sexpLen <= 3))
      throw new IllegalArgumentException(className +
					 ": illegal Sexp length: " + sexpLen);

    if (!sexp.isAllSymbols())
	throw new IllegalArgumentException(className +
					   ": non-Symbol element to Sexp");

  }

  /**
   * Returns the word itself of this <code>Word</code> object.
   */
  public Symbol word() { return word; }

  /**
   * Sets the word itself of this <code>Word</code> object.
   *
   * @param word the word itself
   */
  public void setWord(Symbol word) { this.word = word; }

  /**
   * Returns the part-of-speech tag of this word.
   */
  public Symbol tag() { return tag; }

  /**
   * Sets the part-of-speech tag for this word.
   *
   * @param tag the part-of-speech tag
   */
  public void setTag(Symbol tag) { this.tag = tag; }

  /**
   * Returns the features of this word, or <code>null</code> if no features
   * have been set for this word.
   */
  public Symbol features() { return features; }

  /**
   * Sets the features for this word.
   */
  public void setFeatures(Symbol features) { this.features = features; }

  /**
   * Sets all three data members for this word.
   *
   * @return this Word object
   */
  public Word set(Symbol word, Symbol tag, Symbol features) {
    setWord(word);
    setTag(tag);
    setFeatures(features);
    return this;
  }

  /**
   * Returns a hash value for this object.
   *
   * @return the hash value for this object.
   */
  public int hashCode() {
    int code = word.hashCode();
    code = (code << 2) ^ tag.hashCode();
    return code;
  }

  /**
   * Determines whether two Word objects are equal.
   *
   * @param obj the Word object to compare with.
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj instanceof Word) {
      Word other = (Word)obj;
      return (word == other.word &&
	      tag == other.tag &&
	      features == other.features);
    }
    return false;
  }

  /**
   * Converts this Word object to a string (in S-expression format).
   *
   * @return the string representation.
   */
  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("(");
    b.append(word);
    b.append(" ");
    b.append(tag);
    if (features != null)
      b.append(" ").append(features);
    b.append(")");
    return b.toString();
  }

  /**
   * Returns a clone of this object, which is effectively a deep copy,
   * since all data members of unique {@link Symbol} references.
   * @return a clone of this object
   */
  public Object clone() {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException cnse) {
      System.err.println(cnse);
      return null;
    }
  }

  /**
   * Returns a clone of this object.  A shallow copy is all that is needed,
   * as all data members are basic types or references to unique objects
   * (symbols).
   */
  public Word copy() {
    return (Word)this.clone();
  }

  public Sexp toSexp() {
    return (features == null ?
	    new SexpList(2).add(word()).add(tag()) :
	    new SexpList(3).add(word()).add(tag()).add(features()));
  }
}
