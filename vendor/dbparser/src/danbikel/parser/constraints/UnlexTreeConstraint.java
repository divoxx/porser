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
    package danbikel.parser.constraints;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

/**
 * An implementation of a constraint to sit in a tree structure of constraints
 * that represents a particular, unlexicalized tree, constraining a decoder
 * to only pursue derivations consistent with that unlexicalized tree.
 */
public class UnlexTreeConstraint implements Constraint, SexpConvertible {
  /** The parent of this constraint. */
  protected UnlexTreeConstraint parent;
  /** The children of this constraint. */
  protected List children;
  /** The nonterminal label associated with this constraint. */
  protected Symbol label;
  /** The start index of the span associated with this constraint. */
  protected int start;
  /** The end index of the span associated with this constraint. */
  protected int end;
  /**
   * Contains whether this constraint has been satisfied at least once during
   * the bottom-up decoding process.
   */
  protected boolean satisfied;

  /**
   * Constructs the root constraint of a tree of constraints isomorphic to the
   * specified unlexicalized tree.
   * @param tree the unlexicalized syntactic tree with which to construct
   * this constraint and all its constraint subtrees
   */
  public UnlexTreeConstraint(Sexp tree) {
    this(null, tree, new IntCounter(0));
  }

  /**
   * Constructs an empty constraint.
   */
  protected UnlexTreeConstraint() {}

  /**
   * Constructs a tree of constraints isomorphic to the specified unlexicalized
   * syntactic tree.
   *
   * @param parent      the parent of the constraint subtree to be constructed
   * @param tree        the unlexicalized syntactic tree for which an isomorphic
   *                    constraint tree is to be constructed
   * @param currWordIdx the index of the leftmost word, threaded throughout the
   *                    recursive calls to this constructor
   */
  protected UnlexTreeConstraint(UnlexTreeConstraint parent,
				Sexp tree, IntCounter currWordIdx) {
    if (Language.treebank().isPreterminal(tree)) {
      this.parent = parent;
      children = Collections.EMPTY_LIST;
      Word word = Language.treebank().makeWord(tree);
      label = word.tag();
      start = end = currWordIdx.get();
      currWordIdx.increment();
    }
    else {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      label = treeList.symbolAt(0);
      if (!Language.treebank().isBaseNP(label))
        label = Language.treebank().getCanonical(label);
      start = currWordIdx.get();
      this.parent = parent;
      children = new ArrayList(treeListLen - 1);
      for (int i = 1; i < treeListLen; i++) {
	Sexp child = treeList.get(i);
	children.add(new UnlexTreeConstraint(this, child, currWordIdx));
      }
      end = currWordIdx.get() - 1;
    }
  }

  /**
   * Returns whether this constraint corresponds to a leaf (a preterminal).
   * @return whether this constraint corresponds to a leaf (a preterminal)
   */
  public boolean isLeaf() { return children.size() == 0; }

  /**
   * Returns whether this constraint is violated by the specified child chart
   * item. Violation occurs when the specified child item's parent's constraint
   * is not this constraint or if the specified child item's constraint is not
   * equal to any of this constraint's child constraints.
   *
   * @param childItem the child chart item item to be tested against this
   *                  constraint
   * @return whether this constraint is violated by the specified child chart
   *         item
   */
  public boolean isViolatedByChild(Item childItem) {
    return !(childItem.getConstraint().getParent() == this &&
	     children.contains(childItem.getConstraint()));
  }

  /**
   * Returns the parent of this constraint.
   * @return the parent of this constraint
   */
  public Constraint getParent() { return parent; }
  /**
   * Returns the children of this constraint.
   * @return the children of this constraint
   */
  protected List getChildren() { return children; }
  /**
   * Returns the nonterminal label associated with this constraint.
   * @return the nonterminal label associated with this constraint
   */
  public Symbol label() { return label; }
  /**
   * Returns the start index of the span covered by this constraint.
   * @return the start index of the span covered by this constraint
   */
  public int start() { return start; }
  /**
   * Returns the end index of the span covered by this constraint.
   * @return the end index of the span covered by this constraint
   */
  public int end() { return end; }

  /**
   * Throws an {@link UnsupportedOperationException}.
   */
  public boolean isViolatedBy(Item item) {
    throw new UnsupportedOperationException();
  }

  /**
   * Simply returns true and sets this constraint's satisfaction bit to
   * be true.
   * @param item the preterminal item to be tested against this constraint
   * @return true and sets this constraint's satisfaction bit to
   * be true
   */
  protected boolean isSatisfiedByPreterminal(CKYItem item) {
    //if (isLocallySatisfiedBy(item) && spanMatches(item)) {
    if (true) {
      satisfied = true;
      return true;
    }
    else
      return false;
  }

  /**
   * Returns <code>true</code> if this constraint is satisfied by its local
   * information and either
   * <ul>
   * <li>the specified item represents a preterminal or
   * <li>the constraints of the specified item's children are identical
   * to the children of this constraint, and are in the same order
   * </ul>
   * More formally, let us define the term <i>nuclear family</i> of a node
   * in a tree to refer to the node itself and its (immediately dominated)
   * sequence of children.  Given that chart items that have received their
   * stop probabilities form a tree structure, let us also define an
   * <i>item-induced constraint tree</i> as the tree of constraints induced
   * by mapping the nodes of a tree of stopped chart items to their assigned
   * constraints.  Let <tt>c</tt> be the nuclear family of this constraint
   * and let <tt>t</tt> be the nuclear family of the item-induced constraint
   * tree of the specified item.  This method returns <code>true</code> if
   * this constraint is satisfied by its local information and if <tt>c</tt>
   * is identical to <tt>t</tt>.
   *
   * @param item the item to test for satisfaction by this constraint
   * @return whether this constraint is satisfied the specified item
   *
   * @see #isLocallySatisfiedBy(Item)
   */
  public boolean isSatisfiedBy(Item item) {
    CKYItem ckyItem = (CKYItem)item;

    // normally, preterminal items should be assigned constraints from the
    // list of leaves, and thus this case should normally not be considered;
    // but just in case a brain-dead programmer creates a decoder that is
    // inefficient in this way, here's the code to deal with it
    if (ckyItem.isPreterminal()) {
      return isSatisfiedByPreterminal(ckyItem);
    }

    if (!isLocallySatisfiedBy(item) || !spanMatches(item))
      return false;

    // now, make sure that number of children equals number of child constraints
    int numLeftChildren = ckyItem.numLeftChildren();
    int numRightChildren = ckyItem.numRightChildren();
    if (numLeftChildren + numRightChildren + 1 != children.size())
      return false;
    // check that each child constraint is met in proper order
    // first, check left children
    int constraintIdx = 0;
    for (SLNode curr = ckyItem.children(Constants.LEFT);
	 curr != null && constraintIdx < children.size();
	 curr = curr.next(), constraintIdx++) {
      Constraint currConstraint = ((CKYItem)curr.data()).getConstraint();
      if (currConstraint != children.get(constraintIdx))
	return false;
    }
    // now, check head child
    int headChildIdx = numLeftChildren;
    if (ckyItem.headChild().getConstraint() != children.get(headChildIdx))
      return false;
    // finally, check right children
    constraintIdx = children.size() - 1;
    for (SLNode curr = ckyItem.children(Constants.RIGHT);
	 curr != null && constraintIdx >= 0;
	 curr = curr.next(), constraintIdx--) {
      Constraint currConstraint = ((CKYItem)curr.data()).getConstraint();
      if (currConstraint != children.get(constraintIdx))
	return false;
    }
    satisfied = true;
    return true;
  }

  /**
   * Returns whether this constraint has been satisfied by at least one
   * chart item.
   * @return whether this constraint has been satisfied by at least one
   * chart item
   */
  public boolean hasBeenSatisfied() { return satisfied; }

  /**
   * Returns whether the specified chart item satisfies the local information
   * contained in this constraint (span and nonterminal label).
   *
   * @param item the chart item to be tested against this constraint
   * @return whether the specified chart item satisfies the local information
   *         contained in this constraint (span and nonterminal label)
   */
  public boolean isLocallySatisfiedBy(Item item) {
    return (item.label() == label ||
	    Language.treebank().getCanonical((Symbol)item.label()) == label);
  }

  /**
   * Returns whether the specified item's span matches that of this constraint.
   * @param item the chart item whose span is to be compared to that of
   * this constraint
   * @return whether the specified item's span matches that of this constraint
   */
  protected boolean spanMatches(Item item) {
    CKYItem ckyItem = (CKYItem)item;
    return ckyItem.start() == start && ckyItem.end() == end;
  }

  /**
   * Returns a symbolic expression version of the constraint tree rooted
   * at this constraint.
   * @return a symbolic expression version of the constraint tree rooted
   * at this constraint
   */
  public Sexp toSexp() {
    SexpList retVal = new SexpList(children.size() + 1);

    Symbol thisNode = Symbol.add(label.toString() + "-" + start + "-" + end);

    retVal.add(thisNode);
    for (int i = 0; i < children.size(); i++)
      retVal.add(((SexpConvertible)children.get(i)).toSexp());

    return retVal;
  }

  /**
   * Returns a human-readable string representation of the local information
   * of this constraint (for debugging purposes).
   * @return a human-readable string representation of the local information
   * of this constraint
   */
  public String toString() {
    return "label=" + label + ", span=(" + start + "," + end +
	   "), parentLabel=" +
	   (parent == null ? "null" : parent.label.toString());
  }
}
