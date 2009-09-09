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
 * that represent a subgraph (certain brackets) of a tree, constraining a
 * decoder to pursue only theories that contain the brackets of the
 * constraint set of these objects.
 */
public class PartialTreeConstraint implements Constraint, SexpConvertible {
  /**
   * Inner class to provide hidden "root" constraint that is always
   * partially satisfied, but never <i>fully satisfied</i> (much like some
   * people I know).
   *
   * @see #satisfied
   * @see #fullySatisfied
   */
  static class Root extends PartialTreeConstraint {
    public Root(PartialTreeConstraint observedRoot) {
      label = Symbol.get("*ROOT*");
      children = new ArrayList(1);
      children.add(observedRoot);
      this.start = observedRoot.start();
      this.end = observedRoot.end();
      satisfied = true;
      fullySatisfied = false;
      // modify observedRoot's parent!!!
      observedRoot.parent = this;
    }
    public boolean isLeaf() { return false; }
    public boolean isViolatedByChild(Item childItem) { return false; }
    public boolean isViolatedBy(Item item) { return false; }
    public boolean isSatisfiedBy(Item item) { return true; }
    public boolean isLocallySatisfiedBy(Item item) { return true; }
  }

  /**
   * The parent constraint of this constraint.
   */
  protected PartialTreeConstraint parent;
  /**
   * All child constraints of this constraint.
   */
  protected List children;
  /**
   * The nonterminal label associated with this constraint.
   */
  protected Symbol label;
  /**
   * A {@link Nonterminal} object for use with
   * {@link Treebank#parseNonterminal(Symbol,Nonterminal)}.
   */
  protected Nonterminal nt = new Nonterminal();
  /**
   * A {@link Nonterminal} object for use with
   * {@link Treebank#parseNonterminal(Symbol,Nonterminal)}.
   */
  protected Nonterminal otherNT = new Nonterminal();
  /**
   * The starting word index of the syntactic subtree covered by this
   * constraint.
   */
  protected int start;
  /**
   * The ending word index of the syntactic subtree covered by this constraint.
   */
  protected int end;
  /**
   * Contains whether this constraint has been <i>partially satisfied</i>. A
   * constraint of this type has been partially satisfied if, during the
   * bottom-up decoding process, its label and other local information are
   * found to be consisting with the derivation being pursued.
   */
  protected boolean satisfied;
  /**
   * Contains whether this constraint has been <i>fully satisfied</i>.  A
   * constraint of this type is fully satisfied if it is both {@linkplain
   * #satisfied partially satisfied} and if all child constraints have been
   * {@linkplain #fullySatisfied fully satisfied}.
   */
  protected boolean fullySatisfied;

  /**
   * Constructs a tree of constraints rooted at the specified syntactic tree.
   *
   * @param tree a possibly-underspecified syntactic tree representing nodes
   * and spans that must be produced by any derivation pursued by the
   * decoder
   */
  public PartialTreeConstraint(Sexp tree) {
    this(null, tree, new IntCounter(0));
  }

  /**
   * Constructs an empty constraint tree.
   */
  protected PartialTreeConstraint() {}

  /**
   * A helper constructor for constructing a tree of constraints rooted
   * at the specified subtree with the specified <code>parent</code>.
   *
   * @param parent the parent of the specified tree, or <code>null</code>
   * if the specified tree has no parent
   * @param tree the tree for which to construct an isomorphic tree of
   * constraints
   * @param currWordIdx the start index of the span covered by the specified
   * tree, threaded through the recursive calls to this constructor
   */
  protected PartialTreeConstraint(PartialTreeConstraint parent,
				Sexp tree, IntCounter currWordIdx) {
    if (Language.treebank().isPreterminal(tree)) {
      this.parent = parent;
      children = Collections.EMPTY_LIST;
      Word word = Language.treebank().makeWord(tree);
      label = word.tag();
      Language.treebank().parseNonterminal(label, nt);
      start = end = currWordIdx.get();
      currWordIdx.increment();
    }
    else {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      //label = Language.treebank().getCanonical(treeList.symbolAt(0));
      label = treeList.symbolAt(0);
      Language.treebank().parseNonterminal(label, nt);
      start = currWordIdx.get();
      this.parent = parent;
      children = new ArrayList(treeListLen - 1);
      for (int i = 1; i < treeListLen; i++) {
	Sexp child = treeList.get(i);
	children.add(new PartialTreeConstraint(this, child, currWordIdx));
      }
      end = currWordIdx.get() - 1;
    }
  }

  /**
   * Returns whether this constraint corresponds to a preterminal in the
   * original syntactic tree.
   * @return whether this constraint corresponds to a preterminal in the
   * original syntactic tree.
   */
  public boolean isLeaf() { return children.size() == 0; }

  /**
   * Returns whether the specified child chart item violates this constraint
   * by having a span beyond the boundaries of this constraint's span.
   * @param childItem the child item to be tested
   * @return whether the specified child chart item violates this constraint
   * by having a span beyond the boundaries of this constraint's span.
   */
  public boolean isViolatedByChild(Item childItem) {
    //return !spanOK(childItem);
    if (!spanOK(childItem))
      return true;
    // if the child item's constraint has a span equal to the child item's span
    // then the child item's constraint must be fully satisfied
    PartialTreeConstraint ptc =
      (PartialTreeConstraint)childItem.getConstraint();
    if (ptc.spanMatches(childItem))
      return !ptc.fullySatisfied;
    else
      return false;
  }

  /**
   * Returns the parent of this constraint if this constraint has been {@link
   * #fullySatisfied}; otherwise, returns this constraint.
   * <p/>
   * <b>Implementation note</b>: While this method does not always return the
   * parent constraint of this constraint, the semantics of this method are
   * appropriate for imposing partial tree constraints during a bottom-up
   * decoding process.
   *
   * @return the parent of this constraint if this constraint has been {@link
   *         #fullySatisfied}; otherwise, returns this constraint.
   */
  public Constraint getParent() { return fullySatisfied ? parent : this; }

  /**
   * Returns the list of children of this constraint.
   * @return the list of children of this constraint.
   */
  protected List getChildren() { return children; }

  /**
   * Returns the nonterminal label associated with this constraint.
   * @return the nonterminal label associated with this constraint.
   */
  public Symbol label() { return label; }
  /**
   * Returns the start index of the span associated with this constraint.
   * @return the start index of the span associated with this constraint.
   */
  public int start() { return start; }
  /**
   * Returns the end index of the span associated with this constraint.
   * @return the end index of the span associated with this constraint.
   */
  public int end() { return end; }

  /**
   * Throws an {@link UnsupportedOperationException}, as this operation
   * is not appropriate for partial tree constraints.
   */
  public boolean isViolatedBy(Item item) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns <code>true</code>.
   * @param item a preterminal derivation item
   * @return <code>true</code> under all circumstances
   */
  protected boolean isSatisfiedByPreterminal(CKYItem item) {
    //if (isLocallySatisfiedBy(item) && spanMatches(item)) {
    if (true) {
      satisfied = true;
      fullySatisfied = true;
      return true;
    }
    else
      return false;
  }

  /**
   * Returns <code>true</code> if this constraint is satisfied by its local
   * information.  Internally, this constraint is said to be <i>fully
   * satisfied</i> if all its child constraints are fully satisfied, and if the
   * specified item has a matching span <i>and</i> a matching label, as
   * determined by the {@link #spanMatches(Item)} and {@link
   * #labelMatches(Item)} methods, respectively.
   *
   * @param item the item to test for satisfaction by this constraint
   * @return whether this constraint is satisfied the specified item
   *
   * @see #isLocallySatisfiedBy(Item)
   * @see #spanMatches(Item)
   * @see #labelMatches(Item)
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

    if (!isLocallySatisfiedBy(item))
      return false;

    satisfied = true;

    if (spanMatches(item) && labelMatches(item) &&
	allChildConstraintsAreFullySatisfied())
      fullySatisfied = true;

    return true;
  }

  private boolean allChildConstraintsAreFullySatisfied() {
    Iterator it = children.iterator();
    while (it.hasNext()) {
      PartialTreeConstraint child = (PartialTreeConstraint)it.next();
      if (!child.fullySatisfied)
	return false;
    }
    return true;
  }

  /**
   * Returns whether this constraint has been <i>partially satisfied</i>.
   * @return whether this constraint has been <i>partially satisfied</i>.
   *
   * @see #satisfied
   */
  public boolean hasBeenSatisfied() { return satisfied; }

  /**
   * Returns whether the specified item's span does not exceed the bounds
   * of the span associated with this constraint.
   * @param item the chart item whose span is to be tested
   * @return whether the specified item's span does not exceed the bounds
   * of the span associated with this constraint.
   */
  public boolean isLocallySatisfiedBy(Item item) {
    return spanOK(item);
  }

  /**
   * Returns whether the span of the specified item crosses the span.
   * of this constraint
   * @param item the item whose span is to be tested
   * @return whether the span of the specified item crosses the span
   */
  protected boolean spanOK(Item item) {
    CKYItem ckyItem = (CKYItem)item;
    return ckyItem.start() >= this.start && ckyItem.end() <= this.end();
  }

  /**
   * Returns whether the start and end indices of the specified chart item are
   * equal to the start and end indices, respectively, of the span associated
   * with this constraint.
   *
   * @param item the chart item whose span is to be compared to that associated
   *             with this constraint
   * @return whether the start and end indices of the specified chart item are
   *         equal to the start and end indices, respectively, of the span
   *         associated with this constraint.
   */
  protected boolean spanMatches(Item item) {
    CKYItem ckyItem = (CKYItem)item;
    return ckyItem.start() == start && ckyItem.end() == end;
  }

  /**
   * Returns whether this constraint's label subsumes the label of the
   * specified item.
   *
   * @param item the item whose label is to be tested
   * @return whether this constraint's label subsumes the label of the
   * specified item.
   *
   * @see Nonterminal#subsumes(Nonterminal)
   */
  protected boolean labelMatches(Item item) {
    Language.treebank().parseNonterminal((Symbol)item.label(), otherNT);
    return this.nt.subsumes(otherNT);
  }

  /**
   * Returns a symbolic expression representing the tree of constraints rooted
   * at this constraint (intended for debugging purposes).
   *
   * @return a symbolic expression representing the tree of constraints rooted
   *         at this constraint
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
   * Returns a human-readable string representation of this constraint (intended
   * for debugging purposes).
   * @return a human-readable string representation of this constraint.
   */
  public String toString() {
    return "label=" + label + ", span=(" + start + "," + end +
	   "), parentLabel=" +
	   (parent == null ? "null" : parent.label.toString()) +
	   ", sat=" + satisfied + ", fullySat=" + fullySatisfied;
  }
}
