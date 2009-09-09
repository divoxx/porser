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
 * Represents a set of constraints that correspond to a specific unlexicalized
 * parse tree, for use when the bottom-up parsing algorithm needs to generate
 * only the analyses that are consistent with a particular unlexicalized tree.
 * Accordingly, the individual <tt>Constraint</tt> objects in this set form
 * an isomorphic tree structure.
 */
public class UnlexTreeConstraintSet
  extends AbstractCollection implements ConstraintSet {

  /**
   * The root of this tree of constraints.
   */
  protected UnlexTreeConstraint root;

  /**
   * The number of constraints in this set.
   */
  protected int size;

  /**
   * A list of all the constraints in this set, for making iteration easy.
   */
  protected ArrayList list = new ArrayList();

  /**
   * The leaves of this tree of constraints in their correct order as they
   * correspond to single words (technically, preterminals) of a sentence.
   */
  protected ArrayList leaves = new ArrayList();

  /**
   * Constructs an empty set of constraints.
    */
  public UnlexTreeConstraintSet() {
  }

  /**
   * Constructs a constraint set derived from the specified unlexicalized
   * syntactic tree.
   *
   * @param tree the unlexicalized syntactic tree from which to construct a set
   *             of constraints
   */
  public UnlexTreeConstraintSet(Sexp tree) {
    buildConstraintSet(tree);
  }

  /**
   * Builds the constraint tree from the specified unlexicalized parse tree.
   * As a necessary side-effect, the {@link #root} and {@link #leaves} data
   * members will be set/populated by this method.
   *
   * @param tree the tree from which to build this constraint set
   */
  protected void buildConstraintSet(Sexp tree) {
    root = new UnlexTreeConstraint(tree);
    collectNodes(root);
    list.trimToSize();
    leaves.trimToSize();
  }

  /**
   * A helper method to collect all of the constraints in the specified
   * cosntraint tree.
   * @param tree the root of a tree of constraints to be collected
   */
  protected void collectNodes(UnlexTreeConstraint tree) {
    size++;
    list.add(tree);

    if (tree.isLeaf())
      leaves.add(tree);
    else {
      List children = tree.getChildren();
      for (int i = 0; i < children.size(); i++)
	collectNodes((UnlexTreeConstraint)children.get(i));
    }
  }

  // predicates
  /**
   * Returns <code>true</code>, since this type of constraint set does,
   * indeed, have a tree structure.
   *
   * @return <code>true</code>
   */
  public boolean hasTreeStructure() { return true; }

  /**
   * Returns <code>true</code>, since a satisfying constraint must be found
   * for every chart item.
   *
   * @return <code>true</code>
   */
  public boolean findAtLeastOneSatisfying() { return true; }

  /**
   * Returns <code>false</code>, since this type of constraint set guarantees
   * consistency among its constraints, meaning that, since every chart item
   * must have an assigned, satisfying constraint, there cannot be any
   * constraint violations, and therefore no such violation-checking needs
   * to occur during decoding.
   *
   * @return <code>false</code>
   *
   * @see #findAtLeastOneSatisfying()
   */
  public boolean findNoViolations() { return false; }

  /**
   * Returns the root of the constraint tree contained by this object.
   * @return the root of the constraint tree contained by this object
   */
  public Constraint root() { return root; }
  /**
   * Returns the leaves of the constraint tree contained by this object.
   * @return the leaves of the constraint tree contained by this object
   */
  public List leaves() { return leaves; }

  /**
   * Simply throws an <code>UnsupportedOperationException</code>, since
   * violations do not need to be checked for this type of constraint set.
   *
   * @param item the item to be checked for violations
   * @return <code>true</code> if the specified item violates a constraint
   * in this set, <code>false</code> otherwise
   *
   * @see #findNoViolations()
   */
  public boolean isViolatedBy(Item item) {
    throw new UnsupportedOperationException();
  }

  public Constraint constraintSatisfying(Item item) {
    // make sure all children items have assigned constraints, and that
    // all assigned constraints of children items have the same parent
    // constraint
    CKYItem ckyItem = (CKYItem)item;

    if (ckyItem.isPreterminal()) {
      Constraint constraint = (Constraint)leaves.get(ckyItem.start());
      return (constraint.isSatisfiedBy(item) ? constraint : null);
    }

    // get the parent of the head child's constraint
    Constraint headChildConstraintParent =
      ckyItem.headChild().getConstraint().getParent();

    return (headChildConstraintParent.isSatisfiedBy(item) ?
	    headChildConstraintParent : null);
  }

  /**
   * Returns a string representation of the constraint tree encapsulated
   * by this constraint set object.
   * @return a string representation of the constraint tree encapsulated
   * by this constraint set object
   */
  public String toString() {
    return root.toSexp().toString();
  }

  // methods to comply with Collection interface
  /** Returns the number of constraints in this constraint set. */
  public int size() { return size; }

  /**
   * Returns an iterator over the constraints in this set.
   * @return an iterator over the constraints in this set
   */
  public Iterator iterator() {
    return list.iterator();
  }

  /**
   * Test driver for this class.
   * @param args
   */
  public static void main(String[] args) {
    try {
      SexpTokenizer tok =
	new SexpTokenizer(System.in, Language.encoding(), 8192);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
	UnlexTreeConstraintSet set = new UnlexTreeConstraintSet(curr);
	System.out.println(((SexpConvertible)set.root()).toSexp());
      }
    }
    catch (Exception e) {
      System.err.println(e);
    }
  }
}