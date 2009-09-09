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

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.Serializable;

/**
 * Class to represent a chart item when performing the Inside-Outside algorithm.
 */
public class EMItem extends CKYItem.MappedPrevModBaseNPAware {
  /**
   * Holds references to the one or two antecedents that yielded a particular
   * consequent, along with the one or more events that generated the
   * consequent.  This class also has a next reference, so that it acts
   * as a node in a singly-linked list of {@link EMItem.AntecedentPair} objects.
   */
  public static class AntecedentPair implements Serializable {
    EMItem first;
    EMItem second;
    // for each pair of antecedents, there is one or more events that generated
    // this consequent; the events and their associated probabilities
    // are stored in the following co-indexed arrays
    transient TrainerEvent[] events;
    double[] probs;

    // the next pair in this singly-linked list
    AntecedentPair next;

    AntecedentPair(EMItem first, EMItem second,
		   TrainerEvent event, double prob, AntecedentPair next) {
      this(first, second, new TrainerEvent[]{event}, new double[]{prob}, next);
    }
    AntecedentPair(EMItem first, EMItem second,
		   TrainerEvent[] events, double[] probs,
		   AntecedentPair next) {
      this.first = first;
      this.second = second;
      this.events = events;
      this.probs = probs;
      this.next = next;
    }

    EMItem first() { return first; }
    EMItem second() { return second; }
    AntecedentPair next() { return next; }
    TrainerEvent[] events() { return events; }
    double[] probs() { return probs; }

    /**
     * The string representation of this antecedent pair.
     * @return the string representation of this antecedent pair
     */
    public String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("[");
      for (AntecedentPair curr = this; curr != null; curr = curr.next) {
        buf.append("(@");
        buf.append(System.identityHashCode(curr.first));
        if (curr.second != null) {
          buf.append(",@");
          buf.append(System.identityHashCode(curr.second));
        }
        buf.append(")");
        if (curr.next != null)
          buf.append(", ");
      }
      buf.append("]");
      return buf.toString();
    }
  }

  // additional data members
  /** A list of antecedent pairs for this item. */
  protected AntecedentPair antecedentPairs;
  /**
   * The unary production level for this item.
   *
   * @see EMDecoder#addUnaries(CKYItem, java.util.List)
   */
  protected int unaryLevel;

  /**
   * Constructs a new EM (Inside-Outside) chart item, with all data members
   * set to default values.
   *
   * @see #set(Symbol, Word, Subcat, Subcat, CKYItem, SLNode, SLNode, SexpList, SexpList, int, int, boolean, boolean, boolean, int, double)
   */
  public EMItem() {}

  /**
   * This method simply throws an UnsupportedOperationException,
   * as the log probabilities of the superclass are not used by this class.
   */
  public void set(Symbol label,
                  Word headWord,
                  Subcat leftSubcat,
                  Subcat rightSubcat,
                  CKYItem headChild,
                  SLNode leftChildren,
                  SLNode rightChildren,
                  SexpList leftPrevMods,
                  SexpList rightPrevMods,
                  int start,
                  int end,
                  boolean leftVerb,
                  boolean rightVerb,
                  boolean stop,
                  double logTreeProb,
                  double logPrior,
                  double logProb) {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets all the data for this EM chart item.
   *
   * @param label         the unlexicalized root label of this chart item
   * @param headWord      the head word of this chart item
   * @param leftSubcat    the subcat on the left side of this item's head child
   * @param rightSubcat   the subcat on the right side of this item's head
   *                      child
   * @param headChild     the head child item of this chart item
   * @param leftChildren  the modifiers on the left side of this item's head
   *                      child
   * @param rightChildren the modifiers on the right side of this item's head
   *                      child
   * @param leftPrevMods  a list of the previous modifiers on the left side of
   *                      this item's head child
   * @param rightPrevMods a list of the previous modifiers on the right side of
   *                      this item's head child
   * @param start         the index of the first word spanned by this item
   * @param end           the index of the last word spanned by this item
   * @param leftVerb      whether a verb has been generated anywhere in the
   *                      surface string of the modifiers on the left side of
   *                      this item's head child
   * @param rightVerb     whether a verb has been generated anywhere in the
   *                      surface string of the modifiers on the right side of
   *                      this item's head child
   * @param stop          whether this item has received its stop probabilities
   * @param unaryLevel    the unary production level of this item
   * @param insideProb    the total inside probability of the derivations
   *                      represented by this item
   */
  public void set(Symbol label,
                  Word headWord,
                  Subcat leftSubcat,
                  Subcat rightSubcat,
                  CKYItem headChild,
                  SLNode leftChildren,
                  SLNode rightChildren,
                  SexpList leftPrevMods,
                  SexpList rightPrevMods,
                  int start,
                  int end,
                  boolean leftVerb,
                  boolean rightVerb,
                  boolean stop,
                  int unaryLevel,
                  double insideProb) {
    super.set(label, headWord, leftSubcat, rightSubcat, headChild,
              leftChildren, rightChildren, leftPrevMods, rightPrevMods,
              start, end, leftVerb, rightVerb, stop, 0.0, 0.0, 0.0);
    setInsideProb(insideProb);
    setOutsideProb(0.0);
    antecedentPairs = null;
    this.unaryLevel = unaryLevel;
  }

  /**
   * Sets all the data in this item from the specified item.
   *
   * @param other the item whose data is to be transfered to this item.
   * @return this item
   */
  public CKYItem setDataFrom(CKYItem other) {
    super.setDataFrom(other);
    antecedentPairs = null;
    unaryLevel = ((EMItem)other).unaryLevel;
    setOutsideProb(0.0);
    return this;
  }

  // We employ the logProb, logPrior and logTreeProb data members for different
  // purposes in this class.  We accomplish this by overriding their
  // accessor/mutator methods so that they each throw an
  // UnsupportedOperationException and by adding new methods with appropriate
  // names that use the old data members for storage.
  /** Throws an {@link UnsupportedOperationException}. */
  public double logProb() {
    throw new UnsupportedOperationException();
  }
  /** Throws an {@link UnsupportedOperationException}. */
  public void setLogProb(double logProb) {
    throw new UnsupportedOperationException();
  }
  /** Throws an {@link UnsupportedOperationException}. */
  public double logPrior() {
    throw new UnsupportedOperationException();
  }
  /** Throws an {@link UnsupportedOperationException}. */
  public void setLogPrior(double logPrior) {
    throw new UnsupportedOperationException();
  }
  /** Throws an {@link UnsupportedOperationException}. */
  public double logTreeProb() {
    throw new UnsupportedOperationException();
  }
  /** Throws an {@link UnsupportedOperationException}. */
  public void setLogTreeProb(double logTreeProb) {
    throw new UnsupportedOperationException();
  }

  /*
  public double prob() { return logProb; }
  public void setProb(double prob) { logProb = prob; }
  */
  /**
   * Gets the outside probability of this item.
   * @return the outside probability of this item.
   */
  public double outsideProb() { return logPrior; }
  /**
   * Sets the outside probability of this item
   * @param outsideProb the outside probability to be set for this item
   */
  public void setOutsideProb(double outsideProb) {
    logPrior = outsideProb;
  }
  /**
   * Increases the outside probability of this item by the specified amount.
   * @param amount the amount to increase the outside probability of this item
   */
  public void increaseOutsideProb(double amount) {
    logPrior += amount;
  }

  /**
   * Gets the total inside probability of the derivations represented by this
   * item.
   *
   * @return the total inside probability of the derivations represented by this
   *         item
   */
  public double insideProb() { return logTreeProb; }
  /**
   * Sets the total inside probability of the derivations represented by this
   * item.
   * @param insideProb the inside probability to set for this item
   */
  public void setInsideProb(double insideProb) {
    logTreeProb = insideProb;
  }
  /**
   * Increases the inside probability of the derivations represented by this
   * item by the specified amount.
   * @param amount the amount by which to increase the inside probability
   * of the derivations represented by this item
   */
  public void increaseInsideProb(double amount) {
    logTreeProb += amount;
  }

  /** Gets the antecedent pairs for this item. */
  public AntecedentPair antecedentPairs() { return antecedentPairs; }
  /**
   * Sets the antecedent pairs for this item.
   * @param pair the list of antecedent pairs for this item
   */
  public void setAntecedentPairs(AntecedentPair pair) {
    this.antecedentPairs = pair;
  }

  /** Gets the unary production level of this item. */
  public int unaryLevel() { return unaryLevel; }
  /**
   * Sets the unary productions level of this item.
   * @param unaryLevel the unary production level to be set for this item
   */
  public void setUnaryLevel(int unaryLevel) { this.unaryLevel = unaryLevel; }

  /**
   * Clears all chart-related data in this item (most of the data members
   * do not need to be cleared, as they are expressly set by the decoder).
   * @return this item
   */
  public Item clear() {
    antecedentPairs = null;
    return super.clear();
  }

  /**
   * Returns whether this item is equal to the specified object, which must
   * also be an instance of {@link EMItem}.
   * @param obj the object to compare with this item
   * @return whether this item is equal to the specified object, which must
   * also be an instance of {@link EMItem}
   */
  public boolean equals(Object obj) {
    return super.equals(obj) && unaryLevel == ((EMItem)obj).unaryLevel();
  }
  /**
   * Returns a human-readable string representation of the information
   * in this item.
   * @return a human-readable string representation of the information
   * in this item.
   */
  public String toString() {
    return toSexp().toString() + "\t; head=" + headWord +
      "; lc=" + leftSubcat.toSexp() + "; rc=" + rightSubcat.toSexp() +
      "; leftPrev=" + leftPrevMods + "; rightPrev=" + rightPrevMods +
      "; lv=" + shortBool(leftVerb) + "; rv=" + shortBool(rightVerb) +
      "; hasVerb=" + shortContainsVerb(containsVerb) +
      "; stop=" + shortBool(stop) +
      "; inside=" + insideProb() +
      "; outside=" + outsideProb() +
      "; antecedentPairs=" + String.valueOf(antecedentPairs) +
      "; unaryLevel=" + unaryLevel +
      " (@" + System.identityHashCode(this) + ")";
  }
}
