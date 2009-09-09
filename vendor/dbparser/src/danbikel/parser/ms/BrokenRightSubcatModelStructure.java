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
    package danbikel.parser.ms;

import danbikel.parser.*;
import danbikel.lisp.*;

/**
 * Provides the complete back-off structure of the subcat-generation
 * model for the right side of the head child. This model structure is
 * just like {@link RightSubcatModelStructure1} but is &ldquo;broken&rdquo;
 * in that its {@link #lambdaFudge(int)} method returns <tt>5.0</tt> for
 * all back-off levels and its {@link #lambdaFudgeTerm(int)} method returns
 * <tt>0.0</tt> for all back-off levels, just as Collins had implemented
 * for his thesis parser.
 * <p/>
 * For the actual details of this model's back-off structure, please see
 * {@link SubcatModelStructure1}.
 */
public class BrokenRightSubcatModelStructure extends SubcatModelStructure1 {
  /**
   * Constructs a new {@link BrokenRightSubcatModelStructure} instance.
   */
  public BrokenRightSubcatModelStructure() {
    super();
  }

  /** Returns <tt>5.0</tt> regardless of the value of the argument. */
  public double lambdaFudge(int backOffLevel) { return 5.0; }
  /** Returns <tt>0.0</tt> regardless of the value of the argument. */
  public double lambdaFudgeTerm(int backOffLevel) { return 0.0; }

  /**
   * Gets the future being predicted conditioning on this subcat event.
   * @param trainerEvent the maximal-context event from which to get
   * the future being predicted
   * @param backOffLevel the back-off level whose estimate is being
   * sought
   * @return the future being predicted conditioning on the right subcat
   * event contained in the speciified {@link TrainerEvent} instance
   */
  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    return ((HeadEvent)trainerEvent).rightSubcat();
  }

  /** Returns a copy of this object. */
  public ProbabilityStructure copy() {
    return new BrokenRightSubcatModelStructure();
  }
}
