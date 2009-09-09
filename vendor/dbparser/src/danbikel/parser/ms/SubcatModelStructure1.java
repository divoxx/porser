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
 * Provides the complete back-off structure of the subcat-generation model
 * for either side of the head child.
 * <p/>
 * The specific back-off structure provided by this class is:
 * <ul>
 * <li><i>p( &hellip; | H, P, w<sub>h</sub>, t<sub>h</sub>)</i>
 * <li><i>p( &hellip; | H, P, t)</i>
 * <li><i>p( &hellip; | H, P)</i>
 * </ul>
 */
abstract public class SubcatModelStructure1 extends ProbabilityStructure {
  protected SubcatModelStructure1() { super(); }

  /** Returns 4. */
  public int maxEventComponents() { return 4; }
  /** Returns 3. */
  public int numLevels() { return 3; }

  /** Returns <tt>0.0</tt> regardlesss of back-off level. */
  public double lambdaFudge(int backOffLevel) { return 0.0; }
  /** Returns <tt>5.0</tt> regardlesss of back-off level. */
  public double lambdaFudgeTerm(int backOffLevel) { return 5.0; }

  /**
   * Returns a history for the specified back-off level, according to
   * the following zero-indexed list of history events.
   * <ul>
   * <li><i>p( &hellip; | H, P, w<sub>h</sub>, t<sub>h</sub>)</i>
   * <li><i>p( &hellip; | H, P, t)</i>
   * <li><i>p( &hellip; | H, P)</i>
   * </ul>
   *
   * @param trainerEvent the maximal-context event from which to derive
   * the history events used by the various subcat models that are subclasses
   * of this class
   * @param backOffLevel the back-off level for which to get a history
   * @return a history to condition on when generating a either a left or right
   *         subcat
   */
  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    HeadEvent headEvent = (HeadEvent)trainerEvent;

    Sexp head =
      Language.training().removeGapAugmentation(headEvent.head());
    head = Language.training().removeArgAugmentation(head.symbol());
    Sexp parent =
      Language.training().removeGapAugmentation(headEvent.parent());
    parent = Language.training().removeArgAugmentation(parent.symbol());

    MutableEvent history = histories[backOffLevel];
    history.clear();
    switch (backOffLevel) {
    case 0:
      // for p(Subcat | H, P, w, t)
      history.add(head);
      history.add(parent);
      history.add(headEvent.headWord().word());
      history.add(headEvent.headWord().tag());
      break;
    case 1:
      // for p(Subcat | H, P, t)
      history.add(head);
      history.add(parent);
      history.add(headEvent.headWord().tag());
      break;
    case 2:
      // for p(Subcat | H, P)
      history.add(head);
      history.add(parent);
      break;
    }
    return history;
  }
}
