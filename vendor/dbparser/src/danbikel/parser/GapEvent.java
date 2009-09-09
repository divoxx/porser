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
 * A class to represent the gap generation event implicit in the models
 * supported by this parsing package.  The class {@link Trainer} counts
 * such events, from which other events are derived.
 */
public class GapEvent implements TrainerEvent, Cloneable {
  // constants
  /** The symbol representing a gap passed from a parent to its head. */
  public final static Symbol toHead = Symbol.add("head");
  /**
   * The symbol representing a gap passed from a parent to one of the
   * premodifiers of its head child.
   */
  public final static Symbol toLeft = Symbol.add("left");
  /**
   * The symbol representing a gap passed from a parent to one of the
   * postmodifiers of its head child.
   */
  public final static Symbol toRight = Symbol.add("right");

  // data members
  /** The direction of the gap passed from parent to child. */
  private Symbol direction;
  /** The head word of the parent and child involved in the gap event. */
  private Word headWord;
  /** The parent nonterminal that is passing its gap to one of its children. */
  private Symbol parent;
  /**
   * The head child of the parent that is passing its gap to one of its
   * children.
   */
  private Symbol head;

  /**
   * Contructs a new object from the specified S-expression.  The
   * <code>Sexp</code> must be an instance of a list with the
   * following format:
   * <pre> (direction headWord parent head) </pre>
   * where
   * <ul>
   * <li> <tt>direction</tt> is one of {{@link #toHead}, {@link #toLeft},
   * {@link #toRight}}
   * <li> <tt>headWord</tt> is an S-expression that is
   * compatible with the {@link Word#Word(Sexp)}
   * <li> <tt>parent</tt> is a parent nonterminal label
   * <li> <tt>head</tt> is a head child nonterminal label
   *
   * @param sexp a list containing all the information necessary to
   * construct this <code>HeadEvent</code> object
   */
  public GapEvent(Sexp sexp) {
    this(sexp.list().symbolAt(0),
	 Words.get(sexp.list().get(1)),
	 sexp.list().symbolAt(2),
	 sexp.list().symbolAt(3));
  }

  /**
   * Constructs a new <code>GapEvent</code> object, setting all its
   * data members to the specified values.
   *
   * @param direction a symbol specifying the direction of the gap (trace)
   * relative to the head child; this symbol must be one of {{@link #toHead},
   * {@link #toLeft}, {@link #toRight}}
   * @param headWord the head word
   * @param parent the parent nonterminal label
   * @param head the head nonterminal label
   */
  public GapEvent(Symbol direction, Word headWord, Symbol parent, Symbol head) {
    this.direction = direction;
    this.headWord = headWord;
    this.parent = parent;
    this.head = head;
  }

  void canonicalize(Map map) {
    headWord = (Word)getCanonical(map, headWord);
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
  /**
   * Returns the direction of this gap event: one of {{@link #toHead},
   * {@link #toLeft}, {@link #toRight}}.
   */
  public Symbol direction() { return direction; }
  /** Returns the head nonterminal label.  */
  public Symbol head() { return head; }
  /** Returns the head word. */
  public Word headWord() { return headWord; }
  /** Returns the parent nonterminal label. */
  public Symbol parent() { return parent; }

  // accessors to comply with interface TrainerEvent
  /**
   * Returns <code>null</code>, as gap events do not deal with modifier words.
   */
  public Word modHeadWord() { return null; }
  /**
   * Throws an <code>UnsupportedOperationException</code>, as this is not
   * a modifier event.
   *
   * @exception UnsupportedOperationException because this is not a modifier
   * event
   */
  public boolean side() { throw new UnsupportedOperationException(); }

  public void setHeadWord(Word word) {
    this.headWord = word;
  }

  /**
   * Returns <code>true</code> if the specified object is an instance of
   * a <code>GapEvent</code> object containing data members which are all
   * pairwise-equal with the data members of this <code>GapEvent</code>
   * object, according to each data member's <code>equals(Object)</code> method.
   */
  public boolean equals(Object o) {
    if (!(o instanceof GapEvent))
      return false;
    GapEvent other = (GapEvent)o;
    return (direction == other.direction &&
	    headWord.equals(other.headWord) &&
	    parent == other.parent &&
	    head == other.head);
  }

  /**
   * Returns an S-expression of the form accepted by
   * {@link GapEvent#GapEvent(Sexp)}.
   */
  public String toString() {
    return "(" + direction + " " + headWord + " " + parent + " " + head + ")";
  }

  /**
   * Returns the hash code of this object, calculated from the hash codes
   * of all its data members.
   */
  public int hashCode() {
    int code = direction.hashCode();
    code = (code << 2) ^ headWord.hashCode();
    code = (code << 2) ^ parent.hashCode();
    code = (code << 2) ^ head.hashCode();
    return code;
  }

  /** Returns a deep copy of this object. */
  public Object clone() { return copy(); }
  /** Returns a deep copy of this object. */
  public TrainerEvent copy() {
    return new GapEvent(direction, headWord.copy(), parent, head);
  }
  public TrainerEvent shallowCopy() {
    return new GapEvent(direction, headWord, parent, head);
  }
}
