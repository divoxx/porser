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

import danbikel.lisp.*;

/**
 * Factory to produce {@link PartialLexTreeConstraintSet} objects.
 */
public class PartialLexTreeConstraintSetFactory
  implements ConstraintSetFactory {

  /**
   * Constructs a new factory for {@link PartialLexTreeConstraintSet} objects.
   */
  public PartialLexTreeConstraintSetFactory() {}

  /**
   * Returns a new, empty {@link PartialLexTreeConstraintSet} object.
   * @return a new, empty {@link PartialLexTreeConstraintSet} object.
   *
   * @see PartialLexTreeConstraintSet#PartialLexTreeConstraintSet()
   */
  public ConstraintSet get() {
    return new PartialLexTreeConstraintSet();
  }

  /**
   * Returns a new {@link PartialLexTreeConstraintSet} using the specified
   * syntactic tree.
   * @param tree the syntactic tree from which to construct a tree structure
   * of {@link PartialLexTreeConstraint} objects contained in a
   * {@link PartialLexTreeConstraintSet}
   * @return a new {@link PartialLexTreeConstraintSet} using the specified
   * syntactic tree.
   *
   * @see PartialLexTreeConstraintSet#PartialLexTreeConstraintSet(Sexp) 
   */
  public ConstraintSet get(Object tree) {
    return new PartialLexTreeConstraintSet((Sexp)tree);
  }
}
