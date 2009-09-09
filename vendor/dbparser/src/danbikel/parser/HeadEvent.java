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
import java.util.*;

/**
 * A class to represent the head generation event implicit in the models
 * supported by this parsing package.  The class {@link Trainer} counts
 * such events, from which other events are derived.
 */
public class HeadEvent implements TrainerEvent, Cloneable {
  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method
  private Word headWord;
  private Symbol parent;
  private Symbol head;
  private Subcat leftSubcat;
  private Subcat rightSubcat;

  /**
   * Contructs a new object from the specified S-expression.  The
   * <code>Sexp</code> must be an instance of a list with the
   * following format:
   * <pre> (headWord parent head leftSubcat rightSubcat) </pre>
   * where
   * <ul>
   * <li> <tt>headWord</tt> is an S-expression that is compatible with the
   * {@link Word#Word(Sexp) Sexp word constructor}
   * <li> <tt>parent</tt> is a parent nonterminal label
   * <li> <tt>head</tt> is a head child nonterminal label
   * <li> <tt>leftSubcat</tt> is a list of arguments (nonterminal labels) and
   * possibly other elements to be generated on the left side of the head child
   * <li> <tt>rightSubcat</tt> is a list of arguments (nonterminal labels) and
   * possibly other elements to be generated on the right side of the head child
   * </ul>
   *
   * @param sexp a list containing all the information necessary to
   * construct this <code>HeadEvent</code> object
   */
  public HeadEvent(Sexp sexp) {
    this(Words.get(sexp.list().get(0)),
	 sexp.list().symbolAt(1),
	 sexp.list().symbolAt(2),
	 sexp.list().listAt(3),
	 sexp.list().listAt(4));
  }

  /**
   * Constructs a new <code>HeadEvent</code> object, setting all its data
   * members to the specified values.
   *
   * @param headWord    the head word
   * @param parent      the parent nonterminal label
   * @param head        the head nonterminal label
   * @param leftSubcat  an S-expression representation of the left
   *                    subcategorization frame, to be converted to a {@link
   *                    Subcat} instance via {@link Subcats#get(SexpList)}
   * @param rightSubcat an S-expression representation of the right
   *                    subcategorization frame, to be converted to a {@link
   *                    Subcat} instance via {@link Subcats#get(SexpList)}
   */
  public HeadEvent(Word headWord, Symbol parent, Symbol head,
		   SexpList leftSubcat, SexpList rightSubcat) {
    this(headWord, parent, head,
	 Subcats.get(leftSubcat), Subcats.get(rightSubcat));
  }

  /**
   * Constructs a new <code>HeadEvent</code> object, settings all its data
   * members to the specified values.
   *
   * @param headWord    the head word
   * @param parent      the parent nonterminal label
   * @param head        the head nonterminal label
   * @param leftSubcat  the left subcategorization frame
   * @param rightSubcat the right subcategorization frame
   */
  public HeadEvent(Word headWord, Symbol parent, Symbol head,
		   Subcat leftSubcat, Subcat rightSubcat) {
    set(headWord, parent, head, leftSubcat, rightSubcat);
  }

  void canonicalize(Map map) {
    headWord  = (Word)getCanonical(map, headWord);
    leftSubcat = (Subcat)getCanonical(map, leftSubcat);
    rightSubcat = (Subcat)getCanonical(map, rightSubcat);
  }

  Object getCanonical(Map map, Object obj) {
    Object mapObj = map.get(obj);
    if (mapObj == null) {
      map.put(obj, obj);
      return obj;
    }
    else
      return mapObj;
  }

  // accessors
  /** Returns the head word of this head event. */
  public Word headWord() { return headWord; }
  /** Returns the parent nonterminal label of this head event. */
  public Symbol parent() { return parent; }
  /** Returns the head nonterminal label of this head event. */
  public Symbol head() { return head; }
  /** Returns the left subcategorization frame of this head event. */
  public Subcat leftSubcat() { return leftSubcat; }
  /** Returns the right subcategorization frame of this head event. */
  public Subcat rightSubcat() { return rightSubcat; }

  // accessors to comply with interface TrainerEvent
  /**
   * Returns <code>null</code>, as head events do not deal with modifier words.
   */
  public Word modHeadWord() { return null; }

  // mutators

  public void setHeadWord(Word headWord) { this.headWord = headWord; }
  void setParent(Symbol parent) { this.parent = parent; }
  void setHead(Symbol head) { this.head = head; }
  void setLeftSubcat(Subcat leftSubcat) { this.leftSubcat = leftSubcat; }
  void setRightSubcat(Subcat rightSubcat) { this.rightSubcat = rightSubcat; }
  void set(Word headWord, Symbol parent, Symbol head,
	   Subcat leftSubcat, Subcat rightSubcat) {
    this.headWord = headWord;
    this.parent = parent;
    this.head = head;
    this.leftSubcat = leftSubcat;
    this.rightSubcat = rightSubcat;
  }

  /**
   * Throws an <code>UnsupportedOperationException</code>, as this is not
   * a modifier event.
   *
   * @exception UnsupportedOperationException because this is not a modifier
   * event
   */
  public boolean side() { throw new UnsupportedOperationException(); }

  /**
   * Returns <code>true</code> if the specified object is an instance of
   * a <code>HeadEvent</code> object containing data members which are all
   * pairwise-equal with the data members of this <code>HeadEvent</code>
   * object, according to each data member's <code>equals(Object)</code> method.
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof HeadEvent))
      return false;
    HeadEvent other = (HeadEvent)o;
    boolean headWordsEqual = (headWord == null ? other.headWord == null :
			      headWord.equals(other.headWord));
    return (headWordsEqual &&
	    parent == other.parent &&
	    head == other.head &&
	    leftSubcat.equals(other.leftSubcat) &&
	    rightSubcat.equals(other.rightSubcat));
  }

  /**
   * Returns an S-expression of the form accepted by
   * {@link HeadEvent#HeadEvent(Sexp)}.
   */
  public String toString() {
    return
      "(" + headWord + " " + parent + " " + head + " " + leftSubcat.toSexp() +
      " " + rightSubcat.toSexp() + ")";
  }

  /**
   * Returns the hash code of this object, calculated from the hash codes
   * of all its data members.
   */
  public int hashCode() {
    int code = 0;
    if (headWord != null)
      code = headWord.hashCode();
    code = (code << 2) ^ parent.hashCode();
    code = (code << 2) ^ head.hashCode();
    code = (code << 2) ^ leftSubcat.hashCode();
    code = (code << 2) ^ rightSubcat.hashCode();
    return code;
  }

  /** Returns a deep copy of this object. */
  public Object clone() { return copy(); }
  /** Returns a deep copy of this object. */
  public TrainerEvent copy() {
    return new HeadEvent(headWord.copy(),
			 parent,
			 head,
			 (Subcat)leftSubcat.copy(),
			 (Subcat)rightSubcat.copy());
  }

  public TrainerEvent shallowCopy() {
    return new HeadEvent(headWord, parent, head, leftSubcat, rightSubcat);
  }
}
