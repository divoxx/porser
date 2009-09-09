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
 * A class to represent the marginal probabilities of lexicalized nonterminals
 * (loosely, if incorrectly, referred to as &ldquo;prior probabilities&rdquo;).
 */
public class PriorEvent implements TrainerEvent, Cloneable {
  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method.
  private Word headWord;
  private Symbol label;

  /**
   * Constructs a new <code>PriorEvent</code> object, setting all its
   * data members to the specified values.
   *
   * @param headWord the head word
   * @param label the unlexicalized nonterminal label
   */
  public PriorEvent(Word headWord, Symbol label) {
    this.headWord = headWord;
    this.label = label;
  }

  // accessors
  /** Returns the head word of this event. */
  public Word headWord() { return headWord; }
  /** Returns the nonterminal label of this event. */
  public Symbol label() { return label; }

  /**
   * Returns the same symbol for all instances of this class, so that priors
   * may be computed via the same mechanism as conditional probabilities: if
   * the conditioning context is the same for all events counted, then the MLEs
   * for those conditional events are the same as would be the MLEs for the
   * prior probabilities of the predicted events.  That is, when computing MLEs
   * via counting, P(X | Y) = P_prior(X) if Y is always the same.
   */
  public Symbol history() { return Language.training().stopSym(); }
  /** Returns <code>null</code>. */
  public Symbol parent() { return null; }
  /** Returns <code>null</code>. */
  public Word modHeadWord() { return null; }


  // mutators
  /**
   * Sets the head word and nonterminal label (all the data members) of this
   * event.
   *
   * @param headWord the head word
   * @param label    the nontemrinal label
   */
  public void set(Word headWord, Symbol label) {
    this.headWord = headWord;
    this.label = label;
  }
  /**
   * Sets the head word of this event.
   * @param headWord the head word
   */
  public void setHeadWord(Word headWord) { this.headWord = headWord; }
  void setLabel(Symbol label) { this.label = label; }

  /**
   * Throws an <code>UnsupportedOperationException</code>, as this is not
   * a modifier event.
   *
   * @exception UnsupportedOperationException because this is not a modifier
   * event
   */
  public boolean side() { throw new UnsupportedOperationException(); }

  /**
   * Returns whether the specified object is also an instance of this class
   * and is equal to this object.
   * @param obj the object to compare with this object
   * @return whether the specified object is also an instance of this class
   * and is equal to this object
   */
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof PriorEvent))
      return false;
    PriorEvent other = (PriorEvent)obj;
    boolean headWordsEqual = (headWord == null ? other.headWord == null :
			      headWord.equals(other.headWord));
    return headWordsEqual && label == other.label;
  }

  /**
   * Returns an S-expression string representation of the data in this object.
   * @return an S-expression string representation of the data in this object.
   */
  public String toString() {
    return "(" + headWord + " " + label + ")";
  }

  /**
   * Returns a hash code for this object.
   * @return a hash code for this object.
   */
  public int hashCode() {
    int code = 0;
    if (headWord != null)
      code = headWord.hashCode();
    code = (code << 2) ^ label.hashCode();
    return code;
  }

  /** Returns a deep copy of this object. */
  public Object clone() { return copy(); }
  /** Returns a deep copy of this object. */
  public TrainerEvent copy() {
    return new PriorEvent(headWord.copy(), label);
  }
  /** Returns a shallow copy of this object. */
  public TrainerEvent shallowCopy() {
    return new PriorEvent(headWord, label);
  }
}
