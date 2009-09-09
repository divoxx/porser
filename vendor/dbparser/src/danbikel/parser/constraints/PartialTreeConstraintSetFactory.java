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
 * Factory to produce {@link PartialTreeConstraintSet} objects.
 */
public class PartialTreeConstraintSetFactory implements ConstraintSetFactory {

  /**
   * Returns a new factory for {@link PartialTreeConstraintSet} objects.
   */
  public PartialTreeConstraintSetFactory() {}

  /**
   * Returns an empty partial tree constraint set.
   * @return an empty partial tree constraint set.
   */
  public ConstraintSet get() {
    return new PartialTreeConstraintSet();
  }

  /**
   * Returns a partial tree constraint set for the specified tree, which must
   * be a {@link Sexp} instance.
   * @param tree a {@link Sexp} representing a tree from which to construct
   * a set of partial tree constraints
   * @return a partial tree constraint set constructed from the specified
   * syntactic tree
   */
  public ConstraintSet get(Object tree) {
    return new PartialTreeConstraintSet((Sexp)tree);
  }
}
