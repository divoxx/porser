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
import danbikel.parser.constraints.*;
import java.io.Serializable;

/**
 * Skeletal class to represent items in a parsing chart.  Items implement the
 * comparable interface, so as to be sorted by their probability.
 *
 * @see Chart
 */
public abstract class Item implements Serializable, Comparable {
  // data member

  /** The log-probability of this chart item. */
  protected double logProb;

  // constructor

  /**
   * Constructs this item to have an initial log-probability of
   * <code>Constants.logOfZero</code>.  This constructor will be called,
   * often implicitly, by the constructor of a subclass.
   *
   * @see Constants#logOfZero
   */
  protected Item() {
    logProb = Constants.logOfZero;
  }

  /**
   * Constructs a chart item with the specified log-probability score.
   * @param logProb the log probability of this chart item, also known as its
   * <i>score</i>
   */
  protected Item(double logProb) {
    this.logProb = logProb;
  }

  /** Returns the label of this chart item. */
  public abstract Object label();

  /** Sets the label of this chart item. */
  public abstract void setLabel(Object label);

  /** Gets the log probability of this chart item. */
  public double logProb() { return logProb; }

  /** Sets the log probability of this chart item. */
  public void setLogProb(double logProb) { this.logProb = logProb; }

  /**
   * Returns whether this item has been eliminated from the chart because
   * another, equivalent item was added (meaning that this item could not
   * be immediately reclaimed, since the caller of
   * <code>Chart.add</code> may have a handle onto this item).
   *
   * @see Chart#add(int,int,Item)
   */
  public abstract boolean garbage();

  /**
   * Returns the constraint associated with this chart item, or
   * <code>null</code> if this item has no associated constraint.
   */
  public abstract Constraint getConstraint();

  /**
   * Sets the constraint for this item.
   * @param constraint the constraint to be associated with this item.
   */
  public abstract void setConstraint(Constraint constraint);

  /**
   * Sets the value of this item's garbage status.
   *
   * @see #garbage()
   * @see Chart#add(int,int,Item)
   */
  public abstract void setGarbage(boolean garbage);

  /**
   * Indicates that the specified item that was produced during decoding is
   * equivalent to this item.
   *
   * @param equivalentItem a chart item equivalent to this one
   */
  public void hasEquivalentItem(Item equivalentItem) { }

  /**
   * Clears data members of this item before reclamation (called by
   * {@link Chart#reclaimItem(Item)}).  The default implementation here does
   * nothing.
   *
   * @return the item being cleared (this item)
   */
  public Item clear() { return this; }

  /**
   * Compares this item's log-probability score with
   * that of the specified object, which must also be an instance of {@link
   * Item}.  Returns -1, 0 or 1, depending on whether this item's score is less
   * than, equal to or greater than, respectively, the specified item's score.
   * By implementing the {@link Comparable} interface, instances of {@link Item}
   * may be used in collections and with algorithms in the Java Collections
   * Framework that perform sorting.
   *
   * @param o the {@link Item} instance whose score is to be compared with the
   *          score of this item
   * @return -1, 0 or 1, depending on whether this item's score is less than,
   *         equal to or greater than, respectively, the specified item's score
   *
   * @see #logProb()
   */
  public int compareTo(Object o) {
    Item otherItem = (Item)o;
    return (logProb < otherItem.logProb ? -1 :
	    (logProb == otherItem.logProb ? 0 :
	     1));
  }
}
