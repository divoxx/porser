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

import java.util.*;
import danbikel.parser.Item;

/**
 * Specifies methods for searching a set of parsing constraints either for
 * satisfaction or violation of a particular chart item, as well as
 * predicates describing properties of a constraint set and other methods
 * to access various distinguished constraints in a set.
 *
 * @see Constraint
 * @see Item
 */
public interface ConstraintSet extends Collection {

  /**
   * Returns <code>true</code> if this constraint set forms a tree structure.
   * The return value of this method for an implementation of this interface
   * informs the {@link danbikel.parser.Decoder} using this {@link
   * ConstraintSet} that it can optimize constrain-parsing by tightly coupling
   * it with its bottom-up decoding algorithm.
   *
   * @return <code>true</code> if this constraint set forms a tree structure.
   */
  public boolean hasTreeStructure();

  /**
   * Returns <code>true</code> if at least one satisfying constraint should
   * be found for every chart item, <code>false</code> otherwise.  If this
   * method returns <code>true</code>, each chart item's satisfying constraint
   * should be &quot;attached&quot; via its
   * {@link Item#setConstraint(Constraint)} method.
   *
   * @return whether at least one satisfying constraint must be found per
   * chart item generated
   *
   * @see #constraintSatisfying(Item)
   */
  public boolean findAtLeastOneSatisfying();

  /**
   * Returns <code>true</code> if every chart item generated must violate
   * none of the constraints in this constraint set, <code>false</code>
   * otherwise.  Put procedurally, whenever a new chart item is generated,
   * all constraints in this set must be checked for violations (although
   * in practice, there may often be efficient ways to avoid an exhaustive
   * search).  This method should return <code>false</code> if an
   * implementation inherently guarantees consistency among its constraints,
   * thereby obviating the need for the decoder to check for violations
   * for every chart item generated, as is the case, for example, with the
   * {@link UnlexTreeConstraintSet} class.
   *
   * @return whether chart items need to violate none of the constraints
   * in this set
   *
   * @see #isViolatedBy(Item)
   */
  public boolean findNoViolations();

  /**
   * Returns the first constraint in this set that is found that the specified
   * item satisfies.
   *
   * @param item the item for which a satisfying constraint is to be found
   * @return the first constraint in this set that is found that the specified
   * item satisfies.
   */
  public Constraint constraintSatisfying(Item item);

  /**
   * Returns whether this constraint set is violated by the specified item
   * (optional operation).  It should usually be the case that
   * this method returns <code>true</code> if there is at least one constraint
   * in the set for which {@link Constraint#isViolatedBy(Item)} returns
   * <code>true</code>, but such behavior is not required.  For example,
   * this method may return <code>false</code> simply because the specified
   * item fails to satisfy certain constraints in this constraint set.
   *
   * @param item the item to be tested for violations
   * @return <code>true</code> if this constraint set is violated by the
   * specified item, <code>false</code> otherwise.
   */
  public boolean isViolatedBy(Item item);

  /**
   * Returns the root constraint in a set if the set forms a tree structure
   * (optional operation).
   *
   * @return the root constraint in a set if the set forms a tree structure
   *
   * @see #hasTreeStructure()
   */
  public Constraint root();

  /**
   * Returns a list containing the leaves of a tree-structured set of
   * constraints (optional operation). Such constraint leaves would typically
   * correspond to chart items representing words or preterminals.
   *
   * @return a list containing the leaves of a tree-structured set of
   * constraints
   *
   * @see #hasTreeStructure()
   */
  public List leaves();

}