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
 * Factory to produce {@link LexTreeConstraintSet} objects.
 */
public class LexTreeConstraintSetFactory
  implements ConstraintSetFactory {

  /**
   * Constructs a factory for {@link LexTreeConstraintSet} instances.
   */
  public LexTreeConstraintSetFactory() {}

  /**
   * Gets a new, empty {@link LexTreeConstraintSet} instance.
   * @return a new, empty {@link LexTreeConstraintSet} instance.
   */
  public ConstraintSet get() {
    return new LexTreeConstraintSet();
  }

  /**
   * Gets a new set of constraints for parsing the specified lexicalized tree.
   *
   * @param tree the lexicalized tree for which to get constraints;
   *             lexicalization performed by the current {@linkplain
   *             danbikel.parser.Language#headFinder() head finder}.
   * @return a new set of constraints for a lexicalized version of the specified
   *         parse tree.
   */
  public ConstraintSet get(Object tree) {
    return new LexTreeConstraintSet((Sexp)tree);
  }
}
