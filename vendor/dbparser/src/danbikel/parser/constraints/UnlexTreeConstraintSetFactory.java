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
 * Factory to produce {@link UnlexTreeConstraintSet} objects.
 */
public class UnlexTreeConstraintSetFactory implements ConstraintSetFactory {

  /**
   * Returns a new factory for {@link UnlexTreeConstraintSet} objects.
   */
  public UnlexTreeConstraintSetFactory() {}

  /**
   * Returns an empty {@link UnlexTreeConstraintSet} object.
   * @return an empty {@link UnlexTreeConstraintSet} object
   */
  public ConstraintSet get() {
    return new UnlexTreeConstraintSet();
  }

  /**
   * Returns an {@link UnlexTreeConstraintSet} constructed with the specified
   * syntactic tree.
   * @param tree the syntactic tree from which to construct a set
   * of constraints
   * @return an {@link UnlexTreeConstraintSet} constructed with the specified
   * syntactic tree
   */
  public ConstraintSet get(Object tree) {
    return new UnlexTreeConstraintSet((Sexp)tree);
  }
}
